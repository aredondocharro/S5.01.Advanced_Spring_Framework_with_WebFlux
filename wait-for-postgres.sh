#!/bin/bash

if [ -n "$SPRING_R2DBC_URL" ]; then
    host=$(echo "$SPRING_R2DBC_URL" | sed -n 's/r2dbc:postgresql:\/\/\([^:/]*\).*/\1/p')
elif [ -n "$DB_HOST" ]; then
    host="$DB_HOST"
else
    host="localhost"
fi

port="5432"

echo "Waiting for a PostgreSQL conection at $host:$port..."
while ! nc -zv "$host" "$port"; do
    echo "Waiting for PostgreSQL..."
    sleep 1
done
echo "PostgreSQL is able, running app"
exec "$@"