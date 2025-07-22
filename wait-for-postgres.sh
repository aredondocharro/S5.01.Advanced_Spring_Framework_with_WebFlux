#!/bin/bash

# Intenta extraer el host desde la URL R2DBC (si existe)
if [ -n "$SPRING_R2DBC_URL" ]; then
    host=$(echo "$SPRING_R2DBC_URL" | sed -n 's/r2dbc:postgresql:\/\/\([^:/]*\).*/\1/p')
else
    host="${DB_HOST:-localhost}"
fi

port="5432"

echo "Esperando conexión a PostgreSQL en $host:$port..."
while ! nc -zv "$host" "$port"; do
    echo "Esperando a PostgreSQL..."
    sleep 1
done
echo "PostgreSQL disponible, arrancando app"
exec "$@"