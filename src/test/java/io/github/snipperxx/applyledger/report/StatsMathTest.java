package io.github.snipperxx.applyledger.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatsMathTest {

    @Test
    void medianOfOddAndEvenCounts() {
        assertThat(StatsService.median(List.of(9L, 1L, 4L))).isEqualTo(4.0);
        assertThat(StatsService.median(List.of(3L, 1L, 4L, 10L))).isEqualTo(3.5);
        assertThat(StatsService.median(List.of())).isNull();
    }

    @Test
    void ratioIsRoundedAndNullWithoutData() {
        assertThat(StatsService.ratio(1, 3)).isEqualByComparingTo(new BigDecimal("0.33"));
        assertThat(StatsService.ratio(2, 3)).isEqualByComparingTo(new BigDecimal("0.67"));
        assertThat(StatsService.ratio(0, 0)).isNull();
    }
}
