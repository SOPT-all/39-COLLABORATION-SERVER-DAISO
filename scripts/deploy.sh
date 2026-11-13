#!/usr/bin/env bash
# Invoked as root by SSM with an immutable release directory and image digest.
set -euo pipefail
umask 077
release=$(cd "$(dirname "$0")/.." && pwd)
base=${DEPLOY_ROOT:-/opt/daiso}
project=${COMPOSE_PROJECT_NAME:-daiso-prod}
image=${1:?Pass image@sha256:digest}
[[ $image =~ ^ghcr\.io/[a-z0-9._/-]+@sha256:[a-f0-9]{64}$ ]] || { echo 'Invalid immutable image.' >&2; exit 1; }
[[ -f $base/.env && ! -L $base/.env ]] || { echo 'Prepare deployment .env first.' >&2; exit 1; }
[[ $(stat -c %a "$base/.env") == 600 && $(stat -c %u "$base/.env") == "$EUID" ]] || { echo '.env must be owned by the deployment user with mode 600.' >&2; exit 1; }
mkdir -p "$base/state"
exec 9>"$base/state/deploy.lock"
flock -n 9 || { echo 'Another deployment is running.' >&2; exit 1; }
export BACKEND_IMAGE=$image
compose=(docker compose -p "$project" --env-file "$base/.env" -f "$release/docker-compose.prod.yml")
"${compose[@]}" config --quiet
docker pull "$image"
[[ $(docker image inspect --format '{{.Architecture}}' "$image") == arm64 ]] || { echo 'ARM64 image required.' >&2; exit 1; }
# Keep the previous release as well as its image: configuration can change too.
previous=''
[[ ! -f $base/state/current ]] || previous=$(cat "$base/state/current")
previous_image=''
[[ ! -f $base/state/image ]] || previous_image=$(cat "$base/state/image")
"${compose[@]}" up -d --wait --wait-timeout 360 mysql
migration_hash() {
  (cd "$1/src/main/resources/db/migration" && sha256sum ./*.sql) | sha256sum | cut -d ' ' -f 1
}
db_history() {
  # Expand credentials inside the DB container, never in the SSM command.
  # shellcheck disable=SC2016
  "${compose[@]}" exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql --batch --skip-column-names -u"$MYSQL_USER" "$MYSQL_DATABASE" -e "SELECT installed_rank, version, checksum, success FROM flyway_schema_history ORDER BY installed_rank"' 2>/dev/null
}
# Compare actual migration state too; a missing/unreadable history is not safe to restore.
before_history=''
history_known=false
if before_history=$(db_history); then history_known=true; fi
# No automatic rollback across migration changes, including uncertain/first deployments.
can_restore=false
if [[ -n $previous && -f $previous/docker-compose.prod.yml && -n $previous_image ]]; then
  if [[ $(migration_hash "$previous") == "$(migration_hash "$release")" ]]; then can_restore=true; fi
fi
if "${compose[@]}" up -d --no-deps --wait --wait-timeout 420 backend; then
  printf '%s\n' "$previous_image" > "$base/state/previous-image"
  printf '%s\n' "$previous" > "$base/state/previous-release"
  printf '%s\n' "$image" > "$base/state/image"
  printf '%s\n' "$release" > "$base/state/current"
  echo 'Application healthy. External checks still required.'
else
  # Do not emit application logs to SSM: they can contain database URLs or secrets.
  "${compose[@]}" ps --all
  after_history=''
  if [[ $can_restore == true && $history_known == true ]] && after_history=$(db_history) && [[ $after_history == "$before_history" ]]; then
    export BACKEND_IMAGE=$previous_image
    if docker compose -p "$project" --env-file "$base/.env" -f "$previous/docker-compose.prod.yml" \
      up -d --no-deps --wait --wait-timeout 420 backend; then
      echo 'Previous application restored. New deployment FAILED.' >&2
    else
      echo 'Recovery also failed; inspect locally via an authorized session.' >&2
    fi
  else
    echo 'No verified compatible previous release; inspect DB migration state before recovery.' >&2
  fi
  exit 1
fi
