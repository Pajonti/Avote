#!/bin/sh
set -eu

# Refuse de démarrer sans mot de passe : ce Redis écoute sur le réseau des conteneurs.
if [ -z "${REDIS_PASSWORD:-}" ]; then
  echo "REDIS_PASSWORD est obligatoire" >&2
  exit 1
fi

exec redis-server /usr/local/etc/redis/redis.conf --requirepass "$REDIS_PASSWORD" "$@"
