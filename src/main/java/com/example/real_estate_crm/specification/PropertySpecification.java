package com.example.real_estate_crm.specification;

import com.example.real_estate_crm.dto.PropertySearchRequest;
import com.example.real_estate_crm.model.Property;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class PropertySpecification {

    public static Specification<Property> getProperties(PropertySearchRequest request) {
        return (Root<Property> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // ✅ Sabse pehle companyId ka filter
            if (request.getCompanyId() != null) {
            	predicates.add(cb.equal(root.get("company").get("id"), request.getCompanyId()));
            }

            // ✅ Type filter
            if (request.getType() != null) {
                predicates.add(cb.equal(cb.lower(root.get("type")), request.getType().toLowerCase()));
            }

            // ✅ BHK filter
            if (request.getBhk() != null) {
                predicates.add(cb.like(cb.lower(root.get("bhk")), "%" + request.getBhk().toLowerCase() + "%"));
            }

            // ✅ Size filter
            if (request.getSize() != null) {
                predicates.add(cb.equal(cb.lower(root.get("size")), request.getSize().toLowerCase()));
            }

            // ✅ Status filter
            if (request.getStatus() != null) {
                try {
                    Property.Status statusEnum = Property.Status.valueOf(request.getStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException e) {
                    // If status is not valid enum, ignore this filter
                }
            }

            // ✅ Source filter
            if (request.getSource() != null) {
                predicates.add(cb.equal(cb.lower(root.get("source")), request.getSource().toLowerCase()));
            }

            // ✅ CreatedBy filter (userId se)
            if (request.getCreatedBy() != null) {
                Join<Object, Object> createdByJoin = root.join("createdBy", JoinType.LEFT);
                predicates.add(cb.equal(createdByJoin.get("userId"), request.getCreatedBy()));
            }

            // ✅ Keyword search
            if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {
                for (String keyword : request.getKeywords()) {
                    String lowerKeyword = keyword.toLowerCase();
                    List<Predicate> perKeywordPredicates = new ArrayList<>();

                    perKeywordPredicates.add(cb.like(cb.lower(root.get("propertyName")), "%" + lowerKeyword + "%"));
                    perKeywordPredicates.add(cb.like(cb.lower(root.get("ownerName")), "%" + lowerKeyword + "%"));
                    perKeywordPredicates.add(cb.like(cb.lower(root.get("location")), "%" + lowerKeyword + "%"));
                    perKeywordPredicates.add(cb.like(cb.lower(root.get("sector")), "%" + lowerKeyword + "%"));
                    perKeywordPredicates.add(cb.like(cb.lower(root.get("unitDetails")), "%" + lowerKeyword + "%"));
                    perKeywordPredicates.add(cb.like(cb.lower(root.get("source")), "%" + lowerKeyword + "%"));

                    Join<Object, Object> createdByJoin = root.join("createdBy", JoinType.LEFT);
                    perKeywordPredicates.add(cb.like(cb.lower(createdByJoin.get("name")), "%" + lowerKeyword + "%"));

                    predicates.add(cb.or(perKeywordPredicates.toArray(new Predicate[0])));
                }
            }

            // ✅ Price filter
            if (request.getMinPrice() != null && request.getMaxPrice() != null) {
                predicates.add(cb.between(root.get("price"), request.getMinPrice(), request.getMaxPrice()));
            } else if (request.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), request.getMinPrice()));
            } else if (request.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), request.getMaxPrice()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
