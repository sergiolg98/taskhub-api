# TaskHub — snapshot `01-jwt-security`

Proyecto guía del curso: monolito Spring Boot con arquitectura hexagonal, JPA y MySQL.
Este snapshot añade **Spring Security + JWT**: registro, login, roles, propiedad de tareas por usuario y endpoints admin.

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
**o ajeno** (no se confirma que exista) · `409` email ya registrado · `400` validación.

## Arquitectura

```text
com.taskhub
├── domain            model (User, Task, Role, TaskStatus, AuthenticatedUser) y exception
├── application       port.in (TaskUseCase, AdminUseCase), port.out (repositorios), service
├── infrastructure    web (controllers, DTOs, errores) · persistence (JPA + adapters)
│                     security (SecurityConfig, JwtService, filtro JWT, UserDetailsService)
└── TaskHubApplication
```

- La regla de propiedad de las tareas vive en `TaskService` (caso de uso), no en el controller.
- El filtro JWT solo identifica; quien responde 401/403 es la cadena de seguridad (`SecurityErrorHandler`).
- Sesión `STATELESS`: cada petición trae su token.

## Pendiente a propósito (clases siguientes)

Patrones de diseño (clase 2), fronteras entre dominios (clase 4), microservicios (clase 7 en adelante).
