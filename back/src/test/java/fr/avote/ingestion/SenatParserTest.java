package fr.avote.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import fr.avote.ingestion.senat.SenatParser;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class SenatParserTest {

    private static final String LIST = """
            [{"matricule": "21071F", "civilite": "Mme", "nom": "Aeschlimann", "prenom": "Marie-Do",
              "url": "/senateur/aeschlimann_marie_do21071f.html",
              "urlAvatar": "/senimg/aeschlimann_marie_do21071f_carre.jpg",
              "groupe": {"code": "LR", "libelle": "Groupe Les Républicains", "libelleCourt": "Les Républicains"},
              "circonscription": {"code": "92", "libelle": "Hauts-de-Seine"}},
             {"matricule": "19591F", "civilite": "M.", "nom": "Iacovelli", "prenom": "Xavier",
              "url": "/senateur/iacovelli_xavier19591f.html", "urlAvatar": "/senimg/iacovelli_xavier19591f_carre.jpg",
              "groupe": {"code": "NI", "libelle": "Réunion administrative des Sénateurs ne figurant sur la liste d'aucun groupe",
                         "libelleCourt": "NI"},
              "circonscription": {"libelle": "Hauts-de-Seine"}}]""";

    private static final String GENERAL = """
            {"results": [
              {"Matricule": "21071F", "Etat": "ACTIF", "Date_naissance": "1974/04/17 00:00:00",
               "Courrier_electronique": "md.aeschlimann@senat.fr", "Description_de_la_profession": "Avocat",
               "Categorie_professionnelle": "Professions judiciaires"},
              {"Matricule": "19591F", "Etat": "ACTIF", "Date_naissance": null,
               "Courrier_electronique": "Non public", "Description_de_la_profession": null,
               "Categorie_professionnelle": "Salariés (Employés)"},
              {"Matricule": "83008P", "Etat": "ANCIEN", "Date_naissance": "1930/06/19 00:00:00"}]}""";

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final SenatParser parser = new SenatParser();

    @Test
    void mergesActiveListWithGeneralExport() {
        List<ImportedParliamentarian> result = parse(LIST, GENERAL);

        assertThat(result).hasSize(2);
        ImportedParliamentarian first = result.getFirst();
        assertThat(first.sourceId()).isEqualTo("21071F");
        assertThat(first.gender()).isEqualTo("F");
        assertThat(first.birthDate()).isEqualTo(LocalDate.of(1974, 4, 17));
        assertThat(first.profession()).isEqualTo("Avocat");
        assertThat(first.email()).isEqualTo("md.aeschlimann@senat.fr");
        assertThat(first.photoUrl()).isEqualTo("https://www.senat.fr/senimg/aeschlimann_marie_do21071f_carre.jpg");
        assertThat(first.externalUrl()).isEqualTo("https://www.senat.fr/senateur/aeschlimann_marie_do21071f.html");
        assertThat(first.groupCode()).isEqualTo("Les Républicains");
        assertThat(first.groupLabel()).isEqualTo("Les Républicains");
        assertThat(first.constituency()).isEqualTo("Hauts-de-Seine");
    }

    @Test
    void hidesNonPublicEmailAndFallsBackToProfessionCategory() {
        ImportedParliamentarian second = parse(LIST, GENERAL).get(1);

        assertThat(second.email()).isNull();
        assertThat(second.profession()).isEqualTo("Salariés (Employés)");
        assertThat(second.birthDate()).isNull();
    }

    @Test
    void keepsSenatorsAbsentFromGeneralExport() {
        List<ImportedParliamentarian> result = parse(LIST, "{\"results\": []}");

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().birthDate()).isNull();
        assertThat(result.getFirst().email()).isNull();
    }

    private List<ImportedParliamentarian> parse(String list, String general) {
        JsonNode listNode = mapper.readTree(list);
        return parser.parse(listNode, mapper.readTree(general));
    }
}
