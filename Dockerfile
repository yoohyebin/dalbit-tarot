# 빌드된 jar 을 담기만 한다. 컴파일과 테스트는 Gradle 이 바깥에서 한다.
#
# 컨테이너 안에서 Gradle 을 돌리면 배포판과 의존성을 매번 새로 받아 15분 넘게 걸리고,
# CI 가 이미 같은 빌드를 하므로 두 번 하는 셈이 된다.
#
# 빌드 전에 ./gradlew bootJar 을 먼저 돌려야 한다.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# root 로 돌리지 않는다
RUN addgroup -S dalbit && adduser -S dalbit -G dalbit \
    && mkdir -p /var/log/dalbit-tarot \
    && chown -R dalbit:dalbit /var/log/dalbit-tarot

ARG JAR_FILE=build/libs/*.jar
COPY --chown=dalbit:dalbit ${JAR_FILE} app.jar

USER dalbit
EXPOSE 8080

# t3.micro 는 메모리가 1GB 라 힙 상한을 비율로 건다
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -Duser.timezone=Asia/Seoul"
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
