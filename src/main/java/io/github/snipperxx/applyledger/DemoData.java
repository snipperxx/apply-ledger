package io.github.snipperxx.applyledger;

import io.github.snipperxx.applyledger.application.ApplicationRequest;
import io.github.snipperxx.applyledger.application.ApplicationService;
import io.github.snipperxx.applyledger.application.ApplicationStatus;
import io.github.snipperxx.applyledger.application.StatusChangeRequest;
import io.github.snipperxx.applyledger.application.WorkMode;
import io.github.snipperxx.applyledger.company.CompanyRequest;
import io.github.snipperxx.applyledger.company.CompanyService;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Made-up sample data for trying the API ({@code --spring.profiles.active=demo}). Only runs on an empty database.
 * All companies and roles are fictional.
 */
@Component
@Profile("demo")
class DemoData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoData.class);

    private final CompanyService companies;
    private final ApplicationService applications;
    private final Clock clock;

    DemoData(CompanyService companies, ApplicationService applications, Clock clock) {
        this.companies = companies;
        this.applications = applications;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!companies.list().isEmpty()) {
            log.info("Database is not empty, skipping demo data");
            return;
        }
        LocalDate today = LocalDate.now(clock);

        long example = company("Example Software GmbH", "https://example.com");
        long northwind = company("Northwind Analytics", "https://northwind.example");
        long contoso = company("Contoso Cloud", null);
        long fabrikam = company("Fabrikam Logistics", "https://fabrikam.example");

        long a1 = apply(example, "Backend Developer (Java)", "Karlsruhe", WorkMode.HYBRID, "company website",
                today.minusDays(24), "Send thank-you note", today.minusDays(1));
        move(a1, ApplicationStatus.SCREENING, today.minusDays(18), "30 min call with HR");
        move(a1, ApplicationStatus.INTERVIEWING, today.minusDays(9), "Technical interview, live coding");

        long a2 = apply(northwind, "Python Developer", null, WorkMode.REMOTE, "job board", today.minusDays(30),
                null, null);
        move(a2, ApplicationStatus.REJECTED, today.minusDays(12), "Standard rejection e-mail");

        apply(contoso, "Platform Engineer", "Berlin", WorkMode.ONSITE, "referral", today.minusDays(20), null, null);

        long a4 = apply(fabrikam, "Software Engineer, Data", "Frankfurt", WorkMode.HYBRID, "job board",
                today.minusDays(40), null, null);
        move(a4, ApplicationStatus.INTERVIEWING, today.minusDays(33), null);
        move(a4, ApplicationStatus.OFFER, today.minusDays(5), "Written offer received");

        applications.create(new ApplicationRequest(northwind, "Data Engineer", "https://northwind.example/jobs/42",
                null, WorkMode.REMOTE, null, ApplicationStatus.WISHLIST, null, "Tailor CV and apply",
                today.plusDays(3), null));

        log.info("Loaded demo data: 4 companies, 5 applications");
    }

    private long company(String name, String website) {
        return companies.create(new CompanyRequest(name, website, null)).id();
    }

    private long apply(long companyId, String role, String location, WorkMode mode, String source,
                       LocalDate appliedOn, String nextAction, LocalDate nextActionOn) {
        return applications.create(new ApplicationRequest(companyId, role, null, location, mode, source,
                ApplicationStatus.APPLIED, appliedOn, nextAction, nextActionOn, null)).id();
    }

    private void move(long id, ApplicationStatus status, LocalDate on, String note) {
        applications.changeStatus(id, new StatusChangeRequest(status, on, note));
    }
}
