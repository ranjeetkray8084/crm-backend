package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.dto.TaskFileDTO;
import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.TaskFile;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.model.UserSummary;
import com.example.real_estate_crm.repository.CompanyRepository;
import com.example.real_estate_crm.repository.TaskFileRepository;
import com.example.real_estate_crm.repository.UserRepository;
import com.example.real_estate_crm.service.NotificationService;
import com.example.real_estate_crm.service.dao.TaskFileDao;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/task-files")
@RequiredArgsConstructor
public class TaskFileController {

    private final TaskFileDao taskFileDao;
    private final TaskFileRepository taskrepo;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepo;
    private final NotificationService notification;
    private final Path uploadDir = Paths.get("uploads");

    // ✅ 1. Upload Excel File
    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcelFile(@RequestParam("title") String title,
                                             @RequestParam("companyId") Long companyId,
                                             @RequestParam("uploadedBy") Long uploadedById,
                                             @RequestParam("file") MultipartFile file) throws IOException {
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        if (!title.toLowerCase().endsWith(".xlsx")) {
            title += ".xlsx";
        }

        Path filePath = uploadDir.resolve(title);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        User uploadedBy = userRepository.findById(uploadedById)
                .orElseThrow(() -> new RuntimeException("Uploader not found"));

        TaskFile task = TaskFile.builder()
                .title(title)
                .filePath(filePath.toString())
                .companyId(companyId)
                .uploadedBy(uploadedBy)
                .uploadDate(LocalDateTime.now())
                .build();

        taskFileDao.save(task);
        return ResponseEntity.ok("File uploaded successfully.");
    }

    // ✅ 2. Assign/Unassign Task
    @PutMapping("/{taskId}/assign")
    public ResponseEntity<String> assignTaskToUser(@PathVariable Long taskId,
                                                   @RequestParam(required = false) Long userId,
                                                   @RequestParam Long companyId) {

        Optional<TaskFile> optionalTaskFile = taskrepo.findByIdAndCompanyId(taskId, companyId);
        if (optionalTaskFile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ Task not found for the given company.");
        }

        Optional<Company> optionalCompany = companyRepo.findById(companyId);
        if (optionalCompany.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ Company not found.");
        }

        Company company = optionalCompany.get();
        TaskFile taskFile = optionalTaskFile.get();
        Long previousAssignedUserId = taskFile.getAssignedTo();

        if (userId == null) {
            // Unassign logic
            taskFile.setAssignedTo(null);
            taskFileDao.save(taskFile);

            // Notify ONLY the user who was previously unassigned
            if (previousAssignedUserId != null) {
                Optional<User> previouslyAssignedUser = userRepository.findById(previousAssignedUserId);
                previouslyAssignedUser.ifPresent(u -> {
                    String message = "⚠️ Task \"" + taskFile.getTitle() + "\" has been unassigned from you.";
                    notification.sendNotification(u.getUserId(), company, message);
                });
            }

            return ResponseEntity.ok("✅ Task unassigned successfully.");
        } else {
            // Assign logic
            taskFile.setAssignedTo(userId);
            taskFileDao.save(taskFile);

            // Notify ONLY the newly assigned user
            Optional<User> newlyAssignedUser = userRepository.findById(userId);
            newlyAssignedUser.ifPresent(u -> {
                String message = "📌 Task \"" + taskFile.getTitle() + "\" has been assigned to you.";
                notification.sendNotification(u.getUserId(), company, message);
            });

            // Optional: Notify previous assignee if task was reassigned
            if (previousAssignedUserId != null && !previousAssignedUserId.equals(userId)) {
                Optional<User> previousUser = userRepository.findById(previousAssignedUserId);
                previousUser.ifPresent(u -> {
                    String unassignMessage = "⚠️ Task \"" + taskFile.getTitle() + "\" has been reassigned from you.";
                    notification.sendNotification(u.getUserId(), company, unassignMessage);
                });
            }

            return ResponseEntity.ok("✅ Task assigned successfully.");
        }
    }

