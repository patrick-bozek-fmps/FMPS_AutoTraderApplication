#!/usr/bin/env bash
# FMPS AutoTrader Desktop UI launcher (macOS placeholder)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${REPO_ROOT}"
./gradlew :desktop-ui:runDesktopMac "$@"


