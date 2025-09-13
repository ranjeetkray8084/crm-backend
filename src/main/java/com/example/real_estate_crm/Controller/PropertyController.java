package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.dto.PropertyDTO;
import com.example.real_estate_crm.dto.PropertySearchRequest;
import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.Property;
import com.example.real_estate_crm.model.PropertyRemark;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.PropertyRemarkRepository;
import com.example.real_estate_crm.repository.PropertyRepository;
import com.example.real_estate_crm.repository.UserRepository;
import com.example.real_estate_crm.repository.CompanyRepository;
import com.example.real_estate_crm.model.Property.Status;
import com.example.real_estate_crm.service.NotificationService;
import com.example.real_estate_crm.service.dao.PropertyDao;
import com.example.real_estate_crm.service.dao.UserDao;
import com.example.real_estate_crm.security.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

// Excel export imports
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/companies/{companyId}/properties")
public class PropertyController {

    private final PropertyDao propertyService;
    private final NotificationService notificationService;
    private final UserDao userService;
    private final PropertyRemarkRepository propertyRemarkRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JwtUtil jwtUtil;

    public PropertyController(PropertyDao propertyService,
                            NotificationService notificationService,
                            UserDao userService,
                            PropertyRemarkRepository propertyRemarkRepository,
                            PropertyRepository propertyRepository,
                            UserRepository userRepository,
                            CompanyRepository companyRepository,
                            JwtUtil jwtUtil) {
        this.propertyService = propertyService;
        this.notificationService = notificationService;
        this.userService = userService;
        this.propertyRemarkRepository = propertyRemarkRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.jwtUtil = jwtUtil;
    }
    
