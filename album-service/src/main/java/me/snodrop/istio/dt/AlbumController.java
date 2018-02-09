package me.snodrop.istio.dt;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@RestController
public class AlbumController {

    private static final List<Album> ALBUMS = Arrays.asList(
            new Album("Metallica", "...And Justice for All"),
            new Album("Iron Maiden", "Brave New World"),
            new Album("Disturbed", "Immortalized")
    );

    private final Random random = new Random();

    @RequestMapping("/random")
    public Album random() {
        return getRandomAlbum();
    }

    private Album getRandomAlbum() {
        return ALBUMS.get(random.nextInt(ALBUMS.size()));
    }
}
