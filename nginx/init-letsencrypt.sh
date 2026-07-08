#!/usr/bin/env bash
# Jednorazowy bootstrap certyfikatu Let's Encrypt. Odpalic RAZ na serwerze, po tym jak
# DNS domeny z DOMAIN juz wskazuje na ten serwer i porty 80/443 sa otwarte.
#
# Uzycie: ./nginx/init-letsencrypt.sh   (z katalogu glownego repo, obok docker-compose.yml)
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

COMPOSE="docker compose -f docker-compose.yml --env-file .env.prod --profile prod"

echo "==> tymczasowy self-signed cert, zeby nginx w ogole wystartowal na 443"
$COMPOSE run --rm --entrypoint sh certbot -c "
  apk add --no-cache openssl >/dev/null &&
  mkdir -p /etc/letsencrypt/live/${DOMAIN} &&
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout /etc/letsencrypt/live/${DOMAIN}/privkey.pem \
    -out /etc/letsencrypt/live/${DOMAIN}/fullchain.pem \
    -subj '/CN=${DOMAIN}'
"

echo "==> startuje backend + nginx (na razie z tymczasowym certem)"
$COMPOSE up -d --build backend nginx

echo "==> czekam az nginx wstanie"
sleep 3

echo "==> kasuje tymczasowy cert i wystepuje o prawdziwy z Let's Encrypt (ACME http-01 przez port 80)"
$COMPOSE run --rm --entrypoint sh certbot -c "
  rm -rf /etc/letsencrypt/live/${DOMAIN} /etc/letsencrypt/archive/${DOMAIN} /etc/letsencrypt/renewal/${DOMAIN}.conf
"
$COMPOSE run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d "${DOMAIN}" \
  --email "${CERTBOT_EMAIL}" \
  --agree-tos \
  --no-eff-email

echo "==> restart nginx z prawdziwym certem"
$COMPOSE restart nginx

echo "gotowe - sprawdz https://${DOMAIN}"
echo "certbot w kontenerze bedzie sam odnawial cert co 12h (jesli sie zblizy termin)."
