package io.github.snipperxx.applyledger.company;

import java.time.Instant;

public record CompanyResponse(long id, String name, String website, String notes, long applications,
                              Instant createdAt) {

    static CompanyResponse of(Company company, long applications) {
        return new CompanyResponse(company.getId(), company.getName(), company.getWebsite(), company.getNotes(),
                applications, company.getCreatedAt());
    }
}
