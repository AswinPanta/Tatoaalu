package com.tatoalu.hotpotato;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.content.ContextCompat;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// खेल दृश्य - मुख्य खेल इन्टरफेस (Game View - Main Game Interface)
public class GameView extends View {
    // खेल श्रोता इन्टरफेस (Game listener interface)
    public interface GameListener {
        void onTick(long millisRemaining); // बाँकी समय (Remaining time)
        void onGameOver(String loserName); // खेल समाप्त (Game over)
    }

    // पास कलब्याक इन्टरफेस (Pass callback interface)
    public interface PassCallback {
        void onPassRequested(); // पास अनुरोध (Pass request)
    }

    // खेलाडी वर्ग (Player class)
    private static class Player {
        String name; // नाम (Name)
        Bitmap avatar; // अवतार (Avatar)
        float x, y; // स्थिति (Position)
        boolean eliminated = false; // हटाइएको (Eliminated)
        Player(String n, Bitmap a) { name = n; avatar = a; }
    }

    // पेन्ट र रेन्डरिङ उपकरणहरू (Paint and rendering tools)
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // खेल डेटा (Game data)
    private final List<Player> players = new ArrayList<>();
    private Bitmap potato; // आलु (Potato)
    private Bitmap campfire; // क्याम्पफायर (Campfire)
    private Bitmap a1, a2, a3, a4; // अवतारहरू (Avatars)

    // खेल अवस्था (Game state)
    private int currentHolder = 0; // हालको धारक (Current holder)
    private long holderStartTime; // धारक सुरु समय (Holder start time)
    private long burnThresholdMs; // जलाउने सीमा (Burn threshold)

    // एनिमेसन अवस्था (Animation state)
    private boolean potatoAnimating = false; // आलु एनिमेट गर्दै (Potato animating)
    private long potatoAnimStartMs;
    private long potatoAnimDurationMs = 500;
    private float potatoStartX, potatoStartY;
    private float potatoEndX, potatoEndY;
    private int pendingHolder = -1;

    private List<String> pendingPlayerNames = new ArrayList<>();

    // खेल नियन्त्रण (Game control)
    private boolean running = true; // चलिरहेको (Running)
    private boolean gameOver = false; // खेल समाप्त (Game over)
    private boolean remoteMode = false; // रिमोट मोड (Remote mode)
    private PassCallback passCallback;

    private GameListener listener;
    private android.content.SharedPreferences winPrefs;
    private int score = 0; // स्कोर ट्र्याक गर्न (Track score)

    // कन्स्ट्रक्टरहरू (Constructors)
    public GameView(Context context) {
        super(context);
    }

    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // खेल सुरु गर्नुहोस् (Initialize game)
    public void init(int playerCount, GameListener listener) {
        this.listener = listener;
        paint.setFilterBitmap(true);
        paint.setDither(true);
        winPrefs = getContext().getSharedPreferences("tato_wins", Context.MODE_PRIVATE);
        // Configure paints (fiery theme)
        textPaint.setColor(0xFFFFE0B2); // light orange-cream
        textPaint.setTextAlign(Paint.Align.CENTER);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(0xFFFF6F00); // deep hot orange
        // Configure placeholder style for avatars while loading
        placeholderPaint.setColor(0x66FFE0B2);
        placeholderPaint.setStyle(Paint.Style.FILL);

        // Create placeholder players; avatars loaded asynchronously
        String[] names = new String[] { "Aanya", "Ben", "Chloe", "Drew" };
        players.clear();
        for (int i = 0; i < playerCount; i++) {
            players.add(new Player(names[i % names.length], null));
        }
        resetBurnThreshold();
        holderStartTime = SystemClock.uptimeMillis();
        invalidate();

        // Load bitmaps off the UI thread
        new Thread(() -> {
            int avatarTarget = 256;
            int potatoTarget = 192;
            int campfireTarget = 384;
            
            // Use optimized vector drawables for better performance
            Bitmap a1L = drawableToBitmap(ContextCompat.getDrawable(getContext(), R.drawable.player_avatar_optimized), avatarTarget);
            Bitmap a2L = drawableToBitmap(ContextCompat.getDrawable(getContext(), R.drawable.player_avatar_optimized), avatarTarget);
            Bitmap a3L = drawableToBitmap(ContextCompat.getDrawable(getContext(), R.drawable.player_avatar_optimized), avatarTarget);
            Bitmap a4L = drawableToBitmap(ContextCompat.getDrawable(getContext(), R.drawable.player_avatar_optimized), avatarTarget);
            Bitmap potatoL = drawableToBitmap(ContextCompat.getDrawable(getContext(), R.drawable.potato_optimized), potatoTarget);
            Bitmap campfireL = drawableToBitmap(ContextCompat.getDrawable(getContext(), R.drawable.campfire_optimized), campfireTarget);
            
            post(() -> {
                a1 = a1L; a2 = a2L; a3 = a3L; a4 = a4L;
                potato = potatoL; campfire = campfireL;
                for (int i = 0; i < players.size(); i++) {
                    players.get(i).avatar = (i==0?a1:(i==1?a2:(i==2?a3:a4)));
                }
                rebuildPlayersList(); // Call rebuildPlayersList after avatars are loaded
                invalidate();
            });
        }).start();
    }

