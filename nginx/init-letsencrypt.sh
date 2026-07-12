#!/usr/bin/env bash
# One-time server bootstrap: blue-green state, a placeholder frontend release, and the
# first Let's Encrypt cert. Run ONCE, after DNS points here, ports 80/443 are open, and
# at least one image exists in GHCR (i.e. after cd.yml has run once - tag :latest).
#
# Usage: ./nginx/init-letsencrypt.sh   (from repo root, next to docker-compose.yml)
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f .env.prod ]; then
  echo ".env.prod nie istnieje - skopiuj .env.prod.example do .env.prod i uzupelnij wartosci." >&2
  exit 1
fi

set -a
source .env.prod
set +a

: "${DOMAIN:?ustaw DOMAIN w .env.prod}"
: "${CERTBOT_EMAIL:?ustaw CERTBOT_EMAIL w .env.prod}"

DATA_DIR=/home/ubuntu/bombadle-data
STATE_DIR="$DATA_DIR/state"
RELEASES_DIR="$DATA_DIR/releases/frontend"

# CD pins these per-deploy; this first bootstrap has no prior deploy to pin to,
# so default to :latest. Override before running if needed, e.g.
# BACKEND_IMAGE=ghcr.io/.../bombadle-backend:<sha> ./nginx/init-letsencrypt.sh
BACKEND_IMAGE="${BACKEND_IMAGE:-ghcr.io/juliandemucha/bombadle-backend:latest}"
NGINX_IMAGE="${NGINX_IMAGE:-ghcr.io/juliandemucha/bombadle-nginx:latest}"
export BACKEND_IMAGE NGINX_IMAGE

COMPOSE="docker compose -f docker-compose.yml --env-file .env.prod --profile prod"

echo "==> przygotowuje stan blue-green (backend-blue jako pierwszy aktywny kolor)"
mkdir -p "$STATE_DIR" "$RELEASES_DIR"
echo "blue" > "$STATE_DIR/active_color"
printf 'server backend-blue:8080;\n' > "$STATE_DIR/active_upstream.conf"

if [ ! -e "$RELEASES_DIR/current" ]; then
  echo "==> brak jeszcze wydania frontendu - tworze tymczasowa strone powitalna"
  mkdir -p "$RELEASES_DIR/bootstrap"
  echo '<html><body>bombadle - wdrozenie w toku</body></html>' > "$RELEASES_DIR/bootstrap/index.html"
  ln -sfn bootstrap "$RELEASES_DIR/current"
fi

echo "==> startuje baze danych"
$COMPOSE up -d db

echo "==> czekam az baza bedzie healthy"
TIMEOUT=60; ELAPSED=0
until [ "$(docker inspect -f '{{.State.Health.Status}}' bombadle-db 2>/dev/null)" = "healthy" ]; do
  sleep 2; ELAPSED=$((ELAPSED + 2))
  if [ "$ELAPSED" -ge "$TIMEOUT" ]; then
    echo "baza nie osiagnela healthy po ${TIMEOUT}s" >&2
    exit 1
  fi
done

echo "==> tymczasowy self-signed cert, zeby nginx w ogole wystartowal na 443"
$COMPOSE run --rm --entrypoint sh certbot -c "
  apk add --no-cache openssl >/dev/null &&
  mkdir -p /etc/letsencrypt/live/${DOMAIN} &&
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout /etc/letsencrypt/live/${DOMAIN}/privkey.pem \
    -out /etc/letsencrypt/live/${DOMAIN}/fullchain.pem \
    -subj '/CN=${DOMAIN}'
"

echo "==> startuje backend-blue + nginx (na razie z tymczasowym certem), obrazy: ${BACKEND_IMAGE} / ${NGINX_IMAGE}"
# --no-deps: db already up; keep backend-green off until the first real deploy
$COMPOSE up -d --no-deps backend-blue nginx

echo "==> czekam az backend-blue bedzie healthy"
TIMEOUT=90; ELAPSED=0
until [ "$(docker inspect -f '{{.State.Health.Status}}' bombadle-backend-blue 2>/dev/null)" = "healthy" ]; do
  sleep 3; ELAPSED=$((ELAPSED + 3))
  if [ "$ELAPSED" -ge "$TIMEOUT" ]; then
    echo "backend-blue nie osiagnal healthy po ${TIMEOUT}s - sprawdz: docker logs bombadle-backend-blue" >&2
    exit 1
  fi
done
echo "backend-blue healthy po ${ELAPSED}s"

echo "==> kasuje tymczasowy cert i wystepuje o prawdziwy z Let's Encrypt (ACME http-01 przez port 80)"
$COMPOSE run --rm --entrypoint sh certbot -c "
  rm -rf /etc/letsencrypt/live/${DOMAIN} /etc/letsencrypt/archive/${DOMAIN} /etc/letsencrypt/renewal/${DOMAIN}.conf
"
$COMPOSE run --rm --entrypoint certbot certbot certonly \
  --webroot -w /var/www/certbot \
  -d "${DOMAIN}" \
  --email "${CERTBOT_EMAIL}" \
  --agree-tos \
  --no-eff-email

echo "==> restart nginx z prawdziwym certem"
$COMPOSE restart nginx

echo "==> startuje certbota w tle (petla auto-odnawiania co 12h)"
$COMPOSE up -d certbot

echo "gotowe - sprawdz https://${DOMAIN}"
echo "certbot w kontenerze bedzie sam odnawial cert co 12h (jesli sie zblizy termin)."
echo "od teraz kolejne wdrozenia robi automatycznie deploy/deploy.sh, wyzwalany przez CD po pushu do master."
