# রঙমহল (Rongmohal) — Original Bengali Card Game Android App

An original Bengali card game Android application built with **Kotlin**, **Jetpack Compose**, **Material 3**, **MVVM**, **Clean Architecture**, **Hilt**, **Coroutines & Flow**, **Room**, **DataStore**, and **Firebase (Auth, Realtime Database, Cloud Functions & Firestore)**.

> **Branding Note:** The application name (`app_name`) is configurable in `app/src/main/res/values/strings.xml` and synced with `metadata.json` (`রঙমহল`).

---

## Original Visual Identity & Design System

Rongmohal features a bespoke visual system inspired by Bangladeshi Nakshi Kantha embroidery, traditional Alpona geometric motifs, warm brasswork, and royal emerald/indigo velvet card tables—completely distinct from UNO or Western playing card decks:

- **Brand Logo & Adaptive Icon (`RongmohalBrandLogo.kt`, `ic_launcher_emblem.png`):** Custom octagonal Alpona seal with four fanned cards in emerald/crimson/indigo/gold and Bengali display typography (`Noto Serif Bengali`).
- **Four Original Suits (`RongmohalCards.kt`):**
  - **শাপলা (Shapla / Water Lily)** — সিঁদুর লাল (`#C62828`)
  - **পাতা (Pata / Emerald Leaf)** — পান্না সবুজ (`#157A4C`)
  - **তারা (Tara / Eight-Pointed Star)** — নীলকণ্ঠ নীল (`#1E50B5`)
  - **সূর্য (Surjo / Sunburst)** — সোনালি হলুদ (`#D98200`)
  - **মহল (Mohal / Royal Palace)** — রাজকীয় জাম (`#4A1942`, for wild & palace cards)
- **Original Card Components:**
  - `GameCardBack`: Deep indigo-emerald Nakshi Kantha weave, gold corner accents, and an eight-pointed Alpona mandala.
  - `GameNumberCard`: Warm ivory card stock, bold Bengali numerals (`০–৯`), and custom Compose `Canvas` suit emblems.
  - `GameActionCard`: Colored palace frames for **+২ টানুন**, **পালা বদল**, **গতি থামান**, **রঙ বদল**, and **মহল +৪**.
  - **Five Original Special Cards:** **বজ্রপাত (+৩)**, **হাতবদল**, **বাউন্স ব্যাক**, **সবাই মিলে +১**, and **রহস্য কার্ড**.
- **Six Custom Illustrated Player Avatars (`RongmohalAvatars.kt`):** বাউল শিল্পী, শাপলা কুসুম, বেঙ্গল টাইগার, নীল ময়ূর, পালতোলা নৌকা, and আলোক প্রদীপ—all drawn natively with Compose `Canvas`.

---

## Architecture & Package Structure

```text
com.example
├── BanglaCardGameApp.kt               # @HiltAndroidApp entry point
├── MainActivity.kt                    # @AndroidEntryPoint, enableEdgeToEdge(), root theme
├── core/
│   ├── config/                        # AppConfig, BengaliDigits, sealed UiState
│   └── network/                       # NetworkConnectivityObserver
├── domain/                            # Pure business models, game & AI engines, repositories & use cases
│   ├── ai/                            # Easy / Medium / Hard AI (বল্টু, মফিজ, টুনটুনি, ঝন্টু, পলাশ)
│   ├── game/                          # Pure Kotlin card engine (CardGameEngine, DeckGenerator, CardValidator)
│   ├── model/                         # AppPreferences, GameRoom, OnlineMultiplayerModels, MatchRecord
│   └── repository/                    # Auth, MatchHistory, MultiplayerLobby, OnlineMultiplayer, Preferences
├── data/
│   ├── local/                         # Room (MatchRecordDao, AppDatabase), DataStore (UserPreferencesDataSource)
│   └── remote/
│       ├── firestore/                 # Legacy Firestore data source & error handler
│       └── rtdb/                      # Firebase Realtime Database + Cloud Functions client & mappers
├── di/                                # Hilt DI modules (Database, DataStore, Firebase, Repository)
└── ui/
    ├── auth/                          # AuthGateScreen (Google Credential Manager + Offline Guest Mode)
    ├── components/                    # Brand logo, custom cards, avatars, card animations, table textures
    ├── navigation/                    # Type-safe @Serializable AppDestination + AppNavigation
    └── screens/                       # Splash, Home, Mode, PlayerSelection, Lobby, OnlineRoom, Board, Rules, Settings, Profile, Result
```

