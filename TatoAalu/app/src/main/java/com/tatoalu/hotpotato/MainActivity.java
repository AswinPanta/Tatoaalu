package com.tatoalu.hotpotato;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

// मुख्य गतिविधि - खेल सुरु गर्ने ठाउँ (Main Activity - Game Starting Place)
public class MainActivity extends AppCompatActivity implements UnifiedMultiplayerManager.UnifiedMultiplayerListener {
    private Spinner playerCountSpinner;
    private Spinner gameModeSpinner;
    private Spinner connectionModeSpinner;
    private EditText name1, name2, name3, name4;
    private MaterialButton hostButton, joinButton;
    private boolean introAnimated = false;
    
    // एकीकृत मल्टिप्लेयर प्रबन्धक (Unified Multiplayer Manager)
    private UnifiedMultiplayerManager multiplayerManager;
    private AlertDialog roomCodeDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupSpinners();
        setupButtons();
        
        // मल्टिप्लेयर प्रबन्धक सुरु गर्नुहोस् (Initialize multiplayer manager)
        String playerName = safeName(name1.getText().toString(), "खेलाडी");
        multiplayerManager = new UnifiedMultiplayerManager(this);
        multiplayerManager.setPlayerName(playerName);
        multiplayerManager.setMultiplayerListener(this);
    }
    
    // दृश्य तत्वहरू सुरु गर्नुहोस् (Initialize views)
    private void initializeViews() {
        playerCountSpinner = findViewById(R.id.playerCountSpinner);
        gameModeSpinner = findViewById(R.id.gameModeSpinner);
        connectionModeSpinner = findViewById(R.id.connectionModeSpinner);
        name1 = findViewById(R.id.name1);
        name2 = findViewById(R.id.name2);
        name3 = findViewById(R.id.name3);
        name4 = findViewById(R.id.name4);
        hostButton = findViewById(R.id.hostButton);
        joinButton = findViewById(R.id.joinButton);
    }
    
    // स्पिनरहरू सेटअप गर्नुहोस् (Setup spinners)
    private void setupSpinners() {
        // खेलाडी संख्या स्पिनर (Player count spinner)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.player_counts, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        playerCountSpinner.setAdapter(adapter);

        // खेल मोड स्पिनर (Game mode spinner)
        ArrayAdapter<CharSequence> gameModeAdapter = ArrayAdapter.createFromResource(this,
                R.array.game_modes, android.R.layout.simple_spinner_item);
        gameModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gameModeSpinner.setAdapter(gameModeAdapter);
        
        // जडान मोड स्पिनर (Connection mode spinner)
        ArrayAdapter<CharSequence> connectionModeAdapter = ArrayAdapter.createFromResource(this,
                R.array.connection_modes, android.R.layout.simple_spinner_item);
        connectionModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        connectionModeSpinner.setAdapter(connectionModeAdapter);

        playerCountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int count = Integer.parseInt(parent.getItemAtPosition(position).toString());
                updateNameFieldsVisibility(count);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        
        // प्रारम्भिक चयनको आधारमा दृश्यता सुरु गर्नुहोस् (Initialize visibility based on default selection)
        int initialCount = Integer.parseInt(playerCountSpinner.getSelectedItem().toString());
        updateNameFieldsVisibility(initialCount);
    }
    
    // बटनहरू सेटअप गर्नुहोस् (Setup buttons)
    private void setupButtons() {
        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLocalGame();
            }
        });

        hostButton.setOnClickListener(v -> {
            if (allNamesEmpty()) {
                startLocalGame();
            } else {
                hostMultiplayerGame();
            }
        });

        joinButton.setOnClickListener(v -> {
            if (allNamesEmpty()) {
                Toast.makeText(this, "कम्तिमा एक नाम प्रविष्ट गर्नुहोस्", Toast.LENGTH_SHORT).show();
            } else {
                joinMultiplayerGame();
            }
        });

        findViewById(R.id.leaderboardButton).setOnClickListener(v -> {
            startActivity(new Intent(this, LeaderboardActivity.class));
        });
    }

    // स्थानीय खेल सुरु गर्नुहोस् (Start local game)
    private void startLocalGame() {
        int playerCount = Integer.parseInt(playerCountSpinner.getSelectedItem().toString());
        String gameMode = gameModeSpinner.getSelectedItem().toString();
        
        ArrayList<String> names = collectNames(playerCount);
        
        Intent intent = new Intent(this, GameActivity.class);
        intent.putStringArrayListExtra("playerNames", names);
        intent.putExtra("gameMode", gameMode);
        intent.putExtra("useMultiplayerManager", false);
        startActivity(intent);
        
        if (gameMode.equals("Musical Chairs")) {
            Intent musicalIntent = new Intent(this, MusicalChairsActivity.class);
            musicalIntent.putStringArrayListExtra("playerNames", names);
            startActivity(musicalIntent);
        }
    }

    // मल्टिप्लेयर खेल होस्ट गर्नुहोस् (Host multiplayer game)
    private void hostMultiplayerGame() {
        String playerName = safeName(name1.getText().toString(), "खेलाडी");
        int maxPlayers = Integer.parseInt(playerCountSpinner.getSelectedItem().toString());
        
        multiplayerManager.setPlayerName(playerName);
        UnifiedMultiplayerManager.ConnectionMode mode = getSelectedConnectionMode();
        multiplayerManager.setConnectionMode(mode);
        
        setButtonsEnabled(false);
        multiplayerManager.createRoom(maxPlayers);
    }

    // मल्टिप्लेयर खेलमा सामेल हुनुहोस् (Join multiplayer game)
    private void joinMultiplayerGame() {
        String playerName = safeName(name1.getText().toString(), "खेलाडी");
        
        multiplayerManager.setPlayerName(playerName);
        UnifiedMultiplayerManager.ConnectionMode mode = getSelectedConnectionMode();
        multiplayerManager.setConnectionMode(mode);
        
        setButtonsEnabled(false);
        
        if (mode == UnifiedMultiplayerManager.ConnectionMode.FIREBASE_ONLY) {
            showRoomCodeDialog();
        } else {
            // LAN वा Auto मोडको लागि (For LAN or Auto mode)
            multiplayerManager.joinRoom("AUTO");
        }
    }

    // चयनित जडान मोड प्राप्त गर्नुहोस् (Get selected connection mode)
    private UnifiedMultiplayerManager.ConnectionMode getSelectedConnectionMode() {
        int position = connectionModeSpinner.getSelectedItemPosition();
        switch (position) {
            case 0: return UnifiedMultiplayerManager.ConnectionMode.LAN_ONLY;
            case 1: return UnifiedMultiplayerManager.ConnectionMode.FIREBASE_ONLY;
            default: return UnifiedMultiplayerManager.ConnectionMode.LAN_ONLY;
        }
    }

    // कोठा कोड संवाद देखाउनुहोस् (Show room code dialog)
    private void showRoomCodeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_room_code, null);
        TextInputEditText roomCodeInput = dialogView.findViewById(R.id.roomCodeInput);
        
        builder.setView(dialogView)
                .setTitle("कोठा कोड प्रविष्ट गर्नुहोस्")
                .setPositiveButton("सामेल हुनुहोस्", (dialog, which) -> {
                    String roomCode = roomCodeInput.getText().toString().trim();
                    if (!roomCode.isEmpty()) {
                        multiplayerManager.joinRoom(roomCode);
                    }
                })
                .setNegativeButton("रद्द गर्नुहोस्", (dialog, which) -> {
                    setButtonsEnabled(true);
                });
        
        roomCodeDialog = builder.create();
        roomCodeDialog.show();
    }

    // बटनहरू सक्षम/असक्षम गर्नुहोस् (Enable/disable buttons)
    private void setButtonsEnabled(boolean enabled) {
        hostButton.setEnabled(enabled);
        joinButton.setEnabled(enabled);
    }

    // UnifiedMultiplayerManager.UnifiedMultiplayerListener कार्यान्वयन (Implementation)
    @Override
    public void onConnectionStateChanged(UnifiedMultiplayerManager.ConnectionState state, 
                                       UnifiedMultiplayerManager.NetworkType networkType) {
        runOnUiThread(() -> {
            switch (state) {
                case CONNECTED:
                    Toast.makeText(this, "जडान भयो: " + networkType, Toast.LENGTH_SHORT).show();
                    break;
                case CONNECTING:
                    Toast.makeText(this, "जडान हुँदै...", Toast.LENGTH_SHORT).show();
                    break;
                case ERROR:
                    Toast.makeText(this, "जडान त्रुटि", Toast.LENGTH_SHORT).show();
                    setButtonsEnabled(true);
                    break;
            }
        });
    }

    @Override
    public void onRoomCreated(String roomCode, UnifiedMultiplayerManager.NetworkType networkType) {
        runOnUiThread(() -> {
            String networkTypeStr = networkType.toString();
            String message = "कोठा सिर्जना भयो: " + roomCode + " (" + networkTypeStr + ")";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            startGameActivity("multiplayer");
        });
    }

    @Override
    public void onRoomJoined(String roomCode, UnifiedMultiplayerManager.NetworkType networkType) {
        runOnUiThread(() -> {
            String networkTypeStr = networkType.toString();
            Toast.makeText(this, "कोठामा सामेल भयो: " + roomCode + " (" + networkTypeStr + ")", 
                         Toast.LENGTH_LONG).show();
            startGameActivity("multiplayer");
        });
    }

    // खेल गतिविधि सुरु गर्नुहोस् (Start game activity)
    private void startGameActivity(String mode) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("gameMode", mode);
        intent.putExtra("useUnifiedMultiplayerManager", true);
        startActivity(intent);
    }

    @Override
    public void onPlayerJoined(String playerId, String playerName, UnifiedMultiplayerManager.NetworkType networkType) {
        // खेलाडी सामेल भयो (Player joined)
    }

    @Override
    public void onPlayerLeft(String playerId, String playerName, UnifiedMultiplayerManager.NetworkType networkType) {
        // खेलाडी छोड्यो (Player left)
    }

    @Override
    public void onHostChanged(String newHostId, UnifiedMultiplayerManager.NetworkType networkType) {
        // होस्ट परिवर्तन भयो (Host changed)
    }

    @Override
    public void onNetworkError(String error, UnifiedMultiplayerManager.NetworkType networkType) {
        runOnUiThread(() -> {
            Toast.makeText(this, "नेटवर्क त्रुटि: " + error, Toast.LENGTH_SHORT).show();
            setButtonsEnabled(true);
        });
    }


    // नाम फिल्डहरूको दृश्यता अपडेट गर्नुहोस् (Update name fields visibility)
    private void updateNameFieldsVisibility(int count) {
        name1.setVisibility(View.VISIBLE);
        name2.setVisibility(count >= 2 ? View.VISIBLE : View.GONE);
        name3.setVisibility(count >= 3 ? View.VISIBLE : View.GONE);
        name4.setVisibility(count >= 4 ? View.VISIBLE : View.GONE);
    }

    // सबै नामहरू खाली छन् कि छैनन् जाँच गर्नुहोस् (Check if all names are empty)
    private boolean allNamesEmpty() {
        return name1.getText().toString().trim().isEmpty() &&
               name2.getText().toString().trim().isEmpty() &&
               name3.getText().toString().trim().isEmpty() &&
               name4.getText().toString().trim().isEmpty();
    }

    // नामहरू सङ्कलन गर्नुहोस् (Collect names)
    private ArrayList<String> collectNames(int count) {
        ArrayList<String> names = new ArrayList<>();
        names.add(safeName(name1.getText().toString(), "खेलाडी १"));
        if (count >= 2) names.add(safeName(name2.getText().toString(), "खेलाडी २"));
        if (count >= 3) names.add(safeName(name3.getText().toString(), "खेलाडी ३"));
        if (count >= 4) names.add(safeName(name4.getText().toString(), "खेलाडी ४"));
        return names;
    }

    // सुरक्षित नाम (Safe name)
    private String safeName(String s, String fallback) {
        return (s == null || s.trim().isEmpty()) ? fallback : s.trim();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !introAnimated) {
            runIntroAnimations();
            introAnimated = true;
        }
    }

    // परिचय एनिमेसनहरू चलाउनुहोस् (Run intro animations)
    private void runIntroAnimations() {
        float dp = getResources().getDisplayMetrics().density;
        
        // Animate title
        View titleView = findViewById(R.id.title);
        if (titleView != null) {
            titleView.setAlpha(0f);
            titleView.setTranslationY(-50 * dp);
            titleView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
        
        // Animate buttons with stagger
        View[] buttons = {hostButton, joinButton};
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] != null) {
                buttons[i].setAlpha(0f);
                buttons[i].setScaleX(0.8f);
                buttons[i].setScaleY(0.8f);
                buttons[i].animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(600)
                        .setStartDelay(200 + i * 100)
                        .setInterpolator(new OvershootInterpolator(1.2f))
                        .start();
            }
        }
        
        // Animate spinners
        View[] spinners = {playerCountSpinner, gameModeSpinner, connectionModeSpinner};
        for (int i = 0; i < spinners.length; i++) {
            if (spinners[i] != null) {
                spinners[i].setAlpha(0f);
                spinners[i].setTranslationX(50 * dp);
                spinners[i].animate()
                        .alpha(1f)
                        .translationX(0f)
                        .setDuration(500)
                        .setStartDelay(400 + i * 100)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (multiplayerManager != null) {
            multiplayerManager.cleanup();
        }
    }
}