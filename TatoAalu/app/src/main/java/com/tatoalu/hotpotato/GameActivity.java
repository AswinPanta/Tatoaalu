package com.tatoalu.hotpotato;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.SystemClock;
import android.widget.TextView;
import android.widget.Button;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.util.Log;
import java.util.Random;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

// खेल गतिविधि - मुख्य खेल स्क्रिन (Game Activity - Main Game Screen)
public class GameActivity extends AppCompatActivity {
    private static final String TAG = "GameActivity";
    private GameView gameView;
    private TextView timerText;
    private TextView lobbyCountdownTextView;
    private TextView playerStatusTextView;
    private Button startGameButton;
    private Handler uiHandler = new Handler();
    private LeaderboardManager leaderboardManager;

    // एकीकृत नेटवर्किङ (Unified Networking)
    private UnifiedMultiplayerManager multiplayerManager;
    private WifiManager.MulticastLock multicastLock;

    // होस्ट टिक (Host tick)
    private final Handler tickHandler = new Handler();
    private Runnable tickRunnable;
    private long hostStartTime;
    private long hostBurnMs;
    private final Random random = new Random();

    // खेल अवस्था (Game state)
    private String mode = "local";
    private int players = 4;
    private int currentHolder = 0;
    private final List<String> playerNames = new ArrayList<>();
    private String roomCode;
    private UnifiedMultiplayerManager.ConnectionMode connectionMode = UnifiedMultiplayerManager.ConnectionMode.AUTO_FALLBACK;

    private static final int PORT = 54567;
    private String selfName = "खेलाडी"; // Player

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // दृश्य तत्वहरू सुरु गर्नुहोस् (Initialize views)
        gameView = findViewById(R.id.gameView);
        timerText = findViewById(R.id.timerText);
        lobbyCountdownTextView = findViewById(R.id.lobbyCountdownText);
        playerStatusTextView = findViewById(R.id.playerStatusText);
        startGameButton = findViewById(R.id.startGameButton);

        // लिडरबोर्ड प्रबन्धक सुरु गर्नुहोस् (Initialize leaderboard manager)
        leaderboardManager = new LeaderboardManager();

        // इन्टेन्ट डेटा प्राप्त गर्नुहोस् (Get intent data)
        Intent intent = getIntent();
        if (intent != null) {
            mode = intent.getStringExtra("gameMode");
            if (mode == null) mode = "local";
            
            ArrayList<String> names = intent.getStringArrayListExtra("playerNames");
            if (names != null) {
                playerNames.addAll(names);
                players = playerNames.size();
            }
            
            boolean useUnified = intent.getBooleanExtra("useUnifiedMultiplayerManager", false);
             if (useUnified) {
                 multiplayerManager = new UnifiedMultiplayerManager(this);
                 if (multiplayerManager != null) {
                     setupMultiplayerListeners();
                 }
             }
        }

