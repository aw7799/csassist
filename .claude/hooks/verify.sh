#!/usr/bin/env bash
set -e
if git diff --name-only | grep -q '^kb-mcp/'; then
  (cd kb-mcp && npm run -s build && npm test) || true
fi
if git diff --name-only | grep -q '^service/'; then
  (cd service && ./mvnw -q test) || true
fi
