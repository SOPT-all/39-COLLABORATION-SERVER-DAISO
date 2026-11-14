#!/usr/bin/env bash
# Generate a transaction from the existing seed without maintaining a second dataset.
set -euo pipefail
seed=$(cd "$(dirname "$0")" && pwd)/seed-dummy-data.sql
[[ $(grep -cx 'START TRANSACTION;' "$seed") == 1 && $(grep -cx 'COMMIT;' "$seed") == 1 ]] || exit 1
awk '
{ print }
$0 == "START TRANSACTION;" {
  print "DELETE FROM store_inventory;"
  print "DELETE FROM product;"
  print "DELETE FROM store;"
}' "$seed"
