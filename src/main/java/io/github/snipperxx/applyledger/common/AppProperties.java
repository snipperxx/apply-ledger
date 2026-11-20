package io.github.snipperxx.applyledger.common;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings under {@code app.*}. */
@ConfigurationProperties("app")
public record AppProperties(ZoneId zone) {

    public AppProperties {
        if (zone == null) {
            zone = ZoneId.of("Europe/Berlin");
        }
    }
}
