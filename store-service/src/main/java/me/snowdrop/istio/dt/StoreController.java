package me.snowdrop.istio.dt;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@RestController
public class StoreController {

    private static final List<Store> STORES = Arrays.asList(
            new Store("Spotify"),
            new Store("Deezer"),
            new Store("Apple Music")
    );

    private final Random random = new Random();

    @RequestMapping("/random")
    public Store random() {
        return getRandomAlbum();
    }

    private Store getRandomAlbum() {
        return STORES.get(random.nextInt(STORES.size()));
    }
}
