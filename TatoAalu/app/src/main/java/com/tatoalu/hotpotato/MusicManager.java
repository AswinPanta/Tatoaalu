package com.tatoalu.hotpotato;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Music Manager for Hot Potato Game
 * Handles random music playback with random stopping for elimination mechanics
 */
public class MusicManager {
    private static final String TAG = "MusicManager";

    // Dynamically discover music files stored in assets/music/ folder
    private List<String> getMusicFiles() {
        List<String> result = new ArrayList<>();
        try {
            String[] files = context.getAssets().list("music");
            if (files != null) {
                for (String f : files) {
                    // Build full asset path
                    result.add("music/" + f);
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "Unable to list assets/music", e);
        }
        return result;
    }

    // Random timing constraints (in milliseconds)
    private static final int MIN_PLAY_TIME = 10000; // 10 seconds minimum
    private static final int MAX_PLAY_TIME = 30000; // 30 seconds maximum
    private static final int FADE_OUT_DURATION = 1000; // 1 second fade out

    private Context context;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Random random;
    private boolean isPlaying = false;
    private boolean isPaused = false;

    // Current session variables
    private Runnable stopMusicRunnable;
    private MusicStoppedListener musicStoppedListener;
    private int currentRandomStopTime;
    private long musicStartTime;

    // Listener interface for when music stops
    public interface MusicStoppedListener {
        void onMusicStopped();
        void onMusicStarted(int expectedDurationMs);
    }

    public MusicManager(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
        this.random = new Random();
        initializeMediaPlayer();
    }

    private void initializeMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> {
            Log.d(TAG, "Music completed naturally");
            if (musicStoppedListener != null) {
                musicStoppedListener.onMusicStopped();
            }
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
            return false;
        });
    }

    /**
     * Start playing random music with random stop time
     */
    public void startRandomMusic() {
        if (isPlaying) {
            stopMusic();
        }

        try {
            // Select random music file from discovered assets
            List<String> musicFiles = getMusicFiles();
            if (musicFiles.isEmpty()) {
                Log.w(TAG, "No music files found in assets/music, using fallback timer");
                playFallbackSound();
                return;
            }
            String selectedMusic = musicFiles.get(random.nextInt(musicFiles.size()));
            Log.d(TAG, "Starting music: " + selectedMusic);

            // Check if file exists in assets
            AssetFileDescriptor afd = null;
            try {
                afd = context.getAssets().openFd(selectedMusic);
                initializeMediaPlayer();
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
            } catch (IOException e) {
                // Fallback to a simple beep sound if no music files
                Log.w(TAG, "Music file not found: " + selectedMusic + ", using fallback timer");
                if (afd != null) {
                    try {
                        afd.close();
                    } catch (IOException closeException) {
                        Log.w(TAG, "Error closing AssetFileDescriptor", closeException);
                    }
                }
                playFallbackSound();
                return;
            }

            mediaPlayer.prepare();
            mediaPlayer.setLooping(true); // Loop until we stop it randomly
            mediaPlayer.start();

            isPlaying = true;
            isPaused = false;
            musicStartTime = System.currentTimeMillis();

            // Calculate random stop time
            currentRandomStopTime = MIN_PLAY_TIME + random.nextInt(MAX_PLAY_TIME - MIN_PLAY_TIME);
            Log.d(TAG, "Music will stop after: " + currentRandomStopTime + "ms");

            // Notify listener that music started
            if (musicStoppedListener != null) {
                musicStoppedListener.onMusicStarted(currentRandomStopTime);
            }

            // Schedule random stop
            scheduleRandomStop();

        } catch (IOException e) {
            Log.e(TAG, "Error starting music", e);
            playFallbackSound();
        }
    }

    /**
     * Fallback to timer-based system when music files are not available
     */
    private void playFallbackSound() {
        Log.d(TAG, "Using fallback timer system (no music files)");

        // Use a simple timer-based approach
        currentRandomStopTime = MIN_PLAY_TIME + random.nextInt(MAX_PLAY_TIME - MIN_PLAY_TIME);
        isPlaying = true;
        isPaused = false;
        musicStartTime = System.currentTimeMillis();

        Log.d(TAG, "Fallback timer will trigger after: " + currentRandomStopTime + "ms");

        if (musicStoppedListener != null) {
            musicStoppedListener.onMusicStarted(currentRandomStopTime);
        }

        // Schedule the stop without actual music
        handler.postDelayed(() -> {
            Log.d(TAG, "Fallback timer triggered - stopping music");
            if (musicStoppedListener != null && isPlaying) {
                musicStoppedListener.onMusicStopped();
            }
            isPlaying = false;
        }, currentRandomStopTime);
    }

    /**
     * Schedule the random music stop
     */
    private void scheduleRandomStop() {
        if (stopMusicRunnable != null) {
            handler.removeCallbacks(stopMusicRunnable);
        }

        stopMusicRunnable = () -> {
            Log.d(TAG, "Random stop time reached - stopping music");
            stopMusicWithFade();
        };

        handler.postDelayed(stopMusicRunnable, currentRandomStopTime);
    }

    /**
     * Stop music with fade out effect
     */
    private void stopMusicWithFade() {
        if (!isPlaying || mediaPlayer == null) return;

        // Simple fade out by reducing volume gradually
        final float originalVolume = 1.0f;
        final int fadeSteps = 10;
        final int fadeInterval = FADE_OUT_DURATION / fadeSteps;

        for (int i = 0; i <= fadeSteps; i++) {
            final float volume = originalVolume * (fadeSteps - i) / fadeSteps;
            final boolean isLastStep = (i == fadeSteps);

            handler.postDelayed(() -> {
                if (mediaPlayer != null && isPlaying) {
                    mediaPlayer.setVolume(volume, volume);
                    if (isLastStep) {
                        stopMusic();
                        if (musicStoppedListener != null) {
                            musicStoppedListener.onMusicStopped();
                        }
                    }
                }
            }, i * fadeInterval);
        }
    }

    /**
     * Immediately stop music without fade
     */
    public void stopMusic() {
        if (stopMusicRunnable != null) {
            handler.removeCallbacks(stopMusicRunnable);
            stopMusicRunnable = null;
        }

        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            isPlaying = false;
            isPaused = false;
        }
    }

    /**
     * Pause music (for app lifecycle events)
     */
    public void pauseMusic() {
        if (mediaPlayer != null && isPlaying && !isPaused) {
            mediaPlayer.pause();
            isPaused = true;

            // Pause the random stop timer
            if (stopMusicRunnable != null) {
                handler.removeCallbacks(stopMusicRunnable);
            }
        }
    }

    /**
     * Resume music (for app lifecycle events)
     */
    public void resumeMusic() {
        if (mediaPlayer != null && isPaused) {
            mediaPlayer.start();
            isPaused = false;

            // Recalculate remaining time and reschedule stop
            long elapsedTime = System.currentTimeMillis() - musicStartTime;
            long remainingTime = currentRandomStopTime - elapsedTime;

            if (remainingTime > 0) {
                stopMusicRunnable = () -> {
                    Log.d(TAG, "Random stop time reached after resume");
                    stopMusicWithFade();
                };
                handler.postDelayed(stopMusicRunnable, remainingTime);
            } else {
                // Time already expired, stop immediately
                stopMusicWithFade();
            }
        }
    }

    /**
     * Get remaining play time in milliseconds
     */
    public long getRemainingTime() {
        if (!isPlaying) return 0;
        long elapsedTime = System.currentTimeMillis() - musicStartTime;
        return Math.max(0, currentRandomStopTime - elapsedTime);
    }

    /**
     * Get total planned duration for current session
     */
    public int getCurrentSessionDuration() {
        return currentRandomStopTime;
    }

    /**
     * Check if music is currently playing
     */
    public boolean isPlaying() {
        return isPlaying && !isPaused;
    }

    /**
     * Check if using fallback mode (no actual music)
     */
    public boolean isFallbackMode() {
        return mediaPlayer == null || !mediaPlayer.isPlaying();
    }

    /**
     * Set listener for music events
     */
    public void setMusicStoppedListener(MusicStoppedListener listener) {
        this.musicStoppedListener = listener;
    }

    /**
     * Release resources
     */
    public void release() {
        stopMusic();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    /**
     * Test method to simulate music stopping for testing
     */
    public void forceStop() {
        Log.d(TAG, "Force stopping music for testing");
        stopMusicWithFade();
    }
}
