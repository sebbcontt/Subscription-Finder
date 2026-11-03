package com.sebbcontt.subscriptions.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Runs the whole app against a real PostgreSQL in Docker. Needs Docker running. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ImportApiIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private static MockMultipartFile csv(String name, String content) {
        return new MockMultipartFile("file", name, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    /** Netflix every month plus a payday deposit, as a bank export. */
    private static String sampleCsv() {
        StringBuilder sb = new StringBuilder("date,description,amount\n");
        LocalDate start = LocalDate.of(2026, 1, 3);
        for (int i = 0; i < 6; i++) {
            sb.append(start.plusMonths(i)).append(",NETFLIX.COM 866-579-7172 CA,-15.49\n");
            sb.append(start.plusMonths(i)).append(",PAYROLL,2400.00\n");
        }
        return sb.toString();
    }

    private long uploadSample() throws Exception {
        String body = mvc.perform(multipart("/api/imports").file(csv("bank.csv", sampleCsv())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    @Test
    void uploadingACsvReportsWhatWasFound() throws Exception {
        mvc.perform(multipart("/api/imports").file(csv("bank.csv", sampleCsv())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.rowsRead").value(12))
                .andExpect(jsonPath("$.charges").value(6))
                .andExpect(jsonPath("$.subscriptionsFound").value(1));
    }

    @Test
    void subscriptionsComeBackFromTheDatabase() throws Exception {
        long id = uploadSample();

        mvc.perform(get("/api/imports/" + id + "/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].merchant").value("Netflix"))
                .andExpect(jsonPath("$[0].cadence").value("MONTHLY"))
                .andExpect(jsonPath("$[0].yearlyCost").value(185.88));
    }

    @Test
    void summaryAddsUpTheTotals() throws Exception {
        long id = uploadSample();

        mvc.perform(get("/api/imports/" + id + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCount").value(1))
                .andExpect(jsonPath("$.monthlyTotal").value(15.49));
    }

    @Test
    void importsAreListed() throws Exception {
        uploadSample();

        mvc.perform(get("/api/imports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].filename", hasItem("bank.csv")));
    }

    @Test
    void aBadCsvGetsAClearMessage() throws Exception {
        mvc.perform(multipart("/api/imports").file(csv("bad.csv", "date,description,amount\nnope,X,-1\n")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Row 2")));
    }

    @Test
    void aFileThatIsNotCsvIsRejected() throws Exception {
        mvc.perform(multipart("/api/imports").file(csv("notes.txt", "hello")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anEmptyFileIsRejected() throws Exception {
        mvc.perform(multipart("/api/imports").file(csv("empty.csv", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anUnknownImportIsNotFound() throws Exception {
        mvc.perform(get("/api/imports/999999/subscriptions")).andExpect(status().isNotFound());
        mvc.perform(get("/api/imports/999999/summary")).andExpect(status().isNotFound());
    }
}
