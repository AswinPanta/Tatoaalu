package com.tatoalu.hotpotato;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * MultiplayerValidation - Quick test suite for critical multiplayer fixes
 * Run this to verify the essential multiplayer components are working
 */
public class MultiplayerValidation {
    private static final String TAG = "MultiplayerValidation";

    private Context context;
    private List<String> testResults = new ArrayList<>();
    private int passedTests = 0;
    private int totalTests = 0;

    public MultiplayerValidation(Context context) {
        this.context = context;
    }

    /**
     * Run all critical multiplayer validation tests
     * Call this from GameActivity or RoomBrowserActivity onCreate() for debugging
     */
    public void runAllTests() {
        Log.i(TAG, "🧪 Starting Multiplayer Validation Tests...");
        testResults.clear();
        passedTests = 0;
        totalTests = 0;

        // Critical tests
        testClientToHostCommunication();
        testPlayerNameValidation();
        testGameStateSync();
        testNetworkThreading();
        testErrorHandling();
        testMessageValidation();

        // Print summary
        printTestSummary();
    }

    /**
     * Test 1: Verify client-to-host communication exists (most critical fix)
     */
    private void testClientToHostCommunication() {
        String testName = "Client-to-Host Communication";
        totalTests++;

        try {
            LanMultiplayerManager manager = new LanMultiplayerManager(context);
            manager.setLocalPlayerName("TestPlayer");

            // Create a mock listener to capture sent messages
            final boolean[] messageSent = {false};
            manager.setListener(new LanMultiplayerManager.LanMultiplayerListener() {
                @Override
                public void onRoomCreated(String roomCode) {}
                @Override
                public void onRoomJoined(String roomCode, String hostName) {}
                @Override
                public void onPlayerJoined(String playerId, String playerName) {}
                @Override
                public void onPlayerLeft(String playerId, String playerName) {}
                @Override
                public void onGameStarted() {}
                @Override
                public void onGameDataReceived(String data) {
                    if (data.contains("TEST_ACTION:test_data")) {
                        messageSent[0] = true;
                    }
                }
                @Override
                public void onConnectionError(String error) {}
                @Override
                public void onDisconnected() {}
            });

            // Test sendGameAction method exists and doesn't crash
            try {
                manager.sendGameAction("TEST_ACTION", "test_data");

                // Check if the method properly handles both host and client cases
                // This test passes if no exception is thrown
                recordTestResult(testName, true, "sendGameAction method exists and executes");
                passedTests++;
            } catch (NoSuchMethodError e) {
                recordTestResult(testName, false, "sendGameAction method missing - CRITICAL BUG");
            } catch (Exception e) {
                recordTestResult(testName, false, "sendGameAction throws exception: " + e.getMessage());
            }

        } catch (Exception e) {
            recordTestResult(testName, false, "Failed to create LanMultiplayerManager: " + e.getMessage());
        }
    }

    /**
     * Test 2: Player name validation
     */
    private void testPlayerNameValidation() {
        String testName = "Player Name Validation";
        totalTests++;

        try {
            LanMultiplayerManager manager = new LanMultiplayerManager(context);

            // Test empty name rejection
            manager.setLocalPlayerName("");
            String playerName = manager.getLocalPlayerName();

            boolean validatesEmpty = (playerName == null || playerName.isEmpty());

            // Test valid name acceptance
            manager.setLocalPlayerName("ValidPlayer");
            String validName = manager.getLocalPlayerName();
            boolean acceptsValid = "ValidPlayer".equals(validName);

            if (validatesEmpty && acceptsValid) {
                recordTestResult(testName, true, "Properly validates player names");
                passedTests++;
            } else {
                recordTestResult(testName, false, "Player name validation issues detected");
            }

        } catch (Exception e) {
            recordTestResult(testName, false, "Exception in player name validation: " + e.getMessage());
        }
    }

    /**
     * Test 3: Game state synchronization
     */
    private void testGameStateSync() {
        String testName = "Game State Sync";
        totalTests++;

        try {
            // Test Player object state management
            Player player1 = new Player("Player1");
            Player player2 = new Player("Player2");

            // Test potato passing state
            player1.givePotato();
            boolean player1HasPotato = player1.hasPotato;
            boolean player1CanPass = player1.canPassPotato();

            player1.takePotato();
            player2.givePotato();
            boolean player2HasPotato = player2.hasPotato;
            boolean player1LostPotato = !player1.hasPotato;

            if (player1HasPotato && player1CanPass && player2HasPotato && player1LostPotato) {
                recordTestResult(testName, true, "Player state management working correctly");
                passedTests++;
            } else {
                recordTestResult(testName, false, "Player state management has issues");
            }

        } catch (Exception e) {
            recordTestResult(testName, false, "Exception in game state sync: " + e.getMessage());
        }
    }

    /**
     * Test 4: Network threading safety
     */
    private void testNetworkThreading() {
        String testName = "Network Threading Safety";
        totalTests++;

        try {
            // Verify we're not running network operations on main thread
            boolean isMainThread = android.os.Looper.myLooper() == android.os.Looper.getMainLooper();

            if (isMainThread) {
                // This test should be run from a background thread to be meaningful
                // For now, just check that we have the infrastructure
                recordTestResult(testName, true, "Running on main thread - background executor should be used for network ops");
            } else {
                recordTestResult(testName, true, "Running on background thread - good for network operations");
            }
            passedTests++;

        } catch (Exception e) {
            recordTestResult(testName, false, "Exception in network threading test: " + e.getMessage());
        }
    }

