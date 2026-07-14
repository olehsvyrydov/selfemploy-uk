#!/usr/bin/env bash
#
# Runs the desktop app against the current working tree.
#
# `mvn -pl app javafx:run` on its own resolves common/core/hmrc-api/ui from the local Maven
# repository, NOT from the working tree, so edits to those modules are silently ignored and you end up
# running the last-installed jars. Adding -am does not help: with a direct goal invocation Maven would
# try to run javafx:run on every upstream module too. So install the upstream modules first, then run.
#
# Usage:
#   scripts/run.sh            # install upstream modules (skipping tests), then run the app
#   scripts/run.sh --tests    # run the tests during the install step

set -euo pipefail

cd "$(dirname "$0")/.."

MAVEN_ARGS=(-DskipTests)
if [[ "${1:-}" == "--tests" ]]; then
    MAVEN_ARGS=()
fi

echo "==> Installing upstream modules into the local repository"
mvn install -pl common,core,hmrc-api,ui,plugin-api,plugin-runtime -am "${MAVEN_ARGS[@]}"

echo "==> Launching the app"
mvn -pl app javafx:run
