FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml ./
COPY src ./src

# Force spring-boot repackage because pom currently has skip=true.
RUN mvn -B -DskipTests clean package spring-boot:repackage -Dspring-boot.repackage.skip=false
RUN JAR_FILE=$(ls target/*.jar | grep -v "original" | head -n 1) && cp "$JAR_FILE" /build/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app

ENV TZ=Asia/Shanghai
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m"

COPY --from=builder /build/app.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
