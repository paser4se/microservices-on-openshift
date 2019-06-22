#!/usr/bin/env bash

. ../../env.sh

OCP=https://${IP}:8443
PROJECT=istio-operator
USER=justin

oc login ${OCP} -u $USER

oc delete project $PROJECT
oc adm new-project $PROJECT --node-selector='' 2> /dev/null
while [ $? \> 0 ]; do
    sleep 1
    printf "."
oc adm new-project $PROJECT --node-selector='' 2> /dev/null
done

PROJECT=istio-system

oc delete project $PROJECT
oc adm new-project $PROJECT --node-selector='' 2> /dev/null
while [ $? \> 0 ]; do
    sleep 1
    printf "."
oc adm new-project $PROJECT --node-selector='' 2> /dev/null
done

#oc apply -n istio-operator -f https://raw.githubusercontent.com/Maistra/istio-operator/maistra-0.10/deploy/servicemesh-operator.yaml
oc apply -n istio-operator -f https://raw.githubusercontent.com/Maistra/istio-operator/maistra-0.11/deploy/servicemesh-operator.yaml