# Connect to Cloud SQL for PostgreSQL from Cloud Run using Serverless VPC.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

Also uses Apache Camel ™ a versatile open-source integration framework based on known Enterprise Integration Patterns.

If you want to learn more about Apache Camel on Quarkus, please visit its website: https://camel.apache.org/camel-quarkus .

## Objectives
* Write, build and deploy a Camel Quarkus service to Cloud Run to consume using Private IP Cloud SQL PostgreSQL Instance
* Configure Serverless VPC to expose Cloud SQL privately on your project.

## Before you begin
### Setting up gcloud defaults
* Set your default project.
```shell script
gcloud config set project PROJECT_ID
```
Replace **PROJECT_ID** with the name of the project you created  for this example.

* Configure gcloud for your chosen region
```shell script
gcloud config set run/region REGION
```
Replace **REGION** with the region of your choice.

* Enable the Cloud API neccessary to run this example
```shell script
gcloud services enable compute.googleapis.com sqladmin.googleapis.com run.googleapis.com \
containerregistry.googleapis.com cloudbuild.googleapis.com servicenetworking.googleapis.com
```
This commando enables the following APIs:
* Compute Engine API
* Cloud SQL Admin API
* Cloud Run API
* Container Registry API
* Cloud Build API
* Service Networking API

## Set up Network
* Allocate an IP Addresses range and create a private connection to configure private services access for Cloud SQL

```shell script
 gcloud compute addresses create google-managed-services-default \
--global --purpose=VPC_PEERING --prefix-length=16 \
--description="peering range for Google" --network=default
```

* Create a private connection to the allocated IP Address range

```shell script
gcloud services vpc-peerings connect --service=servicenetworking.googleapis.com \
--ranges=google-managed-services-default --network=default \
--project=YOUR_PROJECT_ID
```
Replaces **YOUR_PROJECT_ID** with your project's project ID.

### Create an Cloud SQL Instance
## Create a Cloud SQL PostgreSQL instance with Private IP
* Run command to create an Cloud SQL instance

```shell script
gcloud sql instances create my-instance \
--database-version=POSTGRES_13 \
 --cpu=1 \
 --memory=4GB \
 --region=us-central \
 --root-password=DB_ROOT_PASSWORD \
 --no-assign-ip \
--network=default
```

Modify the value for the following parameters.
* --database_version: The database engine type and version. If left unspecified, the API default is used. See the gcloud [database versions documentation](https://cloud.google.com/sdk/gcloud/reference/sql/instances/create#--database-version) to see the current available versions.
* --cpu: The number of cores in the machine.
* --memory: A whole number value indicating how much memory to include in the machine. A size unit can be provided (for example, 3072MB or 9GB).
* --region: The regional location of the instance (for example asia-east1, us-east1). If left unspecified, the default us-central1 is used.
* DB_ROOT_PASSWORD for your custom password.
* --no-assign-ip Create an instance without Public IP.

## Create a database
* Run the following command to create a database
```shell script
gcloud sql databases create my_db --instance=my-instance
```

## Create a User
* Run the following command to create a user
```shell script
gcloud sql users create my-user \
--instance=my-instance \
--password=PASSWORD
```
Replace **PASSWORD** with a password for your database user.

## Deploy Sample App to Cloud Run
### Configure a Cloud Run service account
* Run the following commad to get list of your project's service accounts
```shell script
gcloud iam service-accounts list
```

* Copy the **EMAIL** of the **Compute Engine service account**

* Run the following command to add the **Cloud SQL Client** role to Compute Engince Service Account
```shell script
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:SERVICE_ACCOUNT_EMAIL" \
  --role="roles/cloudsql.client"
```
Replace **YOUR_PROJECT_ID** and **SERVICE_ACCOUNT_EMAIL** with your own values.

### Configure a Cloud SQL Sample App
For private IP paths, your application connects directly to your instance through Serverless VPC Access. This method uses a TCP socket to connect directly to the Cloud SQL instance without using the Cloud SQL Auth proxy.

* Create a Serverless VPC, running the following command
```shell script
gcloud compute networks vpc-access connectors create my-connector \
--region=us-central1 \
--network=default \
--range=10.8.0.0/28 \
--min-instances=2 \
--max-instances=10 \
--machine-type=e2-micro
```

* Run the following command
```shell script
gcloud sql instances list --filter my-instance
```
Copy output information from column PRIVATE_ADDRESS

## Packaging and running the application.
*  Download this repo code
```shell script
git clone https://github.com/mikeintoch/gcp-quarkus-samples.git
```
* Change directory
```shell script
cd gcp-quarkus-samples/cloudsql
```

* Modify **application.properties** file replacing PRIVATE_ADDRESS value with your IP Address from your instance
```shell script
quarkus.datasource.jdbc.url=jdbc:postgresql://<PRIVATE_ADDRESS>:5432/my_db
```
Replace YOUR_PASSWORD from your previously created user.

* Building Cloud Run service

* Authorize Docker to push to your Container Registry
```shell script
gcloud auth configure-docker
```
* Use the Jib Plugin for Quarkus to build and push the container to Container Registry

([Jib plugin](https://quarkus.io/guides/container-image#jib)) is integrated into a Quarkus dependency library and can be configured to build and push images without the need for a docker file. All dependencies are cached in a different layer than the application making rebuilds fast and small.

```shell script
mvn clean package \
 -Dquarkus.container-image.push=true \
 -Dquarkus.container-image.registry=gcr.io \
 -Dquarkus.container-image.group=PROJECT_ID \
 -Dquarkus.container-image.name=IMAGE_NAME \
 -Dquarkus.container-image.tag=TAG_ID \
 -Dquarkus.jib.base-jvm-image=gcr.io/distroless/java:latest
```
Replace **PROJECT_ID**, **TAG_ID** and **IMAGE_NAME** with your own values.

It produces an container image with the next format **gcr.io/PROJECT_ID/IMAGE_NAME:TAG_ID**

* Run the following command to deploy app on Cloud Run
```shell script
gcloud run deploy camel-sql-service --image gcr.io/PROJECT_ID/IMAGE_NAME:TAG_ID --no-allow-unauthenticated
```
Replace **PROJECT_ID**, **TAG_ID** and **IMAGE_NAME** with your own values.

The `--no-allow-unauthenticated` flag restricts unauthenticated access to the service.

## Trying out

* Send a Pub/Sub message to the Topic
```shell script
gcloud pubsub topics publish myTopic --message "Human"
```

### Navigate to the service logs:

* Navigate to the Google Cloud console
* Click the camel-pubsub-service service.
* Select the Logs tab.

Logs might take a few moments to appear. If you don't see them immediately, check again after a few moments.

* Look for the `"Hello Human!"` message.
