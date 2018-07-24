package me.snowdrop.istio.dt;


import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.arquillian.cube.istio.api.IstioResource;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.assertj.core.api.Condition;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@IstioResource("classpath:greeting-gateway.yml")
public class OpenshiftIT {

    private static final String ISTIO_NAMESPACE = "istio-system";
    private static final String JAEGER_QUERY_NAME = "jaeger-query";
    private static final String ISTIO_INGRESS_GATEWAY_NAME = "istio-ingressgateway";

    @RouteURL(value = JAEGER_QUERY_NAME, namespace = ISTIO_NAMESPACE)
    private URL jaegerQueryURL;

    @RouteURL(value = ISTIO_INGRESS_GATEWAY_NAME, namespace = ISTIO_NAMESPACE)
    private URL ingressGatewayURL;


    /**
     * Interacts with application and test if traces are present in jaeger
     * @throws InterruptedException
     */
    @Test
    public void tracingTest() throws InterruptedException {
        waitUntilApplicationIsReady();

        Long startTime = TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis());

        RestAssured
                .given()
                .baseUri(ingressGatewayURL.toString())
                .get("/greeting/api/greeting");

        TimeUnit.SECONDS.sleep(10); // wait 10 seconds so span will show up in the jaeger

        final Response response =
                RestAssured
                .given()
                .baseUri(jaegerQueryURL.toString())
                .relaxedHTTPSValidation() //jaeger uses https, so we need to trust the cert
                .param("service", ISTIO_INGRESS_GATEWAY_NAME)
                .param("start", startTime)
                .get("/api/traces");

        assertThat(response.statusCode()).isEqualTo(200);

        final JsonPath jsonPath = response.jsonPath();

       /*
           data[0].processes should contain something like:
           {
               "p1": {
                   "serviceName": "istio-ingressgateway",
                   "tags": [
                       {
                           "key": "ip",
                           "type": "string",
                           "value": "172.17.0.8"
                       }
                   ]
               },
               "p2": {
                   "serviceName": "istio-policy",
                   "tags": [
                       {
                           "key": "ip",
                           "type": "string",
                           "value": "172.17.0.11"
                       }
                   ]
               }

               ...

           }

        */
        final Map<String, Map> processesMap = jsonPath.getMap("data[0].processes", String.class, Map.class);
        assertThat(processesMap.values())
                .isNotEmpty()
                .extracting("serviceName", String.class)
                .filteredOn((Predicate<String>) s -> s.contains("spring-boot"))
                .haveAtLeastOne(isApplicationService("greeting"))
                .haveAtLeastOne(isApplicationService("cute-name"));
    }

    // wait until the Istio Ingress Gateway responds with HTTP 200 at the path that is specified
    // in the VirtualService for the the greeting-service application
    private void waitUntilApplicationIsReady() {
        await()
            .pollInterval(1, TimeUnit.SECONDS)
            .atMost(1, TimeUnit.MINUTES)
                .untilAsserted(() ->
                        RestAssured
                                .given()
                                .baseUri(ingressGatewayURL.toString())
                                .when()
                                .get("/greeting/")
                                .then()
                                .statusCode(200)
                );
    }

    private Condition<String> isApplicationService(String name) {
        return new Condition<>(s -> s.contains(name), "a trace named: " + name);
    }

}
