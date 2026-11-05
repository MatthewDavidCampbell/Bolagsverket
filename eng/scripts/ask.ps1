# Chat with the service in Swedish or English (bonus).
#   eng\scripts\ask.ps1 --setup        once: model, tokenizer and Ollama (asks Yes/No first)
#   eng\scripts\ask.ps1 --user alice   chat; the service must be running (see README)
$ErrorActionPreference = "Stop"
$root = Join-Path $PSScriptRoot "../.."
if (-not (Test-Path (Join-Path $root "ai/target/ai.jar"))) {
    Push-Location $root
    try { ./mvnw.cmd -B -q -DskipTests -pl ai -am package } finally { Pop-Location }
}
java --enable-native-access=ALL-UNNAMED -jar (Join-Path $root "ai/target/ai.jar") @args
exit $LASTEXITCODE
