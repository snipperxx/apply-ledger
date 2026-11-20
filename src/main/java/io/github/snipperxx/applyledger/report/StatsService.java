package io.github.snipperxx.applyledger.report;

import io.github.snipperxx.applyledger.application.ApplicationStatus;
import io.github.snipperxx.applyledger.application.JobApplicationRepository;
import io.github.snipperxx.applyledger.common.BadRequestException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final JobApplicationRepository applications;
    private final EntityManager entityManager;
    private final Clock clock;

    StatsService(JobApplicationRepository applications, EntityManager entityManager, Clock clock) {
        this.applications = applications;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    /**
     * Current status counts over everything, plus funnel numbers for the applications sent between
     * {@code from} and {@code to} (both inclusive; default: all time up to today).
     */
    public Stats stats(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now(clock) : to;
        LocalDate start = from == null ? LocalDate.of(1970, 1, 1) : from;
        if (start.isAfter(end)) {
            throw new BadRequestException("from " + start + " is after to " + end);
        }

        Map<ApplicationStatus, Long> byStatus = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            byStatus.put(status, 0L);
        }
        applications.countByStatus().forEach(c -> byStatus.put(c.getStatus(), c.getCount()));
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long open = ApplicationStatus.openStatuses().stream().mapToLong(byStatus::get).sum();

        return new Stats(total, open, byStatus, funnel(start, end));
    }

    private Funnel funnel(LocalDate from, LocalDate to) {
        List<Object[]> rows = entityManager.createQuery("""
                        select a.id, a.appliedOn, sc.toStatus, sc.changedOn
                        from StatusChange sc join sc.application a
                        where a.appliedOn between :from and :to
                        order by sc.id
                        """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        Set<Long> applied = new HashSet<>();
        Set<Long> interviewed = new HashSet<>();
        Set<Long> offers = new HashSet<>();
        Map<Long, Long> daysToFirstResponse = new HashMap<>();
        for (Object[] row : rows) {
            long id = (Long) row[0];
            LocalDate appliedOn = (LocalDate) row[1];
            ApplicationStatus reached = (ApplicationStatus) row[2];
            LocalDate changedOn = (LocalDate) row[3];
            applied.add(id);
            if (reached.isCompanyResponse()) {
                daysToFirstResponse.putIfAbsent(id, ChronoUnit.DAYS.between(appliedOn, changedOn));
            }
            if (reached == ApplicationStatus.INTERVIEWING) {
                interviewed.add(id);
            }
            if (reached == ApplicationStatus.OFFER) {
                offers.add(id);
            }
        }
        return new Funnel(from, to, applied.size(), daysToFirstResponse.size(), interviewed.size(), offers.size(),
                ratio(daysToFirstResponse.size(), applied.size()), median(daysToFirstResponse.values()));
    }

    static BigDecimal ratio(long part, long whole) {
        return whole == 0 ? null : BigDecimal.valueOf(part).divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }

    static Double median(Iterable<Long> values) {
        List<Long> sorted = new ArrayList<>();
        values.forEach(sorted::add);
        if (sorted.isEmpty()) {
            return null;
        }
        sorted.sort(null);
        int mid = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(mid) : (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
    }

    public record Stats(long total, long open, Map<ApplicationStatus, Long> byStatus, Funnel funnel) {
    }

    /**
     * @param applied                   applications sent in the period
     * @param responded                 of those, how many got any answer (screening, interview, offer or rejection)
     * @param responseRate              responded / applied, 2 decimals; null when nothing was sent
     * @param medianDaysToFirstResponse null when nobody answered yet
     */
    public record Funnel(LocalDate from, LocalDate to, long applied, long responded, long interviewed, long offers,
                         BigDecimal responseRate, Double medianDaysToFirstResponse) {
    }
}
