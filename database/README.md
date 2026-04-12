# PostgreSQL setup

The application is configured to use PostgreSQL by default.

## Option 1: Full Docker Compose

From the project root:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

This starts:

- app: `http://localhost:8080`
- database: `ron_project`
- username: `postgres`
- password: `postgres`
- postgres port: `5432`

Set at least these values in `.env` before starting:

- `APP_JWT_SECRET`
- `APP_FOOTBALL_API_KEY`

## Option 2: PostgreSQL only via Docker Compose

```powershell
docker compose up -d postgres
```

## Option 2: Existing local PostgreSQL

Run the SQL in `database/create-database.sql` with a PostgreSQL superuser, then point the app to the created database with environment variables.

Example:

```powershell
$env:APP_DATASOURCE_URL="jdbc:postgresql://localhost:5432/ron_project"
$env:APP_DATASOURCE_USERNAME="ron_project_app"
$env:APP_DATASOURCE_PASSWORD="change_me"
```

## Create tables manually

If you want to create the current tables yourself instead of relying on Hibernate, run:

```powershell
psql -U postgres -d ron_project -f database/schema.sql
```

This creates the current application tables:

- `users`
- `memos`
- `workout_logs`

## JWT secret

Set a real secret before running outside development:

```powershell
$env:APP_JWT_SECRET="replace-this-with-a-long-random-secret-at-least-32-characters"
```

## Notes

- Hibernate is currently configured with `spring.jpa.hibernate.ddl-auto=update`, so the app will create/update tables automatically.
- The compose setup stores database data in the named volume `postgres_data`.
- The app container connects to PostgreSQL using the compose service name `postgres`.
