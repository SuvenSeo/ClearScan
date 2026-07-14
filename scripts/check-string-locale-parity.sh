#!/usr/bin/env bash
# Fails if values/strings.xml keys are missing from values-si or values-ta.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE="$ROOT/app/src/main/res/values/strings.xml"
SI="$ROOT/app/src/main/res/values-si/strings.xml"
TA="$ROOT/app/src/main/res/values-ta/strings.xml"

if [[ ! -f "$BASE" || ! -f "$SI" || ! -f "$TA" ]]; then
  echo "Missing strings.xml files under app/src/main/res/" >&2
  exit 1
fi

extract_keys() {
  # Emits sorted unique string and plurals resource names.
  grep -oE '<(string|plurals)[[:space:]]+name="[^"]+"' "$1" \
    | sed -E 's/.*name="([^"]+)".*/\1/' \
    | sort -u
}

BASE_KEYS="$(mktemp)"
SI_KEYS="$(mktemp)"
TA_KEYS="$(mktemp)"
trap 'rm -f "$BASE_KEYS" "$SI_KEYS" "$TA_KEYS"' EXIT

extract_keys "$BASE" >"$BASE_KEYS"
extract_keys "$SI" >"$SI_KEYS"
extract_keys "$TA" >"$TA_KEYS"

STATUS=0

SI_MISSING="$(comm -23 "$BASE_KEYS" "$SI_KEYS" || true)"
TA_MISSING="$(comm -23 "$BASE_KEYS" "$TA_KEYS" || true)"

if [[ -n "$SI_MISSING" ]]; then
  echo "values-si/strings.xml is missing keys present in values/strings.xml:"
  echo "$SI_MISSING" | sed 's/^/  /'
  STATUS=1
fi

if [[ -n "$TA_MISSING" ]]; then
  echo "values-ta/strings.xml is missing keys present in values/strings.xml:"
  echo "$TA_MISSING" | sed 's/^/  /'
  STATUS=1
fi

if [[ $STATUS -ne 0 ]]; then
  exit 1
fi

echo "Locale string parity check passed (si/ta cover all values/ keys)."
