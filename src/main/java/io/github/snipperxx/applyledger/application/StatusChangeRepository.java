package io.github.snipperxx.applyledger.application;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusChangeRepository extends JpaRepository<StatusChange, Long> {

    List<StatusChange> findByApplicationIdOrderByIdAsc(long applicationId);

    Optional<StatusChange> findFirstByApplicationIdOrderByIdDesc(long applicationId);
}
