# Builds the service (without tests) and starts it on http://localhost:8080.
# Set $env:RESTCOUNTRIES_API_KEY to import live data; without it the bundled snapshot is used.
$ErrorActionPreference = "Stop"
Push-Location (Join-Path $PSScriptRoot "../..")
try {
    ./mvnw.cmd -B -q -DskipTests package
    java -jar service/target/service.jar @args
} finally { Pop-Location }
