package com.tatoalu.hotpotato;

public enum GameMode {
    HOT_POTATO("Hot Potato", "Pass the potato before time runs out!"),
    MUSICAL_CHAIRS("Musical Chairs", "Find a chair when the music stops!");

    private final String displayName;
    private final String description;

    GameMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static GameMode fromString(String mode) {
        switch (mode.toLowerCase()) {
            case "musical_chairs":
                return MUSICAL_CHAIRS;
            case "hot_potato":
            default:
                return HOT_POTATO;
        }
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}