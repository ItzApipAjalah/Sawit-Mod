pipeline {
    agent any

    environment {
        // Set Java version (Always use Java 21 for Gradle 8.14+ since some Architectury plugins require it)
        JAVA_HOME = '/usr/lib/jvm/java-21-openjdk-amd64'
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
                sh 'echo org.gradle.java.installations.auto-detect=false >> gradle.properties'
                sh 'echo org.gradle.java.installations.paths=/usr/lib/jvm/java-21-openjdk-amd64 >> gradle.properties'
                sh 'echo org.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64 >> gradle.properties'
                sh "echo 'allprojects { tasks.withType(JavaCompile).configureEach { javaCompiler = null } }' >> build.gradle"
                sh './gradlew build'
            }
        }
    }

    post {
                always {
            sh '''
                mkdir -p Output/Fabric Output/Forge Output/Quilt Output/NeoForge
                find fabric/build/libs -maxdepth 1 -name "*.jar" ! -name "*-dev-shadow.jar" ! -name "*-sources.jar" -exec cp {} Output/Fabric/ \; 2>/dev/null || true
                find forge/build/libs -maxdepth 1 -name "*.jar" ! -name "*-dev-shadow.jar" ! -name "*-sources.jar" -exec cp {} Output/Forge/ \; 2>/dev/null || true
                find quilt/build/libs -maxdepth 1 -name "*.jar" ! -name "*-dev-shadow.jar" ! -name "*-sources.jar" -exec cp {} Output/Quilt/ \; 2>/dev/null || true
                find neoforge/build/libs -maxdepth 1 -name "*.jar" ! -name "*-dev-shadow.jar" ! -name "*-sources.jar" -exec cp {} Output/NeoForge/ \; 2>/dev/null || true
                rmdir Output/* 2>/dev/null || true
            '''
            archiveArtifacts artifacts: 'Output/**/*.jar', allowEmptyArchive: true
        }
        success {
            echo "Build finished successfully!"
        }
        failure {
            echo "Build failed! Please check the logs."
        }
    }
}







