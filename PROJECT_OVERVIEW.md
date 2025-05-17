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
- **Modern UI**: Jetpack Compose-based screens for title, menu, and end/game over, with custom fonts and animated effects.

## Main Kotlin Classes & Structure

- **HelloArActivity.kt**: Main AR activity. Sets up ARCore session, renderer, and view.
- **HelloArView.kt**: Handles UI, overlays, and user interaction (sliders, tap, dialogs).
- **HelloArRenderer.kt**: Core game logic, rendering loop, physics, object placement, model/texture loading. Manages game state, level reset, and frame updates. Handles both planets and moons.
- **GameObjectRenderer.kt**: Handles rendering of planets, moons, apples, arrows, and trajectory in the AR scene.
- **LevelManager.kt** (LevelGenerator): Procedurally generates planet, moon, and apple positions and properties for each level, including moon orbit logic.
- **PhysicsSimulator.kt**: Simulates arrow physics, gravity, collision, and trajectory prediction, including moving moons.
- **LightManager.kt**: Handles AR lighting estimation and shader updates.
- **MathUtils.kt**: Math helpers for vector/matrix operations and rotations.
- **GameData.kt**: Data classes for `Planet`, `Moon`, `Arrow`, `Apple`, `GameState`, `PuzzleState`, and `LevelCluster`.
- **GameConstants.kt**: Centralized constants for gameplay, physics, and rendering, including moon parameters.
- **AssetLoader.kt**: Loads 3D models and textures from assets, including moon textures.
- **AnchorManager.kt**: Manages ARCore anchors and tracking state.
- **TitleScreenActivity.kt**: Jetpack Compose title screen with animated logo and tap-to-play.
- **MenuScreenActivity.kt**: Jetpack Compose menu screen with Play button.
- **EndScreenActivity.kt**: Jetpack Compose end/game over screen showing score, points, and back-to-menu navigation.
- **ui/theme/Type.kt**: Custom font and typography definitions for Compose UI.
- **ui/theme/OrbitVectorARTheme.kt**: Light/dark color schemes and theme setup for Compose UI.

## Assets & Resources
- **assets/models/**: 3D models (arrow, apple, planets), DFG texture, shaders.
- **res/layout/**: UI layouts (activity_main, overlays).
- **res/values/**: Strings, arrays, and other resources.
- **res/drawable/**: App icons, logos, and Compose UI images.

## Game Loop & Flow
- **Main Loop**: On each frame, updates physics, checks collisions, draws AR scene.
- **Tap**: Launches an arrow from camera position toward aim direction.
- **Level Generation**: Each level randomizes planets (position, size, texture), moons (if enabled), and apple location.
- **UI**: Displays level, arrows left, and game state messages. Compose screens for title, menu, and end/game over.

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
  - EndScreenActivity.kt
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
**Note:** This overview reflects the current modular structure and file responsibilities as of May 2025, including the Compose UI screens and moons feature.
