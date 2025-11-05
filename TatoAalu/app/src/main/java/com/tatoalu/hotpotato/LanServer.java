package com.tatoalu.hotpotato;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// LAN सर्भर - स्थानीय नेटवर्क खेल होस्टिङ (LAN Server - Local Network Game Hosting)
public class LanServer {
    private static final String TAG = Config.TAG_SERVER;
    
    // सर्भर कन्फिगरेसन (Server configuration)
    private final int port;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    
    // क्लाइन्ट व्यवस्थापन (Client management)
    private final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, ClientHandler> clientsByName = new ConcurrentHashMap<>();
    
    // खेल अवस्था (Game state)
    private volatile boolean gameStarted = false;
    private volatile int currentHolder = 0;
    private final List<String> playerNames = new ArrayList<>();
    
    // इभेन्ट श्रोता (Event listener)
    public interface ServerListener {
        void onClientConnected(String clientName); // क्लाइन्ट जडान भयो (Client connected)
        void onClientDisconnected(String clientName); // क्लाइन्ट विच्छेदन भयो (Client disconnected)
        void onGameStateChanged(String state); // खेल अवस्था परिवर्तन (Game state changed)
        void onError(String error); // त्रुटि (Error)
    }
    
    private ServerListener listener;

    // कन्स्ट्रक्टर (Constructor)
    public LanServer(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(Config.SERVER_THREAD_POOL);
    }

    // श्रोता सेट गर्नुहोस् (Set listener)
    public void setServerListener(ServerListener listener) {
        this.listener = listener;
    }

