package com.tatoalu.hotpotato;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Main Activity - Hot Potato Game Launcher
 * Simplified for LAN-only Hot Potato gameplay
 */
public class MainActivity extends AppCompatActivity {
    private Spinner playerCountSpinner;
    private EditText name1, name2, name3, name4;
    private MaterialButton startButton, lanBrowserButton;
    private boolean introAnimated = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupSpinners();
        setupButtons();
    }

    private void initializeViews() {
        playerCountSpinner = findViewById(R.id.playerCountSpinner);
        name1 = findViewById(R.id.name1);
        name2 = findViewById(R.id.name2);
        name3 = findViewById(R.id.name3);
        name4 = findViewById(R.id.name4);
        startButton = findViewById(R.id.startButton);
        lanBrowserButton = findViewById(R.id.lanBrowserButton);

        // Set default names
        name1.setText("Player 1");
        name2.setText("Player 2");
        name3.setText("Player 3");
        name4.setText("Player 4");
    }

    private void setupSpinners() {
        // Player count spinner
        ArrayAdapter<CharSequence> playerCountAdapter = ArrayAdapter.createFromResource(this,
                R.array.player_counts, android.R.layout.simple_spinner_item);
        playerCountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        playerCountSpinner.setAdapter(playerCountAdapter);

        playerCountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int playerCount = Integer.parseInt(parent.getItemAtPosition(position).toString());
                updateNameFieldsVisibility(playerCount);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Initialize visibility based on default selection
        int initialCount = Integer.parseInt(playerCountSpinner.getSelectedItem().toString());
        updateNameFieldsVisibility(initialCount);
    }

    private void setupButtons() {
        startButton.setOnClickListener(v -> startLocalGame());
        lanBrowserButton.setOnClickListener(v -> openLanBrowser());
    }

    private void updateNameFieldsVisibility(int playerCount) {
        name1.setVisibility(View.VISIBLE);
        name2.setVisibility(playerCount >= 2 ? View.VISIBLE : View.GONE);
        name3.setVisibility(playerCount >= 3 ? View.VISIBLE : View.GONE);
        name4.setVisibility(playerCount >= 4 ? View.VISIBLE : View.GONE);
    }

    private void startLocalGame() {
        int playerCount = Integer.parseInt(playerCountSpinner.getSelectedItem().toString());
        ArrayList<String> names = collectNames(playerCount);

        Intent intent = new Intent(this, GameActivity.class);
        intent.putStringArrayListExtra("playerNames", names);
        intent.putExtra("gameMode", "local");
        startActivity(intent);
    }

    private void openLanBrowser() {
        String playerName = safeName(name1.getText().toString(), "Player 1");

        Intent intent = new Intent(this, RoomBrowserActivity.class);
        intent.putExtra("playerName", playerName);
        startActivity(intent);
    }

    private ArrayList<String> collectNames(int playerCount) {
        ArrayList<String> names = new ArrayList<>();
        EditText[] nameFields = {name1, name2, name3, name4};

        for (int i = 0; i < playerCount; i++) {
            String name = safeName(nameFields[i].getText().toString(), "Player " + (i + 1));
            names.add(name);
        }

        return names;
    }

    private String safeName(String input, String fallback) {
        if (input == null || input.trim().isEmpty()) {
            return fallback;
        }
        return input.trim();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!introAnimated) {
            runIntroAnimations();
            introAnimated = true;
        }
    }

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
        MaterialButton[] buttons = {startButton, lanBrowserButton};
        for (int i = 0; i < buttons.length; i++) {
            MaterialButton button = buttons[i];
            if (button != null) {
                button.setAlpha(0f);
                button.setScaleX(0.8f);
                button.setScaleY(0.8f);
                button.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(600)
                        .setStartDelay(200 + i * 100)
                        .setInterpolator(new OvershootInterpolator())
                        .start();
            }
        }

        // Animate other UI elements
        View[] otherViews = {playerCountSpinner, findViewById(R.id.namesCard)};
        for (int i = 0; i < otherViews.length; i++) {
            View view = otherViews[i];
            if (view != null) {
                view.setAlpha(0f);
                view.setTranslationX(30 * dp);
                view.animate()
                        .alpha(1f)
                        .translationX(0f)
                        .setDuration(500)
                        .setStartDelay(400 + i * 150)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        }
    }
}
