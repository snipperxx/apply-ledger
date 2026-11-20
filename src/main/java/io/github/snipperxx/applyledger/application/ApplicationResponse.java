package io.github.snipperxx.applyledger.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record ApplicationResponse(
        long id,
        long companyId,
        String companyName,
        String roleTitle,
        String jobUrl,
        String location,
        WorkMode workMode,
        String source,
        ApplicationStatus status,
        Set<ApplicationStatus> allowedNextStatuses,
        LocalDate appliedOn,
        String nextAction,
        LocalDate nextActionOn,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    /** Needs the company loaded (or fetched in the same transaction). */
    public static ApplicationResponse of(JobApplication a) {
        return new ApplicationResponse(a.getId(), a.getCompany().getId(), a.getCompany().getName(), a.getRoleTitle(),
                a.getJobUrl(), a.getLocation(), a.getWorkMode(), a.getSource(), a.getStatus(),
                a.getStatus().allowedNext(), a.getAppliedOn(), a.getNextAction(), a.getNextActionOn(), a.getNotes(),
                a.getCreatedAt(), a.getUpdatedAt(), a.getVersion());
    }
}
