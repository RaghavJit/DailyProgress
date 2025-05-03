# Scilab on Cloud – Containerized on Minikube

## Repo

Forked from: [prashantsinalkar/scilab-on-cloud](https://github.com/prashantsinalkar/scilab-on-cloud)  
My changes: [Compare here](https://github.com/prashantsinalkar/scilab-on-cloud/compare/maxprocess...RaghavJit:scilab-on-cloud:maxprocess)

## Prerequisites

- Python 3.9.21 (used in Dockerfile)
- Podman (or Docker)
- Minikube
- kubectl
- config file with name `config.py` in the same directory
- clone of above mentioned fork in the same direcotry

---

## Build and Save Docker Image

[Dockerfile](./Dockerfile)

```bash
podman build -t scilab-app .
podman save -o scilab-app.tar scilab-app
```

---

## Setup Minikube (Rootless with Podman)
Podman is currently in experimental version I recommend using rootless docker instead or VM [Refer this](https://minikube.sigs.k8s.io/docs/drivers/podman/).

```bash
minikube config set rootless true
minikube start --driver=podman --container-runtime=containerd
```

---

## Load Image into Minikube

There might be better alternatives to creating a tar archive and then importing to minikube.

```bash
minikube image load scilab-app.tar
```

---

## Deploy App

[scilab-deploy.yaml](./scilab-deploy.yaml)

```bash
kubectl apply -f scilab-deploy.yaml
```

---

## Access the App

Launch dashboard:

```bash
minikube dashboard
```

Or get the app URL:

```bash
minikube service scilab-app --url
```

---

## .dockerignore

Ignore build dirs and tar:

```
scilab-on-cloud/
scilab-app.tar
```