#!/usr/bin/env bash
# Checklist de integración de TaskHub, ejecutada de extremo a extremo contra el sistema en Docker Compose.
#
#   scripts/smoke-test.sh            comprueba el sistema que ya está levantado
#   scripts/smoke-test.sh --fresh    antes hace `docker compose down -v && up --build --wait` (arranque desde cero)
#   scripts/smoke-test.sh --down     al terminar hace `docker compose down -v`
#
# Código de salida: 0 si todo pasa, 1 si algo falla. Solo necesita bash, curl, docker compose y (opcional) openssl.
# El secreto del JWT (solo para el caso del «dueño inexistente») se lee de JWT_SECRET o de .env; si no está, ese punto se omite.
set -u
cd "$(dirname "$0")/.."

BASE="${BASE:-http://localhost:8080}"
FRESH=0; DOWN=0
for arg in "$@"; do
  case "$arg" in --fresh) FRESH=1 ;; --down) DOWN=1 ;; *) echo "opción desconocida: $arg"; exit 2 ;; esac
done

PASS=0; FAIL=0; SKIP=0
RUN_ID="$(date +%s)"
BODY=""; CODE=""; HEADERS=""

ok()   { PASS=$((PASS + 1)); printf '  \033[32mOK\033[0m   %s\n' "$1"; }
bad()  { FAIL=$((FAIL + 1)); printf '  \033[31mFALLA\033[0m %s\n         esperado: %s\n         obtenido: %s\n' "$1" "$2" "$3"; }
skip() { SKIP=$((SKIP + 1)); printf '  \033[33mOMITIDO\033[0m %s (%s)\n' "$1" "$2"; }
section() { printf '\n\033[1m%s\033[0m\n' "$1"; }

# req METHOD PATH [TOKEN] [JSON_BODY]  ->  CODE, BODY y HEADERS
req() {
  local method="$1" path="$2" token="${3:-}" data="${4:-}" args=(-s -D - -o /dev/stderr -X "$1")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$data" ] && args+=(-H 'Content-Type: application/json' -d "$data")
  local out; out="$(curl "${args[@]}" "$BASE$path" 2>/tmp/taskhub-smoke-body)"
  HEADERS="$out"; CODE="$(printf '%s' "$out" | head -1 | awk '{print $2}')"; BODY="$(cat /tmp/taskhub-smoke-body)"
}

expect_code() { # descripción, código esperado
  if [ "$CODE" = "$2" ]; then ok "$1 -> $2"; else bad "$1" "HTTP $2" "HTTP $CODE ${BODY:0:120}"; fi
}
expect_body() { # descripción, texto que debe contener
  if printf '%s' "$BODY" | grep -q -- "$2"; then ok "$1"; else bad "$1" "cuerpo con «$2»" "${BODY:0:140}"; fi
}
json_field() { printf '%s' "$BODY" | sed -E "s/.*\"$1\":\"?([^\",}]*)\"?.*/\1/"; }
login() { req POST /auth/login "" "{\"email\":\"$1\",\"password\":\"$2\"}"; json_field token; }

compose_service_states() { docker compose ps --format '{{.Service}} {{.Health}}'; }

if [ "$FRESH" = 1 ]; then
  section "Arranque desde cero"
  docker compose down -v >/dev/null 2>&1
  if docker compose up --build -d --wait >/tmp/taskhub-smoke-up.log 2>&1; then ok "docker compose up --build --wait"; else bad "docker compose up --build --wait" "todos healthy" "$(tail -3 /tmp/taskhub-smoke-up.log)"; fi
fi

section "1. Infraestructura (Docker Compose)"
for svc in mysql-auth mysql-task config-server auth-service task-service api-gateway; do
  state="$(compose_service_states | awk -v s="$svc" '$1==s {print $2}')"
  [ "$state" = "healthy" ] && ok "$svc está healthy" || bad "$svc healthy" "healthy" "${state:-no existe}"
done
# solo los 6 servicios de este Compose (el contenedor de desarrollo de docker-compose.dev.yml comparte proyecto y no cuenta)
published="$(docker compose --file docker-compose.yml ps --format '{{.Service}}|{{.Ports}}' | grep -E '^(mysql-auth|mysql-task|config-server|auth-service|task-service|api-gateway)\|' | grep -- '->' | cut -d'|' -f1 | sort | tr '\n' ' ')"
[ "$published" = "api-gateway " ] && ok "solo api-gateway publica puerto al host" || bad "puertos publicados" "solo api-gateway" "${published:-ninguno}"

