# TaskHub — snapshot `13-jenkins`

Proyecto guía del curso. Dos servicios independientes (`07-microservices`), cada uno con su proyecto Maven y su base de datos,
cuya configuración vive en un **Config Server** (`08-config-server`) y que ahora se usan a través de un **API Gateway** (`09-api-gateway`):

| Servicio | Puerto | Base de datos | Responsabilidad |
|---|---|---|---|
| [`auth-service`](auth-service/) | 8081 | `auth_db` | registro, login, emisión del JWT, usuarios |
| [`task-service`](task-service/) | 8082 | `task_db` | tareas, regla de propiedad, endpoints de admin |
| [`api-gateway`](api-gateway/) | 8080 | — | punto único de entrada: enruta a los dos servicios |
| [`config-server`](config-server/) | 8888 | — | sirve la configuración de los dos servicios desde [`config-repo/`](config-repo/) |

No hay `pom.xml` padre ni clases compartidas: cada servicio lleva su **propia copia** de `common` (validación del token, formato de error).
Se hace así a propósito: compartir código entre servicios los acopla.

## Requisitos

- Java 21+
- Docker (para MySQL)

## Arrancar todo con Docker Compose (clase 13)

```bash
cp .env.example .env                  # una sola vez; cambia JWT_SECRET (mínimo 32 caracteres) y las contraseñas
docker compose up --build -d --wait   # 6 contenedores; --wait espera a que todos estén healthy
curl localhost:8080/actuator/health   # el gateway: {"status":"UP"}
docker compose down -v                # para y borra los datos
```

Flujo de comprobación (solo el gateway publica puerto, `:8080`):

```bash
curl -X POST localhost:8080/auth/register -H 'Content-Type: application/json' -d '{"name":"Eva","email":"eva@taskhub.com","password":"Secret123"}'
TOKEN=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"luis@taskhub.com","password":"def456"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')
curl -X POST localhost:8080/tasks -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" -d '{"title":"Preparar clase"}'
```

| Servicio de Compose | Imagen / origen | Espera a (`condition: service_healthy`) |
|---|---|---|
| `mysql-auth`, `mysql-task` | `mysql:8.4`, cada uno con su base | — |
| `config-server` | `./config-server` (monta `config-repo` solo lectura) | — |
| `auth-service` | `./auth-service` | `mysql-auth`, `config-server` |
| `task-service` | `./task-service` | `mysql-task`, `config-server` |
| `api-gateway` (publica `8080`) | `./api-gateway` | `config-server`, `auth-service`, `task-service` |

- **Orden de arranque real:** lo garantizan los *healthchecks*, no `depends_on` a secas (que solo espera a que el contenedor *arranque*, no a que responda).
  Trampa observada: el healthcheck de MySQL con `-h localhost` daba `healthy` durante la inicialización (la imagen levanta un servidor temporal sin red que sí responde por socket) y `auth-service` fallaba con *Communications link failure*. Se comprueba por TCP (`-h 127.0.0.1`).
- **Perfil `docker`** de `config-repo` (`*-docker.properties`): URLs por nombre de servicio (`mysql-auth`, `auth-service`...), nunca `localhost`. Un test del config-server lo vigila.
- **Secretos** solo por variables de entorno / `.env` (git ignora `.env`; `.env.example` lleva valores de muestra). Compose falla al arrancar si faltan `JWT_SECRET` o las contraseñas.
- **Memoria:** `JAVA_TOOL_OPTIONS=-Xmx256m` por JVM. Con los 6 contenedores, el consumo observado fue ~2,3 GB.
- **Imágenes** con etiqueta `taskhub/<servicio>:${IMAGE_TAG:-latest}`.

## Jenkins (clase 13)

El `Jenkinsfile` de la raíz define el pipeline **Checkout → Build → Test → Package → Docker Build**; cada stage recorre en paralelo los cuatro servicios.
Para tener un Jenkins ya configurado (usuario y job por *Configuration as Code*): [`jenkins/README.md`](jenkins/README.md). **Montar el socket de Docker es un riesgo de seguridad** (equivale a ser root en la máquina); allí se explica.

## Desarrollo desde el IDE o con Maven

