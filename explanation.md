# Tato Aalu (Hot Potato) Game Documentation

## Game Overview
Tato Aalu is a digital implementation of the classic "Hot Potato" game for Android. Players pass a virtual potato around, and when the music stops, the player holding the potato is eliminated. The last player standing wins.

## Technical Architecture
The game is built as an Android application using Java. Key components include:
- `GameActivity.java`: Main game controller handling game logic and player interactions
- `GameView.java`: Rendering component for the game visuals
- `LanServer.java`: Handles multiplayer functionality over local network

## Build Process and Fixes
The following issues were fixed to make the app buildable:
1. SVG files renamed to have `.xml` extension as required by Android
2. Removed duplicate PNG files that conflicted with SVG/XML resources
3. Removed duplicate `game_theme.svg` from the raw directory
4. Fixed type mismatch in `GameActivity.java`:
   - Changed `players` variable to `playerCount` for integer values
   - Ensured consistent use of `players` List<String> throughout the code
   - Updated GameView.init() calls to use playerCount instead of players list

## Installation and Running
1. Build the app with `./gradlew assembleDebug`
2. Install on a connected Android device with `./gradlew installDebug`
3. Launch the app from the device menu

This document provides a comprehensive explanation of all functions within the Tato Aalu project, organized by module and file structure.

## Table of Contents

