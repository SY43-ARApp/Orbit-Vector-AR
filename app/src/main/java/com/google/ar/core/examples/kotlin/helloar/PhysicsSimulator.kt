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

    val arrows: MutableList<Arrow> = mutableListOf()
    val trajectoryPoints: MutableList<FloatArray> = mutableListOf()

    fun launchArrow(camera: Camera, gameState: GameState) {
        if (gameState.arrowsLeft <= 0) {
            Log.d(TAG, "No arrows left to launch.")
            return
        }

        val camPose = camera.pose
        val startOffset = 0.2f // launch slightly in front of the camera // TODO make this a constant
        val forward = FloatArray(3).apply { camPose.getTransformedAxis(2, -1f, this, 0) } // -Z is forward

        val startPosition = floatArrayOf(
            camPose.tx() + forward[0] * startOffset,
            camPose.ty() + forward[1] * startOffset,
            camPose.tz() + forward[2] * startOffset
        )
        val startVelocity = floatArrayOf(
            forward[0] * ARROW_LAUNCH_SPEED,
            forward[1] * ARROW_LAUNCH_SPEED,
            forward[2] * ARROW_LAUNCH_SPEED
        )

        arrows.add(Arrow(startPosition.copyOf(), startVelocity.copyOf(), ARROW_MASS))
        gameState.arrowsLeft--
        Log.i(TAG, "Arrow launched. Arrows left: ${gameState.arrowsLeft}")
    }

    fun simulateArrowTrajectory(
        startCamera: Camera,
        currentPlanets: List<Planet>,
        currentApple: Apple?
    ) {
        trajectoryPoints.clear()
        if (currentPlanets.isEmpty() && currentApple == null) return 

        val camPose = startCamera.pose
        val startOffset = 0.2f
        val forward = FloatArray(3).apply { camPose.getTransformedAxis(2, -1f, this, 0) }

        val simPosition = floatArrayOf(
            camPose.tx() + forward[0] * startOffset,
            camPose.ty() + forward[1] * startOffset,
            camPose.tz() + forward[2] * startOffset
        )
        val simVelocity = floatArrayOf(
            forward[0] * ARROW_LAUNCH_SPEED,
            forward[1] * ARROW_LAUNCH_SPEED,
            forward[2] * ARROW_LAUNCH_SPEED
        )
        val simulatedArrowMass = ARROW_MASS

        for (step in 0 until TRAJECTORY_SIMULATION_STEPS) {
            // Newtonian gravity: F = G * m1 * m2 / r^2, update velocity and position
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

            // Euler integration: update position by velocity * timestep
            simPosition[0] += simVelocity[0] * TRAJECTORY_SIMULATION_TIMESTEP
            simPosition[1] += simVelocity[1] * TRAJECTORY_SIMULATION_TIMESTEP
            simPosition[2] += simVelocity[2] * TRAJECTORY_SIMULATION_TIMESTEP

            trajectoryPoints.add(simPosition.copyOf())

            // check collision with apple (sphere-sphere)
            currentApple?.let { apple ->
                val collisionDistSq = (ARROW_VISUAL_AND_COLLISION_RADIUS + apple.targetRadius).pow(2)
                if (MathUtils.calculateDistanceSquared(simPosition, apple.worldPosition) < collisionDistSq) {
                    return 
                }
            }
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