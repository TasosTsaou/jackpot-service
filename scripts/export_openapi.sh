#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="$ROOT_DIR/docs/api"
mkdir -p "$OUT_DIR"

echo "Exporting OpenAPI spec from http://localhost:8080/v3/api-docs ..."
curl -sS http://localhost:8080/v3/api-docs > "$OUT_DIR/openapi.json"
echo "Wrote $OUT_DIR/openapi.json"

