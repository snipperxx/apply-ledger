package io.github.snipperxx.applyledger.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) @Pattern(regexp = "^$|^https?://\\S+$", message = "must be an http(s) URL") String website,
        @Size(max = 5000) String notes) {
}
