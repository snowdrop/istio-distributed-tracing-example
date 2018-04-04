## Purpose 

Showcase Istio's Distributed Tracing capabilities with a set of properly instrumented Spring Boot applications

## Prerequisites

- Minishift running an Openshift 3.7 cluster
- Istio 0.6.0 (without auth enabled )installed on the aforementioned cluster.
To install Istio simply follow one of the following docs:
    * https://istio.io/docs/setup/kubernetes/quick-start.html
    * https://istio.io/docs/setup/kubernetes/ansible-install.html
- Login to the cluster with the admin user
- Jaeger installed in the istio-system namespace

## Deploy project onto Minishift

```bash
    oc new-project demo-istio
    oc adm policy add-scc-to-user privileged -z default -n demo-istio
    mvn clean package fabric8:deploy -Popenshift
    oc expose svc istio-ingress -n istio-system
    oc create -f rules/route-rule-redir.yml    
    open $(minishift openshift service istio-ingress -n istio-system --url)/suggest/
```

The traces from the invocation of the two endpoints should look like the following:

* Serial
![](images/serial.jpg)

* Parallel
![](images/parallel.jpg)


## Formal description



### Serial invocation of dependent micro-services

*Actors*:
- User
- Suggestion Service
- Album Service
- Album Details Service
- Store Service

*Triggers*:
External system invokes the /serial endpoint of the Suggestion Service

*Preconditions*:
- All four services have been deployed to the Service Mesh
- All four services correctly propagate tracing HTTP headers
- The /serial endpoint of Suggestion Service is visible outside the cluster via an Istio RouteRule
- The implementation of the /serial endpoint calls the Album Service and the Store Service in a serial fashion

*Post-conditions*:
- Once the call to Suggestion Service has completed, then given that the users waits a few seconds, a Trace should show up in the Jaeger UI that will contain Spans from all four services (as well as the Istio proxies)
- The trace gathered by Istio clearly indicate the Album Service and Store Service were called in serial fashion from the Suggestion Service, thus giving valuable insights into the (non-optimal) implementation of the whole system, as well as providing accurate timing information

*Normal flow*:

- The user opens the Istio Distributed Web page
- The user clicks a button that results in an HTTP call to the /serial endpoint 
- The HTTP call is ultimately handled by the /serial endpoint of the Suggestion Service
- The implementation of the /serial endpoint calls the Album Service
- The Album Service calls the Album Details Service
- The Album Details service responds with some album details
- The Album Service uses its own data as well the data collected from the Album Details Service to respond to the Suggestion Service
- After collecting the response from the Album Service, the implementation of the /serial endpoint calls the Store Service
- The Store Service responds with the store details
- After collecting the response from the Store Service, the implementation of the /serial endpoint uses both the store and the album data to reply to the issuer of the call
- Once the call is done, the user navigates to the Jaeger Query UI and views the trace.

### Parallel invocation of dependent micro-services

*Actors*:
- User
- Suggestion Service
- Album Service
- Album Details Service
- Store Service

Triggers:
External system invokes the /parallel endpoint of the Suggestion Service

Preconditions:
- All four services have been deployed to the Service Mesh
- All four services correctly propagare tracing HTTP headers
- The /parallel endpoint of Suggestion Service is visible outside the cluster via an Istio RouteRule
- The implementation of the /parallel endpoint calls the Album Service and the Store Service in a parallel fashion

Post-conditions:
- Once the call to Suggestion Service has completed, then given that the users waits a few seconds, a Trace should show up in the Jaeger UI that will contain Spans from all four services (as well as the Istio proxies)
- The traces gathered by Istio clearly indicate the Album Service and Store Service were called in parallel fashion from Suggestion Service, thus giving valuable insights into the implementation of the whole system, as well as providing accurate timing information

Normal flow:

- The user opens the Istio Distributed Web page
- The user clicks a button that results in an HTTP call to the /parallel endpoint
- The HTTP call is ultimately handled by the /parallel endpoint of the Suggestion Service
- The implementation of the /parallel endpoint calls the Album Service
- The Album Service calls the Album Details Service
- The Album Details service responds with some album details
- The Album Service uses its own data as well the data collected from the Album Details Service to respond to the Suggestion Service
- Concurrently with the call to the Album Service, the implementation of the /parallel endpoint calls the Store Service
- The Store Service responds with the store details
- Once both calls to the Album and Store services are fulfilled, the implementation of the /parallel endpoint uses the responses to reply to the issuer of the call
- Once the call is done, the user navigates to the Jaeger Query UI and views the trace.
