package fr.avote.config;

import static fr.avote.service.ParliamentarianQueryService.CACHE_CHAMBERS;
import static fr.avote.service.ParliamentarianQueryService.CACHE_DETAIL;
import static fr.avote.service.ParliamentarianQueryService.CACHE_GROUPS;
import static fr.avote.service.ParliamentarianQueryService.CACHE_LIST;

import fr.avote.api.ChamberStats;
import fr.avote.api.GroupDto;
import fr.avote.api.PageResponse;
import fr.avote.api.ParliamentarianDetail;
import fr.avote.api.ParliamentarianSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cache Redis : valeurs en JSON typé (lisibles, tolérantes aux évolutions de classes), préfixe
 * {@code avote:}. Une panne de Redis dégrade les performances mais n'interrompt pas l'API.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    RedisCacheManagerBuilderCustomizer avoteCaches(JsonMapper mapper, AvoteProperties properties) {
        var types = mapper.getTypeFactory();
        JavaType page = types.constructParametricType(PageResponse.class, ParliamentarianSummary.class);
        JavaType groups = types.constructCollectionType(java.util.List.class, GroupDto.class);
        JavaType chambers = types.constructCollectionType(java.util.List.class, ChamberStats.class);
        JavaType detail = types.constructType(ParliamentarianDetail.class);

        return builder -> {
            builder.withCacheConfiguration(CACHE_LIST, configuration(mapper, page, properties));
            builder.withCacheConfiguration(CACHE_DETAIL, configuration(mapper, detail, properties));
            builder.withCacheConfiguration(CACHE_GROUPS, configuration(mapper, groups, properties));
            builder.withCacheConfiguration(CACHE_CHAMBERS, configuration(mapper, chambers, properties));
        };
    }

    private static RedisCacheConfiguration configuration(JsonMapper mapper, JavaType type,
                                                         AvoteProperties properties) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.cache().ttl())
                .disableCachingNullValues()
                .prefixCacheNameWith("avote:")
                .serializeValuesWith(SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(mapper, type)));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache {} illisible (clé {}) : {}", cache.getName(), key, e.toString());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Écriture impossible dans le cache {} : {}", cache.getName(), e.toString());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Éviction impossible dans le cache {} : {}", cache.getName(), e.toString());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Vidage impossible du cache {} : {}", cache.getName(), e.toString());
            }
        };
    }
}
