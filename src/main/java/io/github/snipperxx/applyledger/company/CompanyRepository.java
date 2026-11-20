package io.github.snipperxx.applyledger.company;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByNameIgnoreCase(String name);

    List<Company> findAllByOrderByNameAsc();

    /** Company id with its number of applications, for the list view. */
    @Query("""
            select a.company.id as companyId, count(a) as applications
            from JobApplication a
            group by a.company.id
            """)
    List<ApplicationCount> countApplicationsPerCompany();

    interface ApplicationCount {
        Long getCompanyId();

        long getApplications();
    }
}
