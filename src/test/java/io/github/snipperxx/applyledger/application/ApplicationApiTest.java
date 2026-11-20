package io.github.snipperxx.applyledger.application;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.snipperxx.applyledger.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ApplicationApiTest extends IntegrationTest {

    private long companyId;

    @BeforeEach
    void company() throws Exception {
        companyId = createCompany("Example Software GmbH");
    }

    @Test
    void newApplicationDefaultsToAppliedToday() throws Exception {
        postJson("/api/applications", """
                {"companyId": %d, "roleTitle": "Backend Developer (Java)", "workMode": "HYBRID",
                 "location": "Karlsruhe", "source": "company website"}
                """.formatted(companyId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.appliedOn").value("2026-11-20"))
                .andExpect(jsonPath("$.companyName").value("Example Software GmbH"))
                .andExpect(jsonPath("$.allowedNextStatuses",
                        containsInAnyOrder("SCREENING", "INTERVIEWING", "REJECTED", "WITHDRAWN")));
    }

    @Test
    void wishlistEntriesHaveNoApplicationDate() throws Exception {
        postJson("/api/applications", """
                {"companyId": %d, "roleTitle": "Data Engineer", "status": "WISHLIST", "appliedOn": "2026-11-01"}
                """.formatted(companyId))
                .andExpect(status().isBadRequest());

        long id = createApplication(companyId, "Data Engineer", "\"status\": \"WISHLIST\"");
        getJson("/api/applications/" + id).andExpect(jsonPath("$.appliedOn").doesNotExist());
    }

    @Test
    void cannotStartInALaterStage() throws Exception {
        postJson("/api/applications", """
                {"companyId": %d, "roleTitle": "Backend Developer", "status": "OFFER"}
                """.formatted(companyId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsFutureDatesAndUnknownCompanies() throws Exception {
        postJson("/api/applications", """
                {"companyId": %d, "roleTitle": "Backend Developer", "appliedOn": "2026-11-21"}
                """.formatted(companyId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("appliedOn 2026-11-21 is in the future"));

        postJson("/api/applications", "{\"companyId\": 999, \"roleTitle\": \"Backend Developer\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    void walksThroughTheProcessAndKeepsHistory() throws Exception {
        long id = createApplication(companyId, "Backend Developer", "\"appliedOn\": \"2026-11-02\"");

        changeStatus(id, "SCREENING", "2026-11-06").andExpect(status().isOk());
        changeStatus(id, "INTERVIEWING", "2026-11-12").andExpect(status().isOk());
        postJson("/api/applications/" + id + "/status", """
                {"status": "OFFER", "note": "Verbal offer, written one follows"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OFFER"))
                .andExpect(jsonPath("$.allowedNextStatuses",
                        containsInAnyOrder("ACCEPTED", "DECLINED", "REJECTED")));

        getJson("/api/applications/" + id + "/history")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].fromStatus").doesNotExist())
                .andExpect(jsonPath("$[0].toStatus").value("APPLIED"))
                .andExpect(jsonPath("$[0].changedOn").value("2026-11-02"))
                .andExpect(jsonPath("$[3].fromStatus").value("INTERVIEWING"))
                .andExpect(jsonPath("$[3].changedOn").value("2026-11-20"))
                .andExpect(jsonPath("$[3].note").value("Verbal offer, written one follows"));
    }

    @Test
    void illegalMovesAreConflicts() throws Exception {
        long id = createApplication(companyId, "Backend Developer", null);

        changeStatus(id, "OFFER", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Cannot move from APPLIED to OFFER; allowed: [SCREENING, INTERVIEWING, REJECTED, WITHDRAWN]"));
        changeStatus(id, "APPLIED", null).andExpect(status().isConflict());

        changeStatus(id, "REJECTED", null).andExpect(status().isOk());
        changeStatus(id, "INTERVIEWING", null).andExpect(status().isConflict());
    }

    @Test
    void statusDatesCannotGoBackwards() throws Exception {
        long id = createApplication(companyId, "Backend Developer", "\"appliedOn\": \"2026-11-10\"");

        changeStatus(id, "SCREENING", "2026-11-09")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "changedOn 2026-11-09 is before the previous step (APPLIED on 2026-11-10)"));
    }

    @Test
    void movingFromWishlistToAppliedSetsTheApplicationDate() throws Exception {
        long id = createApplication(companyId, "Data Engineer", "\"status\": \"WISHLIST\"");

        // The wishlist entry was made today, so applying cannot be dated earlier.
        changeStatus(id, "APPLIED", "2026-11-18").andExpect(status().isBadRequest());
        changeStatus(id, "APPLIED", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedOn").value("2026-11-20"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void closingAnApplicationClearsItsNextAction() throws Exception {
        long id = createApplication(companyId, "Backend Developer",
                "\"nextAction\": \"Ask for feedback\", \"nextActionOn\": \"2026-11-25\"");

        changeStatus(id, "WITHDRAWN", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextAction").doesNotExist())
                .andExpect(jsonPath("$.nextActionOn").doesNotExist());
    }

    @Test
    void editsFieldsButNotTheStatus() throws Exception {
        long id = createApplication(companyId, "Backend Developer", null);
        long otherCompany = createCompany("Northwind");

        mvc.perform(put("/api/applications/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"companyId": %d, "roleTitle": "Senior Backend Developer", "appliedOn": "2026-11-19",
                         "jobUrl": "https://jobs.example.com/123", "notes": "Referral from a former colleague"}
                        """.formatted(otherCompany)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Northwind"))
                .andExpect(jsonPath("$.roleTitle").value("Senior Backend Developer"))
                .andExpect(jsonPath("$.version").value(1));

        mvc.perform(put("/api/applications/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"companyId": %d, "roleTitle": "Backend Developer", "status": "OFFER"}
                        """.formatted(otherCompany)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchesByStatusTextAndOpenFlag() throws Exception {
        long northwind = createCompany("Northwind");
        createApplication(companyId, "Backend Developer (Java)", "\"location\": \"Karlsruhe\"");
        long python = createApplication(northwind, "Python Developer", "\"source\": \"LinkedIn\"");
        long rejected = createApplication(northwind, "Platform Engineer", null);
        changeStatus(python, "INTERVIEWING", null).andExpect(status().isOk());
        changeStatus(rejected, "REJECTED", null).andExpect(status().isOk());

        getJson("/api/applications?status=INTERVIEWING&status=APPLIED&sort=roleTitle")
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].roleTitle").value("Backend Developer (Java)"))
                .andExpect(jsonPath("$.items[1].roleTitle").value("Python Developer"));

        getJson("/api/applications?q=NORTH").andExpect(jsonPath("$.totalItems").value(2));
        getJson("/api/applications?q=karls").andExpect(jsonPath("$.items[0].location").value("Karlsruhe"));
        getJson("/api/applications?q=linkedin").andExpect(jsonPath("$.items[0].id").value(python));
        getJson("/api/applications?open=false").andExpect(jsonPath("$.items[0].id").value(rejected));
        getJson("/api/applications?companyId=" + northwind + "&open=true")
                .andExpect(jsonPath("$.totalItems").value(1));
        getJson("/api/applications?size=1&sort=roleTitle")
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void badSortFieldOrStatusIsABadRequest() throws Exception {
        getJson("/api/applications?sort=salary").andExpect(status().isBadRequest());
        getJson("/api/applications?status=GHOSTED").andExpect(status().isBadRequest());
    }

    @Test
    void deletingAnApplicationRemovesItsHistory() throws Exception {
        long id = createApplication(companyId, "Backend Developer", null);

        mvc.perform(delete("/api/applications/" + id)).andExpect(status().isNoContent());
        getJson("/api/applications/" + id).andExpect(status().isNotFound());
        getJson("/api/applications/" + id + "/history").andExpect(status().isNotFound());
    }
}
