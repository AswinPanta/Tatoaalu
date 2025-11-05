package com.tatoalu.hotpotato;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.airbnb.lottie.LottieAnimationView;
import java.util.List;

// लिडरबोर्ड गतिविधि (Leaderboard Activity)
public class LeaderboardActivity extends AppCompatActivity {

    // UI घटकहरू (UI Components)
    private RecyclerView leaderboardRecycler;
    private LottieAnimationView trophyAnim;
    private LeaderboardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        // UI घटकहरू प्रारम्भ गर्नुहोस् (Initialize UI Components)
        leaderboardRecycler = findViewById(R.id.leaderboardRecycler);
        trophyAnim = findViewById(R.id.trophyAnim);

        // एडाप्टर सेटअप गर्नुहोस् (Setup Adapter)
        adapter = new LeaderboardAdapter();
        leaderboardRecycler.setLayoutManager(new LinearLayoutManager(this));
        leaderboardRecycler.setAdapter(adapter);

        // लिडरबोर्ड लोड गर्नुहोस् (Load Leaderboard)
        loadLeaderboard();
    }

    // लिडरबोर्ड डाटा लोड गर्ने विधि (Method to load leaderboard data)
    private void loadLeaderboard() {
        FirebaseManager.getInstance().getLeaderboard(new LeaderboardManager.OnScoresLoadedListener() {
            @Override
            public void onScoresLoaded(List<LeaderboardManager.Score> scores) {
                runOnUiThread(() -> {
                    adapter.setScores(scores);
                    trophyAnim.playAnimation(); // डाटा लोड हुँदा ट्रफी एनिमेसन चलाउनुहोस्
                });
            }

            @Override
            public void onError(String error) {
                Log.e("LeaderboardActivity", "Error loading leaderboard: " + error);
            }
        }, "local_quick_game");
    }
}