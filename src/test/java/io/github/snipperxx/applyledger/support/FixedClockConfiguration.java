package io.github.snipperxx.applyledger.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Tests run on a fixed "today" so date rules (no future dates, follow-ups due) are deterministic. */
@TestConfiguration(proxyBeanMethods = false)
public class FixedClockConfiguration {

    public static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    public static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-11-20T09:00:00Z"), ZONE);
    }
}
