#!/usr/bin/env sh
set -eu
GRADLE_VERSION=8.12.1
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
CACHE="$ROOT/.gradle-local"
DIST="$CACHE/gradle-$GRADLE_VERSION"
ZIP="$CACHE/gradle-$GRADLE_VERSION-bin.zip"
if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$CACHE"
  echo "[Liminal Space] Descargando Gradle $GRADLE_VERSION..."
  curl -L "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
  unzip -q -o "$ZIP" -d "$CACHE"
fi
exec "$DIST/bin/gradle" "$@"
