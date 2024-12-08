FROM eclipse-temurin:21
WORKDIR /app

# JAR 파일을 직접 복사
COPY build/libs/*.jar app.jar

CMD ["java", "-jar", "app.jar"]


EXPOSE 8080
EXPOSE 443
EXPOSE 80