FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S -g 10001 spring && adduser -S -u 10001 spring -G spring
USER spring:spring
WORKDIR /app
COPY --chown=spring:spring app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
