package me.snowdrop.istio.dt;


import static org.awaitility.Awaitility.await;

import io.restassured.RestAssured;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.arquillian.cube.istio.api.IstioResource;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
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

        RestAssured
                .given()
                .baseUri(jaegerQueryURL.toString())
                .relaxedHTTPSValidation() //jaeger uses https, so we need to trust the cert
                .param("service", ISTIO_INGRESS_GATEWAY_NAME)
                .param("start",startTime)
                .get("/api/traces")
                .then()
                .statusCode(200)
                /*
                    data[0].processes should contain something like:

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

                 */
                .body("data[0].processes", new TypeSafeMatcher<Map<String, Object>>() {

                    @Override
                    public void describeTo(Description description) {}

                    @Override
                    protected boolean matchesSafely(Map<String, Object> item) {
                        return ensureProcessesMapContainsBothServices(item);
                    }

                    private boolean ensureProcessesMapContainsBothServices(
                            Map<String, Object> item) {

                        return (item
                                 .values()
                                 .stream()
                                 .map(o -> ((Map<String, String>) o).get("serviceName"))
                                 .filter(n -> n.contains("greeting-service") || n.contains("name-service"))
                                 .distinct()
                                 .count()) == 2;
                    }
                });
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

}
