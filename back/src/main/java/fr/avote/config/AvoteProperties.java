package fr.avote.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "avote")
public record AvoteProperties(Ingestion ingestion, Cache cache, Sources sources) {

    public record Ingestion(String cron, String zone, boolean runOnStartupIfEmpty, double minRatio) {
    }

    public record Cache(Duration ttl) {
    }

    public record Sources(String userAgent, String anUrl, String senatListUrl, String senatGeneralUrl,
                          String epBaseUrl) {
    }
}
