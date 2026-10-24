# The online game server.
#   docker build -t dominoes-server .
#   docker run -p 8080:8080 -v dominoes-data:/data dominoes-server
#
# The build stage runs on the build machine's own architecture (the result is plain
# JVM bytecode), so multi-arch images (amd64 + arm64) build quickly without emulation.
FROM --platform=$BUILDPLATFORM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY . .
ENV DOMINO_SERVER_ONLY=1
# Release version reported by GET /version (set by the release workflow).
ARG APP_VERSION=dev
RUN ./gradlew --no-daemon :server:installDist -PappVersion="$APP_VERSION"

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/server/build/install/server /app
# Run as an unprivileged user; the database lives on the /data volume.
RUN useradd --system --uid 10001 --home /data dominoes && mkdir -p /data && chown dominoes /data
USER dominoes
ENV PORT=8080 DATABASE=/data/domino.db
VOLUME /data
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /health HTTP/1.0\r\n\r\n" >&3 && grep -q ok <&3'
CMD ["/app/bin/server"]
