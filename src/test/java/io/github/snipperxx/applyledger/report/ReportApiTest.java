package io.github.snipperxx.applyledger.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.snipperxx.applyledger.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** "Today" is 2026-11-20 (see FixedClockConfiguration). */
class ReportApiTest extends IntegrationTest {

    private long companyId;

    @BeforeEach
    void company() throws Exception {
        companyId = createCompany("Example Software GmbH");
    }

    @Test
    void followUpsListDueActionsAndStaleApplications() throws Exception {
        long overdue = createApplication(companyId, "Overdue",
                "\"nextAction\": \"Send portfolio\", \"nextActionOn\": \"2026-11-18\"");
        long soon = createApplication(companyId, "Soon", "\"nextAction\": \"Call back\", \"nextActionOn\": \"2026-11-25\"");
        createApplication(companyId, "Later", "\"nextAction\": \"Check status\", \"nextActionOn\": \"2026-12-15\"");
        long stale = createApplication(companyId, "Quiet", "\"appliedOn\": \"2026-11-01\"");
        createApplication(companyId, "Recent", "\"appliedOn\": \"2026-11-15\"");
        long closed = createApplication(companyId, "Closed",
                "\"appliedOn\": \"2026-10-01\", \"nextAction\": \"x\", \"nextActionOn\": \"2026-11-10\"");
        changeStatus(closed, "REJECTED", null).andExpect(status().isOk());

        getJson("/api/follow-ups")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today").value("2026-11-20"))
                .andExpect(jsonPath("$.due", hasSize(2)))
                .andExpect(jsonPath("$.due[0].applicationId").value(overdue))
                .andExpect(jsonPath("$.due[0].daysUntil").value(-2))
                .andExpect(jsonPath("$.due[1].applicationId").value(soon))
                .andExpect(jsonPath("$.due[1].daysUntil").value(5))
                .andExpect(jsonPath("$.stale", hasSize(1)))
                .andExpect(jsonPath("$.stale[0].applicationId").value(stale))
                .andExpect(jsonPath("$.stale[0].daysSinceApplied").value(19));

        getJson("/api/follow-ups?within=30").andExpect(jsonPath("$.due", hasSize(3)));
        getJson("/api/follow-ups?within=-1").andExpect(status().isBadRequest());
    }

    @Test
    void statsCountStatusesAndTheFunnel() throws Exception {
        long a = createApplication(companyId, "A", "\"appliedOn\": \"2026-11-01\"");
        long b = createApplication(companyId, "B", "\"appliedOn\": \"2026-11-02\"");
        long c = createApplication(companyId, "C", "\"appliedOn\": \"2026-11-03\"");
        createApplication(companyId, "D", "\"appliedOn\": \"2026-10-01\"");
        createApplication(companyId, "E", "\"status\": \"WISHLIST\"");
        changeStatus(a, "SCREENING", "2026-11-05").andExpect(status().isOk());
        changeStatus(a, "INTERVIEWING", "2026-11-10").andExpect(status().isOk());
        changeStatus(a, "OFFER", "2026-11-19").andExpect(status().isOk());
        changeStatus(b, "REJECTED", "2026-11-12").andExpect(status().isOk());
        changeStatus(c, "WITHDRAWN", "2026-11-04").andExpect(status().isOk());

        getJson("/api/stats")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.open").value(3))
                .andExpect(jsonPath("$.byStatus.OFFER").value(1))
                .andExpect(jsonPath("$.byStatus.ACCEPTED").value(0))
                .andExpect(jsonPath("$.funnel.applied").value(4))
                .andExpect(jsonPath("$.funnel.responded").value(2))
                .andExpect(jsonPath("$.funnel.interviewed").value(1))
                .andExpect(jsonPath("$.funnel.offers").value(1))
                .andExpect(jsonPath("$.funnel.responseRate").value(0.5))
                // a answered after 4 days (screening), b after 10 days (rejection)
                .andExpect(jsonPath("$.funnel.medianDaysToFirstResponse").value(7.0));

        getJson("/api/stats?from=2026-11-01&to=2026-11-30")
                .andExpect(jsonPath("$.funnel.applied").value(3))
                .andExpect(jsonPath("$.funnel.responseRate").value(0.67));

        getJson("/api/stats?from=2026-12-01&to=2026-11-01").andExpect(status().isBadRequest());
    }

    @Test
    void emptyStatsHaveNoRates() throws Exception {
        getJson("/api/stats")
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.funnel.responseRate").doesNotExist())
                .andExpect(jsonPath("$.funnel.medianDaysToFirstResponse").doesNotExist());
    }

    @Test
    void exportsCsvNewestFirstWithFilters() throws Exception {
        createApplication(companyId, "Older", "\"appliedOn\": \"2026-11-01\", \"location\": \"Karlsruhe, DE\"");
        createApplication(companyId, "Newer", "\"appliedOn\": \"2026-11-10\", \"notes\": \"=1+1\"");
        createApplication(companyId, "Someday", "\"status\": \"WISHLIST\"");

        String csv = mvc.perform(get("/api/applications/export.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"applications.csv\""))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv.split("\r\n")).containsExactly(
                "id,company,role,status,applied_on,location,work_mode,source,job_url,next_action,next_action_on,notes",
                "2,Example Software GmbH,Newer,APPLIED,2026-11-10,,,,,,,'=1+1",
                "1,Example Software GmbH,Older,APPLIED,2026-11-01,\"Karlsruhe, DE\",,,,,,",
                "3,Example Software GmbH,Someday,WISHLIST,,,,,,,,");

        String filtered = mvc.perform(get("/api/applications/export.csv?status=WISHLIST"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(filtered.split("\r\n")).hasSize(2);
    }
}
