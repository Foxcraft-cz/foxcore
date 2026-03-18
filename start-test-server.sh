#!/bin/bash

set -euo pipefail

SERVER_DIR="/home/KolegaDragan/Desktop/MC server/test-server"

cd "$SERVER_DIR"

JAVA_OPTS="-Xms1G -Xmx2G -XX:+UseG1GC"
JAR_FILE=$(ls paper-*.jar | head -n1)

exec java $JAVA_OPTS -jar "$JAR_FILE" nogui
