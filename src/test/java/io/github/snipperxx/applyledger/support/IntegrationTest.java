package io.github.snipperxx.applyledger.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("truncate status_change, job_application, company restart identity cascade");
    }

    protected ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions getJson(String url) throws Exception {
        return mvc.perform(get(url).accept(MediaType.APPLICATION_JSON));
    }

    protected long createCompany(String name) throws Exception {
        String body = postJson("/api/companies", "{\"name\": \"" + name + "\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** Creates an APPLIED application; extra JSON members (e.g. {@code "appliedOn": "2026-11-01"}) are appended. */
    protected long createApplication(long companyId, String roleTitle, String extraJson) throws Exception {
        String json = "{\"companyId\": " + companyId + ", \"roleTitle\": \"" + roleTitle + "\""
                + (extraJson == null || extraJson.isBlank() ? "" : ", " + extraJson) + "}";
        String body = postJson("/api/applications", json)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected ResultActions changeStatus(long applicationId, String status, String changedOn) throws Exception {
        String json = "{\"status\": \"" + status + "\""
                + (changedOn == null ? "" : ", \"changedOn\": \"" + changedOn + "\"") + "}";
        return postJson("/api/applications/" + applicationId + "/status", json);
    }
}
