#!/bin/bash

if [ -n "$SPRING_R2DBC_URL" ]; then
    host=$(echo "$SPRING_R2DBC_URL" | sed -n 's/r2dbc:postgresql:\/\/\([^:/]*\).*/\1/p')
elif [ -n "$DB_HOST" ]; then
    host="$DB_HOST"
else
    host="localhost"
fi

port="5432"

echo "Esperando conexión a PostgreSQL en $host:$port..."
while ! nc -zv "$host" "$port"; do
    echo "Esperando a PostgreSQL..."
    sleep 1
done
echo "PostgreSQL disponible, arrancando app"
exec "$@"