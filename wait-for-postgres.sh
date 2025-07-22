#!/bin/sh
echo "Esperando a PostgreSQL..."

while ! nc -z postgres_blackjack 5432; do
  sleep 1
done

echo "PostgreSQL disponible, arrancando app"
exec java -jar app.jar