package me.snodrop.istio.dt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@RestController
public class SuggestionController {

    private final RestTemplate restTemplate;
    private final AsyncRestTemplate asyncRestTemplate;

    private final String albumServiceName;
    private final String storeServiceName;

    public SuggestionController(RestTemplate restTemplate,
                                AsyncRestTemplate asyncRestTemplate,
                                @Value("${service.album.name}") String albumServiceName,
                                @Value("${service.store.name}") String storeServiceName) {
        this.restTemplate = restTemplate;
        this.asyncRestTemplate = asyncRestTemplate;
        this.albumServiceName = albumServiceName;
        this.storeServiceName = storeServiceName;
    }

    @RequestMapping("/serial")
    public Suggestion serial() {

        final Suggestion.Album album =
                restTemplate.getForObject(getURI(albumServiceName, "random"), Suggestion.Album.class);

        final Suggestion.Store store =
                restTemplate.getForObject(getURI(storeServiceName, "random"), Suggestion.Store.class);

        return new Suggestion(album, store);
    }

    @RequestMapping("/parallel")
    public Suggestion parallel() throws Exception {

        final ListenableFuture<ResponseEntity<Suggestion.Album>> albumFuture =
                asyncRestTemplate.getForEntity(getURI(albumServiceName, "random"), Suggestion.Album.class);

        final ListenableFuture<ResponseEntity<Suggestion.Store>> storeFuture =
                asyncRestTemplate.getForEntity(getURI(storeServiceName, "random"), Suggestion.Store.class);

        return new Suggestion(albumFuture.get().getBody(), storeFuture.get().getBody());
    }

    private URI getURI(String serviceName, String path) {
        return URI.create(String.format("http://%s/%s", serviceName, path));
    }
}