section "2. Registro y login por el gateway"
EMAIL="smoke-$RUN_ID@taskhub.com"
req POST /auth/register "" "{\"name\":\"Smoke\",\"email\":\"$EMAIL\",\"password\":\"Secret123\"}"
expect_code "POST /auth/register" 201
req POST /auth/register "" "{\"name\":\"Smoke\",\"email\":\"$EMAIL\",\"password\":\"Secret123\"}"
expect_code "registrar el mismo email otra vez" 409
req POST /auth/login "" "{\"email\":\"$EMAIL\",\"password\":\"mala\"}"
expect_code "login con contraseña errónea" 401
LUIS="$(login luis@taskhub.com def456)"; [ -n "$LUIS" ] && ok "login de Luis (USER) devuelve token" || bad "login de Luis" "token" "$BODY"
ANA="$(login ana@taskhub.com abc123)";   [ -n "$ANA" ] && ok "login de Ana (ADMIN) devuelve token" || bad "login de Ana" "token" "$BODY"
EVA="$(login "$EMAIL" Secret123)";       [ -n "$EVA" ] && ok "login del usuario recién registrado" || bad "login del usuario nuevo" "token" "$BODY"

section "3. CRUD de tareas y llamada Feign a auth-service"
req POST /tasks "$LUIS" '{"title":"Smoke","description":"d"}'
expect_code "POST /tasks (Luis)" 201
expect_body "el dueño se verificó con auth-service (OpenFeign)" '"ownerVerified":true'
TASK="$(json_field id)"
req GET "/tasks/$TASK" "$LUIS";                         expect_code "GET /tasks/{id} (dueño)" 200
req GET /tasks "$LUIS";                                  expect_body "GET /tasks lista la tarea" "\"id\":$TASK"
req PUT "/tasks/$TASK" "$LUIS" '{"title":"Smoke 2","description":null}'; expect_code "PUT /tasks/{id}" 200
req PATCH "/tasks/$TASK/status" "$LUIS" '{"status":"COMPLETED"}'; expect_body "PATCH /tasks/{id}/status" '"status":"COMPLETED"'
req POST /tasks "$LUIS" '{"title":""}';                  expect_code "título vacío" 400

section "4. Propiedad de los recursos"
req GET "/tasks/$TASK" "$EVA";   expect_code "otro usuario ve la tarea ajena como inexistente" 404
req DELETE "/tasks/$TASK" "$EVA"; expect_code "otro usuario no puede borrarla" 404
req GET "/tasks/$TASK" "$ANA";   expect_code "un ADMIN sí la ve" 200
req DELETE "/tasks/$TASK" "$LUIS"; expect_code "el dueño la borra" 204
req GET "/tasks/$TASK" "$LUIS";  expect_code "ya no existe" 404

section "5. Seguridad: 401 y 403"
req GET /tasks;                              expect_code "sin token" 401
req GET /tasks "garbage";                    expect_code "token basura (lo rechaza el servicio, no el gateway)" 401
req GET /admin/tasks "$LUIS";                expect_code "/admin/tasks con USER" 403
req GET /admin/users "$LUIS";                expect_code "/admin/users con USER" 403
req GET /admin/tasks "$ANA";                 expect_code "/admin/tasks con ADMIN" 200
req GET /admin/users "$ANA";                 expect_code "/admin/users con ADMIN" 200
req GET /users/2 "$LUIS";                    expect_body "GET /users/{id} solo expone id, name y rol" '"role":"USER"'
printf '%s' "$BODY" | grep -qE 'password|email' && bad "UserSummary sin email ni contraseña" "sin esos campos" "$BODY" || ok "UserSummary sin email ni contraseña"
req GET /users/1 "$LUIS";                    expect_code "consultar a otro usuario siendo USER" 403

section "6. Dueño inexistente (token válido de un usuario que no existe)"
SECRET="${JWT_SECRET:-$(grep -E '^JWT_SECRET=' .env 2>/dev/null | cut -d= -f2-)}"
if [ -n "$SECRET" ] && command -v openssl >/dev/null; then
  b64() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }
  header="$(printf '{"alg":"HS256"}' | b64)"
  payload="$(printf '{"sub":"ghost@taskhub.com","uid":999999,"role":"USER","exp":%s}' "$(( $(date +%s) + 600 ))" | b64)"
  sig="$(printf '%s.%s' "$header" "$payload" | openssl dgst -sha256 -hmac "$SECRET" -binary | b64)"
  req POST /tasks "$header.$payload.$sig" '{"title":"fantasma"}'
  expect_code "POST /tasks con uid inexistente" 404
