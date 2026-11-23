package fr.avote.repository;

import fr.avote.domain.Chamber;
import fr.avote.domain.SearchText;
import fr.avote.ingestion.ImportedParliamentarian;
import java.sql.Date;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Écritures liées à l'ingestion : upserts idempotents et suivi des exécutions. */
@Repository
public class IngestionWriteRepository {

    private final JdbcClient jdbc;

    public IngestionWriteRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long upsertGroup(Chamber chamber, String code, String label, String color) {
        return jdbc.sql("""
                insert into political_group (chamber, code, label, color)
                values (:chamber, :code, :label, :color)
                on conflict (chamber, code) do update
                   set label = excluded.label,
                       color = coalesce(excluded.color, political_group.color)
                returning id
                """)
                .param("chamber", chamber.name())
                .param("code", code)
                .param("label", label == null ? code : label)
                .param("color", color)
                .query(Long.class).single();
    }

    public long upsertParliamentarian(Chamber chamber, ImportedParliamentarian p) {
        return jdbc.sql("""
                insert into parliamentarian (chamber, source_id, civility, first_name, last_name, gender,
                                             birth_date, profession, photo_url, email, external_url, hatvp_url,
                                             search_text, active)
                values (:chamber, :sourceId, :civility, :firstName, :lastName, :gender,
                        :birthDate, :profession, :photoUrl, :email, :externalUrl, :hatvpUrl,
                        :searchText, true)
                on conflict (chamber, source_id) do update
                   set civility = excluded.civility, first_name = excluded.first_name,
                       last_name = excluded.last_name, gender = excluded.gender,
                       birth_date = excluded.birth_date, profession = excluded.profession,
                       photo_url = excluded.photo_url, email = excluded.email,
                       external_url = excluded.external_url, hatvp_url = excluded.hatvp_url,
                       search_text = excluded.search_text, active = true, updated_at = now()
                returning id
                """)
                .param("chamber", chamber.name())
                .param("sourceId", p.sourceId())
                .param("civility", p.civility())
                .param("firstName", p.firstName())
                .param("lastName", p.lastName())
                .param("gender", p.gender())
                .param("birthDate", p.birthDate() == null ? null : Date.valueOf(p.birthDate()))
                .param("profession", p.profession())
                .param("photoUrl", p.photoUrl())
                .param("email", p.email())
                .param("externalUrl", p.externalUrl())
                .param("hatvpUrl", p.hatvpUrl())
                .param("searchText", SearchText.normalize(p.firstName() + " " + p.lastName()))
                .query(Long.class).single();
    }

    public void upsertActiveMandate(long parliamentarianId, Long groupId, String constituency,
                                    java.time.LocalDate start) {
        jdbc.sql("""
                insert into mandate (parliamentarian_id, group_id, constituency, start_date, active)
                values (:pid, :groupId, :constituency, :start, true)
                on conflict (parliamentarian_id) where active do update
                   set group_id = excluded.group_id,
                       constituency = excluded.constituency,
                       start_date = coalesce(excluded.start_date, mandate.start_date)
                """)
                .param("pid", parliamentarianId)
                .param("groupId", groupId)
                .param("constituency", constituency)
                .param("start", start == null ? null : Date.valueOf(start))
                .update();
    }

    /** Désactive les parlementaires de la chambre absents de l'import et clôt leur mandat. */
    public int deactivateMissing(Chamber chamber, Collection<String> presentSourceIds) {
        List<Long> toDeactivate = jdbc.sql("select id, source_id from parliamentarian where chamber = :c and active")
                .param("c", chamber.name())
                .query((rs, n) -> new Object[]{rs.getLong("id"), rs.getString("source_id")})
                .list().stream()
                .filter(row -> !presentSourceIds.contains((String) row[1]))
                .map(row -> (Long) row[0])
                .toList();
        if (toDeactivate.isEmpty()) {
            return 0;
        }
        jdbc.sql("update mandate set active = false, end_date = current_date "
                        + "where active and parliamentarian_id in (:ids)")
                .param("ids", toDeactivate).update();
        return jdbc.sql("update parliamentarian set active = false, updated_at = now() where id in (:ids)")
                .param("ids", toDeactivate).update();
    }

    public long countActive(Chamber chamber) {
        return jdbc.sql("select count(*) from parliamentarian where chamber = :c and active")
                .param("c", chamber.name()).query(Long.class).single();
    }

    public long startRun(Chamber chamber) {
        return jdbc.sql("insert into ingestion_run (chamber, status) values (:c, 'RUNNING') returning id")
                .param("c", chamber.name()).query(Long.class).single();
    }

    public void finishRun(long runId, boolean success, Integer fetchedCount, String error) {
        jdbc.sql("""
                update ingestion_run
                   set finished_at = now(), status = :status, fetched_count = :count, error = :error
                 where id = :id
                """)
                .param("status", success ? "SUCCESS" : "FAILED")
                .param("count", fetchedCount)
                .param("error", error)
                .param("id", runId)
                .update();
    }
}
