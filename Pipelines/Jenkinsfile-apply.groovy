#!groovy

def channel = '#devops-builds'

properties(
  [[$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/hmcts/azure-packer-jenkins/'],
   pipelineTriggers([[$class: 'GitHubPushTrigger']])]
)

@Library('Reform') _

node {
    def secrets = [
        [$class: 'VaultSecret', path: 'secret/devops/test/jenkins/azure_vm_slaves_admin_password', secretValues: [
            [$class: 'VaultSecretValue', envVar: 'JENKINS_USER_PASSWORD', vaultKey: 'value']
           ]
        ]
    ]

  ws('azure-packer-jenkins') { // This must be the name of the role otherwise ansible won't find the role

    try {
      wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {

        stage('Checkout') {
          checkout scm
          dir('roles/bootstrap-role') {
            git url: "https://github.com/hmcts/bootstrap-role", branch: "master"
          }
          dir('roles/jenkins-common-role') {
            git url: "https://github.com/hmcts/jenkins-common-role", branch: "master"
          }
          dir('ansible-management') {
            git url: "https://github.com/hmcts/ansible-management", branch: "master", credentialsId: "jenkins-public-github-api-token"
          }

          dir('roles/cis-role') {
            git url: "https://github.com/hmcts/cis-role.git", branch: "master", credentialsId: "jenkins-public-github-api-token"
          }
        }

	stage('Create symlinks to work around relative-directory-playbook-path bug') {
		sh '''
		ln -s `pwd`/roles `pwd`/roles/bootstrap-role/roles
		ln -s `pwd`/roles `pwd`/roles/jenkins-common-role/roles
		ln -s `pwd`/roles `pwd`/roles/cis-role/roles
		ln -s `pwd`/roles/cis-role `pwd`/roles/cis
		'''
	}

        stage('Bootstrap Role Installation/Download') {
          sh '''
            ansible-galaxy install -r roles/bootstrap-role/requirements.yml --force --roles-path=roles/
          '''
        }

        stage('Jenkins Common/Slave Roles Installation/Download') {
          sh '''
            ansible-galaxy install -r roles/jenkins-common-role/requirements.yml --force --roles-path=roles/
            cp ansible-management/files/*.rpm roles/devops.common/files/
          '''
        }

        stage('Packer Version') {
          sh '''
            packer version
          '''
        }

        stage('Verify Syntax') {
          sh '''
            packer validate -var "azure_client_id=ug" -var "azure_client_secret=ogg" -var "azure_subscription_id=laurel" -var "azure_resource_group_name=adam" -var "azure_storage_account_name=eve" -var "jenkins_user_password=secret" azure-jenkinsagent-gold.json
          '''
        }

        withCredentials([
            [$class: 'StringBinding', credentialsId: 'IDAM_ARM_CLIENT_SECRET', variable: 'AZURE_CLIENT_SECRET'],
            [$class: 'StringBinding', credentialsId: 'IDAM_ARM_CLIENT_ID', variable: 'AZURE_CLIENT_ID'],
            [$class: 'StringBinding', credentialsId: 'IDAM_ARM_TENANT_ID', variable: 'AZURE_TENANT_ID'],
            [$class: 'StringBinding', credentialsId: 'IDAM_ARM_SUBSCRIPTION_ID', variable: 'AZURE_SUBSCRIPTION_ID']
        ]) {

          stage('Log in to Azure') {
            sh '''
              az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET --tenant $AZURE_TENANT_ID
            '''
          }

          stage('Set appropriate Subcription Name') {
            sh '''
	      az account set --subscription $Subscription
            '''
          }

          stage('Ensure RG exists.') {
            sh '''
              az group create -n jabrg$Subscription -l uksouth
            '''
          }

          stage('Ensure Storage Account exists.') {
            sh '''
		az storage account create -n jabsa$(az account show | jq -r .name | tr [:upper:] [:lower:] | sed s#-##g) --resource-group jabrg$Subscription -l uksouth --sku Standard_LRS
            '''
          }

          stage('Remove Previous images directory') {
            sh '''
              az storage container delete --account-name jabsa$(az account show | jq -r .name | tr [:upper:] [:lower:] | sed s#-##g) -n images
            '''
          }

          stage('Remove Previous system directory') {
            sh '''
              az storage container delete --account-name jabsa$(az account show | jq -r .name | tr [:upper:] [:lower:] | sed s#-##g) -n system
            '''
          }

          stage('Arbitrary wait for Azure to complete provisioning.') {
            sh '''
              sleep 60
            '''
          }

          stage('Packer Deploy') {
	    timestamps {
              wrap([$class: 'VaultBuildWrapper', vaultSecrets: secrets]) {

                sh '''
                  pwd
                  find . -type f
                  packer build -var-file=ansible-management/packer_vars/azure-packer-jenkins.json -var "azure_subscription_id=$(az account show | jq -r .id)" -var "azure_resource_group_name=jabrg$Subscription" -var "azure_storage_account_name=jabsa$(az account show | jq -r .name | tr [:upper:] [:lower:] | sed s#-##g)" -var "jenkins_user_password=$JENKINS_USER_PASSWORD" azure-jenkinsagent-gold.json
                '''
              }
            }
          }
        }
      }

    } catch (err) {
      notifyBuildFailure channel: "${channel}"
      throw err
    } finally {
      stage('Cleanup') {
        '''
        echo Clearing work dir to avoid disclosure of secrets.
        '''
	deleteDir()
      }
    }
  }
}
