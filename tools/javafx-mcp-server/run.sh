#!/usr/bin/env bash
# MCP stdio launcher: builds the server on first use (or after source changes),
# then execs it. All build output goes to stderr - stdout is the MCP channel.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

find_jar() {
  ls "$DIR"/target/javafx-mcp-server-*.jar 2>/dev/null | grep -v original- | head -n1 || true
}

JAR="$(find_jar)"
if [[ -z "$JAR" || -n "$(find "$DIR/src" "$DIR/pom.xml" -newer "$JAR" -print -quit 2>/dev/null)" ]]; then
  echo "[javafx-mcp-server] building..." >&2
  mvn -q -f "$DIR/pom.xml" package 1>&2
  JAR="$(find_jar)"
fi

[[ -n "$JAR" ]] || { echo "[javafx-mcp-server] build produced no jar" >&2; exit 1; }
exec java -jar "$JAR"
