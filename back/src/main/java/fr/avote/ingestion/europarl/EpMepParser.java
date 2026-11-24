package fr.avote.ingestion.europarl;

import fr.avote.ingestion.ImportedParliamentarian;
import java.util.Locale;
import tools.jackson.databind.JsonNode;

import static fr.avote.ingestion.Json.text;

/** Combine la fiche de la liste « en exercice » et, si disponible, le détail d'un député européen. */
public class EpMepParser {

    static final String MEP_PAGE = "https://www.europarl.europa.eu/meps/fr/";

    /** {@code detail} peut être absent (null) : on garde alors les seules données de la liste. */
    public ImportedParliamentarian parse(JsonNode listItem, JsonNode detail) {
        JsonNode d = detail == null ? tools.jackson.databind.node.MissingNode.getInstance() : detail;
        String id = text(listItem, "identifier");
        String groupCode = text(listItem, "api:political-group");
        String email = text(d, "hasEmail");

        EpGroups.Info group = groupCode == null ? null : EpGroups.of(groupCode);
        return new ImportedParliamentarian(
                id,
                civility(text(d, "hasHonorificPrefix")),
                text(listItem, "givenName"),
                text(listItem, "familyName"),
                gender(text(d, "hasGender")),
                null,
                null,
                text(d, "img") != null ? text(d, "img") : "https://www.europarl.europa.eu/mepphoto/" + id + ".jpg",
                email == null ? null : email.replaceFirst("^mailto:", ""),
                MEP_PAGE + id,
                null,
                groupCode,
                group == null ? null : group.label(),
                group == null ? null : group.color(),
                country(text(listItem, "api:country-of-representation")),
                null);
    }

    static String country(String isoCode) {
        if (isoCode == null) {
            return null;
        }
        String name = new Locale.Builder().setRegion(isoCode).build().getDisplayCountry(Locale.FRENCH);
        return name.isBlank() ? isoCode : name;
    }

    private static String gender(String uri) {
        if (uri == null) {
            return null;
        }
        return uri.endsWith("/MALE") ? "M" : uri.endsWith("/FEMALE") ? "F" : null;
    }

    private static String civility(String uri) {
        if (uri == null) {
            return null;
        }
        return uri.endsWith("/MR") ? "M." : uri.endsWith("/MRS") || uri.endsWith("/MS") ? "Mme" : null;
    }
}
