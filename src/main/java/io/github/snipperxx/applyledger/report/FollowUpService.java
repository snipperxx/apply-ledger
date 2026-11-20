package io.github.snipperxx.applyledger.report;

import io.github.snipperxx.applyledger.application.ApplicationStatus;
import io.github.snipperxx.applyledger.application.JobApplication;
import io.github.snipperxx.applyledger.application.JobApplicationRepository;
import io.github.snipperxx.applyledger.common.AppProperties;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What needs my attention: planned next actions that are due, and applications that went quiet. */
@Service
@Transactional(readOnly = true)
public class FollowUpService {

    private final JobApplicationRepository applications;
    private final Clock clock;
    private final int staleAfterDays;

    FollowUpService(JobApplicationRepository applications, Clock clock, AppProperties properties) {
        this.applications = applications;
        this.clock = clock;
        this.staleAfterDays = properties.staleAfterDays();
    }

    public FollowUps followUps(int withinDays) {
        LocalDate today = LocalDate.now(clock);

        Specification<JobApplication> due = (root, query, cb) -> cb.and(
                root.get("status").in(ApplicationStatus.openStatuses()),
                cb.lessThanOrEqualTo(root.get("nextActionOn"), today.plusDays(withinDays)));
        List<FollowUp> dueItems = applications.findAll(due, Sort.by("nextActionOn", "id")).stream()
                .map(a -> FollowUp.of(a, today, ChronoUnit.DAYS.between(today, a.getNextActionOn())))
                .toList();

        // Applied a while ago, no answer yet and nothing planned: probably worth a polite nudge (or closing it).
        Specification<JobApplication> stale = (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), ApplicationStatus.APPLIED),
                cb.isNull(root.get("nextActionOn")),
                cb.lessThanOrEqualTo(root.get("appliedOn"), today.minusDays(staleAfterDays)));
        List<FollowUp> staleItems = applications.findAll(stale, Sort.by("appliedOn", "id")).stream()
                .map(a -> FollowUp.of(a, today, null))
                .toList();

        return new FollowUps(today, staleAfterDays, dueItems, staleItems);
    }

    public record FollowUps(LocalDate today, int staleAfterDays, List<FollowUp> due, List<FollowUp> stale) {
    }

    /**
     * @param daysUntil days until the next action (negative = overdue); null for stale entries
     * @param daysSinceApplied days since the application was sent, if it was
     */
    public record FollowUp(long applicationId, String companyName, String roleTitle, ApplicationStatus status,
                           String nextAction, LocalDate nextActionOn, Long daysUntil, Long daysSinceApplied) {

        static FollowUp of(JobApplication a, LocalDate today, Long daysUntil) {
            Long sinceApplied = a.getAppliedOn() == null ? null : ChronoUnit.DAYS.between(a.getAppliedOn(), today);
            return new FollowUp(a.getId(), a.getCompany().getName(), a.getRoleTitle(), a.getStatus(),
                    a.getNextAction(), a.getNextActionOn(), daysUntil, sinceApplied);
        }
    }
}