    // ✅ 3. Preview Excel Data
    @GetMapping("/{taskId}/preview")
    public ResponseEntity<List<List<String>>> previewExcel(@PathVariable Long taskId,
                                                           @RequestParam Long companyId) throws Exception {
        TaskFile task = taskFileDao.findById(taskId)
                .filter(f -> Objects.equals(f.getCompanyId(), companyId))
                .orElseThrow(() -> new RuntimeException("Unauthorized or Not Found")); // Consider specific exception

        try (FileInputStream fis = new FileInputStream(task.getFilePath());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            List<List<String>> data = new ArrayList<>();

            int maxColumns = 0;

            // First pass: find max columns used across all rows
            for (Row row : sheet) {
                if (row.getLastCellNum() > maxColumns) {
                    maxColumns = row.getLastCellNum();
                }
            }

            // Second pass: collect rows with equal column count
            for (Row row : sheet) {
                List<String> rowData = new ArrayList<>();

                for (int i = 0; i < maxColumns; i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

                    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                        rowData.add(new SimpleDateFormat("dd-MMM-yyyy").format(cell.getDateCellValue()));
                    } else if (cell.getCellType() == CellType.NUMERIC) {
                        // Use toPlainString to avoid scientific notation for large numbers
                        rowData.add(BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString());
                    } else {
                        rowData.add(cell.toString().trim());
                    }
                }
                data.add(rowData);
            }
            return ResponseEntity.ok(data);
        }
    }

    // ✅ 4. Get Assigned Tasks
    @GetMapping("/assigned")
    public ResponseEntity<List<TaskFile>> getAssignedFiles(@RequestParam Long companyId,
                                                           @RequestParam Long userId) {
        List<TaskFile> assignedFiles = taskrepo.findByCompanyIdAndAssignedToOrderByUploadDateDesc(companyId, userId);
        return ResponseEntity.ok(assignedFiles);
    }

    // ✅ 5. Update Cell in Excel File
    @PatchMapping("/{taskId}/update-cell")
    public ResponseEntity<?> updateCell(@PathVariable Long taskId,
                                        @RequestParam Long companyId,
                                        @RequestParam int row,
                                        @RequestParam int col,
                                        @RequestParam String newValue) throws Exception {
        TaskFile task = taskFileDao.findById(taskId)
                .filter(f -> Objects.equals(f.getCompanyId(), companyId))
                .orElseThrow(() -> new RuntimeException("Unauthorized or Not Found")); // Consider specific exception

        File file = new File(task.getFilePath());

        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ File not found: " + file.getAbsolutePath());
        }

        FileInputStream fis = null;
        XSSFWorkbook workbook = null;

        try {
            fis = new FileInputStream(file);
            workbook = new XSSFWorkbook(fis);
            fis.close(); // Immediately close input stream after reading

            Sheet sheet = workbook.getSheetAt(0);
            Row targetRow = sheet.getRow(row);
            if (targetRow == null) targetRow = sheet.createRow(row);

            Cell cell = targetRow.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(newValue);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos); // Write changes
                fos.flush();
            }

            return ResponseEntity.ok("Cell updated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Failed to update cell: " + e.getMessage());
        } finally {
            if (workbook != null) workbook.close(); // Close workbook in finally block
        }
    }

  


    // ✅ 6. Delete Column in Excel File
    @DeleteMapping("/{taskId}/delete-column") // Using DELETE for a deletion operation
    public ResponseEntity<String> deleteColumn(@PathVariable Long taskId,
                                               @RequestParam Long companyId,
                                               @RequestParam int colIndex) { // 0-indexed column to delete

        Optional<TaskFile> optionalTaskFile = taskrepo.findByIdAndCompanyId(taskId, companyId);
        if (optionalTaskFile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ Task not found for the given company.");
        }

        TaskFile task = optionalTaskFile.get();
        File file = new File(task.getFilePath());

        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ File not found: " + file.getAbsolutePath());
        }

        FileInputStream fis = null;
        XSSFWorkbook workbook = null;

        try {
            fis = new FileInputStream(file);
            workbook = new XSSFWorkbook(fis);
            fis.close();

            Sheet sheet = workbook.getSheetAt(0); // Assuming you always work with the first sheet

            if (colIndex < 0) {
                 return ResponseEntity.badRequest().body("Column index cannot be negative.");
            }

            // Find the maximum column index used across all rows for correct shifting
            int lastColumn = 0;
            for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                if (row != null && row.getLastCellNum() > lastColumn) {
                    lastColumn = row.getLastCellNum();
                }
            }

            // If the column to delete is beyond the last existing column, it's an invalid request.
            // Note: lastColumn is 1-indexed, colIndex is 0-indexed.
            if (colIndex >= lastColumn) { // Check if colIndex is at or beyond the last actual column
                return ResponseEntity.badRequest().body("Column index is out of bounds or column does not exist.");
            }

            // Shift cells to the left, effectively deleting the column at colIndex
            // Cells from (colIndex + 1) up to lastColumn are shifted left by 1 position.
            sheet.shiftColumns(colIndex + 1, lastColumn, -1);

            // After shifting, the previous 'lastColumn' now contains the original 'lastColumn - 1' content.
            // Any cells in what was originally 'lastColumn' are now effectively empty, but might still exist.
            // To properly remove them, iterate through rows and remove the cell at the new 'lastColumn - 1' position.
            // This loop ensures the rightmost column's cells are truly removed after the shift.
            for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                if (row != null) {
                    // The column that was shifted into (now the effective last column)
                    Cell cellToDelete = row.getCell(lastColumn - 1);
                    if (cellToDelete != null) {
                        row.removeCell(cellToDelete);
                    }
                }
            }

            // Save the modified workbook
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
                fos.flush();
            }

            return ResponseEntity.ok("✅ Column " + colIndex + " deleted successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Failed to delete column: " + e.getMessage());
        } finally {
            if (workbook != null) {
                try {
                    workbook.close(); // Ensure workbook is closed
                } catch (IOException e) {
                    e.printStackTrace(); // Log workbook close error
                }
            }
        }
    }

  

    // ✅ 7. Download Excel File
    @GetMapping("/{taskId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long taskId,
                                                 @RequestParam Long companyId) throws Exception {
        TaskFile task = taskFileDao.findById(taskId)
                .filter(f -> Objects.equals(f.getCompanyId(), companyId))
                .orElseThrow(() -> new RuntimeException("Unauthorized or Not Found")); // Consider specific exception

        Path path = Paths.get(task.getFilePath());
        Resource file = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + task.getTitle() + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    // ✅ 8. Get All Tasks for a Company (with assigned user name)
    @GetMapping
    public ResponseEntity<List<TaskFileDTO>> getAllTasksByCompanyId(@RequestParam Long companyId) {
        List<TaskFile> taskFiles = taskrepo.findByCompanyIdOrderByUploadDateDesc(companyId);

        // Extract all unique user IDs (assignedTo and uploadedBy) to minimize DB calls
        Set<Long> allUserIds = taskFiles.stream()
                .map(task -> {
                    Set<Long> ids = new HashSet<>();
                    if (task.getAssignedTo() != null) ids.add(task.getAssignedTo());
                    if (task.getUploadedBy() != null) ids.add(task.getUploadedBy().getUserId());
                    return ids;
                })
                .flatMap(Set::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Fetch user summaries in one go
        Map<Long, UserSummary> userMap = userRepository.findAllById(allUserIds).stream()
                .collect(Collectors.toMap(
                        User::getUserId,
                        user -> new UserSummary(user.getUserId(), user.getName())
                ));

        // Build DTOs
        List<TaskFileDTO> dtos = taskFiles.stream()
                .map(task -> TaskFileDTO.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .uploadDate(task.getUploadDate())
                        .assignedTo(
                                task.getAssignedTo() != null ? userMap.get(task.getAssignedTo()) : null
                        )
                        .uploadedByName(
                                task.getUploadedBy() != null && userMap.containsKey(task.getUploadedBy().getUserId())
                                        ? userMap.get(task.getUploadedBy().getUserId()).getName()
                                        : "Unknown" // Fallback if uploader is null or not found
                        )
                        .build()
                )
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ✅ 9. Get Tasks Uploaded by Specific User
    @GetMapping("/uploaded")
    public ResponseEntity<List<TaskFileDTO>> getTasksUploadedByUser(@RequestParam Long uploadedById,
                                                                  @RequestParam Long companyId) {
        List<TaskFile> taskFiles = taskrepo.findByCompanyIdAndUploadedBy_UserIdOrderByUploadDateDesc(companyId, uploadedById);

        // Collect all user IDs involved (assigned and the uploader)
        Set<Long> allRelatedUserIds = taskFiles.stream()
                .map(TaskFile::getAssignedTo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        allRelatedUserIds.add(uploadedById); // Make sure the uploader's ID is also included

        Map<Long, UserSummary> userMap = userRepository.findAllById(allRelatedUserIds).stream()
                .collect(Collectors.toMap(
                        User::getUserId,
                        user -> new UserSummary(user.getUserId(), user.getName())
                ));

        List<TaskFileDTO> dtos = taskFiles.stream()
                .map(task -> TaskFileDTO.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .uploadDate(task.getUploadDate())
                        .assignedTo(task.getAssignedTo() != null ? userMap.get(task.getAssignedTo()) : null)
                        .uploadedByName(userMap.getOrDefault(uploadedById, new UserSummary(uploadedById, "Unknown")).getName())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ✅ 10. Delete Task (and its associated file)
    @DeleteMapping("/{taskId}")
    public ResponseEntity<String> deleteTask(@PathVariable Long taskId,
                                           @RequestParam Long companyId) {
        Optional<TaskFile> optionalTask = taskFileDao.findById(taskId)
                .filter(t -> Objects.equals(t.getCompanyId(), companyId));

        if (optionalTask.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task not found for the given company.");
        }

        TaskFile task = optionalTask.get();

        try {
            Path path = Paths.get(task.getFilePath());
            Files.deleteIfExists(path); // Delete the actual file from storage

            taskrepo.delete(task); // Delete the task record from the database
            return ResponseEntity.ok("Task deleted successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete task file: " + e.getMessage());
        }
    }

    // ✅ 11. Admin: Get All Tasks related to Admin's managed users (including admin's own tasks)
    @GetMapping("/admin-all")
    public ResponseEntity<List<TaskFileDTO>> getAllTasksForAdmin(@RequestParam Long adminId,
                                                                 @RequestParam Long companyId) {
        // Step 1: Get all users under this admin (assuming 'adminId' is the ID of an admin user)
        List<User> usersUnderAdmin = userRepository.findByAdmin_UserId(adminId);
        Set<Long> userIdsUnderAdmin = usersUnderAdmin.stream()
                .map(User::getUserId)
                .collect(Collectors.toSet());

        // Step 2: Add the admin himself to the set of managed users
        userIdsUnderAdmin.add(adminId);

        // Step 3: Fetch all tasks for the company and then filter by uploadedBy OR assignedTo users
        // Using a Map to prevent duplicates if a task is both uploaded by and assigned to a managed user
        List<TaskFile> allTasks = taskrepo.findByCompanyIdOrderByUploadDateDesc(companyId).stream()
                .filter(task ->
                        (task.getUploadedBy() != null && userIdsUnderAdmin.contains(task.getUploadedBy().getUserId())) ||
                        (task.getAssignedTo() != null && userIdsUnderAdmin.contains(task.getAssignedTo()))
                )
                .collect(Collectors.toMap(
                        TaskFile::getId,
                        task -> task,
                        (existing, replacement) -> existing, // Merge function to handle duplicates (keep existing)
                        LinkedHashMap::new // Preserve order (if source stream was sorted)
                ))
                .values()
                .stream()
                .collect(Collectors.toList());


        // Step 4: Fetch all needed user data (for uploadedBy and assignedTo) in one batch DB call
        Set<Long> allRelatedUserIds = new HashSet<>();
        for (TaskFile task : allTasks) {
            if (task.getUploadedBy() != null) allRelatedUserIds.add(task.getUploadedBy().getUserId());
            if (task.getAssignedTo() != null) allRelatedUserIds.add(task.getAssignedTo());
        }

        Map<Long, UserSummary> userMap = userRepository.findAllById(allRelatedUserIds).stream()
                .collect(Collectors.toMap(
                        User::getUserId,
                        user -> new UserSummary(user.getUserId(), user.getName())
                ));

        // Step 5: Build DTO list
        List<TaskFileDTO> dtos = allTasks.stream()
                .map(task -> TaskFileDTO.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .uploadDate(task.getUploadDate())
                        .assignedTo(task.getAssignedTo() != null ? userMap.get(task.getAssignedTo()) : null)
                        .uploadedByName(
                                task.getUploadedBy() != null && userMap.containsKey(task.getUploadedBy().getUserId())
                                        ? userMap.get(task.getUploadedBy().getUserId()).getName()
                                        : "Unknown" // Fallback
                        )
                        .build()
                )
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

}