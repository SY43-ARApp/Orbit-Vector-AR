package com.google.ar.core.examples.kotlin.helloar

object GameConstants {
    // --- AR PLACEMENT AND STABILITY ---
    const val MIN_TRACKING_FRAMES_FOR_ANCHOR_PLACEMENT = 60
    const val ANCHOR_LOST_RESET_THRESHOLD = 120

    // --- AR ANCHOR PLACEMENT TWEAKS ---
    const val ANCHOR_PLACEMENT_OFFSET_METERS = 1.5f 
    const val ANCHOR_PLACEMENT_HEIGHT_ABOVE_PLANE = 0.5f 

    // --- LEVEL GENERATION ---
    const val MAX_PLANETS_CAP = 25
    const val INITIAL_PLANET_COUNT = 3
    const val LEVELS_PER_NEW_PLANET = 1

    // --- CLUSTER GENERATION ---
    const val CLUSTER_MAX_RADIUS_APPLE = 1.0f
    const val CLUSTER_MAX_RADIUS_PLANETS = 0.7f
    const val CLUSTER_MIN_DIST_PLANETS_FROM_ANCHOR = 0.3f
    const val CLUSTER_VERTICAL_SPREAD_FACTOR = 0.5f

    // --- OBJECT MODEL DEFAULT RADII (from Blender, in meters) ---
    const val APPLE_MODEL_DEFAULT_RADIUS = 0.15f 
    const val PLANET_MODEL_DEFAULT_RADIUS = 0.20f
    const val ARROW_MODEL_DEFAULT_RADIUS = 0.03f 
    const val TRAJECTORY_DOT_MODEL_DEFAULT_RADIUS = 0.05f

    // --- OBJECT TARGET/COLLISION RADII (gameplay, in meters) ---
    const val APPLE_TARGET_RADIUS = APPLE_MODEL_DEFAULT_RADIUS
    const val PLANET_TARGET_RADIUS_MIN = PLANET_MODEL_DEFAULT_RADIUS * 1.5f
    const val PLANET_TARGET_RADIUS_MAX = PLANET_MODEL_DEFAULT_RADIUS * 2.5f
    const val ARROW_TARGET_RADIUS = ARROW_MODEL_DEFAULT_RADIUS
    const val TRAJECTORY_DOT_TARGET_RADIUS = 0.01f

    // --- PHYSICS AND GAMEPLAY ---
    const val PLANET_MASS_SCALE_FACTOR = 2000.0f
    const val ARROW_LAUNCH_SPEED = 10.0f
    const val ARROW_MASS = 0.5f
    var GRAVITY_CONSTANT = 0.05f
    const val INITIAL_ARROWS_PER_LEVEL = 1
    const val ARROW_LIFETIME_SECONDS = 3f

    // --- TRAJECTORY SIMULATION ---
    const val TRAJECTORY_SIMULATION_START_AT_STEP = 2
    var TRAJECTORY_SIMULATION_STEPS = 30 
    var TRAJECTORY_SIMULATION_TIMESTEP = 0.10f
    const val MAX_TRAJECTORY_DISTANCE = 5.0f

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
    const val SPAWN_OFFSET_DOWN = 0.35f
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
    const val MOON_MASS_SCALE_FACTOR = 1800.0f
}