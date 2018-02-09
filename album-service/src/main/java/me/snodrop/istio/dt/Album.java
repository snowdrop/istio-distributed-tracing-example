package me.snodrop.istio.dt;

public class Album {

    private final String name;
    private final String artist;

    public Album(String name, String artist) {
        this.name = name;
        this.artist = artist;
    }

    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }
}