    @PostMapping
    public ResponseEntity<?> createProperty(@PathVariable Long companyId, @RequestBody Property property) {
        if (property.getCreatedBy() == null || property.getCreatedBy().getUserId() == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("❌ Please login before creating a property.");
        }

        Long userId = property.getCreatedBy().getUserId();
        
        Optional<User> optionalUser = userService.findById(userId);
        if (optionalUser.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("❌ Invalid user. Please login again.");
        }

        User creator = optionalUser.get();
        
        // Fetch company from DB
        Company company = companyRepository.findById(companyId).orElse(null);
        if (company == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("❌ Company not found.");
        }
        
        property.setCreatedBy(creator);
        property.setCompany(company);

        try {
            Property created = propertyService.addProperty(property);

            // Notification Logic
            try {
                String message = "📢 A new property \"" + created.getPropertyName() + "\" was created by " + creator.getName();

                if (creator.getRole() == User.Role.USER) {
                    // USER created property - always notify director first
                    User director = userService.findDirectorByCompany(company);
                    if (director != null) {
                        notificationService.sendNotification(director.getUserId(), company, message);
                        
                        // Also notify all other directors if there are multiple
                        try {
                            var allDirectors = userRepository.findByCompanyIdAndRole(company.getId(), User.Role.DIRECTOR);
                            if (allDirectors.size() > 1) {
                                for (User otherDirector : allDirectors) {
                                    if (!otherDirector.getUserId().equals(director.getUserId())) {
                                        notificationService.sendNotification(otherDirector.getUserId(), company, message);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Continue processing if additional director notification fails
                        }
                    }
                    
                    // Also notify admin if user has admin assigned
                    if (creator.getAdmin() != null) {
                        notificationService.sendNotification(creator.getAdmin().getUserId(), company, message);
                    }

                } else if (creator.getRole() == User.Role.ADMIN) {
                    // ADMIN created property - notify director only
                    User director = userService.findDirectorByCompany(company);
                    if (director != null) {
                        notificationService.sendNotification(director.getUserId(), company, message);
                        
                        // Also notify all other directors if there are multiple
                        try {
                            var allDirectors = userRepository.findByCompanyIdAndRole(company.getId(), User.Role.DIRECTOR);
                            if (allDirectors.size() > 1) {
                                for (User otherDirector : allDirectors) {
                                    if (!otherDirector.getUserId().equals(director.getUserId())) {
                                        notificationService.sendNotification(otherDirector.getUserId(), company, message);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Continue processing if additional director notification fails
                        }
                    }
                }
                // DIRECTOR created property - no notifications sent (top level)
            } catch (Exception notificationEx) {
                // Don't fail the entire operation if notification fails
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(created);
            
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Failed to create property: " + ex.getMessage());
        }
    }



    @PutMapping("/{propertyId}")
    public ResponseEntity<Property> updatePropertyById(@PathVariable Long companyId, @PathVariable Long propertyId,
                                                       @RequestBody Property updatedProperty) {
        Property existingProperty = propertyService.findById(propertyId, companyId);
        if (existingProperty == null) {
            return ResponseEntity.notFound().build();
        }

        // Update fields only if provided
        if (updatedProperty.getPropertyName() != null) existingProperty.setPropertyName(updatedProperty.getPropertyName());
        if (updatedProperty.getType() != null) existingProperty.setType(updatedProperty.getType());
        if (updatedProperty.getBhk() != null) existingProperty.setBhk(updatedProperty.getBhk());
        if (updatedProperty.getUnitDetails() != null) existingProperty.setUnitDetails(updatedProperty.getUnitDetails());
        if (updatedProperty.getSize() != null) existingProperty.setSize(updatedProperty.getSize());
        if (updatedProperty.getOwnerName() != null) existingProperty.setOwnerName(updatedProperty.getOwnerName());
        if (updatedProperty.getOwnerContact() != null) existingProperty.setOwnerContact(updatedProperty.getOwnerContact());
        if (updatedProperty.getPrice() != null) existingProperty.setPrice(updatedProperty.getPrice());
        if (updatedProperty.getStatus() != null) existingProperty.setStatus(updatedProperty.getStatus());
        if (updatedProperty.getSector() != null) existingProperty.setSector(updatedProperty.getSector());
        if (updatedProperty.getSource() != null) existingProperty.setSource(updatedProperty.getSource());
        if(updatedProperty.getLocation() != null) existingProperty.setLocation(updatedProperty.getLocation());
        if(updatedProperty.getFloor() != null) existingProperty.setFloor(updatedProperty.getFloor());
        if(updatedProperty.getSource() !=null) existingProperty.setSource(updatedProperty.getSource());
        if(updatedProperty.getReferenceName() !=null) existingProperty.setReferenceName(updatedProperty.getReferenceName());

        Company company = new Company();
        company.setId(companyId);
        existingProperty.setCompany(company);
// ensure company is set

        Property savedProperty = propertyService.updateProperty(existingProperty);

      

        return ResponseEntity.ok(savedProperty);
    }

    @PostMapping("/{propertyId}/remarks")
    public ResponseEntity<?> addRemarkToProperty(@PathVariable Long companyId,
                                                 @PathVariable Long propertyId,
                                                 @RequestBody Map<String, String> requestBody) {
        // Find the property and check company match
        Optional<Property> optionalProperty = propertyRepository.findById(propertyId);
        if (optionalProperty.isEmpty() || !optionalProperty.get().getCompany().getId().equals(companyId)) {
            return ResponseEntity.notFound().build();
        }

        String remarkText = requestBody.get("remark");
        String userIdStr = requestBody.get("userId");

        if (remarkText == null || remarkText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Remark cannot be empty");
        }

        if (userIdStr == null || userIdStr.isEmpty()) {
            return ResponseEntity.badRequest().body("User ID is required");
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid User ID");
        }

        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        // Save the remark
        PropertyRemark remark = new PropertyRemark();
        remark.setRemark(remarkText);
        remark.setProperty(optionalProperty.get());
        remark.setCreatedBy(optionalUser.get());

        propertyRemarkRepository.save(remark);
        return ResponseEntity.ok("Remark added successfully");
    }


    @GetMapping("/{propertyId}/remarks")
    public ResponseEntity<?> getRemarksByPropertyId(@PathVariable Long companyId, @PathVariable Long propertyId) {
        Optional<Property> propertyOpt = propertyRepository.findById(propertyId);
        if (propertyOpt.isEmpty() || !propertyOpt.get().getCompany().getId().equals(companyId)) {
            return ResponseEntity.notFound().build();
        }

        List<PropertyRemark> remarks = propertyRemarkRepository.findByProperty_PropertyId(propertyId);
        return ResponseEntity.ok(remarks);
    }


    @PutMapping
    public ResponseEntity<Property> updateProperty(@PathVariable Long companyId, @RequestBody Property property) {
        Company company = new Company();
        company.setId(companyId);
        property.setCompany(company);

        Property updated = propertyService.updateProperty(property);

        String message = "Property updated: " + updated.getPropertyName();
        notificationService.sendNotification(1L, company, message);

        return ResponseEntity.ok(updated);
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<PropertyDTO>> getAllPropertiesPaged(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long userId
    ) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        User currentUser = optionalUser.get();
        String role = currentUser.getRole().name();

        Pageable pageable = PageRequest.of(page, size, Sort.by("propertyId").descending());
        Page<Property> propertiesPage = propertyRepository.findByCompanyId(companyId, pageable);

        List<PropertyDTO> filteredProperties = propertiesPage.getContent().stream()
                .map((Property property) -> {
                    // Handle null createdBy to prevent NullPointerException
                    if (property.getCreatedBy() == null) {
                        // If createdBy is null, hide sensitive info for all users except DIRECTOR
                        boolean hideSensitive = !"DIRECTOR".equals(role);
                        return PropertyDTO.from(property, hideSensitive);
                    }
                    
                    Long createdById = property.getCreatedBy().getUserId();

                    boolean hideSensitive = switch (role) {
                        case "DIRECTOR" -> false;

                        case "ADMIN" -> {
                            if (createdById.equals(userId)) {
                                yield false;
                            }
                            boolean isUserCreatedByThisAdmin = userRepository
                                    .findByUserIdAndAdmin_UserId(createdById, userId)
                                    .isPresent();
                            yield !isUserCreatedByThisAdmin;
                        }

                        case "USER" -> !createdById.equals(userId);

                        default -> true;
                    };

                    return PropertyDTO.from(property, hideSensitive);
                })
                .collect(Collectors.toList());


        Page<PropertyDTO> dtoPage = new PageImpl<>(filteredProperties, pageable, propertiesPage.getTotalElements());
        return ResponseEntity.ok(dtoPage);
    }



    @GetMapping("/{id}")
    public ResponseEntity<Property> getProperty(@PathVariable Long companyId, @PathVariable Long id) {
        Property property = propertyService.findById(id, companyId);
        return property != null ? ResponseEntity.ok(property) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProperty(@PathVariable Long companyId, @PathVariable Long id) {
        propertyService.deleteById(id, companyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{status}")
    public List<Property> getPropertiesByStatus(@PathVariable Long companyId, @PathVariable Status status) {
        return propertyService.getPropertiesByStatus(companyId, status);
    }

    @GetMapping("/sector/{sector}")
    public List<Property> getPropertiesBySector(@PathVariable Long companyId, @PathVariable String sector) {
        return propertyService.getPropertiesBySector(companyId, sector);
    }

    @GetMapping("/source/{source}")
    public List<Property> getPropertiesBySource(@PathVariable Long companyId, @PathVariable String source) {
        return propertyService.getPropertiesBySource(companyId, source);
    }


    @GetMapping("/search")
    public List<Property> searchPropertiesByName(@PathVariable Long companyId, @RequestParam String name) {
        return propertyService.searchPropertiesByName(companyId, name);
    }

    @GetMapping("/type/{type}")
    public List<Property> getPropertiesByType(@PathVariable Long companyId, @PathVariable String type) {
        return propertyService.getPropertiesByType(companyId, type);
    }

    @GetMapping("/bhk/{bhk}")
    public List<Property> getPropertiesByBhk(@PathVariable Long companyId, @PathVariable String bhk) {
        return propertyService.getPropertiesByBhk(companyId, bhk);
    }

    @GetMapping("/owner-contact/{contact}")
    public List<Property> getPropertiesByOwnerContact(@PathVariable Long companyId, @PathVariable String contact) {
        return propertyService.getPropertiesByOwnerContact(companyId, contact);
    }

    @GetMapping("/created-by/{userId}")
    public List<Property> getPropertiesByCreatedBy(@PathVariable Long companyId, @PathVariable Long userId) {
        return propertyService.getPropertiesByCreatedBy(companyId, userId);
    }
    
  

    @GetMapping("/{propertyId}/name")
    public ResponseEntity<String> getPropertyNameById(@PathVariable Long companyId, @PathVariable Long propertyId) {
        Optional<Property> propertyOptional = propertyRepository.findById(propertyId);

        if (propertyOptional.isPresent()) {
            Property property = propertyOptional.get();
            if (property.getCompany().getId().equals(companyId)) {
                return ResponseEntity.ok(property.getPropertyName());
            } else {
                return ResponseEntity.status(403).body("Property does not belong to this company.");
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/count")
    public ResponseEntity<Long> getTotalPropertyCount(@PathVariable Long companyId) {
        long count = propertyRepository.countByCompany_Id(companyId);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/created-by/{userId}/count")
    public ResponseEntity<Map<String, Long>> countPropertiesCreatedByUser(
            @PathVariable Long companyId,
            @PathVariable Long userId) {

        long count = propertyRepository.countByCompanyIdAndCreatedByUserId(companyId, userId);

        Map<String, Long> response = new HashMap<>();
        response.put("createdCount", count);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/search-paged")
    public ResponseEntity<Page<PropertyDTO>> searchPropertiesPaged(
            @PathVariable Long companyId,
            @RequestParam(required = false) List<String> keywords,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String bhk,
            @RequestParam(required = false) Long createdByName,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId // 🆕 Added userId for visibility control
    ) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        User currentUser = optionalUser.get();
        String role = currentUser.getRole().name();

        PropertySearchRequest request = new PropertySearchRequest();
        request.setCompanyId(companyId);
        request.setKeywords(keywords);
        request.setType(type);
        request.setStatus(status);
        request.setBhk(bhk);
        request.setCreatedBy(createdByName);
        request.setSource(source);
        request.setMinPrice(minPrice);
        request.setMaxPrice(maxPrice);

        Pageable pageable = PageRequest.of(page, size, Sort.by("propertyId").descending());
        Page<Property> propertiesPage = propertyService.advancedSearchPaged(request, pageable);

        List<PropertyDTO> filteredProperties = propertiesPage.getContent().stream()
                .map((Property property) -> {
                    // Handle null createdBy to prevent NullPointerException
                    if (property.getCreatedBy() == null) {
                        // If createdBy is null, hide sensitive info for all users except DIRECTOR
                        boolean hideSensitive = !"DIRECTOR".equals(role);
                        return PropertyDTO.from(property, hideSensitive);
                    }
                    
                    Long createdById = property.getCreatedBy().getUserId();

                    boolean hideSensitive = switch (role) {
                        case "DIRECTOR" -> false;

                        case "ADMIN" -> {
                            if (createdById.equals(userId)) {
                                yield false;
                            }
                            boolean isUserCreatedByThisAdmin = userRepository
                                    .findByUserIdAndAdmin_UserId(createdById, userId)
                                    .isPresent();
                            yield !isUserCreatedByThisAdmin;
                        }

                        case "USER" -> !createdById.equals(userId);

                        default -> true;
                    };

                    return PropertyDTO.from(property, hideSensitive);
                })
                .collect(Collectors.toList());

        Page<PropertyDTO> dtoPage = new PageImpl<>(filteredProperties, pageable, propertiesPage.getTotalElements());
        return ResponseEntity.ok(dtoPage);
    }


    
    @GetMapping("/count/property-overview")
    public ResponseEntity<Map<String, Long>> getPropertyCountSummary(@PathVariable Long companyId) {
        long total = propertyRepository.countByCompany_Id(companyId);
        long available_for_sale = propertyRepository.countByCompany_IdAndStatus(companyId, Property.Status.AVAILABLE_FOR_SALE);
        long available_for_rate = propertyRepository.countByCompany_IdAndStatus(companyId, Property.Status.AVAILABLE_FOR_RENT);

        long rent_out = propertyRepository.countByCompany_IdAndStatus(companyId, Property.Status.RENT_OUT);
        long sold_out= propertyRepository.countByCompany_IdAndStatus(companyId, Property.Status.SOLD_OUT);

        Map<String, Long> response = new HashMap<>();
        response.put("totalProperties", total);
        response.put("available for sale", available_for_sale);
        response.put("available for rent", available_for_rate);
        response.put("sold out", sold_out);
        response.put("rent out", rent_out);

        return ResponseEntity.ok(response);
    }

    // Excel Export Endpoint for Properties
    @GetMapping("/export")
    public ResponseEntity<Resource> exportPropertiesToExcel(
            @PathVariable Long companyId,
            @RequestParam(required = false) List<String> keywords,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) String userRole,
            @RequestParam(required = false) Long userId,
            HttpServletRequest request) {
        
        try {
            // Authentication check
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ByteArrayResource("Missing or invalid authorization header".getBytes()));
            }
            
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            if (!jwtUtil.isTokenValid(token) || jwtUtil.isTokenExpired(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ByteArrayResource("Invalid or expired token".getBytes()));
            }
            
            // Get user from token
            String email = jwtUtil.extractEmail(token);
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ByteArrayResource("User not found".getBytes()));
            }
            // Fetch all properties data
            List<Property> properties = propertyService.getAllProperties(companyId);
            
            if (properties.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ByteArrayResource("No properties found for export".getBytes()));
            }
            
            // Create Excel workbook
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Properties Export");
            
            // Create header row based on user role
            Row headerRow = sheet.createRow(0);
            int colNum = 0;
            
            // Define columns based on role (similar to frontend logic)
            if ("DIRECTOR".equals(userRole)) {
                String[] headers = {
                    "Property Name", "Location", "Type", "Status", "Price", "Size", 
                    "BHK", "Bathrooms", "Sector", "Created Date", "Created By", 
                    "Unit Details", "Source", "Owner Contact", "Owner Name", "Floor", "Reference Name"
                };
                
                for (String header : headers) {
                    Cell cell = headerRow.createCell(colNum++);
                    cell.setCellValue(header);
                }
            } else if ("ADMIN".equals(userRole)) {
                String[] headers = {
                    "Property Name", "Location", "Type", "Status", "Price", "Size", 
                    "BHK", "Bathrooms", "Sector", "Created Date", "Created By", 
                    "Unit Details", "Source", "Owner Contact", "Owner Name", "Floor", "Reference Name"
                };
                
                for (String header : headers) {
                    Cell cell = headerRow.createCell(colNum++);
                    cell.setCellValue(header);
                }
            } else {
                // Default USER role - hide sensitive information like PropertyDTO
                String[] headers = {
                    "Property Name", "Location", "Type", "Status", "Price", "Size", 
                    "BHK", "Bathrooms", "Sector", "Created Date", "Unit Details", "Source", "Owner Contact", "Owner Name", "Floor", "Reference Name"
                };
                
                for (String header : headers) {
                    Cell cell = headerRow.createCell(colNum++);
                    cell.setCellValue(header);
                }
            }
            
            // Add data rows
            int rowNum = 1;
            for (Property property : properties) {
                Row row = sheet.createRow(rowNum++);
                colNum = 0;
                
                // Format data similar to frontend
                addCellValue(row, colNum++, property.getPropertyName());
                addCellValue(row, colNum++, property.getLocation());
                addCellValue(row, colNum++, property.getType());
                addCellValue(row, colNum++, property.getStatus() != null ? property.getStatus().name() : "");
                addCellValue(row, colNum++, property.getPrice() != null ? property.getPrice().toString() : "");
                addCellValue(row, colNum++, property.getSize());
                addCellValue(row, colNum++, property.getBhk());
                addCellValue(row, colNum++, ""); // Bathrooms placeholder
                addCellValue(row, colNum++, property.getSector());
                addCellValue(row, colNum++, formatDate(property.getCreatedAt()));
                
                if ("DIRECTOR".equals(userRole) || "ADMIN".equals(userRole)) {
                    addCellValue(row, colNum++, property.getCreatedBy() != null ? property.getCreatedBy().getName() : "");
                }
                
                // Add unit details - hide for USER/ADMIN role unless created by same user
                if ("USER".equals(userRole) || "ADMIN".equals(userRole)) {
                    boolean isOwnProperty = property.getCreatedBy() != null && 
                                          property.getCreatedBy().getUserId().equals(user.getUserId());
                    addCellValue(row, colNum++, isOwnProperty ? property.getUnitDetails() : "🔒 Hidden");
                } else {
                    addCellValue(row, colNum++, property.getUnitDetails());
                }
                
                addCellValue(row, colNum++, property.getSource());
                
                // Add owner contact - hide for USER/ADMIN role unless created by same user
                if ("USER".equals(userRole) || "ADMIN".equals(userRole)) {
                    boolean isOwnProperty = property.getCreatedBy() != null && 
                                          property.getCreatedBy().getUserId().equals(user.getUserId());
                    addCellValue(row, colNum++, isOwnProperty ? property.getOwnerContact() : "🔒 Hidden");
                } else {
                    addCellValue(row, colNum++, property.getOwnerContact());
                }
                
                // Add owner name - hide for USER/ADMIN role unless created by same user
                if ("USER".equals(userRole) || "ADMIN".equals(userRole)) {
                    boolean isOwnProperty = property.getCreatedBy() != null && 
                                          property.getCreatedBy().getUserId().equals(user.getUserId());
                    addCellValue(row, colNum++, isOwnProperty ? property.getOwnerName() : "🔒 Hidden");
                } else {
                    addCellValue(row, colNum++, property.getOwnerName());
                }
                
                // Add floor - hide for USER/ADMIN role unless created by same user
                if ("USER".equals(userRole) || "ADMIN".equals(userRole)) {
                    boolean isOwnProperty = property.getCreatedBy() != null && 
                                          property.getCreatedBy().getUserId().equals(user.getUserId());
                    addCellValue(row, colNum++, isOwnProperty ? property.getFloor() : "🔒 Hidden");
                } else {
                    addCellValue(row, colNum++, property.getFloor());
                }
                
                // Add reference name - always visible
                addCellValue(row, colNum++, property.getReferenceName());
            }
            
            // Auto-size columns
            for (int i = 0; i < colNum; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Convert to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            
            byte[] excelBytes = outputStream.toByteArray();
            
            // Create filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String filename = "properties_export_" + timestamp + ".xlsx";
            
            // Create resource
            ByteArrayResource resource = new ByteArrayResource(excelBytes);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(excelBytes.length)
                    .body(resource);
                    
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ByteArrayResource(("Export failed: " + e.getMessage()).getBytes()));
        }
    }
    
    private void addCellValue(Row row, int colNum, String value) {
        Cell cell = row.createCell(colNum);
        cell.setCellValue(value != null ? value : "");
    }
    
    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

}