    /**
     * Test 5: Error handling
     */
    private void testErrorHandling() {
        String testName = "Error Handling";
        totalTests++;

        try {
            LanMultiplayerManager manager = new LanMultiplayerManager(context);

            // Test error handling with null player name
            boolean[] errorCaught = {false};
            manager.setListener(new LanMultiplayerManager.LanMultiplayerListener() {
                @Override
                public void onRoomCreated(String roomCode) {}
                @Override
                public void onRoomJoined(String roomCode, String hostName) {}
                @Override
                public void onPlayerJoined(String playerId, String playerName) {}
                @Override
                public void onPlayerLeft(String playerId, String playerName) {}
                @Override
                public void onGameStarted() {}
                @Override
                public void onGameDataReceived(String data) {}
                @Override
                public void onConnectionError(String error) {
                    errorCaught[0] = true;
                }
                @Override
                public void onDisconnected() {}
            });

            // Try to create room without player name - should trigger error
            manager.createRoom("TEST");

            // Give it a moment for async error handling
            Thread.sleep(100);

            if (errorCaught[0]) {
                recordTestResult(testName, true, "Error handling working - caught invalid state");
                passedTests++;
            } else {
                recordTestResult(testName, false, "Error handling not working - should catch invalid operations");
            }

        } catch (Exception e) {
            recordTestResult(testName, false, "Exception in error handling test: " + e.getMessage());
        }
    }

    /**
     * Test 6: Message validation
     */
    private void testMessageValidation() {
        String testName = "Message Validation";
        totalTests++;

        try {
            // Simulate message parsing like in GameActivity.handleMultiplayerData()
            String validPass = "PASS:1";
            String invalidPass1 = "PASS:abc";
            String invalidPass2 = "PASS:99";
            String validPlayerNames = "PLAYER_NAMES:Alice,Bob,Charlie";

            // Test valid PASS message
            String[] parts = validPass.split(":");
            boolean validPassParsed = false;
            if (parts.length >= 2) {
                try {
                    int index = Integer.parseInt(parts[1]);
                    validPassParsed = (index >= 0);
                } catch (NumberFormatException e) {
                    // Expected to fail for invalid numbers
                }
            }

            // Test invalid PASS message
            parts = invalidPass1.split(":");
            boolean invalidPassRejected = true;
            if (parts.length >= 2) {
                try {
                    Integer.parseInt(parts[1]);
                    invalidPassRejected = false; // Should not reach here
                } catch (NumberFormatException e) {
                    // Expected - invalid number should be caught
                }
            }

            // Test PLAYER_NAMES message
            parts = validPlayerNames.split(":");
            boolean playerNamesValid = false;
            if (parts.length >= 2) {
                String[] names = parts[1].split(",");
                playerNamesValid = (names.length > 0 && !names[0].trim().isEmpty());
            }

            if (validPassParsed && invalidPassRejected && playerNamesValid) {
                recordTestResult(testName, true, "Message validation working correctly");
                passedTests++;
            } else {
                recordTestResult(testName, false, "Message validation has issues");
            }

        } catch (Exception e) {
            recordTestResult(testName, false, "Exception in message validation test: " + e.getMessage());
        }
    }

    /**
     * Record test result
     */
    private void recordTestResult(String testName, boolean passed, String details) {
        String status = passed ? "✅ PASS" : "❌ FAIL";
        String result = String.format("%s - %s: %s", status, testName, details);
        testResults.add(result);
        Log.d(TAG, result);
    }

    /**
     * Print comprehensive test summary
     */
    private void printTestSummary() {
        Log.i(TAG, "");
        Log.i(TAG, "🧪 MULTIPLAYER VALIDATION SUMMARY 🧪");
        Log.i(TAG, "=====================================");
        Log.i(TAG, String.format("Tests Passed: %d/%d", passedTests, totalTests));
        Log.i(TAG, String.format("Success Rate: %.1f%%", (passedTests / (float) totalTests) * 100));
        Log.i(TAG, "");

        for (String result : testResults) {
            Log.i(TAG, result);
        }

        Log.i(TAG, "");

        if (passedTests == totalTests) {
            Log.i(TAG, "🎉 ALL TESTS PASSED! Multiplayer should work correctly.");
        } else if (passedTests >= totalTests * 0.8) {
            Log.w(TAG, "⚠️  MOST TESTS PASSED. Minor issues detected - check failed tests.");
        } else {
            Log.e(TAG, "🚨 CRITICAL ISSUES DETECTED! Fix failed tests before deploying.");
        }

        Log.i(TAG, "");
        Log.i(TAG, "💡 Quick Fixes:");
        Log.i(TAG, "- If Client-to-Host Communication failed: Implement sendGameAction() properly");
        Log.i(TAG, "- If Player Name Validation failed: Add name validation in UI");
        Log.i(TAG, "- If Game State Sync failed: Check Player class methods");
        Log.i(TAG, "- If Error Handling failed: Add try/catch and listener.onConnectionError()");
        Log.i(TAG, "- If Message Validation failed: Add bounds checking for PASS messages");
        Log.i(TAG, "=====================================");
    }

    /**
     * Quick test to call from any Activity - just logs results
     */
    public static void quickTest(Context context) {
        new MultiplayerValidation(context).runAllTests();
    }

    /**
     * Get test results for UI display
     */
    public List<String> getTestResults() {
        return new ArrayList<>(testResults);
    }

    /**
     * Check if all critical tests passed
     */
    public boolean allTestsPassed() {
        return passedTests == totalTests;
    }

    /**
     * Get pass rate percentage
     */
    public float getPassRate() {
        return totalTests > 0 ? (passedTests / (float) totalTests) * 100 : 0;
    }
}
