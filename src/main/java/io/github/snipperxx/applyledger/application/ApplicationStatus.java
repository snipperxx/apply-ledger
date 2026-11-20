package io.github.snipperxx.applyledger.application;

import java.util.EnumSet;
import java.util.Set;

/**
 * Where an application stands. The allowed moves are kept here in one place; the API returns them so a client
 * can offer only valid next steps.
 *
 * <pre>
 * WISHLIST -> APPLIED -> SCREENING -> INTERVIEWING -> OFFER -> ACCEPTED
 *                  \           \            \           \-> DECLINED
 *                   \-----------\------------\-----------\-> REJECTED (offer can be withdrawn by the company)
 * any open status -> WITHDRAWN (I stopped the process)
 * </pre>
 *
 * APPLIED may jump straight to INTERVIEWING, because many companies skip a separate screening call.
 */
public enum ApplicationStatus {
    WISHLIST,
    APPLIED,
    SCREENING,
    INTERVIEWING,
    OFFER,
    ACCEPTED,
    DECLINED,
    REJECTED,
    WITHDRAWN;

    public Set<ApplicationStatus> allowedNext() {
        return switch (this) {
            case WISHLIST -> EnumSet.of(APPLIED, WITHDRAWN);
            case APPLIED -> EnumSet.of(SCREENING, INTERVIEWING, REJECTED, WITHDRAWN);
            case SCREENING -> EnumSet.of(INTERVIEWING, REJECTED, WITHDRAWN);
            case INTERVIEWING -> EnumSet.of(OFFER, REJECTED, WITHDRAWN);
            case OFFER -> EnumSet.of(ACCEPTED, DECLINED, REJECTED);
            case ACCEPTED, DECLINED, REJECTED, WITHDRAWN -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }

    public boolean canBecome(ApplicationStatus next) {
        return allowedNext().contains(next);
    }

    /** Still in progress, i.e. not a final outcome. */
    public boolean isOpen() {
        return !allowedNext().isEmpty();
    }

    /** A status that can only be reached because the company answered (used for the response rate). */
    public boolean isCompanyResponse() {
        return this == SCREENING || this == INTERVIEWING || this == OFFER || this == REJECTED;
    }

    /** Statuses a new application may start in. Later stages are reached through status changes. */
    public boolean isValidStart() {
        return this == WISHLIST || this == APPLIED;
    }

    public static Set<ApplicationStatus> openStatuses() {
        EnumSet<ApplicationStatus> open = EnumSet.noneOf(ApplicationStatus.class);
        for (ApplicationStatus status : values()) {
            if (status.isOpen()) {
                open.add(status);
            }
        }
        return open;
    }
}
