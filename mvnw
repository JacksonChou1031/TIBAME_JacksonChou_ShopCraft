#!/bin/sh
set -eu

MAVEN_PROJECTBASEDIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
  JAVA_EXE="${JAVA_HOME}/bin/java"
else
  JAVA_EXE="java"
fi

exec "$JAVA_EXE" ${MAVEN_OPTS:-} ${MAVEN_ARGS:-} \
  -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" \
  -classpath "${MAVEN_PROJECTBASEDIR}/.mvn/wrapper/maven-wrapper.jar" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"
