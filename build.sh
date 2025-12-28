#!/bin/bash
# build.sh - Полная сборка проекта

echo "=== Начало сборки wallet-service ==="

# 1. Очистка
echo "1. Очистка проекта..."
mvn clean

# 2. Компиляция
echo "2. Компиляция..."
mvn compile

# 3. Применение миграций БД
echo "3. Применение миграций базы данных..."
mvn liquibase:update -Dliquibase.url=jdbc:postgresql://localhost:5432/walletdb \
                     -Dliquibase.username=walletuser \
                     -Dliquibase.password=walletpass

# 4. Запуск тестов
echo "4. Запуск тестов..."
mvn test -Ptest

# 5. Сборка JAR
echo "5. Сборка JAR файла..."
mvn package -DskipTests

# 6. Сборка Docker образа (опционально)
if [ "$1" == "--docker" ]; then
    echo "6. Сборка Docker образа..."
    mvn docker:build -Pdocker
fi

# 7. Запуск приложения
if [ "$1" == "--run" ]; then
    echo "7. Запуск приложения..."
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
fi

echo "=== Сборка завершена ==="