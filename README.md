# Ocena Przedszkolaka (class-evaluation)

Aplikacja webowa dla nauczycielki przedszkola: bieżąca ocena umiejętności
dzieci w grupie (potrafi / jeszcze nie + notatki) i semestralne raporty dla
rodziców z zaleceniami.

**Stack:** React + TypeScript · Kotlin + Spring Boot · PostgreSQL · Docker

## Dokumentacja projektowa

| Dokument | Zawartość |
|----------|-----------|
| [docs/architektura.md](docs/architektura.md) | stack, architektura, model danych (encje i powiązania), API, bezpieczeństwo/RODO, Docker, plan iteracji |
| [docs/ekrany.md](docs/ekrany.md) | projekt UX: mapa nawigacji, opis ekranów, zasady interakcji |
| [docs/makiety.html](docs/makiety.html) | interaktywne makiety ekranów — otwórz w przeglądarce |

## Uruchomienie produkcyjne (Raspberry Pi, obrazy z GHCR)

Obrazy multi-arch (amd64 + arm64) buduje GitHub Actions
(`.github/workflows/deploy.yml`) przy każdym pushu na `main` i przy
publikacji release'a, po czym wypycha je do GHCR.

```bash
cp .env.example .env    # ustaw DB_PASSWORD (i opcjonalnie hasło admina)
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

Aktualizacja do nowszej wersji: `pull` + `up -d` ponownie. Konkretną wersję
przypinasz przez `VERSION=1.0.0` w `.env` (domyślnie `main`).
Przy prywatnym repo najpierw `docker login ghcr.io` z PAT-em `read:packages`.

## Uruchomienie z lokalnym buildem (Docker)

```bash
cp .env.example .env    # ustaw DB_PASSWORD (i opcjonalnie hasło admina)
docker compose up -d --build
```

Aplikacja: http://localhost:8000. Przy pierwszym starcie backend tworzy
konto administratora (`APP_ADMIN_EMAIL`), grupę i semestry bieżącego roku
szkolnego. Jeśli `APP_ADMIN_PASSWORD` jest puste, wygenerowane hasło
znajdziesz w `docker compose logs backend`.

## Development

```bash
docker compose -f docker-compose.dev.yml up -d   # sam Postgres
cd backend && ./gradlew bootRun                  # API na :8080
cd frontend && npm install && npm run dev        # UI na :5173 (proxy /api)
```

Testy backendu (wymagają Dockera — Testcontainers):

```bash
cd backend && ./gradlew test
```

## Struktura

```
backend/    Kotlin + Spring Boot 3 (REST API, Flyway, Spring Security)
frontend/   React 18 + TypeScript + Vite (TanStack Query, React Router)
docs/       dokumentacja projektowa i makiety
```
