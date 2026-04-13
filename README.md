Kuna lubatud fronti projecti link jäi meilist välja siis lisan siia :) https://test.washlab.ee/
# Ron Project

Spring Boot + Angular veebiprojekt: kasutaja autentimine (JWT), jalgpalli tulemused, märkmed ja treeningulogi.

## Eeldused

- **Java 21** (JDK)
- **Node.js 22+** ja **npm**
- **Docker** (PostgreSQL jaoks)

## Kiirkäivitus

### 1. Andmebaas

```bash
docker compose up -d postgres
```

See käivitab PostgreSQL konteineri (`localhost:5432`, andmebaas `ron_project`, kasutaja `postgres`, parool `postgres`).

### 2. Keskkonnamuutujad

Kopeeri `.env.example` → `.env`:

```bash
cp .env.example .env
```

| Muutuja | Kirjeldus |
|---------|-----------|
| `APP_JWT_SECRET` | JWT allkirjastamise võti (vähemalt 32 tähemärki) |
| `APP_FOOTBALL_API_KEY` | [api-sports.io](https://www.api-football.com/) API võti |

### 3. Backend

```bash
./mvnw spring-boot:run
```

Backend käivitub aadressil **http://localhost:8081**.

Rakendus: `http://localhost:8080` | PostgreSQL: `localhost:5432`

## Testide käivitamine

```bash
# Kõik testid (kasutab H2 in-memory andmebaasi, PostgreSQL pole vaja)
./mvnw test

# Üksik test
./mvnw test -Dtest=AuthenticationIntegrationTests
```

## API

| Meetod | URL | Kirjeldus | Auth |
|--------|-----|-----------|------|
| POST | `/api/auth/register` | Registreerimine | - |
| POST | `/api/auth/login` | Sisselogimine | - |
| GET | `/api/football/leagues` | Jalgpalli liigad | JWT |
| GET | `/api/football/matches` | Järgmised mängud | JWT |
| GET | `/api/memos` | Märkmete nimekiri | JWT |
| POST | `/api/memos` | Märkme loomine | JWT |
| PUT | `/api/memos/{id}` | Märkme muutmine | JWT |
| DELETE | `/api/memos/{id}` | Märkme kustutamine | JWT |
| GET | `/api/workout-logs` | Treeningulogi nimekiri | JWT |
| POST | `/api/workout-logs` | Treeningulogi kirje | JWT |
| PUT | `/api/workout-logs/{id}` | Kirje muutmine | JWT |
| DELETE | `/api/workout-logs/{id}` | Kirje kustutamine | JWT |


## Projekti struktuur

```
Ron-project/                   ← Backend (Spring Boot)
├── src/main/java/.../
│   ├── auth/                  ← Registreerimine, login
│   ├── security/              ← JWT filter, SecurityConfig
│   ├── football/              ← Väline API (api-sports.io)
│   ├── memo/                  ← Märkmete CRUD
│   ├── workout/               ← Treeningulogi CRUD
│   └── config/                ← Seadistused
├── database/                  ← SQL skriptid
└── compose.yaml               ← Docker Compose

ron-project-frontend/          ← Frontend (Angular + Material)
├── src/app/
│   ├── core/services/         ← AuthService, FootballService
│   ├── core/interceptors/     ← JWT interceptor
│   ├── core/guards/           ← Auth guard
│   ├── features/auth/         ← Login, Register lehed
│   ├── features/football/     ← Mängud ja liigad
│   └── layout/navbar/         ← Navigatsioon
```