        // खेल मोडको आधारमा सेटअप गर्नुहोस् (Setup based on game mode)
        if (mode.equals("local")) {
            setupLocal();
        } else if (mode.equals("multiplayer")) {
            if (multiplayerManager != null && multiplayerManager.isHost()) {
                setupHost();
            } else {
                setupClient();
            }
        }
    }

    // मल्टिप्लेयर श्रोताहरू सेटअप गर्नुहोस् (Setup multiplayer listeners)
    private void setupMultiplayerListeners() {
        multiplayerManager.setMultiplayerListener(new UnifiedMultiplayerManager.UnifiedMultiplayerListener() {
            @Override
            public void onConnectionStateChanged(UnifiedMultiplayerManager.ConnectionState state, 
                                               UnifiedMultiplayerManager.NetworkType networkType) {
                uiHandler.post(() -> {
                    Log.d(TAG, "Connection state changed: " + state + " on " + networkType);
                    updateConnectionStatus(state, networkType);
                });
            }

            @Override
            public void onRoomCreated(String roomCode, UnifiedMultiplayerManager.NetworkType networkType) {
                uiHandler.post(() -> {
                    GameActivity.this.roomCode = roomCode;
                    Log.d(TAG, "Room created: " + roomCode + " on " + networkType);
                    playerStatusTextView.setText("Room Code: " + roomCode + "\nWaiting for players...");
                });
            }

            @Override
            public void onRoomJoined(String roomCode, UnifiedMultiplayerManager.NetworkType networkType) {
                uiHandler.post(() -> {
                    GameActivity.this.roomCode = roomCode;
                    Log.d(TAG, "Joined room: " + roomCode + " on " + networkType);
                    playerStatusTextView.setText("Joined room: " + roomCode);
                });
            }

            @Override
            public void onPlayerJoined(String playerId, String playerName, UnifiedMultiplayerManager.NetworkType networkType) {
                uiHandler.post(() -> {
                    if (!playerNames.contains(playerName)) {
                        playerNames.add(playerName);
                        gameView.setPlayerNames(playerNames);
                        updatePlayerStatus();
                        
                        // Enable start button if we have enough players and we're the host
                        if (mode.equals("host") && playerNames.size() >= 2) {
                            startGameButton.setEnabled(true);
                        }
                    }
                });
            }

            @Override
            public void onPlayerLeft(String playerId, String playerName, UnifiedMultiplayerManager.NetworkType networkType) {
                uiHandler.post(() -> {
                    playerNames.remove(playerName);
                    gameView.setPlayerNames(playerNames);
                    updatePlayerStatus();
                });
            }

            @Override
            public void onHostChanged(String newHostId, UnifiedMultiplayerManager.NetworkType networkType) {
                uiHandler.post(() -> {
                    Log.d(TAG, "Host changed to: " + newHostId);
                    // Handle host migration if needed
                });
            }

            @Override
            public void onNetworkError(String error, UnifiedMultiplayerManager.NetworkType networkType) {
                uiHandler.post(() -> {
                    Log.e(TAG, "Network error on " + networkType + ": " + error);
                    showNetworkError(error, networkType);
                });
            }

            @Override
            public void onFallbackActivated(UnifiedMultiplayerManager.NetworkType fromType, 
                                          UnifiedMultiplayerManager.NetworkType toType) {
                uiHandler.post(() -> {
                    Log.d(TAG, "Network fallback: " + fromType + " -> " + toType);
                    playerStatusTextView.setText("Switched to " + toType + " network");
                });
            }
        });

        multiplayerManager.setGameEventListener(new UnifiedMultiplayerManager.GameEventListener() {
            @Override
            public void onGameStateChanged(FirebaseRealtimeMultiplayer.GameState state, java.util.Map<String, Object> data) {
                uiHandler.post(() -> {
                    handleGameStateChange(state, data);
                });
            }

            @Override
            public void onGameTick(long remainingMs) {
                uiHandler.post(() -> {
                    timerText.setText("🔥 " + remainingMs + " ms");
                });
            }

            @Override
            public void onPotatoPassed(String fromPlayer, String toPlayer) {
                uiHandler.post(() -> {
                    // Find player indices and update game view
                    int fromIndex = playerNames.indexOf(fromPlayer);
                    int toIndex = playerNames.indexOf(toPlayer);
                    if (toIndex >= 0) {
                        currentHolder = toIndex;
                        gameView.setCurrentHolder(currentHolder);
                    }
                });
            }

            @Override
            public void onGameOver(String loserPlayerId, String loserName) {
                uiHandler.post(() -> {
                    timerText.setText(getString(R.string.timer_game_over, loserName));
                    showOutcomeOverlay(loserName);
                    findViewById(R.id.endButtonsContainer).setVisibility(View.VISIBLE);
                    setupEndButtons();
                    updateWins(loserName);
                });
            }
        });
    }

    // जडान स्थिति अपडेट गर्नुहोस् (Update connection status)
    private void updateConnectionStatus(UnifiedMultiplayerManager.ConnectionState state, 
                                     UnifiedMultiplayerManager.NetworkType networkType) {
        String statusText = "";
        switch (state) {
            case CONNECTING:
                statusText = "Connecting via " + networkType + "...";
                break;
            case CONNECTED:
                statusText = "Connected via " + networkType;
                break;
            case DISCONNECTED:
                statusText = "Disconnected";
                break;
            case ERROR:
                statusText = "Connection error";
                break;
        }
        
        if (playerStatusTextView != null) {
            playerStatusTextView.setText(statusText);
        }
    }

    // खेलाडी स्थिति अपडेट गर्नुहोस् (Update player status)
    private void updatePlayerStatus() {
        if (playerStatusTextView != null) {
            String status = "Players (" + playerNames.size() + "/" + players + "): " + 
                           String.join(", ", playerNames);
            if (roomCode != null) {
                status = "Room: " + roomCode + "\n" + status;
            }
            playerStatusTextView.setText(status);
        }
    }

    // नेटवर्क त्रुटि देखाउनुहोस् (Show network error)
    private void showNetworkError(String error, UnifiedMultiplayerManager.NetworkType networkType) {
        new AlertDialog.Builder(this)
            .setTitle("Network Error")
            .setMessage("Error on " + networkType + ": " + error)
            .setPositiveButton("OK", null)
            .show();
    }

    // खेल अवस्था परिवर्तन ह्यान्डल गर्नुहोस् (Handle game state change)
    private void handleGameStateChange(FirebaseRealtimeMultiplayer.GameState state, 
                                     java.util.Map<String, Object> data) {
        switch (state) {
            case WAITING_FOR_PLAYERS:
                // Game is waiting for players
                break;
            case COUNTDOWN:
                // Game is starting
                if (lobbyCountdownTextView != null) {
                    lobbyCountdownTextView.setVisibility(View.GONE);
                }
                if (startGameButton != null) {
                    startGameButton.setVisibility(View.GONE);
                }
                timerText.setVisibility(View.VISIBLE);
                break;
            case PLAYING:
                // Game is in progress
                if (data != null && data.containsKey("currentHolder")) {
                    Object holderObj = data.get("currentHolder");
                    if (holderObj instanceof Number) {
                        currentHolder = ((Number) holderObj).intValue();
                        gameView.setCurrentHolder(currentHolder);
                    }
                }
                break;
            case GAME_OVER:
                // Game has ended
                break;
        }
    }

    private void setupLocal() {
        // Show one-time visual cue when countdown begins
        TextView cue = findViewById(R.id.countdownCue);
        if (cue != null) {
            cue.setAlpha(1f);
            cue.setVisibility(TextView.VISIBLE);
            cue.animate().alpha(0f).setDuration(1200).withEndAction(() -> cue.setVisibility(TextView.GONE)).start();
        }
        // Play a short audio beep as an additional cue
        try {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 300);
        } catch (Exception ignored) {}

        gameView.init(players, new GameView.GameListener() {
            @Override
            public void onTick(long millisRemaining) {
                uiHandler.post(() -> timerText.setText("🔥 " + millisRemaining + " ms"));
            }
            @Override
            public void onGameOver(String loserName) {
                uiHandler.post(() -> {
                    timerText.setText(getString(R.string.timer_game_over, loserName));
                    // Outcome overlay
                    showOutcomeOverlay(loserName);
                    // Show end buttons
                    findViewById(R.id.endButtonsContainer).setVisibility(TextView.VISIBLE);
                    setupEndButtons();
                    // Update wins for all non-losers
                    updateWins(loserName);

                    // Submit score to leaderboard
                    leaderboardManager.submitScore("local_player", loserName, gameView.getScore(), "local_quick_game");
                });
            }
        });
        // Handle both ArrayList<String> and String[] for player names
        ArrayList<String> names = null;
        try {
            names = getIntent().getStringArrayListExtra("names");
        } catch (Exception e) {
            Log.d(TAG, "Failed to get ArrayList, trying String array");
        }
        
        if (names == null) {
            String[] nameArray = getIntent().getStringArrayExtra("names");
            if (nameArray != null) {
                names = new ArrayList<>();
                for (String name : nameArray) {
                    names.add(name);
                }
            }
        }
        
        if (names != null && !names.isEmpty()) {
            playerNames.clear();
            playerNames.addAll(names);
            gameView.setPlayerNames(playerNames);
            gameView.setCurrentHolder(0);
        }
    }

    private void setupHost() {
        gameView.init(players, new GameView.GameListener() {
            @Override public void onTick(long millisRemaining) { /* host uses own tick */ }
            @Override public void onGameOver(String loserName) { /* host triggers manually */ }
        });
        gameView.setRemoteMode(true);
        gameView.setPassCallback(this::advancePass);

        // Host name and initial state
        currentHolder = 0;
        // Don't clear playerNames - keep the lobby players
        gameView.setPlayerNames(playerNames);
        gameView.setCurrentHolder(currentHolder);

        // Create room using unified multiplayer manager
        multiplayerManager.createRoom(players);

        // Start centralized game timer
        startGameTimer();
    }

    // Centralized state broadcast system
    private void broadcastCurrentState() {
        // State broadcasting is handled automatically by the unified multiplayer manager
        // through game event listeners and network-specific implementations
        Log.d(TAG, "Current holder: " + currentHolder + ", Players: " + playerNames.size());
    }

    // Centralized game timer management
    private void startGameTimer() {
        // Stop any existing timer
        stopGameTimer();
        
        hostStartTime = SystemClock.uptimeMillis();
        hostBurnMs = 3500 + random.nextInt(4000);
        tickRunnable = new Runnable() {
            @Override public void run() {
                long elapsed = SystemClock.uptimeMillis() - hostStartTime;
                long remaining = Math.max(0, hostBurnMs - elapsed);
                uiHandler.post(() -> timerText.setText("🔥 " + remaining + " ms"));
                
                // Centralized tick broadcast
                if (multiplayerManager != null) {
                    multiplayerManager.broadcastGameTick(remaining);
                }
                
                if (remaining <= 0) {
                    burnOut();
                } else {
                    tickHandler.postDelayed(this, 100);
                }
            }
        };
        tickHandler.post(tickRunnable);
    }

    private void stopGameTimer() {
        if (tickRunnable != null) {
            tickHandler.removeCallbacks(tickRunnable);
            tickRunnable = null;
        }
    }

    private static final int LOBBY_DURATION_MS = 45000; // 45 seconds
    private Handler lobbyHandler = new Handler();
    private Runnable lobbyCountdownRunnable;
    private long lobbyStartTime;

    private void startMatchmakingLobby() {
        lobbyCountdownTextView.setVisibility(View.VISIBLE);
        playerStatusTextView.setVisibility(View.VISIBLE);
        startGameButton.setVisibility(View.VISIBLE);
        startGameButton.setEnabled(false);
        timerText.setVisibility(View.GONE); // Hide game timer during lobby

        startGameButton.setOnClickListener(v -> {
            lobbyHandler.removeCallbacks(lobbyCountdownRunnable);
            lobbyCountdownTextView.setVisibility(View.GONE);
            playerStatusTextView.setVisibility(View.GONE);
            startGameButton.setVisibility(View.GONE);
            timerText.setVisibility(View.VISIBLE);
            
            // Start the game using unified multiplayer manager
            multiplayerManager.startGame();
            setupHost();
        });

        playerNames.clear();
        playerNames.add(selfName);
        updatePlayerStatus();

        // Create room using unified multiplayer manager
        multiplayerManager.createRoom(players);

        // Start lobby countdown
        lobbyStartTime = SystemClock.uptimeMillis();
        lobbyCountdownRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - lobbyStartTime;
                long remaining = LOBBY_DURATION_MS - elapsed;

                if (remaining <= 0) {
                    // Lobby timed out
                    lobbyCountdownTextView.setText("Lobby: 0s");
                    handleLobbyTimeout();
                } else {
                    lobbyCountdownTextView.setText("Lobby: " + (remaining / 1000) + "s");
                    lobbyHandler.postDelayed(this, 1000);
                }
            }
        };
        lobbyHandler.post(lobbyCountdownRunnable);
    }

    private void handleLobbyTimeout() {
        // Stop multiplayer manager
        if (multiplayerManager != null) {
            multiplayerManager.cleanup();
        }
        lobbyHandler.removeCallbacks(lobbyCountdownRunnable);

        // Display message and offer retry/exit
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("No players joined. Would you like to retry matchmaking?")
                .setPositiveButton("Retry", (dialog, id) -> {
                    // Restart matchmaking
                    startMatchmakingLobby();
                })
                .setNegativeButton("Exit", (dialog, id) -> {
                    // Exit to main menu
                    finish();
                });
        builder.create().show();
    }

    private void setupClient() {
        gameView.init(players, new GameView.GameListener() {
            @Override public void onTick(long millisRemaining) { /* client uses host tick */ }
            @Override public void onGameOver(String loserName) { /* client uses host burn */ }
        });
        gameView.setRemoteMode(true);

        // Join room using unified multiplayer manager
        if (roomCode != null && !roomCode.isEmpty()) {
            multiplayerManager.joinRoom(roomCode);
        } else {
            // Auto-discover and join available rooms
            // For now, just try to join with a default room code
            multiplayerManager.joinRoom("AUTO");
        }
    }

    // Method removed - now handled by UnifiedMultiplayerManager

    private void advancePass() {
        // Advance holder and reset timer
        currentHolder = (currentHolder + 1) % Math.max(1, playerNames.size());
        gameView.setCurrentHolder(currentHolder);
        
        // Notify multiplayer manager about potato pass
        if (multiplayerManager != null && !playerNames.isEmpty()) {
            String targetPlayerId = playerNames.get(currentHolder);
            multiplayerManager.passPotatoTo(targetPlayerId);
        }
        
        // Use centralized timer restart
        startGameTimer();
        
        // Use centralized broadcast
        broadcastCurrentState();
    }

    private void burnOut() {
        String loserName = playerNames.isEmpty() ? "Player" : playerNames.get(currentHolder);
        gameView.triggerBurn();
        uiHandler.post(() -> {
            timerText.setText("Game Over! " + loserName + " burned");
            showOutcomeOverlay(loserName);
            findViewById(R.id.endButtonsContainer).setVisibility(TextView.VISIBLE);
            setupEndButtons();
            updateWins(loserName);

            // Submit score to leaderboard
            leaderboardManager.submitScore("host_player", loserName, gameView.getScore(), "multiplayer");
        });
        if (multiplayerManager != null) {
            multiplayerManager.broadcastGameOver(loserName);
        }
    }

    private void setupEndButtons() {
        findViewById(R.id.restartButton).setOnClickListener(v -> restartGame());
        findViewById(R.id.homeButton).setOnClickListener(v -> finish());
        findViewById(R.id.leaderboardButton).setOnClickListener(v -> {
            Intent intent = new Intent(GameActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });
    }

    private void restartGame() {
        recreate();
    }

    private void updateWins(String loserName) {
        android.content.SharedPreferences prefs = getSharedPreferences("tato_wins", Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        for (String name : playerNames) {
            if (name != null && !name.equals(loserName)) {
                int current = prefs.getInt(name, 0);
                int newWins = current + 1;
                editor.putInt(name, newWins);
                
                // Update Firebase leaderboard
                FirebaseManager.getInstance().submitScore(name, name, newWins, "local_game");
            }
        }
        editor.apply();
    }

    private void showOutcomeOverlay(String loserName) {
        // Implement the logic to show an overlay with the game outcome
        // This could involve making a TextView or other layout visible and setting its text
        // For now, let's just log it.
        Log.d("GameActivity", "Game Over! Loser: " + loserName);

        // Example: Make a TextView visible and set its text
        TextView outcomeText = findViewById(R.id.outcomeText);
        if (outcomeText != null) {
            outcomeText.setText(getString(R.string.game_over_message, loserName));
            outcomeText.setVisibility(View.VISIBLE);

            // Animate the overlay to appear
            outcomeText.setAlpha(0f);
            outcomeText.setScaleX(0.5f);
            outcomeText.setScaleY(0.5f);
            outcomeText.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setInterpolator(new OvershootInterpolator())
                    .start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pause();
        // Pause multiplayer connections to conserve resources
        if (multiplayerManager != null) {
            Log.i(Config.TAG_GAME, "Pausing multiplayer connections");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resume();
        // Resume multiplayer connections if needed
        if (multiplayerManager != null) {
            Log.i(Config.TAG_GAME, "Resuming multiplayer connections");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // सफाई गर्नुहोस् (Cleanup)
        if (tickHandler != null && tickRunnable != null) {
            tickHandler.removeCallbacks(tickRunnable);
        }
        
        if (lobbyHandler != null && lobbyCountdownRunnable != null) {
            lobbyHandler.removeCallbacks(lobbyCountdownRunnable);
        }
        
        if (multicastLock != null && multicastLock.isHeld()) {
            multicastLock.release();
        }
        
        if (multiplayerManager != null) {
            multiplayerManager.cleanup();
        }
    }
}