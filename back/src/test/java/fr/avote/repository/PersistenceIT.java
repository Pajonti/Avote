package fr.avote.repository;

import static org.assertj.core.api.Assertions.assertThat;

import fr.avote.api.PageResponse;
import fr.avote.api.ParliamentarianSummary;
import fr.avote.domain.Chamber;
import fr.avote.ingestion.ImportedParliamentarian;
import fr.avote.ingestion.ParliamentarianPersister;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ParliamentarianPersister.class, IngestionWriteRepository.class, ParliamentarianQueryRepository.class})
@Testcontainers
class PersistenceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    ParliamentarianPersister persister;
    @Autowired
    IngestionWriteRepository writes;
    @Autowired
    ParliamentarianQueryRepository queries;

    private static ImportedParliamentarian deputy(String id, String first, String last, String group) {
        return new ImportedParliamentarian(id, "M.", first, last, "M", LocalDate.of(1980, 1, 1), "Avocat",
                "https://example.org/" + id + ".jpg", null, "https://example.org/" + id, null,
                group, "Groupe " + group, "#112233", "Paris, 1re circonscription", LocalDate.of(2024, 7, 7));
    }

    @Test
    void persistsAndQueriesWithFiltersSearchAndPaging() {
        persister.persist(Chamber.AN, List.of(
                deputy("PA1", "Hélène", "Bergé", "HOR"),
                deputy("PA2", "Jean", "Dupont", "HOR"),
                deputy("PA3", "Zoé", "Martin", "SOC")));
        persister.persist(Chamber.SENAT, List.of(deputy("S1", "Paul", "Durand", "LR")));

        PageResponse<ParliamentarianSummary> all = queries.search(null, null, null, 0, 10);
        assertThat(all.totalItems()).isEqualTo(4);
        assertThat(all.items()).extracting(ParliamentarianSummary::lastName)
                .containsExactly("Bergé", "Dupont", "Durand", "Martin");

        assertThat(queries.search(Chamber.AN, null, null, 0, 10).totalItems()).isEqualTo(3);
        // La recherche ignore accents et casse, et combine les mots.
        assertThat(queries.search(null, null, "helene berge", 0, 10).items())
                .extracting(ParliamentarianSummary::lastName).containsExactly("Bergé");
        // Les jokers LIKE saisis par l'utilisateur sont neutralisés.
        assertThat(queries.search(null, null, "%", 0, 10).totalItems()).isZero();

        PageResponse<ParliamentarianSummary> page2 = queries.search(Chamber.AN, null, null, 1, 2);
        assertThat(page2.items()).hasSize(1);
        assertThat(page2.totalPages()).isEqualTo(2);

        var anGroups = queries.groups(Chamber.AN);
        assertThat(anGroups).extracting("code").containsExactly("HOR", "SOC");
        assertThat(anGroups.getFirst().memberCount()).isEqualTo(2);
        Long horId = anGroups.getFirst().id();
        assertThat(queries.search(Chamber.AN, horId, null, 0, 10).totalItems()).isEqualTo(2);

        assertThat(queries.chamberStats()).extracting("memberCount").containsExactly(3L, 1L, 0L);
    }

    @Test
    void importIsIdempotentAndUpdatesInPlace() {
        persister.persist(Chamber.AN, List.of(deputy("PA1", "Jean", "Dupont", "HOR")));
        persister.persist(Chamber.AN, List.of(deputy("PA1", "Jean", "Dupont", "SOC")));

        PageResponse<ParliamentarianSummary> result = queries.search(Chamber.AN, null, null, 0, 10);
        assertThat(result.totalItems()).isEqualTo(1);
        assertThat(result.items().getFirst().groupCode()).isEqualTo("SOC");
        var detail = queries.findById(result.items().getFirst().id()).orElseThrow();
        assertThat(detail.birthDate()).isEqualTo(LocalDate.of(1980, 1, 1));
        assertThat(detail.mandateStart()).isEqualTo(LocalDate.of(2024, 7, 7));
    }

    @Test
    void deactivatesMembersMissingFromLatestImportAndKeepsHistory() {
        persister.persist(Chamber.AN, List.of(deputy("PA1", "Jean", "Dupont", "HOR"),
                deputy("PA2", "Anne", "Martin", "HOR")));
        int deactivated = persister.persist(Chamber.AN, List.of(deputy("PA2", "Anne", "Martin", "HOR")));

        assertThat(deactivated).isEqualTo(1);
        assertThat(writes.countActive(Chamber.AN)).isEqualTo(1);
        assertThat(queries.search(Chamber.AN, null, null, 0, 10).items())
                .extracting(ParliamentarianSummary::lastName).containsExactly("Martin");
        assertThat(queries.groups(Chamber.AN).getFirst().memberCount()).isEqualTo(1);

        // Un retour est réactivé avec un nouveau mandat.
        persister.persist(Chamber.AN, List.of(deputy("PA1", "Jean", "Dupont", "HOR"),
                deputy("PA2", "Anne", "Martin", "HOR")));
        assertThat(writes.countActive(Chamber.AN)).isEqualTo(2);
    }

    @Test
    void recordsIngestionRuns() {
        long run = writes.startRun(Chamber.EP);
        writes.finishRun(run, false, null, "boom");

        assertThat(run).isPositive();
    }
}