---

## Server-Authoritative Online Multiplayer Architecture (`functions/` + `database.rules.json`)

Online multiplayer is completely **server-authoritative** using **Firebase Realtime Database**, **Firebase Cloud Functions (Node.js 22 / TypeScript v2 Callables + RTDB triggers)**, and **zero-trust RTDB Security Rules**:

- **Hidden Cards Stay Private:** Deck order (`serverPrivate/drawPile`), discard history (`serverPrivate/discardPile`), and processed command log (`serverPrivate/processedActions`) live under `rooms/$roomId/serverPrivate` with `".read": false, ".write": false`. Each player can only read their own `rooms/$roomId/playerHands/$uid`.
- **Never Trust Client Moves or Scores:** All room creation (`createMultiplayerRoom`), room code joins (`joinRoomByCode`), ready toggles (`setPlayerReady`), match starts (`startMultiplayerMatch`), card plays/draws/declarations (`submitGameCommand`), and turn timeout claims (`claimTurnTimeout`) validate membership, optimistic concurrency (`expectedVersion`), duplicate idempotency (`actionId`), card ownership, and turn legality inside RTDB transactions on the server.
- **Presence & Reconnection:** Clients attach `.info/connected` + `onDisconnect()` handlers on their own `connected` / `lastSeenAt` nodes; `onPlayerPresenceChanged` updates disconnected status and mid-match reconnections restore the player's seat and private hand automatically.
- **Authoritative Leaderboard:** `/leaderboard/$uid` and `/profiles/$uid/stats` are server-write-only (`".write": false`).
- **Offline Mode Independence & Guest Mode:** Players can enter offline Guest Mode from `AuthGateScreen` (persisted via DataStore) to play Offline AI Classic, Blitz, and Tactical modes, browse rules, and track local Room history completely without internet. Online Multiplayer & Global Leaderboard prompt 1-tap Google Sign-In via Jetpack `CredentialManager` (Firebase Anonymous Auth is not required by the client and is not called in `app/src/main/`).

---

## Build, Backend Deployment & Test Instructions

### 1. Build & Run Android Unit Tests
```bash
# Build Debug APK
gradle :app:assembleDebug

# Run domain engines, AI, and multiplayer ViewModel/Robolectric unit tests
gradle :app:testDebugUnitTest --tests "com.example.domain.*" --tests "com.example.ui.*" --tests "com.example.ExampleUnitTest" --tests "com.example.ExampleRobolectricTest"
```

### 2. Cloud Functions Unit Tests & TypeScript Build
```bash
cd functions
npm install
npm test
```

### 3. Deploy Firebase Backend (RTDB Rules + Cloud Functions)
1. Enable **Realtime Database** in the Firebase Console (region matching `https://gen-lang-client-0257961535-default-rtdb.firebaseio.com`, configurable via `firebase_rtdb_url` in `app/src/main/res/values/strings.xml`).
2. Ensure **Google Sign-In** provider is enabled in **Firebase Authentication** (add your app's SHA-1/SHA-256 fingerprints in Project Settings).
3. Upgrade the Firebase project to Blaze (required for Cloud Functions v2) and deploy:
```bash
firebase deploy --only database,functions
```
*(Do NOT include Firebase service account JSON keys inside `app/src/`.)*

### 4. Firebase Security Rules Tests (Local Emulators)
```bash
# RTDB security rules tests
firebase emulators:exec --only database "node --test database.rules.test.js"

# Firestore security rules & Android Robolectric rules tests
firebase emulators:start --only firestore,auth --project demo-no-project
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
gradle :app:testDebugUnitTest --tests "*Rule*"
```
