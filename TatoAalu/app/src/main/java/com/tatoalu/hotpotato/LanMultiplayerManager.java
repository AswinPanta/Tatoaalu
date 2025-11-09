package com.tatoalu.hotpotato;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified LAN-only Multiplayer Manager for Hot Potato
 * Handles only local network connectivity using EnhancedLanDiscovery
 */
public class LanMultiplayerManager {
    private static final String TAG = "LanMultiplayerManager";

    private Context context;
    private EnhancedLanDiscovery lanDiscovery;
    private Handler mainHandler;
    private LanMultiplayerListener listener;

    private boolean isHost = false;
    private boolean isConnected = false;
    private String localPlayerName;
    private String roomCode;
    private boolean transferredFromBrowser = false;

    public interface LanMultiplayerListener {
        void onRoomCreated(String roomCode);
        void onRoomJoined(String roomCode, String hostName);
        void onPlayerJoined(String playerId, String playerName);
        void onPlayerLeft(String playerId, String playerName);
        void onGameStarted();
        void onGameDataReceived(String data);
        void onConnectionError(String error);
        void onDisconnected();
    }

    public LanMultiplayerManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.lanDiscovery = new EnhancedLanDiscovery(context);
        setupLanDiscovery();
    }

    private void setupLanDiscovery() {
        lanDiscovery.setListener(new EnhancedLanDiscovery.LanDiscoveryListener() {
            @Override
            public void onRoomsDiscovered(List<EnhancedLanDiscovery.DiscoveredRoom> rooms) {
                // Room discovery is handled by RoomBrowserActivity
            }

            @Override
            public void onRoomJoined(String roomCode, String hostName) {
                LanMultiplayerManager.this.roomCode = roomCode;
                isConnected = true;
                isHost = false;

                if (listener != null) {
                    listener.onRoomJoined(roomCode, hostName);
                }

                Log.d(TAG, "Successfully joined room: " + roomCode);
            }

            @Override
            public void onRoomHosted(String roomCode, int port) {
                LanMultiplayerManager.this.roomCode = roomCode;
                isConnected = true;
                isHost = true;

                if (listener != null) {
                    listener.onRoomCreated(roomCode);
                }

                Log.d(TAG, "Successfully created room: " + roomCode + " on port: " + port);
            }

            @Override
            public void onPlayerJoined(String playerId, String playerName) {
                if (listener != null) {
                    listener.onPlayerJoined(playerId, playerName);
                }

                Log.d(TAG, "Player joined: " + playerName + " (" + playerId + ")");
            }

            @Override
            public void onPlayerLeft(String playerId, String playerName) {
                if (listener != null) {
                    listener.onPlayerLeft(playerId, playerName);
                }

                Log.d(TAG, "Player left: " + playerName + " (" + playerId + ")");
            }

            @Override
            public void onGameStarted() {
                if (listener != null) {
                    listener.onGameStarted();
                }

                Log.d(TAG, "Game started");
            }

            @Override
            public void onGameDataReceived(String data) {
                Log.d(TAG, "NETWORK RECV: " + data);
                if (listener != null) {
                    listener.onGameDataReceived(data);
                }
            }

            @Override
            public void onConnectionError(String error) {
                isConnected = false;

                if (listener != null) {
                    listener.onConnectionError(error);
                }

                Log.e(TAG, "Connection error: " + error);
            }

            @Override
            public void onDiscoveryStateChanged(boolean isDiscovering) {
                // Discovery state changes handled by RoomBrowserActivity
            }
        });
    }

    // Public API methods
    public void setListener(LanMultiplayerListener listener) {
        this.listener = listener;
    }

    public void setLocalPlayerName(String playerName) {
        this.localPlayerName = playerName;
        if (lanDiscovery != null) {
            lanDiscovery.setLocalPlayerName(playerName);
        }
    }

    public void createRoom(String roomCode) {
        if (isConnected) {
            Log.w(TAG, "Already connected to a room");
            return;
        }

        if (localPlayerName == null || localPlayerName.trim().isEmpty()) {
            if (listener != null) {
                listener.onConnectionError("Player name is required");
            }
            return;
        }

        // If we already have a connection from browser transfer, don't create new room
        if (transferredFromBrowser && isConnected && this.roomCode != null) {
            Log.d(TAG, "Using existing room connection: " + this.roomCode);
            if (listener != null) {
                listener.onRoomCreated(this.roomCode);
            }
            return;
        }

        Log.d(TAG, "Creating room: " + roomCode);
        lanDiscovery.hostRoom(roomCode);
    }

    public void joinRoom(EnhancedLanDiscovery.DiscoveredRoom room) {
        if (isConnected) {
            Log.w(TAG, "Already connected to a room");
            return;
        }

        if (localPlayerName == null || localPlayerName.trim().isEmpty()) {
            if (listener != null) {
                listener.onConnectionError("Player name is required");
            }
            return;
        }

        Log.d(TAG, "Joining room: " + room.roomCode + " hosted by: " + room.hostName);
        lanDiscovery.joinRoom(room);
    }

    public void startGame() {
        if (!isHost) {
            Log.w(TAG, "Only host can start the game");
            return;
        }

        if (!isConnected) {
            Log.w(TAG, "Not connected to any room");
            return;
        }

        Log.d(TAG, "Starting game");
        lanDiscovery.startGame();
    }

    public void transferConnectionState(LanMultiplayerManager sourceManager) {
        if (sourceManager != null) {
            this.isHost = sourceManager.isHost;
            this.isConnected = sourceManager.isConnected;
            this.roomCode = sourceManager.roomCode;
            this.transferredFromBrowser = true;

            Log.d(TAG, "Transferred connection state - isHost: " + isHost + ", roomCode: " + roomCode);

            // Transfer the underlying discovery instance
            if (sourceManager.lanDiscovery != null) {
                this.lanDiscovery = sourceManager.lanDiscovery;
                setupLanDiscovery(); // Re-setup listener for this manager
            }
        }
    }

    public boolean isTransferredFromBrowser() {
        return transferredFromBrowser;
    }

    public boolean isConnectedToRoom() {
        return isConnected && roomCode != null;
    }

    public void broadcastGameData(String data) {
        if (!isHost) {
            Log.w(TAG, "Only host can broadcast game data");
            return;
        }

        if (!isConnected) {
            Log.w(TAG, "Not connected to any room");
            return;
        }

        Log.d(TAG, "NETWORK SEND: " + data + " (broadcast)");
        lanDiscovery.broadcastGameData(data);
    }

    /**
     * CRITICAL FIX: Proper client-to-host communication
     * This method handles both host broadcasting and client-to-host sending
     */
    public void sendGameAction(String action, String data) {
        String message = action + ":" + data;
        Log.d(TAG, "NETWORK SEND: " + message);

        if (isHost) {
            // Host broadcasts to everyone
            broadcastGameData(message);
        } else {
            // Client sends ONLY to the host
            Log.d(TAG, "Client sending action to host: " + message);
            if (lanDiscovery != null) {
                lanDiscovery.sendMessageToHost(message);
            } else {
                Log.e(TAG, "Cannot send action: lanDiscovery is null");
                if (listener != null) {
                    listener.onConnectionError("Cannot send action - not connected");
                }
            }
        }
    }

    public List<EnhancedLanDiscovery.ConnectedPlayer> getConnectedPlayers() {
        if (lanDiscovery != null) {
            return lanDiscovery.getConnectedPlayers();
        }
        return null;
    }

    public List<String> getConnectedPlayerNames() {
        List<String> names = new ArrayList<>();
        List<EnhancedLanDiscovery.ConnectedPlayer> players = getConnectedPlayers();

        if (players != null) {
            for (EnhancedLanDiscovery.ConnectedPlayer player : players) {
                names.add(player.playerName);
            }
        }

        // Always include local player name
        if (localPlayerName != null && !localPlayerName.trim().isEmpty()) {
            boolean hasLocalPlayer = false;
            for (String name : names) {
                if (name.equals(localPlayerName)) {
                    hasLocalPlayer = true;
                    break;
                }
            }
            if (!hasLocalPlayer) {
                names.add(0, localPlayerName); // Add at beginning
            }
        }

        return names;
    }

    public boolean isHost() {
        return isHost;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getLocalPlayerName() {
        return localPlayerName;
    }

    public void cleanup() {
        disconnect();
    }

    public void disconnect() {
        Log.d(TAG, "Disconnecting from room");

        isConnected = false;
        isHost = false;
        roomCode = null;

        if (lanDiscovery != null) {
            lanDiscovery.disconnect();
        }

        if (listener != null) {
            listener.onDisconnected();
        }
    }

    // Hot Potato specific game actions
    public void broadcastMusicStart(int expectedDurationMs) {
        sendGameAction("MUSIC_START", String.valueOf(expectedDurationMs));
    }

    public void broadcastMusicStop() {
        sendGameAction("MUSIC_STOP", "");
    }

    public void broadcastPlayerElimination(String playerName) {
        sendGameAction("PLAYER_ELIMINATED", playerName);
    }

    public void broadcastGameWinner(String winnerName) {
        sendGameAction("GAME_WINNER", winnerName);
    }

    public void broadcastNextRound(String currentPlayerName) {
        sendGameAction("NEXT_ROUND", currentPlayerName);
    }
}
