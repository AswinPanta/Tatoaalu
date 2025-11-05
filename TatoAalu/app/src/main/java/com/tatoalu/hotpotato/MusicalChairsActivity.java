package com.tatoalu.hotpotato;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.animation.Animator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.media.MediaPlayer;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.airbnb.lottie.LottieAnimationView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// संगीतमय कुर्सी खेल गतिविधि (Musical Chairs Game Activity)
public class MusicalChairsActivity extends AppCompatActivity {
    private static final String TAG = "MusicalChairsActivity";
    
    // UI घटकहरू (UI Components)
    private TextView statusText;
    private TextView roundText;
    private TextView playersLeftText;
    private Button startRoundButton;
    private Button[] chairButtons;
    private ImageView[] chairImages;
    private LottieAnimationView musicAnimation;
    private ConstraintLayout gameContainer;
    private View endButtonsContainer;
    private View outcomeOverlay;
    private TextView outcomeText;
    private LeaderboardManager leaderboardManager;
    
    // खेल अवस्था (Game State)
    private List<String> playerNames = new ArrayList<>();
    private List<String> activePlayers = new ArrayList<>();
    private int currentRound = 1;
    private int chairCount;
    private boolean musicPlaying = false;
    private boolean roundInProgress = false;
    private String eliminatedPlayer = "";
    
    // खेल नियन्त्रण (Game Control)
    private Handler gameHandler = new Handler();
    private Random random = new Random();
    private MediaPlayer musicPlayer;
    
