package fr.avote.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    /** Client dédié à la récupération des données publiques ; le User-Agent est exigé par certaines sources. */
    @Bean
    RestClient sourcesRestClient(RestClient.Builder builder, AvoteProperties properties) {
        return builder
                .defaultHeader(HttpHeaders.USER_AGENT, properties.sources().userAgent())
                .build();
    }
}
