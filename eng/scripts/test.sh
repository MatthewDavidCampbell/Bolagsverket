#!/usr/bin/env sh
# Runs the tests only (no packaging).
set -e
cd "$(dirname "$0")/../.."
./mvnw -B test "$@"
