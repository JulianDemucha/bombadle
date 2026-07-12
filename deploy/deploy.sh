#!/usr/bin/env bash
set -euo pipefail

SHA="$1"
REPO_DIR=/home/ubuntu/bombadle
DATA_DIR=/home/ubuntu/bombadle-data
STATE_DIR="$DATA_DIR/state"
RELEASES_DIR="$DATA_DIR/releases/frontend"
ACTIVE_COLOR_FILE="$STATE_DIR/active_color"
UPSTREAM_FILE="$STATE_DIR/active_upstream.conf"
BACKEND_IMAGE="ghcr.io/juliandemucha/bombadle-backend:${SHA}"
export BACKEND_IMAGE
COMPOSE="docker compose -f docker-compose.yml --env-file .env.prod --profile prod"

cd "$REPO_DIR"

# 0. sync infra config to this commit
git fetch --quiet origin master
git checkout --quiet "$SHA"

# 1. determine current vs. target color
CURRENT=$(cat "$ACTIVE_COLOR_FILE" 2>/dev/null || echo blue)
NEW=$([ "$CURRENT" = "blue" ] && echo green || echo blue)
echo "active=$CURRENT new=$NEW image=$BACKEND_IMAGE"

# 2. start the inactive color (db already running, untouched)
$COMPOSE pull "backend-$NEW"
$COMPOSE up -d --no-deps "backend-$NEW"

# 3. poll healthcheck, abort cleanly on timeout
TIMEOUT=90
ELAPSED=0
until [ "$(docker inspect -f '{{.State.Health.Status}}' "bombadle-backend-$NEW" 2>/dev/null)" = "healthy" ]; do
  sleep 3
  ELAPSED=$((ELAPSED + 3))
  if [ "$ELAPSED" -ge "$TIMEOUT" ]; then
    echo "ABORT: backend-$NEW nie osiagnal healthy po ${TIMEOUT}s" >&2
    $COMPOSE logs --tail=200 "backend-$NEW" || true
    $COMPOSE stop "backend-$NEW" || true
    exit 1
  fi
done
echo "backend-$NEW healthy po ${ELAPSED}s"

# 4. swap upstream, validate before reload
printf 'server backend-%s:8080;\n' "$NEW" > "$UPSTREAM_FILE.tmp"
mv -f "$UPSTREAM_FILE.tmp" "$UPSTREAM_FILE"

if ! $COMPOSE exec -T nginx nginx -t; then
  echo "ABORT: nginx -t failed - nginx nigdy nie zrobil reload, stary kolor dalej serwuje" >&2
  exit 1
fi
$COMPOSE exec -T nginx nginx -s reload

# 5. drain: let old color finish in-flight requests
sleep 10

# 6. smoke test through the real vhost
DOMAIN=$(grep -Po '(?<=^DOMAIN=).*' .env.prod)
if ! curl -fsS --max-time 5 --resolve "${DOMAIN}:443:127.0.0.1" \
     "https://${DOMAIN}/api/card-guessing/quotes/prompt" > /dev/null; then
  echo "SMOKE TEST FAILED po przelaczeniu na $NEW - rollback na $CURRENT" >&2
  printf 'server backend-%s:8080;\n' "$CURRENT" > "$UPSTREAM_FILE.tmp"
  mv -f "$UPSTREAM_FILE.tmp" "$UPSTREAM_FILE"
  $COMPOSE exec -T nginx nginx -t && $COMPOSE exec -T nginx nginx -s reload
  echo "Rollback wykonany. backend-$NEW zostawiony uruchomiony (bez ruchu) do analizy." >&2
  exit 1
fi

# 7. stop old color now (graceful shutdown)
$COMPOSE stop -t 30 "backend-$CURRENT"
echo "$NEW" > "$ACTIVE_COLOR_FILE"

# 8. frontend: atomic symlink swap (dist/ for $SHA already rsynced by CD)
ln -sfn "$SHA" "$RELEASES_DIR/current_tmp"
mv -T "$RELEASES_DIR/current_tmp" "$RELEASES_DIR/current"

# 9. prune old frontend releases, keep last 5
( cd "$RELEASES_DIR" && ls -1dt */ | tail -n +6 | xargs -r rm -rf -- )

echo "deploy $SHA zakonczony, aktywny kolor = $NEW"
