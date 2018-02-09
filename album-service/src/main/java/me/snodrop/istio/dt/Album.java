package me.snodrop.istio.dt;

import java.util.HashMap;
import java.util.Map;

public class Album {

    private final Long id;
    private final String name;
    private final String artist;
    private final Map<String, String> details;

    public Album(Long id, String name, String artist) {
        this(id, name, artist, new HashMap<>());
    }

    public Album(Long id, String name, String artist, Map<String, String> details) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.details = details;
    }

    public Album withDetails(Map<String, String> details) {
        return new Album(id, name, artist, details);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
