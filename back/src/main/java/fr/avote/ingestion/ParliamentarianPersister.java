package fr.avote.ingestion;

import fr.avote.domain.Chamber;
import fr.avote.repository.IngestionWriteRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applique un import complet en une seule transaction : tout ou rien. */
@Service
public class ParliamentarianPersister {

    private final IngestionWriteRepository repository;

    public ParliamentarianPersister(IngestionWriteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public int persist(Chamber chamber, List<ImportedParliamentarian> items) {
        Map<String, Long> groupIds = new HashMap<>();
        Set<String> presentIds = new HashSet<>();
        for (ImportedParliamentarian item : items) {
            Long groupId = null;
            if (item.groupCode() != null) {
                groupId = groupIds.computeIfAbsent(item.groupCode(),
                        code -> repository.upsertGroup(chamber, code, item.groupLabel(), item.groupColor()));
            }
            long id = repository.upsertParliamentarian(chamber, item);
            repository.upsertActiveMandate(id, groupId, item.constituency(), item.mandateStart());
            presentIds.add(item.sourceId());
        }
        return repository.deactivateMissing(chamber, presentIds);
    }
}
