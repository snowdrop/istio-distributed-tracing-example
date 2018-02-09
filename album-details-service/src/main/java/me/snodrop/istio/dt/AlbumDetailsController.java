package me.snodrop.istio.dt;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AlbumDetailsController {

    @RequestMapping("/{id}")
    public Map<String, String> details(@PathVariable("id") Long id) {
        return new HashMap<String, String>() {{
            put("detail1", "value-for-album-" + id);
            put("detail2", "something-for-album-" + id);
        }};
    }

}