```bash
cp .env.example .env                  # una sola vez
docker compose -f docker-compose.dev.yml up -d   # SOLO la base de datos: un MySQL con dos esquemas (auth_db y task_db) en localhost:3306
export JWT_SECRET='un-secreto-de-al-menos-32-caracteres-para-hs256'   # el MISMO para los dos servicios

(cd config-server && ./mvnw spring-boot:run)   # terminal 1 → http://localhost:8888 (arrancar primero)
(cd auth-service && ./mvnw spring-boot:run)    # terminal 2 → http://localhost:8081
(cd task-service && ./mvnw spring-boot:run)    # terminal 3 → http://localhost:8082
(cd api-gateway && ./mvnw spring-boot:run)     # terminal 4 → http://localhost:8080 (la única URL que usa el cliente)
for s in config-server auth-service task-service api-gateway; do (cd $s && ./mvnw test); done   # sin Docker
```

El config-server lee `../config-repo`: arráncalo desde su carpeta o define `CONFIG_REPO_PATH`.

> Si ya tenías el volumen de MySQL de las clases anteriores, el script que crea `auth_db` y `task_db` no se ejecuta
> (solo corre con el volumen vacío). Haz `docker compose -f docker-compose.dev.yml down -v && docker compose -f docker-compose.dev.yml up -d` (borra los datos de desarrollo).

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

Desde la clase 9 el cliente solo habla con el gateway (`:8080`); los puertos 8081 y 8082 siguen abiertos en local, pero conceptualmente son internos.

