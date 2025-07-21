#!/bin/sh
echo "Esperando a MySQL..."

while ! nc -z mysql_blackjack 3306; do
  sleep 1
done

echo "MySQL disponible, arrancando app"
exec java -jar app.jar