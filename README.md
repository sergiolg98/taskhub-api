# TaskHub — snapshot `03-microservices-design`

Proyecto guía del curso: monolito Spring Boot con arquitectura hexagonal, JPA y MySQL.
Mismo código que `02-patterns`: **esta clase solo añade documentación** de diseño. El monolito sigue corriendo. Heredado de `02-patterns` (diseño interno refinado): registro/login en la capa de aplicación, **Strategy + Factory** de notificaciones y reglas de arquitectura ejecutables (**ArchUnit**).

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
├── application
│   ├── port.in       TaskUseCase, AdminUseCase, RegisterUserUseCase, LoginUseCase
│   ├── port.out      TaskRepositoryPort, UserRepositoryPort, NotificationPort,
│   │                 PasswordHasherPort, TokenIssuerPort, CredentialsAuthenticatorPort
│   └── service       TaskService, AdminService, AuthService
├── infrastructure
│   ├── web           controllers, DTOs, GlobalExceptionHandler
│   ├── persistence   entidades JPA + adapters (Repository / Adapter)
│   ├── security      SecurityConfig, JwtService, filtro JWT + adapters de los puertos de auth
│   └── notification  NotificationStrategy (log | email), NotificationStrategyFactory, NotificationAdapter
└── TaskHubApplication
```

Flujo de una petición: `Controller → Input Port → Application Service → Output Port → Adapter`.
Todas las dependencias apuntan hacia el dominio; `ArchitectureTest` lo comprueba en cada `mvn test`.

## Patrones y dónde verlos

| Patrón / principio | Dónde |
|---|---|
| Repository | `TaskRepositoryPort`, `UserRepositoryPort` |
| Adapter | `TaskPersistenceAdapter`, `UserPersistenceAdapter` (JPA ↔ dominio), `SpringPasswordHasher`, `JwtTokenIssuer`, `SpringCredentialsAuthenticator`, `NotificationAdapter` |
| Strategy | `NotificationStrategy` → `LogNotificationStrategy`, `EmailNotificationStrategy` |
| Factory | `NotificationStrategyFactory.forType(type)` |
| DIP | `AuthService` y `TaskService` dependen de puertos, no de Spring Security, JJWT ni JPA |
| OCP | una estrategia nueva = una clase nueva; la factory recibe todas las `NotificationStrategy` por inyección |
| SRP | `AuthService` (casos de uso) / `JwtService` (tokens) / `SecurityConfig` (cadena de filtros) |
| DI | siempre por constructor y `final`; `ArchitectureTest` prohíbe `@Autowired` en campos |

## Notificaciones

Al crear una tarea se notifica por el puerto `NotificationPort`. La estrategia se elige por configuración:

```properties
taskhub.notifications.type=log     # log | email (el email es simulado: solo escribe en el log)
```

Un valor desconocido hace fallar el arranque con la lista de tipos disponibles.

## Pruebas

- `AuthServiceTest`, `TaskServiceTest`: unitarias, sin Spring ni HTTP (solo puertos con Mockito).
- `NotificationStrategyFactoryTest`: la factory y su error con un tipo desconocido.
- `ArchitectureTest`: `domain` no conoce frameworks; `application` no conoce JPA/web/security/JJWT; controllers no tocan `persistence`.
- `TaskApiTest`, `SecurityApiTest`: de la clase 1, **sin cambios** (prueban que el refactor no alteró el comportamiento).

## Pendiente a propósito (clases siguientes)

Fronteras entre dominios `auth` y `task` (clase 4), microservicios (clase 7 en adelante).
