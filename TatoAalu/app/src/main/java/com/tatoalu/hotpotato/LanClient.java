package com.tatoalu.hotpotato;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

// LAN क्लाइन्ट - स्थानीय नेटवर्क खेल जडान (LAN Client - Local Network Game Connection)
public class LanClient {
    private static final String TAG = Config.TAG_CLIENT;
    
    // जडान कन्फिगरेसन (Connection configuration)
    private final String serverHost;
    private final int serverPort;
    private final String playerName;
    
    // नेटवर्क घटकहरू (Network components)
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;
    
    // जडान अवस्था (Connection state)
    private volatile boolean connected = false;
    private volatile boolean connecting = false;
    private int playerId = -1;
    
    // इभेन्छीस्त (Event listener)
    public interface ClientListener {
        void onConnected(int playerId); // जडान सफल (Connection successful)
        void onDisconnected(); // जडान विच्छेदन (Connection disconnected)
        void onGameStarted(); // खेल सुरु भयो (Game started)
        void onPassReceived(int from, int to); // पास प्राप्त भयो (Pass received)
        void onError(String error); // त्रुटि (Error)
    }
    
    private ClientListener listener;

    // कन्स्ट्रक्टर (Constructor)
    public LanClient(String serverHost, int serverPort, String playerName) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.playerName = playerName;
    }

    // श्रोता सेट गर्नुहोस् (Set listener)
    public void setClientListener(ClientListener listener) {
        this.listener = listener;
    }

    // सर्भरमा जडान गर्नुहोस् (Connect to server)
    public void connect() {
        if (connected || connecting) {
            Log.w(TAG, "पहिले नै जडान भएको वा जडान प्रक्रियामा छ (Already connected or connecting)");
            return;
        }

        connecting = true;
        
        // पृष्ठभूमि थ्रेडमा जडान गर्नुहोस् (Connect in background thread)
        new Thread(() -> {
            try {
                Log.i(TAG, "सर्भरमा जडान गर्दै: " + serverHost + ":" + serverPort + " (Connecting to server: " + serverHost + ":" + serverPort + ")");
                
                // सकेट जडान (Socket connection)
                socket = new Socket(serverHost, serverPort);
                socket.setSoTimeout(Config.SOCKET_TIMEOUT_MS);
                
                // इनपुट/आउटपुट स्ट्रिमहरू सेटअप गर्नुहोस् (Setup input/output streams)
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                connected = true;
                connecting = false;
                
                Log.i(TAG, "सर्भरमा सफलतापूर्वक जडान भयो (Successfully connected to server)");
                
                // जडान सन्देश पठाउनुहोस् (Send join message)
                sendJoinMessage();
                
                // सन्देश श्रोता सुरु गर्नुहोस् (Start message listener)
                startMessageListener();
                
            } catch (IOException e) {
                connecting = false;
                connected = false;
                Log.e(TAG, "सर्भर जडान त्रुटि (Server connection error)", e);
                notifyError("सर्भर जडान असफल: " + e.getMessage());
                cleanup();
            }
        }, "LanClient-Connect").start();
    }

    // जडान सन्देश पठाउनुहोस् (Send join message)
    private void sendJoinMessage() {
        try {
            JSONObject joinMessage = new JSONObject();
            joinMessage.put("type", "join");
            joinMessage.put("name", playerName);
            sendMessage(joinMessage.toString());
            Log.i(TAG, "जडान सन्देश पठाइयो: " + playerName + " (Join message sent: " + playerName + ")");
        } catch (JSONException e) {
            Log.e(TAG, "जडान सन्देश बनाउन त्रुटि (Error creating join message)", e);
        }
    }

    // सन्देश श्रोता सुरु गर्नुहोस् (Start message listener)
    private void startMessageListener() {
        listenerThread = new Thread(() -> {
            try {
                String inputLine;
                while (connected && (inputLine = in.readLine()) != null) {
                    handleMessage(inputLine);
                }
            } catch (SocketTimeoutException e) {
                Log.w(TAG, "सकेट टाइमआउट (Socket timeout)");
            } catch (IOException e) {
                if (connected) {
                    Log.e(TAG, "सन्देश पढ्न त्रुटि (Error reading message)", e);
                    notifyError("सन्देश पढ्न त्रुटि: " + e.getMessage());
                }
            } finally {
                disconnect();
            }
        }, "LanClient-Listener");
        
        listenerThread.start();
    }

    // सन्देश ह्यान्डल गर्नुहोस् (Handle message)
    private void handleMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.getString("type");

            switch (type) {
                case "welcome":
                    // स्वागत सन्देश (Welcome message)
                    playerId = json.getInt("playerId");
                    Log.i(TAG, "स्वागत सन्देश प्राप्त भयो, खेलाडी ID: " + playerId + " (Welcome message received, player ID: " + playerId + ")");
                    if (listener != null) {
                        listener.onConnected(playerId);
                    }
                    break;

                case "game_started":
                    // खेल सुरु सन्देश (Game started message)
                    Log.i(TAG, "खेल सुरु भयो (Game started)");
                    if (listener != null) {
                        listener.onGameStarted();
                    }
                    break;

                case "pass":
                    // पास सन्देश (Pass message)
                    int from = json.getInt("from");
                    int to = json.getInt("to");
                    Log.i(TAG, "पास प्राप्त भयो: " + from + " देखि " + to + " (Pass received: " + from + " to " + to + ")");
                    if (listener != null) {
                        listener.onPassReceived(from, to);
                    }
                    break;

                case "error":
                    // त्रुटि सन्देश (Error message)
                    String error = json.optString("message", "अज्ञात त्रुटि (Unknown error)");
                    Log.e(TAG, "सर्भर त्रुटि: " + error + " (Server error: " + error + ")");
                    notifyError("सर्भर त्रुटि: " + error);
                    break;

                default:
                    Log.w(TAG, "अज्ञात सन्देश प्रकार: " + type + " (Unknown message type: " + type + ")");
                    break;
            }
        } catch (JSONException e) {
            Log.e(TAG, "सन्देश पार्स गर्न त्रुटि (Error parsing message): " + message, e);
        }
    }

    // पास अनुरोध पठाउनुहोस् (Send pass request)
    public void sendPassRequest(int targetPlayerId) {
        if (!connected) {
            Log.w(TAG, "जडान नभएको, पास पठाउन सकिएन (Not connected, cannot send pass)");
            return;
        }

        try {
            JSONObject passMessage = new JSONObject();
            passMessage.put("type", "pass");
            passMessage.put("from", playerId);
            passMessage.put("to", targetPlayerId);
            sendMessage(passMessage.toString());
            Log.i(TAG, "पास अनुरोध पठाइयो: " + playerId + " देखि " + targetPlayerId + " (Pass request sent: " + playerId + " to " + targetPlayerId + ")");
        } catch (JSONException e) {
            Log.e(TAG, "पास सन्देश बनाउन त्रुटि (Error creating pass message)", e);
        }
    }

    // सन्देश पठाउनुहोस् (Send message)
    private void sendMessage(String message) {
        if (connected && out != null) {
            out.println(message);
            if (out.checkError()) {
                Log.e(TAG, "सन्देश पठाउन त्रुटि (Error sending message)");
                disconnect();
            }
        }
    }

    // जडान विच्छेदन गर्नुहोस् (Disconnect)
    public void disconnect() {
        if (!connected && !connecting) {
            return;
        }

        Log.i(TAG, "सर्भरबाट विच्छेदन गर्दै (Disconnecting from server)");
        
        connected = false;
        connecting = false;
        
        // श्रोता थ्रेड बन्द गर्नुहोस् (Stop listener thread)
        if (listenerThread != null && listenerThread.isAlive()) {
            listenerThread.interrupt();
        }
        
        cleanup();
        
        // श्रोतालाई सूचना दिनुहोस् (Notify listener)
        if (listener != null) {
            listener.onDisconnected();
        }
        
        Log.i(TAG, "सर्भरबाट विच्छेदन भयो (Disconnected from server)");
    }

    // सफाई गर्नुहोस् (Cleanup)
    private void cleanup() {
        try {
            if (out != null) {
                out.close();
                out = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "आउटपुट स्ट्रिम बन्द गर्न त्रुटि (Error closing output stream)", e);
        }

        try {
            if (in != null) {
                in.close();
                in = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "इनपुट स्ट्रिम बन्द गर्न त्रुटि (Error closing input stream)", e);
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                socket = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "सकेट बन्द गर्न त्रुटि (Error closing socket)", e);
        }
    }

    // त्रुटि सूचना दिनुहोस् (Notify error)
    private void notifyError(String error) {
        if (listener != null) {
            listener.onError(error);
        }
    }

    // गेटर मेथडहरू (Getter methods)
    public boolean isConnected() {
        return connected;
    }

    public boolean isConnecting() {
        return connecting;
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }
}