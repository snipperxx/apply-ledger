package io.github.snipperxx.applyledger.company;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.snipperxx.applyledger.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CompanyApiTest extends IntegrationTest {

    @Test
    void createsAndReadsACompany() throws Exception {
        postJson("/api/companies", """
                {"name": "  Example Software GmbH ", "website": "https://example.com", "notes": "Met at a meetup"}
                """)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/companies/1"))
                .andExpect(jsonPath("$.name").value("Example Software GmbH"))
                .andExpect(jsonPath("$.applications").value(0));

        getJson("/api/companies/1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.website").value("https://example.com"));
    }

    @Test
    void namesAreUniqueIgnoringCase() throws Exception {
        createCompany("Northwind");

        postJson("/api/companies", "{\"name\": \"NORTHWIND\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A company named 'Northwind' already exists (id 1)"));
    }

    @Test
    void renamingToItsOwnNameIsFine() throws Exception {
        long id = createCompany("Contoso");

        mvc.perform(put("/api/companies/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"contoso\", \"website\": \"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("contoso"))
                .andExpect(jsonPath("$.website").doesNotExist());
    }

    @Test
    void rejectsInvalidInputPerField() throws Exception {
        postJson("/api/companies", "{\"name\": \" \", \"website\": \"example.com\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.website").value("must be an http(s) URL"));
    }

    @Test
    void listsCompaniesByNameWithApplicationCounts() throws Exception {
        long zeta = createCompany("Zeta Labs");
        createCompany("Alpha Systems");
        createApplication(zeta, "Backend Developer", null);
        createApplication(zeta, "Java Developer", null);

        getJson("/api/companies")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Alpha Systems"))
                .andExpect(jsonPath("$[0].applications").value(0))
                .andExpect(jsonPath("$[1].applications").value(2));
    }

    @Test
    void aCompanyWithApplicationsCannotBeDeleted() throws Exception {
        long id = createCompany("Fabrikam");
        createApplication(id, "Platform Engineer", null);

        mvc.perform(delete("/api/companies/" + id)).andExpect(status().isConflict());
    }

    @Test
    void deletesAnEmptyCompany() throws Exception {
        long id = createCompany("Tailspin");

        mvc.perform(delete("/api/companies/" + id)).andExpect(status().isNoContent());
        getJson("/api/companies/" + id).andExpect(status().isNotFound());
    }
}
