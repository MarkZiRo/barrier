FROM eclipse-temurin:21 AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && \
    ./gradlew bootJar && \
    mv build/libs/*.jar app.jar

# 두 번째 스테이지: 실행
FROM eclipse-temurin:21
WORKDIR /app
COPY --from=build /app/app.jar .

CMD ["java", "-jar", "app.jar"]

EXPOSE 8080
EXPOSE 443
EXPOSE 80