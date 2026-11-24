FROM postgres:18-alpine

COPY postgresql.conf /etc/postgresql/postgresql.conf

# Utilisateur non-root fourni par l'image de base ; le volume de données lui appartient.
USER postgres
EXPOSE 5432
CMD ["postgres", "-c", "config_file=/etc/postgresql/postgresql.conf"]
