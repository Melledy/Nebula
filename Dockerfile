# Build Nebula.jar
FROM eclipse-temurin:21-jdk-alpine-3.23 AS build
WORKDIR /build
RUN apk add --no-cache gradle
COPY . .
RUN gradle jar

# Grab latest resources
FROM alpine:3.23 AS resources
WORKDIR /build
RUN apk add --no-cache git wget
RUN git clone --depth=1 https://github.com/Hiro420/StellaSoraData
RUN wget https://nova-static.stellasora.global/meta/and.html
RUN wget https://nova-static.stellasora.global/meta/win.html

# Final image
# Resources are included in the image for the sake of convenience.
FROM eclipse-temurin:25-alpine-3.23
WORKDIR /app
EXPOSE 80
RUN apk add --no-cache curl
HEALTHCHECK --interval=5s --timeout=15s --retries=3 --start-period=10s CMD [ "curl", "http://127.0.0.1:80" ]
RUN mkdir /app/resources && mkdir /app/web && mkdir /app/web/meta
COPY --from=build /build/Nebula.jar .
COPY --from=resources /build/StellaSoraData/EN/bin/ /app/resources/bin/
COPY --from=resources /build/StellaSoraData/EN/language/ /app/resources/language/
COPY --from=resources /build/and.html /app/web/meta/and.html
COPY --from=resources /build/win.html /app/web/meta/win.html
CMD [ "java", "-jar", "/app/Nebula.jar", "-nohandbook" ]