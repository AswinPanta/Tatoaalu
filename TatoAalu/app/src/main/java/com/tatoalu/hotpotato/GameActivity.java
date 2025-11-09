package com.tatoalu.hotpotato;

import android.content.Intent;
import android.media.ToneGenerator;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * GameActivity - Clean Hot Potato Game Implementation
 * Focused on simple tap-to-pass functionality
 */
public class GameActivity extends AppCompatActivity {
    private static final String TAG = "GameActivity";

    // UI Components
    private TextView timerText;
    private TextView playerStatusTextView;
    private Button startGameButton;
    private TextView tapInstructionText;
    private ImageView flyingPotato;

    // Game State
    private String mode = "local";
    private int players = 4;
    private final List<String> playerNames = new ArrayList<>();

    // Core Game State
    private boolean gameInProgress = false;
    private boolean potatoPassing = false;
    private List<Player> activePlayers = new ArrayList<>();
    private Player currentPlayerWithPotato = null;
    private int currentHolderIndex = 0;

    // Audio
    private ToneGenerator toneGenerator;

    // Touch handling
    private View[] playerTouchZones = new View[4];
    private long lastTouchTime = 0;
    private static final long TOUCH_COOLDOWN = 300;

    // UI Handler
    private Handler uiHandler = new Handler();

    // Multiplayer components
    private LanMultiplayerManager lanMultiplayerManager;
    private String roomCode;

    // Network operation executor
    private java.util.concurrent.ExecutorService networkExecutor =
        java.util.concurrent.Executors.newCachedThreadPool();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        initializeViews();
        initializeAudio();
        processIntent();
        setupPlayerTouchZones();

