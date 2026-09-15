pipeline {

    agent {
        node { label 'jenkins-node-label-1' }
    }

    environment {
        DOCKER_HUB_CREDENTIALS = 'docker-hub-credentials'
        DOCKER_HUB_IMAGE_NAME = 'indigopaas/orchestrator'
        HARBOR_CREDENTIALS = 'harbor-paas-credentials'
        HARBOR_IMAGE_NAME = 'datacloud-middleware/orchestrator'
    }

    stages {

        stage('checkout and compiling') {
            agent {
                docker {
                    label 'jenkinsworker00'
                    image 'maven:3.5.4-ibmjava-8'
                    args '--privileged'
                    reuseNode true
                }
            }
            steps {
                configFileProvider([configFile(fileId: 'maven-nexus-settings.xml', variable: 'MAVEN_SETTINGS')]) {
                    sh 'mvn -s $MAVEN_SETTINGS clean install'
                }
            }
        }

        stage('Build and tag Docker Image') {
            steps {
                script {
                    def dockerImage = docker.build("orchestrator:${env.BRANCH_NAME}", "-f docker/Dockerfile docker/")

                    sh("docker tag orchestrator:${env.BRANCH_NAME} ${HARBOR_IMAGE_NAME}:${env.BRANCH_NAME}")
                    sh("docker tag orchestrator:${env.BRANCH_NAME} ${DOCKER_HUB_IMAGE_NAME}:${env.BRANCH_NAME}")
                }
            }
        }
        stage('Push to Docker Hub and Harbor') {
            parallel {
                stage('Push to Docker Hub') {
                    steps {
                        script {
                            // Retrieve the Docker image object from the previous stage
                            def dockerhubImage = docker.image("${DOCKER_HUB_IMAGE_NAME}:${env.BRANCH_NAME}")

                            // Login to Docker Hub
                            docker.withRegistry('https://index.docker.io/v1/', DOCKER_HUB_CREDENTIALS) {
                                // Push the Docker image to Docker Hub
                                dockerhubImage.push()
                            }
                        }
                    }
                }

                stage('Push to Harbor') {
                    steps {
                        script {
                            // Retrieve the Docker image object from the previous stage
                            def harborImage = docker.image("${HARBOR_IMAGE_NAME}:${env.BRANCH_NAME}")

                            // Login to Harbor
                            docker.withRegistry('https://harbor.cloud.infn.it', HARBOR_CREDENTIALS) {
                                // Push the Docker image to Harbor
                                harborImage.push()
                            }
                        }
                    }
                }
            }
        }
    }
}
