package fr.avote.ingestion.an;

import fr.avote.ingestion.ImportedParliamentarian;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static fr.avote.ingestion.Json.date;
import static fr.avote.ingestion.Json.items;
import static fr.avote.ingestion.Json.text;

/**
 * Lit l'export « députés actifs et mandats actifs » de l'Assemblée nationale (zip de fichiers JSON,
 * un par acteur et un par organe).
 */
public class AnDeputyParser {

    private static final String ACTEUR_DIR = "json/acteur/";
    private static final String ORGANE_DIR = "json/organe/";

    private record Group(String code, String label, String color) {
    }

    private final JsonMapper mapper;

    public AnDeputyParser(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public List<ImportedParliamentarian> parse(byte[] zip) {
        Map<String, Group> groups = readGroups(zip);
        List<ImportedParliamentarian> result = new ArrayList<>();
        forEachJson(zip, ACTEUR_DIR, root -> {
            ImportedParliamentarian deputy = toDeputy(root.path("acteur"), groups);
            if (deputy != null) {
                result.add(deputy);
            }
        });
        return result;
    }

    /** Premier passage : seuls les groupes politiques nous intéressent parmi les milliers d'organes. */
    private Map<String, Group> readGroups(byte[] zip) {
        Map<String, Group> groups = new HashMap<>();
        forEachJson(zip, ORGANE_DIR, root -> {
            JsonNode organe = root.path("organe");
            if ("GP".equals(text(organe, "codeType"))) {
                groups.put(text(organe, "uid"), new Group(
                        text(organe, "libelleAbrev"), text(organe, "libelle"), text(organe, "couleurAssociee")));
            }
        });
        return groups;
    }

    private ImportedParliamentarian toDeputy(JsonNode acteur, Map<String, Group> groups) {
        JsonNode seat = null;
        Group group = null;
        for (JsonNode mandate : items(acteur.path("mandats").path("mandat"))) {
            if (text(mandate, "dateFin") != null) {
                continue;
            }
            String type = text(mandate, "typeOrgane");
            if ("ASSEMBLEE".equals(type)) {
                seat = mandate;
            } else if ("GP".equals(type)) {
                group = groups.get(text(mandate, "organes", "organeRef"));
            }
        }
        if (seat == null) {
            return null; // pas de mandat de député en cours
        }

        String id = text(acteur, "uid", "#text");
        JsonNode ident = acteur.path("etatCivil").path("ident");
        String civility = text(ident, "civ");
        String legislature = text(seat, "legislature");
        String digits = id.replaceAll("\\D", "");

        return new ImportedParliamentarian(
                id,
                civility,
                text(ident, "prenom"),
                text(ident, "nom"),
                genderOf(civility),
                date(text(acteur, "etatCivil", "infoNaissance", "dateNais")),
                text(acteur, "profession", "libelleCourant"),
                legislature == null ? null
                        : "https://www2.assemblee-nationale.fr/static/tribun/%s/photos/%s.jpg".formatted(legislature, digits),
                email(acteur),
                "https://www.assemblee-nationale.fr/dyn/deputes/" + id,
                text(acteur, "uri_hatvp"),
                group == null ? null : group.code(),
                group == null ? null : group.label(),
                group == null ? null : group.color(),
                constituency(seat.path("election").path("lieu")),
                date(text(seat, "dateDebut")));
    }

    private static String email(JsonNode acteur) {
        for (JsonNode address : items(acteur.path("adresses").path("adresse"))) {
            if ("AdresseMail_Type".equals(text(address, "@xsi:type"))) {
                return text(address, "valElec");
            }
        }
        return null;
    }

    static String genderOf(String civility) {
        if (civility == null) {
            return null;
        }
        return switch (civility) {
            case "M.", "M" -> "M";
            case "Mme", "Mlle" -> "F";
            default -> null;
        };
    }

    static String constituency(JsonNode lieu) {
        String department = text(lieu, "departement");
        String number = text(lieu, "numCirco");
        if (department == null) {
            return null;
        }
        if (number == null) {
            return department;
        }
        String ordinal = "1".equals(number) ? "1re" : number + "e";
        return "%s, %s circonscription".formatted(department, ordinal);
    }

    private void forEachJson(byte[] zip, String dir, java.util.function.Consumer<JsonNode> consumer) {
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().startsWith(dir) && entry.getName().endsWith(".json")) {
                    // readAllBytes : readTree(InputStream) fermerait le flux du zip.
                    consumer.accept(mapper.readTree(in.readAllBytes()));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Archive de l'Assemblée nationale illisible", e);
        }
    }
}
