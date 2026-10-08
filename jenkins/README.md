# Jenkins del laboratorio (clase 13)

Jenkins en Docker, ya configurado: Java 21, CLI de Docker, plugins y un job `taskhub` que ejecuta el `Jenkinsfile` de la raíz.
Usuario y job salen de `casc.yaml` (*Configuration as Code*), sin asistente de instalación.

```bash
JENKINS_ADMIN_PASSWORD='elige-una-contraseña' docker compose -f jenkins/docker-compose.yml up --build -d
# http://localhost:8090  (usuario: admin)  ->  job "taskhub"  ->  Build Now
```

Variables: `JENKINS_ADMIN_PASSWORD` (obligatoria), `TASKHUB_REPO_URL` (por defecto el repositorio de GitHub) y `TASKHUB_BRANCH` (por defecto `*/main`; para un snapshot,
p. ej. `*/13-jenkins`). Si el repositorio es privado hay que añadir credenciales al job.

## Cómo funciona

- **Pipeline as code:** el job solo apunta al repositorio; los stages viven en el `Jenkinsfile`.
- **Build / Test / Package** usan el `./mvnw` de cada servicio (Maven se descarga la primera vez, por eso la primera ejecución es más lenta). Los informes de `surefire` se publican con `junit` aunque el test falle, y los JAR se archivan.
- **Docker Build** ejecuta `docker build -t taskhub/<servicio>:<commit>` por servicio. Cada Dockerfile compila otra vez dentro de la imagen (multi-stage): es reproducible, a costa de compilar dos veces.
- **Versionado de imágenes:** una etiqueta por commit (`git rev-parse --short HEAD`). Subirlas a un registro (`docker push`) queda como concepto, no está implementado.

## Riesgo de seguridad: el socket de Docker

`docker-compose.yml` monta `/var/run/docker.sock` dentro de Jenkins para que los builds puedan usar el Docker de tu máquina. Quien controle ese Jenkins
(cualquiera que pueda editar un `Jenkinsfile` que se ejecute) puede arrancar contenedores privilegiados y, con ello, **ser root en tu máquina**.
Además corre como root porque en Docker Desktop el socket es `root:root` con permisos `660`. Es aceptable en un laboratorio local; en un servidor compartido
se usan agentes aislados con Docker, Docker *rootless* o constructores sin demonio (Kaniko, BuildKit).

## Detalles que conviene saber

- Los plugins se instalan al construir la imagen (`plugins.txt`). El paso `timestamps()` pertenece al plugin **`timestamper`** (el id `timestamps` no existe).
- El plugin Git **bloquea por defecto los repositorios locales** (`file://`). Con la URL de GitHub no hace falta; para probar con un directorio local hay que arrancar con
  `-Dhudson.plugins.git.GitSCM.ALLOW_LOCAL_CHECKOUT=true` (en `JAVA_OPTS`). No está activado aquí.
- El resultado `UNSTABLE` aparece durante la ejecución en cuanto `junit` ve un test fallido; el resultado final de un test roto es `FAILURE`.