    // सर्भर सुरु गर्नुहोस् (Start server)
    public void start() throws IOException {
        if (running) {
            Log.w(TAG, "सर्भर पहिले नै चलिरहेको छ (Server already running)");
            return;
        }

        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(Config.SOCKET_TIMEOUT_MS);
        running = true;

        Log.i(TAG, "LAN सर्भर पोर्ट " + port + " मा सुरु भयो (LAN Server started on port " + port + ")");

        // क्लाइन्ट जडानहरू स्वीकार गर्नुहोस् (Accept client connections)
        threadPool.execute(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    if (running) {
                        ClientHandler handler = new ClientHandler(clientSocket);
                        clients.add(handler);
                        threadPool.execute(handler);
                        Log.i(TAG, "नयाँ क्लाइन्ट जडान स्वीकार गरियो (New client connection accepted)");
                    }
                } catch (SocketTimeoutException e) {
                    // सामान्य टाइमआउट, जारी राख्नुहोस् (Normal timeout, continue)
                } catch (IOException e) {
                    if (running) {
                        Log.e(TAG, "क्लाइन्ट जडान स्वीकार गर्न त्रुटि (Error accepting client connection)", e);
                        notifyError("क्लाइन्ट जडान त्रुटि: " + e.getMessage());
                    }
                }
            }
        });
    }

    // सर्भर बन्द गर्नुहोस् (Stop server)
    public void stop() {
        running = false;
        
        // सबै क्लाइन्टहरू बन्द गर्नुहोस् (Close all clients)
        for (ClientHandler client : clients) {
            client.close();
        }
        clients.clear();
        clientsByName.clear();

        // सर्भर सकेट बन्द गर्नुहोस् (Close server socket)
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "सर्भर सकेट बन्द गर्न त्रुटि (Error closing server socket)", e);
            }
        }

        // थ्रेड पूल बन्द गर्नुहोस् (Shutdown thread pool)
        threadPool.shutdown();
        Log.i(TAG, "LAN सर्भर बन्द भयो (LAN Server stopped)");
    }

    // खेल सुरु प्रसारण गर्नुहोस् (Broadcast game started)
    public void broadcastGameStarted() {
        gameStarted = true;
        JSONObject message = new JSONObject();
        try {
            message.put("type", "game_started");
            message.put("players", playerNames.size());
            broadcastMessage(message);
            Log.i(TAG, "खेल सुरु सन्देश प्रसारण गरियो (Game started message broadcasted)");
        } catch (JSONException e) {
            Log.e(TAG, "खेल सुरु सन्देश बनाउन त्रुटि (Error creating game started message)", e);
        }
    }

    // सन्देश प्रसारण गर्नुहोस् (Broadcast message)
    public void broadcastMessage(JSONObject message) {
        String messageStr = message.toString();
        for (ClientHandler client : clients) {
            client.sendMessage(messageStr);
        }
    }

    // खेल टिक प्रसारण गर्नुहोस् (Broadcast game tick)
    public void broadcastTick(long remainingMs) {
        JSONObject message = new JSONObject();
        try {
            message.put("type", "game_tick");
            message.put("remaining_ms", remainingMs);
            broadcastMessage(message);
        } catch (JSONException e) {
            Log.e(TAG, "टिक सन्देश बनाउन त्रुटि (Error creating tick message)", e);
        }
    }

    // खेल समाप्त प्रसारण गर्नुहोस् (Broadcast game over)
    public void broadcastBurn(String loserName) {
        JSONObject message = new JSONObject();
        try {
            message.put("type", "game_over");
            message.put("loser", loserName);
            broadcastMessage(message);
            Log.i(TAG, "खेल समाप्त सन्देश प्रसारण गरियो (Game over message broadcasted)");
        } catch (JSONException e) {
            Log.e(TAG, "खेल समाप्त सन्देश बनाउन त्रुटि (Error creating game over message)", e);
        }
    }

    // त्रुटि सूचना दिनुहोस् (Notify error)
    private void notifyError(String error) {
        if (listener != null) {
            listener.onError(error);
        }
    }

    // क्लाइन्ट ह्यान्डलर वर्ग (Client handler class)
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;
        private volatile boolean connected = true;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // क्लाइन्ट सन्देशहरू सुन्नुहोस् (Listen for client messages)
                String inputLine;
                while (connected && (inputLine = in.readLine()) != null) {
                    handleMessage(inputLine);
                }
            } catch (IOException e) {
                Log.e(TAG, "क्लाइन्ट ह्यान्डलर त्रुटि (Client handler error)", e);
            } finally {
                close();
            }
        }

        // सन्देश ह्यान्डल गर्नुहोस् (Handle message)
        private void handleMessage(String message) {
            try {
                JSONObject json = new JSONObject(message);
                String type = json.getString("type");

                switch (type) {
                    case "join":
                        clientName = json.getString("name");
                        clientsByName.put(clientName, this);
                        playerNames.add(clientName);
                        
                        // स्वागत सन्देश पठाउनुहोस् (Send welcome message)
                        JSONObject welcome = new JSONObject();
                        welcome.put("type", "welcome");
                        welcome.put("playerId", playerNames.size() - 1);
                        sendMessage(welcome.toString());
                        
                        if (listener != null) {
                            listener.onClientConnected(clientName);
                        }
                        Log.i(TAG, "क्लाइन्ट जडान भयो: " + clientName + " (Client connected: " + clientName + ")");
                        break;

                    case "pass":
                        // पास अनुरोध ह्यान्डल गर्नुहोस् (Handle pass request)
                        if (gameStarted) {
                            JSONObject passMsg = new JSONObject();
                            passMsg.put("type", "pass");
                            passMsg.put("from", json.getInt("from"));
                            passMsg.put("to", json.getInt("to"));
                            broadcastMessage(passMsg);
                        }
                        break;
                }
            } catch (JSONException e) {
                Log.e(TAG, "सन्देश पार्स गर्न त्रुटि (Error parsing message): " + message, e);
            }
        }

        // सन्देश पठाउनुहोस् (Send message)
        public void sendMessage(String message) {
            if (connected && out != null) {
                out.println(message);
            }
        }

        // जडान बन्द गर्नुहोस् (Close connection)
        public void close() {
            connected = false;
            
            if (clientName != null) {
                clientsByName.remove(clientName);
                playerNames.remove(clientName);
                if (listener != null) {
                    listener.onClientDisconnected(clientName);
                }
                Log.i(TAG, "क्लाइन्ट विच्छेदन भयो: " + clientName + " (Client disconnected: " + clientName + ")");
            }

            clients.remove(this);

            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "क्लाइन्ट सकेट बन्द गर्न त्रुटि (Error closing client socket)", e);
            }
        }
    }

    // गेटर मेथडहरू (Getter methods)
    public boolean isRunning() {
        return running;
    }

    public int getConnectedClients() {
        return clients.size();
    }

    public List<String> getPlayerNames() {
        return new ArrayList<>(playerNames);
    }
}