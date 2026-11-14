# OmniAsset

Kotlin Multiplatform application for computer equipment inventory.

## Server

The Ktor server uses PostgreSQL through HikariCP. The server is the only component that connects to the database; desktop and Android clients use the HTTP API.

### Database

Create a database named `omniasset`. On an empty database, Flyway applies:

`server/src/main/resources/db/migration/V1__create_inventory_schema.sql`

If the `inventory` schema was created manually from the supplied SQL, Flyway baselines that existing schema at version 1.

### Environment

Set these variables before starting the server:

```bash
export DATABASE_JDBC_URL='jdbc:postgresql://10.1.2.114:5432/omniasset?currentSchema=inventory'
export DATABASE_USERNAME='postgres'
export DATABASE_PASSWORD='change-me'
export JWT_SECRET='replace-with-a-long-random-secret'
```

Optional pool settings:

```bash
export DATABASE_MAX_POOL_SIZE=15
export DATABASE_MIN_IDLE=5
export DATABASE_CONNECTION_TIMEOUT=5000
```

Start the server:

```bash
./gradlew :server:run
```

The health check is available at `GET /health`.

### Initial administrator

After the migration, create the first administrator in DBeaver. The password is hashed by PostgreSQL `pgcrypto`:

```sql
INSERT INTO inventory.app_users (
    username,
    password_hash,
    full_name,
    role
)
VALUES (
    'admin',
    crypt('replace-with-your-password', gen_salt('bf')),
    'Administrator',
    'admin'
);
```

### Equipment API

After login, send the returned JWT as:

```http
Authorization: Bearer <token>
```

List equipment with pagination and filters:

```http
GET /api/equipment?search=thinkpad&status=assigned&categoryId=1&locationId=2&employeeId=3&limit=50&offset=0
```

The first server stage includes the login endpoint, filtered equipment list, atomic movement and status change operations, Flyway migration, and a bounded report semaphore. Equipment movement and status changes are available only to authenticated users.
