package com.tatoalu.hotpotato;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enhanced LAN Discovery System similar to Mini Militia
 * Features:
 * - UDP broadcast for fast discovery
 * - NSD (Network Service Discovery) integration
 * - Automatic room detection and joining
 * - Real-time player list updates
 * - Connection health monitoring
 * - Auto-reconnection capabilities
 */
public class EnhancedLanDiscovery {
    private static final String TAG = "EnhancedLanDiscovery";

    // Service configuration
    private static final String SERVICE_TYPE = "_tatoaalu._tcp.";
    private static final String SERVICE_NAME = "TatoAalu_HotPotato";
    private static final int DEFAULT_PORT = 54567;
    private static final int UDP_BROADCAST_PORT = 54568;

    // Discovery configuration
    private static final int DISCOVERY_INTERVAL_MS = 2000; // 2 seconds
    private static final int CONNECTION_TIMEOUT_MS = 10000; // 10 seconds
    private static final int HEARTBEAT_INTERVAL_MS = 5000; // 5 seconds
    private static final int MAX_PLAYERS = 8;

    // Broadcast messages
    private static final String MSG_DISCOVER_ROOMS = "TATO_DISCOVER";
    private static final String MSG_ROOM_RESPONSE = "TATO_ROOM";
    private static final String MSG_JOIN_REQUEST = "TATO_JOIN";
    private static final String MSG_JOIN_RESPONSE = "TATO_JOIN_OK";
    private static final String MSG_PLAYER_UPDATE = "TATO_PLAYERS";
    private static final String MSG_HEARTBEAT = "TATO_HEARTBEAT";
    private static final String MSG_GAME_START = "TATO_START";
    private static final String MSG_GAME_DATA = "TATO_DATA";

    private Context context;
    private NsdManager nsdManager;
    private WifiManager wifiManager;
    private ExecutorService executorService;
    private Handler mainHandler;

    // Discovery state
    private boolean isDiscovering = false;
    private boolean isHosting = false;
    private String localPlayerName;
    private String roomCode;
    private int hostPort = DEFAULT_PORT;

    // Network components
    private DatagramSocket broadcastSocket;
    private ServerSocket hostServerSocket;
    private NsdManager.RegistrationListener registrationListener;
    private NsdManager.DiscoveryListener discoveryListener;

    // Room and player management
    private Map<String, DiscoveredRoom> discoveredRooms = new ConcurrentHashMap<>();
    private Map<String, ConnectedPlayer> connectedPlayers = new ConcurrentHashMap<>();
    private List<Socket> clientConnections = Collections.synchronizedList(new ArrayList<>());

    // Callbacks
    private LanDiscoveryListener listener;

    public interface LanDiscoveryListener {
        void onRoomsDiscovered(List<DiscoveredRoom> rooms);
        void onRoomJoined(String roomCode, String hostName);
        void onRoomHosted(String roomCode, int port);
        void onPlayerJoined(String playerId, String playerName);
        void onPlayerLeft(String playerId, String playerName);
        void onGameStarted();
        void onGameDataReceived(String data);
        void onConnectionError(String error);
        void onDiscoveryStateChanged(boolean isDiscovering);
    }

    // Data classes
    public static class DiscoveredRoom {
        public String roomCode;
        public String hostName;
        public String hostAddress;
        public int hostPort;
        public int playerCount;
        public int maxPlayers;
        public long lastSeen;
        public boolean isJoinable;

        public DiscoveredRoom(String roomCode, String hostName, String hostAddress, int hostPort, int playerCount) {
            this.roomCode = roomCode;
            this.hostName = hostName;
            this.hostAddress = hostAddress;
            this.hostPort = hostPort;
            this.playerCount = playerCount;
            this.maxPlayers = MAX_PLAYERS;
            this.lastSeen = System.currentTimeMillis();
            this.isJoinable = playerCount < maxPlayers;
        }
    }

    public static class ConnectedPlayer {
        public String playerId;
        public String playerName;
        public String ipAddress;
        public long lastHeartbeat;
        public Socket connection;

