package me.snowdrop.istio.dt;


import io.fabric8.openshift.api.model.v3_1.Route;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import me.snowdrop.istio.api.model.IstioResource;
import org.arquillian.cube.istio.impl.IstioAssistant;
import org.arquillian.cube.openshift.impl.client.OpenShiftAssistant;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@RunWith(Arquillian.class)
public class OpenshiftIT {
    @ArquillianResource
    private IstioAssistant istioAssistant;

    @ArquillianResource
    private OpenShiftAssistant openShiftAssistant;

    private static String istioURL;
    private static String jaegerURL;
    private static boolean isInit = false;
    private static List<IstioResource> routeRule = null;

    private final String APP_URL = "greeting/";

    @Before
    public void init() throws Exception {
        // initiate only before first test
        if (!isInit) {
            istioURL = "http://" + getIstioRoute("istio-ingress");
            jaegerURL = "https://" + getIstioRoute("jaeger-query") + "api/traces";

            routeRule = deployRouteRule("route-rule-redir.yml");
            await().
                    pollInterval(1, TimeUnit.SECONDS)
                    .atMost(1, TimeUnit.MINUTES)
                    .until(() -> {
                try {
                    Response response = RestAssured.get(istioURL + APP_URL);
                    return response.getStatusCode() == 200;
                } catch (Exception ignored) {
                    return false;
                }
            });
            isInit = true;
        }
    }

    @After
    public void cleanup() {
        if (routeRule != null){
            istioAssistant.undeployIstioResources(routeRule);
        }
    }

    /**
     * Interacts with application and test if traces are present in jaeger
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
    public void tracingTest() throws ParseException, InterruptedException {
        Long startTime = TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis());

        RestAssured.get(istioURL + "greeting/api/greeting");

        TimeUnit.SECONDS.sleep(10); // wait 10 seconds so span will show up in the jaeger

        JSONArray jaegerTraces = getJaegerTraces(startTime);
        Assert.assertEquals("Query should create one trace",1,jaegerTraces.size());
        int spanCount = ((JSONArray)((JSONObject)jaegerTraces.get(0)).get("spans")).size();
        Assert.assertTrue("Trace should have at least 4 spans, has: " + spanCount, spanCount >= 4);
    }

    private List<IstioResource> deployRouteRule(String routeRuleFile) throws IOException {
        return istioAssistant.deployIstioResources(
                Files.newInputStream(Paths.get("../rules/" + routeRuleFile)));
    }

    /**
     * Get a Route from istio namespace
     * @param routeName name of the Route to get
     * @return Route URL, without the protocol specification
     */
    private String getIstioRoute(String routeName) {
        Route istioRoute = openShiftAssistant.getClient()
                .routes()
                .inNamespace("istio-system")
                .withName(routeName)
                .get();
        if (istioRoute == null) {
            throw new RuntimeException("Istio " + routeName + " route not found");
        }
        return istioRoute.getSpec().getHost() + "/";
    }

    private JSONArray getJaegerTraces(Long startTime) throws ParseException {

        Response response = RestAssured
                .given()
                    .relaxedHTTPSValidation() //jaeger uses https, so we need to trust the cert
                    .param("service","istio-ingress")
                    .param("start",startTime)
                .get(jaegerURL);
        JSONParser parser = new JSONParser();
        JSONObject obj = (JSONObject)parser.parse(response.asString());

        return (JSONArray)obj.get("data");
    }
}
