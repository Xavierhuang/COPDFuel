#!/bin/bash
# Removes exposed API keys from all files in the current tree (for git filter-branch).
set -e
GOOGLE_KEY="AIzaSyAr_cwXKSSXfegotjm1nwFf6PdFuw6QHyI"
USDA_KEY="RFO3h9yHwHQa7hgnVMM5dmJetqevrZCyq3hYkhax"
find . -type f ! -path './.git/*' 2>/dev/null | while read -r f; do
  [ -f "$f" ] || continue
  if grep -q "$GOOGLE_KEY" "$f" 2>/dev/null; then
    sed -i.bak "s/$GOOGLE_KEY/[REDACTED_GOOGLE_PLACES_KEY]/g" "$f"
    rm -f "${f}.bak"
  fi
  if grep -q "$USDA_KEY" "$f" 2>/dev/null; then
    sed -i.bak "s/$USDA_KEY/[REDACTED_USDA_API_KEY]/g" "$f"
    rm -f "${f}.bak"
  fi
done
