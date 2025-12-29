FROM openjdk:17-jdk-slim

# Установка ожидания для БД (опционально)
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Копируем JAR
COPY target/*.jar app.jar

# Копируем конфигурацию
COPY src/main/resources/application-docker.yml ./config/

EXPOSE 8080

# Запускаем с профилем docker
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=classpath:/,classpath:/config/", "--spring.profiles.active=docker"]