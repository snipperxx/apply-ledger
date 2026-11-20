package io.github.snipperxx.applyledger.application;

import io.github.snipperxx.applyledger.common.BadRequestException;
import io.github.snipperxx.applyledger.common.ConflictException;
import io.github.snipperxx.applyledger.common.NotFoundException;
import io.github.snipperxx.applyledger.common.PageResponse;
import io.github.snipperxx.applyledger.company.Company;
import io.github.snipperxx.applyledger.company.CompanyService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApplicationService {

    private final JobApplicationRepository applications;
    private final StatusChangeRepository history;
    private final CompanyService companies;
    private final Clock clock;

    ApplicationService(JobApplicationRepository applications, StatusChangeRepository history,
                       CompanyService companies, Clock clock) {
        this.applications = applications;
        this.history = history;
        this.companies = companies;
        this.clock = clock;
    }

    public ApplicationResponse create(ApplicationRequest request) {
        ApplicationStatus status = request.status() == null ? ApplicationStatus.APPLIED : request.status();
        if (!status.isValidStart()) {
            throw new BadRequestException("A new application starts as WISHLIST or APPLIED, not " + status
                    + "; record later steps with a status change");
        }
        LocalDate today = today();
        LocalDate appliedOn = request.appliedOn();
        if (status == ApplicationStatus.APPLIED && appliedOn == null) {
            appliedOn = today;
        }
        Company company = companies.find(request.companyId());
        Instant now = clock.instant();

        JobApplication application = new JobApplication(company, status, now);
        applyEdits(application, company, request, appliedOn, now);
        applications.save(application);

        LocalDate startedOn = status == ApplicationStatus.APPLIED ? appliedOn : today;
        history.save(new StatusChange(application, null, status, startedOn, null, now));
        return ApplicationResponse.of(application);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> search(ApplicationSearch search, Pageable pageable) {
        return PageResponse.of(applications.findAll(search.toSpecification(), pageable), ApplicationResponse::of);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(long id) {
        return ApplicationResponse.of(find(id));
    }

    public ApplicationResponse update(long id, ApplicationRequest request) {
        JobApplication application = find(id);
        if (request.status() != null && request.status() != application.getStatus()) {
            throw new BadRequestException("Status cannot be edited directly; use POST /api/applications/" + id
                    + "/status");
        }
        Company company = request.companyId().equals(application.getCompany().getId())
                ? application.getCompany()
                : companies.find(request.companyId());
        applyEdits(application, company, request, request.appliedOn(), clock.instant());
        // Flush so the response carries the incremented version.
        return ApplicationResponse.of(applications.saveAndFlush(application));
    }

    public ApplicationResponse changeStatus(long id, StatusChangeRequest request) {
        JobApplication application = find(id);
        ApplicationStatus from = application.getStatus();
        ApplicationStatus to = request.status();
        if (from == to) {
            throw new ConflictException("Application " + id + " is already " + to);
        }
        if (!from.canBecome(to)) {
            throw new ConflictException("Cannot move from " + from + " to " + to + "; allowed: " + from.allowedNext());
        }

        LocalDate changedOn = request.changedOn() == null ? today() : request.changedOn();
        notInFuture(changedOn, "changedOn");
        history.findFirstByApplicationIdOrderByIdDesc(id)
                .filter(last -> changedOn.isBefore(last.getChangedOn()))
                .ifPresent(last -> {
                    throw new BadRequestException("changedOn " + changedOn + " is before the previous step ("
                            + last.getToStatus() + " on " + last.getChangedOn() + ")");
                });

        Instant now = clock.instant();
        application.moveTo(to, changedOn, now);
        history.save(new StatusChange(application, from, to, changedOn, blankToNull(request.note()), now));
        return ApplicationResponse.of(applications.saveAndFlush(application));
    }

    @Transactional(readOnly = true)
    public List<StatusChangeResponse> history(long id) {
        if (!applications.existsById(id)) {
            throw new NotFoundException("Application", id);
        }
        return history.findByApplicationIdOrderByIdAsc(id).stream().map(StatusChangeResponse::of).toList();
    }

    public void delete(long id) {
        applications.delete(find(id));
    }

    private JobApplication find(long id) {
        return applications.findWithCompanyById(id).orElseThrow(() -> new NotFoundException("Application", id));
    }

    private void applyEdits(JobApplication application, Company company, ApplicationRequest request,
                            LocalDate appliedOn, Instant now) {
        if (appliedOn != null) {
            notInFuture(appliedOn, "appliedOn");
            if (application.getStatus() == ApplicationStatus.WISHLIST) {
                throw new BadRequestException("appliedOn must be empty while the application is on the wishlist");
            }
        }
        String nextAction = blankToNull(request.nextAction());
        if ((nextAction != null || request.nextActionOn() != null) && !application.getStatus().isOpen()) {
            throw new BadRequestException("A " + application.getStatus() + " application has no next action");
        }
        application.edit(company, request.roleTitle().strip(), blankToNull(request.jobUrl()),
                blankToNull(request.location()), request.workMode(), blankToNull(request.source()), appliedOn,
                nextAction, request.nextActionOn(), blankToNull(request.notes()), now);
    }

    private void notInFuture(LocalDate date, String field) {
        if (date.isAfter(today())) {
            throw new BadRequestException(field + " " + date + " is in the future");
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
