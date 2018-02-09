package me.snodrop.istio.dt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
public class AlbumController {

    private static final List<Album> ALBUMS = Arrays.asList(
            new Album(1L, "Metallica", "...And Justice for All"),
            new Album(2L, "Iron Maiden", "Brave New World"),
            new Album(3L, "Disturbed", "Immortalized")
    );

    private final Random random = new Random();

    private final RestTemplate restTemplate;
    private final String albumDetailsServiceName;

    public AlbumController(RestTemplate restTemplate,
                           @Value("${service.album-details.name}") String albumDetailsServiceName) {
        this.restTemplate = restTemplate;
        this.albumDetailsServiceName = albumDetailsServiceName;
    }

    @RequestMapping("/random")
    public Album random() {
        final Album randomAlbum = getRandomAlbum();

        return randomAlbum.withDetails(
                restTemplate.getForObject(getURI(albumDetailsServiceName, randomAlbum.getId()), Map.class)
        );
    }

    private Album getRandomAlbum() {
        return ALBUMS.get(random.nextInt(ALBUMS.size()));
    }

    private URI getURI(String serviceName, Long albumId) {
        return URI.create(String.format("http://%s/%d", serviceName, albumId));
    }
}
