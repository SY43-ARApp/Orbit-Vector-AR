# Orbit Vector AR – Project Overview 

## Purpose & Requirements
- **AR Puzzle Game**: Shoot arrows to hit apples in AR, using real-world surfaces.
- **Planets**: Procedurally placed, exert gravity, visually diverse (size, texture).
- **Moons**: Moving planets (appear from a certain level), orbiting with unique textures and gravity, added for extra challenge and variety.
- **Arrow Physics**: Gravity from planets and moons affects arrow trajectory.
- **Victory**: Hit the apple.
- **Defeat**: Run out of arrows.
- **Replayability**: Procedural levels, increasing difficulty.
- **Visuals**: Custom 3D models (arrow, apple, planets, moons), random planet and moon textures.
- **Modern UI**: Jetpack Compose-based screens for title, menu, registration, and end/game over, with custom fonts and animated effects.
- **Online Features**: User registration, login, and global/user scoreboards via REST API.

## Main Kotlin Classes & Structure

- **HelloArActivity.kt**: Main AR activity. Sets up ARCore session, renderer, and view.
- **HelloArView.kt**: Handles UI, overlays, and user interaction (sliders, tap, dialogs).
- **HelloArRenderer.kt**: Core game logic, rendering loop, physics, object placement, model/texture loading. Manages game state, level reset, and frame updates. Handles both planets and moons.  
  - **Anchor Placement**: Anchor is placed on a detected plane if possible, or at a fallback distance (default 2.5m) in front of the camera for instant start.
- **GameObjectRenderer.kt**: Handles rendering of planets, moons, apples, arrows, and trajectory in the AR scene.
- **LevelManager.kt** (LevelGenerator): Procedurally generates planet, moon, and apple positions and properties for each level, including moon orbit logic. Now uses the anchor directly for world placement.
- **PhysicsSimulator.kt**: Simulates arrow physics, gravity, collision, and trajectory prediction, including moving moons.
- **LightManager.kt**: Handles AR lighting estimation and shader updates.
- **MathUtils.kt**: Math helpers for vector/matrix operations and rotations.
- **GameData.kt**: Data classes for `Planet`, `Moon`, `Arrow`, `Apple`, `GameState`, `PuzzleState`, and `LevelCluster`.
- **GameConstants.kt**: Centralized constants for gameplay, physics, and rendering, including moon parameters.
- **AssetLoader.kt**: Loads 3D models and textures from assets, including moon textures.
- **AnchorManager.kt**: Manages ARCore anchors and tracking state.
- **TitleScreenActivity.kt**: Jetpack Compose title screen with animated logo and tap-to-play.
- **MenuScreenActivity.kt**: Jetpack Compose menu screen with Play button, leaderboard, and battle pass.
- **FirstTimeScreenActivity.kt**: Jetpack Compose registration screen for first-time users, with username check, App Set ID/UUID, T&C and policy checkboxes, and animated loading overlay.
- **EndScreenActivity.kt**: Jetpack Compose end/game over screen showing score, points, and back-to-menu navigation.
- **data/ApiService.kt**: Retrofit API interface for registration, login, score submission, leaderboard, and username availability.
- **data/MainViewModel.kt**: ViewModel for user authentication, registration, and score management.
- **data/UserPreference.kt**: Handles persistent storage of user UUID/App Set ID and username.
- **data/Model.kt**: Data classes for API responses and score models.
- **ui/theme/Type.kt**: Custom font and typography definitions for Compose UI.
- **ui/theme/OrbitVectorARTheme.kt**: Light/dark color schemes and theme setup for Compose UI.

## Assets & Resources
- **assets/models/**: 3D models (arrow, apple, planets), DFG texture, shaders.
- **res/layout/**: UI layouts (activity_main, overlays, tracking_overlay).
- **res/values/**: Strings, arrays, and other resources.
- **res/drawable/**: App icons, logos, Compose UI images, and button/checkbox assets.

## Game Loop & Flow
- **Main Loop**: On each frame, updates physics, checks collisions, draws AR scene.
- **Tap**: Launches an arrow from camera position toward aim direction.
- **Level Generation**: Each level randomizes planets (position, size, texture), moons (if enabled), and apple location, using the anchor as the world reference.
- **UI**: Displays level, arrows left, and game state messages. Compose screens for title, menu, registration, and end/game over.
- **Anchor Placement**:  
  - Anchor is placed as soon as a plane is detected, or instantly at a fallback distance in front of the camera if no plane is found.
  - Plane mesh and point cloud are visualized during placement to provide user feedback.
- **User Registration & Login**:  
  - On first launch, user is prompted to register with a username and App Set ID (or fallback UUID).
  - Username is checked for availability via API before registration.
  - T&C and policy checkboxes must be accepted to proceed.
  - Animated loading overlay is shown during registration.
- **Scoreboards**:  
  - Global and user-specific leaderboards fetched from server.
  - Scores submitted after each game session.

## Directory Structure (Key Files)

- `app/src/main/java/com/google/ar/core/examples/kotlin/helloar/`
  - HelloArActivity.kt
  - HelloArView.kt
  - HelloArRenderer.kt
  - GameObjectRenderer.kt
  - LevelManager.kt
  - PhysicsSimulator.kt
  - LightManager.kt
  - MathUtils.kt
  - GameData.kt
  - GameConstants.kt
  - AssetLoader.kt
  - AnchorManager.kt
  - TitleScreenActivity.kt
  - MenuScreenActivity.kt
  - FirstTimeScreenActivity.kt
  - EndScreenActivity.kt
  - data/ApiService.kt
  - data/MainViewModel.kt
  - data/UserPreference.kt
  - data/Model.kt
  - ui/theme/Type.kt
  - ui/theme/OrbitVectorARTheme.kt
- `app/src/main/res/layout/`
- `app/src/main/res/values/`
- `app/src/main/res/drawable/`
- `app/src/main/assets/models/`

---
- **Build/Gradle**: Standard Android project structure with Gradle build files.
- **Docs**: Design docs, UML, and slides in `/docs` and `/uml`.

---
**Note:** This overview reflects the current modular structure and file responsibilities as of May 2025, including the Compose UI screens, moons feature, improved anchor/level system, and the full online registration/scoreboard flow.
