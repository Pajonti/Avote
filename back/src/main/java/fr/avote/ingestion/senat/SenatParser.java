package fr.avote.ingestion.senat;

import fr.avote.ingestion.ImportedParliamentarian;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import tools.jackson.databind.JsonNode;

import static fr.avote.ingestion.Json.date;
import static fr.avote.ingestion.Json.text;

/**
 * Fusionne l'API publique du Sénat (sénateurs en exercice : photo, groupe, circonscription) avec
 * l'export ODSEN_GENERAL (date de naissance, profession, courriel), jointure sur le matricule.
 */
public class SenatParser {

    static final String SITE = "https://www.senat.fr";

    public List<ImportedParliamentarian> parse(JsonNode activeList, JsonNode general) {
        Map<String, JsonNode> generalByMatricule = new HashMap<>();
        for (JsonNode row : general.path("results")) {
            String matricule = text(row, "Matricule");
            if (matricule != null && "ACTIF".equals(text(row, "Etat"))) {
                generalByMatricule.put(matricule.toLowerCase(Locale.ROOT), row);
            }
        }

        List<ImportedParliamentarian> result = new ArrayList<>();
        for (JsonNode senator : activeList) {
            String matricule = text(senator, "matricule");
            if (matricule == null) {
                continue;
            }
            JsonNode extra = generalByMatricule.getOrDefault(matricule.toLowerCase(Locale.ROOT),
                    tools.jackson.databind.node.MissingNode.getInstance());
            String civility = text(senator, "civilite");
            String avatar = text(senator, "urlAvatar");
            String page = text(senator, "url");
            String groupCode = text(senator, "groupe", "libelleCourt");
            String email = text(extra, "Courrier_electronique");

            result.add(new ImportedParliamentarian(
                    matricule,
                    civility,
                    text(senator, "prenom"),
                    text(senator, "nom"),
                    "M.".equals(civility) ? "M" : "Mme".equals(civility) ? "F" : null,
                    date(text(extra, "Date_naissance")),
                    firstNonNull(text(extra, "Description_de_la_profession"),
                            text(extra, "Categorie_professionnelle")),
                    avatar == null ? null : SITE + avatar,
                    "Non public".equalsIgnoreCase(email) ? null : email,
                    page == null ? null : SITE + page,
                    null,
                    groupCode,
                    groupLabel(text(senator, "groupe", "libelle")),
                    null,
                    text(senator, "circonscription", "libelle"),
                    null));
        }
        return result;
    }

    private static String groupLabel(String label) {
        return label != null && label.startsWith("Groupe ") ? label.substring("Groupe ".length()) : label;
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}
