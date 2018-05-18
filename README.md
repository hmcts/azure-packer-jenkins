Jenkins Agent Image Builder for Tactical
========================================

This repository is the home of the *Build Jenkins Agent Image in Azure* Pipeline in Prod Jenkins. It is an extension of the work by Adam that is found in the
azure-packer-baseimage repository.

It is triggered every Thursday around 01:00 and should remove the old build prior to a run, meaning we only ever have one and we're not adding pointless cost.

Jenkinsfile
-----------

There are two Jenkinsfiles in this directory, the first, at the root, is what's triggered when a merge-request is raised, it performs some basic tests listed below:

**Checkout**

Used to checkout this repository onto the build node, also grabs the master branch of 'bootstrap-role' and 'ansible-management.' 

**Role Installation/Download**

This ensures that we have a local copy of the various repos 'bootstrap-role' requires.

**Ansible Requirements**

"pip install hvac" so you can access the vault secrets within ansible. Ansible
must be 2.4+

**Packer Version**

Simply a print of the current Packer version. Controlled by the Artifactory Role. 

**Initial Verify Syntax**

Makes sure we've not screwed up the packer syntax of `azure-jenkinsagent-gold.json` by supplying a few foo variables and running `validate`.

**Log in to Azure for next steps**

Using stored Credentials from Jenkins, we log in to Azure.

**Set appropriate Subscription ID**

Sets default Subscription ID.

**Show associated Subscription ID Name**

Prints the name of the ID we set.

**Arbitrary wait for Azure to complete provisioning.**

This is an example wait, the main run does one too, but it's longer. Azure is not always the quickest to provision.

Jenkinsfile-apply.groovy
------------------------

This is the second Jenkinsfile, the one triggered from Jenkins.

**Checkout**

Used to checkout this repository onto the build node, also grabs the master branch of 'bootstrap-role' and 'ansible-management.' and any further relevant jenkins agent roles (eg. jenkins-role).

**Role Installation/Download**

This ensures that we have a local copy of the various repos 'bootstrap-role' requires.

**Packer Version**

Simply a print of the current Packer version. Controlled by the Artifactory Role.

**Verify Syntax**

Makes sure we've not screwed up the packer syntax of `azure-jenkinsagent-gold.json` by supplying a few foo variables and running `validate`.
 
**Log in to Azure**

Using stored Credentials from Jenkins, we log in to Azure.

**Set appropriate Subcription Name**

Uses passed in variable (from run/ansible-management) to set the subscription we're using. 

**Ensure RG exists.**

Uses Azure CLI to ensure our Resource Group is in place.

**Ensure Storage Account exists.**

Uses Azure CLI to ensure our Storage Account exists. Note dodgy `tr` and `sed` usage due to name restrictions in Azure, which are bothersome to work around.

**Remove Previous images directory**

Get rid of any previous image directories before we rebuild, ensures we don't make many.

**Remove Previous system directory**

Like the above, only for the system directory.

**Arbitrary wait for Azure to complete provisioning.**

At the time of writing it's 2017, but that doesn't mean that arbitrary sleeps don't still have their place...

This is because resources get listed as 'deleted' by Azure, even when they still exist behind the scenes. 

**Packer Deploy**

Run Packer with all the variables and names we've specified, builds our template. 

What Happens?
-------------

During a build, either triggered manually through the Jenkins web interface, or run automatically every Thursday night, the following occurs:

* Job logs into Azure, prepares environment. 
* Job deletes previously used VHD from directory. 
* Job triggers Packer, which pulls in the OpenLogic image to start.
* A VM is spun up, in a private subnet shared with Jenkins.
* The VM has the previously pulled `bootstrap-role` applied. 
* Job has the jenkins agent roles applied (possibly same step as above).
* The VM has `yum update -y` run again it. 
* The VM is shut down, and converted into a VHD. 
* The VHD is placed in the specified Storage Account, ready to be used. 

Testing
-------

It is currently impossible to test the `azure-jenkinsagent-gold.json` changes using sandbox.

However, with some changes, such as commenting out the three 'virtual_network' entries within the JSON, you should be able to deploy to Sandbox with something like the following:

```
$ packer build -var "azure_resource_group_name=<some rg that exists>" -var "azure_storage_account_name=<some sa that exists>" -var "azure_subscription_id=<sandbox subscription id>" -var "azure_client_id=<your client id>" -var "azure_client_secret=<your client secret> -var "azure_tenant_id=<your tenant id>" azure-jenkinsagent-gold.json
```

Please note, that for this to work Sandbox would need the above mentioned RG and SA to be in place. 

