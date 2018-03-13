## Purpose 

Showcase Istio's Distributed Tracing capabilities with a set of properly instrumented Spring Boot applications

## Prerequisites

- Minishift running an Openshift 1.7 cluster
- Istio installed on the aforementioned cluster.
To install Istio (version `0.4.0` has been tested and works, version `0.6.0` is known to NOT work) simply follow one of the following docs:
    * https://istio.io/docs/setup/kubernetes/quick-start.html
    * https://istio.io/docs/setup/kubernetes/ansible-install.html
- Login to the cluster with the admin user
- Jaeger installed in the istio-system namespace

## Deploy project onto Minishift

```bash
    $ oc new-project demo-istio
    $ oc adm policy add-scc-to-user privileged -z default -n demo-istio
    $ mvn clean package fabric8:deploy -Pistio-openshift
    $ oc expose svc istio-ingress -n istio-system
    $ oc create -f rules/route-rule-redir.yml    
    $ open $(minishift openshift service istio-ingress -n istio-system --url)/suggest/
```

The traces from the invocation of the two endpoints should look like the following:

* Serial
![](images/serial.jpg)

* Parallel
![](images/parallel.jpg)