package com.tatoalu.hotpotato;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unified Multiplayer Manager for Hot Potato game
 * Integrates Firebase Realtime Database, NSD, and Wi-Fi Direct networking
 * Provides automatic fallback and connection mode selection
 */
public class UnifiedMultiplayerManager {
    private static final String TAG = "UnifiedMultiplayer";
    
    // Connection modes in priority order
    public enum ConnectionMode {
        AUTO_FALLBACK,    // Try LAN first, then Wi-Fi Direct, then Firebase
        LAN_ONLY,         // Network Service Discovery only
        WIFI_DIRECT_ONLY, // Wi-Fi Direct P2P only
        FIREBASE_ONLY,    // Firebase Realtime Database only
        HYBRID           // Use multiple modes simultaneously
    }
    
    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }
    
    public enum NetworkType {
        NONE,
        LAN_NSD,
        WIFI_DIRECT,
        FIREBASE,
        HYBRID
    }
    
    private final Context context;
    private ConnectionMode connectionMode = ConnectionMode.AUTO_FALLBACK;
    private ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private NetworkType activeNetworkType = NetworkType.NONE;
    
    // Networking components
    private FirebaseRealtimeMultiplayer firebaseMultiplayer;
    private NsdHelper nsdHelper;
    private WiFiDirectHelper wifiDirectHelper;
    private LanServer lanServer;
    private LanClient lanClient;
    
    // Configuration
    private String playerName;
    private boolean isHost = false;
    private int serverPort = 8888;
    private String roomCode;
    
    // Listeners
    private UnifiedMultiplayerListener multiplayerListener;
    private GameEventListener gameEventListener;
    
    public interface UnifiedMultiplayerListener {
        void onConnectionStateChanged(ConnectionState state, NetworkType networkType);
        void onRoomCreated(String roomCode, NetworkType networkType);
        void onRoomJoined(String roomCode, NetworkType networkType);
        void onPlayerJoined(String playerId, String playerName, NetworkType networkType);
        void onPlayerLeft(String playerId, String playerName, NetworkType networkType);
        void onHostChanged(String newHostId, NetworkType networkType);
        void onNetworkError(String error, NetworkType networkType);
        void onFallbackActivated(NetworkType fromType, NetworkType toType);
    }
    
    public interface GameEventListener {
        void onGameStateChanged(FirebaseRealtimeMultiplayer.GameState state, Map<String, Object> data);
        void onGameTick(long remainingMs);
        void onPotatoPassed(String fromPlayer, String toPlayer);
        void onGameOver(String loserPlayerId, String loserName);
    }
    
    public UnifiedMultiplayerManager(Context context) {
        this.context = context.getApplicationContext();
        initializeComponents();
    }
    
    public void setMultiplayerListener(UnifiedMultiplayerListener listener) {
        this.multiplayerListener = listener;
    }
    
    public void setGameEventListener(GameEventListener listener) {
        this.gameEventListener = listener;
    }
    
    public void setConnectionMode(ConnectionMode mode) {
        this.connectionMode = mode;
        Log.d(TAG, "Connection mode set to: " + mode);
    }
    
    public void setPlayerName(String name) {
        this.playerName = name;
        if (firebaseMultiplayer != null) {
            firebaseMultiplayer.setPlayerName(name);
        }
    }
    
    public void setServerPort(int port) {
        this.serverPort = port;
    }
    
    private void initializeComponents() {
        // Initialize Firebase multiplayer
        firebaseMultiplayer = new FirebaseRealtimeMultiplayer();
        setupFirebaseListeners();
        
        // Initialize NSD helper
        nsdHelper = new NsdHelper(context);
        setupNsdListeners();
        
        // Initialize Wi-Fi Direct helper
        wifiDirectHelper = new WiFiDirectHelper(context);
        setupWiFiDirectListeners();
        
        Log.d(TAG, "Unified multiplayer manager initialized");
    }
    
    /**
     * Create a new multiplayer room as host
     */
    public void createRoom(int maxPlayers) {
        if (playerName == null || playerName.trim().isEmpty()) {
            notifyError("Player name is required", NetworkType.NONE);
            return;
        }
        
        isHost = true;
        connectionState = ConnectionState.CONNECTING;
        
        switch (connectionMode) {
            case LAN_ONLY:
                createLanRoom(maxPlayers);
                break;
            case WIFI_DIRECT_ONLY:
                createWiFiDirectRoom();
                break;
            case FIREBASE_ONLY:
                createFirebaseRoom(maxPlayers);
                break;
            case AUTO_FALLBACK:
                // Try LAN first
                createLanRoom(maxPlayers);
                break;
            case HYBRID:
                // Create on multiple networks
                createHybridRoom(maxPlayers);
                break;
        }
    }
    
    /**
     * Join an existing multiplayer room
     */
    public void joinRoom(String roomCode) {
        if (playerName == null || playerName.trim().isEmpty()) {
            notifyError("Player name is required", NetworkType.NONE);
            return;
        }
        
        this.roomCode = roomCode;
        isHost = false;
        connectionState = ConnectionState.CONNECTING;
        
        switch (connectionMode) {
            case LAN_ONLY:
                joinLanRoom(roomCode);
                break;
            case WIFI_DIRECT_ONLY:
                joinWiFiDirectRoom();
                break;
            case FIREBASE_ONLY:
                joinFirebaseRoom(roomCode);
                break;
            case AUTO_FALLBACK:
                // Try to discover room on LAN first
                discoverAndJoinRoom(roomCode);
                break;
            case HYBRID:
                // Try to join on multiple networks
                joinHybridRoom(roomCode);
                break;
        }
    }
    
    /**
     * Start the game (host only)
     */
    public void startGame() {
        if (!isHost) {
            Log.w(TAG, "Only host can start the game");
            return;
        }
        
        switch (activeNetworkType) {
            case FIREBASE:
                if (firebaseMultiplayer != null) {
                    firebaseMultiplayer.startGame();
                }
                break;
            case LAN_NSD:
                if (lanServer != null) {
                    lanServer.broadcastGameStarted();
                }
                break;
            case WIFI_DIRECT:
                // Implement Wi-Fi Direct game start
                startWiFiDirectGame();
                break;
            case HYBRID:
                // Start on all active networks
                startHybridGame();
                break;
        }
    }
    
    /**
     * Pass the potato to another player
     */
    public void passPotatoTo(String targetPlayerId) {
        switch (activeNetworkType) {
            case FIREBASE:
                if (firebaseMultiplayer != null) {
                    firebaseMultiplayer.passPotatoTo(targetPlayerId);
                }
                break;
            case LAN_NSD:
                if (lanClient != null) {
                    try {
                        int targetId = Integer.parseInt(targetPlayerId);
                        lanClient.sendPassRequest(targetId);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid target player ID: " + targetPlayerId);
                    }
                } else if (lanServer != null) {
                    // Server doesn't directly pass - it handles pass requests from clients
                    Log.d(TAG, "Server received pass request for: " + targetPlayerId);
                }
                break;
            case WIFI_DIRECT:
                // Implement Wi-Fi Direct potato passing
                passPotatoWiFiDirect(targetPlayerId);
                break;
            case HYBRID:
                // Pass on all active networks
                passPotatoHybrid(targetPlayerId);
                break;
        }
    }
    
    /**
     * Broadcast game tick to all connected players
     */
    public void broadcastGameTick(long remainingMs) {
        if (gameEventListener != null) {
            gameEventListener.onGameTick(remainingMs);
        }
        
        switch (activeNetworkType) {
            case FIREBASE:
                // Firebase handles ticks through real-time listeners
                break;
            case LAN_NSD:
                if (lanServer != null) {
                    lanServer.broadcastTick(remainingMs);
                }
                break;
            case WIFI_DIRECT:
                // Implement Wi-Fi Direct tick broadcast
                broadcastWiFiDirectTick(remainingMs);
                break;
            case HYBRID:
                // Broadcast on all active networks
                broadcastHybridTick(remainingMs);
                break;
        }
    }
    
    /**
     * Broadcast game over to all connected players
     */
    public void broadcastGameOver(String loserName) {
        if (gameEventListener != null) {
            gameEventListener.onGameOver("", loserName);
        }
        
        switch (activeNetworkType) {
            case FIREBASE:
                if (firebaseMultiplayer != null) {
                    firebaseMultiplayer.endGame(loserName);
                }
                break;
            case LAN_NSD:
                if (lanServer != null) {
                    lanServer.broadcastBurn(loserName);
                }
                break;
            case WIFI_DIRECT:
                // Implement Wi-Fi Direct game over broadcast
                broadcastWiFiDirectGameOver(loserName);
                break;
            case HYBRID:
                // Broadcast on all active networks
                broadcastHybridGameOver(loserName);
                break;
        }
    }
    
    /**
     * Leave the current room
     */
    public void leaveRoom() {
        Log.d(TAG, "Leaving room");
        
        // Clean up based on active network type
        switch (activeNetworkType) {
            case FIREBASE:
                if (firebaseMultiplayer != null) {
                    firebaseMultiplayer.leaveRoom();
                }
                break;
            case LAN_NSD:
                if (lanServer != null) {
                    lanServer.stop();
                    lanServer = null;
                }
                if (lanClient != null) {
                    try {
                        lanClient.disconnect();
                    } catch (Exception e) {
                        // Safely handle any exceptions during disconnect
                        Log.e("UnifiedMultiplayerManager", "Error disconnecting LAN client: " + e.getMessage());
                    }
                    lanClient = null;
                }
                if (nsdHelper != null) {
                    nsdHelper.unregisterService();
                    nsdHelper.stopDiscovery();
                }
                break;
            case WIFI_DIRECT:
                if (wifiDirectHelper != null) {
                    wifiDirectHelper.disconnect();
                }
                break;
            case HYBRID:
                // Clean up all networks
                leaveAllNetworks();
                break;
        }
        
        // Reset state
        connectionState = ConnectionState.DISCONNECTED;
        activeNetworkType = NetworkType.NONE;
        isHost = false;
        roomCode = null;
        
        notifyConnectionStateChanged();
    }
    
    private void createLanRoom(int maxPlayers) {
        try {
            // Start LAN server
            lanServer = new LanServer(serverPort);
            // Note: Max players limit is handled by Config.MAX_PLAYERS in LanServer
            
            // Register NSD service
            nsdHelper.registerService(serverPort, playerName);
            
            activeNetworkType = NetworkType.LAN_NSD;
            connectionState = ConnectionState.CONNECTED;
            
            // Generate room code based on service name
            roomCode = nsdHelper.getServiceName();
            
            notifyRoomCreated(roomCode, NetworkType.LAN_NSD);
            notifyConnectionStateChanged();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create LAN room", e);
            if (connectionMode == ConnectionMode.AUTO_FALLBACK) {
                // Fallback to Wi-Fi Direct
                createWiFiDirectRoom();
            } else {
                notifyError("Failed to create LAN room: " + e.getMessage(), NetworkType.LAN_NSD);
            }
        }
    }
    
    private void createWiFiDirectRoom() {
        wifiDirectHelper.registerReceiver();
        wifiDirectHelper.createGroup();
        
        activeNetworkType = NetworkType.WIFI_DIRECT;
        // Connection state will be updated by Wi-Fi Direct callbacks
    }
    
    private void createFirebaseRoom(int maxPlayers) {
        firebaseMultiplayer.createRoom(maxPlayers);
        activeNetworkType = NetworkType.FIREBASE;
        // Connection state will be updated by Firebase callbacks
    }
    
    private void createHybridRoom(int maxPlayers) {
        // Create on multiple networks simultaneously
        createLanRoom(maxPlayers);
        createFirebaseRoom(maxPlayers);
        activeNetworkType = NetworkType.HYBRID;
    }
    
    private void joinLanRoom(String roomCode) {
        // Start NSD discovery to find the room
        nsdHelper.discoverServices();
        // Connection will be established in NSD callbacks
    }
    
    private void joinWiFiDirectRoom() {
        wifiDirectHelper.registerReceiver();
        wifiDirectHelper.startPeerDiscovery();
        activeNetworkType = NetworkType.WIFI_DIRECT;
    }
    
    private void joinFirebaseRoom(String roomCode) {
        firebaseMultiplayer.joinRoom(roomCode);
        activeNetworkType = NetworkType.FIREBASE;
    }
    
    private void discoverAndJoinRoom(String roomCode) {
        // Try LAN discovery first
        nsdHelper.discoverServices();
        
        // Set a timeout to fallback to other methods
        new android.os.Handler().postDelayed(() -> {
            if (connectionState != ConnectionState.CONNECTED) {
                Log.d(TAG, "LAN discovery timeout, trying Wi-Fi Direct");
                joinWiFiDirectRoom();
            }
        }, 5000); // 5 second timeout
    }
    
    private void joinHybridRoom(String roomCode) {
        // Try to join on multiple networks
        joinLanRoom(roomCode);
        joinFirebaseRoom(roomCode);
        activeNetworkType = NetworkType.HYBRID;
    }
    
    private void startWiFiDirectGame() {
        // Implement Wi-Fi Direct specific game start logic
        Log.d(TAG, "Starting Wi-Fi Direct game");
    }
    
    private void startHybridGame() {
        // Start game on all active networks
        if (firebaseMultiplayer != null) {
            firebaseMultiplayer.startGame();
        }
        if (lanServer != null) {
            lanServer.broadcastGameStarted();
        }
        startWiFiDirectGame();
    }
    
    private void passPotatoWiFiDirect(String targetPlayerId) {
        // Implement Wi-Fi Direct specific potato passing
        Log.d(TAG, "Passing potato via Wi-Fi Direct to: " + targetPlayerId);
    }
    
    private void passPotatoHybrid(String targetPlayerId) {
        // Pass potato on all active networks
        if (firebaseMultiplayer != null) {
            firebaseMultiplayer.passPotatoTo(targetPlayerId);
        }
        if (lanClient != null) {
            try {
                int targetId = Integer.parseInt(targetPlayerId);
                lanClient.sendPassRequest(targetId);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid target player ID: " + targetPlayerId);
            } catch (Exception e) {
                Log.e(TAG, "Error sending pass request: " + e.getMessage());
            }
        } else if (lanServer != null) {
            // Server doesn't directly pass - it handles pass requests from clients
            Log.d(TAG, "Server received pass request for: " + targetPlayerId);
        }
        passPotatoWiFiDirect(targetPlayerId);
    }
    
    private void leaveAllNetworks() {
        if (firebaseMultiplayer != null) {
            firebaseMultiplayer.leaveRoom();
        }
        if (lanServer != null) {
            lanServer.stop();
            lanServer = null;
        }
        if (lanClient != null) {
            lanClient.disconnect();
            lanClient = null;
        }
        if (nsdHelper != null) {
            nsdHelper.unregisterService();
            nsdHelper.stopDiscovery();
        }
        if (wifiDirectHelper != null) {
            wifiDirectHelper.disconnect();
        }
    }
    
    private void setupFirebaseListeners() {
        if (firebaseMultiplayer == null) return;
        
        firebaseMultiplayer.setGameStateListener(new FirebaseRealtimeMultiplayer.GameStateListener() {
            @Override
            public void onGameStateChanged(FirebaseRealtimeMultiplayer.GameState state, Map<String, Object> data) {
                if (gameEventListener != null) {
                    gameEventListener.onGameStateChanged(state, data);
                }
            }
            
            @Override
            public void onPlayerJoined(String playerId, String playerName) {
                if (multiplayerListener != null) {
                    multiplayerListener.onPlayerJoined(playerId, playerName, NetworkType.FIREBASE);
                }
            }
            
            @Override
            public void onPlayerLeft(String playerId, String playerName) {
                if (multiplayerListener != null) {
                    multiplayerListener.onPlayerLeft(playerId, playerName, NetworkType.FIREBASE);
                }
            }
            
            @Override
            public void onGameTick(long remainingMs) {
                if (gameEventListener != null) {
                    gameEventListener.onGameTick(remainingMs);
                }
            }
            
            @Override
            public void onPotatoPassed(String fromPlayer, String toPlayer) {
                if (gameEventListener != null) {
                    gameEventListener.onPotatoPassed(fromPlayer, toPlayer);
                }
            }
            
            @Override
            public void onGameOver(String loserPlayerId, String loserName) {
                if (gameEventListener != null) {
                    gameEventListener.onGameOver(loserPlayerId, loserName);
                }
            }
            
            @Override
            public void onError(String error) {
                notifyError(error, NetworkType.FIREBASE);
            }
        });
        
        firebaseMultiplayer.setRoomListener(new FirebaseRealtimeMultiplayer.RoomListener() {
            @Override
            public void onRoomCreated(String roomId) {
                roomCode = roomId;
                connectionState = ConnectionState.CONNECTED;
                notifyRoomCreated(roomId, NetworkType.FIREBASE);
                notifyConnectionStateChanged();
            }
            
            @Override
            public void onRoomJoined(String roomId) {
                roomCode = roomId;
                connectionState = ConnectionState.CONNECTED;
                notifyRoomJoined(roomId, NetworkType.FIREBASE);
                notifyConnectionStateChanged();
            }
            
            @Override
            public void onRoomNotFound() {
                if (connectionMode == ConnectionMode.AUTO_FALLBACK) {
                    // Try Wi-Fi Direct
                    joinWiFiDirectRoom();
                } else {
                    notifyError("Room not found", NetworkType.FIREBASE);
                }
            }
            
            @Override
            public void onRoomFull() {
                notifyError("Room is full", NetworkType.FIREBASE);
            }
            
            @Override
            public void onHostLeft() {
                if (multiplayerListener != null) {
                    multiplayerListener.onHostChanged("", NetworkType.FIREBASE);
                }
            }
        });
    }
    
    private void setupNsdListeners() {
        if (nsdHelper == null) return;
        
        nsdHelper.setServiceDiscoveryListener(new NsdHelper.ServiceDiscoveryListener() {
            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "NSD service found: " + serviceInfo.getServiceName());
            }
            
            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "NSD service lost: " + serviceInfo.getServiceName());
            }
            
            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "NSD service resolved: " + serviceInfo.getServiceName());
                
                // Check if this is the room we're looking for
                if (roomCode != null && serviceInfo.getServiceName().contains(roomCode)) {
                    // Connect to this service
                    connectToLanService(serviceInfo);
                }
            }
            
            @Override
            public void onDiscoveryStarted() {
                Log.d(TAG, "NSD discovery started");
            }
            
            @Override
            public void onDiscoveryStopped() {
                Log.d(TAG, "NSD discovery stopped");
            }
            
            @Override
            public void onDiscoveryFailed(int errorCode) {
                Log.e(TAG, "NSD discovery failed: " + errorCode);
                if (connectionMode == ConnectionMode.AUTO_FALLBACK) {
                    // Fallback to Firebase
                    joinFirebaseRoom(roomCode);
                }
            }
        });
        
        nsdHelper.setServiceRegistrationListener(new NsdHelper.ServiceRegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "NSD service registered: " + serviceInfo.getServiceName());
            }
            
            @Override
            public void onServiceUnregistered() {
                Log.d(TAG, "NSD service unregistered");
            }
            
            @Override
            public void onRegistrationFailed(int errorCode) {
                Log.e(TAG, "NSD registration failed: " + errorCode);
                notifyError("Failed to register service", NetworkType.LAN_NSD);
            }
            
            @Override
            public void onUnregistrationFailed(int errorCode) {
                Log.e(TAG, "NSD unregistration failed: " + errorCode);
            }
        });
    }
    
    private void setupWiFiDirectListeners() {
        if (wifiDirectHelper == null) return;
        
        wifiDirectHelper.setConnectionListener(new WiFiDirectHelper.ConnectionListener() {
            @Override
            public void onConnectionEstablished(WifiP2pInfo info) {
                Log.d(TAG, "Wi-Fi Direct connection established");
                connectionState = ConnectionState.CONNECTED;
                activeNetworkType = NetworkType.WIFI_DIRECT;
                
                // Generate room code for Wi-Fi Direct
                roomCode = "WFD_" + System.currentTimeMillis();
                
                if (info.isGroupOwner) {
                    notifyRoomCreated(roomCode, NetworkType.WIFI_DIRECT);
                } else {
                    notifyRoomJoined(roomCode, NetworkType.WIFI_DIRECT);
                }
                
                notifyConnectionStateChanged();
            }
            
            @Override
            public void onConnectionLost() {
                Log.d(TAG, "Wi-Fi Direct connection lost");
                connectionState = ConnectionState.DISCONNECTED;
                notifyConnectionStateChanged();
            }
            
            @Override
            public void onConnectionFailed(String reason) {
                Log.e(TAG, "Wi-Fi Direct connection failed: " + reason);
                if (connectionMode == ConnectionMode.AUTO_FALLBACK) {
                    // Fallback to Firebase
                    if (isHost) {
                        createFirebaseRoom(4); // Default max players
                    } else {
                        joinFirebaseRoom(roomCode);
                    }
                } else {
                    notifyError("Wi-Fi Direct connection failed: " + reason, NetworkType.WIFI_DIRECT);
                }
            }
            
            @Override
            public void onGroupFormed(android.net.wifi.p2p.WifiP2pGroup group) {
                Log.d(TAG, "Wi-Fi Direct group formed");
            }
            
            @Override
            public void onGroupRemoved() {
                Log.d(TAG, "Wi-Fi Direct group removed");
            }
        });
    }
    
    private void connectToLanService(NsdServiceInfo serviceInfo) {
        try {
            String host = serviceInfo.getHost().getHostAddress();
            int port = serviceInfo.getPort();
            
            lanClient = new LanClient(host, port, playerName);
            lanClient.connect();
            
            connectionState = ConnectionState.CONNECTED;
            activeNetworkType = NetworkType.LAN_NSD;
            
            notifyRoomJoined(serviceInfo.getServiceName(), NetworkType.LAN_NSD);
            notifyConnectionStateChanged();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to LAN service", e);
            if (connectionMode == ConnectionMode.AUTO_FALLBACK) {
                // Fallback to Firebase
                joinFirebaseRoom(roomCode);
            } else {
                notifyError("Failed to connect to LAN service: " + e.getMessage(), NetworkType.LAN_NSD);
            }
        }
    }
    
    private void notifyConnectionStateChanged() {
        if (multiplayerListener != null) {
            multiplayerListener.onConnectionStateChanged(connectionState, activeNetworkType);
        }
    }
    
    private void notifyRoomCreated(String roomCode, NetworkType networkType) {
        if (multiplayerListener != null) {
            multiplayerListener.onRoomCreated(roomCode, networkType);
        }
    }
    
    private void notifyRoomJoined(String roomCode, NetworkType networkType) {
        if (multiplayerListener != null) {
            multiplayerListener.onRoomJoined(roomCode, networkType);
        }
    }
    
    private void notifyError(String error, NetworkType networkType) {
        Log.e(TAG, "Error on " + networkType + ": " + error);
        connectionState = ConnectionState.ERROR;
        if (multiplayerListener != null) {
            multiplayerListener.onNetworkError(error, networkType);
        }
    }
    
    // Getters
    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }
    
    public ConnectionState getConnectionState() {
        return connectionState;
    }
    
    public NetworkType getActiveNetworkType() {
        return activeNetworkType;
    }
    
    public String getRoomCode() {
        return roomCode;
    }
    
    public boolean isHost() {
        return isHost;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    // Helper methods for Wi-Fi Direct and Hybrid broadcasting
    private void broadcastWiFiDirectTick(long remainingMs) {
        // TODO: Implement Wi-Fi Direct tick broadcasting
        Log.d(TAG, "Wi-Fi Direct tick broadcast: " + remainingMs + "ms");
    }
    
    private void broadcastHybridTick(long remainingMs) {
        // Broadcast on all active networks
        if (lanServer != null) {
            lanServer.broadcastTick(remainingMs);
        }
        // Firebase handles ticks through real-time listeners
        broadcastWiFiDirectTick(remainingMs);
    }
    
    private void broadcastWiFiDirectGameOver(String loserName) {
        // TODO: Implement Wi-Fi Direct game over broadcasting
        Log.d(TAG, "Wi-Fi Direct game over broadcast: " + loserName);
    }
    
    private void broadcastHybridGameOver(String loserName) {
        // Broadcast on all active networks
        if (firebaseMultiplayer != null) {
            firebaseMultiplayer.endGame(loserName);
        }
        if (lanServer != null) {
            lanServer.broadcastBurn(loserName);
        }
        broadcastWiFiDirectGameOver(loserName);
    }
    
    /**
     * Clean up all resources
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up unified multiplayer manager");
        
        leaveRoom();
        
        if (firebaseMultiplayer != null) {
            firebaseMultiplayer.cleanup();
        }
        
        if (nsdHelper != null) {
            nsdHelper.tearDown();
        }
        
        if (wifiDirectHelper != null) {
            wifiDirectHelper.cleanup();
        }
        
        // Clear listeners
        multiplayerListener = null;
        gameEventListener = null;
    }
}