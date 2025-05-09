# Orbit Vector AR – Quick Project Overview

## Purpose & Requirements
- **AR Puzzle Game**: Shoot arrows to hit apples in AR, using real-world surfaces.
- **Planets**: Procedurally placed, exert gravity, visually diverse (size, texture).
- **Arrow Physics**: Gravity from planets affects arrow trajectory.
- **Victory**: Hit the apple.
- **Defeat**: Run out of arrows.
- **Replayability**: Procedural levels, increasing difficulty.
- **Visuals**: Custom 3D models (arrow, apple, planets), random planet textures.

## Project Construction (Kotlin Classes)
- **HelloArActivity.kt**: Main AR activity. Sets up ARCore session, renderer, and view.
- **HelloArView.kt**: Handles UI, overlays, and user interaction.
- **HelloArRenderer.kt**: Core game logic, rendering loop, physics, object placement, model/texture loading.
- **Game Data Classes**: `Planet`, `Arrow`, `Apple`, `GameState`, `PuzzleState` (in renderer).
- **Assets**: 3D models and textures in `assets/models/`.

---
- **Main Loop**: On each frame, updates physics, checks collisions, draws AR scene.
- **Tap**: Launches an arrow from camera position toward aim direction.
- **Level Generation**: Each level randomizes planets (position, size, texture).
