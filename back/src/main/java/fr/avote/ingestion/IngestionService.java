package fr.avote.ingestion;

import fr.avote.config.AvoteProperties;
import fr.avote.domain.Chamber;
import fr.avote.repository.IngestionWriteRepository;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/** Orchestre un import : récupération, garde-fous, persistance, suivi et invalidation du cache. */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final List<ChamberImporter> importers;
    private final ParliamentarianPersister persister;
    private final IngestionWriteRepository repository;
    private final CacheManager cacheManager;
    private final double minRatio;
    // Évite deux imports simultanés dans cette instance (démarrage + cron par exemple).
    private final ReentrantLock lock = new ReentrantLock();

    public IngestionService(List<ChamberImporter> importers, ParliamentarianPersister persister,
                            IngestionWriteRepository repository, CacheManager cacheManager,
                            AvoteProperties properties) {
        this.importers = importers;
        this.persister = persister;
        this.repository = repository;
        this.cacheManager = cacheManager;
        this.minRatio = properties.ingestion().minRatio();
    }

    /** Importe toutes les chambres ; l'échec de l'une n'empêche pas les autres. */
    public void runAll() {
        for (ChamberImporter importer : importers) {
            run(importer.chamber());
        }
    }

    public boolean run(Chamber chamber) {
        ChamberImporter importer = importers.stream()
                .filter(i -> i.chamber() == chamber).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Aucun importeur pour " + chamber));
        if (!lock.tryLock()) {
            log.warn("Import {} ignoré : un autre import est déjà en cours", chamber);
            return false;
        }
        long runId = repository.startRun(chamber);
        try {
            long started = System.nanoTime();
            List<ImportedParliamentarian> fetched = importer.fetch();
            checkSanity(chamber, fetched.size());
            int deactivated = persister.persist(chamber, fetched);
            repository.finishRun(runId, true, fetched.size(), null);
            evictCaches();
            log.info("Import {} terminé : {} parlementaires, {} désactivés ({} ms)", chamber, fetched.size(),
                    deactivated, (System.nanoTime() - started) / 1_000_000);
            return true;
        } catch (Exception e) {
            log.error("Import {} en échec", chamber, e);
            repository.finishRun(runId, false, null, truncate(e.toString()));
            return false;
        } finally {
            lock.unlock();
        }
    }

    /** Une source vide ou anormalement réduite ne doit pas vider la base. */
    private void checkSanity(Chamber chamber, int fetched) {
        long known = repository.countActive(chamber);
        if (fetched == 0) {
            throw new IllegalStateException("La source " + chamber + " n'a renvoyé aucun parlementaire");
        }
        if (known > 0 && fetched < known * minRatio) {
            throw new IllegalStateException("La source %s renvoie %d parlementaires pour %d connus (seuil %.0f%%)"
                    .formatted(chamber, fetched, known, minRatio * 100));
        }
    }

    private void evictCaches() {
        try {
            cacheManager.getCacheNames().forEach(name -> {
                var cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            });
        } catch (RuntimeException e) {
            // Redis indisponible : les entrées expireront d'elles-mêmes (TTL).
            log.warn("Invalidation du cache impossible : {}", e.toString());
        }
    }

    private static String truncate(String s) {
        return s.length() > 2000 ? s.substring(0, 2000) : s;
    }
}
