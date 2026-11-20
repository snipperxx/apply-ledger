package io.github.snipperxx.applyledger.application;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filters for the application list. All parts are optional and combined with AND.
 *
 * @param statuses  any of these statuses
 * @param companyId only this company
 * @param open      true = still in progress, false = finished
 * @param text      case-insensitive match on role title, company name, location or source
 */
public record ApplicationSearch(Set<ApplicationStatus> statuses, Long companyId, Boolean open, String text) {

    /** Most recent application date first; wishlist entries (no date) last. */
    public static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("appliedOn").nullsLast(), Sort.Order.desc("id"));

    public Specification<JobApplication> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            if (companyId != null) {
                predicates.add(cb.equal(root.get("company").get("id"), companyId));
            }
            if (open != null) {
                var openPredicate = root.get("status").in(ApplicationStatus.openStatuses());
                predicates.add(open ? openPredicate : cb.not(openPredicate));
            }
            if (text != null && !text.isBlank()) {
                String pattern = "%" + escapeLike(text.strip().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("roleTitle")), pattern, '\\'),
                        cb.like(cb.lower(root.get("company").get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("location")), pattern, '\\'),
                        cb.like(cb.lower(root.get("source")), pattern, '\\')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** So that "100%" or "c_sharp" are searched literally. */
    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