else
  skip "POST /tasks con uid inexistente" "falta JWT_SECRET (variable o .env) u openssl"
fi

section "7. Gateway: cabeceras, CORS y formato de error"
req GET /tasks "$LUIS"
printf '%s' "$HEADERS" | grep -qi '^x-request-id:' && ok "respuesta con X-Request-Id" || bad "X-Request-Id" "cabecera presente" "ausente"
out="$(curl -s -i -X OPTIONS "$BASE/tasks" -H 'Origin: http://localhost:3000' -H 'Access-Control-Request-Method: POST')"
printf '%s' "$out" | grep -qi '^access-control-allow-origin: http://localhost:3000' && ok "preflight CORS respondido por el gateway" || bad "preflight CORS" "Allow-Origin" "$(printf '%s' "$out" | head -3)"
req GET /ruta-que-no-existe "$LUIS"; expect_code "ruta inexistente" 404
expect_body "el error del gateway usa el formato común" '"status":404'

section "8. Configuración centralizada (Config Server)"
cfg="$(docker compose exec -T api-gateway curl -s http://config-server:8888/task-service/docker 2>/dev/null)"
printf '%s' "$cfg" | grep -q 'mysql-task:3306/task_db' && ok "task-service/docker se sirve con URLs por nombre de servicio" || bad "config de task-service" "mysql-task:3306" "${cfg:0:120}"
for svc in auth-service task-service api-gateway; do
  docker compose logs "$svc" 2>&1 | grep -q 'Located environment: name='"$svc"', profiles=\[docker\]' \
    && ok "$svc pidió su configuración al Config Server (perfil docker)" || bad "$svc pidió configuración" "Located environment ... docker" "no aparece en los logs"
done

section "9. Resiliencia: auth-service caído"
docker compose stop auth-service >/dev/null 2>&1
req POST /tasks "$LUIS" '{"title":"con auth caído"}'
expect_code "POST /tasks con auth-service apagado" 201
expect_body "se acepta sin verificar (fallback)" '"ownerVerified":false'
DEGRADED="$(json_field id)"
req GET /tasks "$LUIS";                 expect_code "las lecturas siguen funcionando" 200
req POST /auth/login "" '{"email":"luis@taskhub.com","password":"def456"}'; expect_code "/auth/login con auth apagado (503 del gateway)" 503
expect_body "el 503 usa el formato común" '"status":503'
docker compose start auth-service >/dev/null 2>&1
recovered=0
for _ in $(seq 1 40); do   # arranque de auth-service + los 10 s del circuito abierto
  req POST /tasks "$LUIS" '{"title":"recuperación"}'
  if printf '%s' "$BODY" | grep -q '"ownerVerified":true'; then recovered=1; break; fi
  sleep 2
done
[ "$recovered" = 1 ] && ok "al volver auth-service, el circuito se cierra y las tareas vuelven a verificarse" || bad "recuperación tras el fallo" "ownerVerified=true en <80 s" "${BODY:0:120}"
# el gateway debe volver a encontrar a auth-service aunque el contenedor haya cambiado de IP (DNS cacheado, hallazgo de la clase 14)
# (req y no login: login corre en una subshell y su $CODE no llegaría aquí)
for _ in $(seq 1 30); do
  req POST /auth/login "" '{"email":"luis@taskhub.com","password":"def456"}'
  [ "$CODE" = 200 ] && break
  sleep 2
done
LUIS="$(json_field token)"
[ "$CODE" = 200 ] && ok "el gateway vuelve a alcanzar a auth-service (/auth/login -> 200)" || bad "el gateway alcanza a auth-service tras reiniciarlo" "HTTP 200 en <60 s" "HTTP $CODE"
# limpieza de lo creado por la prueba
req GET /tasks "$LUIS"
for id in $(printf '%s' "$BODY" | grep -o '"id":[0-9]*' | cut -d: -f2); do req DELETE "/tasks/$id" "$LUIS" >/dev/null; done

[ "$DOWN" = 1 ] && docker compose down -v >/dev/null 2>&1

section "Resumen"
printf '  %s correctas, %s fallos, %s omitidas\n' "$PASS" "$FAIL" "$SKIP"
[ "$FAIL" = 0 ]
