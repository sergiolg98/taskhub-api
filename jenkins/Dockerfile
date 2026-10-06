# Jenkins para el laboratorio de la clase 13: Java 21 (los builds usan ./mvnw de cada servicio) + CLI de Docker para construir las imágenes.
FROM jenkins/jenkins:lts-jdk21

USER root
# Solo el CLI de Docker (cliente), no el motor: las órdenes `docker` se ejecutan contra el Docker del anfitrión (socket montado).
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl \
 && install -m 0755 -d /etc/apt/keyrings \
 && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
 && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" > /etc/apt/sources.list.d/docker.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends docker-ce-cli docker-buildx-plugin \
 && rm -rf /var/lib/apt/lists/* \
 # el repositorio puede venir de un volumen con otro propietario
 && git config --system --add safe.directory '*'

USER jenkins
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

COPY casc.yaml /usr/share/jenkins/ref/casc.yaml
# Sin asistente de instalación: usuario y job salen de casc.yaml (Configuration as Code)
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false" \
    CASC_JENKINS_CONFIG=/usr/share/jenkins/ref/casc.yaml
