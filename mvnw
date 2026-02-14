#!/bin/sh

set -e

BASE_DIR=$(cd "$(dirname "$0")" && pwd)
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/maven-wrapper.jar"
WRAPPER_PROPERTIES="$WRAPPER_DIR/maven-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
  if [ -f "$WRAPPER_DIR/MavenWrapperDownloader.java" ]; then
    javac "$WRAPPER_DIR/MavenWrapperDownloader.java"
    java -Dmaven.multiModuleProjectDirectory="$BASE_DIR" -cp "$WRAPPER_DIR" MavenWrapperDownloader
  fi
fi

exec java -Dmaven.multiModuleProjectDirectory="$BASE_DIR" -DwrapperProperties="$WRAPPER_PROPERTIES" -classpath "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