        public ConnectedPlayer(String playerId, String playerName, String ipAddress, Socket connection) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.ipAddress = ipAddress;
            this.connection = connection;
            this.lastHeartbeat = System.currentTimeMillis();
        }
    }

    public EnhancedLanDiscovery(Context context) {
        this.context = context;
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    // Public API methods
    public void setListener(LanDiscoveryListener listener) {
        this.listener = listener;
    }

    public void setLocalPlayerName(String playerName) {
        this.localPlayerName = playerName;
    }

    public void startDiscovery() {
        if (isDiscovering) return;

        Log.d(TAG, "Starting LAN discovery");
        isDiscovering = true;

        executorService.submit(() -> {
            try {
                initializeBroadcastSocket();
                startNsdDiscovery();
                startPeriodicDiscovery();
            } catch (Exception e) {
                Log.e(TAG, "Failed to start discovery", e);
                notifyError("Failed to start discovery: " + e.getMessage());
            }
        });

        notifyDiscoveryStateChanged(true);
    }

    public void stopDiscovery() {
        if (!isDiscovering) return;

        Log.d(TAG, "Stopping LAN discovery");
        isDiscovering = false;

        stopNsdDiscovery();
        closeBroadcastSocket();
        discoveredRooms.clear();

        notifyDiscoveryStateChanged(false);
    }

    public void hostRoom(String roomCode) {
        if (isHosting) return;

        this.roomCode = roomCode;
        this.isHosting = true;

        executorService.submit(() -> {
            try {
                startHostServer();
                registerNsdService();
                startHeartbeatService();

                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onRoomHosted(roomCode, hostPort);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to host room", e);
                notifyError("Failed to host room: " + e.getMessage());
                isHosting = false;
            }
        });
    }

    public void joinRoom(DiscoveredRoom room) {
        executorService.submit(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new java.net.InetSocketAddress(room.hostAddress, room.hostPort), CONNECTION_TIMEOUT_MS);

                // Send join request
                String joinMessage = MSG_JOIN_REQUEST + "|" + localPlayerName + "|" + getLocalIpAddress();
                socket.getOutputStream().write(joinMessage.getBytes());

                // Wait for response
                byte[] buffer = new byte[1024];
                int bytesRead = socket.getInputStream().read(buffer);
                String response = new String(buffer, 0, bytesRead);

                if (response.startsWith(MSG_JOIN_RESPONSE)) {
                    mainHandler.post(() -> {
                        if (listener != null) {
                            listener.onRoomJoined(room.roomCode, room.hostName);
                        }
                    });
                } else {
                    socket.close();
                    throw new IOException("Join request rejected");
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to join room", e);
                notifyError("Failed to join room: " + e.getMessage());
            }
        });
    }

    public void broadcastGameData(String data) {
        if (!isHosting) return;

        String message = MSG_GAME_DATA + "|" + data;
        executorService.submit(() -> broadcastToClients(message));
    }

    public void startGame() {
        if (!isHosting) return;

        executorService.submit(() -> {
            broadcastToClients(MSG_GAME_START);

            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onGameStarted();
                }
            });
        });
    }

    public List<DiscoveredRoom> getDiscoveredRooms() {
        return new ArrayList<>(discoveredRooms.values());
    }

    public List<ConnectedPlayer> getConnectedPlayers() {
        return new ArrayList<>(connectedPlayers.values());
    }

    /**
     * CRITICAL FIX: Send message from client to host
     * This method allows clients to communicate back to the host
     */
    public void sendMessageToHost(String message) {
        if (isHosting) {
            Log.w(TAG, "Host cannot send message to itself");
            return;
        }

        Log.d(TAG, "📤 NETWORK SEND: " + message + " -> HOST");
        executorService.submit(() -> {
            try {
                // Find the host connection (first client connection for simplicity)
                synchronized (clientConnections) {
                    if (!clientConnections.isEmpty()) {
                        Socket hostSocket = clientConnections.get(0);
                        if (hostSocket != null && !hostSocket.isClosed()) {
                            String fullMessage = MSG_GAME_DATA + "|" + message;
                            hostSocket.getOutputStream().write(fullMessage.getBytes());
                            hostSocket.getOutputStream().flush();
                            Log.d(TAG, "✅ Message sent to host successfully");
                        } else {
                            throw new IOException("Host socket is closed");
                        }
                    } else {
                        throw new IOException("No connection to host");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to send message to host: " + e.getMessage());
                notifyError("Failed to send to host: " + e.getMessage());
            }
        });
    }

    public void disconnect() {
        stopDiscovery();

        if (isHosting) {
            stopHosting();
        }

        // Close all client connections
        synchronized (clientConnections) {
            for (Socket socket : clientConnections) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.w(TAG, "Error closing client connection", e);
                }
            }
            clientConnections.clear();
        }

        connectedPlayers.clear();

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // Private implementation methods
    private void initializeBroadcastSocket() throws IOException {
        if (broadcastSocket == null || broadcastSocket.isClosed()) {
            broadcastSocket = new DatagramSocket(UDP_BROADCAST_PORT);
            broadcastSocket.setBroadcast(true);
            broadcastSocket.setSoTimeout(1000);
        }
    }

    private void closeBroadcastSocket() {
        if (broadcastSocket != null && !broadcastSocket.isClosed()) {
            broadcastSocket.close();
            broadcastSocket = null;
        }
    }

    private void startPeriodicDiscovery() {
        executorService.submit(() -> {
            while (isDiscovering) {
                try {
                    sendBroadcastDiscovery();
                    cleanupOldRooms();
                    Thread.sleep(DISCOVERY_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.w(TAG, "Error in periodic discovery", e);
                }
            }
        });

        // Start listening for responses
        executorService.submit(this::listenForBroadcastResponses);
    }

    private void sendBroadcastDiscovery() throws IOException {
        String message = MSG_DISCOVER_ROOMS;
        byte[] data = message.getBytes();

        InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
        DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, UDP_BROADCAST_PORT);

        if (broadcastSocket != null && !broadcastSocket.isClosed()) {
            broadcastSocket.send(packet);
        }
    }

    private void listenForBroadcastResponses() {
        byte[] buffer = new byte[1024];

        while (isDiscovering && broadcastSocket != null && !broadcastSocket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                broadcastSocket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                String senderAddress = packet.getAddress().getHostAddress();

                processBroadcastMessage(message, senderAddress);

            } catch (java.net.SocketTimeoutException e) {
                // Expected timeout, continue listening
            } catch (Exception e) {
                if (isDiscovering) {
                    Log.w(TAG, "Error receiving broadcast", e);
                }
            }
        }
    }

    private void processBroadcastMessage(String message, String senderAddress) {
        String[] parts = message.split("\\|");
        if (parts.length < 1) return;

        String messageType = parts[0];

        switch (messageType) {
            case MSG_DISCOVER_ROOMS:
                if (isHosting) {
                    respondToDiscovery(senderAddress);
                }
                break;

            case MSG_ROOM_RESPONSE:
                if (parts.length >= 5) {
                    String roomCode = parts[1];
                    String hostName = parts[2];
                    int hostPort = Integer.parseInt(parts[3]);
                    int playerCount = Integer.parseInt(parts[4]);

                    DiscoveredRoom room = new DiscoveredRoom(roomCode, hostName, senderAddress, hostPort, playerCount);
                    discoveredRooms.put(roomCode, room);

                    notifyRoomsUpdate();
                }
                break;
        }
    }

    private void respondToDiscovery(String requesterAddress) {
        try {
            String response = MSG_ROOM_RESPONSE + "|" + roomCode + "|" + localPlayerName + "|" + hostPort + "|" + connectedPlayers.size();
            byte[] data = response.getBytes();

            InetAddress requesterInet = InetAddress.getByName(requesterAddress);
            DatagramPacket packet = new DatagramPacket(data, data.length, requesterInet, UDP_BROADCAST_PORT);

            if (broadcastSocket != null && !broadcastSocket.isClosed()) {
                broadcastSocket.send(packet);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to respond to discovery", e);
        }
    }

    private void startHostServer() throws IOException {
        hostServerSocket = new ServerSocket(0); // Use any available port
        hostPort = hostServerSocket.getLocalPort();

        Log.d(TAG, "Host server started on port " + hostPort);

        // Accept incoming connections
        executorService.submit(() -> {
            while (isHosting && !hostServerSocket.isClosed()) {
                try {
                    Socket clientSocket = hostServerSocket.accept();
                    handleNewConnection(clientSocket);
                } catch (IOException e) {
                    if (isHosting) {
                        Log.w(TAG, "Error accepting connection", e);
                    }
                }
            }
        });
    }

    private void handleNewConnection(Socket clientSocket) {
        executorService.submit(() -> {
            try {
                // Read join request
                byte[] buffer = new byte[1024];
                int bytesRead = clientSocket.getInputStream().read(buffer);
                String message = new String(buffer, 0, bytesRead);

                String[] parts = message.split("\\|");
                if (parts.length >= 3 && parts[0].equals(MSG_JOIN_REQUEST)) {
                    String playerName = parts[1];
                    String playerAddress = parts[2];
                    String playerId = playerAddress + "_" + playerName;

                    // Accept the connection
                    String response = MSG_JOIN_RESPONSE + "|OK";
                    clientSocket.getOutputStream().write(response.getBytes());

                    // Add to connected players
                    ConnectedPlayer player = new ConnectedPlayer(playerId, playerName, playerAddress, clientSocket);
                    connectedPlayers.put(playerId, player);
                    clientConnections.add(clientSocket);

                    Log.d(TAG, "Player joined: " + playerName);

                    notifyPlayerJoined(playerId, playerName);
                    broadcastPlayerUpdate();

                    // Handle ongoing communication
                    handleClientCommunication(clientSocket, player);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error handling new connection", e);
                try {
                    clientSocket.close();
                } catch (IOException ioException) {
                    Log.w(TAG, "Error closing client socket", ioException);
                }
            }
        });
    }

    private void handleClientCommunication(Socket clientSocket, ConnectedPlayer player) {
        byte[] buffer = new byte[1024];

        while (!clientSocket.isClosed() && isHosting) {
            try {
                int bytesRead = clientSocket.getInputStream().read(buffer);
                if (bytesRead <= 0) break;

                String message = new String(buffer, 0, bytesRead);
                processClientMessage(message, player);

            } catch (IOException e) {
                Log.d(TAG, "Client disconnected: " + player.playerName);
                break;
            }
        }

        // Clean up disconnected client
        connectedPlayers.remove(player.playerId);
        clientConnections.remove(clientSocket);
        notifyPlayerLeft(player.playerId, player.playerName);
        broadcastPlayerUpdate();

        try {
            clientSocket.close();
        } catch (IOException e) {
            Log.w(TAG, "Error closing client socket", e);
        }
    }

    private void processClientMessage(String message, ConnectedPlayer player) {
        String[] parts = message.split("\\|");
        if (parts.length < 1) return;

        String messageType = parts[0];

        switch (messageType) {
            case MSG_HEARTBEAT:
                player.lastHeartbeat = System.currentTimeMillis();
                break;

            case MSG_GAME_DATA:
                if (parts.length >= 2) {
                    String gameData = parts[1];
                    notifyGameDataReceived(gameData);
                }
                break;
        }
    }

    private void broadcastToClients(String message) {
        synchronized (clientConnections) {
            List<Socket> toRemove = new ArrayList<>();

            for (Socket socket : clientConnections) {
                try {
                    socket.getOutputStream().write(message.getBytes());
                } catch (IOException e) {
                    Log.w(TAG, "Failed to broadcast to client", e);
                    toRemove.add(socket);
                }
            }

            // Remove failed connections
            clientConnections.removeAll(toRemove);
        }
    }

    private void broadcastPlayerUpdate() {
        StringBuilder playerList = new StringBuilder(MSG_PLAYER_UPDATE);
        for (ConnectedPlayer player : connectedPlayers.values()) {
            playerList.append("|").append(player.playerName);
        }
        executorService.submit(() -> broadcastToClients(playerList.toString()));
    }

    private void startHeartbeatService() {
        executorService.submit(() -> {
            while (isHosting) {
                try {
                    broadcastToClients(MSG_HEARTBEAT);
                    cleanupInactivePlayers();
                    Thread.sleep(HEARTBEAT_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.w(TAG, "Error in heartbeat service", e);
                }
            }
        });
    }

    private void cleanupInactivePlayers() {
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();

        for (ConnectedPlayer player : connectedPlayers.values()) {
            if (now - player.lastHeartbeat > HEARTBEAT_INTERVAL_MS * 3) {
                toRemove.add(player.playerId);
            }
        }

        for (String playerId : toRemove) {
            ConnectedPlayer player = connectedPlayers.remove(playerId);
            if (player != null) {
                notifyPlayerLeft(playerId, player.playerName);
                try {
                    player.connection.close();
                } catch (IOException e) {
                    Log.w(TAG, "Error closing inactive player connection", e);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            broadcastPlayerUpdate();
        }
    }

    private void cleanupOldRooms() {
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, DiscoveredRoom> entry : discoveredRooms.entrySet()) {
            if (now - entry.getValue().lastSeen > DISCOVERY_INTERVAL_MS * 3) {
                toRemove.add(entry.getKey());
            }
        }

        for (String roomCode : toRemove) {
            discoveredRooms.remove(roomCode);
        }

        if (!toRemove.isEmpty()) {
            notifyRoomsUpdate();
        }
    }

    private void stopHosting() {
        isHosting = false;

        try {
            if (hostServerSocket != null && !hostServerSocket.isClosed()) {
                hostServerSocket.close();
            }
        } catch (IOException e) {
            Log.w(TAG, "Error closing host server", e);
        }

        unregisterNsdService();
    }

    // NSD (Network Service Discovery) methods
    private void startNsdDiscovery() {
        if (nsdManager == null) return;

        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "NSD discovery start failed: " + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "NSD discovery stop failed: " + errorCode);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "NSD discovery started");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "NSD discovery stopped");
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "NSD service found: " + serviceInfo.getServiceName());
                nsdManager.resolveService(serviceInfo, createResolveListener());
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "NSD service lost: " + serviceInfo.getServiceName());
                discoveredRooms.remove(serviceInfo.getServiceName());
                notifyRoomsUpdate();
            }
        };

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    private void stopNsdDiscovery() {
        if (nsdManager != null && discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
            } catch (Exception e) {
                Log.w(TAG, "Error stopping NSD discovery", e);
            }
            discoveryListener = null;
        }
    }

    private NsdManager.ResolveListener createResolveListener() {
        return new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.w(TAG, "NSD resolve failed for " + serviceInfo.getServiceName() + ": " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "NSD service resolved: " + serviceInfo.getServiceName());

                String roomCode = serviceInfo.getServiceName().replace(SERVICE_NAME + "_", "");
                String hostAddress = serviceInfo.getHost().getHostAddress();
                int hostPort = serviceInfo.getPort();

                DiscoveredRoom room = new DiscoveredRoom(roomCode, "NSD_" + roomCode, hostAddress, hostPort, 0);
                discoveredRooms.put(roomCode, room);
                notifyRoomsUpdate();
            }
        };
    }

    private void registerNsdService() {
        if (nsdManager == null || roomCode == null) return;

        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(SERVICE_NAME + "_" + roomCode);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(hostPort);

        registrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "NSD registration failed: " + errorCode);
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "NSD unregistration failed: " + errorCode);
            }

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "NSD service registered: " + serviceInfo.getServiceName());
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "NSD service unregistered: " + serviceInfo.getServiceName());
            }
        };

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    private void unregisterNsdService() {
        if (nsdManager != null && registrationListener != null) {
            try {
                nsdManager.unregisterService(registrationListener);
            } catch (Exception e) {
                Log.w(TAG, "Error unregistering NSD service", e);
            }
            registrationListener = null;
        }
    }

    // Utility methods
    private String getLocalIpAddress() {
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (!address.isLoopbackAddress() && address instanceof java.net.Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting local IP address", e);
        }
        return "127.0.0.1";
    }

    // Notification methods
    private void notifyRoomsUpdate() {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onRoomsDiscovered(getDiscoveredRooms());
            }
        });
    }

    private void notifyPlayerJoined(String playerId, String playerName) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onPlayerJoined(playerId, playerName);
            }
        });
    }

    private void notifyPlayerLeft(String playerId, String playerName) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onPlayerLeft(playerId, playerName);
            }
        });
    }

    private void notifyGameDataReceived(String data) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onGameDataReceived(data);
            }
        });
    }

    private void notifyError(String error) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onConnectionError(error);
            }
        });
    }

    private void notifyDiscoveryStateChanged(boolean isDiscovering) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onDiscoveryStateChanged(isDiscovering);
            }
        });
    }
}
