# Builds all modules and runs every test.
$ErrorActionPreference = "Stop"
Push-Location (Join-Path $PSScriptRoot "../..")
try { ./mvnw.cmd -B verify @args } finally { Pop-Location }
