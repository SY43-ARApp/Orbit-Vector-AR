package com.google.ar.core.examples.kotlin.helloar

import android.util.Log
import com.google.ar.core.Camera
import com.google.ar.core.Pose
import com.google.ar.core.examples.kotlin.helloar.GameConstants.ARROW_LAUNCH_SPEED
import com.google.ar.core.examples.kotlin.helloar.GameConstants.ARROW_MASS
import com.google.ar.core.examples.kotlin.helloar.GameConstants.ARROW_VISUAL_AND_COLLISION_RADIUS
import com.google.ar.core.examples.kotlin.helloar.GameConstants.GRAVITY_CONSTANT
import com.google.ar.core.examples.kotlin.helloar.GameConstants.TRAJECTORY_SIMULATION_STEPS
import com.google.ar.core.examples.kotlin.helloar.GameConstants.TRAJECTORY_SIMULATION_TIMESTEP
import kotlin.math.pow
import kotlin.math.sqrt

class PhysicsSimulator {
    companion object {
        private const val TAG = "PhysicsSimulator"
    }

    // --- Arrow spawn/visual constants ---
    private val SPAWN_OFFSET_FORWARD = GameConstants.SPAWN_OFFSET_FORWARD
    private val SPAWN_OFFSET_DOWN = GameConstants.SPAWN_OFFSET_DOWN
    private val MAX_X_OFFSET = GameConstants.MAX_X_OFFSET

    val arrows: MutableList<Arrow> = mutableListOf()
    val trajectoryPoints: MutableList<FloatArray> = mutableListOf()

    fun getReadyArrowPose(camera: Camera, xOffset: Float = 0f): Pair<FloatArray, FloatArray> {
        val camPose = camera.pose
        val forward = FloatArray(3).apply { camPose.getTransformedAxis(2, -1f, this, 0) }
        val up = FloatArray(3).apply { camPose.getTransformedAxis(1, 1f, this, 0) }
        val right = FloatArray(3).apply { camPose.getTransformedAxis(0, 1f, this, 0) }
        val horizontalOffset = xOffset.coerceIn(-MAX_X_OFFSET, MAX_X_OFFSET)
        val startPosition = floatArrayOf(
            camPose.tx() + forward[0] * SPAWN_OFFSET_FORWARD + up[0] * horizontalOffset + right[0] * SPAWN_OFFSET_DOWN,
            camPose.ty() + forward[1] * SPAWN_OFFSET_FORWARD + up[1] * horizontalOffset + right[1] * SPAWN_OFFSET_DOWN,
            camPose.tz() + forward[2] * SPAWN_OFFSET_FORWARD + up[2] * horizontalOffset + right[2] * SPAWN_OFFSET_DOWN
        )
        return Pair(startPosition, forward)
    }

    fun launchArrow(camera: Camera, gameState: GameState, xOffset: Float = 0f) {
        if (gameState.arrowsLeft <= 0) {
            Log.d(TAG, "No arrows left to launch.")
            return
        }
        val camPose = camera.pose
        val forward = FloatArray(3).apply { camPose.getTransformedAxis(2, -1f, this, 0) }
        val up = FloatArray(3).apply { camPose.getTransformedAxis(1, 1f, this, 0) }
        val right = FloatArray(3).apply { camPose.getTransformedAxis(0, 1f, this, 0) }
        val horizontalOffset = xOffset.coerceIn(-MAX_X_OFFSET, MAX_X_OFFSET)
        val startPosition = floatArrayOf(
            camPose.tx() + forward[0] * SPAWN_OFFSET_FORWARD + up[0] * horizontalOffset + right[0] * SPAWN_OFFSET_DOWN,
            camPose.ty() + forward[1] * SPAWN_OFFSET_FORWARD + up[1] * horizontalOffset + right[1] * SPAWN_OFFSET_DOWN,
            camPose.tz() + forward[2] * SPAWN_OFFSET_FORWARD + up[2] * horizontalOffset + right[2] * SPAWN_OFFSET_DOWN
        )
        val startVelocity = floatArrayOf(
            forward[0] * ARROW_LAUNCH_SPEED,
            forward[1] * ARROW_LAUNCH_SPEED,
            forward[2] * ARROW_LAUNCH_SPEED
        )
        arrows.add(Arrow(startPosition.copyOf(), startVelocity.copyOf(), ARROW_MASS))
        gameState.arrowsLeft--
        Log.i(TAG, "Arrow launched. Arrows left: ${gameState.arrowsLeft})")
    }

