pipeline {
    agent none

    environment {
        MVN_SETTINGS = '/etc/m2/settings.xml' //This should be changed in Jenkins config for the DS agent
        PROJECT = 'ds-datahandler'
        BUILD_TO_TRIGGER = 'ds-discover'
    }

    triggers {
        // This triggers the pipeline when a PR is opened or updated or so I hope
        githubPush()
    }

    parameters {
        string(name: 'ORIGINAL_BRANCH', defaultValue: "${env.BRANCH_NAME}", description: 'Branch of first job to run, will also be PI_ID for a PR')
        string(name: 'ORIGINAL_JOB', defaultValue: "${env.PROJECT}", description: 'What job was the first to build?')
        string(name: 'TARGET_BRANCH', defaultValue: "${env.CHANGE_TARGET}", description: 'Target branch if PR')
    }

    stages {
        stage('Echo Environment Variables') {
            agent {
                label 'DS agent'
            }

            steps {
                echo "ORIGINAL_BRANCH: ${env.ORIGINAL_BRANCH}"
                echo "PROJECT: ${env.PROJECT}"
                echo "ORIGINAL_JOB: ${env.ORIGINAL_JOB}"
                echo "BUILD_TO_TRIGGER: ${env.BUILD_TO_TRIGGER}"
                echo "TARGET_BRANCH: ${env.TARGET_BRANCH}"
            }
        }

        stage('Checkout aegis and copy files') {
            agent {
                label 'DS agent'
            }

            steps {
                dir('aegis') {
                    checkout scmGit(
                        branches: [[name: 'refs/heads/master']],
                        userRemoteConfigs: [[
                            credentialsId: 'kb-dk-jenkins-github-app',
                            url: 'https://github.com/kb-dk/aegis.git'
                        ]]
                    )
                }

                sh 'cp --recursive aegis/ds-datahandler/local/src/test/resources/. ./src/test/resources/'
                sh 'rm --recursive aegis'
            }
        }

        stage('Change version if part of PR') {
            agent {
                label 'DS agent'
            }

            when {
                expression {
                    env.ORIGINAL_BRANCH ==~ "PR-[0-9]+"
                }
            }
            steps {
                script {
                    sh "mvn -s ${env.MVN_SETTINGS} versions:set -DnewVersion=${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-${env.PROJECT}-SNAPSHOT"
                    echo "Changing MVN version to: ${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-${env.PROJECT}-SNAPSHOT"
                }
            }
        }

        stage('Change dependencies') {
            agent {
                label 'DS agent'
            }

            when {
                expression {
                    env.ORIGINAL_BRANCH ==~ "PR-[0-9]+"
                }
            }
            steps {
                script {
                    if ( env.ORIGINAL_JOB == 'ds-storage' ){
                        sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.storage:* -DdepVersion=${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-${env.ORIGINAL_JOB}-SNAPSHOT -DforceVersion=true"
                        sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.present:* -DdepVersion=${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-ds-present-SNAPSHOT -DforceVersion=true"
                        sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.kaltura:* -DdepVersion=${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-ds-kaltura-SNAPSHOT -DforceVersion=true"
                        echo "Changing MVN dependency storage to: ${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-${env.ORIGINAL_JOB}-SNAPSHOT"
                        echo "Changing MVN dependency present to: ${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-ds-present-SNAPSHOT"
                        echo "Changing MVN dependency kaltura to: ${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-ds-kaltura-SNAPSHOT"
                    }
                    if ( env.ORIGINAL_JOB == 'ds-license' || env.ORIGINAL_JOB == 'ds-present'){
                        sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.present:* -DdepVersion=${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-ds-present-SNAPSHOT -DforceVersion=true"
                        sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.kaltura:* -DdepVersion=${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-ds-kaltura-SNAPSHOT -DforceVersion=true"
                        echo "Changing MVN dependency present to: ${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-ds-present-SNAPSHOT"
                        echo "Changing MVN dependency kaltura to: ${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-ds-kaltura-SNAPSHOT"
                    }
                    if ( env.ORIGINAL_JOB == 'ds-kaltura' ){
                        sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.kaltura:* -DdepVersion=${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-${env.ORIGINAL_JOB}-SNAPSHOT -DforceVersion=true"
                        echo "Changing MVN dependency kaltura to: ${env.ORIGINAL_BRANCH}-${env.ORIGINAL_JOB}-${env.ORIGINAL_JOB}-SNAPSHOT"
                    }
                }
            }
        }

        stage('Build') {
            agent {
                label 'DS agent'
            }

            steps {
                script {
                    // Execute Maven build
                    sh "mvn -s ${env.MVN_SETTINGS} clean package"
                }
            }
        }

        stage('Push to Nexus') {
            agent {
                label 'DS agent'
            }

            when {
                // Check if Build was successful
                expression {
                    currentBuild.currentResult == "SUCCESS" && env.ORIGINAL_BRANCH ==~ "master|release_v[0-9]+|PR-[0-9]+"
                }
            }
            steps {
                sh "mvn -s ${env.MVN_SETTINGS} clean deploy -DskipTests=true"
            }
        }

        stage('Trigger Discover Build') {
            when {
                expression {
                    currentBuild.currentResult == "SUCCESS" && env.ORIGINAL_BRANCH ==~ "master|release_v[0-9]+|PR-[0-9]+"
                }
            }
            steps {
                script {
                    if ( env.ORIGINAL_BRANCH ==~ "PR-[0-9]+" ) {
                        echo "Triggering: DS-GitHub/${env.BUILD_TO_TRIGGER}/${env.TARGET_BRANCH}"

                        def result = build job: "DS-GitHub/${env.BUILD_TO_TRIGGER}/${env.TARGET_BRANCH}",
                        parameters: [
                            string(name: 'ORIGINAL_BRANCH', value: env.ORIGINAL_BRANCH),
                            string(name: 'ORIGINAL_JOB', value: env.ORIGINAL_JOB),
                            string(name: 'TARGET_BRANCH', value: env.TARGET_BRANCH)
                        ]
                        wait: true // Wait for the pipeline to finish
                    }

                    else if ( env.ORIGINAL_BRANCH ==~ "master|release_v[0-9]+" ){
                        echo "Triggering: DS-GitHub/${env.BUILD_TO_TRIGGER}/${env.ORIGINAL_BRANCH}"

                        def result = build job: "DS-GitHub/${env.BUILD_TO_TRIGGER}/${env.ORIGINAL_BRANCH}"
                        wait: true // Wait for the pipeline to finish
                    }
                    echo "Child Pipeline Result: ${result}"
                }
            }
        }
    }
}