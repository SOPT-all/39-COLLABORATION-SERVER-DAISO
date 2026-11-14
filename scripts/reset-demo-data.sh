#!/usr/bin/env bash
# Reset only the three demo tables; keep schema, Flyway history and deployed image.
set -euo pipefail
[[ ${1:-} == --confirmed ]] || { echo 'Explicit reset confirmation required.' >&2; exit 1; }
umask 077
source_dir=$(cd "$(dirname "$0")" && pwd)
base=${DEPLOY_ROOT:-/opt/daiso}
[[ -f $base/.env && ! -L $base/.env ]] || exit 1
[[ $(stat -c %a "$base/.env") == 600 && $(stat -c %u "$base/.env") == "$EUID" ]] || exit 1
exec 9>"$base/state/deploy.lock"
flock -n 9 || { echo 'Deployment or reset already running.' >&2; exit 1; }
# Use the running release's configuration, not a new image/configuration from main.
release=$(cat "$base/state/current")
BACKEND_IMAGE=$(cat "$base/state/image")
export BACKEND_IMAGE
compose=(docker compose -p daiso-prod --env-file "$base/.env" -f "$release/docker-compose.prod.yml")
"${compose[@]}" config --quiet
for service in mysql backend; do
  container=$("${compose[@]}" ps -q "$service")
  [[ -n $container && $(docker inspect --format '{{.State.Health.Status}}' "$container") == healthy ]] || {
    echo "$service must already be healthy." >&2; exit 1;
  }
done
resume_app() {
  "${compose[@]}" start backend || return 1
  local container status attempt
  container=$("${compose[@]}" ps -q backend) || return 1
  for attempt in {1..84}; do
    status=$(docker inspect --format '{{.State.Health.Status}}' "$container") || return 1
    [[ $status != healthy ]] || return 0
    sleep 5
  done
  echo 'Application did not become healthy after reset.' >&2
  return 1
}
sql=$(mktemp)
cleanup() {
  result=$?
  trap - EXIT
  rm -f "$sql"
  # Resume the existing container even if SQL failed; do not deploy/recreate it.
  if [[ ${stopped:-false} == true ]]; then
    resume_app || result=1
  fi
  exit "$result"
}
trap cleanup EXIT
bash "$source_dir/render-reset-sql.sh" > "$sql"
stopped=true
"${compose[@]}" stop backend
# Credentials expand only inside the existing MySQL container. No --force.
"${compose[@]}" exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql --user="$MYSQL_USER" --database="$MYSQL_DATABASE" --default-character-set=utf8mb4 --batch' < "$sql"
counts=$("${compose[@]}" exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql --user="$MYSQL_USER" --database="$MYSQL_DATABASE" --batch --skip-column-names -e "SELECT (SELECT COUNT(*) FROM product), (SELECT COUNT(*) FROM store), (SELECT COUNT(*) FROM store_inventory)"')
[[ $counts == $'15\t8\t120' ]] || { echo 'Demo row count mismatch after commit.' >&2; exit 1; }
echo 'Demo data committed. Resuming application; external checks still required.'
