package io.github.snipperxx.applyledger.common;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code app.*}.
 *
 * @param zone           time zone that decides what "today" is
 * @param staleAfterDays an APPLIED application with no answer and no planned action after this many days is
 *                       listed as stale in the follow-ups
 */
@ConfigurationProperties("app")
public record AppProperties(ZoneId zone, Integer staleAfterDays) {

    public AppProperties {
        if (zone == null) {
            zone = ZoneId.of("Europe/Berlin");
        }
        if (staleAfterDays == null) {
            staleAfterDays = 14;
        }
        if (staleAfterDays < 1) {
            throw new IllegalArgumentException("app.stale-after-days must be at least 1");
        }
    }
}
