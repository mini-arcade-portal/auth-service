[English](README.md) | **Magyar**

# auth-service

A Mini Arcade Portal autentikációs szolgáltatása. Regisztrációt és
bejelentkezést kezel, valamint kiállítja azokat a JWT-ket, amelyeket a többi
szolgáltatás elfogad.

## Stack

- Java 21, Spring Boot 4
- Spring Security + JJWT (JWT kiállítás/validáció)
- Spring Data JPA + PostgreSQL
- springdoc-openapi (Swagger UI)
- Testcontainers az integrációs tesztekhez

## Végpontok

- `POST /api/auth/register`
- `POST /api/auth/login`

Teljes API dokumentáció: `http://localhost:8081/swagger-ui.html`

## Futtatás lokálisan

A szolgáltatáshoz PostgreSQL és egy közös `JWT_SECRET` kell, ezért a
legegyszerűbb az egész stacket elindítani az [`infra`](../infra) mappából:

```bash
cd ../infra
docker compose up --build
```

Ezután az auth-service a `http://localhost:8081` címen érhető el.

Alternatívaként a szolgáltatás önállóan is futtatható egy saját PostgreSQL
példány ellen:

```bash
./mvnw spring-boot:run
```

## Tesztek

```bash
./mvnw test
```

Az integrációs tesztek Testcontainers segítségével indítanak PostgreSQL-t —
nincs szükség lokális adatbázisra.
