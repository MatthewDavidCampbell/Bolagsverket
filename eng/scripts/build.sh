#!/usr/bin/env sh
# Builds all modules and runs every test.
set -e
cd "$(dirname "$0")/../.."
./mvnw -B verify "$@"
