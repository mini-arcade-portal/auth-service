**English** | [Magyar](README.hu.md)

# auth-service

Authentication service for the Mini Arcade Portal. Handles user registration,
login, and issues the JWTs the other services trust.

## Stack

- Java 21, Spring Boot 4
- Spring Security + JJWT (JWT issuing/validation)
- Spring Data JPA + PostgreSQL
- springdoc-openapi (Swagger UI)
- Testcontainers for integration tests

## Endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`

Full API docs: `http://localhost:8081/swagger-ui.html`

## Running locally

The service needs PostgreSQL and a shared `JWT_SECRET`, so it's easiest to run
the whole stack from [`infra`](../infra):

```bash
cd ../infra
docker compose up --build
```

auth-service is then available at `http://localhost:8081`.

Alternatively, run just this service against your own PostgreSQL instance:

```bash
./mvnw spring-boot:run
```

## Tests

```bash
./mvnw test
```

Integration tests spin up PostgreSQL via Testcontainers — no local database
needed.
