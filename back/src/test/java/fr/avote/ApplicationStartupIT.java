package fr.avote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Démarre l'application complète (Postgres + Redis réels) : câblage, migrations, cache, API. */
@SpringBootTest(properties = "avote.ingestion.run-on-startup-if-empty=false")
@AutoConfigureMockMvc
@Testcontainers
class ApplicationStartupIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

    @Autowired
    MockMvc mvc;
    @Autowired
    CacheManager cacheManager;

    @Test
    void contextStartsAndServesEmptyDatabase() throws Exception {
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/chambers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].chamber").value("AN"));
        mvc.perform(get("/api/v1/parliamentarians"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(get("/api/v1/parliamentarians/1")).andExpect(status().isNotFound());
    }

    @Test
    void responsesAreStoredInRedisAndReadBack() throws Exception {
        mvc.perform(get("/api/v1/groups?chamber=AN")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/groups?chamber=AN")).andExpect(status().isOk());

        assertThat(cacheManager.getCache("groups").get("AN")).isNotNull();
    }
}
