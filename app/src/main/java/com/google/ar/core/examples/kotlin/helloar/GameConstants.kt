package com.google.ar.core.examples.kotlin.helloar

object GameConstants {
    // --- AR PLACEMENT AND STABILITY ---
    const val MIN_TRACKING_FRAMES_FOR_ANCHOR_PLACEMENT = 60
    const val LEVEL_ANCHOR_DISTANCE_FORWARD = 8.5f
    const val LEVEL_ANCHOR_DISTANCE_UP = 1.5f
    const val ANCHOR_LOST_RESET_THRESHOLD = 120

    // --- LEVEL GENERATION ---
    const val MAX_PLANETS_CAP = 50
    const val INITIAL_PLANET_COUNT = 5
    const val LEVELS_PER_NEW_PLANET = 1

    // --- CLUSTER GENERATION ---
    const val CLUSTER_MAX_RADIUS_APPLE = 1.5f
    const val CLUSTER_MAX_RADIUS_PLANETS = 2.0f
    const val CLUSTER_MIN_DIST_PLANETS_FROM_ANCHOR = 0.5f
    const val CLUSTER_VERTICAL_SPREAD_FACTOR = 0.8f

    // --- OBJECT MODEL DEFAULT RADII (for scaling) ---
    const val APPLE_MODEL_DEFAULT_RADIUS = 0.1f
    const val PLANET_MODEL_DEFAULT_RADIUS = 0.1f
    const val ARROW_MODEL_DEFAULT_RADIUS = 0.1f
    const val TRAJECTORY_DOT_MODEL_DEFAULT_RADIUS = 0.05f

    // --- OBJECT TARGET/COLLISION RADII ---
    const val APPLE_TARGET_RADIUS = 0.2f
    const val PLANET_TARGET_RADIUS_MIN = 0.15f
    const val PLANET_TARGET_RADIUS_MAX = 0.55f
    const val ARROW_VISUAL_AND_COLLISION_RADIUS = 0.1f
    const val TRAJECTORY_DOT_TARGET_RADIUS = 0.01f

    // --- PHYSICS AND GAMEPLAY ---
    const val PLANET_MASS_SCALE_FACTOR = 4500.0f
    const val ARROW_LAUNCH_SPEED = 10.0f
    const val ARROW_MASS = 0.5f
    var GRAVITY_CONSTANT = 0.05f
    const val INITIAL_ARROWS_PER_LEVEL = 10
    const val ARROW_LIFETIME_SECONDS = 5f

    // --- TRAJECTORY SIMULATION ---
    const val TRAJECTORY_SIMULATION_START_AT_STEP = 2
    var TRAJECTORY_SIMULATION_STEPS = 70 
    var TRAJECTORY_SIMULATION_TIMESTEP = 0.10f
    const val MAX_TRAJECTORY_DISTANCE = 15.0f

    // --- ASSET PATHS ---
    val PLANET_TEXTURE_FILES = listOf(
        "models/textures/planet_texture_1.jpg",
        "models/textures/Tropical.png",
        "models/textures/Terrestrial1.png",
        "models/textures/Tropical2.jpg"
    )
    const val APPLE_TEXTURE_FILE = "models/textures/Apple_BaseColor.jpg"
    const val ARROW_TEXTURE_FILE = "models/textures/arrow_texture.png"
    const val TRAJECTORY_DOT_TEXTURE_FILE = "models/textures/dot_texture.jpg"

    const val PLANET_OBJ_FILE = "models/planet.obj"
    const val APPLE_OBJ_FILE = "models/apple.obj"
    const val ARROW_OBJ_FILE = "models/arrow.obj"
    const val TRAJECTORY_DOT_OBJ_FILE = "models/trajectory_dot.obj"

    // --- Arrow spawn/visual constants ---
    const val SPAWN_OFFSET_FORWARD = 0.6f
    const val SPAWN_OFFSET_DOWN = 0.4f
    const val MAX_X_OFFSET = 0.2f

    // --- MOON SYSTEM ---
    const val MOON_START_LEVEL = 2
    const val LEVELS_PER_NEW_MOON = 2
    const val MAX_MOONS_CAP = 8
    val MOON_TEXTURE_FILES = listOf(
        "models/textures/Icy.png",
        "models/textures/moon_mercury.jpg"
    )
    const val MOON_ORBIT_RADIUS_MIN = 0.7f
    const val MOON_ORBIT_RADIUS_MAX = 2.2f
    const val MOON_ORBIT_SPEED_MIN = 0.3f // radians/sec
    const val MOON_ORBIT_SPEED_MAX = 1.0f
    const val MOON_TARGET_RADIUS_MIN = 0.20f
    const val MOON_TARGET_RADIUS_MAX = 0.52f
    const val MOON_MASS_SCALE_FACTOR = 4200.0f
}