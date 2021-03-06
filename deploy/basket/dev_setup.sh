#!/usr/bin/env bash

APP=basket
S2I_IMAGE=java:8

. ../../env.sh

oc delete all -l app=${APP} --ignore-not-found=true -n ${DEV_PROJECT}
oc delete pvc -l app=${APP} --ignore-not-found=true -n ${DEV_PROJECT}
oc delete is,bc,dc,svc,route,sa ${APP} --ignore-not-found=true -n ${DEV_PROJECT}
oc delete configmap ${APP}-config --ignore-not-found=true -n ${DEV_PROJECT}

echo Setting up ${APP} for ${DEV_PROJECT}
oc new-app -f ../spring-boot-dev-nexus-template.yaml \
    -p PROJECT=${DEV_PROJECT} \
    -p APPLICATION_NAME=${APP} \
    -p BASE_IMAGE_NAMESPACE="openshift" \
    -p BASE_IMAGE=${S2I_IMAGE} \
    -n ${DEV_PROJECT}

oc secrets link ${APP}-sa nexus-dockercfg --for=pull -n ${DEV_PROJECT}