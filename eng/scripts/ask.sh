#!/usr/bin/env sh
# Chat with the service in Swedish or English (bonus).
#   eng/scripts/ask.sh --setup        once: model, tokenizer and Ollama (asks Yes/No first)
#   eng/scripts/ask.sh --user alice   chat; the service must be running (see README)
set -e
cd "$(dirname "$0")/../.."
if [ ! -f ai/target/ai.jar ]; then
  ./mvnw -B -q -DskipTests -pl ai -am package
fi
exec java --enable-native-access=ALL-UNNAMED -jar ai/target/ai.jar "$@"
