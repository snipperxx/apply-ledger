package io.github.snipperxx.applyledger.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Move an application to {@code status}. {@code changedOn} defaults to today and may be in the past. */
public record StatusChangeRequest(@NotNull ApplicationStatus status, LocalDate changedOn,
                                  @Size(max = 1000) String note) {
}