    fun simulateArrowTrajectory(
        startCamera: Camera,
        currentPlanets: List<Planet>,
        currentApple: Apple?,
        xOffset: Float = 0f
    ) {
        // --- Simulate the full path with fine steps ---
        val camPose = startCamera.pose
        val forward = FloatArray(3).apply { camPose.getTransformedAxis(2, -1f, this, 0) }
        val up = FloatArray(3).apply { camPose.getTransformedAxis(1, 1f, this, 0) }
        val right = FloatArray(3).apply { camPose.getTransformedAxis(0, 1f, this, 0) }
        val horizontalOffset = xOffset.coerceIn(-MAX_X_OFFSET, MAX_X_OFFSET)
        val simPosition = floatArrayOf(
            camPose.tx() + forward[0] * SPAWN_OFFSET_FORWARD + up[0] * horizontalOffset + right[0] * SPAWN_OFFSET_DOWN,
            camPose.ty() + forward[1] * SPAWN_OFFSET_FORWARD + up[1] * horizontalOffset + right[1] * SPAWN_OFFSET_DOWN,
            camPose.tz() + forward[2] * SPAWN_OFFSET_FORWARD + up[2] * horizontalOffset + right[2] * SPAWN_OFFSET_DOWN
        )
        val startPosition = simPosition.copyOf()
        val simVelocity = floatArrayOf(
            forward[0] * ARROW_LAUNCH_SPEED,
            forward[1] * ARROW_LAUNCH_SPEED,
            forward[2] * ARROW_LAUNCH_SPEED
        )
        val simulatedArrowMass = ARROW_MASS
        val oversampleSteps = GameConstants.TRAJECTORY_SIMULATION_STEPS * 10
        val allPositions = mutableListOf<FloatArray>()
        allPositions.add(simPosition.copyOf())
        var totalDistance = 0f
        for (step in 1..oversampleSteps) {
            // Physics step
            currentPlanets.forEach { planet ->
                val dx = planet.worldPosition[0] - simPosition[0]
                val dy = planet.worldPosition[1] - simPosition[1]
                val dz = planet.worldPosition[2] - simPosition[2]
                var distSq = dx * dx + dy * dy + dz * dz
                val minCollisionDistSq = (planet.targetRadius * 0.5f).pow(2)
                if (distSq < minCollisionDistSq) distSq = minCollisionDistSq
                distSq += 0.01f
                val dist = sqrt(distSq)
                if (dist > 0.0001f) {
                    val forceMagnitude = GRAVITY_CONSTANT * planet.mass * simulatedArrowMass / distSq
                    simVelocity[0] += (forceMagnitude * dx / dist) * TRAJECTORY_SIMULATION_TIMESTEP
                    simVelocity[1] += (forceMagnitude * dy / dist) * TRAJECTORY_SIMULATION_TIMESTEP
                    simVelocity[2] += (forceMagnitude * dz / dist) * TRAJECTORY_SIMULATION_TIMESTEP
                }
            }
            simPosition[0] += simVelocity[0] * TRAJECTORY_SIMULATION_TIMESTEP
            simPosition[1] += simVelocity[1] * TRAJECTORY_SIMULATION_TIMESTEP
            simPosition[2] += simVelocity[2] * TRAJECTORY_SIMULATION_TIMESTEP
            totalDistance = MathUtils.calculateDistance(simPosition, startPosition)
            if (totalDistance > GameConstants.MAX_TRAJECTORY_DISTANCE) break
            allPositions.add(simPosition.copyOf())
            currentApple?.let { apple ->
                val collisionDistSq = (ARROW_VISUAL_AND_COLLISION_RADIUS + apple.targetRadius).pow(2)
                if (MathUtils.calculateDistanceSquared(simPosition, apple.worldPosition) < collisionDistSq) {
                    //break
                }
            }
        }
        // --- Resample to get exactly TRAJECTORY_SIMULATION_STEPS points, evenly spaced from 0 to MAX_TRAJECTORY_DISTANCE ---
        val cumulativeDistances = FloatArray(allPositions.size)
        cumulativeDistances[0] = 0f
        for (i in 1 until allPositions.size) {
            cumulativeDistances[i] = cumulativeDistances[i-1] + MathUtils.calculateDistance(allPositions[i], allPositions[i-1])
        }
        trajectoryPoints.clear()
        val steps = GameConstants.TRAJECTORY_SIMULATION_STEPS
        for (i in 0 until steps) {
            val targetDist = GameConstants.MAX_TRAJECTORY_DISTANCE * i / (steps - 1).coerceAtLeast(1)
            // Find segment containing this distance
            var seg = 0
            while (seg < cumulativeDistances.size - 2 && cumulativeDistances[seg+1] < targetDist) seg++
            val t = if (cumulativeDistances[seg+1] > cumulativeDistances[seg])
                (targetDist - cumulativeDistances[seg]) / (cumulativeDistances[seg+1] - cumulativeDistances[seg])
            else 0f
            val p0 = allPositions[seg]
            val p1 = allPositions[seg+1]
            val interp = floatArrayOf(
                p0[0] + (p1[0] - p0[0]) * t,
                p0[1] + (p1[1] - p0[1]) * t,
                p0[2] + (p1[2] - p0[2]) * t
            )
            trajectoryPoints.add(interp)
        }
    }

