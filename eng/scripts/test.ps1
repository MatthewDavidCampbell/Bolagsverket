# Runs the tests only (no packaging).
$ErrorActionPreference = "Stop"
Push-Location (Join-Path $PSScriptRoot "../..")
try { ./mvnw.cmd -B test @args } finally { Pop-Location }
