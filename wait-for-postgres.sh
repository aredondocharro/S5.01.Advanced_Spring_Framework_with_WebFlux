#!/bin/sh

# Usa DB_HOST si está definido, si no usa postgres_blackjack por defecto
DB_HOST="${DB_HOST:-postgres_blackjack}"

echo "Esperando a PostgreSQL en $DB_HOST:5432..."

# Espera hasta que esté disponible
until nc -z "$DB_HOST" 5432; do
  echo "Esperando a $DB_HOST..."
  sleep 1
done

echo "PostgreSQL disponible! Arrancando app..."
exec java -jar app.jar