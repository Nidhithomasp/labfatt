pipeline {
    agent any
    environment {
        DOCKER_IMAGE = 'YOUR_DOCKERHUB_USERNAME/password-reset'
        DOCKER_CRED_ID = 'docker-hub-creds'
        KUBE_CRED_ID = 'k8s-config'
    }
    stages {
        stage('Checkout') {
            steps { checkout scm }
        }
        stage('Maven Build') {
            steps { sh 'mvn clean package -DskipTests' }
        }
        stage('JUnit Tests') {
            steps { sh 'mvn test' }
            post { always { junit 'target/surefire-reports/*.xml' } }
        }
        stage('Docker Build & Push') {
            steps {
                sh "docker build -t $DOCKER_IMAGE:$BUILD_NUMBER ."
                withCredentials([usernamePassword(credentialsId: DOCKER_CRED_ID, usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                    sh "echo $PASS | docker login -u $USER --password-stdin"
                    sh "docker push $DOCKER_IMAGE:$BUILD_NUMBER"
                    sh "docker tag $DOCKER_IMAGE:$BUILD_NUMBER $DOCKER_IMAGE:latest"
                    sh "docker push $DOCKER_IMAGE:latest"
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([file(credentialsId: KUBE_CRED_ID, variable: 'KUBECONFIG')]) {
                    sh "sed -i 's|YOUR_DOCKERHUB_USERNAME/password-reset:latest|$DOCKER_IMAGE:$BUILD_NUMBER|g' k8s/deployment.yaml"
                    sh "kubectl apply -f k8s/deployment.yaml"
                    sh "kubectl apply -f k8s/service.yaml"
                    sh "kubectl rollout status deployment/password-reset-app"
                }
            }
        }
    }
    post {
        success { echo '✅ Pipeline succeeded. App deployed to K8s.' }
        failure { echo '❌ Pipeline failed. Check console logs.' }
    }
}