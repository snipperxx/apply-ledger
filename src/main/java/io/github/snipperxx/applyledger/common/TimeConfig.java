package io.github.snipperxx.applyledger.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** One injectable clock, so "today" can be fixed in tests. */
@Configuration(proxyBeanMethods = false)
class TimeConfig {

    @Bean
    Clock clock(AppProperties properties) {
        return Clock.system(properties.zone());
    }
}
