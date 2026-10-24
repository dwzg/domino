# The online game server. Build and run:
#   docker build -t dominoes-server .
#   docker run -p 8080:8080 -v dominoes-data:/data dominoes-server
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY . .
ENV DOMINO_SERVER_ONLY=1
RUN ./gradlew --no-daemon :server:installDist

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/server/build/install/server /app
# Keep the database on a volume so games survive restarts and deploys.
ENV PORT=8080 DATABASE=/data/domino.db
VOLUME /data
EXPOSE 8080
CMD ["/app/bin/server"]
