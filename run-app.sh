#!/usr/bin/env bash
# Runs the backend app — equivalent to the IntelliJ "ApplicationKt -Dev" run config
# (main class eu.gaelicgames.referee.ApplicationKt, JRE temurin-21,
#  VM param -Dio.ktor.development=true).
#
# The Gradle application plugin sets -Dio.ktor.development=true via
# applicationDefaultJvmArgs (build.gradle.kts: isDevelopment = true), so `gradlew run`
# already matches the Dev config's VM param.
set -euo pipefail

cd "$(dirname "$0")"

# Gradle launcher must run under JDK21 (system JDK25 fails with a cryptic "25.0.1" error).
JDK21="/Users/danielthiem/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home"
if [ -x /usr/libexec/java_home ]; then
  JDK21="$(/usr/libexec/java_home -v 21 2>/dev/null || echo "$JDK21")"
fi

if [ ! -d "$JDK21" ]; then
  echo "JDK21 not found at: $JDK21" >&2
  exit 1
fi

JAVA_HOME="$JDK21" exec ./gradlew run "$@"
