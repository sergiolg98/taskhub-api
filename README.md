# TaskHub — snapshot `00-base`

Punto de partida del proyecto guía: monolito Spring Boot con arquitectura hexagonal, JPA y MySQL.
Todavía **sin seguridad**: Spring Security, JWT y OAuth2 se agregan en la clase 1 (`01-security-oauth`).

## Requisitos

- Java 21+
- Docker (para MySQL)

## Arrancar

```bash
docker compose up -d      # MySQL en localhost:3306, configurado con `.env` (copia `.env.example`)
./mvnw spring-boot:run    # API en http://localhost:8080
./mvnw test               # tests con H2, no necesitan Docker
```

Flyway crea las tablas y dos usuarios de ejemplo (`id 1` ADMIN, `id 2` USER). Su `password` es un
valor de relleno: el registro, el login y el cifrado llegan en la clase 1.

## Endpoints (versión base)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/tasks?ownerId=2` | Tareas de un usuario |
| GET | `/tasks/{id}` | Detalle |
| POST | `/tasks` | Crear (`title`, `description`, `ownerId`) |
| PUT | `/tasks/{id}` | Actualizar título y descripción |
| PATCH | `/tasks/{id}/status` | Cambiar estado (`PENDING`, `IN_PROGRESS`, `COMPLETED`) |
| DELETE | `/tasks/{id}` | Eliminar |

```bash
curl -X POST localhost:8080/tasks -H 'Content-Type: application/json' \
  -d '{"title":"Preparar clase","ownerId":2}'
```

`ownerId` viaja como parámetro solo porque aún no hay autenticación; con JWT saldrá del token.
No existen aún `/auth/*` ni `/admin/*`.

## Arquitectura

```text
com.taskhub
├── domain            model (User, Task, Role, TaskStatus) y exception
├── application       port.in (TaskUseCase), port.out (repositorios), service (TaskService)
├── infrastructure    web (controller, DTOs, errores), persistence (entidades JPA + adapters)
└── TaskHubApplication
```

El dominio no depende de Spring ni de JPA. Los adapters de `persistence` traducen entre
entidades JPA y modelo de dominio. Los paquetes `security` y `configuration` se llenan en clases posteriores.

## Pendiente a propósito (clases siguientes)

Registro/login, JWT, roles, propiedad de tareas por usuario, endpoints admin, OAuth2 demo, patrones.
