package me.snodrop.istio.dt;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class GreeetingController {

    private final RestTemplate restTemplate;

    private final String cutenameServiceName;

    public GreeetingController(RestTemplate restTemplate,
                               @Value("${service.cute-name.name}") String cutenameServiceName) {
        this.restTemplate = restTemplate;
        this.cutenameServiceName = cutenameServiceName;
    }

    @GetMapping("/api/greeting")
    public Greeting serial() {
        final String name =
            restTemplate.getForObject(getURI(cutenameServiceName, "api/name"), String.class);

        return new Greeting(String.format("Hello, %s!", name));
    }

    private URI getURI(String serviceName, String path) {
        return URI.create(String.format("http://%s/%s", serviceName, path));
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
