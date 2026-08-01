pipeline {
    agent any

    environment {
        // Set Java version (Always use Java 21 for Gradle 8.14+ since some Architectury plugins require it)
        JAVA_HOME = '/usr/lib/jvm/java-1.21.0-openjdk-amd64'
        PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
    }

    stages {
        stage('Print Info') {
            steps {
                echo "Building branch: ${env.BRANCH_NAME}"
                echo "Using JAVA_HOME: ${env.JAVA_HOME}"
                sh 'java -version'
            }
        }
        stage('Build Mod') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew build'
            }
        }
    }

    post {
        always {
            // Archive output jars from all possible loader directories
            // allowEmptyArchive handles cases where a loader (like neoforge) might not exist on a specific branch
            archiveArtifacts artifacts: 'fabric/build/libs/*.jar, forge/build/libs/*.jar, quilt/build/libs/*.jar, neoforge/build/libs/*.jar, fabric-like/build/libs/*.jar', allowEmptyArchive: true
        }
        success {
            echo "Build finished successfully!"
        }
        failure {
            echo "Build failed! Please check the logs."
        }
    }
}