1. [Main Activity](#main-activity)
2. [Game Activity](#game-activity)
3. [Game View](#game-view)
4. [Multiplayer Functionality](#multiplayer-functionality)
   - [LAN Server](#lan-server)
   - [LAN Client](#lan-client)
   - [Network Service Discovery Helper](#network-service-discovery-helper)
5. [Audio System](#audio-system)

---

## Main Activity

**File:** `MainActivity.java`

The MainActivity serves as the entry point to the application, handling player setup and game initialization.

### Functions

#### `onCreate(Bundle savedInstanceState)`
- **Purpose:** Initializes the main activity, sets up UI components, and starts background music.
- **Parameters:**
  - `savedInstanceState`: Bundle containing the activity's previously saved state, if available.
- **Return Value:** None (void)
- **Implementation Notes:** Initializes theme music, player count spinner, and name input fields.

#### `onResume()`
- **Purpose:** Handles activity resumption, ensuring music playback and triggering intro animations.
- **Parameters:** None
- **Return Value:** None (void)
- **Implementation Notes:** Restarts theme music if it was paused and triggers intro animations if not already shown.

#### `onPause()`
- **Purpose:** Handles activity pausing, managing resource cleanup.
- **Parameters:** None
- **Return Value:** None (void)
- **Implementation Notes:** Pauses the theme music to conserve resources.

#### `runIntroAnimations()`
- **Purpose:** Executes entrance animations for UI elements on the main screen.
- **Parameters:** None
- **Return Value:** None (void)
- **Implementation Notes:** Uses property animations for smooth, engaging UI element introductions.

#### `startLocalGame(View view)`
- **Purpose:** Initiates a local multiplayer game session.
- **Parameters:**
  - `view`: The button view that was clicked.
- **Return Value:** None (void)
- **Implementation Notes:** Collects player names and count, then launches GameActivity in local mode.

#### `startHostGame(View view)`
- **Purpose:** Initiates a network game as the host.
- **Parameters:**
  - `view`: The button view that was clicked.
- **Return Value:** None (void)
- **Implementation Notes:** Collects player information and launches GameActivity in host mode.

#### `startJoinGame(View view)`
- **Purpose:** Joins an existing network game.
- **Parameters:**
  - `view`: The button view that was clicked.
- **Return Value:** None (void)
- **Implementation Notes:** Collects local player information and launches GameActivity in client mode.

#### `updateNameFieldVisibility(int playerCount)`
- **Purpose:** Shows or hides name input fields based on selected player count.
- **Parameters:**
  - `playerCount`: Number of players selected (2-4).
- **Return Value:** None (void)
- **Implementation Notes:** Dynamically adjusts UI visibility for a cleaner interface.

#### `getPlayerNames()`
- **Purpose:** Collects and validates player names from input fields.
- **Parameters:** None
- **Return Value:** ArrayList<String> containing player names.
- **Implementation Notes:** Ensures all visible name fields have valid entries.

---

## Game Activity

**File:** `GameActivity.java`

The GameActivity manages the actual gameplay, handling game state, player interactions, and network communication for multiplayer.

### Functions

#### `onCreate(Bundle savedInstanceState)`
- **Purpose:** Initializes the game activity and sets up the game environment.
- **Parameters:**
  - `savedInstanceState`: Bundle containing the activity's previously saved state, if available.
- **Return Value:** None (void)
- **Implementation Notes:** Sets up game view, audio system, and determines game mode (local/host/client).

#### `setupHost()`
- **Purpose:** Configures the device as a game host for network play.
- **Parameters:** None
- **Return Value:** None (void)
- **Implementation Notes:** Initializes LanServer, registers service with NsdHelper, and prepares for client connections.
- **Relationship:** Works with NsdHelper and LanServer to establish multiplayer functionality.

#### `setupClient()`
- **Purpose:** Configures the device as a game client for network play.
- **Parameters:** None
- **Return Value:** None (void)
- **Implementation Notes:** Initializes service discovery via NsdHelper, acquires multicast lock, and prepares to connect to host.
- **Relationship:** Works with NsdHelper and LanClient to establish multiplayer functionality.

#### `onGameTick(long millisRemaining)`
- **Purpose:** Handles regular game state updates during gameplay.
- **Parameters:**
  - `millisRemaining`: Time remaining before potential explosion.
- **Return Value:** None (void)
- **Implementation Notes:** Updates UI elements and broadcasts state in network mode.

#### `onGameOver(String loserName)`
- **Purpose:** Handles end-of-game state when a player loses.
- **Parameters:**
  - `loserName`: Name of the player who lost.
- **Return Value:** None (void)
- **Implementation Notes:** Displays game over message, plays explosion sound, and handles cleanup.

#### `onBackPressed()`
- **Purpose:** Handles back button press during gameplay.
- **Parameters:** None
- **Return Value:** None (void)
- **Implementation Notes:** Shows confirmation dialog to prevent accidental game exit.

#### `onDestroy()`
- **Purpose:** Cleans up resources when the activity is destroyed.
- **Parameters:** None
- **Return Value:** None (void)
- **Implementation Notes:** Releases network resources, audio resources, and multicast lock if acquired.

---

## Game View

**File:** `GameView.java`

The GameView handles the visual representation of the game, rendering players, the potato, and animations.

### Functions

#### `init(int playerCount, GameListener listener)`
- **Purpose:** Initializes the game view with specified number of players.
- **Parameters:**
  - `playerCount`: Number of players in the game.
  - `listener`: Callback interface for game events.
- **Return Value:** None (void)
- **Implementation Notes:** Sets up player objects, loads graphics, and prepares the game state.

#### `setPlayerNames(List<String> names)`
- **Purpose:** Updates player names in the game view.
- **Parameters:**
  - `names`: List of player names.
- **Return Value:** None (void)
- **Implementation Notes:** Recreates player objects with provided names while preserving avatars.

#### `setCurrentHolder(int idx)`
- **Purpose:** Updates which player currently holds the potato.
- **Parameters:**
  - `idx`: Index of the player who now holds the potato.
- **Return Value:** None (void)
- **Implementation Notes:** Updates game state and triggers UI refresh.

#### `passPotato(int fromIdx, int toIdx)`
- **Purpose:** Animates passing the potato between players.
- **Parameters:**
  - `fromIdx`: Index of the player passing the potato.
  - `toIdx`: Index of the player receiving the potato.
- **Return Value:** None (void)
- **Implementation Notes:** Calculates animation path and triggers animation sequence.

#### `onDraw(Canvas canvas)`
- **Purpose:** Renders the game state on the provided canvas.
- **Parameters:**
  - `canvas`: Canvas to draw on.
- **Return Value:** None (void)
- **Implementation Notes:** Draws players, potato, animations, and game state indicators.

#### `decodeScaled(int resId, int targetPx)`
- **Purpose:** Loads and scales image resources efficiently.
- **Parameters:**
  - `resId`: Resource ID of the image to load.
  - `targetPx`: Target dimension in pixels.
- **Return Value:** Bitmap of the loaded and scaled image.
- **Implementation Notes:** Supports both SVG and PNG formats with efficient memory usage.

#### `getBitmapFromVectorDrawable(int drawableId, int targetPx)`
- **Purpose:** Converts vector drawables (SVG) to bitmaps.
- **Parameters:**
  - `drawableId`: Resource ID of the vector drawable.
  - `targetPx`: Target dimension in pixels.
- **Return Value:** Bitmap created from the vector drawable.
- **Implementation Notes:** Ensures proper rendering of SVG images in the game.

#### `onTouchEvent(MotionEvent event)`
- **Purpose:** Handles touch input during gameplay.
- **Parameters:**
  - `event`: The motion event containing touch information.
- **Return Value:** Boolean indicating if the event was handled.
- **Implementation Notes:** Detects taps for passing the potato in local mode.

---

## Multiplayer Functionality

### LAN Server

**File:** `LanServer.java`

The LanServer manages the host-side of network gameplay, handling client connections and game state broadcasting.

### Functions

#### `start(int port)`
- **Purpose:** Starts the server on the specified port.
- **Parameters:**
  - `port`: Port number to listen on.
- **Return Value:** Boolean indicating success or failure.
- **Implementation Notes:** Creates server socket and starts accept thread.

#### `stop()`
- **Purpose:** Stops the server and releases resources.
- **Parameters:** None
- **Return Value:** None (void)
- **Implementation Notes:** Closes all client connections and the server socket.

#### `acceptLoop()`
- **Purpose:** Continuously accepts new client connections.
- **Parameters:** None
- **Return Value:** None (void)
- **Implementation Notes:** Runs in a separate thread, creating handler threads for each client.

#### `broadcastState(String state)`
- **Purpose:** Sends current game state to all connected clients.
- **Parameters:**
  - `state`: JSON string representing game state.
- **Return Value:** None (void)
- **Implementation Notes:** Ensures all clients have synchronized game state.

#### `broadcastTick(long millisRemaining)`
- **Purpose:** Broadcasts time remaining to all clients.
- **Parameters:**
  - `millisRemaining`: Time remaining in milliseconds.
- **Return Value:** None (void)
- **Implementation Notes:** Keeps client timers synchronized with host.

#### `broadcastBurn(String loserName)`
- **Purpose:** Notifies all clients when a player loses.
- **Parameters:**
  - `loserName`: Name of the player who lost.
- **Return Value:** None (void)
- **Implementation Notes:** Triggers game over state on all clients.

#### `handleLine(String line, PrintWriter out)`
- **Purpose:** Processes incoming messages from clients.
- **Parameters:**
  - `line`: The received message.
  - `out`: PrintWriter for responding to the client.
- **Return Value:** None (void)
- **Implementation Notes:** Handles join requests and potato pass actions.

### LAN Client

**File:** `LanClient.java`

The LanClient manages the client-side of network gameplay, handling communication with the game host.

### Functions

#### `connect(String host, int port, String playerName)`
- **Purpose:** Connects to a game host server.
- **Parameters:**
  - `host`: Host address to connect to.
  - `port`: Port number to connect on.
  - `playerName`: Name of the local player.
- **Return Value:** Boolean indicating success or failure.
- **Implementation Notes:** Establishes socket connection and starts read thread.

#### `disconnect()`
- **Purpose:** Disconnects from the host server.
- **Parameters:** None
- **Return Value:** None (void)
- **Implementation Notes:** Closes socket connection and cleans up resources.

#### `sendPass(int toPlayerIdx)`
- **Purpose:** Sends a request to pass the potato to another player.
- **Parameters:**
  - `toPlayerIdx`: Index of the player to receive the potato.
- **Return Value:** None (void)
- **Implementation Notes:** Formats and sends the pass command to the server.

#### `readLoop()`
- **Purpose:** Continuously reads and processes messages from the server.
- **Parameters:** None
- **Return Value:** None (void)
- **Implementation Notes:** Runs in a separate thread, handling various server commands.

### Network Service Discovery Helper

**File:** `NsdHelper.java`

The NsdHelper facilitates network service discovery, allowing clients to find and connect to game hosts on the local network.

### Functions

#### `registerService(int port)`
- **Purpose:** Registers the host's game service for discovery.
- **Parameters:**
  - `port`: Port number the service is running on.
- **Return Value:** None (void)
- **Implementation Notes:** Uses Android's NSD API to make the service discoverable.

#### `discoverServices()`
- **Purpose:** Begins searching for available game services.
- **Parameters:** None
- **Return Value:** None (void)
- **Implementation Notes:** Uses Android's NSD API to discover services on the local network.

#### `stopDiscovery()`
- **Purpose:** Stops the service discovery process.
- **Parameters:** None
- **Return Value:** None (void)
- **Implementation Notes:** Cleans up discovery resources to prevent leaks.

#### `unregisterService()`
- **Purpose:** Unregisters the host's game service.
- **Parameters:** None
- **Return Value:** None (void)
- **Implementation Notes:** Removes the service from the network to prevent discovery after game end.

#### `resolveService(NsdServiceInfo serviceInfo)`
- **Purpose:** Resolves a discovered service to get connection details.
- **Parameters:**
  - `serviceInfo`: Basic information about the discovered service.
- **Return Value:** None (void)
- **Implementation Notes:** Retrieves host address and port for connection.

---

## Audio System

The audio system is integrated throughout the application, primarily in MainActivity and GameActivity.

### Key Components

#### Theme Music
- **Purpose:** Provides background music during menu navigation and gameplay.
- **Implementation:** Uses MediaPlayer to loop a theme track with dynamic stopping for gameplay mechanics.
- **Usage Example:**
  ```java
  // Initialize and play theme music
  themeMusic = MediaPlayer.create(this, R.raw.game_theme);
  themeMusic.setLooping(true);
  themeMusic.setVolume(0.7f, 0.7f);
  themeMusic.start();
  ```

#### Explosion Sound
- **Purpose:** Provides audio feedback when a player loses the game.
- **Implementation:** Triggered when the music stops randomly, creating the "hot potato" gameplay mechanic.
- **Usage Example:**
  ```java
  // Play explosion sound when game ends
  MediaPlayer explosion = MediaPlayer.create(context, R.raw.explosion);
  explosion.setOnCompletionListener(mp -> mp.release());
  explosion.start();
  ```

#### Dynamic Music Stopping
- **Purpose:** Replaces traditional timers with random music stopping to determine game over.
- **Implementation:** Music plays for a random duration between 5-15 seconds before stopping and triggering game over.
- **Relationship:** Works with the game state management in GameActivity to determine when a player loses.

---

This documentation covers the main functions and components of the Tato Aalu project. For specific implementation details, refer to the source code files directly.