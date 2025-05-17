package com.google.ar.core.examples.kotlin.helloar

import com.google.ar.core.examples.kotlin.helloar.GameConstants.INITIAL_ARROWS_PER_LEVEL

// --- GAME STATE DATA CLASSES ---
data class Planet(
    val worldPosition: FloatArray,
    val mass: Float,
    val textureIdx: Int,
    val targetRadius: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Planet
        if (!worldPosition.contentEquals(other.worldPosition)) return false
        if (mass != other.mass) return false
        if (textureIdx != other.textureIdx) return false
        if (targetRadius != other.targetRadius) return false
        return true
    }

    override fun hashCode(): Int {
        var result = worldPosition.contentHashCode()
        result = 31 * result + mass.hashCode()
        result = 31 * result + textureIdx
        result = 31 * result + targetRadius.hashCode()
        return result
    }
}

data class Arrow(
    var position: FloatArray,
    var velocity: FloatArray,
    val mass: Float,
    var active: Boolean = true,
    val launchTime: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Arrow
        if (!position.contentEquals(other.position)) return false
        return true
    }

    override fun hashCode(): Int {
        return position.contentHashCode()
    }
}

data class Apple(
    var worldPosition: FloatArray,
    val targetRadius: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Apple
        if (!worldPosition.contentEquals(other.worldPosition)) return false
        return true
    }

    override fun hashCode(): Int {
        return worldPosition.contentHashCode()
    }
}

data class Moon(
    var orbitCenter: FloatArray, 
    var orbitRadius: Float, 
    var orbitSpeed: Float, // radians/sec
    var orbitPhase: Float, // Initial phase offset (radians)
    var mass: Float,
    var textureIdx: Int,
    var targetRadius: Float,
    var currentAngle: Float = 0f
) {
    fun getWorldPosition(): FloatArray {
        val x = orbitCenter[0] + orbitRadius * kotlin.math.cos(currentAngle + orbitPhase)
        val y = orbitCenter[1]
        val z = orbitCenter[2] + orbitRadius * kotlin.math.sin(currentAngle + orbitPhase)
        return floatArrayOf(x, y, z)
    }
}

enum class PuzzleState {
    WAITING_FOR_ANCHOR,
    PLAYING,
    VICTORY,
    DEFEAT
}

data class GameState(
    var level: Int = 1,
    var points: Int = 0,
    var arrowsLeft: Int = INITIAL_ARROWS_PER_LEVEL,
    var state: PuzzleState = PuzzleState.WAITING_FOR_ANCHOR
)

data class LevelCluster(
    val planetLocals: List<Triple<FloatArray, Float, Int>>, // localPos, mass, textureIdx
    val planetRadii: List<Float>,
    val appleLocal: FloatArray,
    val appleRadius: Float,
    val moons: List<Moon> = emptyList() // Add moons to the cluster
)