package fr.avote.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import fr.avote.ingestion.europarl.EpMepParser;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class EpMepParserTest {

    private static final String LIST_ITEM = """
            {"id": "person/1294", "type": "Person", "identifier": "1294", "label": "Elio DI RUPO",
             "familyName": "Di Rupo", "givenName": "Elio",
             "api:country-of-representation": "BE", "api:political-group": "S&D"}""";

    private static final String DETAIL = """
            {"id": "person/1294", "hasEmail": "mailto:elio.dirupo@europarl.europa.eu",
             "hasGender": "http://publications.europa.eu/resource/authority/human-sex/MALE",
             "hasHonorificPrefix": "http://publications.europa.eu/resource/authority/honorific/MR",
             "img": "https://www.europarl.europa.eu/mepphoto/1294.jpg"}""";

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final EpMepParser parser = new EpMepParser();

    @Test
    void combinesListItemAndDetail() {
        ImportedParliamentarian mep = parser.parse(mapper.readTree(LIST_ITEM), mapper.readTree(DETAIL));

        assertThat(mep.sourceId()).isEqualTo("1294");
        assertThat(mep.firstName()).isEqualTo("Elio");
        assertThat(mep.lastName()).isEqualTo("Di Rupo");
        assertThat(mep.civility()).isEqualTo("M.");
        assertThat(mep.gender()).isEqualTo("M");
        assertThat(mep.email()).isEqualTo("elio.dirupo@europarl.europa.eu");
        assertThat(mep.constituency()).isEqualTo("Belgique");
        assertThat(mep.groupCode()).isEqualTo("S&D");
        assertThat(mep.groupLabel()).contains("socialistes");
        assertThat(mep.groupColor()).startsWith("#");
        assertThat(mep.externalUrl()).isEqualTo("https://www.europarl.europa.eu/meps/fr/1294");
    }

    @Test
    void worksWithoutDetail() {
        JsonNode item = mapper.readTree(LIST_ITEM);
        ImportedParliamentarian mep = parser.parse(item, null);

        assertThat(mep.email()).isNull();
        assertThat(mep.gender()).isNull();
        assertThat(mep.photoUrl()).isEqualTo("https://www.europarl.europa.eu/mepphoto/1294.jpg");
        assertThat(mep.lastName()).isEqualTo("Di Rupo");
    }

    @Test
    void keepsUnknownGroupCodeAsLabel() {
        JsonNode item = mapper.readTree("""
                {"identifier": "1", "givenName": "A", "familyName": "B", "api:political-group": "XYZ",
                 "api:country-of-representation": "FR"}""");

        ImportedParliamentarian mep = parser.parse(item, null);

        assertThat(mep.groupCode()).isEqualTo("XYZ");
        assertThat(mep.groupLabel()).isEqualTo("XYZ");
        assertThat(mep.groupColor()).isNull();
        assertThat(mep.constituency()).isEqualTo("France");
    }
}
