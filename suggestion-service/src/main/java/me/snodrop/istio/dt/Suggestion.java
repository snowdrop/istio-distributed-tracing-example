package me.snodrop.istio.dt;

import java.util.Map;

public class Suggestion {

    private final Album album;
    private final Store store;

    public Suggestion(Album album, Store store) {
        this.album = album;
        this.store = store;
    }

    public Album getAlbum() {
        return album;
    }

    public Store getStore() {
        return store;
    }

    public static class Album {

        private String name;
        private String artist;
        private Map<String, String> details;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public Map<String, String> getDetails() {
            return details;
        }

        public void setDetails(Map<String, String> details) {
            this.details = details;
        }
    }

    public static class Store {
        private String name;

        public String getName() {
            return name;
        }
    }
}
