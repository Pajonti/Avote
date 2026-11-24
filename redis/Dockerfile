FROM redis:8-alpine

COPY redis.conf /usr/local/etc/redis/redis.conf
COPY docker-entrypoint.sh /usr/local/bin/avote-redis-entrypoint.sh
RUN chmod 0755 /usr/local/bin/avote-redis-entrypoint.sh

# Utilisateur non-root fourni par l'image de base ; /data lui appartient déjà.
USER redis
EXPOSE 6379
ENTRYPOINT ["avote-redis-entrypoint.sh"]
