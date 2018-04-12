package me.snodrop.istio.dt;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class GreeetingController {

    private final RestTemplate restTemplate;

    private final String cutenameServiceName;

    public GreeetingController(RestTemplate restTemplate,
                               @Value("${service.cutename.name}") String cutenameServiceName) {
        this.restTemplate = restTemplate;
        this.cutenameServiceName = cutenameServiceName;
    }

    @GetMapping("/api/greeting")
    public Greeting serial(@RequestHeader HttpHeaders headers) {
        final HttpHeaders tracingHeaders = tracingHeaders(headers);

        final ResponseEntity<String> responseEntity =
            restTemplate.exchange(
                getURI(cutenameServiceName, "api/name"),
                HttpMethod.GET,
                new HttpEntity<>(tracingHeaders),
                String.class
            );

        final String name = responseEntity.getBody();

        return new Greeting(String.format("Hello, %s!", name));
    }

    private URI getURI(String serviceName, String path) {
        return URI.create(String.format("http://%s/%s", serviceName, path));
    }


    private static final List<String> HEADERS_THAT_NEED_TO_BE_PROPAGATES = Arrays.asList(
        "x-request-id",
        "x-b3-traceid",
        "x-b3-spanid",
        "x-b3-parentspanid",
        "x-b3-sampled",
        "x-b3-flags",
        "x-ot-span-context"
    );

    private HttpHeaders tracingHeaders(HttpHeaders allHeaders) {
        final List<Map.Entry<String, List<String>>> list =
            allHeaders
                .entrySet()
                .stream()
                .filter(e -> HEADERS_THAT_NEED_TO_BE_PROPAGATES.contains(e.getKey()))
                .collect(Collectors.toList());

        final HttpHeaders result = new HttpHeaders();
        list.forEach(e -> result.add(e.getKey(), e.getValue().get(0)));
        return result;
    }

    static class Greeting {
        private final String message;

        public Greeting(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
