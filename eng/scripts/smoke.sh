#!/usr/bin/env sh
# Calls a running service the way eng/http does and checks each status code.
# Usage: eng/scripts/smoke.sh [base-url]   (default http://localhost:8080)
set -eu
BASE="${1:-http://localhost:8080}"
USER_ID="smoke-$(date +%s)"
FAILED=0

# Wait up to 60 seconds for the readiness probe.
i=0
until curl -fs "$BASE/actuator/health/readiness" >/dev/null 2>&1; do
  i=$((i + 1))
  if [ "$i" -ge 60 ]; then echo "App not ready at $BASE"; exit 1; fi
  sleep 1
done

check() { # expected-status method path [body] [with-user]
  expected="$1"; method="$2"; path="$3"; body="${4:-}"; user="${5:-yes}"
  set -- -s -o /dev/null -w "%{http_code}" -X "$method"
  [ "$user" = "yes" ] && set -- "$@" -H "X-User-Id: $USER_ID"
  [ -n "$body" ] && set -- "$@" -H "Content-Type: application/json" -d "$body"
  actual="$(curl "$@" "$BASE$path")"
  if [ "$actual" = "$expected" ]; then
    echo "ok    $expected $method $path"
  else
    echo "FAIL  $method $path: expected $expected, got $actual"; FAILED=1
  fi
}

# Countries (brief, user stories 1 and 2)
check 200 GET  "/api/countries"
check 200 GET  "/api/countries?region=europe&sort=population&direction=desc&page=0&size=5"
check 400 GET  "/api/countries?size=0"
check 200 GET  "/api/countries/SWE"
check 404 GET  "/api/countries/XYZ"

# Visits (brief, user story 3)
check 400 GET    "/api/visits" "" no
check 201 POST   "/api/visits" '{"countryCode":"SWE","note":"Midsummer in Dalarna"}'
check 409 POST   "/api/visits" '{"countryCode":"SWE"}'
check 400 POST   "/api/visits" '{"countryCode":"XYZ"}'
check 200 GET    "/api/visits"
check 200 GET    "/api/visits/SWE"
check 200 PUT    "/api/visits/SWE" '{"note":"Fika in Uppsala"}'
check 204 DELETE "/api/visits/SWE"
check 404 DELETE "/api/visits/SWE"

# Documentation
check 200 GET "/v3/api-docs"
check 200 GET "/swagger-ui/index.html"

exit "$FAILED"
