package me.snowdrop.istio.dt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.dekorate.testing.annotation.Inject;
import io.dekorate.testing.openshift.annotation.OpenshiftIntegrationTest;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

@OpenshiftIntegrationTest(deployEnabled = false, buildEnabled = false)
public class OpenshiftIT {

    private static final String ISTIO_NAMESPACE = "istio-system";
    private static final String JAEGER_QUERY_NAME = "jaeger";
    private static final String ISTIO_INGRESS_GATEWAY_NAME = "istio-ingressgateway";

    @Inject
    KubernetesClient kubernetesClient;

    private URL jaegerQueryURL;
    private URL ingressGatewayURL;

    @BeforeEach
    public void setup() throws MalformedURLException {
        jaegerQueryURL = getBaseUrlByRouteName(JAEGER_QUERY_NAME, ISTIO_NAMESPACE);
        ingressGatewayURL = getBaseUrlByRouteName(ISTIO_INGRESS_GATEWAY_NAME, ISTIO_NAMESPACE);
    }

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
                .get("/greeting/api/greeting")
                .then().statusCode(200);

        TimeUnit.SECONDS.sleep(10); // wait 10 seconds so span will show up in the jaeger

        final Response response = RestAssured.given()
                .baseUri(jaegerQueryURL.toString())
                .relaxedHTTPSValidation() //jaeger uses https, so we need to trust the cert
                .param("service", ISTIO_INGRESS_GATEWAY_NAME + "." + ISTIO_NAMESPACE)
                .param("start", startTime)
                .get("/api/traces")
                .then().statusCode(200).extract().response();

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

    private URL getBaseUrlByRouteName(String routeName, String namespace) throws MalformedURLException {
        Route route = kubernetesClient.adapt(OpenShiftClient.class).routes().inNamespace(namespace).withName(routeName).get();
        String protocol = route.getSpec().getTls() == null ? "http" : "https";
        int port = "http".equals(protocol) ? 80 : 443;
        return new URL(protocol, route.getSpec().getHost(), port, "/");
    }

}
