package fr.avote.ingestion.senat;

import fr.avote.config.AvoteProperties;
import fr.avote.domain.Chamber;
import fr.avote.ingestion.ChamberImporter;
import fr.avote.ingestion.ImportedParliamentarian;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SenatImporter implements ChamberImporter {

    private final RestClient client;
    private final AvoteProperties.Sources sources;
    private final JsonMapper mapper;
    private final SenatParser parser = new SenatParser();

    public SenatImporter(RestClient sourcesRestClient, AvoteProperties properties, JsonMapper mapper) {
        this.client = sourcesRestClient;
        this.sources = properties.sources();
        this.mapper = mapper;
    }

    @Override
    public Chamber chamber() {
        return Chamber.SENAT;
    }

    @Override
    public List<ImportedParliamentarian> fetch() {
        JsonNode list = mapper.readTree(download(sources.senatListUrl()));
        JsonNode general = mapper.readTree(download(sources.senatGeneralUrl()));
        return parser.parse(list, general);
    }

    private byte[] download(String url) {
        byte[] body = client.get().uri(url).retrieve().body(byte[].class);
        if (body == null) {
            throw new IllegalStateException("Réponse vide du Sénat : " + url);
        }
        return body;
    }
}
