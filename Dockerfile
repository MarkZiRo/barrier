FROM eclipse-temurin:21

WORKDIR /app
COPY . .

# gradlew에 실행 권한을 부여하고 빌드 실행
RUN chmod +x ./gradlew && \
    ./gradlew bootJar && \
    mv build/libs/*.jar app.jar

FROM eclipse-temurin:21
WORKDIR /app
COPY --from=build /app/app.jar .

CMD ["java", "-jar", "app.jar"]

EXPOSE 8080
EXPOSE 443
EXPOSE 80
