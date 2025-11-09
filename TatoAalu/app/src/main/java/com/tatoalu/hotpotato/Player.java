package com.tatoalu.hotpotato;

/**
 * Player model for Hot Potato game
 * Represents a player with ID, name, and game state
 */
public class Player {
    public final String id;
    public final String name;
    public boolean hasPotato = false;
    public boolean isActive = true;
    public boolean isEliminated = false;

    // UI position for player layout
    public int layoutPosition = -1; // 0=top-left, 1=bottom-right, 2=top-right, 3=bottom-left

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Player(String name) {
        this.id = generateId();
        this.name = name;
    }

    /**
     * Generate a unique ID for the player
     */
    private String generateId() {
        return "player_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    /**
     * Give the potato to this player
     */
    public void givePotato() {
        this.hasPotato = true;
    }

    /**
     * Take the potato away from this player
     */
    public void takePotato() {
        this.hasPotato = false;
    }

    /**
     * Mark this player as eliminated from the game
     */
    public void eliminate() {
        this.isEliminated = true;
        this.isActive = false;
        this.hasPotato = false;
    }

    /**
     * Check if this player can receive the potato
     */
    public boolean canReceivePotato() {
        return isActive && !isEliminated && !hasPotato;
    }

    /**
     * Check if this player can pass the potato
     */
    public boolean canPassPotato() {
        return isActive && !isEliminated && hasPotato;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Player player = (Player) obj;
        return id.equals(player.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
