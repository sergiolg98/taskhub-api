# TaskHub — snapshot `08-config-server`

Proyecto guía del curso. Dos servicios independientes (`07-microservices`), cada uno con su proyecto Maven y su base de datos,
cuya configuración ahora vive en un **Config Server** (`08-config-server`):

| Servicio | Puerto | Base de datos | Responsabilidad |
|---|---|---|---|
| [`auth-service`](auth-service/) | 8081 | `auth_db` | registro, login, emisión del JWT, usuarios |
| [`task-service`](task-service/) | 8082 | `task_db` | tareas, regla de propiedad, endpoints de admin |
| [`config-server`](config-server/) | 8888 | — | sirve la configuración de los dos servicios desde [`config-repo/`](config-repo/) |

No hay `pom.xml` padre ni clases compartidas: cada servicio lleva su **propia copia** de `common` (validación del token, formato de error).
Se hace así a propósito: compartir código entre servicios los acopla.

## Requisitos

- Java 21+
- Docker (para MySQL)

## Arrancar

```bash
cp .env.example .env                  # una sola vez
docker compose up -d                  # un MySQL con dos esquemas: auth_db y task_db
export JWT_SECRET='un-secreto-de-al-menos-32-caracteres-para-hs256'   # el MISMO para los dos servicios

(cd config-server && ./mvnw spring-boot:run)   # terminal 1 → http://localhost:8888 (arrancar primero)
(cd auth-service && ./mvnw spring-boot:run)    # terminal 2 → http://localhost:8081
(cd task-service && ./mvnw spring-boot:run)    # terminal 3 → http://localhost:8082
(cd config-server && ./mvnw test); (cd auth-service && ./mvnw test); (cd task-service && ./mvnw test)   # sin Docker
```

El config-server lee `../config-repo`: arráncalo desde su carpeta o define `CONFIG_REPO_PATH`.

> Si ya tenías el volumen de MySQL de las clases anteriores, el script que crea `auth_db` y `task_db` no se ejecuta
> (solo corre con el volumen vacío). Haz `docker compose down -v && docker compose up -d` (borra los datos de desarrollo).

`JWT_SECRET` es obligatorio en ambos servicios (falla rápido si falta). Es texto plano, mínimo 32 bytes (HS256).
Flyway crea las tablas de cada servicio en su primer arranque; `auth-service` siembra dos usuarios (ver abajo).

## Usuarios de ejemplo (solo desarrollo, en `auth-service`)

| id | email | rol | contraseña |
|---|---|---|---|
| 1 | `ana@taskhub.com` | `ADMIN` | `abc123` |
| 2 | `luis@taskhub.com` | `USER` | `def456` |

## Endpoints

**auth-service (8081)**

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/auth/register` | público | Crea un usuario (siempre `USER`) y devuelve un token |
| POST | `/auth/login` | público | Devuelve un token |
| GET | `/users/{id}` | el propio usuario o `ADMIN` | `UserSummary` (id, name, role) |
| GET | `/admin/users` | `ADMIN` | Lista de usuarios |

**task-service (8082)**

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/tasks` | autenticado | Tareas del usuario del token |
| GET | `/tasks/{id}` | dueño o `ADMIN` | Detalle (404 si no es tuya) |
| POST | `/tasks` | autenticado | Crear; el dueño sale del claim `uid` del token |
| PUT | `/tasks/{id}` | dueño o `ADMIN` | Actualizar título y descripción |
| PATCH | `/tasks/{id}/status` | dueño o `ADMIN` | `PENDING`, `IN_PROGRESS`, `COMPLETED` |
| DELETE | `/tasks/{id}` | dueño o `ADMIN` | Eliminar |
| GET | `/admin/tasks` | `ADMIN` | Todas las tareas |

```bash
TOKEN=$(curl -s -X POST localhost:8081/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"luis@taskhub.com","password":"def456"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')

curl -X POST localhost:8082/tasks -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"Preparar clase"}'                       # el token se emite en 8081 y se usa en 8082
curl localhost:8082/tasks -H "Authorization: Bearer $TOKEN"
```

Los códigos de error son los de siempre (`401`, `403`, `404`, `409`, `400`) y todos usan el mismo formato `ErrorResponse`.

## Configuración centralizada (clase 8)

```text
config-repo/ (archivos)  →  config-server :8888  →  auth-service / task-service
```

- **Qué se externaliza** (en `config-repo/`): puerto, URL de la base de datos, niveles de log, timeout de conexión (`hikari.connection-timeout`),
  expiración del token (auth) y estrategia de notificación (task). Un archivo base por servicio (`task-service.properties`) y uno por perfil
  (`-dev`, `-test`, `-prod`); el del perfil gana al base.
- **Qué NO se externaliza:** `JWT_SECRET`, `DB_USER` y `DB_PASSWORD` siguen en variables de entorno de cada servicio. Un test del config-server falla si aparece
  una clave con `password`, `secret` o `token` en `config-repo/`.
- **Cliente:** `spring.config.import=optional:configserver:http://localhost:8888` (Boot 3; no hay `bootstrap`). El perfil por defecto es `dev`
  (`SPRING_PROFILES_ACTIVE` lo cambia).
- **Server caído:** con `optional:` el servicio arranca con los valores de respaldo de su `application.properties` local; sin `optional:` falla al arrancar.
- **Los tests no usan el server:** `spring.cloud.config.enabled=false` en `src/test/resources/application.properties`.
- **Versiones:** Spring Boot 3.5.16 + Spring Cloud **2025.0.3**, fijadas con `dependencyManagement` y comprobadas con un build y arranque reales.
- Para probar el server: `curl localhost:8888/task-service/dev` y `curl localhost:8888/auth-service/prod`.
- Cambiar un valor (p. ej. `server.port` en `config-repo/task-service.properties`) y **reiniciar** el cliente basta; no se recompila. Sin recarga en caliente (`/actuator/refresh` queda como lectura opcional).

## Cómo se relacionan los dos servicios

- **El token es el único contrato.** `auth-service` lo firma (HS256) con claims `sub`, `uid` y `role`; `task-service` solo comprueba la firma y la
  caducidad con el mismo `JWT_SECRET` y construye el usuario **solo desde los claims**. No hay llamadas entre servicios.
- **`tasks.owner_id` no tiene clave foránea.** `users` vive en otra base de datos y una restricción no cruza bases.
- **Pérdida consciente:** `task-service` ya no comprueba que el dueño exista (antes: `OwnerNotFoundException`, 404). Vuelve en la clase 10 con una llamada HTTP.
- **`task-service` sigue funcionando con `auth-service` apagado** para los tokens ya emitidos (verificado).
- Trade-off del secreto compartido: quien conoce `JWT_SECRET` puede *firmar* tokens, no solo validarlos. Con RSA (clave privada en auth, pública en task) solo `auth-service` firma; se comenta en clase.

## Arquitectura

Cada servicio mantiene la hexagonal de siempre (`domain` · `application` · `infrastructure`) más su `common`. `ArchitectureTest` (en cada servicio) lo comprueba;
en `task-service` además prohíbe cualquier dependencia de `com.taskhub.auth..`.

## Pendiente a propósito (clases siguientes)

API Gateway (09), comunicación entre servicios (10), resiliencia (11), Docker (12), Compose y Jenkins (13), integración (14).
