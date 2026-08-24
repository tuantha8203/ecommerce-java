FROM eclipse-temurin:21-jdk-alpine AS build

ARG HTTP_PROXY
ARG HTTPS_PROXY
ARG NO_PROXY

WORKDIR /build

RUN apk add --no-cache wget

# Maven không tự đọc biến HTTP_PROXY/HTTPS_PROXY như wget/curl/apk — phải khai proxy qua ~/.m2/settings.xml.
# Chỉ tạo file này khi có proxy; máy build không ở sau proxy thì bỏ qua, không ảnh hưởng gì.
RUN set -eu; \
    PROXY_URL="${HTTPS_PROXY:-${HTTP_PROXY:-}}"; \
    if [ -n "$PROXY_URL" ]; then \
      NO_SCHEME="${PROXY_URL#*://}"; \
      PROXY_HOST="${NO_SCHEME%%:*}"; \
      PROXY_PORT="${NO_SCHEME##*:}"; \
      NON_PROXY="$(printf '%s' "${NO_PROXY:-}" | tr ',' '|')"; \
      mkdir -p /root/.m2; \
      printf '%s\n' \
        '<settings>' \
        '  <proxies>' \
        "    <proxy><id>http-proxy</id><active>true</active><protocol>http</protocol><host>${PROXY_HOST}</host><port>${PROXY_PORT}</port><nonProxyHosts>${NON_PROXY}</nonProxyHosts></proxy>" \
        "    <proxy><id>https-proxy</id><active>true</active><protocol>https</protocol><host>${PROXY_HOST}</host><port>${PROXY_PORT}</port><nonProxyHosts>${NON_PROXY}</nonProxyHosts></proxy>" \
        '  </proxies>' \
        '</settings>' \
        > /root/.m2/settings.xml; \
    fi

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

COPY services/eureka-server/pom.xml services/eureka-server/pom.xml
COPY services/config-server/pom.xml services/config-server/pom.xml
COPY services/inventory-service/pom.xml services/inventory-service/pom.xml
COPY services/order-service/pom.xml services/order-service/pom.xml
COPY services/payment-service/pom.xml services/payment-service/pom.xml
COPY services/notification-service/pom.xml services/notification-service/pom.xml
COPY services/search-service/pom.xml services/search-service/pom.xml
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml

COPY services/product-service/pom.xml services/product-service/pom.xml
COPY services/product-service/src services/product-service/src

RUN chmod +x mvnw && ./mvnw -pl services/product-service -am -DskipTests package


FROM eclipse-temurin:21-jre-alpine

ARG HTTP_PROXY
ARG HTTPS_PROXY
ARG NO_PROXY

RUN apk add --no-cache curl

WORKDIR /app

COPY --from=build /build/services/product-service/target/product-service.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]