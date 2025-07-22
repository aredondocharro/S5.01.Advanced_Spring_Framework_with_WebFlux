#!/bin/bash
# Usa DB_HOST como variable de entorno, con un valor por defecto
host="${DB_HOST:-dpg-d1vtrtili9vc73fscvug-a}" # Hostname interno de Render
port="5432"
# Opcional: Usa SPRING_R2DBC_URL para extraer el host si está disponible
if [ -n "$SPRING_R2DBC_URL" ]; then
    host=$(echo "$SPRING_R2DBC_URL" | sed -n 's/r2dbc:postgresql:\/\/\([^:]*\):.*/\1/p')
fi
while ! nc -zv "$host" "$port"; do
    echo "Waiting for PostgreSQL on $host:$port..."
    sleep 1
done
echo "PostgreSQL is up - continuing..."
# Ejecuta el comando pasado como argumento (por ejemplo, java -jar)
exec "$@"