    // समय कन्फिगरेसन (Time Configuration)
    private static final int MIN_MUSIC_TIME = 5000; // 5 सेकेन्ड
    private static final int MAX_MUSIC_TIME = 15000; // 15 सेकेन्ड
    private static final int CHAIR_SELECTION_TIME = 3000; // कुर्सी छनोट गर्न 3 सेकेन्ड

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musical_chairs);
        
        initializeViews();
        setupGame();
        setupButtons();
        
        leaderboardManager = new LeaderboardManager();
    }
    
    private void initializeViews() {
        statusText = findViewById(R.id.statusText);
        roundText = findViewById(R.id.roundText);
        playersLeftText = findViewById(R.id.playersLeftText);
        startRoundButton = findViewById(R.id.startRoundButton);
        musicAnimation = findViewById(R.id.musicAnimation);
        gameContainer = findViewById(R.id.gameContainer);
        endButtonsContainer = findViewById(R.id.endButtonsContainer);
        outcomeOverlay = findViewById(R.id.outcomeOverlay);
        outcomeText = findViewById(R.id.outcomeText);
        
        // Initialize chair buttons and images
        chairButtons = new Button[4];
        chairImages = new ImageView[4];
        chairButtons[0] = findViewById(R.id.chair1Button);
        chairButtons[1] = findViewById(R.id.chair2Button);
        chairButtons[2] = findViewById(R.id.chair3Button);
        chairButtons[3] = findViewById(R.id.chair4Button);
        chairImages[0] = findViewById(R.id.chair1Image);
        chairImages[1] = findViewById(R.id.chair2Image);
        chairImages[2] = findViewById(R.id.chair3Image);
        chairImages[3] = findViewById(R.id.chair4Image);
    }
    
    private void setupGame() {
        // Get player names from intent - handle both ArrayList and String array
        ArrayList<String> names = null;
        try {
            names = getIntent().getStringArrayListExtra("names");
        } catch (Exception e) {
            // If ArrayList fails, try String array
            String[] nameArray = getIntent().getStringArrayExtra("names");
            if (nameArray != null) {
                names = new ArrayList<>();
                for (String name : nameArray) {
                    names.add(name);
                }
            }
        }
        
        int playerCount = getIntent().getIntExtra("players", 4);
        
        if (names != null && !names.isEmpty()) {
            playerNames.addAll(names);
        } else {
            // Default names if none provided
            for (int i = 1; i <= playerCount; i++) {
                playerNames.add("Player " + i);
            }
        }
        
        activePlayers.addAll(playerNames);
        chairCount = activePlayers.size() - 1; // Always one less chair than players
        
        updateGameDisplay();
        setupChairs();
    }
    
    private void setupChairs() {
        // Show/hide chairs based on current chair count
        for (int i = 0; i < chairButtons.length; i++) {
            if (i < chairCount) {
                chairButtons[i].setVisibility(View.VISIBLE);
                chairImages[i].setVisibility(View.VISIBLE);
                chairButtons[i].setEnabled(false);
                chairButtons[i].setAlpha(0.7f);
            } else {
                chairButtons[i].setVisibility(View.GONE);
                chairImages[i].setVisibility(View.GONE);
            }
        }
    }
    
    private void setupButtons() {
        startRoundButton.setOnClickListener(v -> startRound());
        
        // Setup chair button listeners
        for (int i = 0; i < chairButtons.length; i++) {
            final int chairIndex = i;
            chairButtons[i].setOnClickListener(v -> selectChair(chairIndex));
        }
        
        // Setup end game buttons
        findViewById(R.id.restartButton).setOnClickListener(v -> restartGame());
        findViewById(R.id.homeButton).setOnClickListener(v -> finish());
        findViewById(R.id.leaderboardButton).setOnClickListener(v -> {
            Intent intent = new Intent(MusicalChairsActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });
    }
    
    private void startRound() {
        if (activePlayers.size() <= 1) {
            endGame();
            return;
        }
        
        roundInProgress = true;
        musicPlaying = true;
        startRoundButton.setVisibility(View.GONE);
        
        // Enable chair selection
        for (int i = 0; i < chairCount; i++) {
            chairButtons[i].setEnabled(true);
            chairButtons[i].setAlpha(1.0f);
        }
        
        // Start music animation
        musicAnimation.setVisibility(View.VISIBLE);
        musicAnimation.playAnimation();
        
        statusText.setText("🎵 Music is playing! Get ready...");
        
        // Schedule music stop
        int musicDuration = MIN_MUSIC_TIME + random.nextInt(MAX_MUSIC_TIME - MIN_MUSIC_TIME);
        gameHandler.postDelayed(this::stopMusic, musicDuration);
    }
    
    private void stopMusic() {
        musicPlaying = false;
        musicAnimation.pauseAnimation();
        musicAnimation.setVisibility(View.GONE);
        
        statusText.setText("🪑 Music stopped! Find a chair quickly!");
        
        // Start chair selection countdown
        gameHandler.postDelayed(this::eliminatePlayer, CHAIR_SELECTION_TIME);
    }
    
    private void selectChair(int chairIndex) {
        if (!roundInProgress || musicPlaying) return;
        
        // Disable the selected chair
        chairButtons[chairIndex].setEnabled(false);
        chairButtons[chairIndex].setAlpha(0.5f);
        chairButtons[chairIndex].setText("Taken");
        
        // Animate chair selection
        animateChairSelection(chairIndex);
        
        // Check if all chairs are taken
        boolean allChairsTaken = true;
        for (int i = 0; i < chairCount; i++) {
            if (chairButtons[i].isEnabled()) {
                allChairsTaken = false;
                break;
            }
        }
        
        if (allChairsTaken) {
            gameHandler.removeCallbacksAndMessages(null);
            eliminatePlayer();
        }
    }
    
    private void animateChairSelection(int chairIndex) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(chairImages[chairIndex], "scaleX", 1.0f, 1.2f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(chairImages[chairIndex], "scaleY", 1.0f, 1.2f, 1.0f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }
    
    private void eliminatePlayer() {
        roundInProgress = false;
        
        // Find players who didn't get chairs
        List<String> playersWithoutChairs = new ArrayList<>();
        List<String> playersWithChairs = new ArrayList<>();
        
        // For simplicity, randomly eliminate one player if not all chairs taken
        // In a real implementation, you'd track which players actually selected chairs
        if (activePlayers.size() > chairCount) {
            Collections.shuffle(activePlayers);
            eliminatedPlayer = activePlayers.get(0);
            playersWithoutChairs.add(eliminatedPlayer);
            
            for (String player : activePlayers) {
                if (!player.equals(eliminatedPlayer)) {
                    playersWithChairs.add(player);
                }
            }
        }
        
        // Remove eliminated player
        activePlayers.remove(eliminatedPlayer);
        chairCount = activePlayers.size() - 1;
        
        // Update display
        statusText.setText("❌ " + eliminatedPlayer + " was eliminated!");
        
        // Animate elimination
        animateElimination();
        
        // Prepare for next round
        gameHandler.postDelayed(() -> {
            if (activePlayers.size() > 1) {
                prepareNextRound();
            } else {
                endGame();
            }
        }, 2000);
    }
    
    private void animateElimination() {
        // Flash the screen red briefly
        View flashOverlay = findViewById(R.id.flashOverlay);
        if (flashOverlay != null) {
            flashOverlay.setVisibility(View.VISIBLE);
            flashOverlay.setAlpha(0.3f);
            
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(flashOverlay, "alpha", 0.3f, 0.0f);
            fadeOut.setDuration(1000);
            fadeOut.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    flashOverlay.setVisibility(View.GONE);
                }
                @Override public void onAnimationStart(Animator animation) {}
                @Override public void onAnimationCancel(Animator animation) {}
                @Override public void onAnimationRepeat(Animator animation) {}
            });
            fadeOut.start();
        }
    }
    
    private void prepareNextRound() {
        currentRound++;
        updateGameDisplay();
        setupChairs();
        
        // Reset chair buttons
        for (int i = 0; i < chairCount; i++) {
            chairButtons[i].setEnabled(false);
            chairButtons[i].setAlpha(0.7f);
            chairButtons[i].setText("Chair " + (i + 1));
        }
        
        startRoundButton.setVisibility(View.VISIBLE);
        statusText.setText("Ready for Round " + currentRound + "?");
    }
    
    private void endGame() {
        String winner = activePlayers.get(0);
        
        // Update wins for the winner
        updateWins(winner);
        
        // Show outcome
        outcomeText.setText("🏆 " + winner + " wins Musical Chairs!");
        outcomeOverlay.setVisibility(View.VISIBLE);
        endButtonsContainer.setVisibility(View.VISIBLE);
        
        statusText.setText("Game Over! " + winner + " is the Musical Chairs champion!");
    }
    
    private void updateGameDisplay() {
        roundText.setText("Round " + currentRound);
        playersLeftText.setText(activePlayers.size() + " players left");
        
        StringBuilder playersText = new StringBuilder("Players: ");
        for (int i = 0; i < activePlayers.size(); i++) {
            playersText.append(activePlayers.get(i));
            if (i < activePlayers.size() - 1) {
                playersText.append(", ");
            }
        }
        
        TextView playersListText = findViewById(R.id.playersListText);
        if (playersListText != null) {
            playersListText.setText(playersText.toString());
        }
    }
    
    private void updateWins(String winnerName) {
        android.content.SharedPreferences prefs = getSharedPreferences("musical_chairs_wins", Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        
        int currentWins = prefs.getInt(winnerName, 0);
        int newWins = currentWins + 1;
        editor.putInt(winnerName, newWins);
        editor.apply();
        
        // Update Firebase leaderboard
        FirebaseManager.getInstance().submitScore(winnerName, winnerName, newWins, "musical_chairs");
    }
    
    private void restartGame() {
        recreate();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (musicPlayer != null) {
            musicPlayer.release();
            musicPlayer = null;
        }
        gameHandler.removeCallbacksAndMessages(null);
    }
}