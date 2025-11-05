package com.tatoalu.hotpotato;

import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Firebase Realtime Database multiplayer manager for serverless Hot Potato game
 * Provides real-time game state synchronization, room management, and player coordination
 */
public class FirebaseRealtimeMultiplayer {
    private static final String TAG = "FirebaseMultiplayer";
    
    // Database paths
    private static final String ROOMS_PATH = "game_rooms";
    private static final String PLAYERS_PATH = "players";
    private static final String GAME_STATE_PATH = "game_state";
    private static final String MESSAGES_PATH = "messages";
    private static final String PRESENCE_PATH = "presence";
    
    // Game states
    public enum GameState {
        WAITING_FOR_PLAYERS,
        COUNTDOWN,
        PLAYING,
        GAME_OVER
    }
    
    // Connection modes
    public enum ConnectionMode {
        AUTO_FALLBACK,    // Try LAN first, fallback to Firebase
        LAN_ONLY,         // Local network only
        FIREBASE_ONLY     // Cloud only
    }
    
    private final FirebaseDatabase database;
    private final DatabaseReference roomsRef;
    private final DatabaseReference presenceRef;
    
    private String currentRoomId;
    private String playerId;
    private String playerName;
    private boolean isHost;
    private ConnectionMode connectionMode = ConnectionMode.AUTO_FALLBACK;
    
    private GameStateListener gameStateListener;
    private RoomListener roomListener;
    private PresenceListener presenceListener;
    
    // Listeners
    private ValueEventListener roomStateListener;
    private ChildEventListener playersListener;
    private ChildEventListener messagesListener;
    private ValueEventListener presenceValueListener;
    
    public interface GameStateListener {
        void onGameStateChanged(GameState state, Map<String, Object> data);
        void onPlayerJoined(String playerId, String playerName);
        void onPlayerLeft(String playerId, String playerName);
        void onGameTick(long remainingMs);
        void onPotatoPassed(String fromPlayer, String toPlayer);
        void onGameOver(String loserPlayerId, String loserName);
        void onError(String error);
    }
    
    public interface RoomListener {
        void onRoomCreated(String roomId);
        void onRoomJoined(String roomId);
        void onRoomNotFound();
        void onRoomFull();
        void onHostLeft();
    }
    
    public interface PresenceListener {
        void onPlayerOnline(String playerId);
        void onPlayerOffline(String playerId);
        void onConnectionStateChanged(boolean connected);
    }
    
    public FirebaseRealtimeMultiplayer() {
        database = FirebaseDatabase.getInstance();
        roomsRef = database.getReference(ROOMS_PATH);
        presenceRef = database.getReference(PRESENCE_PATH);
        
        // Generate unique player ID
        playerId = UUID.randomUUID().toString();
        
        setupPresenceSystem();
    }
    
    public void setGameStateListener(GameStateListener listener) {
        this.gameStateListener = listener;
    }
    
    public void setRoomListener(RoomListener listener) {
        this.roomListener = listener;
    }
    
    public void setPresenceListener(PresenceListener listener) {
        this.presenceListener = listener;
    }
    
    public void setConnectionMode(ConnectionMode mode) {
        this.connectionMode = mode;
    }
    
    public void setPlayerName(String name) {
        this.playerName = name;
    }
    
    /**
     * Create a new game room as host
     */
    public void createRoom(int maxPlayers) {
        if (playerName == null || playerName.trim().isEmpty()) {
            if (gameStateListener != null) {
                gameStateListener.onError("Player name is required");
            }
            return;
        }
        
        isHost = true;
        currentRoomId = generateRoomCode();
        
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("hostId", playerId);
        roomData.put("hostName", playerName);
        roomData.put("maxPlayers", maxPlayers);
        roomData.put("currentPlayers", 1);
        roomData.put("gameState", GameState.WAITING_FOR_PLAYERS.name());
        roomData.put("createdAt", ServerValue.TIMESTAMP);
        roomData.put("lastActivity", ServerValue.TIMESTAMP);
        
        // Initialize players list
        Map<String, Object> players = new HashMap<>();
        Map<String, Object> hostPlayer = new HashMap<>();
        hostPlayer.put("name", playerName);
        hostPlayer.put("isHost", true);
        hostPlayer.put("joinedAt", ServerValue.TIMESTAMP);
        hostPlayer.put("isReady", true);
        players.put(playerId, hostPlayer);
        roomData.put("players", players);
        
        // Initialize game state
        Map<String, Object> gameState = new HashMap<>();
        gameState.put("currentHolder", playerId);
        gameState.put("timeRemaining", 0);
        gameState.put("gameStartTime", 0);
        gameState.put("burnTime", 0);
        roomData.put("gameData", gameState);
        
        roomsRef.child(currentRoomId).setValue(roomData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Room created: " + currentRoomId);
                setupRoomListeners();
                if (roomListener != null) {
                    roomListener.onRoomCreated(currentRoomId);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to create room", e);
                if (gameStateListener != null) {
                    gameStateListener.onError("Failed to create room: " + e.getMessage());
                }
            });
    }
    