    fun updateGamePhysics(
        dt: Float,
        currentPlanets: List<Planet>,
        currentApple: Apple?,
        levelOriginAnchorPose: Pose?,
        gameState: GameState // To modify state on hit
    ): Boolean {
        if (gameState.state != PuzzleState.PLAYING) return false

        var appleHitThisFrame = false

        arrows.filter { it.active }.forEach { arrow ->
            // Newtonian gravity: F = G * m1 * m2 / r^2, update velocity
            currentPlanets.forEach { planet ->
                val dx = planet.worldPosition[0] - arrow.position[0]
                val dy = planet.worldPosition[1] - arrow.position[1]
                val dz = planet.worldPosition[2] - arrow.position[2]
                var distSq = dx * dx + dy * dy + dz * dz
                val minCollisionDistSq = (planet.targetRadius * 0.5f).pow(2)
                if (distSq < minCollisionDistSq) distSq = minCollisionDistSq
                distSq += 0.01f

                val dist = sqrt(distSq)
                if (dist > 0.0001f) {
                    val forceMagnitude = GRAVITY_CONSTANT * planet.mass * arrow.mass / distSq
                    arrow.velocity[0] += (forceMagnitude * dx / dist) * dt
                    arrow.velocity[1] += (forceMagnitude * dy / dist) * dt
                    arrow.velocity[2] += (forceMagnitude * dz / dist) * dt
                }
            }

            // Euler integration: update position by velocity * dt
            arrow.position[0] += arrow.velocity[0] * dt
            arrow.position[1] += arrow.velocity[1] * dt
            arrow.position[2] += arrow.velocity[2] * dt

            // check collision with apple (sphere-sphere)
            currentApple?.let { apple ->
                val collisionDistanceSq = (ARROW_VISUAL_AND_COLLISION_RADIUS + apple.targetRadius).pow(2)
                if (MathUtils.calculateDistanceSquared(arrow.position, apple.worldPosition) < collisionDistanceSq) {
                    Log.i(TAG, "Apple hit!");
                    gameState.score += 100 * gameState.level
                    arrow.active = false
                    appleHitThisFrame = true
                }
            }

            // Deactivate arrow if it flies too far
            // levelOriginAnchorPose?.translation?.let { origin ->
            //     if (MathUtils.calculateDistanceSquared(arrow.position, origin) > 50f.pow(2)) {
            //         arrow.active = false
            //         Log.d(TAG, "Arrow deactivated, too far from origin.")
            //     }
            // }
        }
        return appleHitThisFrame
    }

    fun clearArrows() {
        arrows.clear()
        Log.d(TAG, "All arrows cleared.")
    }

    fun clearTrajectory() {
        trajectoryPoints.clear()
    }

    fun hasActiveArrows(): Boolean {
        return arrows.any { it.active }
    }
}