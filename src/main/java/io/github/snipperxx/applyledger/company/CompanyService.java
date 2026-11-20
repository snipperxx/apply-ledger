package io.github.snipperxx.applyledger.company;

import io.github.snipperxx.applyledger.application.JobApplicationRepository;
import io.github.snipperxx.applyledger.common.ConflictException;
import io.github.snipperxx.applyledger.common.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companies;
    private final JobApplicationRepository applications;
    private final Clock clock;

    CompanyService(CompanyRepository companies, JobApplicationRepository applications, Clock clock) {
        this.companies = companies;
        this.applications = applications;
        this.clock = clock;
    }

    public CompanyResponse create(CompanyRequest request) {
        String name = request.name().strip();
        ensureNameFree(name, null);
        Company company = companies.save(new Company(name, blankToNull(request.website()),
                blankToNull(request.notes()), clock.instant()));
        return CompanyResponse.of(company, 0);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> list() {
        Map<Long, Long> counts = companies.countApplicationsPerCompany().stream()
                .collect(Collectors.toMap(CompanyRepository.ApplicationCount::getCompanyId,
                        CompanyRepository.ApplicationCount::getApplications));
        return companies.findAllByOrderByNameAsc().stream()
                .map(c -> CompanyResponse.of(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyResponse get(long id) {
        Company company = find(id);
        return CompanyResponse.of(company, applications.countByCompanyId(id));
    }

    public CompanyResponse update(long id, CompanyRequest request) {
        Company company = find(id);
        String name = request.name().strip();
        ensureNameFree(name, id);
        company.update(name, blankToNull(request.website()), blankToNull(request.notes()));
        return CompanyResponse.of(company, applications.countByCompanyId(id));
    }

    /** A company that still has applications cannot be deleted; delete or move those first. */
    public void delete(long id) {
        Company company = find(id);
        long count = applications.countByCompanyId(id);
        if (count > 0) {
            throw new ConflictException("Company " + id + " still has " + count + " application(s)");
        }
        companies.delete(company);
    }

    public Company find(long id) {
        return companies.findById(id).orElseThrow(() -> new NotFoundException("Company", id));
    }

    private void ensureNameFree(String name, Long ownId) {
        companies.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(ownId))
                .ifPresent(existing -> {
                    throw new ConflictException("A company named '" + existing.getName() + "' already exists (id "
                            + existing.getId() + ")");
                });
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
