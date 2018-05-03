pipeline {
	agent any
	tools {
		maven 'Maven 3'
	}
	options {
		timeout(time: 1, unit: 'HOURS')
		skipStagesAfterUnstable()
		buildDiscarder(logRotator(numToKeepStr : "10"))
	}
	environment {
		VERSION = readMavenPom().getVersion()
		ARTIFACT_ID = readMavenPom().getArtifactId()
	}
	stages {
		stage('Initialisation'){
			steps {
				echo "Starting Job ${env.BUILD_NUMBER} : \n" + 
					"Artifact ID : ${ARTIFACT_ID} \n" + 
					"Version ${VERSION} \n" +
					"Branch ${GIT_BRANCH}"
			}
		}
		stage('Build') {
			steps {
				sh 'mvn clean install'
			} 
			post {
				always {
					junit 'target/surefire-reports/**/*.xml'
				}
			}
		}
		stage('SonarQube Analysis'){
			when {
				equals expected: 'origin/master', actual: GIT_BRANCH
			} 
			steps {
				withSonarQubeEnv('my-sonarqube') {
					sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.4.0.905:sonar '
				}
			}
		}
		stage('SonarQube Quality Gate'){
			when {
				equals expected: 'origin/master', actual: GIT_BRANCH
			}
			steps {
				script {
					def qualitygate = waitForQualityGate()
					if (qualitygate.status != "OK") {
						error "Pipeline failed due to quality gate : ${qualitygate.status}"
					} 
				}
			}
		}
		stage('Source publish'){
			when {
				equals expected: 'origin/master', actual: GIT_BRANCH
			}
			steps {
				script {
					def artifactoryServer = Artifactory.server('my-artifactory')
					def mavenBuild = Artifactory.newMavenBuild()
					mavenBuild.resolver server: artifactoryServer, releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot'
					mavenBuild.deployer server: artifactoryServer, releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local'
					mavenBuild.tool = 'Maven 3'
					def buildInfo = mavenBuild.run pom: 'pom.xml', goals: 'clean install'
					artifactoryServer.publishBuildInfo buildInfo
				}
			}
		}
		stage('Deploy'){
			when {
				equals expected: 'origin/master', actual: GIT_BRANCH
			}
			options {
                timeout(time: 15, unit: 'MINUTES') 
            }
			steps {
				script {
					sshagent(credentials : ['tomcat-cred']) {
    					echo 'deploy'
					}
				}
			}
		}
	}
	post {
		success {
			script {
				if(GIT_BRANCH == 'origin/master'){
					slackSend color: 'good', message: "Build successfull ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
				}
			}
		}
		failure {
			script {
				targetEmail = 'Last commiter'
				if(GIT_BRANCH == 'origin/master'){
					targetEmail = 'Team'
					slackSend color: 'danger', message: "Build failure ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
				}
				echo "Sending email to ${targetEmail}"
			}
		}
		fixed {
			script {
				targetEmail = 'Last commiter'
				if(GIT_BRANCH == 'origin/master'){
					targetEmail = 'Team'
					slackSend color: 'good', message: "Build back to normal ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
				}
				echo "Sending email to ${targetEmail}"
			}
		}
	}
}
