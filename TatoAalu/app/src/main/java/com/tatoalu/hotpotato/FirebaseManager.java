package com.tatoalu.hotpotato;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

// Firebase व्यवस्थापक (Firebase Manager)
public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    
    // सिंगलटन इन्स्ट्यान्स (Singleton Instance)
    private static FirebaseManager instance;
    private LeaderboardManager leaderboardManager;
    
    // निजी कन्स्ट्रक्टर (Private Constructor)
    private FirebaseManager() {
        leaderboardManager = new LeaderboardManager();
    }
    
    // इन्स्ट्यान्स प्राप्त गर्ने विधि (Method to get instance)
    public static FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }
    
    // स्कोर पेश गर्ने विधि (Method to submit score)
    public void submitScore(String userId, String playerName, int score, String gameMode) {
        leaderboardManager.submitScore(userId, playerName, score, gameMode);
    }
    
    // लिडरबोर्ड प्राप्त गर्ने विधि (Method to get leaderboard)
    public void getLeaderboard(LeaderboardManager.OnScoresLoadedListener listener, String gameMode) {
        leaderboardManager.getTopScores(listener, gameMode);
    }
    
    // लिडरबोर्ड कलब्याक इन्टरफेस (Leaderboard Callback Interface)
    public interface LeaderboardCallback {
        void onLeaderboardReceived(Map<String, Integer> leaderboard);
        void onError(String error);
    }
}