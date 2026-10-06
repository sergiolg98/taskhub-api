# TaskHub — snapshot final `14-pro-final`

Gestor de tareas como **sistema distribuido**: dos microservicios con su propia base de datos, un API Gateway, un Config Server, comunicación entre servicios con
resiliencia, todo en contenedores con Docker Compose y un pipeline de Jenkins. Proyecto guía del curso; cada clase es una rama (ver [Snapshots](#snapshots-del-curso)).

```text
Cliente → api-gateway :8080 ─┬→ auth-service :8081 → auth_db
                             └→ task-service :8082 → task_db
task-service ──OpenFeign + Circuit Breaker──→ auth-service
config-server :8888 → configuración de los tres      Docker Compose → infraestructura      Git → Jenkins → Build · Test · Package · Docker Build
```

| Servicio | Puerto | Base de datos | Responsabilidad |
|---|---|---|---|
| [`api-gateway`](api-gateway/) | 8080 (único publicado) | — | punto único de entrada: enruta, `X-Request-Id`, CORS, exige `Bearer` |
| [`auth-service`](auth-service/) | 8081 | `auth_db` | registro, login, emisión del JWT, usuarios |
| [`task-service`](task-service/) | 8082 | `task_db` | tareas, regla de propiedad, endpoints de admin |
| [`config-server`](config-server/) | 8888 | — | sirve la configuración desde [`config-repo/`](config-repo/) |

No hay `pom.xml` padre ni clases compartidas: cada servicio es un proyecto Maven independiente con su propia copia de `common` (validación del token, formato de error).

## Requisitos

- Docker con Compose v2 (unos 3 GB de memoria libres para los 6 contenedores).
- Para trabajar desde el IDE o con Maven: Java 21.

## Arrancar

```bash
cp .env.example .env                  # una sola vez; cambia JWT_SECRET (mínimo 32 caracteres) y las contraseñas
docker compose up --build -d --wait   # 6 contenedores; --wait espera a que todos estén healthy
scripts/smoke-test.sh                 # comprueba todo el sistema de extremo a extremo
docker compose down -v                # para y borra los datos
```

Solo el gateway publica puerto (`http://localhost:8080`). Flujo mínimo:

```bash
curl -X POST localhost:8080/auth/register -H 'Content-Type: application/json' -d '{"name":"Eva","email":"eva@taskhub.com","password":"Secret123"}'
TOKEN=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"luis@taskhub.com","password":"def456"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')
curl -X POST localhost:8080/tasks -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" -d '{"title":"Preparar clase"}'
curl localhost:8080/tasks -H "Authorization: Bearer $TOKEN"
```

Usuarios de ejemplo (solo desarrollo, los siembra `auth-service`):

| id | email | rol | contraseña |
|---|---|---|---|
| 1 | `ana@taskhub.com` | `ADMIN` | `abc123` |
| 2 | `luis@taskhub.com` | `USER` | `def456` |

### Desarrollo desde el IDE o con Maven

```bash
docker compose -f docker-compose.dev.yml up -d   # SOLO la base de datos: un MySQL con dos esquemas (auth_db y task_db) en localhost:3306
export JWT_SECRET='un-secreto-de-al-menos-32-caracteres-para-hs256'   # el MISMO para auth-service y task-service

(cd config-server && ./mvnw spring-boot:run)   # primero: http://localhost:8888
(cd auth-service && ./mvnw spring-boot:run)    # http://localhost:8081
(cd task-service && ./mvnw spring-boot:run)    # http://localhost:8082
(cd api-gateway && ./mvnw spring-boot:run)     # http://localhost:8080
for s in config-server auth-service task-service api-gateway; do (cd $s && ./mvnw test); done   # los tests usan H2, no necesitan Docker
```

El config-server lee `../config-repo` (o `CONFIG_REPO_PATH`). Si ya tenías el volumen de MySQL de clases anteriores, el script que crea las bases solo corre con el volumen vacío:
`docker compose -f docker-compose.dev.yml down -v` y vuelve a subirlo (borra los datos de desarrollo).

## Endpoints (por el gateway, `:8080`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/auth/register` | público | Crea un usuario (siempre `USER`) y devuelve un token |
| POST | `/auth/login` | público | Devuelve un token (HS256, 5 min) |
| GET | `/users/{id}` | el propio usuario o `ADMIN` | `UserSummary` (id, name, role) |
| GET | `/admin/users` | `ADMIN` | Lista de usuarios |
| GET | `/tasks` | autenticado | Tareas del usuario del token |
| POST | `/tasks` | autenticado | Crear; el dueño sale del claim `uid` del token |
| GET · PUT · DELETE | `/tasks/{id}` | dueño o `ADMIN` | Detalle, actualizar, eliminar (404 si la tarea es ajena) |
| PATCH | `/tasks/{id}/status` | dueño o `ADMIN` | `PENDING`, `IN_PROGRESS`, `COMPLETED` |
| GET | `/admin/tasks` | `ADMIN` | Todas las tareas |

Códigos: `401` sin token o token inválido · `403` rol insuficiente · `404` recurso inexistente o ajeno · `409` email ya registrado · `400` validación · `503` servicio caído (gateway).
Todos los errores, también los del gateway, usan el mismo formato (`status`, `message`, `details`, `timestamp`).

## Flujo de autenticación

1. `auth-service` firma un JWT (HS256, claims `sub`, `uid`, `role`) en `/auth/login` y `/auth/register`. Es el único que **firma**.
2. El gateway solo exige que exista `Authorization: Bearer <algo>` fuera de `/auth/**`; **no** lee el token.
3. Cada servicio valida firma y caducidad con el **mismo `JWT_SECRET`** y construye el usuario solo desde los claims, sin consultar su base de datos. Roles y propiedad los decide cada servicio.
4. Contrapartida conocida: el secreto compartido permite *firmar* a quien lo conozca (con RSA solo firmaría auth), y un token de un usuario borrado vale hasta que expira.

## Comportamiento degradado

Al crear una tarea, `task-service` pregunta a `auth-service` si el dueño existe (OpenFeign) protegido con timeout, reintento, circuit breaker y fallback.
Si `auth-service` está caído, lento o con el circuito abierto, la tarea **se acepta** con `ownerVerified: false` (el JWT firmado ya prueba la identidad). Un usuario inexistente da `404`.
Las lecturas no dependen de `auth-service`. Límite conocido: nadie reverifica después las tareas pendientes.

## Pipeline (Jenkins)

[`Jenkinsfile`](Jenkinsfile): **Checkout → Build → Test → Package → Docker Build**, con los cuatro servicios en paralelo y las imágenes etiquetadas con el commit.
Un Jenkins de laboratorio ya configurado está en [`jenkins/`](jenkins/README.md) (monta el socket de Docker: lee el riesgo de seguridad allí).

## Lista de verificación de integración

`scripts/smoke-test.sh` la ejecuta contra el sistema levantado (`--fresh` lo reconstruye desde cero antes; `--down` lo apaga al final) y devuelve código de salida distinto de cero si algo falla.

| # | Punto | Cómo se comprueba |
|---|---|---|
| 1 | `docker compose up` desde cero, 6 servicios `healthy` | `--fresh` |
| 2 | Solo el gateway publica puerto | script |
| 3 | Registro y login por el gateway | script |
| 4 | CRUD de tareas con propiedad (404 en la ajena, ADMIN sí) | script |
| 5 | 401 y 403; `/admin/*` solo ADMIN | script |
| 6 | Llamada Feign OK (`ownerVerified=true`) y dueño inexistente → 404 | script |
| 7 | `auth-service` caído → fallback (201 sin verificar, lecturas OK, 503 en login) y recuperación | script |
| 8 | El gateway vuelve a alcanzar a `auth-service` tras reiniciarlo | script |
| 9 | Configuración servida por el Config Server (perfil `docker`) | script |
| 10 | `X-Request-Id`, CORS y formato de error común en el gateway | script |
| 11 | Pipeline de Jenkins en verde | manual |
| 12 | README y diagramas actualizados | esta página |

## Snapshots del curso

Cada clase es una rama encadenada a la anterior (`main` → `01-…` → … → `14-pro-final`) con un tag `snapshot/<rama>`. Las clases 6 y 15 no tienen código.

| Clase | Rama | Qué añade |
|---|---|---|
| 0 | `main` (tag `00-base`) | Monolito Spring Boot con arquitectura hexagonal, JPA y MySQL |
| 1 | `01-jwt-security` | Registro, login, JWT, roles y propiedad de recursos |
| 2 | `02-patterns` | Puertos y adaptadores, Strategy + Factory, ArchUnit |
| 3 | `03-microservices-design` | Diseño y diagramas (solo documentación) |
| 4 | `04-expert-final` | Fronteras `auth` / `task` dentro del monolito |
| 5 | `05-expert-final-reviewed` | Code review: 6 defectos corregidos |
| 7 | `07-microservices` | `auth-service` y `task-service` independientes, una base de datos cada uno |
| 8 | `08-config-server` | Spring Cloud Config y perfiles |
| 9 | `09-api-gateway` | Spring Cloud Gateway |
| 10 | `10-service-communication` | OpenFeign entre servicios |
| 11 | `11-resilience` | Timeout, Retry, Circuit Breaker y Fallback (Resilience4j) |
| 12 | `12-docker` | Dockerfile multi-stage por servicio |
| 13 | `13-jenkins` | Docker Compose completo y Jenkinsfile |
| 14 | `14-pro-final` | Integración, script de humo, documentación final |

Los nombres de snapshot de los lectures (`05-microservices`, `12-pro-final`…) difieren de los de las ramas; las ramas son la referencia.
