# Countries and visits API

A REST API that lists the world's countries and lets users keep track of the countries they
have visited, with a short note per visit. Built as a work sample for Bolagsverket
("Arbetsprov, IT-specialist systemutveckling", 2026-11-03).

## The task in short

- **Countries at first start:** on start-up, import every country from
  [REST Countries](https://restcountries.com) into the application's own storage.
- **List countries:** an endpoint with paging, a region filter and sorting by name or
  population, ascending or descending, answered from that storage and never by calling REST
  Countries.
- **Visited countries:** mark and unmark countries as visited per user, with a short note:
  create, read, update and delete, and list a user's visits with their notes. A mocked user id
  is enough.
- **Browser testing:** Swagger, so every endpoint can be tried in a browser.

I gave myself a 4-hour time box, so some things are deliberately left as documented extension
points rather than built; see [Design choices](#design-choices) and
[Known limitations](#known-limitations).

I come from C# and .NET; [Blueprint.md](Blueprint.md) maps every Java, Spring and Maven idea
used here to its .NET counterpart.

## Run it

You need **JDK 25**, or only **Docker**. Maven comes with the repository (`mvnw`).

| What | Linux / macOS | Windows (PowerShell) |
|---|---|---|
| Build and run all tests | `eng/scripts/build.sh` | `eng\scripts\build.ps1` |
| Tests only | `eng/scripts/test.sh` | `eng\scripts\test.ps1` |
| Start the app on port 8080 | `eng/scripts/run-local.sh` | `eng\scripts\run-local.ps1` |
| Check a running app | `eng/scripts/smoke.sh` | (run it from Git Bash) |

Or with Docker:

```bash
docker build -f service/Dockerfile -t countries-api .
docker run --rm -p 8080:8080 countries-api
```

The build context is the repository root because the image compiles the service together with
the client libraries it uses.

Then open **http://localhost:8080**. It takes you to Swagger UI at `/swagger-ui.html`. Visits
calls need the header `X-User-Id`, for example `alice`; Swagger has a field for it. The same
calls are also in [`eng/http`](eng/http) for IntelliJ or VS Code's REST Client.

### REST Countries API key (optional)

REST Countries v1 to v4 have been shut down. v5 needs an API key, which is free on sign-up at
restcountries.com. Without a key, the app starts from a **snapshot bundled in the code**
(250 countries, taken 2026-11-04) and logs a warning saying so. To import live data:

```bash
export RESTCOUNTRIES_API_KEY=<your key>          # PowerShell: $env:RESTCOUNTRIES_API_KEY="<your key>"
eng/scripts/run-local.sh
# or: docker run --rm -p 8080:8080 -e RESTCOUNTRIES_API_KEY=<your key> countries-api
```

The snapshot is also the fallback if REST Countries can't be reached at start-up, so the app
always starts.

## The API

| Method and path | What it does |
|---|---|
| `GET /api/countries?region=&sort=name\|population&direction=asc\|desc&page=0&size=20` | One page of countries. Parameter values ignore letter case. |
| `GET /api/countries/{code}` | One country by ISO alpha-3 code, for example `SWE` |
| `POST /api/visits` `{"countryCode":"SWE","note":"..."}` | Mark a country as visited; returns 201 with `Location` |
| `GET /api/visits` | My visits, ordered by code, with country name and note |
| `GET /api/visits/{code}` | One of my visits |
| `PUT /api/visits/{code}` `{"note":"..."}` | Change the note; an empty note removes it |
| `DELETE /api/visits/{code}` | Unmark; returns 204, or 404 if it wasn't marked |

Errors are [RFC 9457 problem details](https://www.rfc-editor.org/rfc/rfc9457): 400 for invalid
input or an unknown country, 404 for not found, 409 for a country that is already marked.
Health probes are at `/actuator/health/liveness` and `/actuator/health/readiness`.

## Assumptions

- **Spring Boot.** I assume Bolagsverket builds Java services with Spring Boot, so the API
  uses it.
- **Docker, Kubernetes and REST.** I assume services run as containers in Kubernetes and talk
  to each other over REST rather than gRPC. That is why there is one Dockerfile, readiness
  that waits for the import, and service libraries that look like inter-service clients.
- **The user id.** `X-User-Id` stands in for a user taken from an authentication token. No
  real authentication is done, as the task allows.
- **No visit date.** A visit has a note and created/updated times, but no visit date, because
  the task doesn't ask for one.

## Design choices

### Structure: a mono repository of client libraries

```
core        HTTP pipeline shared by the client libraries (no Spring)
countries   CountriesClient: start-up import + in-memory country cache (no Spring)
visits      VisitsClient: visits per user in an in-memory cache (no Spring)
service     Spring Boot REST API: maps HTTP onto the two clients (Dockerfile here)
```

Dependencies only point downwards: `service → countries, visits → core`. Only `service` knows
about Spring. The libraries follow the pattern of the Azure SDK: a module per service, a
`<Service>Client` built from options, and a shared core.

**Why:** in a landscape of many services, each service would publish a small client library
that other services use. This repository is meant for those clients. Here both backing
"services" run inside one process, but each sits behind its own client, so either could be
split out later without its callers changing.

The `service` module is the exception. It is a deployable REST service, not a client library,
and in a real setup it would live in a repository of its own and depend on published versions
of the clients. It stays here so the work sample is one repository to build and run.

### `core`: a synchronous HTTP pipeline

A request passes through ordered **policies** down to a **transport** (the JDK's
`java.net.http.HttpClient`). The response passes back up through the same policies, and
JSON is turned into typed results.

- **`AuthorizationPolicy` is real.** It sends the REST Countries key as a bearer token.
- **`RetryPolicy` is a documented skeleton.** It names its strategies (exponential backoff,
  fixed delay) but passes requests through unchanged.

The point is to show where cross-cutting concerns plug in, such as retry, auth, logging and
tracing, without building more than the task needs.

**Why synchronous:** the app runs on **virtual threads**
(`spring.threads.virtual.enabled`), so blocking code is cheap and stays simple. Spring MVC
plus virtual threads gives the scalability that reactive code is usually chosen for, without
mixing two concurrency models. Microsoft's next-generation Java core library takes the same
direction.

### `countries`: a cache, not a proxy

- **Import:** at start-up, `CountriesClient.load()` pages through REST Countries v5, 100 per
  call, so three calls in all.
- **Reshaping:** the nested v5 objects become a flat `Country` record. Entries without an ISO
  alpha-3 code (4 partially recognised territories) are skipped.
- **Atomic swap:** the result goes into an in-memory cache in one step.
- **Reads:** queries are Java Streams (filter, sort, page) over an immutable list. Sorting by
  name uses a collator, so "Åland Islands" sorts with the A's.
- **Readiness:** the import runs before Spring Boot reports the app ready, so Kubernetes would
  send no traffic to an empty cache.
- **No partial data:** if any page fails, the whole import is replaced by the snapshot.

### `visits`: per-user cache with atomic operations

- **Storage:** visits are kept in a `ConcurrentHashMap` per user. Create, update and delete
  are each a single atomic map operation (`putIfAbsent`, `computeIfPresent`, `remove`), so
  concurrent requests need no locks.
- **Country check without coupling:** `visits` checks that a country exists through a
  one-method `CountryDirectory` interface, which the service wires to the countries cache. So
  `visits` doesn't depend on `countries`.
- **Backend option:** the backend is chosen in configuration (`visits.backend: in-process`).
  Only the in-process backend exists. A remote visits service would be a second backend
  behind the same client.

### Engineering

- **One Maven build:** the root `pom.xml` holds the shared conventions (Java 25, dependency
  versions from the Spring Boot BOM, compiler and test settings), and each module only adds
  what it needs.
- **Why not Gradle:** I chose Maven as the most common choice for Spring services. Gradle,
  with convention plugins, would suit centralising build logic across many repositories in
  CI/CD.
- **Scripts:** `eng/` holds the build, test, run and smoke scripts and the `.http` files.
- **CI:** [`.github/workflows/ci.yml`](.github/workflows/ci.yml) builds, runs all tests,
  starts the app, runs the smoke checks against it and builds the Docker image.
- **Tests:**
  - 96 tests in all: unit tests for each library, and end-to-end tests that start the whole
    app on a random port.
  - REST Countries is replaced by WireMock or the snapshot, so no test calls the real service.

## Known limitations

- **Data is lost on restart.** Visits live in memory. That's fine for the task, which allows
  an in-memory store.
- **Replicas don't share data.** With more than one replica, each pod would have its own
  visits. A real deployment would use a shared store (for example PostgreSQL), or move visits
  to a service of its own behind the remote backend.
- **Retry is a skeleton.** If REST Countries fails at start-up, the app uses the snapshot
  rather than retrying.
- **No real authentication.** `X-User-Id` is trusted as given.
- **The snapshot can go stale.** It is refreshed by hand.
