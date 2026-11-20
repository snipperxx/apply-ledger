package io.github.snipperxx.applyledger.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Body for creating and editing an application. {@code status} is only read on create (WISHLIST or APPLIED,
 * default APPLIED); afterwards the status changes through {@code POST /api/applications/{id}/status}.
 */
public record ApplicationRequest(
        @NotNull Long companyId,
        @NotBlank @Size(max = 200) String roleTitle,
        @Size(max = 1000) @Pattern(regexp = "^$|^https?://\\S+$", message = "must be an http(s) URL") String jobUrl,
        @Size(max = 200) String location,
        WorkMode workMode,
        @Size(max = 100) String source,
        ApplicationStatus status,
        LocalDate appliedOn,
        @Size(max = 300) String nextAction,
        LocalDate nextActionOn,
        @Size(max = 10000) String notes) {
}
