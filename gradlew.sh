#!/usr/bin/env sh
# Lightweight wrapper to fetch Gradle if not present (CI-friendly)
set -e
if [ -f "./gradle/wrapper/gradle-wrapper.jar" ]; then
  JAVA_OPTS=${JAVA_OPTS:-"-Xmx2g"}
  exec java $JAVA_OPTS -jar "./gradle/wrapper/gradle-wrapper.jar" "$@"
else
  echo "Gradle wrapper jar missing. Downloading..."
  mkdir -p gradle/wrapper
  curl -L -o gradle/wrapper/gradle-wrapper.jar https://repo1.maven.org/maven2/org/gradle/gradle-wrapper/8.6/gradle-wrapper-8.6.jar
  JAVA_OPTS=${JAVA_OPTS:-"-Xmx2g"}
  exec java $JAVA_OPTS -jar "./gradle/wrapper/gradle-wrapper.jar" "$@"
fi