        if (mode.equals("local")) {
            setupLocal();
        } else if (mode.equals("multiplayer")) {
            setupMultiplayer();
        }
    }

    private void initializeViews() {
        timerText = findViewById(R.id.timerText);
        playerStatusTextView = findViewById(R.id.playerStatusText);
        startGameButton = findViewById(R.id.startGameButton);
        flyingPotato = findViewById(R.id.flyingPotato);
        tapInstructionText = findViewById(R.id.tapInstructionText);
    }

    private void initializeAudio() {
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to initialize ToneGenerator", e);
        }
    }

    private void processIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            mode = intent.getStringExtra("gameMode");
            if (mode == null) mode = "local";

            ArrayList<String> names = intent.getStringArrayListExtra("playerNames");
            if (names != null) {
                playerNames.addAll(names);
                players = playerNames.size();

                // Initialize Player objects from names
                activePlayers.clear();
                for (int i = 0; i < names.size(); i++) {
                    Player player = new Player(names.get(i));
                    player.layoutPosition = i;
                    activePlayers.add(player);
                }
            }

            // Initialize multiplayer manager if needed
            boolean useEnhancedLan = intent.getBooleanExtra("useEnhancedLan", false);
            boolean isHost = intent.getBooleanExtra("isHost", false);
            String playerName = intent.getStringExtra("playerName");
            roomCode = intent.getStringExtra("roomCode");
            boolean hasTransferredConnection = intent.getBooleanExtra("hasTransferredConnection", false);

            if (useEnhancedLan && mode.equals("multiplayer")) {
                // Create new multiplayer manager
                lanMultiplayerManager = new LanMultiplayerManager(this);

                // Set player name BEFORE setting up listeners
                if (playerName != null && !playerName.trim().isEmpty()) {
                    lanMultiplayerManager.setLocalPlayerName(playerName.trim());
                    Log.d(TAG, "Set local player name: " + playerName.trim());
                } else {
                    Log.e(TAG, "Player name is null or empty!");
                }

                setupMultiplayerListeners();

                // Store host/client info
                Log.d(TAG, "Multiplayer setup - isHost: " + isHost + ", playerName: " + playerName + ", roomCode: " + roomCode + ", transferred: " + hasTransferredConnection);
            }
        }
    }

    private void setupLocal() {
        // Setup start button for local play
        startGameButton.setVisibility(View.VISIBLE);
        startGameButton.setText("Start Hot Potato");
        startGameButton.setEnabled(true);
        startGameButton.setOnClickListener(v -> startHotPotatoGame());

        // Show player status
        timerText.setText("Ready to play Hot Potato!");
        timerText.setVisibility(View.VISIBLE);

        String status = "Players: " + String.join(", ", playerNames);
        if (playerStatusTextView != null) {
            playerStatusTextView.setText(status);
            playerStatusTextView.setVisibility(View.VISIBLE);
        }

        setupPlayerPositions();
    }

    private void setupMultiplayer() {
        Intent intent = getIntent();
        boolean isHost = intent.getBooleanExtra("isHost", false);
        String playerName = intent.getStringExtra("playerName");
        String roomCode = intent.getStringExtra("roomCode");

        Log.d(TAG, "🔧 Setting up multiplayer - isHost: " + isHost + ", playerName: " + playerName + ", roomCode: " + roomCode);

        // Validate required parameters
        if (playerName == null || playerName.trim().isEmpty()) {
            Log.e(TAG, "❌ Player name is required for multiplayer!");
            if (playerStatusTextView != null) {
                playerStatusTextView.setText("❌ Error: Player name required");
            }
            return;
        }

        // Debug current state
        debugMultiplayerState();

        if (isHost) {
            setupHost(playerName);
        } else {
            setupClient(playerName);
        }
    }

    private void setupHost(String hostPlayerName) {
        Log.d(TAG, "🏠 SETTING UP HOST: " + hostPlayerName);

        // Clear and setup host player
        activePlayers.clear();
        playerNames.clear();

        if (hostPlayerName != null && !hostPlayerName.trim().isEmpty()) {
            Player hostPlayer = new Player(hostPlayerName.trim());
            hostPlayer.layoutPosition = 0;
            activePlayers.add(hostPlayer);
            playerNames.add(hostPlayerName.trim());
        } else {
            Player hostPlayer = new Player("Host");
            hostPlayer.layoutPosition = 0;
            activePlayers.add(hostPlayer);
            playerNames.add("Host");
        }

        // Show HOST start button
        startGameButton.setVisibility(View.VISIBLE);
        startGameButton.setText("🚀 START GAME (HOST)");
        startGameButton.setEnabled(true);
        startGameButton.setOnClickListener(v -> {
            // Debug before starting
            debugMultiplayerState();
            startMultiplayerGame();
        });

        // Show host status
        timerText.setText("HOST - Waiting for players to join...");
        timerText.setVisibility(View.VISIBLE);

        String status = "HOST: " + activePlayers.get(0).name + " | Room: " + (roomCode != null ? roomCode : "Unknown");
        if (playerStatusTextView != null) {
            playerStatusTextView.setText(status);
            playerStatusTextView.setVisibility(View.VISIBLE);

            // Make status clickable to retry connection
            playerStatusTextView.setOnClickListener(v -> {
                Log.d(TAG, "🔄 Retrying host setup...");
                debugMultiplayerState();
                if (lanMultiplayerManager != null && !activePlayers.isEmpty()) {
                    lanMultiplayerManager.setLocalPlayerName(activePlayers.get(0).name);
                    runNetworkOperation(() -> {
                        lanMultiplayerManager.createRoom(roomCode != null ? roomCode : "AUTO");
                    });
                }
            });
        }

        setupPlayerPositions();

        // Create room if we have LAN manager
        if (lanMultiplayerManager != null) {
            // Ensure player name is set before creating room
            if (activePlayers.size() > 0) {
                String hostName = activePlayers.get(0).name;
                lanMultiplayerManager.setLocalPlayerName(hostName);
                Log.d(TAG, "🔧 Setting host player name before creating room: " + hostName);

                // Add delay to ensure name is set, then create room on background thread
                uiHandler.postDelayed(() -> {
                    Log.d(TAG, "🏠 Creating room with code: " + (roomCode != null ? roomCode : "AUTO"));
                    runNetworkOperation(() -> {
                        lanMultiplayerManager.createRoom(roomCode != null ? roomCode : "AUTO");
                        Log.d(TAG, "✅ Room creation initiated");
                    });
                }, 100);

                // Check for existing players after setup
                uiHandler.postDelayed(() -> {
                    checkForExistingPlayers();
                }, 500);
            } else {
                Log.e(TAG, "❌ No active players to create room");
                if (playerStatusTextView != null) {
                    playerStatusTextView.setText("❌ Error: No players found");
                }
            }
        } else {
            Log.e(TAG, "❌ LanMultiplayerManager is null!");
            if (playerStatusTextView != null) {
                playerStatusTextView.setText("❌ Error: Network manager not initialized");
            }
        }
    }

    private void setupClient(String clientPlayerName) {
        Log.d(TAG, "📱 SETTING UP CLIENT: " + clientPlayerName);

        // Clear and setup client player
        activePlayers.clear();
        playerNames.clear();

        if (clientPlayerName != null && !clientPlayerName.trim().isEmpty()) {
            Player clientPlayer = new Player(clientPlayerName.trim());
            clientPlayer.layoutPosition = 0;
            activePlayers.add(clientPlayer);
            playerNames.add(clientPlayerName.trim());
        } else {
            Player clientPlayer = new Player("Client");
            clientPlayer.layoutPosition = 0;
            activePlayers.add(clientPlayer);
            playerNames.add("Client");
        }

        // HIDE start button for clients
        startGameButton.setVisibility(View.GONE);

        // Show client status
        timerText.setText("CLIENT - Waiting for host to start...");
        timerText.setVisibility(View.VISIBLE);

        String status = "CLIENT: " + activePlayers.get(0).name + " | Room: " + (roomCode != null ? roomCode : "Unknown");
        if (playerStatusTextView != null) {
            playerStatusTextView.setText(status);
            playerStatusTextView.setVisibility(View.VISIBLE);
        }

        setupPlayerPositions();

        // Join room if we have LAN manager
        if (lanMultiplayerManager != null && roomCode != null) {
            String clientName = activePlayers.get(0).name;
            lanMultiplayerManager.setLocalPlayerName(clientName);
            Log.d(TAG, "🔧 Setting client player name: " + clientName);

            // Check if we already joined via RoomBrowserActivity
            if (lanMultiplayerManager.isConnectedToRoom()) {
                Log.d(TAG, "📱 Already connected to room: " + roomCode);
            } else {
                Log.d(TAG, "📱 Ready to join room: " + roomCode);
            }
        }
    }

    private void startMultiplayerGame() {
        Log.d(TAG, "🚀 HOST STARTING MULTIPLAYER GAME");
        Log.d(TAG, "Current players: " + activePlayers.toString());

        if (activePlayers.isEmpty()) {
            timerText.setText("❌ No players to start!");
            return;
        }

        // Broadcast player list to all clients on background thread
        if (lanMultiplayerManager != null) {
            StringBuilder namesList = new StringBuilder();
            for (int i = 0; i < activePlayers.size(); i++) {
                if (i > 0) namesList.append(",");
                namesList.append(activePlayers.get(i).name);
            }
            String playerListMessage = namesList.toString();
            Log.d(TAG, "📡 Broadcasting player list: " + playerListMessage);

            // Move network operation to background thread
            runNetworkOperation(() -> {
                try {
                    lanMultiplayerManager.broadcastGameData("PLAYER_NAMES:" + playerListMessage);
                    Log.d(TAG, "✅ Successfully broadcast player names");

                    // Start game after short delay on UI thread
                    uiHandler.postDelayed(() -> {
                        startHotPotatoGame();
                        Log.d(TAG, "📡 Broadcasting START_GAME to clients");

                        // Another background thread for START_GAME broadcast
                        runNetworkOperation(() -> {
                            try {
                                lanMultiplayerManager.broadcastGameData("START_GAME");
                                Log.d(TAG, "✅ Successfully broadcast START_GAME");
                            } catch (Exception e) {
                                Log.e(TAG, "❌ Failed to broadcast START_GAME: " + e.getMessage());
                            }
                        });
                    }, 300);
                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to broadcast player names: " + e.getMessage());
                    uiHandler.post(() -> {
                        if (playerStatusTextView != null) {
                            playerStatusTextView.setText("❌ Failed to start multiplayer: " + e.getMessage());
                        }
                    });
                }
            });
        } else {
            startHotPotatoGame();
        }

        startGameButton.setVisibility(View.GONE);
    }

    private void setupMultiplayerListeners() {
        if (lanMultiplayerManager == null) return;

        lanMultiplayerManager.setListener(new LanMultiplayerManager.LanMultiplayerListener() {
            @Override
            public void onRoomCreated(String roomCode) {
                uiHandler.post(() -> {
                    GameActivity.this.roomCode = roomCode;
                    Log.d(TAG, "🏠 Room created successfully: " + roomCode);
                    if (playerStatusTextView != null) {
                        StringBuilder playerList = new StringBuilder();
                        for (int i = 0; i < activePlayers.size(); i++) {
                            if (i > 0) playerList.append(", ");
                            playerList.append(activePlayers.get(i).name);
                        }
                        String status = "HOST: " + playerList.toString() + " | Room: " + roomCode + " | Ready!";
                        playerStatusTextView.setText(status);
                    }

                    // Update timer text for host
                    if (timerText != null) {
                        timerText.setText("HOST - " + activePlayers.size() + " players. Waiting for more to join...");
                    }
                });
            }

            @Override
            public void onRoomJoined(String roomCode, String hostName) {
                uiHandler.post(() -> {
                    GameActivity.this.roomCode = roomCode;
                    Log.d(TAG, "📱 Joined room: " + roomCode + " hosted by: " + hostName);
                });
            }

            @Override
            public void onPlayerJoined(String playerId, String playerName) {
                uiHandler.post(() -> {
                    Log.d(TAG, "🎮 Player joined: " + playerName);

                    // Add player if not already exists
                    boolean playerExists = false;
                    for (Player player : activePlayers) {
                        if (player.name.equals(playerName)) {
                            playerExists = true;
                            break;
                        }
                    }

                    if (!playerExists) {
                        Player newPlayer = new Player(playerName);
                        newPlayer.layoutPosition = activePlayers.size();
                        activePlayers.add(newPlayer);
                        playerNames.add(playerName);

                        setupPlayerPositions();

                        updateHostUI();
                        Log.d(TAG, "✅ Total players: " + activePlayers.size());
                    }

            @Override
            public void onPlayerLeft(String playerId, String playerName) {
                uiHandler.post(() -> {
                    Log.d(TAG, "👋 Player left: " + playerName);
                    activePlayers.removeIf(player -> player.name.equals(playerName));
                    playerNames.remove(playerName);
                    setupPlayerPositions();
                });
            }

            @Override
            public void onGameStarted() {
                uiHandler.post(() -> {
                    Log.d(TAG, "🎮 Game started by host");
                    startHotPotatoGame();
                });
            }

            @Override
            public void onGameDataReceived(String data) {
                handleMultiplayerData(data);
            }

            @Override
            public void onConnectionError(String error) {
                uiHandler.post(() -> {
                    Log.e(TAG, "❌ Connection error: " + error);
                    debugMultiplayerState();

                    if (playerStatusTextView != null) {
                        playerStatusTextView.setText("❌ Error: " + error + " | Tap to retry");
                    }

                    // Try to fix common issues
                    if ((error.contains("player name") || error.contains("EADDRINUSE") || error.contains("bind failed"))
                        && !activePlayers.isEmpty()) {
                        Log.d(TAG, "🔧 Attempting to fix connection issue...");
                        if (lanMultiplayerManager != null) {
                            String playerName = activePlayers.get(0).name;
                            Log.d(TAG, "🔧 Setting player name to: " + playerName);
                            lanMultiplayerManager.setLocalPlayerName(playerName);

                            // Cleanup and retry with delay
                            uiHandler.postDelayed(() -> {
                                Log.d(TAG, "🔄 Retrying connection...");
                                // Attempt cleanup first
                                try {
                                    lanMultiplayerManager.cleanup();
                                } catch (Exception e) {
                                    Log.w(TAG, "Cleanup error: " + e.getMessage());
                                }

                                // Retry on background thread
                                uiHandler.postDelayed(() -> {
                                    runNetworkOperation(() -> {
                                        lanMultiplayerManager.createRoom(roomCode != null ? roomCode : "AUTO");
                                    });
                                }, 500);
                            }, 1000);
                        }
                    }
                });
            }

            @Override
            public void onDisconnected() {
                uiHandler.post(() -> {
                    Log.d(TAG, "📡 Disconnected from game");
                    if (playerStatusTextView != null) {
                        playerStatusTextView.setText("❌ Disconnected from game");
                    }

                    // Attempt reconnection for host
                    Intent intent = getIntent();
                    boolean isHost = intent.getBooleanExtra("isHost", false);
                    if (isHost && lanMultiplayerManager != null && !activePlayers.isEmpty()) {
                        Log.d(TAG, "🔄 Attempting to recreate room...");
                        uiHandler.postDelayed(() -> {
                            lanMultiplayerManager.setLocalPlayerName(activePlayers.get(0).name);
                            runNetworkOperation(() -> {
                                lanMultiplayerManager.createRoom("AUTO");
                            });
                        }, 2000);
                    }
                });
            }
        });
    }

    private void debugMultiplayerState() {
        Log.d(TAG, "🔍 MULTIPLAYER DEBUG STATE:");
        Log.d(TAG, "  lanMultiplayerManager: " + (lanMultiplayerManager != null ? "initialized" : "NULL"));
        Log.d(TAG, "  mode: " + mode);
        Log.d(TAG, "  roomCode: " + roomCode);
        Log.d(TAG, "  activePlayers.size(): " + activePlayers.size());
        if (!activePlayers.isEmpty()) {
            Log.d(TAG, "  first player: " + activePlayers.get(0).name);
        }
        Intent intent = getIntent();
        if (intent != null) {
            Log.d(TAG, "  intent.isHost: " + intent.getBooleanExtra("isHost", false));
            Log.d(TAG, "  intent.playerName: " + intent.getStringExtra("playerName"));
            Log.d(TAG, "  intent.useEnhancedLan: " + intent.getBooleanExtra("useEnhancedLan", false));
        }
    }

    private void handleMultiplayerData(String data) {
        String[] parts = data.split(":");
        if (parts.length < 1) return;

        String action = parts[0];
        String payload = parts.length > 1 ? parts[1] : "";

        switch (action) {
            case "START_GAME":
                uiHandler.post(() -> {
                    Log.d(TAG, "🚀 START_GAME received from host");
                    startGameButton.setVisibility(View.GONE);
                    startHotPotatoGame();
                });
                break;

            case "PLAYER_NAMES":
                String[] namesArray = payload.split(",");
                uiHandler.post(() -> {
                    Log.d(TAG, "📋 Received player names: " + payload);

                    // Keep current player but update full list
                    String currentPlayerName = activePlayers.isEmpty() ? null : activePlayers.get(0).name;
                    activePlayers.clear();
                    playerNames.clear();

                    for (int i = 0; i < namesArray.length; i++) {
                        String name = namesArray[i].trim();
                        if (!name.isEmpty()) {
                            Player player = new Player(name);
                            player.layoutPosition = i;
                            activePlayers.add(player);
                            playerNames.add(name);
                        }
                    }

                    setupPlayerPositions();

                    // Update client UI
                    if (playerStatusTextView != null) {
                        StringBuilder playerList = new StringBuilder();
                        for (int i = 0; i < activePlayers.size(); i++) {
                            if (i > 0) playerList.append(", ");
                            playerList.append(activePlayers.get(i).name);
                        }
                        String status = "CLIENT: " + (currentPlayerName != null ? currentPlayerName : "Unknown") +
                                       " | Players: " + playerList.toString();
                        playerStatusTextView.setText(status);
                    }
                });
                break;

            case "PASS":
                int newHolderIndex = Integer.parseInt(payload);
                uiHandler.post(() -> {
                    if (newHolderIndex >= 0 && newHolderIndex < activePlayers.size()) {
                        currentHolderIndex = newHolderIndex;
                        currentPlayerWithPotato = activePlayers.get(newHolderIndex);
                        currentPlayerWithPotato.givePotato();

                        updateUIAfterPass();

                        Log.d(TAG, "🥔 PASS received - new holder: " + currentPlayerWithPotato.name);
                    }
                });
                break;
        }
    }

    // Start the Hot Potato game
    private void startHotPotatoGame() {
        Log.d(TAG, "🎮 STARTING HOT POTATO");
        Log.d(TAG, "Mode: " + mode + ", activePlayers: " + activePlayers.size());

        // Validate we have players to start
        if (activePlayers.isEmpty()) {
            Log.e(TAG, "❌ Cannot start - no active players!");
            timerText.setText("❌ No players to start game!");
            return;
        }

        if (mode.equals("local") && activePlayers.size() < 2) {
            timerText.setText("Need at least 2 players for local game!");
            return;
        }

        Log.d(TAG, "🎯 Starting game with " + activePlayers.size() + " players: " + activePlayers.toString());

        gameInProgress = true;

        // Ensure first player has potato
        if (currentPlayerWithPotato == null && !activePlayers.isEmpty()) {
            currentPlayerWithPotato = activePlayers.get(0);
            currentPlayerWithPotato.givePotato();
            currentHolderIndex = 0;
        }

        // Hide UI elements
        startGameButton.setVisibility(View.GONE);

        // Show game UI
        timerText.setVisibility(View.VISIBLE);

        // Setup player positions
        setupPlayerPositions();

        // Show who starts with the potato
        String firstPlayer = currentPlayerWithPotato != null ? currentPlayerWithPotato.name : "Unknown";
        timerText.setText("🥔 " + firstPlayer + " starts with the potato!");

        // Force enable all game states immediately
        Log.d(TAG, "🔥 ENABLING GAME STATES");
        gameInProgress = true;
        potatoPassing = true;

        // Enable touch zones
        enableTapToPass();

        // Show instruction immediately
        if (tapInstructionText != null && currentPlayerWithPotato != null) {
            tapInstructionText.setText(currentPlayerWithPotato.name + ": Tap anywhere to pass!");
            tapInstructionText.setVisibility(View.VISIBLE);
        }

        Log.d(TAG, "🎮 Game started successfully!");
    }

    // Setup player positions based on count
    private void setupPlayerPositions() {
        // Hide all player containers first
        findViewById(R.id.player1Container).setVisibility(View.GONE);
        findViewById(R.id.player2Container).setVisibility(View.GONE);
        findViewById(R.id.player3Container).setVisibility(View.GONE);
        findViewById(R.id.player4Container).setVisibility(View.GONE);

        // Show and setup based on active player count
        for (int i = 0; i < activePlayers.size() && i < 4; i++) {
            Player playerObj = activePlayers.get(i);
            String playerName = playerObj.name;
            setupPlayerPosition(i + 1, playerName);
        }
    }

    private void setupPlayerPosition(int position, String name) {
        int containerId = getPlayerContainerId(position);
        int textId = getPlayerTextId(position);

        View container = findViewById(containerId);
        TextView nameText = findViewById(textId);

        if (container != null) {
            container.setVisibility(View.VISIBLE);
        }

        if (nameText != null) {
            nameText.setText(name);
        }
    }

    private int getPlayerContainerId(int position) {
        switch (position) {
            case 1: return R.id.player1Container;
            case 2: return R.id.player2Container;
            case 3: return R.id.player3Container;
            case 4: return R.id.player4Container;
            default: return R.id.player1Container;
        }
    }

    private int getPlayerTextId(int position) {
        switch (position) {
            case 1: return R.id.player1Name;
            case 2: return R.id.player2Name;
            case 3: return R.id.player3Name;
            case 4: return R.id.player4Name;
            default: return R.id.player1Name;
        }
    }

    // Setup simple tap zones for each player
    private void setupPlayerTouchZones() {
        playerTouchZones[0] = findViewById(R.id.player1Container);
        playerTouchZones[1] = findViewById(R.id.player2Container);
        playerTouchZones[2] = findViewById(R.id.player3Container);
        playerTouchZones[3] = findViewById(R.id.player4Container);

        Log.d(TAG, "Setting up tap zones for players");

        for (int i = 0; i < 4; i++) {
            if (playerTouchZones[i] != null) {
                final int playerIndex = i;
                Log.d(TAG, "Setting up tap zone for player " + (playerIndex + 1));

                playerTouchZones[i].setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        Log.d(TAG, "🖐️ TOUCH EVENT - Player " + (playerIndex + 1) + " - action: " + event.getAction());
                        Log.d(TAG, "   gameInProgress: " + gameInProgress + ", currentHolder: " + currentHolderIndex);

                        // Allow tap on ACTION_DOWN for better responsiveness
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            // Allow current holder to pass
                            boolean canPass = (currentHolderIndex == playerIndex) && gameInProgress;

                            Log.d(TAG, "   canPass: " + canPass);

                            if (canPass) {
                                Log.d(TAG, "✅ TAP ALLOWED by player " + (playerIndex + 1) + " - PASSING POTATO!");
                                passPotatoToNextPlayer();
                                return true;
                            } else {
                                Log.d(TAG, "❌ TAP BLOCKED - Not your turn! Current holder is player " + (currentHolderIndex + 1));
                            }
                        }
                        return false;
                    }
                });
            } else {
                Log.w(TAG, "Player container " + (i + 1) + " not found!");
            }
        }
    }

    // Pass potato to next player (simple sequential passing)
    private void passPotatoToNextPlayer() {
        if (activePlayers.isEmpty()) {
            Log.d(TAG, "❌ Cannot pass - no players");
            return;
        }

        if (activePlayers.size() < 2) {
            Log.d(TAG, "❌ Cannot pass - need at least 2 players");
            return;
        }

        int currentIndex = activePlayers.indexOf(currentPlayerWithPotato);
        if (currentIndex == -1) {
            Log.d(TAG, "❌ Current player with potato not found in active players");
            Log.d(TAG, "   Current holder: " + (currentPlayerWithPotato != null ? currentPlayerWithPotato.name : "null"));
            Log.d(TAG, "   Active players: " + activePlayers.toString());
            return;
        }

        int nextIndex = (currentIndex + 1) % activePlayers.size();
        Player targetPlayer = activePlayers.get(nextIndex);

        Log.d(TAG, "🥔 Passing from player " + currentIndex + " (" + currentPlayerWithPotato.name +
                   ") to player " + nextIndex + " (" + targetPlayer.name + ")");

        passPotatoTo(targetPlayer);
    }

    /**
     * Passes the potato from the current holder to a valid target player.
     * This function is decoupled from the input method (tap, swipe, etc.).
     *
     * @param targetPlayer The player to receive the potato.
     */
    public void passPotatoTo(Player targetPlayer) {
        // --- 1. Pre-condition Checks ---
        if (!gameInProgress) {
            Log.w(TAG, "Pass ignored: Game is not in progress.");
            return;
        }

        if (currentPlayerWithPotato == null) {
            Log.w(TAG, "Pass ignored: No one currently has the potato.");
            return;
        }

        if (targetPlayer == null) {
            Log.w(TAG, "Pass ignored: Target player is null.");
            return;
        }

        if (targetPlayer == currentPlayerWithPotato) {
            Log.w(TAG, "Pass ignored: Player cannot pass the potato to themselves.");
            Log.w(TAG, "   Current holder: " + currentPlayerWithPotato.name + " (ID: " + currentPlayerWithPotato.id + ")");
            Log.w(TAG, "   Target player: " + targetPlayer.name + " (ID: " + targetPlayer.id + ")");
            Log.w(TAG, "   Active players count: " + activePlayers.size());
            return;
        }

        if (!activePlayers.contains(targetPlayer)) {
            Log.w(TAG, "Pass ignored: Target player " + targetPlayer.name + " is not in the active game.");
            return;
        }

        if (!targetPlayer.canReceivePotato()) {
            Log.w(TAG, "Pass ignored: Target player " + targetPlayer.name + " cannot receive potato.");
            return;
        }

        // Apply cooldown
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTouchTime < TOUCH_COOLDOWN) {
            Log.d(TAG, "Pass ignored: Cooldown active");
            return;
        }
        lastTouchTime = currentTime;

        // --- 2. Execute the Pass ---
        Player oldHolder = currentPlayerWithPotato;
        Player newHolder = targetPlayer;

        Log.i(TAG, "🥔 PASSING POTATO: From " + oldHolder.name + " to " + newHolder.name);

        // Update state
        oldHolder.takePotato();
        newHolder.givePotato();
        currentPlayerWithPotato = newHolder;

        // Update index for compatibility
        currentHolderIndex = activePlayers.indexOf(newHolder);

        // --- 3. Post-pass Actions ---
        updateUIAfterPass();
        playPassSound();

        // Broadcast to multiplayer if host (on background thread)
        if (lanMultiplayerManager != null && mode.equals("multiplayer")) {
            Intent intent = getIntent();
            boolean isHost = intent.getBooleanExtra("isHost", false);
            if (isHost) {
                runNetworkOperation(() -> {
                    try {
                        lanMultiplayerManager.broadcastGameData("PASS:" + currentHolderIndex);
                        Log.d(TAG, "📡 Successfully broadcast PASS:" + currentHolderIndex + " to " + newHolder.name);
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Failed to broadcast pass: " + e.getMessage());
                    }
                });
            }
        }
    }

    // Update UI after potato pass
    private void updateUIAfterPass() {
        // Update instruction text
        if (tapInstructionText != null && currentPlayerWithPotato != null) {
            tapInstructionText.setText(currentPlayerWithPotato.name + ": Tap to pass the potato!");
        }

        // Show player change message briefly
        if (timerText != null && currentPlayerWithPotato != null) {
            String oldText = timerText.getText().toString();
            timerText.setText("✋ " + currentPlayerWithPotato.name + " has the potato!");
            uiHandler.postDelayed(() -> {
                if (timerText != null) {
                    timerText.setText(oldText);
                }
            }, 1500);
        }

        Log.d(TAG, "🥔 Current potato holder: " + currentPlayerWithPotato.name + " (index: " + currentHolderIndex + ")");
    }

    private void playPassSound() {
        // Play a sound effect
        if (toneGenerator != null) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 100);
        }
        Log.d(TAG, "Playing pass sound.");
    }

    // Enable tap-to-pass interaction
    private void enableTapToPass() {
        Log.d(TAG, "🟢 ENABLING TAP TO PASS - currentHolder: " + currentHolderIndex + ", players: " + activePlayers.size());
        potatoPassing = true;
        gameInProgress = true;

        // Update instruction text with current player info
        if (tapInstructionText != null && currentPlayerWithPotato != null) {
            tapInstructionText.setText(currentPlayerWithPotato.name + ": Tap to pass the potato!");
            tapInstructionText.setVisibility(View.VISIBLE);
            Log.d(TAG, "✅ Instructions updated for: " + currentPlayerWithPotato.name);
        }
    }

    // Disable tap-to-pass interaction
    private void disableTapToPass() {
        potatoPassing = false;

        // Hide instruction
        if (tapInstructionText != null) {
            tapInstructionText.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Disable game states first
        gameInProgress = false;
        potatoPassing = false;

        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }

        if (lanMultiplayerManager != null) {
            try {
                lanMultiplayerManager.cleanup();
            } catch (Exception e) {
                Log.w(TAG, "Error cleaning up multiplayer manager: " + e.getMessage());
            }
        }

        if (networkExecutor != null && !networkExecutor.isShutdown()) {
            try {
                networkExecutor.shutdownNow();
                if (!networkExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                    Log.w(TAG, "Network executor did not terminate gracefully");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "Interrupted while waiting for executor shutdown");
            } catch (Exception e) {
                Log.w(TAG, "Error shutting down network executor: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableTapToPass();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume passing if game was in progress
        if (gameInProgress && currentPlayerWithPotato != null) {
            enableTapToPass();
        }
    }

    /**
     * Utility method to run network operations safely in background thread
     */
    private void runNetworkOperation(Runnable networkOperation) {
        if (networkExecutor != null && !networkExecutor.isShutdown()) {
            try {
                networkExecutor.execute(() -> {
                    try {
                        networkOperation.run();
                    } catch (Exception e) {
                        Log.e(TAG, "Network operation failed: " + e.getMessage(), e);
                        // Post error to UI thread if needed
                        uiHandler.post(() -> {
                            if (playerStatusTextView != null) {
                                playerStatusTextView.setText("Network error: " + e.getMessage());
                            }
                        });
                    }
                });
            } catch (java.util.concurrent.RejectedExecutionException e) {
                Log.w(TAG, "Network executor is shutting down, cannot execute operation");
                // Try to inform user if possible
                uiHandler.post(() -> {
                    if (playerStatusTextView != null) {
                        playerStatusTextView.setText("Connection is shutting down");
                    }
                });
            }
        } else {
            Log.w(TAG, "Network executor is not available");
        }
    }

    /**
     * Update host UI with current player count and status
     */
    private void updateHostUI() {
        if (playerStatusTextView != null) {
            StringBuilder playerList = new StringBuilder();
            for (int i = 0; i < activePlayers.size(); i++) {
                if (i > 0) playerList.append(", ");
                playerList.append(activePlayers.get(i).name);
            }
            String status = "HOST: " + playerList.toString() + " | Room: " + (roomCode != null ? roomCode : "Unknown");
            if (activePlayers.size() >= 2) {
                status += " | Ready to start!";
            }
            playerStatusTextView.setText(status);
        }

        // Update main timer text based on player count
        if (timerText != null) {
            if (activePlayers.size() >= 2) {
                timerText.setText("HOST - " + activePlayers.size() + " players connected. Ready to start!");
            } else {
                timerText.setText("HOST - Waiting for players to join... (" + activePlayers.size() + "/2)");
            }
        }
    }

    /**
     * Check if there are existing players connected (for transferred connections)
     */
    private void checkForExistingPlayers() {
        if (lanMultiplayerManager != null) {
            // Get connected players from the multiplayer manager
            try {
                List<String> connectedPlayerNames = lanMultiplayerManager.getConnectedPlayerNames();
                if (connectedPlayerNames != null && connectedPlayerNames.size() > 1) { // More than just host
                    Log.d(TAG, "🔄 Found existing connected players: " + connectedPlayerNames.size());

                    // Add missing players to activePlayers
                    for (String playerName : connectedPlayerNames) {
                        boolean playerExists = false;
                        for (Player player : activePlayers) {
                            if (player.name.equals(playerName)) {
                                playerExists = true;
                                break;
                            }
                        }

                        if (!playerExists && !playerName.equals(activePlayers.get(0).name)) {
                            Player newPlayer = new Player(playerName);
                            newPlayer.layoutPosition = activePlayers.size();
                            activePlayers.add(newPlayer);
                            playerNames.add(playerName);
                            Log.d(TAG, "🔄 Added existing player: " + playerName);
                        }
                    }

                    setupPlayerPositions();
                    updateHostUI();
                }
            } catch (Exception e) {
                Log.w(TAG, "Error checking existing players: " + e.getMessage());
            }
        }
    }
}
