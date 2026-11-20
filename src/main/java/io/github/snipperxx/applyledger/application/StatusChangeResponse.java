package io.github.snipperxx.applyledger.application;

import java.time.Instant;
import java.time.LocalDate;

public record StatusChangeResponse(long id, ApplicationStatus fromStatus, ApplicationStatus toStatus,
                                   LocalDate changedOn, String note, Instant recordedAt) {

    static StatusChangeResponse of(StatusChange change) {
        return new StatusChangeResponse(change.getId(), change.getFromStatus(), change.getToStatus(),
                change.getChangedOn(), change.getNote(), change.getRecordedAt());
    }
}
