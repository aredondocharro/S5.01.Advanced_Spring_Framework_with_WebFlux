# ---------- ETAPA 1: COMPILACIÓN ----------
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiamos el POM y descargamos dependencias primero (para aprovechar la caché de Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Ahora copiamos el resto del código fuente y compilamos
COPY src ./src
RUN mvn clean package -DskipTests

# ---------- ETAPA 2: EJECUCIÓN ----------
FROM eclipse-temurin:21-jdk

WORKDIR /app

# ✅ Instala netcat-openbsd en vez de netcat (más compatible)
RUN apt-get update && apt-get install -y netcat-openbsd && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/*.jar app.jar
COPY wait-for-mysql.sh ./wait-for-mysql.sh
RUN chmod +x ./wait-for-mysql.sh

EXPOSE 8080

ENTRYPOINT ["./wait-for-mysql.sh"]

