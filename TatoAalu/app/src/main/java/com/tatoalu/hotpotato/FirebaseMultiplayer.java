package com.tatoalu.hotpotato;

import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirebaseMultiplayer {
    private static final String TAG = "FirebaseMultiplayer";
    private static final String ROOMS_PATH = "game_rooms";
    private static final String PLAYERS_PATH = "players";
    private static final String GAME_STATE_PATH = "game_state";
    private static final String MESSAGES_PATH = "messages";
    
    public interface FirebaseListener {
        void onRoomCreated(String roomId);
        void onRoomJoined(String roomId, int playerIndex);
        void onPlayerJoined(String playerName, int playerIndex);
        void onPlayerLeft(String playerName, int playerIndex);
        void onGameStateChanged(int holder, String[] players);
        void onGameStarted();
        void onTick(long remainingMs);
        void onBurn(String loserName);
        void onPassReceived(int fromPlayer, int toPlayer);
        void onError(String error);
        void onDisconnected();
    }
    
    private final FirebaseDatabase database;
    private final String playerId;
    private final String playerName;
    private FirebaseListener listener;
    
    private String currentRoomId;
    private DatabaseReference roomRef;
    private DatabaseReference playersRef;
    private DatabaseReference gameStateRef;
    private DatabaseReference messagesRef;
    
    private boolean isHost = false;
    private int myPlayerIndex = -1;
    private List<String> playerNames = new ArrayList<>();
    
    // Event listeners
    private ValueEventListener gameStateListener;
    private ChildEventListener playersListener;
    private ChildEventListener messagesListener;
    
    public FirebaseMultiplayer(String playerName) {
        this.database = FirebaseDatabase.getInstance();
        this.playerId = UUID.randomUUID().toString();
        this.playerName = playerName;
    }
    
    public void setListener(FirebaseListener listener) {
        this.listener = listener;
    }
    
    public String getPlayerId() {
        return playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public boolean isHost() {
        return isHost;
    }
    
    public int getMyPlayerIndex() {
        return myPlayerIndex;
    }
    
    // Host creates a new room
    public void createRoom(int maxPlayers) {
        currentRoomId = generateRoomId();
        isHost = true;
        myPlayerIndex = 0;
        
        roomRef = database.getReference(ROOMS_PATH).child(currentRoomId);
        playersRef = roomRef.child(PLAYERS_PATH);
        gameStateRef = roomRef.child(GAME_STATE_PATH);
        messagesRef = roomRef.child(MESSAGES_PATH);
        
        // Initialize room data
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("host", playerId);
        roomData.put("maxPlayers", maxPlayers);
        roomData.put("created", System.currentTimeMillis());
        roomData.put("status", "waiting");
        
        // Add host as first player
        Map<String, Object> hostPlayer = new HashMap<>();
        hostPlayer.put("id", playerId);
        hostPlayer.put("name", playerName);
        hostPlayer.put("index", 0);
        hostPlayer.put("connected", true);
        
        roomData.put(PLAYERS_PATH + "/0", hostPlayer);
        
        // Initialize game state
        Map<String, Object> gameState = new HashMap<>();
        gameState.put("holder", 0);
        gameState.put("gameStarted", false);
        gameState.put("gameEnded", false);
        gameState.put("remainingTime", 0);
        
        roomData.put(GAME_STATE_PATH, gameState);
        
        roomRef.setValue(roomData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.i(TAG, "Room created: " + currentRoomId);
                playerNames.add(playerName);
                setupListeners();
                if (listener != null) {
                    listener.onRoomCreated(currentRoomId);
                }
            } else {
                Log.e(TAG, "Failed to create room", task.getException());
                if (listener != null) {
                    listener.onError("Failed to create room: " + task.getException().getMessage());
                }
            }
        });
    }
    
    // Client joins an existing room
    public void joinRoom(String roomId) {
        currentRoomId = roomId;
        isHost = false;
        
        roomRef = database.getReference(ROOMS_PATH).child(currentRoomId);
        playersRef = roomRef.child(PLAYERS_PATH);
        gameStateRef = roomRef.child(GAME_STATE_PATH);
        messagesRef = roomRef.child(MESSAGES_PATH);
        
        // Check if room exists and has space
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    if (listener != null) {
                        listener.onError("Room not found");
                    }
                    return;
                }
                
                String status = snapshot.child("status").getValue(String.class);
                if (!"waiting".equals(status)) {
                    if (listener != null) {
                        listener.onError("Game already started");
                    }
                    return;
                }
                
                Long maxPlayers = snapshot.child("maxPlayers").getValue(Long.class);
                if (maxPlayers == null) maxPlayers = 8L;
                
                // Count current players
                int playerCount = 0;
                for (DataSnapshot playerSnapshot : snapshot.child(PLAYERS_PATH).getChildren()) {
                    if (Boolean.TRUE.equals(playerSnapshot.child("connected").getValue(Boolean.class))) {
                        playerCount++;
                    }
                }
                
                if (playerCount >= maxPlayers) {
                    if (listener != null) {
                        listener.onError("Room is full");
                    }
                    return;
                }
                
                // Find available player index
                myPlayerIndex = findAvailablePlayerIndex(snapshot.child(PLAYERS_PATH));
                
                // Add player to room
                Map<String, Object> playerData = new HashMap<>();
                playerData.put("id", playerId);
                playerData.put("name", playerName);
                playerData.put("index", myPlayerIndex);
                playerData.put("connected", true);
                
                playersRef.child(String.valueOf(myPlayerIndex)).setValue(playerData).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.i(TAG, "Joined room: " + currentRoomId + " as player " + myPlayerIndex);
                        setupListeners();
                        if (listener != null) {
                            listener.onRoomJoined(currentRoomId, myPlayerIndex);
                        }
                    } else {
                        Log.e(TAG, "Failed to join room", task.getException());
                        if (listener != null) {
                            listener.onError("Failed to join room: " + task.getException().getMessage());
                        }
                    }
                });
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to check room", error.toException());
                if (listener != null) {
                    listener.onError("Failed to check room: " + error.getMessage());
                }
            }
        });
    }
    
    // Find available player index
    private int findAvailablePlayerIndex(DataSnapshot playersSnapshot) {
        for (int i = 0; i < Config.MAX_PLAYERS; i++) {
            DataSnapshot playerSnapshot = playersSnapshot.child(String.valueOf(i));
            if (!playerSnapshot.exists() || !Boolean.TRUE.equals(playerSnapshot.child("connected").getValue(Boolean.class))) {
                return i;
            }
        }
        return -1; // Room is full
    }
    
    // Setup Firebase listeners
    private void setupListeners() {
        // Listen for player changes
        playersListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                handlePlayerChange(snapshot, "added");
            }
            
            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                handlePlayerChange(snapshot, "changed");
            }
            
            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                handlePlayerChange(snapshot, "removed");
            }
            
            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Players listener cancelled", error.toException());
            }
        };
        playersRef.addChildEventListener(playersListener);
        
        // Listen for game state changes
        gameStateListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                handleGameStateChange(snapshot);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Game state listener cancelled", error.toException());
            }
        };
        gameStateRef.addValueEventListener(gameStateListener);
        
        // Listen for messages (passes, etc.)
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                handleMessage(snapshot);
            }
            
            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onChildRemoved(DataSnapshot snapshot) {}
            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Messages listener cancelled", error.toException());
            }
        };
        messagesRef.addChildEventListener(messagesListener);
    }
    
    private void handlePlayerChange(DataSnapshot snapshot, String changeType) {
        String playerName = snapshot.child("name").getValue(String.class);
        Long playerIndex = snapshot.child("index").getValue(Long.class);
        Boolean connected = snapshot.child("connected").getValue(Boolean.class);
        
        if (playerName == null || playerIndex == null) return;
        
        int index = playerIndex.intValue();
        
        if ("added".equals(changeType) || ("changed".equals(changeType) && Boolean.TRUE.equals(connected))) {
            // Ensure playerNames list is large enough
            while (playerNames.size() <= index) {
                playerNames.add(null);
            }
            playerNames.set(index, playerName);
            
            if (listener != null) {
                listener.onPlayerJoined(playerName, index);
            }
        } else if ("removed".equals(changeType) || ("changed".equals(changeType) && !Boolean.TRUE.equals(connected))) {
            if (index < playerNames.size()) {
                playerNames.set(index, null);
            }
            
            if (listener != null) {
                listener.onPlayerLeft(playerName, index);
            }
        }
    }
    
    private void handleGameStateChange(DataSnapshot snapshot) {
        Boolean gameStarted = snapshot.child("gameStarted").getValue(Boolean.class);
        Boolean gameEnded = snapshot.child("gameEnded").getValue(Boolean.class);
        Long holder = snapshot.child("holder").getValue(Long.class);
        Long remainingTime = snapshot.child("remainingTime").getValue(Long.class);
        String loserName = snapshot.child("loserName").getValue(String.class);
        
        if (Boolean.TRUE.equals(gameStarted) && listener != null) {
            listener.onGameStarted();
        }
        
        if (Boolean.TRUE.equals(gameEnded) && loserName != null && listener != null) {
            listener.onBurn(loserName);
        }
        
        if (holder != null && listener != null) {
            String[] players = new String[playerNames.size()];
            for (int i = 0; i < playerNames.size(); i++) {
                players[i] = playerNames.get(i);
            }
            listener.onGameStateChanged(holder.intValue(), players);
        }
        
        if (remainingTime != null && remainingTime > 0 && listener != null) {
            listener.onTick(remainingTime);
        }
    }
    
    private void handleMessage(DataSnapshot snapshot) {
        String type = snapshot.child("type").getValue(String.class);
        
        if ("pass".equals(type)) {
            Long fromPlayer = snapshot.child("from").getValue(Long.class);
            Long toPlayer = snapshot.child("to").getValue(Long.class);
            
            if (fromPlayer != null && toPlayer != null && listener != null) {
                listener.onPassReceived(fromPlayer.intValue(), toPlayer.intValue());
            }
        }
    }
    
    // Send a pass request
    public void sendPass(int toPlayerIndex) {
        if (messagesRef == null) return;
        
        Map<String, Object> message = new HashMap<>();
        message.put("type", "pass");
        message.put("from", myPlayerIndex);
        message.put("to", toPlayerIndex);
        message.put("timestamp", System.currentTimeMillis());
        message.put("playerId", playerId);
        
        messagesRef.push().setValue(message);
    }
    
    // Host starts the game
    public void startGame() {
        if (!isHost || gameStateRef == null) return;
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("gameStarted", true);
        updates.put("status", "playing");
        
        gameStateRef.updateChildren(updates);
        roomRef.child("status").setValue("playing");
    }
    
    // Host updates game state
    public void updateGameState(int holder, long remainingTime) {
        if (!isHost || gameStateRef == null) return;
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("holder", holder);
        updates.put("remainingTime", remainingTime);
        
        gameStateRef.updateChildren(updates);
    }
    
    // Host ends the game
    public void endGame(String loserName) {
        if (!isHost || gameStateRef == null) return;
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("gameEnded", true);
        updates.put("loserName", loserName);
        updates.put("remainingTime", 0);
        
        gameStateRef.updateChildren(updates);
        roomRef.child("status").setValue("finished");
    }
    
    // Leave the room
    public void leaveRoom() {
        if (currentRoomId == null) return;
        
        // Mark player as disconnected
        if (playersRef != null && myPlayerIndex >= 0) {
            playersRef.child(String.valueOf(myPlayerIndex)).child("connected").setValue(false);
        }
        
        // Remove listeners
        removeListeners();
        
        // If host is leaving, transfer host or close room
        if (isHost) {
            // For simplicity, just mark room as closed
            if (roomRef != null) {
                roomRef.child("status").setValue("closed");
            }
        }
        
        currentRoomId = null;
        myPlayerIndex = -1;
        isHost = false;
        playerNames.clear();
    }
    
    private void removeListeners() {
        if (playersListener != null && playersRef != null) {
            playersRef.removeEventListener(playersListener);
            playersListener = null;
        }
        
        if (gameStateListener != null && gameStateRef != null) {
            gameStateRef.removeEventListener(gameStateListener);
            gameStateListener = null;
        }
        
        if (messagesListener != null && messagesRef != null) {
            messagesRef.removeEventListener(messagesListener);
            messagesListener = null;
        }
    }
    
    private String generateRoomId() {
        // Generate a 6-character room code
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }
    
    public void disconnect() {
        leaveRoom();
        if (listener != null) {
            listener.onDisconnected();
        }
    }
}