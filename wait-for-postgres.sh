#!/bin/sh
echo "Waiting PostgreSQL..."

while ! nc -z postgres_blackjack 5432; do
  sleep 1
done

echo "PostgreSQL ready, running the api"
exec java -jar app.jar