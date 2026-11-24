package fr.avote.repository;

import fr.avote.api.ChamberStats;
import fr.avote.api.GroupDto;
import fr.avote.api.PageResponse;
import fr.avote.api.ParliamentarianDetail;
import fr.avote.api.ParliamentarianSummary;
import fr.avote.domain.Chamber;
import fr.avote.domain.SearchText;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Requêtes de lecture ; ne remonte que les parlementaires actifs. */
@Repository
public class ParliamentarianQueryRepository {

    private static final String FROM = """
            from parliamentarian p
            left join mandate m on m.parliamentarian_id = p.id and m.active
            left join political_group g on g.id = m.group_id
            """;

    private final JdbcClient jdbc;

    public ParliamentarianQueryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public PageResponse<ParliamentarianSummary> search(Chamber chamber, Long groupId, String query, int page,
                                                       int size) {
        List<String> where = new ArrayList<>(List.of("p.active"));
        var params = new java.util.LinkedHashMap<String, Object>();
        if (chamber != null) {
            where.add("p.chamber = :chamber");
            params.put("chamber", chamber.name());
        }
        if (groupId != null) {
            where.add("m.group_id = :groupId");
            params.put("groupId", groupId);
        }
        String[] tokens = SearchText.normalize(query).isEmpty() ? new String[0]
                : SearchText.normalize(query).split(" ");
        for (int i = 0; i < tokens.length; i++) {
            where.add("p.search_text like :t" + i + " escape '\\'");
            params.put("t" + i, "%" + escapeLike(tokens[i]) + "%");
        }
        String whereSql = " where " + String.join(" and ", where);

        long total = bind(jdbc.sql("select count(*) " + FROM + whereSql), params).query(Long.class).single();

        var select = jdbc.sql("""
                select p.id, p.chamber, p.civility, p.first_name, p.last_name, p.photo_url,
                       g.id as group_id, g.code as group_code, g.label as group_label, g.color as group_color,
                       m.constituency
                """ + FROM + whereSql + """
                 order by lower(p.last_name), lower(p.first_name), p.id
                 limit :limit offset :offset
                """);
        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<ParliamentarianSummary> items = bind(select, params)
                .query((rs, n) -> new ParliamentarianSummary(
                        rs.getLong("id"),
                        Chamber.valueOf(rs.getString("chamber")),
                        rs.getString("civility"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("photo_url"),
                        (Long) rs.getObject("group_id"),
                        rs.getString("group_code"),
                        rs.getString("group_label"),
                        rs.getString("group_color"),
                        rs.getString("constituency")))
                .list();
        int totalPages = (int) Math.ceil(total / (double) size);
        return new PageResponse<>(items, page, size, total, totalPages);
    }

    public Optional<ParliamentarianDetail> findById(long id) {
        return jdbc.sql("""
                select p.id, p.chamber, p.civility, p.first_name, p.last_name, p.gender, p.birth_date,
                       p.profession, p.photo_url, p.email, p.external_url, p.hatvp_url,
                       g.id as group_id, g.code as group_code, g.label as group_label, g.color as group_color,
                       m.constituency, m.start_date
                """ + FROM + " where p.id = :id and p.active")
                .param("id", id)
                .query((rs, n) -> {
                    Date birth = rs.getDate("birth_date");
                    Date start = rs.getDate("start_date");
                    return new ParliamentarianDetail(
                            rs.getLong("id"),
                            Chamber.valueOf(rs.getString("chamber")),
                            rs.getString("civility"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("gender"),
                            birth == null ? null : birth.toLocalDate(),
                            rs.getString("profession"),
                            rs.getString("photo_url"),
                            rs.getString("email"),
                            rs.getString("external_url"),
                            rs.getString("hatvp_url"),
                            (Long) rs.getObject("group_id"),
                            rs.getString("group_code"),
                            rs.getString("group_label"),
                            rs.getString("group_color"),
                            rs.getString("constituency"),
                            start == null ? null : start.toLocalDate());
                })
                .optional();
    }

    public List<GroupDto> groups(Chamber chamber) {
        return jdbc.sql("""
                select g.id, g.chamber, g.code, g.label, g.color, count(m.id) as member_count
                from political_group g
                join mandate m on m.group_id = g.id and m.active
                join parliamentarian p on p.id = m.parliamentarian_id and p.active
                where (cast(:chamber as varchar) is null or g.chamber = :chamber)
                group by g.id
                order by g.chamber, member_count desc, g.code
                """)
                .param("chamber", chamber == null ? null : chamber.name())
                .query((rs, n) -> new GroupDto(
                        rs.getLong("id"),
                        Chamber.valueOf(rs.getString("chamber")),
                        rs.getString("code"),
                        rs.getString("label"),
                        rs.getString("color"),
                        rs.getLong("member_count")))
                .list();
    }

    public List<ChamberStats> chamberStats() {
        var counts = new java.util.EnumMap<Chamber, Long>(Chamber.class);
        jdbc.sql("select chamber, count(*) as n from parliamentarian where active group by chamber")
                .query((rs, n) -> {
                    counts.put(Chamber.valueOf(rs.getString("chamber")), rs.getLong("n"));
                    return null;
                }).list();
        return java.util.Arrays.stream(Chamber.values())
                .map(c -> new ChamberStats(c, c.label(), counts.getOrDefault(c, 0L)))
                .toList();
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, java.util.Map<String, Object> params) {
        return spec.params(params);
    }

    private static String escapeLike(String token) {
        return token.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
