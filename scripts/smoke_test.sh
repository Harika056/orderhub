#!/usr/bin/env bash
# Places one order through the gateway and waits for the Kafka saga to confirm it.
set -euo pipefail

BASE="${1:-http://localhost:8080}"

echo "Placing an order..."
RESP=$(curl -sS -X POST "$BASE/orders" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: smoke-$(date +%s)" \
  -d '{"sku":"SKU-1","quantity":2,"amount":49.90}')
echo "$RESP"

ID=$(echo "$RESP" | python3 -c 'import sys, json; print(json.load(sys.stdin)["id"])')

for i in $(seq 1 20); do
  STATUS=$(curl -sS "$BASE/orders/$ID" | python3 -c 'import sys, json; print(json.load(sys.stdin)["status"])')
  echo "check $i: $STATUS"
  if [ "$STATUS" = "CONFIRMED" ]; then
    echo "OK: order $ID was confirmed through inventory and payment."
    exit 0
  fi
  sleep 1
done

echo "Order $ID did not reach CONFIRMED within 20 seconds." >&2
exit 1
