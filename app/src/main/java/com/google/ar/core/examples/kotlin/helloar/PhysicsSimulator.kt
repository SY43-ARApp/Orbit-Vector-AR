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

    private val SPAWN_OFFSET_FORWARD = GameConstants.SPAWN_OFFSET_FORWARD
    private val SPAWN_OFFSET_DOWN = GameConstants.SPAWN_OFFSET_DOWN

    val arrows: MutableList<Arrow> = mutableListOf()
    val trajectoryPoints: MutableList<FloatArray> = mutableListOf()

    private fun calculateInitialArrowSpawnData(camera: Camera, yawOffset: Float): Pair<FloatArray, FloatArray> {
        val camPose = camera.pose
        val camForward = FloatArray(3).apply { camPose.getTransformedAxis(2, -1f, this, 0) }
        val camRight = FloatArray(3).apply { camPose.getTransformedAxis(0, 1f, this, 0) }
        val spawnPosition = floatArrayOf(
            camPose.tx() + camForward[0] * SPAWN_OFFSET_FORWARD + camRight[0] * SPAWN_OFFSET_DOWN,
            camPose.ty() + camForward[1] * SPAWN_OFFSET_FORWARD + camRight[1] * SPAWN_OFFSET_DOWN,
            camPose.tz() + camForward[2] * SPAWN_OFFSET_FORWARD + camRight[2] * SPAWN_OFFSET_DOWN
        )
        val launchDirection = MathUtils.rotateVectorYaw(camForward, yawOffset)
        return Pair(spawnPosition, launchDirection)
    }

    private fun calculateGravitationalAcceleration(
        objectPosition: FloatArray,
        celestialBodyPosition: FloatArray,
        celestialBodyMass: Float,
        bodyForceFieldMinRadius: Float
    ): FloatArray {
        // Newton's law of universal gravitation: a = (G * M) / r^3 * (r_vec)
        val dx = celestialBodyPosition[0] - objectPosition[0]
        val dy = celestialBodyPosition[1] - objectPosition[1]
        val dz = celestialBodyPosition[2] - objectPosition[2]
        var distSq = dx * dx + dy * dy + dz * dz
        val minForceApplicationDistSq = bodyForceFieldMinRadius.pow(2)
        if (distSq < minForceApplicationDistSq && minForceApplicationDistSq > 0.000001f) {
            distSq = minForceApplicationDistSq
        }
        distSq += 0.01f
        val dist = sqrt(distSq)
        if (dist < 0.00001f) {
            return floatArrayOf(0f, 0f, 0f)
        }
        val commonFactor = GRAVITY_CONSTANT * celestialBodyMass / (distSq * dist)
        return floatArrayOf(
            commonFactor * dx,
            commonFactor * dy,
            commonFactor * dz
        )
    }

    fun getReadyArrowPose(camera: Camera, yawOffset: Float = 0f): Pair<FloatArray, FloatArray> {
        return calculateInitialArrowSpawnData(camera, yawOffset)
    }

    fun launchArrow(camera: Camera, gameState: GameState, yawOffset: Float = 0f) {
        if (gameState.arrowsLeft <= 0) {
            Log.d(TAG, "No arrows left to launch.")
            return
        }
        val (startPosition, launchDirection) = calculateInitialArrowSpawnData(camera, yawOffset)
        val startVelocity = floatArrayOf(
            launchDirection[0] * ARROW_LAUNCH_SPEED,
            launchDirection[1] * ARROW_LAUNCH_SPEED,
            launchDirection[2] * ARROW_LAUNCH_SPEED
        )
        arrows.add(Arrow(startPosition.copyOf(), startVelocity.copyOf(), ARROW_MASS, true, System.currentTimeMillis()))
        gameState.arrowsLeft--
        Log.i(TAG, "Arrow launched. Arrows left: ${gameState.arrowsLeft})")
    }

    fun shouldShowTrajectory(gameState: GameState): Boolean {
        // Show trajectory only if there are arrows left and the game is in PLAYING state
        return gameState.state == PuzzleState.PLAYING && gameState.arrowsLeft > 0
    }

    private fun checkCollision(
        position: FloatArray,
        planets: List<Planet>,
        moons: List<Moon>,
        apple: Apple?
    ): Boolean {
        // Check collisions
        apple?.let {
            val collisionDistSq = (ARROW_VISUAL_AND_COLLISION_RADIUS + it.targetRadius).pow(2)
            if (MathUtils.calculateDistanceSquared(position, it.worldPosition) < collisionDistSq) {
                return true
            }
        }
        for (planet in planets) {
            val collisionDistSq = (ARROW_VISUAL_AND_COLLISION_RADIUS + planet.targetRadius).pow(2)
            if (MathUtils.calculateDistanceSquared(position, planet.worldPosition) < collisionDistSq) {
                return true
            }
        }
        for (moon in moons) {
            val moonPos = moon.getWorldPosition()
            val collisionDistSq = (ARROW_VISUAL_AND_COLLISION_RADIUS + moon.targetRadius).pow(2)
            if (MathUtils.calculateDistanceSquared(position, moonPos) < collisionDistSq) {
                return true
            }
        }
        return false
    }

    fun simulateArrowTrajectory(
        startCamera: Camera,
        currentPlanets: List<Planet>,
        currentMoons: List<Moon>,
        currentApple: Apple?,
        yawOffset: Float = 0f
    ) {
        // Kinematic equations: x = x0 + v * t + 0.5 * a * t^2 (Euler integration step)
        val (arrowInitialPosition, launchDirection) = calculateInitialArrowSpawnData(startCamera, yawOffset)
        val simPosition = arrowInitialPosition.copyOf()
        val simVelocity = floatArrayOf(
            launchDirection[0] * ARROW_LAUNCH_SPEED,
            launchDirection[1] * ARROW_LAUNCH_SPEED,
            launchDirection[2] * ARROW_LAUNCH_SPEED
        )
        val oversampleSteps = TRAJECTORY_SIMULATION_STEPS * 10
        val allPositions = mutableListOf<FloatArray>()
        allPositions.add(simPosition.copyOf())
        var totalDistanceTraveled = 0f
        for (step in 1..oversampleSteps) {
            val accumulatedAcceleration = floatArrayOf(0f, 0f, 0f)
            currentPlanets.forEach { planet ->
                val accel = calculateGravitationalAcceleration(
                    simPosition,
                    planet.worldPosition,
                    planet.mass,
                    planet.targetRadius * 0.5f
                )
                accumulatedAcceleration[0] += accel[0]
                accumulatedAcceleration[1] += accel[1]
                accumulatedAcceleration[2] += accel[2]
            }
            currentMoons.forEach { moon ->
                val moonPos = moon.getWorldPosition()
                val accel = calculateGravitationalAcceleration(
                    simPosition,
                    moonPos,
                    moon.mass,
                    moon.targetRadius * 0.5f
                )
                accumulatedAcceleration[0] += accel[0]
                accumulatedAcceleration[1] += accel[1]
                accumulatedAcceleration[2] += accel[2]
            }
            simVelocity[0] += accumulatedAcceleration[0] * TRAJECTORY_SIMULATION_TIMESTEP
            simVelocity[1] += accumulatedAcceleration[1] * TRAJECTORY_SIMULATION_TIMESTEP
            simVelocity[2] += accumulatedAcceleration[2] * TRAJECTORY_SIMULATION_TIMESTEP
            simPosition[0] += simVelocity[0] * TRAJECTORY_SIMULATION_TIMESTEP
            simPosition[1] += simVelocity[1] * TRAJECTORY_SIMULATION_TIMESTEP
            simPosition[2] += simVelocity[2] * TRAJECTORY_SIMULATION_TIMESTEP
            totalDistanceTraveled = MathUtils.calculateDistance(simPosition, arrowInitialPosition)
            if (totalDistanceTraveled > GameConstants.MAX_TRAJECTORY_DISTANCE) {
                break
            }

            if (checkCollision(simPosition, currentPlanets, currentMoons, currentApple)) {
                allPositions.add(simPosition.copyOf())
                break
            }
            allPositions.add(simPosition.copyOf())
            currentApple?.let { apple ->
                val collisionDistSq = (ARROW_VISUAL_AND_COLLISION_RADIUS + apple.targetRadius).pow(2)
                if (MathUtils.calculateDistanceSquared(simPosition, apple.worldPosition) < collisionDistSq) {
                }
            }
        }
        trajectoryPoints.clear()
        if (allPositions.size < 2) {
            if(allPositions.isNotEmpty()) trajectoryPoints.add(allPositions.first().copyOf())
            return
        }
        val cumulativeDistances = FloatArray(allPositions.size)
        cumulativeDistances[0] = 0f
        for (i in 1 until allPositions.size) {
            cumulativeDistances[i] = cumulativeDistances[i-1] + MathUtils.calculateDistance(allPositions[i], allPositions[i-1])
        }
        val totalActualPathLength = cumulativeDistances.last()
        val maxDistForResampling = totalActualPathLength.coerceAtMost(GameConstants.MAX_TRAJECTORY_DISTANCE)
        val numResampleOutputPoints = TRAJECTORY_SIMULATION_STEPS
        if (numResampleOutputPoints <= 0) return
        for (i in 0 until numResampleOutputPoints) {
            val targetDist = if (numResampleOutputPoints == 1) 0f 
                             else maxDistForResampling * i / (numResampleOutputPoints - 1)
            var segmentIndex = 0
            while (segmentIndex < cumulativeDistances.size - 2 && cumulativeDistances[segmentIndex+1] < targetDist) {
                segmentIndex++
            }
            val p0 = allPositions[segmentIndex]
            val p1 = allPositions[segmentIndex+1]
            val segmentActualLength = cumulativeDistances[segmentIndex+1] - cumulativeDistances[segmentIndex]
            val t = if (segmentActualLength > 0.00001f) {
                (targetDist - cumulativeDistances[segmentIndex]) / segmentActualLength
            } else {
                0f
            }.coerceIn(0f, 1f)
            val interpolatedPoint = floatArrayOf(
                p0[0] + (p1[0] - p0[0]) * t,
                p0[1] + (p1[1] - p0[1]) * t,
                p0[2] + (p1[2] - p0[2]) * t
            )
            trajectoryPoints.add(interpolatedPoint)
        }
        if (trajectoryPoints.isEmpty() && allPositions.isNotEmpty()) {
             trajectoryPoints.add(allPositions.first().copyOf())
        }
    }

    fun updateGamePhysics(
        dt: Float,
        currentPlanets: List<Planet>,
        currentMoons: List<Moon>,
        currentApple: Apple?,
        levelOriginAnchorPose: Pose?,
        gameState: GameState
    ): Boolean {
        // Kinematic update: v = v0 + a*dt, x = x0 + v*dt
        if (gameState.state != PuzzleState.PLAYING) return false
        var appleHitThisFrame = false
        arrows.filter { it.active }.forEach { arrow ->
            val accumulatedAcceleration = floatArrayOf(0f, 0f, 0f)
            currentPlanets.forEach { planet ->
                val accel = calculateGravitationalAcceleration(
                    arrow.position,
                    planet.worldPosition,
                    planet.mass,
                    planet.targetRadius * 0.5f
                )
                accumulatedAcceleration[0] += accel[0]
                accumulatedAcceleration[1] += accel[1]
                accumulatedAcceleration[2] += accel[2]
            }
            currentMoons.forEach { moon ->
                val moonPos = moon.getWorldPosition()
                val accel = calculateGravitationalAcceleration(
                    arrow.position,
                    moonPos,
                    moon.mass,
                    moon.targetRadius * 0.5f
                )
                accumulatedAcceleration[0] += accel[0]
                accumulatedAcceleration[1] += accel[1]
                accumulatedAcceleration[2] += accel[2]
            }
            arrow.velocity[0] += accumulatedAcceleration[0] * dt
            arrow.velocity[1] += accumulatedAcceleration[1] * dt
            arrow.velocity[2] += accumulatedAcceleration[2] * dt
            arrow.position[0] += arrow.velocity[0] * dt
            arrow.position[1] += arrow.velocity[1] * dt
            arrow.position[2] += arrow.velocity[2] * dt
            // Stop arrow if it collides with any object
            if (checkCollision(arrow.position, currentPlanets, currentMoons, currentApple)) {
                arrow.active = false
                if (currentApple != null && MathUtils.calculateDistanceSquared(arrow.position, currentApple.worldPosition) < (ARROW_VISUAL_AND_COLLISION_RADIUS + currentApple.targetRadius).pow(2)) {
                    appleHitThisFrame = true
                }
                return@forEach
            }
            currentApple?.let { apple ->
                val collisionDistanceSq = (ARROW_VISUAL_AND_COLLISION_RADIUS + apple.targetRadius).pow(2)
                if (MathUtils.calculateDistanceSquared(arrow.position, apple.worldPosition) < collisionDistanceSq) {
                    Log.i(TAG, "Apple hit!")
                    gameState.points += 100 * gameState.level
                    arrow.active = false
                    appleHitThisFrame = true
                }
            }
            val currentTime = System.currentTimeMillis()
            if ((currentTime - arrow.launchTime) / 1000f > GameConstants.ARROW_LIFETIME_SECONDS) {
                arrow.active = false
                Log.d(TAG, "Arrow deactivated, lifetime expired.")
            }
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