    private void rebuildPlayersList() {
        players.clear();
        Bitmap[] avatars = null;
        if (a1 != null && a2 != null && a3 != null && a4 != null) {
            avatars = new Bitmap[] { a1, a2, a3, a4 };
        }
        for (int i = 0; i < pendingPlayerNames.size(); i++) {
            Bitmap avatar = avatars == null ? null : avatars[i % avatars.length];
            players.add(new Player(pendingPlayerNames.get(i), avatar));
        }
        invalidate();
        gameOver = false;
    }

    public void pause() { running = false; }
    public void resume() { if (!gameOver) { running = true; invalidate(); } }

    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }

    public void setRemoteMode(boolean remote) { this.remoteMode = remote; }
    public void setPassCallback(PassCallback cb) { this.passCallback = cb; }
    public void setCurrentHolder(int idx) { 
        this.currentHolder = idx % Math.max(1, players.size()); 
        invalidate(); 
    }
    
    public int getCurrentHolder() {
        return currentHolder;
    }
    
    public void simulatePassAnimation(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= players.size() || 
            toIndex < 0 || toIndex >= players.size() || 
            potatoAnimating) {
            return;
        }
        
        // Start flying potato animation without changing the holder yet
        potatoStartX = players.get(fromIndex).x;
        potatoStartY = players.get(fromIndex).y;
        potatoEndX = players.get(toIndex).x;
        potatoEndY = players.get(toIndex).y;
        potatoAnimating = true;
        potatoAnimStartMs = SystemClock.uptimeMillis();
        invalidate();
    }
    public void setPlayerNames(List<String> names) {
        if (names == null || names.isEmpty()) return; // Prevent crash if all players leave
        this.pendingPlayerNames.clear();
        this.pendingPlayerNames.addAll(names);
        rebuildPlayersList();
    }

    public void triggerBurn() {
        gameOver = true;
        running = false;
        invalidate();
    }

    public int getScore() { // Add this method
        return score;
    }

    public int getPlayerCount() {
        return players.size();
    }

    private void resetBurnThreshold() {
        // Fixed 60-second countdown from game start
        burnThresholdMs = 60000;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        basePaint.setColor(0xFF0D0D0D);
        float cx = w / 2f;
        float cy = h * 0.85f;
        float r1 = Math.min(w, h) * 0.5f;
        float r2 = Math.min(w, h) * 0.38f;
        glowPaint1.setShader(new RadialGradient(cx, cy, r1,
                0x55FF6F00, 0x00FF6F00, Shader.TileMode.CLAMP));
        glowPaint2.setShader(new RadialGradient(cx, cy - dpToPx(80), r2,
                0x33FFA000, 0x00FFA000, Shader.TileMode.CLAMP));
    }

    private float dpToPx(float dp) { return dp * getResources().getDisplayMetrics().density; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!running) return;

        int w = getWidth();
        int h = getHeight();

        // Background with flame glows
        canvas.drawRect(0, 0, w, h, basePaint);
        canvas.drawCircle(w / 2f, h * 0.85f, Math.min(w, h) * 0.5f, glowPaint1);
        canvas.drawCircle(w / 2f, h * 0.85f - dpToPx(80), Math.min(w, h) * 0.38f, glowPaint2);

        int n = players.size();
        if (n == 0) {
            textPaint.setTextSize(Math.min(w, h) * 0.06f);
            canvas.drawText("Add players to start", w / 2f, h / 2f, textPaint);
            return;
        }

        // Layout players on a circle
        float radius = Math.min(w, h) * 0.35f;
        float cx = w / 2f;
        float cy = h / 2f;

        float avatarSize = Math.min(w, h) * 0.18f;
        float textSize = avatarSize * 0.22f;
        textPaint.setTextSize(textSize);
        ringPaint.setStrokeWidth(avatarSize * 0.06f);
        RectF dst = new RectF(0,0,avatarSize,avatarSize);

        for (int i = 0; i < n; i++) {
            double angle = (2 * Math.PI * i / n) - Math.PI / 2; // start top
            float px = (float)(cx + radius * Math.cos(angle));
            float py = (float)(cy + radius * Math.sin(angle));
            Player p = players.get(i);
            p.x = px; p.y = py;

            dst.offsetTo(px - avatarSize/2f, py - avatarSize/2f);
            if (p.avatar != null) {
                // draw avatar scaled to destination rect
                canvas.drawBitmap(p.avatar, null, dst, paint);
            } else {
                // draw placeholder circle while avatar loads
                canvas.drawCircle(px, py, avatarSize * 0.5f, placeholderPaint);
            }
            // highlight current holder
            if (i == currentHolder) {
                canvas.drawCircle(px, py, avatarSize * 0.55f, ringPaint);
            }
            // draw player name// Name below avatar, with win count
            int wins = (winPrefs == null || p.name == null) ? 0 : winPrefs.getInt(p.name, 0);
            String winText = getContext().getString(R.string.win_count_suffix, wins);
            canvas.drawText(p.name + " " + winText, px, py + avatarSize/2f + textSize + 6f, textPaint);
        }
        Player holder = players.get(currentHolder);
        float potatoSize = avatarSize * 0.45f;
        // Determine potato position: animate if in-flight
        float potatoPx, potatoPy;
        if (potatoAnimating) {
            float t = Math.min(1f, (SystemClock.uptimeMillis() - potatoAnimStartMs) / (float) potatoAnimDurationMs);
            // Ease in-out
            float eased = t < 0.5f ? 2f * t * t : -1f + (4f - 2f * t) * t;
            potatoPx = potatoStartX + (potatoEndX - potatoStartX) * eased;
            potatoPy = potatoStartY + (potatoEndY - potatoStartY) * eased - dpToPx(30) * (1 - Math.abs(1 - 2 * t));
            if (t >= 1f) {
                potatoAnimating = false;
                if (pendingHolder >= 0) {
                    currentHolder = pendingHolder;
                    pendingHolder = -1;
                }
                holder = players.get(currentHolder);
            }
        } else {
            potatoPx = holder.x;
            potatoPy = holder.y;
        }
        RectF potatoDst = new RectF(potatoPx - potatoSize/2f, potatoPy - potatoSize/2f, potatoPx + potatoSize/2f, potatoPy + potatoSize/2f);
        if (potato != null) {
            canvas.drawBitmap(potato, null, potatoDst, paint);
        }

        long elapsed = SystemClock.uptimeMillis() - holderStartTime;
        long remaining = Math.max(0, burnThresholdMs - elapsed);
        if (!remoteMode && listener != null) listener.onTick(remaining);

        // Potato animation handled above

        if (!remoteMode && elapsed >= burnThresholdMs && !gameOver) {
            gameOver = true;
            running = false;
            if (listener != null) listener.onGameOver(holder.name);
        }

        // Draw campfire if game over (both local and remote scenarios)
        if (gameOver && campfire != null) {
            float fireSize = avatarSize * 0.8f;
            RectF fireDst = new RectF(holder.x - fireSize/2f, holder.y - fireSize/2f, holder.x + fireSize/2f, holder.y + fireSize/2f);
            canvas.drawBitmap(campfire, null, fireDst, paint);
        }

        if (!gameOver) {
            postInvalidateOnAnimation();
        }
    }

    // Decode a resource bitmap with inSampleSize chosen for target max dimension
    private Bitmap decodeScaled(int resId, int targetPx) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(getResources(), resId, bounds);
            int maxDim = Math.max(bounds.outWidth, bounds.outHeight);
            int sample = 1;
            while (maxDim / (sample * 2) >= targetPx) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeResource(getResources(), resId, opts);
        } catch (Throwable t) {
            return null;
        }
    }

    private Bitmap scaleIfNeeded(Bitmap src, int w, int h) {
        if (src.getWidth() == w && src.getHeight() == h) return src;
        return Bitmap.createScaledBitmap(src, w, h, true);
    }
    
    private Bitmap drawableToBitmap(Drawable drawable, int targetSize) {
        if (drawable == null) return null;
        
        Bitmap bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, targetSize, targetSize);
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && !gameOver) {
            int n = Math.max(1, players.size());
            int w = getWidth();
            float x = event.getX();
            int zone = Math.min(n - 1, (int) Math.floor(x / Math.max(1f, (float) w / n)));
            boolean allowed = zone == currentHolder;
            if (!allowed) return true; // ignore taps outside active player's zone
            if (remoteMode) {
                if (passCallback != null) passCallback.onPassRequested();
            } else {
                if (players.size() > 0) {
                    passPotato();
                }
                if (passCallback != null) passCallback.onPassRequested();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void passPotato() {
        int from = currentHolder;
        int to = (currentHolder + 1) % Math.max(1, players.size());
        score++; // Increment score on successful pass
        // Start flying potato animation; do not change holder until arrival
        pendingHolder = to;
        potatoStartX = players.get(from).x;
        potatoStartY = players.get(from).y;
        potatoEndX = players.get(to).x;
        potatoEndY = players.get(to).y;
        potatoAnimating = true;
        potatoAnimStartMs = SystemClock.uptimeMillis();
        // Global countdown remains; do not reset holderStartTime or threshold
        invalidate();
    }
}