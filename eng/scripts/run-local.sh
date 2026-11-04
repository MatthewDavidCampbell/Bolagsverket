#!/usr/bin/env sh
# Builds the service (without tests) and starts it on http://localhost:8080.
# Set RESTCOUNTRIES_API_KEY to import live data; without it the bundled snapshot is used.
set -e
cd "$(dirname "$0")/../.."
./mvnw -B -q -DskipTests package
exec java -jar service/target/service.jar "$@"
