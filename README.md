# Bombadle

[![CI](https://github.com/JulianDemucha/bombadle/actions/workflows/bombadle-ci.yml/badge.svg?branch=dev)](https://github.com/JulianDemucha/bombadle/actions/workflows/bombadle-ci.yml)
[![CD](https://github.com/JulianDemucha/bombadle/actions/workflows/cd.yml/badge.svg?branch=master)](https://github.com/JulianDemucha/bombadle/actions/workflows/cd.yml)
[![codecov](https://codecov.io/gh/JulianDemucha/bombadle/branch/dev/graph/badge.svg)](https://codecov.io/gh/JulianDemucha/bombadle)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

A daily guessing game based on the Polish animated series *Kapitan Bomba*. Live at [bombadle.pl](https://bombadle.pl).

New puzzles go live every day at 07:00 (Europe/Warsaw), the same for everyone.

## Game modes

- **Classic** - guess the character. Every attempt is compared on gender, race, alive status, colors, affiliations and first appearance episode, with higher/lower hints for the episode.
- **Quotes** - two stages: complete the quote, then name the character behind it.
- **Images** - a pixelated picture gets sharper with every wrong guess (9 levels). Each level is served separately by the backend, so a clearer image is never sent to the client early.

## Highlights

- **Play without an account.** Anonymous progress is stored server-side and can be merged into an account after signing in.
- **Streaks.** Play daily to build a streak, finish all modes for a superstreak. Daily and streak leaderboards, per-player statistics charts.
- **Fair rotation.** No character repeats until the whole pool is used, and no character appears in two modes on the same day.
- **Safe content updates.** Admin edits to characters are queued and applied at the daily reset, so they never spoil or break the current puzzle. Every admin action is audit logged, and the image levels are generated from a single uploaded picture.
- **Accounts.** Email or Google sign-in, account recovery and delayed account deletion.

## Run locally

```bash
cp .env.example .env
docker compose up -d        # postgres, backend (:8080), mailpit

cd frontend
cp .env.example .env
npm ci
npm run dev                 # http://localhost:5173
```

- The first daily puzzles are picked automatically on the first start.
- Activation codes and other emails land in Mailpit at http://localhost:8025.
- Google sign-in stays hidden until `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` are set in `.env`.

## Technology

### Backend
- Java 21, Spring Boot 3.5 (Web, Data JPA, Validation, Cache, Mail, Actuator), OpenAPI docs via springdoc
- Scheduled jobs for the daily reset, activity tracking and token cleanup
- Type-safe configuration properties, graceful shutdown for zero-downtime deploys

### Data and caching
- PostgreSQL 16, schema fully managed by Flyway (51 migrations)
- Guess history stored as JSONB, one list per player and mode
- Caffeine caches with per-cache TTL and size limits set in YAML. After every daily reset all guess-vs-answer comparisons are precomputed, so a guess is a cache lookup

### Security
- Stateless auth: short-lived JWT and rotating refresh tokens (stored only as SHA-256 hashes) in HttpOnly cookies
- Double-submit-cookie CSRF protection
- Optional Google sign-in (OAuth2/OIDC), enabled only when credentials are set, next to email and password (BCrypt, enforced password strength)
- Email OTP codes: single-use, expiring, rate limited per address
- Role-based admin access (admin, superadmin) and account locking

### Frontend
- React 19, React Router 7, Vite 7, plain JavaScript with ESLint
- One game hook per mode, Recharts for statistics, canvas-confetti for wins
- Axios interceptor with single-flight token refresh plus a silent refresh before expiry
- Diacritics-insensitive search over a cached character index, aliases included

### Testing
- JUnit 5, over 500 tests: unit tests plus Spring Boot integration tests on a real PostgreSQL via Testcontainers (daily reset, cache eviction, leaderboards, anonymous merge, access control)
- JaCoCo coverage of unit and integration tests, reported to Codecov by CI

### Infrastructure
- Docker: multi-stage backend image (JDK build, JRE runtime, non-root user, health check). Compose profiles for dev (Mailpit catches emails) and prod
- nginx with TLS 1.2/1.3, HTTP/2 and Let's Encrypt auto-renewal, serving the SPA and proxying the API
- GitHub Actions: CI on feature branches and pull requests, CD on `master` (tests, images to GHCR tagged with the commit SHA, deploy over SSH)
- Zero-downtime blue/green deploys: the new backend must be healthy and pass a smoke test through the real domain before traffic moves, otherwise nginx is switched back automatically. Frontend releases are versioned and swapped with an atomic symlink

## License

The source code is released under the [MIT License](LICENSE). Characters, images and quotes from *Kapitan Bomba* belong to their respective rights holders and are not covered by it.
