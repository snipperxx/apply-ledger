package io.github.snipperxx.applyledger.application;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long>,
        JpaSpecificationExecutor<JobApplication> {

    long countByCompanyId(long companyId);

    @EntityGraph(attributePaths = "company")
    Optional<JobApplication> findWithCompanyById(long id);

    /** Same as the inherited method, but loads the company in the same query (no N+1 in list views). */
    @Override
    @EntityGraph(attributePaths = "company")
    Page<JobApplication> findAll(Specification<JobApplication> spec, Pageable pageable);
}
