package me.snowdrop.istio.dt;


import static org.awaitility.Awaitility.await;

import io.fabric8.openshift.api.model.v4_0.Route;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.arquillian.cube.istio.api.IstioResource;
import org.arquillian.cube.openshift.impl.client.OpenShiftAssistant;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@IstioResource("classpath:greeting-gateway.yml")
public class OpenshiftIT {

    private static final String ISTIO_NAMESPACE = "istio-system";
    private static final String ISTIO_INGRESS_GATEWAY_ROUTE = "istio-ingressgateway";
    private static final String ISTIO_INGRESS_GATEWAY_SERVICE_NAME = "istio-ingressgateway";
    private static final String JAEGER_QUERY_ROUTE = "jaeger-query";

    @ArquillianResource
    private OpenShiftAssistant openShiftAssistant;

    private String istioIngressGatewayRouteURL;
    private String jaegerTracesURL;

    @Before
    public void init() {
        istioIngressGatewayRouteURL = "http://" + getRouteInIstioNamespace(ISTIO_INGRESS_GATEWAY_ROUTE);
        jaegerTracesURL = "https://" + getRouteInIstioNamespace(JAEGER_QUERY_ROUTE) + "api/traces";

        waitUntilApplicationIsReady();
    }


    /**
     * Interacts with application and test if traces are present in jaeger
     * @throws InterruptedException
     */
    @Test
    public void tracingTest() throws InterruptedException {
        Long startTime = TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis());

        RestAssured.get(istioIngressGatewayRouteURL + "greeting/api/greeting");

        TimeUnit.SECONDS.sleep(10); // wait 10 seconds so span will show up in the jaeger

        RestAssured
                .given()
                .relaxedHTTPSValidation() //jaeger uses https, so we need to trust the cert
                .param("service", ISTIO_INGRESS_GATEWAY_SERVICE_NAME)
                .param("start",startTime)
                .get(jaegerTracesURL)
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

    /**
     * Get a Route from istio namespace
     * @param routeName name of the Route to get
     * @return Route URL, without the protocol specification
     */
    private String getRouteInIstioNamespace(String routeName) {
        Route istioRoute =
                openShiftAssistant
                        .getClient()
                        .routes()
                        .inNamespace(ISTIO_NAMESPACE)
                        .withName(routeName)
                        .get();
        if (istioRoute == null) {
            throw new RuntimeException(String.format("Expected Route %s in namespace %s was not found", routeName, ISTIO_NAMESPACE));
        }
        return istioRoute.getSpec().getHost() + "/";
    }

    // wait until the Istio Ingress Gateway responds with HTTP 200 at the path that is specified
    // in the VirtualService for the the greeting-service application
    private void waitUntilApplicationIsReady() {
        await()
            .pollInterval(1, TimeUnit.SECONDS)
            .atMost(1, TimeUnit.MINUTES)
                .untilAsserted(() ->
                        RestAssured
                                .when()
                                .get(istioIngressGatewayRouteURL + "greeting/")
                                .then()
                                .statusCode(200)
                );
    }

}
