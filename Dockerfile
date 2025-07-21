# ---------- Build Stage ----------
FROM maven:3.9.7-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos SOLO el módulo users-service
COPY users-service/pom.xml users-service/
RUN mvn -f users-service/pom.xml dependency:go-offline -B

# Copiamos el código del módulo
COPY users-service/ users-service/
RUN mvn -f users-service/pom.xml -B package -DskipTests

# ---------- Runtime Stage ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiamos el jar del módulo users-service
COPY --from=build /app/users-service/target/users-service-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar","--spring.profiles.active=prod"]
