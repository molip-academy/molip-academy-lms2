# JDK 25를 명시적으로 고정한다. Gradle 툴체인이 25를 요구하는데, 빌더가 제공하는
# JDK 버전에 기대면 플랫폼 기본값이 바뀔 때 조용히 깨진다.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# 의존성 해석 결과를 캐시에 남기기 위해 빌드 스크립트를 먼저 복사한다.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --configuration runtimeClasspath > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:25-jre
WORKDIR /app

# 루트로 돌리지 않는다.
RUN useradd --system --create-home --shell /usr/sbin/nologin molip
USER molip

COPY --from=build /workspace/build/libs/*.jar app.jar

# Railway가 PORT를 주입한다. application-prod.yml이 그 값을 읽는다.
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
