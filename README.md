# TaskHub — snapshot `05-expert-final-reviewed`

Proyecto guía del curso: monolito Spring Boot con arquitectura hexagonal, JPA y MySQL.
Mismo diseño que `04-expert-final`, tras un **code review** que corrigió 6 defectos reales (identidad por email, filtro JWT duplicado,
límite de BCrypt en bytes, formato de error único, `IllegalArgumentException` como 400, carrera en el registro).

## Requisitos

- Java 21+
- Docker (para MySQL)

## Arrancar

```bash
cp .env.example .env                  # una sola vez
docker compose up -d                  # MySQL en localhost:3306
export JWT_SECRET='un-secreto-de-al-menos-32-caracteres-para-hs256'
./mvnw spring-boot:run                # API en http://localhost:8080
./mvnw test                           # tests con H2, no necesitan Docker
```

`JWT_SECRET` es obligatorio: sin él la app no arranca (falla rápido a propósito). Es texto plano, mínimo 32 bytes (HS256).
Flyway crea las tablas y dos usuarios de ejemplo (ver abajo) en el primer arranque.

## Usuarios de ejemplo (solo desarrollo)

| id | email | rol | contraseña |
|---|---|---|---|
| 1 | `ana@taskhub.com` | `ADMIN` | `abc123` |
| 2 | `luis@taskhub.com` | `USER` | `def456` |

## Endpoints

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/auth/register` | público | Crea un usuario (siempre `USER`) y devuelve un token |
| POST | `/auth/login` | público | Devuelve un token |
| GET | `/tasks` | autenticado | Tareas del usuario del token |
| GET | `/tasks/{id}` | dueño o `ADMIN` | Detalle (404 si no es tuya) |
| POST | `/tasks` | autenticado | Crear (`title`, `description`); el dueño sale del token |
| PUT | `/tasks/{id}` | dueño o `ADMIN` | Actualizar título y descripción |
| PATCH | `/tasks/{id}/status` | dueño o `ADMIN` | `PENDING`, `IN_PROGRESS`, `COMPLETED` |
| DELETE | `/tasks/{id}` | dueño o `ADMIN` | Eliminar |
| GET | `/users/{id}` | uno mismo o `ADMIN` | Resumen público: `id`, `name`, `role` (contrato entre áreas) |
| GET | `/admin/users` | `ADMIN` | Lista de usuarios (sin contraseña) |
| GET | `/admin/tasks` | `ADMIN` | Todas las tareas |

```bash
TOKEN=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"luis@taskhub.com","password":"def456"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')

curl -X POST localhost:8080/tasks -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"Preparar clase"}'
curl localhost:8080/tasks -H "Authorization: Bearer $TOKEN"
```

Códigos: `401` sin token, token inválido o credenciales erróneas · `403` rol insuficiente · `404` recurso inexistente
**o ajeno** (no se confirma que exista) · `409` email ya registrado · `400` validación · `405`/`415`/`500` genéricos.
Todos los errores usan el mismo formato `ErrorResponse` (`status`, `message`, `details`, `timestamp`).

## Arquitectura

```text
com.taskhub
├── auth      identidad: domain · application (port.in/out, service) · infrastructure (web, persistence, security)
├── task      trabajo:   domain · application (port.in/out, service) · infrastructure (web, persistence, notification, lookup)
├── common    token (JwtParser, filtro, JwtPrincipal), SecurityConfig, formato de errores
└── TaskHubApplication
```

Reglas (las comprueba `ArchitectureTest` en cada `mvn test`):

- `auth` no conoce a `task`; `common` no conoce a ninguna de las dos.
- `task` solo conoce a `auth` en `task.infrastructure.lookup` (el adaptador de `UserLookupPort`). Es la única arista entre las áreas.
- Dentro de cada área: `domain` no conoce frameworks, `application` solo conoce puertos.

## Patrones y dónde verlos

| Patrón / principio | Dónde |
|---|---|
| Repository | `TaskRepositoryPort`, `UserRepositoryPort` |
| Adapter | `*PersistenceAdapter`, `SpringPasswordHasher`, `JwtTokenIssuer`, `SpringCredentialsAuthenticator`, `NotificationAdapter`, `UserLookupLocalAdapter` |
| Strategy + Factory | `NotificationStrategy` (`log` \| `email`) + `NotificationStrategyFactory`, elegida con `taskhub.notifications.type` |
| DIP | los servicios dependen de puertos; `UserLookupPort` oculta si el usuario se consulta en proceso o por red |
| SRP | `JwtTokenIssuer` firma (auth); `JwtParser` valida (common) |

## Pruebas

`./mvnw test` (48 tests, usan H2): unitarias de casos de uso con puertos simulados, `JwtParserTest`, `NotificationStrategyFactoryTest`,
`ArchitectureTest` y las de API (`TaskApiTest`, `SecurityApiTest`) que prueban el comportamiento completo.

## Pendiente a propósito (clases siguientes)

Revisión (clase 5) y separación física en microservicios (clase 7 en adelante).
