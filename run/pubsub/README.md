# Using Pub/Sub with Cloud Run and process information with Quarkus + Apache Camel

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

Also uses Apache Camel ™ a versatile open-source integration framework based on known Enterprise Integration Patterns.

If you want to learn more about Apache Camel on Quarkus, please visit its website: https://camel.apache.org/camel-quarkus .

## Objectives
* Write, build and deploy a Camel Quarkus service to Cloud Run
* Call the service by publishing a message to a Pub/Sub topic.

## Before you begin
### Setting up gcloud defaults
* Set your default project.
```shell script
gcloud config set project PROJECT_ID
```
Replace **PROJECT_ID** with the name of the project you created  for this example.

* Configure gcloud for your chosen region
```shell script
gcloud confi set run/region REGION
```
Replace **REGION** with the region of your choice.

* Enable the Cloud API neccessary to run this example
```shell script
gcloud services enable run.googleapis.com containerregistry.googleapis.com \
cloudbuild.googleapis.com 
```
This commando enables the following APIs:
* Cloud Run API
* Container Registry API
* Cloud Build API

## Creating a Pub/Sub Topic
* Create a new Pub/Sub topic
```shell script
gcloud pubsub topics create myTopic
```

## Packaging and running the application.

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
gcloud run deploy my-camel-service --image gcr.io/PROJECT_ID/IMAGE_NAME:TAG_ID --no-allow-unauthenticated
```
Replace **PROJECT_ID**, **TAG_ID** and **IMAGE_NAME** with your own values.

The `--no-allow-unauthenticated` flag restricts unauthenticated access to the service.

## Integrating with Pub/Sub

To integrate the service with Pub/Sub
* Create a service account to represent the Pub/Sub subscription identity
```shell script
gcloud iam service-accounts create cloud-run-pubsub-invoker \
    --display-name "Cloud Run Pub/Sub Invoker"
```
### Create a Pub/Sub subscription with the service account
* Give the invoker service account permission to invoke your camel service:
```shell script
gcloud run services add-iam-policy-binding my-camel-service \
   --member=serviceAccount:cloud-run-pubsub-invoker@PROJECT_ID.iam.gserviceaccount.com \
   --role=roles/run.invoker
```
* Allow Pub/Sub to create authentication tokens in your project
```shell script
gcloud projects add-iam-policy-binding PROJECT_ID \
     --member=serviceAccount:service-PROJECT_NUMBER@gcp-sa-pubsub.iam.gserviceaccount.com \
     --role=roles/iam.serviceAccountTokenCreator
```
Replace
* **PROJECT_ID** with your Google Cloud project ID.
* **PROJECT_NUMBER** with your Google Cloud project number.
Project ID and project number are listed in the Project info panel in the console for your project.

* Create a Pub/Sub subscription with the service account:
```shell script
gcloud pubsub subscriptions create myTopicSubscription --topic myTopic \
   --ack-deadline=600 \
   --push-endpoint=SERVICE-URL/ \
   --push-auth-service-account=cloud-run-pubsub-invoker@PROJECT_ID.iam.gserviceaccount.com
```
Replace
* **SERVICE-URL** with the HTTPS URL provided on deploying the service. This URL works even if you have also added a domain mapping.
* **PROJECT_ID** with your Cloud project ID.

Your service is now fully integrates with Pub/Sub

## Trying out

* Send a Pub/Sub message to the Topic
```shell script
gcloud pubsub topics publis myTopic --message "Mundo"
```

### Navigate to the service logs:

* Navigate to the Google Cloud console
* Click the pubsub-tutorial service.
* Select the Logs tab.

Logs might take a few moments to appear. If you don't see them immediately, check again after a few moments.

* Look for the `"Hello Mundo!"` message.

