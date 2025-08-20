pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_CREDENTIALS = credentials('docker-hub-credentials')
        SONAR_TOKEN = credentials('sonar-token')
        KUBECONFIG = credentials('kubeconfig')
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build & Test') {
            parallel {
                stage('Contract Service') {
                    steps {
                        dir('microservices/contract-service') {
                            sh 'mvn clean test'
                            sh 'mvn package -DskipTests'
                        }
                    }
                }
                stage('API Gateway') {
                    steps {
                        dir('microservices/api-gateway') {
                            sh 'mvn clean test'
                            sh 'mvn package -DskipTests'
                        }
                    }
                }
                stage('LLM Service') {
                    steps {
                        dir('microservices/llm-service') {
                            sh 'mvn clean test'
                            sh 'mvn package -DskipTests'
                        }
                    }
                }
            }
        }
        
        stage('Code Quality') {
            steps {
                script {
                    sh """
                        mvn sonar:sonar \
                          -Dsonar.projectKey=legal-ai \
                          -Dsonar.host.url=http://sonarqube:9000 \
                          -Dsonar.login=${SONAR_TOKEN}
                    """
                }
            }
        }
        
        stage('Security Scan') {
            steps {
                sh 'mvn dependency-check:check'
                publishHTML(target: [
                    reportDir: 'target',
                    reportFiles: 'dependency-check-report.html',
                    reportName: 'OWASP Dependency Check Report'
                ])
            }
        }
        
        stage('Build Docker Images') {
            when {
                branch 'main'
            }
            steps {
                script {
                    docker.withRegistry("https://${DOCKER_REGISTRY}", 'docker-hub-credentials') {
                        def services = ['contract-service', 'api-gateway', 'llm-service', 'discovery-service']
                        services.each { service ->
                            def image = docker.build("legalai/${service}:${env.BUILD_NUMBER}", "./microservices/${service}")
                            image.push()
                            image.push('latest')
                        }
                    }
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            when {
                branch 'main'
            }
            steps {
                script {
                    sh """
                        export KUBECONFIG=${KUBECONFIG}
                        kubectl apply -f k8s/base/
                        kubectl set image deployment/contract-service contract-service=${DOCKER_REGISTRY}/legalai/contract-service:${env.BUILD_NUMBER} -n legal-ai
                        kubectl set image deployment/api-gateway api-gateway=${DOCKER_REGISTRY}/legalai/api-gateway:${env.BUILD_NUMBER} -n legal-ai
                        kubectl rollout status deployment/contract-service -n legal-ai
                        kubectl rollout status deployment/api-gateway -n legal-ai
                    """
                }
            }
        }
        
        stage('Performance Tests') {
            when {
                branch 'main'
            }
            steps {
                sh 'mvn gatling:test'
                gatlingArchive()
            }
        }
    }
    
    post {
        always {
            junit '**/target/surefire-reports/*.xml'
            archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
            cleanWs()
        }
        success {
            slackSend(
                color: 'good',
                message: "Build Successful: ${env.JOB_NAME} - ${env.BUILD_NUMBER}"
            )
        }
        failure {
            slackSend(
                color: 'danger',
                message: "Build Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}"
            )
        }
    }
}