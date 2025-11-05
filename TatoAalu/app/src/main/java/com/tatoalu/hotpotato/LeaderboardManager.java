package com.tatoalu.hotpotato;

import android.util.Log;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// लिडरबोर्ड व्यवस्थापक (Leaderboard Manager)
public class LeaderboardManager {

    private static final String TAG = "LeaderboardManager";
    private FirebaseFirestore db;

    // कन्स्ट्रक्टर (Constructor)
    public LeaderboardManager() {
        db = FirebaseFirestore.getInstance();
    }

    // स्कोर पेश गर्ने विधि (Method to submit score)
    public void submitScore(String userId, String playerName, int score, String gameMode) {
        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("userId", userId);
        scoreData.put("playerName", playerName);
        scoreData.put("score", score);
        scoreData.put("gameMode", gameMode);
        scoreData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("scores").add(scoreData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Score submitted with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding score", e);
                });
    }

    // शीर्ष स्कोरहरू प्राप्त गर्ने विधि (Method to get top scores)
    public void getTopScores(OnScoresLoadedListener listener, String gameMode) {
        Query query = db.collection("scores");

        if (gameMode != null && !gameMode.isEmpty()) {
            query = query.whereEqualTo("gameMode", gameMode);
        }

        query.orderBy("score", Query.Direction.DESCENDING)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Score> scores = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Score score = document.toObject(Score.class);
                            scores.add(score);
                        }
                        listener.onScoresLoaded(scores);
                    } else {
                        listener.onError(task.getException().getMessage());
                    }
                });
    }

    // स्कोर लोड श्रोता इन्टरफेस (Score Load Listener Interface)
    public interface OnScoresLoadedListener {
        void onScoresLoaded(List<Score> scores);
        void onError(String error);
    }

    // स्कोर डाटा क्लास (Score Data Class)
    public static class Score {
        public String userId;
        public String playerName;
        public int score;
        public Date timestamp;
        public String gameMode;

        // खाली कन्स्ट्रक्टर (Empty Constructor)
        public Score() {
            // Firebase को लागि आवश्यक
        }

        // पूर्ण कन्स्ट्रक्टर (Full Constructor)
        public Score(String userId, String playerName, int score, Date timestamp, String gameMode) {
            this.userId = userId;
            this.playerName = playerName;
            this.score = score;
            this.timestamp = timestamp;
            this.gameMode = gameMode;
        }
    }
}