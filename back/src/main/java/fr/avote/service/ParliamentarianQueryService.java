package fr.avote.service;

import fr.avote.api.ChamberStats;
import fr.avote.api.GroupDto;
import fr.avote.api.PageResponse;
import fr.avote.api.ParliamentarianDetail;
import fr.avote.api.ParliamentarianSummary;
import fr.avote.domain.Chamber;
import fr.avote.repository.ParliamentarianQueryRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Lectures mises en cache (Redis) ; le cache est vidé après chaque import réussi. */
@Service
public class ParliamentarianQueryService {

    public static final String CACHE_LIST = "parliamentarians";
    public static final String CACHE_DETAIL = "parliamentarian";
    public static final String CACHE_GROUPS = "groups";
    public static final String CACHE_CHAMBERS = "chambers";

    private final ParliamentarianQueryRepository repository;

    public ParliamentarianQueryService(ParliamentarianQueryRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = CACHE_LIST,
            key = "(#chamber ?: '-') + '|' + (#groupId ?: '-') + '|' + (#query ?: '-') + '|' + #page + '|' + #size")
    public PageResponse<ParliamentarianSummary> search(Chamber chamber, Long groupId, String query, int page,
                                                       int size) {
        return repository.search(chamber, groupId, query, page, size);
    }

    @Cacheable(cacheNames = CACHE_DETAIL, key = "#id")
    public Optional<ParliamentarianDetail> find(long id) {
        return repository.findById(id);
    }

    @Cacheable(cacheNames = CACHE_GROUPS, key = "#chamber ?: 'ALL'")
    public List<GroupDto> groups(Chamber chamber) {
        return repository.groups(chamber);
    }

    @Cacheable(cacheNames = CACHE_CHAMBERS, key = "'ALL'")
    public List<ChamberStats> chambers() {
        return repository.chamberStats();
    }
}
