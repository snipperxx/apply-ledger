package io.github.snipperxx.applyledger.application;

import static io.github.snipperxx.applyledger.application.ApplicationStatus.ACCEPTED;
import static io.github.snipperxx.applyledger.application.ApplicationStatus.APPLIED;
import static io.github.snipperxx.applyledger.application.ApplicationStatus.DECLINED;
import static io.github.snipperxx.applyledger.application.ApplicationStatus.INTERVIEWING;
import static io.github.snipperxx.applyledger.application.ApplicationStatus.OFFER;
import static io.github.snipperxx.applyledger.application.ApplicationStatus.REJECTED;
import static io.github.snipperxx.applyledger.application.ApplicationStatus.SCREENING;
import static io.github.snipperxx.applyledger.application.ApplicationStatus.WISHLIST;
import static io.github.snipperxx.applyledger.application.ApplicationStatus.WITHDRAWN;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ApplicationStatusTest {

    @Test
    void happyPathFromWishlistToAcceptedIsAllowed() {
        assertThat(WISHLIST.canBecome(APPLIED)).isTrue();
        assertThat(APPLIED.canBecome(SCREENING)).isTrue();
        assertThat(SCREENING.canBecome(INTERVIEWING)).isTrue();
        assertThat(INTERVIEWING.canBecome(OFFER)).isTrue();
        assertThat(OFFER.canBecome(ACCEPTED)).isTrue();
    }

    @Test
    void screeningCanBeSkipped() {
        assertThat(APPLIED.canBecome(INTERVIEWING)).isTrue();
    }

    @Test
    void cannotGoBackOrSkipAhead() {
        assertThat(INTERVIEWING.canBecome(APPLIED)).isFalse();
        assertThat(APPLIED.canBecome(OFFER)).isFalse();
        assertThat(WISHLIST.canBecome(REJECTED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(names = {"ACCEPTED", "DECLINED", "REJECTED", "WITHDRAWN"})
    void finalStatusesAreClosedAndHaveNoNextStep(ApplicationStatus status) {
        assertThat(status.isOpen()).isFalse();
        assertThat(status.allowedNext()).isEmpty();
    }

    @Test
    void openStatusesAreTheOnesWithANextStep() {
        assertThat(ApplicationStatus.openStatuses())
                .isEqualTo(EnumSet.of(WISHLIST, APPLIED, SCREENING, INTERVIEWING, OFFER));
    }

    @Test
    void anOfferIsDeclinedNotWithdrawn() {
        assertThat(OFFER.canBecome(DECLINED)).isTrue();
        assertThat(OFFER.canBecome(WITHDRAWN)).isFalse();
    }

    @Test
    void onlyWishlistAndAppliedAreValidStarts() {
        assertThat(EnumSet.allOf(ApplicationStatus.class).stream().filter(ApplicationStatus::isValidStart))
                .containsExactlyInAnyOrder(WISHLIST, APPLIED);
    }
}
