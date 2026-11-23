package fr.avote.ingestion;

import fr.avote.config.AvoteProperties;
import fr.avote.domain.Chamber;
import fr.avote.repository.IngestionWriteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Déclenche les imports : planification quotidienne, et amorçage si la base est vide. */
@Component
public class IngestionTrigger {

    private static final Logger log = LoggerFactory.getLogger(IngestionTrigger.class);

    private final IngestionService service;
    private final IngestionWriteRepository repository;
    private final AvoteProperties properties;

    public IngestionTrigger(IngestionService service, IngestionWriteRepository repository,
                            AvoteProperties properties) {
        this.service = service;
        this.repository = repository;
        this.properties = properties;
    }

    @Scheduled(cron = "${avote.ingestion.cron}", zone = "${avote.ingestion.zone}")
    public void scheduledRun() {
        service.runAll();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapIfEmpty() {
        if (!properties.ingestion().runOnStartupIfEmpty()) {
            return;
        }
        boolean anyEmpty = false;
        for (Chamber chamber : Chamber.values()) {
            anyEmpty |= repository.countActive(chamber) == 0;
        }
        if (anyEmpty) {
            log.info("Base incomplète : import initial lancé en arrière-plan");
            // Hors du thread de démarrage pour ne pas retarder la disponibilité de l'API.
            Thread.ofVirtual().name("initial-ingestion").start(() -> {
                for (Chamber chamber : Chamber.values()) {
                    if (repository.countActive(chamber) == 0) {
                        service.run(chamber);
                    }
                }
            });
        }
    }
}