```bash
TOKEN=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"luis@taskhub.com","password":"def456"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')

curl -X POST localhost:8080/tasks -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"Preparar clase"}'                       # el gateway enruta a task-service
curl localhost:8080/tasks -H "Authorization: Bearer $TOKEN"
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

## API Gateway (clase 9)

```text
Cliente → api-gateway :8080 → auth-service :8081 | task-service :8082
```

Spring Cloud Gateway (reactivo, WebFlux: **nunca** `spring-boot-starter-web` en este proyecto). Sus rutas, CORS y timeouts están en `config-repo/api-gateway*.properties`.

| Ruta en `:8080` | Destino |
|---|---|
| `/auth/**`, `/users/**`, `/admin/users/**` | `auth-service` |
| `/tasks/**`, `/admin/tasks/**` | `task-service` |

Las rutas no se reescriben (sin `StripPrefix`): los servicios exponen los mismos paths que el gateway.

**Qué valida el gateway** (y solo esto):
- Fuera de `/auth/**` exige `Authorization: Bearer <algo>`; si falta responde `401` sin llegar al servicio. **No** lee ni confía en el token.
- Añade `X-Request-Id` (reutiliza el del cliente si es inocuo; si no, genera un UUID) hacia el servicio y de vuelta al cliente, y registra una línea por petición.
- Responde el CORS (único sitio donde se configura) y devuelve el formato `ErrorResponse` también cuando falla él mismo: `503` servicio caído, `504` timeout, `404` ruta inexistente.

**Qué valida cada servicio:** firma y caducidad del JWT, roles (`/admin/**`) y propiedad de los recursos. Es el **modelo A** (el gateway enruta, el servicio valida): defense in depth, y un servicio
sigue protegido aunque alguien lo llame sin pasar por el gateway. El coste: el JWT se valida en cada servicio. En el modelo B (el gateway valida y los servicios confían) hay un solo punto de validación, pero cualquier acceso directo a un servicio queda sin proteger.
Revocación de tokens: no se implementa (el token dura 5 minutos).

Pregunta de clase: la regla «solo el dueño edita su tarea» vive en el servicio, no en el gateway (el gateway no conoce el dominio).

## Comunicación entre servicios (clase 10)

```text
Cliente → api-gateway → task-service ──(OpenFeign, directo)──▶ auth-service  GET /users/{id}
```

Al **crear una tarea**, `task-service` pregunta a `auth-service` si el dueño (el `uid` del token) existe. Se eligió la validación del dueño (no enriquecer el detalle de la tarea).

- **Hexagonal intacta:** el puerto `UserLookupPort` y los casos de uso son los de la clase 4; solo cambia el adaptador (`UserLookupLocalAdapter` → `UserLookupFeignAdapter`). El `@FeignClient` (`AuthClient`) vive en `task.infrastructure.lookup`, y ArchUnit impide que cualquier otro paquete conozca Feign.
- **Contrato:** `GET /users/{id}` → `{id, name, role}` (nunca email ni contraseña). Lo **posee** `auth-service`; `task-service` mantiene su propia copia (`UserSummaryResponse`, `UserSummary`). Cero clases compartidas.
- **La llamada va directa a `auth-service`** (`taskhub.auth-service.url` en `config-repo`), no por el gateway: es tráfico interno.
- **Errores** (`AuthClientConfig`): `404` → el usuario no existe (`OwnerNotFoundException`, 404 al cliente); los fallos técnicos se tratan en la clase 11 (abajo). «No existe» y «no puedo saberlo» son cosas distintas. No se lee el cuerpo del error (puede no ser JSON).
- **Propagación:** un `RequestInterceptor` reenvía `Authorization` (la llamada es en nombre del usuario; `auth-service` solo deja consultarse a uno mismo o a un ADMIN, y el dueño siempre es el propio solicitante) y `X-Request-Id`. La alternativa, credenciales de servicio, no se implementa.
- **Timeouts** (`config-repo/task-service.properties`): `connect-timeout=1000`, `read-timeout=2000`. Feign no reintenta por sí mismo (`NEVER_RETRY`); los reintentos los pone Resilience4j (clase 11).
- **Contrato y versionado:** si `auth-service` cambia el JSON, `task-service` lo descubre en ejecución. Añadir campos es compatible (se ignoran); quitarlos o renombrarlos no.

## Docker (clase 12)

Cada servicio tiene su `Dockerfile` y su `.dockerignore`. Construir desde la carpeta del servicio:

```bash
cd auth-service && docker build -t taskhub/auth-service .      # idem config-server, api-gateway, task-service
```

La primera construcción tarda unos minutos (descarga las dependencias de Maven); las siguientes aprovechan la caché.

- **Multi-stage:** la etapa `build` (Maven + JDK) compila el JAR; la etapa final solo lleva el JRE y la aplicación. No hay Maven ni código fuente en la imagen.
- **Capas de Spring Boot** (`-Djarmode=tools extract --layers`): dependencias, loader y aplicación en capas separadas. Con un cambio de una línea de código solo se reconstruye la capa de la aplicación (comprobado).
- **Usuario no root** (`app`, uid 10001) y `-XX:MaxRAMPercentage=75`.
- **`HEALTHCHECK`** contra `/actuator/health` (el puerto está escrito en cada Dockerfile). `eclipse-temurin:21-jre` trae `curl`; en la variante `-alpine` habría que usar `wget`.
- **Los tests no se ejecutan al construir la imagen** (`-DskipTests`): tienen su propia etapa en el pipeline (clase 13).
- **Sin configuración ni secretos dentro de la imagen:** todo llega por variables de entorno (`SPRING_PROFILES_ACTIVE`, `CONFIG_SERVER_URL`, `JWT_SECRET`, `DB_USER`, `DB_PASSWORD`, `JAVA_TOOL_OPTIONS`). El `config-repo` se **monta** en `config-server` (`-v ./config-repo:/config-repo:ro`).
- **`localhost` dentro de un contenedor es el propio contenedor:** sin `CONFIG_SERVER_URL` el servicio busca el config-server en `localhost:8888` y no lo encuentra. En una red de Docker los servicios se llaman por el **nombre del contenedor**, y el perfil `prod` de `config-repo` ya usa esos nombres (`mysql-auth`, `mysql-task`, `auth-service`, `task-service`).

Ejemplo manual (la clase 13 lo automatiza con Compose); solo el gateway publica puerto:

```bash
docker network create taskhub-net
docker run -d --name mysql-auth --network taskhub-net -e MYSQL_DATABASE=auth_db -e MYSQL_USER=taskhub -e MYSQL_PASSWORD=taskhub -e MYSQL_ROOT_PASSWORD=root mysql:8.4
docker run -d --name config-server --network taskhub-net -v "$PWD/config-repo:/config-repo:ro" taskhub/config-server
docker run -d --name auth-service --network taskhub-net -e SPRING_PROFILES_ACTIVE=prod -e CONFIG_SERVER_URL=http://config-server:8888 \
  -e JWT_SECRET='un-secreto-de-al-menos-32-caracteres-para-hs256' -e DB_USER=taskhub -e DB_PASSWORD=taskhub taskhub/auth-service
```

## Resiliencia (clase 11)

```text
task-service → Retry( CircuitBreaker( OpenFeign con timeouts ) ) → auth-service
                     └─ sin respuesta fiable → Fallback: Unavailable
```

Sobre la única llamada entre servicios (`UserLookupFeignAdapter.findById`), con Resilience4j (`resilience4j-spring-boot3`, anotaciones `@Retry` y `@CircuitBreaker`; se eligió sobre `spring-cloud-starter-circuitbreaker-resilience4j` porque da anotaciones, propiedades y Actuator sin más piezas). Parámetros en `config-repo/task-service.properties`:

| Pieza | Valor | Por qué |
|---|---|---|
| Timeout (Feign) | conexión 1 s, lectura 2 s | menor que lo que el cliente esperaría; un auth colgado no retiene hilos |
| Retry | 2 intentos, 300 ms entre ellos | solo reintenta `ExternalServiceException` (5xx, conexión rechazada, timeout); la consulta es un `GET`, idempotente |
| Circuit Breaker | ventana de 10 llamadas, mínimo 5, abre con ≥ 50 % de fallos, 10 s en `OPEN`, 3 llamadas de prueba en `HALF_OPEN` | pasa solo de `OPEN` a `HALF_OPEN` tras la espera |
| Cuenta como fallo | solo `ExternalServiceException` | un `404` (el usuario no existe) y un `4xx` de rechazo son respuestas de un servicio sano: ni abren el circuito ni se reintentan |

- **Resultado explícito** (`UserLookupResult`): `Found | NotFound | Unavailable`. Ya no es un `Optional`, que mezclaba «no existe» con «no pude preguntar».
- **Decisión de negocio:** si el dueño no se puede verificar (`Unavailable`), **`POST /tasks` acepta la tarea** (el JWT firmado ya prueba la identidad) y la marca con `ownerVerified: false` (columna nueva, migración `V2`). Rechazar todas las escrituras mientras auth esté caído sería peor para el usuario. Un fallback engañoso sería inventar un usuario. La tarea sigue sin verificar: no hay todavía un proceso que la verifique después (queda fuera de alcance).
- **Orden de los aspectos:** Retry envuelve al Circuit Breaker. El *fallback* va en `@Retry` (el más externo); si estuviera en `@CircuitBreaker` se tragaría la excepción y no habría reintentos. Con el circuito abierto no se reintenta (`CallNotPermittedException` ignorada).
- **Trampas conocidas:** las anotaciones solo funcionan si la llamada pasa por el proxy de Spring (otro bean llama al adaptador; un `this.método()` no); el fallback debe tener los mismos parámetros más un `Throwable`.
- **Versiones:** el BOM de Spring Cloud fija los módulos `resilience4j-*` en 2.2.0; con `resilience4j-spring-boot3` 2.3.0 la app no arranca (`NoClassDefFoundError`). Se importa el `resilience4j-bom` 2.3.0 **antes** que el de Spring Cloud en `task-service/pom.xml`.
- **Actuator** (`task-service`): `/actuator/health` es público; `/actuator/circuitbreakers` y `/actuator/circuitbreakerevents/authService` requieren `ADMIN`. El breaker **no** entra en `/actuator/health` (si lo hiciera, un auth caído marcaría task-service como no sano).
- **Demo de lentitud** (`auth-service`, solo perfil `dev`): `POST /admin/dev/delay?ms=5000` (ADMIN) retrasa `GET /users/**`; `DELETE /admin/dev/delay` lo quita. No existe en test ni prod.

## Cómo se relacionan los dos servicios

- **El token es el único contrato.** `auth-service` lo firma (HS256) con claims `sub`, `uid` y `role`; `task-service` solo comprueba la firma y la
  caducidad con el mismo `JWT_SECRET` y construye el usuario **solo desde los claims**. No hay llamadas entre servicios.
- **`tasks.owner_id` no tiene clave foránea.** `users` vive en otra base de datos y una restricción no cruza bases.
- **La comprobación del dueño volvió en la clase 10** con una llamada HTTP (ver abajo).
- **Con `auth-service` apagado**, `task-service` sigue validando tokens y atendiendo todo; al crear una tarea no puede comprobar el dueño y la acepta como **no verificada** (clase 11).
- Trade-off del secreto compartido: quien conoce `JWT_SECRET` puede *firmar* tokens, no solo validarlos. Con RSA (clave privada en auth, pública en task) solo `auth-service` firma; se comenta en clase.

## Arquitectura

Cada servicio mantiene la hexagonal de siempre (`domain` · `application` · `infrastructure`) más su `common`. `ArchitectureTest` (en cada servicio) lo comprueba;
en `task-service` además prohíbe cualquier dependencia de `com.taskhub.auth..`.

## Pendiente a propósito (clases siguientes)

Integración y revisión final (14).
