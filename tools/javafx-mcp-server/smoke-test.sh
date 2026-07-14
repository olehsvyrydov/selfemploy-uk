#!/usr/bin/env bash
# Protocol smoke test for the JavaFX MCP server.
# Verifies initialize / tools/list / ping over stdio; with a display available,
# also verifies get_screen_info and that screenshot returns an MCP image block.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

find_jar() {
  ls "$DIR"/target/javafx-mcp-server-*.jar 2>/dev/null | grep -v original- | head -n1 || true
}

JAR="$(find_jar)"
if [[ -z "$JAR" ]]; then
  mvn -q -f "$DIR/pom.xml" package 1>&2
  JAR="$(find_jar)"
fi

REQUESTS='{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"smoke-test","version":"0"}}}
{"jsonrpc":"2.0","method":"notifications/initialized"}
{"jsonrpc":"2.0","id":2,"method":"ping"}
{"jsonrpc":"2.0","id":3,"method":"tools/list"}'

if [[ -n "${DISPLAY:-}" ]]; then
  REQUESTS+='
{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"get_screen_info","arguments":{}}}
{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"screenshot","arguments":{}}}'
fi

OUTPUT="$(printf '%s\n' "$REQUESTS" | timeout 60 java -jar "$JAR")"

fail() { echo "FAIL: $1" >&2; exit 1; }

# Note: herestrings, not `echo | grep -q` - grep -q closing the pipe early
# would trip pipefail via SIGPIPE on the multi-MB screenshot line.
grep -q '"id":1' <<< "$OUTPUT" || fail "no initialize response"
grep -q '"serverInfo"' <<< "$OUTPUT" || fail "initialize missing serverInfo"
grep -q '"id":2' <<< "$OUTPUT" || fail "no ping response"
grep -q '"tools":\[' <<< "$OUTPUT" || fail "tools/list missing tools array"
grep -q '"name":"screenshot"' <<< "$OUTPUT" || fail "screenshot tool not listed"
grep -q '"name":"list_windows"' <<< "$OUTPUT" || fail "list_windows tool not listed"

if [[ -n "${DISPLAY:-}" ]]; then
  grep -q 'screenWidth' <<< "$OUTPUT" || fail "get_screen_info failed"
  grep -q '"type":"image"' <<< "$OUTPUT" || fail "screenshot did not return an image content block"
fi

# Every response line must be valid single-line JSON-RPC
while IFS= read -r line; do
  echo "$line" | python3 -c 'import json,sys; d=json.load(sys.stdin); assert d.get("jsonrpc")=="2.0"' \
    || fail "invalid JSON-RPC line: $line"
done <<< "$OUTPUT"

echo "OK: all smoke tests passed"
