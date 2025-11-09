package com.tatoalu.hotpotato;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Room Browser Activity - Mini Militia style LAN game browser
 * Shows available Hot Potato rooms on local network
 * Allows creating new rooms or joining existing ones
 */
public class RoomBrowserActivity extends AppCompatActivity implements EnhancedLanDiscovery.LanDiscoveryListener {
    private static final String TAG = "RoomBrowserActivity";

    // Static reference to transfer connection state to GameActivity
    public static LanMultiplayerManager transferManager = null;

    // Network executor for background operations
    private java.util.concurrent.ExecutorService networkExecutor;

    // UI Components
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView roomsRecyclerView;
    private ProgressBar discoveryProgressBar;
    private TextView statusTextView;
    private TextView playerNameLabel;
    private TextInputEditText playerNameInput;
    private MaterialButton createRoomButton;
    private MaterialButton refreshButton;
    private MaterialButton backButton;
    private MaterialCardView noRoomsCard;

    // Discovery and networking
    private EnhancedLanDiscovery lanDiscovery;
    private LanMultiplayerManager lanMultiplayerManager;
    private RoomListAdapter roomAdapter;
    private List<EnhancedLanDiscovery.DiscoveredRoom> discoveredRooms;
    private boolean isDiscovering = false;
    private String playerName = "Player";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_browser);

        // Initialize network executor
        networkExecutor = java.util.concurrent.Executors.newCachedThreadPool();

        initializeViews();
        setupRecyclerView();
        setupNetworking();
        setupButtons();

        // Get player name from intent or use default
        Intent intent = getIntent();
        if (intent != null) {
            String name = intent.getStringExtra("playerName");
            if (name != null && !name.trim().isEmpty()) {
                playerName = name.trim();
                playerNameInput.setText(playerName);
            }
        }

        startDiscovery();
    }

    private void initializeViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        roomsRecyclerView = findViewById(R.id.roomsRecyclerView);
        discoveryProgressBar = findViewById(R.id.discoveryProgressBar);
        statusTextView = findViewById(R.id.statusTextView);
        playerNameLabel = findViewById(R.id.playerNameLabel);
        playerNameInput = findViewById(R.id.playerNameInput);
        createRoomButton = findViewById(R.id.createRoomButton);
        refreshButton = findViewById(R.id.refreshButton);
        backButton = findViewById(R.id.backButton);
        noRoomsCard = findViewById(R.id.noRoomsCard);
    }

    private void setupRecyclerView() {
        discoveredRooms = new ArrayList<>();
        roomAdapter = new RoomListAdapter(discoveredRooms, this::joinRoom);

        roomsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        roomsRecyclerView.setAdapter(roomAdapter);
        roomsRecyclerView.setHasFixedSize(true);

        // Setup swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshRooms);
        swipeRefreshLayout.setColorSchemeResources(
            R.color.flame_orange,
            R.color.flame_red,
            R.color.flame_yellow
        );
    }

    private void setupNetworking() {
        android.util.Log.d(TAG, "🔧 Setting up networking with player name: " + playerName);
        lanDiscovery = new EnhancedLanDiscovery(this);
        lanMultiplayerManager = new LanMultiplayerManager(this);
        lanDiscovery.setListener(this);
        lanMultiplayerManager.setLocalPlayerName(playerName);
        lanDiscovery.setLocalPlayerName(playerName);
        android.util.Log.d(TAG, "✅ Network discovery initialized");
    }

    private void setupButtons() {
        createRoomButton.setOnClickListener(v -> createRoom());
        refreshButton.setOnClickListener(v -> refreshRooms());
        backButton.setOnClickListener(v -> finish());

        // Update player name when changed
        playerNameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                updatePlayerName();
            }
        });
    }

    private void updatePlayerName() {
        String newName = playerNameInput.getText().toString().trim();
        if (!newName.isEmpty() && !newName.equals(playerName)) {
            playerName = newName;
            lanDiscovery.setLocalPlayerName(playerName);
        }
    }

    private void startDiscovery() {
        if (isDiscovering) {
            android.util.Log.d(TAG, "⚠️ Discovery already in progress, skipping");
            return;
        }

        updatePlayerName();
        android.util.Log.d(TAG, "🔍 Starting LAN discovery for player: " + playerName);
        statusTextView.setText("🔍 Searching for Hot Potato games...");
        discoveryProgressBar.setVisibility(View.VISIBLE);
        refreshButton.setEnabled(false);

        // Run discovery on background thread
        networkExecutor.execute(() -> {
            try {
                lanDiscovery.startDiscovery();
                android.util.Log.d(TAG, "✅ Discovery started successfully");
            } catch (Exception e) {
                android.util.Log.e(TAG, "❌ Failed to start discovery: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    statusTextView.setText("❌ Failed to start discovery: " + e.getMessage());
                    discoveryProgressBar.setVisibility(View.GONE);
                    refreshButton.setEnabled(true);
                });
            }
        });
    }

    private void stopDiscovery() {
        if (!isDiscovering) {
            android.util.Log.d(TAG, "⚠️ No discovery to stop");
            return;
        }

        android.util.Log.d(TAG, "⏹️ Stopping LAN discovery");

        // Run stop on background thread
        networkExecutor.execute(() -> {
            try {
                lanDiscovery.stopDiscovery();
                android.util.Log.d(TAG, "✅ Discovery stopped successfully");
            } catch (Exception e) {
                android.util.Log.e(TAG, "❌ Error stopping discovery: " + e.getMessage(), e);
            }
        });

        runOnUiThread(() -> {
            discoveryProgressBar.setVisibility(View.GONE);
            refreshButton.setEnabled(true);
        });
    }

    private void refreshRooms() {
        android.util.Log.d(TAG, "🔄 Refreshing rooms - clearing current list of " + discoveredRooms.size() + " rooms");
        discoveredRooms.clear();
        roomAdapter.notifyDataSetChanged();
        updateUIState();

        stopDiscovery();

        // Small delay before restarting discovery
        statusTextView.postDelayed(() -> {
            if (!isDestroyed() && !isFinishing()) {
                android.util.Log.d(TAG, "🔄 Restarting discovery after refresh");
                startDiscovery();
            } else {
                android.util.Log.d(TAG, "⚠️ Activity destroyed/finishing, skipping discovery restart");
            }
        }, 500);
    }

    // Method for XML onClick attribute
    public void refreshRooms(View view) {
        refreshRooms();
    }

    private void createRoom() {
        updatePlayerName();

        if (playerName.trim().isEmpty()) {
            android.util.Log.w(TAG, "❌ Cannot create room - player name is empty");
            Toast.makeText(this, "Please enter your name first", Toast.LENGTH_SHORT).show();
            playerNameInput.requestFocus();
            return;
        }

        // Generate a random room code
        String roomCode = generateRoomCode();

        android.util.Log.d(TAG, "🏠 Creating room with code: " + roomCode + " for player: " + playerName);
        statusTextView.setText("🏠 Creating room: " + roomCode);
        createRoomButton.setEnabled(false);

        // Start hosting on background thread
        networkExecutor.execute(() -> {
            try {
                lanDiscovery.hostRoom(roomCode);
                android.util.Log.d(TAG, "✅ Room hosting initiated for: " + roomCode);
            } catch (Exception e) {
                android.util.Log.e(TAG, "❌ Failed to host room: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    statusTextView.setText("❌ Failed to create room: " + e.getMessage());
                    createRoomButton.setEnabled(true);
                    Toast.makeText(this, "Failed to create room: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void joinRoom(EnhancedLanDiscovery.DiscoveredRoom room) {
        updatePlayerName();

        if (playerName.trim().isEmpty()) {
            android.util.Log.w(TAG, "❌ Cannot join room - player name is empty");
            Toast.makeText(this, "Please enter your name first", Toast.LENGTH_SHORT).show();
            playerNameInput.requestFocus();
            return;
        }

        if (!room.isJoinable) {
            android.util.Log.w(TAG, "❌ Cannot join room - room is not joinable: " + room.roomCode);
            Toast.makeText(this, "Room is full", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d(TAG, "📡 Attempting to join room: " + room.roomCode + " hosted by: " + room.hostName +
                           " at " + room.hostAddress + ":" + room.hostPort);
        statusTextView.setText("🚪 Joining " + room.hostName + "'s room...");

        // Attempt to join the room on background thread
        networkExecutor.execute(() -> {
            try {
                lanDiscovery.joinRoom(room);
                android.util.Log.d(TAG, "✅ Join request sent for room: " + room.roomCode);
            } catch (Exception e) {
                android.util.Log.e(TAG, "❌ Failed to join room: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    statusTextView.setText("❌ Failed to join room: " + e.getMessage());
                    Toast.makeText(this, "Failed to join room: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void launchGameActivity(String roomCode, boolean isHost) {
        android.util.Log.d(TAG, "🚀 Launching GameActivity:");
        android.util.Log.d(TAG, "   Room Code: " + roomCode);
        android.util.Log.d(TAG, "   Is Host: " + isHost);
        android.util.Log.d(TAG, "   Player Name: " + playerName);

        // Transfer the connection state to GameActivity
        transferManager = lanMultiplayerManager;
        android.util.Log.d(TAG, "🔄 Transferring connection state to GameActivity");

        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("gameMode", "multiplayer");
        intent.putExtra("roomCode", roomCode);
        intent.putExtra("isHost", isHost);
        intent.putExtra("playerName", playerName);
        intent.putExtra("useEnhancedLan", true);
        intent.putExtra("hasTransferredConnection", true);

        ArrayList<String> playerNames = new ArrayList<>();
        playerNames.add(playerName);
        intent.putStringArrayListExtra("playerNames", playerNames);

        startActivity(intent);
    }

    private String generateRoomCode() {
        // Generate a 4-digit room code like Mini Militia
        Random random = new Random();
        return String.format("%04d", random.nextInt(10000));
    }

    private void updateUIState() {
        swipeRefreshLayout.setRefreshing(false);

        if (discoveredRooms.isEmpty()) {
            noRoomsCard.setVisibility(View.VISIBLE);
            roomsRecyclerView.setVisibility(View.GONE);

            if (isDiscovering) {
                statusTextView.setText("🔍 Searching for games...");
            } else {
                statusTextView.setText("📡 No games found. Create one or refresh to search again.");
            }
        } else {
            noRoomsCard.setVisibility(View.GONE);
            roomsRecyclerView.setVisibility(View.VISIBLE);

            int joinableRooms = 0;
            for (EnhancedLanDiscovery.DiscoveredRoom room : discoveredRooms) {
                if (room.isJoinable) joinableRooms++;
            }

            statusTextView.setText("🎮 Found " + discoveredRooms.size() + " games (" + joinableRooms + " joinable)");
        }
    }

    // EnhancedLanDiscovery.LanDiscoveryListener implementation
    @Override
    public void onRoomsDiscovered(List<EnhancedLanDiscovery.DiscoveredRoom> rooms) {
        android.util.Log.d(TAG, "🎮 Discovered " + rooms.size() + " rooms:");
        for (EnhancedLanDiscovery.DiscoveredRoom room : rooms) {
            android.util.Log.d(TAG, "   Room: " + room.roomCode + " by " + room.hostName +
                               " at " + room.hostAddress + ":" + room.hostPort +
                               " (joinable: " + room.isJoinable + ")");
        }

        runOnUiThread(() -> {
            discoveredRooms.clear();
            discoveredRooms.addAll(rooms);
            roomAdapter.notifyDataSetChanged();
            updateUIState();
        });
    }

    @Override
    public void onRoomJoined(String roomCode, String hostName) {
        android.util.Log.d(TAG, "✅ Successfully joined room: " + roomCode + " hosted by: " + hostName + " as CLIENT");

        runOnUiThread(() -> {
            statusTextView.setText("✅ Joined " + hostName + "'s room!");
            Toast.makeText(this, "Successfully joined the game!", Toast.LENGTH_SHORT).show();

            // Launch game activity as CLIENT
            launchGameActivity(roomCode, false);
        });
    }

    @Override
    public void onRoomHosted(String roomCode, int port) {
        android.util.Log.d(TAG, "✅ Successfully hosted room: " + roomCode + " on port: " + port + " as HOST");

        runOnUiThread(() -> {
            statusTextView.setText("✅ Room " + roomCode + " created! Waiting for players...");
            createRoomButton.setEnabled(true);
            Toast.makeText(this, "Room created! Share code: " + roomCode, Toast.LENGTH_LONG).show();

            // Update multiplayer manager state
            if (lanMultiplayerManager != null) {
                lanMultiplayerManager.setLocalPlayerName(playerName);
            }

            // Launch game activity as HOST
            launchGameActivity(roomCode, true);
        });
    }

    @Override
    public void onPlayerJoined(String playerId, String playerName) {
        android.util.Log.d(TAG, "🎮 Player joined: " + playerName + " (ID: " + playerId + ")");

        runOnUiThread(() -> {
            Toast.makeText(this, playerName + " joined the room!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onPlayerLeft(String playerId, String playerName) {
        android.util.Log.d(TAG, "👋 Player left: " + playerName + " (ID: " + playerId + ")");

        runOnUiThread(() -> {
            Toast.makeText(this, playerName + " left the room", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onGameStarted() {
        runOnUiThread(() -> {
            statusTextView.setText("🎮 Game starting!");
        });
    }

    @Override
    public void onGameDataReceived(String data) {
        // Handle game data if needed
    }

    @Override
    public void onConnectionError(String error) {
        android.util.Log.e(TAG, "❌ Connection error: " + error);

        runOnUiThread(() -> {
            statusTextView.setText("❌ " + error);
            createRoomButton.setEnabled(true);
            Toast.makeText(this, "Connection error: " + error, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onDiscoveryStateChanged(boolean isDiscovering) {
        android.util.Log.d(TAG, "🔄 Discovery state changed: " + (isDiscovering ? "STARTED" : "STOPPED"));

        runOnUiThread(() -> {
            this.isDiscovering = isDiscovering;

            if (isDiscovering) {
                discoveryProgressBar.setVisibility(View.VISIBLE);
                refreshButton.setEnabled(false);
            } else {
                discoveryProgressBar.setVisibility(View.GONE);
                refreshButton.setEnabled(true);
            }

            updateUIState();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Restart discovery if we came back from a game
        if (!isDiscovering) {
            refreshRooms();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop discovery to save battery
        stopDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        android.util.Log.d(TAG, "🧹 Cleaning up RoomBrowserActivity");

        // Only cleanup if we're not transferring to GameActivity
        if (transferManager == null) {
            if (lanDiscovery != null) {
                lanDiscovery.disconnect();
            }
            if (lanMultiplayerManager != null) {
                lanMultiplayerManager.cleanup();
            }
        } else {
            android.util.Log.d(TAG, "🔄 Skipping cleanup - connection transferred to GameActivity");
        }

        if (networkExecutor != null && !networkExecutor.isShutdown()) {
            networkExecutor.shutdown();
        }
    }

    @Override
    public void onBackPressed() {
        stopDiscovery();
        super.onBackPressed();
    }
}
