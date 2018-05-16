#!groovy

def channel = '#devops-builds'

properties(
  [[$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/hmcts/azure-packer-jenkins/'],
   pipelineTriggers([[$class: 'GitHubPushTrigger']]),
   [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '7', numToKeepStr: '10']]
],
)

@Library('Reform') _

node() {

//          checkout([$class: 'GitSCM', branches: scm.branches, doGenerateSubmoduleConfigurations: true, extensions: scm.extensions + [[$class: 'SubmoduleOption', parentCredentials: true]], userRemoteConfigs: scm.userRemoteConfigs])


  ws('azure-packer-jenkins') {
    try {
      wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
        environment {
          PATH = "/usr/local/sbin:$PATH"
        }

        stage('Checkout') {
          checkout scm
          dir('bootstrap-role') {
            git url: "https://github.com/hmcts/bootstrap-role", branch: "master"
          }
          dir('jenkins-common-role') {
            git url: "https://github.com/hmcts/jenkins-common-role", branch: "master"
          }

          dir('ansible-management') {
            git url: "https://github.com/hmcts/ansible-management", branch: "master"
          }
        }

        stage('Bootstrap Role Installation/Download') {
          sh '''
            ansible-galaxy install -r bootstrap-role/requirements.yml --force --roles-path=bootstrap-role/roles/ 
          '''
        }

        stage('Jenkins Common/Slave Roles Installation/Download') {
          sh '''
            ansible-galaxy install -r jenkins-common-role/requirements.yml --force --roles-path=bootstrap-role/roles/ 
          '''
        }

        stage('Packer Version') {

          sh '''
            packer version
          '''
        }

        stage('Initial Verify Syntax') {
          sh '''
            packer validate -var "azure_client_id=ug" -var "azure_client_secret=ogg" -var "azure_subscription_id=laurel" -var "azure_resource_group_name=adam" -var "azure_storage_account_name=eve" -var "jenkins_user_password=security" azure-jenkinsagent-gold.json
          '''
        } 

        withCredentials([
            [$class: 'StringBinding', credentialsId: 'IDAM_ARM_CLIENT_ID', variable: 'AZURE_CLIENT_ID'],
            [$class: 'StringBinding', credentialsId: 'IDAM_ARM_TENANT_ID', variable: 'AZURE_TENANT_ID'],
            [$class: 'StringBinding', credentialsId: 'IDAM_ARM_SUBSCRIPTION_ID', variable: 'AZURE_SUBSCRIPTION_ID']
        ]) {
          
          stage('Log in to Azure for next steps') {
            sh '''
              az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET --tenant $AZURE_TENANT_ID
            '''
          }

          stage('Set appropriate Subcription ID') {
            sh '''
              az account set --subscription $AZURE_SUBSCRIPTION_ID
            '''
          }

          stage('Show associated Subscription ID Name') {
            sh '''
              az account show | jq -r .name
            '''
          }

          stage('Arbitrary wait for Azure to complete provisioning.') {
            sh '''
              sleep 30
            '''
          }

					}

        }

    } catch (err) {
      notifyBuildFailure channel: "${channel}"
      throw err
    } finally {
      stage('Cleanup') {
          sh '''
            echo "Nothing to do for Cleanup."
            '''
        }
      }
  }
}