    /**
     * Join an existing room by room code
     */
    public void joinRoom(String roomCode) {
        if (playerName == null || playerName.trim().isEmpty()) {
            if (gameStateListener != null) {
                gameStateListener.onError("Player name is required");
            }
            return;
        }
        
        isHost = false;
        currentRoomId = roomCode.toUpperCase();
        
        roomsRef.child(currentRoomId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    if (roomListener != null) {
                        roomListener.onRoomNotFound();
                    }
                    return;
                }
                
                Map<String, Object> roomData = (Map<String, Object>) dataSnapshot.getValue();
                if (roomData == null) {
                    if (roomListener != null) {
                        roomListener.onRoomNotFound();
                    }
                    return;
                }
                
                Long maxPlayers = (Long) roomData.get("maxPlayers");
                Long currentPlayers = (Long) roomData.get("currentPlayers");
                
                if (currentPlayers != null && maxPlayers != null && currentPlayers >= maxPlayers) {
                    if (roomListener != null) {
                        roomListener.onRoomFull();
                    }
                    return;
                }
                
                // Add player to room
                Map<String, Object> playerData = new HashMap<>();
                playerData.put("name", playerName);
                playerData.put("isHost", false);
                playerData.put("joinedAt", ServerValue.TIMESTAMP);
                playerData.put("isReady", false);
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("players/" + playerId, playerData);
                updates.put("currentPlayers", (currentPlayers != null ? currentPlayers : 0) + 1);
                updates.put("lastActivity", ServerValue.TIMESTAMP);
                
                roomsRef.child(currentRoomId).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Joined room: " + currentRoomId);
                        setupRoomListeners();
                        if (roomListener != null) {
                            roomListener.onRoomJoined(currentRoomId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to join room", e);
                        if (gameStateListener != null) {
                            gameStateListener.onError("Failed to join room: " + e.getMessage());
                        }
                    });
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to check room", databaseError.toException());
                if (gameStateListener != null) {
                    gameStateListener.onError("Failed to check room: " + databaseError.getMessage());
                }
            }
        });
    }
    
    /**
     * Start the game (host only)
     */
    public void startGame() {
        if (!isHost || currentRoomId == null) {
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("gameState", GameState.COUNTDOWN.name());
        updates.put("gameData/gameStartTime", ServerValue.TIMESTAMP);
        updates.put("lastActivity", ServerValue.TIMESTAMP);
        
        roomsRef.child(currentRoomId).updateChildren(updates);
    }
    
    /**
     * Pass the potato to another player
     */
    public void passPotatoTo(String targetPlayerId) {
        if (currentRoomId == null) {
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("gameData/currentHolder", targetPlayerId);
        updates.put("gameData/lastPassTime", ServerValue.TIMESTAMP);
        updates.put("lastActivity", ServerValue.TIMESTAMP);
        
        // Add pass message
        String messageId = UUID.randomUUID().toString();
        Map<String, Object> passMessage = new HashMap<>();
        passMessage.put("type", "pass");
        passMessage.put("fromPlayer", playerId);
        passMessage.put("toPlayer", targetPlayerId);
        passMessage.put("timestamp", ServerValue.TIMESTAMP);
        updates.put("messages/" + messageId, passMessage);
        
        roomsRef.child(currentRoomId).updateChildren(updates);
    }
    
    /**
     * Update game timer (host only)
     */
    public void updateGameTimer(long remainingMs) {
        if (!isHost || currentRoomId == null) {
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("gameData/timeRemaining", remainingMs);
        updates.put("lastActivity", ServerValue.TIMESTAMP);
        
        roomsRef.child(currentRoomId).updateChildren(updates);
    }
    
    /**
     * End the game with a loser (host only)
     */
    public void endGame(String loserPlayerId) {
        if (!isHost || currentRoomId == null) {
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("gameState", GameState.GAME_OVER.name());
        updates.put("gameData/loserPlayerId", loserPlayerId);
        updates.put("gameData/gameEndTime", ServerValue.TIMESTAMP);
        updates.put("lastActivity", ServerValue.TIMESTAMP);
        
        roomsRef.child(currentRoomId).updateChildren(updates);
    }
    
    /**
     * Leave the current room
     */
    public void leaveRoom() {
        if (currentRoomId == null) {
            return;
        }
        
        // Remove listeners
        removeRoomListeners();
        
        // Remove player from room
        roomsRef.child(currentRoomId).child("players").child(playerId).removeValue();
        
        // Update player count
        roomsRef.child(currentRoomId).child("currentPlayers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long currentCount = dataSnapshot.getValue(Long.class);
                if (currentCount != null && currentCount > 0) {
                    roomsRef.child(currentRoomId).child("currentPlayers").setValue(currentCount - 1);
                }
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to update player count", databaseError.toException());
            }
        });
        
        // If host is leaving, notify others
        if (isHost) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("hostLeft", true);
            updates.put("lastActivity", ServerValue.TIMESTAMP);
            roomsRef.child(currentRoomId).updateChildren(updates);
        }
        
        currentRoomId = null;
        isHost = false;
    }
    
    private void setupRoomListeners() {
        if (currentRoomId == null) {
            return;
        }
        
        DatabaseReference roomRef = roomsRef.child(currentRoomId);
        
        // Room state listener
        roomStateListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    return;
                }
                
                Map<String, Object> roomData = (Map<String, Object>) dataSnapshot.getValue();
                if (roomData == null) {
                    return;
                }
                
                // Check if host left
                Boolean hostLeft = (Boolean) roomData.get("hostLeft");
                if (hostLeft != null && hostLeft && !isHost) {
                    if (roomListener != null) {
                        roomListener.onHostLeft();
                    }
                    return;
                }
                
                // Handle game state changes
                String gameStateStr = (String) roomData.get("gameState");
                if (gameStateStr != null) {
                    GameState gameState = GameState.valueOf(gameStateStr);
                    Map<String, Object> gameData = (Map<String, Object>) roomData.get("gameData");
                    
                    if (gameStateListener != null) {
                        gameStateListener.onGameStateChanged(gameState, gameData);
                    }
                    
                    // Handle specific game events
                    if (gameData != null) {
                        Long timeRemaining = (Long) gameData.get("timeRemaining");
                        if (timeRemaining != null && gameStateListener != null) {
                            gameStateListener.onGameTick(timeRemaining);
                        }
                        
                        if (gameState == GameState.GAME_OVER) {
                            String loserPlayerId = (String) gameData.get("loserPlayerId");
                            if (loserPlayerId != null && gameStateListener != null) {
                                // Get loser name from players
                                Map<String, Object> players = (Map<String, Object>) roomData.get("players");
                                if (players != null) {
                                    Map<String, Object> loserData = (Map<String, Object>) players.get(loserPlayerId);
                                    if (loserData != null) {
                                        String loserName = (String) loserData.get("name");
                                        gameStateListener.onGameOver(loserPlayerId, loserName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Room state listener cancelled", databaseError.toException());
            }
        };
        roomRef.addValueEventListener(roomStateListener);
        
        // Players listener
        playersListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                String playerId = dataSnapshot.getKey();
                Map<String, Object> playerData = (Map<String, Object>) dataSnapshot.getValue();
                if (playerData != null && gameStateListener != null) {
                    String playerName = (String) playerData.get("name");
                    gameStateListener.onPlayerJoined(playerId, playerName);
                }
            }
            
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                // Handle player updates if needed
            }
            
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String playerId = dataSnapshot.getKey();
                Map<String, Object> playerData = (Map<String, Object>) dataSnapshot.getValue();
                if (playerData != null && gameStateListener != null) {
                    String playerName = (String) playerData.get("name");
                    gameStateListener.onPlayerLeft(playerId, playerName);
                }
            }
            
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {}
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Players listener cancelled", databaseError.toException());
            }
        };
        roomRef.child("players").addChildEventListener(playersListener);
        
        // Messages listener for real-time events
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Map<String, Object> message = (Map<String, Object>) dataSnapshot.getValue();
                if (message != null) {
                    String type = (String) message.get("type");
                    if ("pass".equals(type) && gameStateListener != null) {
                        String fromPlayer = (String) message.get("fromPlayer");
                        String toPlayer = (String) message.get("toPlayer");
                        gameStateListener.onPotatoPassed(fromPlayer, toPlayer);
                    }
                }
            }
            
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {}
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {}
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Messages listener cancelled", databaseError.toException());
            }
        };
        roomRef.child("messages").addChildEventListener(messagesListener);
    }
    
    private void removeRoomListeners() {
        if (currentRoomId == null) {
            return;
        }
        
        DatabaseReference roomRef = roomsRef.child(currentRoomId);
        
        if (roomStateListener != null) {
            roomRef.removeEventListener(roomStateListener);
            roomStateListener = null;
        }
        
        if (playersListener != null) {
            roomRef.child("players").removeEventListener(playersListener);
            playersListener = null;
        }
        
        if (messagesListener != null) {
            roomRef.child("messages").removeEventListener(messagesListener);
            messagesListener = null;
        }
    }
    
    private void setupPresenceSystem() {
        DatabaseReference connectedRef = database.getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (presenceListener != null) {
                    presenceListener.onConnectionStateChanged(connected);
                }
                
                if (connected) {
                    DatabaseReference presenceRef = database.getReference(PRESENCE_PATH).child(playerId);
                    presenceRef.onDisconnect().removeValue();
                    presenceRef.setValue(true);
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Presence listener was cancelled");
            }
        });
    }
    
    private String generateRoomCode() {
        // Generate a 6-character room code
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return code.toString();
    }
    
    public String getCurrentRoomId() {
        return currentRoomId;
    }
    
    public String getPlayerId() {
        return playerId;
    }
    
    public boolean isHost() {
        return isHost;
    }
    
    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }
    
    /**
     * Clean up all listeners and connections
     */
    public void cleanup() {
        removeRoomListeners();
        if (presenceValueListener != null && presenceRef != null) {
            presenceRef.removeEventListener(presenceValueListener);
        }
    }
}