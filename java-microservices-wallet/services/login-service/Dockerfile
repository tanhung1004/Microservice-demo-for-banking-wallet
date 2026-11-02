# Build bằng Java 25 và Maven Wrapper của dự án.
FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Chuẩn hóa xuống dòng từ Windows sang Linux.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY src/ src/

# Đóng gói; kiểm thử tích hợp sẽ chạy sau khi cả hệ thống sẵn sàng.
RUN ./mvnw -B -DskipTests package


# Image chạy ứng dụng.
FROM eclipse-temurin:25-jre-noble

WORKDIR /app

# curl dùng cho healthcheck.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app app

COPY --from=build --chown=app:app /build/target/*.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"

USER app

ENTRYPOINT ["java", "-jar", "/app/app.jar"]