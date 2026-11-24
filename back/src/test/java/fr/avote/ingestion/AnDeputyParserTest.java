package fr.avote.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import fr.avote.ingestion.an.AnDeputyParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AnDeputyParserTest {

    private static final String GROUP = """
            {"organe": {"uid": "PO845470", "codeType": "GP", "libelle": "Horizons & Indépendants",
              "libelleAbrev": "HOR", "couleurAssociee": "#B5E2F9"}}""";

    private static final String OTHER_ORGAN = """
            {"organe": {"uid": "PO1", "codeType": "COMPER", "libelle": "Commission des lois",
              "libelleAbrev": "CION_LOIS"}}""";

    /** Député actif : mandats en tableau, un mandat de groupe terminé et un en cours. */
    private static final String ACTIVE = """
            {"acteur": {
              "uid": {"@xsi:type": "IdActeur_type", "#text": "PA793912"},
              "etatCivil": {"ident": {"civ": "Mme", "prenom": "Aurore", "nom": "Bergé"},
                "infoNaissance": {"dateNais": "1986-07-06"}},
              "profession": {"libelleCourant": "Juriste"},
              "uri_hatvp": "https://www.hatvp.fr/pages_nominatives/berge-aurore",
              "adresses": {"adresse": [
                {"@xsi:type": "AdresseTelephonique_Type", "valElec": "01 02 03 04 05"},
                {"@xsi:type": "AdresseMail_Type", "valElec": "aurore.berge@assemblee-nationale.fr"}]},
              "mandats": {"mandat": [
                {"typeOrgane": "GP", "legislature": "17", "dateDebut": "2022-06-22", "dateFin": "2024-06-09",
                 "organes": {"organeRef": "PO000"}},
                {"typeOrgane": "GP", "legislature": "17", "dateDebut": "2024-07-18", "dateFin": null,
                 "organes": {"organeRef": "PO845470"}},
                {"typeOrgane": "COMPER", "dateFin": null, "organes": {"organeRef": "PO1"}},
                {"typeOrgane": "ASSEMBLEE", "legislature": "17", "dateDebut": "2024-07-07", "dateFin": null,
                 "election": {"lieu": {"departement": "Yvelines", "numCirco": "1"}},
                 "collaborateurs": {"collaborateur": [{"prenom": "Un", "nom": "Collaborateur"}]}}
              ]}}}""";

    /** Mandat unique : l'export JSON donne alors un objet et non un tableau. */
    private static final String SINGLE_MANDATE = """
            {"acteur": {
              "uid": {"#text": "PA2"},
              "etatCivil": {"ident": {"civ": "M.", "prenom": "Jean", "nom": "Dupont"},
                "infoNaissance": {"dateNais": null}},
              "mandats": {"mandat": {"typeOrgane": "ASSEMBLEE", "legislature": "17", "dateDebut": "2025-01-05",
                "dateFin": null, "election": {"lieu": {"departement": "Paris", "numCirco": "12"}}}}}}""";

    private static final String FORMER_DEPUTY = """
            {"acteur": {
              "uid": {"#text": "PA3"},
              "etatCivil": {"ident": {"civ": "M.", "prenom": "Ancien", "nom": "Député"}},
              "mandats": {"mandat": [{"typeOrgane": "ASSEMBLEE", "legislature": "17",
                "dateDebut": "2024-07-07", "dateFin": "2025-03-01"}]}}}""";

    private final AnDeputyParser parser = new AnDeputyParser(JsonMapper.builder().build());

    @Test
    void parsesActiveDeputyWithGroupAndConstituency() {
        List<ImportedParliamentarian> result = parser.parse(zip(Map.of(
                "json/organe/PO845470.json", GROUP,
                "json/organe/PO1.json", OTHER_ORGAN,
                "json/acteur/PA793912.json", ACTIVE)));

        assertThat(result).singleElement().satisfies(d -> {
            assertThat(d.sourceId()).isEqualTo("PA793912");
            assertThat(d.firstName()).isEqualTo("Aurore");
            assertThat(d.lastName()).isEqualTo("Bergé");
            assertThat(d.gender()).isEqualTo("F");
            assertThat(d.birthDate()).isEqualTo(LocalDate.of(1986, 7, 6));
            assertThat(d.profession()).isEqualTo("Juriste");
            assertThat(d.email()).isEqualTo("aurore.berge@assemblee-nationale.fr");
            assertThat(d.hatvpUrl()).endsWith("berge-aurore");
            assertThat(d.photoUrl())
                    .isEqualTo("https://www2.assemblee-nationale.fr/static/tribun/17/photos/793912.jpg");
            assertThat(d.externalUrl()).isEqualTo("https://www.assemblee-nationale.fr/dyn/deputes/PA793912");
            assertThat(d.groupCode()).isEqualTo("HOR");
            assertThat(d.groupLabel()).isEqualTo("Horizons & Indépendants");
            assertThat(d.groupColor()).isEqualTo("#B5E2F9");
            assertThat(d.constituency()).isEqualTo("Yvelines, 1re circonscription");
            assertThat(d.mandateStart()).isEqualTo(LocalDate.of(2024, 7, 7));
        });
    }

    @Test
    void acceptsSingleMandateObjectAndMissingGroup() {
        List<ImportedParliamentarian> result = parser.parse(zip(Map.of("json/acteur/PA2.json", SINGLE_MANDATE)));

        assertThat(result).singleElement().satisfies(d -> {
            assertThat(d.sourceId()).isEqualTo("PA2");
            assertThat(d.gender()).isEqualTo("M");
            assertThat(d.birthDate()).isNull();
            assertThat(d.groupCode()).isNull();
            assertThat(d.constituency()).isEqualTo("Paris, 12e circonscription");
        });
    }

    @Test
    void skipsDeputiesWithoutOngoingSeat() {
        assertThat(parser.parse(zip(Map.of("json/acteur/PA3.json", FORMER_DEPUTY)))).isEmpty();
    }

    private static byte[] zip(Map<String, String> files) {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            for (var file : new LinkedHashMap<>(files).entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }
}
