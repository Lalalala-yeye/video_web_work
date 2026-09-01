#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
LABEL="${1:?usage: ./run.sh mono|micro [base_url]}"
BASE="${2:-http://127.0.0.1:8081}"
exec node ./run.mjs --label "$LABEL" --base "$BASE"
