package fr.avote.ingestion.an;

import fr.avote.config.AvoteProperties;
import fr.avote.domain.Chamber;
import fr.avote.ingestion.ChamberImporter;
import fr.avote.ingestion.ImportedParliamentarian;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Component
public class AnImporter implements ChamberImporter {

    private final RestClient client;
    private final String url;
    private final AnDeputyParser parser;

    public AnImporter(RestClient sourcesRestClient, AvoteProperties properties, JsonMapper mapper) {
        this.client = sourcesRestClient;
        this.url = properties.sources().anUrl();
        this.parser = new AnDeputyParser(mapper);
    }

    @Override
    public Chamber chamber() {
        return Chamber.AN;
    }

    @Override
    public List<ImportedParliamentarian> fetch() {
        byte[] zip = client.get().uri(url).retrieve().body(byte[].class);
        if (zip == null) {
            throw new IllegalStateException("Réponse vide de l'Assemblée nationale");
        }
        return parser.parse(zip);
    }
}
