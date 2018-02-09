## Purpose 

Showcase Istio's Distributed Tracing capabilities with a set of properly instrumented Spring Boot applications

## Deploy on Minishift

```bash
    $ oc new-project demo-istio
    $ oc adm policy add-scc-to-user privileged -z default -n demo-istio
    $ mvn clean package fabric8:deploy -Pistio-openshift
    $ oc create -f rules/route-rule-redir.yml    
    $ http $(minishift openshift service istio-ingress -n istio-system --url)/suggest/serial
    $ http $(minishift openshift service istio-ingress -n istio-system --url)/suggest/parallel
```