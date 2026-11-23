package fr.avote.ingestion.europarl;

import fr.avote.config.AvoteProperties;
import fr.avote.domain.Chamber;
import fr.avote.ingestion.ChamberImporter;
import fr.avote.ingestion.ImportedParliamentarian;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class EpImporter implements ChamberImporter {

    private static final Logger log = LoggerFactory.getLogger(EpImporter.class);
    private static final int PAGE_SIZE = 500;
    private static final int MAX_CONCURRENT_DETAIL_CALLS = 4;
    private static final String JSON_LD = "application/ld+json";

    private final RestClient client;
    private final String baseUrl;
    private final JsonMapper mapper;
    private final EpMepParser parser = new EpMepParser();

    public EpImporter(RestClient sourcesRestClient, AvoteProperties properties, JsonMapper mapper) {
        this.client = sourcesRestClient;
        this.baseUrl = properties.sources().epBaseUrl();
        this.mapper = mapper;
    }

    @Override
    public Chamber chamber() {
        return Chamber.EP;
    }

    @Override
    public List<ImportedParliamentarian> fetch() {
        List<JsonNode> mepList = fetchCurrentMeps();
        Semaphore permits = new Semaphore(MAX_CONCURRENT_DETAIL_CALLS);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<ImportedParliamentarian>> futures = new ArrayList<>();
            for (JsonNode item : mepList) {
                futures.add(executor.submit(() -> {
                    permits.acquire();
                    try {
                        return parser.parse(item, fetchDetail(item));
                    } finally {
                        permits.release();
                    }
                }));
            }
            List<ImportedParliamentarian> result = new ArrayList<>(futures.size());
            for (Future<ImportedParliamentarian> future : futures) {
                try {
                    result.add(future.get());
                } catch (Exception e) {
                    throw new IllegalStateException("Import des députés européens interrompu", e);
                }
            }
            return result;
        }
    }

    private List<JsonNode> fetchCurrentMeps() {
        List<JsonNode> all = new ArrayList<>();
        for (int offset = 0; ; offset += PAGE_SIZE) {
            JsonNode page = mapper.readTree(get("/meps/show-current?format=application%2Fld%2Bjson&offset="
                    + offset + "&limit=" + PAGE_SIZE));
            int before = all.size();
            page.path("data").forEach(all::add);
            if (all.size() - before < PAGE_SIZE) {
                return all;
            }
        }
    }

    /** Le détail est un enrichissement : son échec ne doit pas faire perdre le député. */
    private JsonNode fetchDetail(JsonNode listItem) {
        String id = listItem.path("identifier").asString();
        try {
            JsonNode data = mapper.readTree(get("/meps/" + id + "?format=application%2Fld%2Bjson")).path("data");
            return data.isArray() && !data.isEmpty() ? data.get(0) : null;
        } catch (RuntimeException e) {
            log.warn("Détail indisponible pour le député européen {} : {}", id, e.toString());
            return null;
        }
    }

    private byte[] get(String pathAndQuery) {
        byte[] body = client.get().uri(java.net.URI.create(baseUrl + pathAndQuery)).header("Accept", JSON_LD)
                .retrieve().body(byte[].class);
        if (body == null) {
            throw new IllegalStateException("Réponse vide du Parlement européen : " + pathAndQuery);
        }
        return body;
    }
}
