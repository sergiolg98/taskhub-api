// Pipeline declarativo de TaskHub: Checkout -> Build -> Test -> Package -> Docker Build.
// Los cuatro servicios son proyectos Maven independientes, así que cada stage los recorre en paralelo.
// Requisitos del agente: Java 21 y el CLI de Docker con acceso a un demonio (ver jenkins/).

def services = ['config-server', 'auth-service', 'task-service', 'api-gateway']

// Una rama paralela por servicio: en la vista de stages se ve cuál falla.
def perService = { Closure body ->
    services.collectEntries { svc -> [(svc): { dir(svc) { body(svc) } }] }
}

pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 40, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    // Versionado de imágenes: una etiqueta por commit (en un registro real se haría push con esta etiqueta)
                    env.IMAGE_TAG = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                }
                echo "Commit ${env.IMAGE_TAG}"
            }
        }

        stage('Build') {
            steps {
                script { parallel perService { svc -> sh './mvnw -B -q -DskipTests compile' } }
            }
        }

        stage('Test') {
            steps {
                script {
                    parallel services.collectEntries { svc ->
                        [(svc): {
                            try {
                                dir(svc) { sh './mvnw -B test' }
                            } finally {
                                // el informe se publica aunque el test falle
                                junit allowEmptyResults: true, testResults: "${svc}/target/surefire-reports/*.xml"
                            }
                        }]
                    }
                }
            }
        }

        stage('Package') {
            steps {
                script { parallel perService { svc -> sh './mvnw -B -q -DskipTests package' } }
                archiveArtifacts artifacts: '*/target/*.jar', excludes: '*/target/*.original', fingerprint: true
            }
        }

        stage('Docker Build') {
            // Cada Dockerfile vuelve a compilar dentro de la imagen (multi-stage): la imagen es reproducible
            // y no depende de lo que haya en el agente. El coste es compilar dos veces.
            steps {
                script {
                    parallel services.collectEntries { svc ->
                        [(svc): { sh "docker build -t taskhub/${svc}:${env.IMAGE_TAG} ${svc}" }]
                    }
                }
            }
        }
    }

    post {
        success { echo "Imágenes construidas: taskhub/*:${env.IMAGE_TAG}" }
        failure { echo 'El pipeline se detuvo en el primer stage que falló; los siguientes no se ejecutan.' }
    }
}
