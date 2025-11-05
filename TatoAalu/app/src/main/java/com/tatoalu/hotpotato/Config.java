package com.tatoalu.hotpotato;

// कन्फिगरेसन सेटिङहरू - खेल र नेटवर्क पैरामिटरहरू (Configuration Settings - Game and Network Parameters)
public final class Config {
    private Config() {} // इन्स्ट्यान्सिएसन रोक्नुहोस् (Prevent instantiation)

    // नेटवर्क कन्फिगरेसन (Network Configuration)
    public static final int PORT = 54567;
    public static final int MAX_PLAYERS = 8; // अधिकतम खेलाडीहरू (Maximum players)
    public static final int SERVER_THREAD_POOL = 10; // अधिकतम समानान्तर ह्यान्डलरहरू (Max concurrent handlers)
    public static final int MAX_MESSAGE_SIZE = 16 * 1024; // १६ KB अधिकतम JSON पेलोड (16 KB max JSON payload)
    
    // समय कन्फिगरेसन (Timing Configuration)
    public static final int TICK_INTERVAL_MS = 100; // टिक अन्तराल (Tick interval)
    public static final int GAME_TIMER_INTERVAL_MS = 100; // खेल टाइमर अन्तराल (Game timer interval)
    
    // क्लाइन्ट पुनः प्रयास कन्फिगरेसन (Client Retry Configuration)
    public static final int CLIENT_RECONNECT_MAX_ATTEMPTS = 6; // अधिकतम पुनः जडान प्रयासहरू (Max reconnect attempts)
    public static final int CLIENT_RECONNECT_BASE_MS = 500; // ब्याकअफ आधार (Backoff base)
    public static final int CLIENT_RECONNECT_MAX_MS = 8000; // अधिकतम ब्याकअफ ढिलाइ (Max backoff delay)
    
    // सकेट कन्फिगरेसन (Socket Configuration)
    public static final int SOCKET_TIMEOUT_MS = 30000; // ३० सेकेन्ड (30 seconds)
    public static final int SOCKET_CONNECT_TIMEOUT_MS = 10000; // १० सेकेन्ड (10 seconds)
    
    // लगिङ ट्यागहरू (Logging Tags)
    public static final String TAG_SERVER = "LanServer";
    public static final String TAG_CLIENT = "LanClient";
    public static final String TAG_NSD = "NsdHelper";
    public static final String TAG_GAME = "GameActivity";
}