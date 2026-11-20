package io.github.snipperxx.applyledger.report;

import io.github.snipperxx.applyledger.application.ApplicationSearch;
import io.github.snipperxx.applyledger.application.ApplicationStatus;
import io.github.snipperxx.applyledger.application.JobApplication;
import io.github.snipperxx.applyledger.application.JobApplicationRepository;
import io.github.snipperxx.applyledger.common.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ReportController {

    private static final List<String> CSV_HEADER = List.of("id", "company", "role", "status", "applied_on",
            "location", "work_mode", "source", "job_url", "next_action", "next_action_on", "notes");

    private final FollowUpService followUps;
    private final StatsService stats;
    private final JobApplicationRepository applications;

    ReportController(FollowUpService followUps, StatsService stats, JobApplicationRepository applications) {
        this.followUps = followUps;
        this.stats = stats;
        this.applications = applications;
    }

    /** Next actions due within {@code within} days (overdue ones included) and stale applications. */
    @GetMapping("/api/follow-ups")
    FollowUpService.FollowUps followUps(@RequestParam(defaultValue = "7") int within) {
        if (within < 0 || within > 365) {
            throw new BadRequestException("within must be between 0 and 365");
        }
        return followUps.followUps(within);
    }

    @GetMapping("/api/stats")
    StatsService.Stats stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return stats.stats(from, to);
    }

    /** Same filters as the list endpoint; returns every match, newest application first. */
    @GetMapping("/api/applications/export.csv")
    @Transactional(readOnly = true)
    ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) Set<ApplicationStatus> status,
                                     @RequestParam(required = false) Long companyId,
                                     @RequestParam(required = false) Boolean open,
                                     @RequestParam(required = false) String q) {
        var spec = new ApplicationSearch(status, companyId, open, q).toSpecification();
        StringBuilder csv = new StringBuilder(Csv.row(CSV_HEADER));
        for (JobApplication a : applications.findAll(spec, ApplicationSearch.NEWEST_FIRST)) {
            csv.append(Csv.row(Arrays.asList(a.getId(), a.getCompany().getName(), a.getRoleTitle(), a.getStatus(),
                    a.getAppliedOn(), a.getLocation(), a.getWorkMode(), a.getSource(), a.getJobUrl(),
                    a.getNextAction(), a.getNextActionOn(), a.getNotes())));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"applications.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
}
