package com.sy43.orbitvectorar.kotlin.game

import android.util.Log
import com.google.ar.core.Camera
import com.google.ar.core.Pose
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
        val commonFactor = GameConstants.GRAVITY_CONSTANT * celestialBodyMass / (distSq * dist)
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
            launchDirection[0] * GameConstants.ARROW_LAUNCH_SPEED,
            launchDirection[1] * GameConstants.ARROW_LAUNCH_SPEED,
            launchDirection[2] * GameConstants.ARROW_LAUNCH_SPEED
        )
        arrows.add(Arrow(startPosition.copyOf(), startVelocity.copyOf(),
            GameConstants.ARROW_MASS, true, System.currentTimeMillis()))
        gameState.arrowsLeft--
        gameState.points += 10
        Log.i(TAG, "Arrow launched. Arrows left: ${gameState.arrowsLeft})")
    }

    fun shouldShowTrajectory(gameState: GameState): Boolean {
        // Show trajectory only if there are arrows left and the game is in PLAYING state
        return gameState.state == PuzzleState.PLAYING && gameState.arrowsLeft > 0
    }

    private fun normalizeVec3(v: FloatArray): FloatArray {
        val len = sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2])
        if (len < 1e-6f) return floatArrayOf(0f, 0f, 0f)
        return floatArrayOf(v[0]/len, v[1]/len, v[2]/len)
    }

    private fun getArrowTipPosition(center: FloatArray, direction: FloatArray): FloatArray {
        val dirNorm = normalizeVec3(direction)
        return floatArrayOf(
            center[0] + dirNorm[0] * (GameConstants.ARROW_MODEL_LENGTH / 2f),
            center[1] + dirNorm[1] * (GameConstants.ARROW_MODEL_LENGTH / 2f),
            center[2] + dirNorm[2] * (GameConstants.ARROW_MODEL_LENGTH / 2f)
        )
    }

    // --- COLLISION CHECKS NOW USE ARROW TIP ---
    private fun checkCollision(
        arrowTip: FloatArray,
        planets: List<Planet>,
        moons: List<Moon>,
        apple: Apple?,
        gameState: GameState
    ): Boolean {
        // Check collisions
        apple?.let {
            val collisionDistSq = (GameConstants.ARROW_TARGET_RADIUS + GameConstants.APPLE_FORGIVING_COLLISION_RADIUS).pow(2)
            if (MathUtils.calculateDistanceSquared(arrowTip, it.worldPosition) < collisionDistSq) {
                gameState.points += 20
                return true
            }
        }
        for (planet in planets) {
            val collisionDistSq = (GameConstants.ARROW_TARGET_RADIUS + GameConstants.PLANET_FORGIVING_COLLISION_RADIUS).pow(2)
            if (MathUtils.calculateDistanceSquared(arrowTip, planet.worldPosition) < collisionDistSq) {
                gameState.points += 20
                return true
            }
        }
        for (moon in moons) {
            val moonPos = moon.getWorldPosition()
            val collisionDistSq = (GameConstants.ARROW_TARGET_RADIUS + GameConstants.MOON_FORGIVING_COLLISION_RADIUS).pow(2)
            if (MathUtils.calculateDistanceSquared(arrowTip, moonPos) < collisionDistSq) {
                gameState.points += 20
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
        yawOffset: Float = 0f,
        gameState: GameState
    ) {
        // --- NEW TRAJECTORY LOGIC: sample at fixed distance intervals, stop at collision ---
        val (arrowInitialPosition, launchDirection) = calculateInitialArrowSpawnData(startCamera, yawOffset)
        val startTip = getArrowTipPosition(arrowInitialPosition, launchDirection)
        val maxDistance = GameConstants.MAX_TRAJECTORY_DISTANCE
        val numDots = GameConstants.TRAJECTORY_SIMULATION_STEPS
        if (numDots < 1) return
        val interval = maxDistance / numDots

        // Simulate using small time steps, and place a dot every 'interval' meters along the path
        val simPosition = arrowInitialPosition.copyOf()
        val simVelocity = floatArrayOf(
            launchDirection[0] * GameConstants.ARROW_LAUNCH_SPEED,
            launchDirection[1] * GameConstants.ARROW_LAUNCH_SPEED,
            launchDirection[2] * GameConstants.ARROW_LAUNCH_SPEED
        )
        val timeStep = 0.01f // small for accuracy
        var traveled = 0f
        var lastTip = getArrowTipPosition(simPosition, simVelocity)
        trajectoryPoints.clear()
        trajectoryPoints.add(lastTip.copyOf()) // always add the starting tip

        var nextDotDist = interval
        var dotsPlaced = 1
        var collision = false

        // Simulate until maxDistance or collision or all dots placed
        while (traveled < maxDistance && dotsPlaced < numDots && !collision) {
            // Integrate physics
            val acc = floatArrayOf(0f, 0f, 0f)
            currentPlanets.forEach { planet ->
                val a = calculateGravitationalAcceleration(
                    simPosition, planet.worldPosition, planet.mass, planet.targetRadius * 0.5f
                )
                acc[0] += a[0]; acc[1] += a[1]; acc[2] += a[2]
            }
            currentMoons.forEach { moon ->
                val moonPos = moon.getWorldPosition()
                val a = calculateGravitationalAcceleration(
                    simPosition, moonPos, moon.mass, moon.targetRadius * 0.5f
                )
                acc[0] += a[0]; acc[1] += a[1]; acc[2] += a[2]
            }
            simVelocity[0] += acc[0] * timeStep
            simVelocity[1] += acc[1] * timeStep
            simVelocity[2] += acc[2] * timeStep
            simPosition[0] += simVelocity[0] * timeStep
            simPosition[1] += simVelocity[1] * timeStep
            simPosition[2] += simVelocity[2] * timeStep

            val tip = getArrowTipPosition(simPosition, simVelocity)
            val stepDist = MathUtils.calculateDistance(lastTip, tip)
            traveled += stepDist

            // Check for collision at this tip
            if (checkCollision(tip, currentPlanets, currentMoons, currentApple, gameState)) {
                collision = true
                trajectoryPoints.add(tip.copyOf())
                break
            }

            // Place a dot if passed the next interval
            if (traveled + 1e-4f >= nextDotDist) {
                trajectoryPoints.add(tip.copyOf())
                dotsPlaced++
                nextDotDist += interval
            }
            lastTip = tip
        }
        // If no collision and not enough dots, fill up to numDots with last tip
        while (trajectoryPoints.size < numDots) {
            trajectoryPoints.add(lastTip.copyOf())
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
            val arrowTip = getArrowTipPosition(arrow.position, arrow.velocity)
            if (checkCollision(arrowTip, currentPlanets, currentMoons, currentApple, gameState)) {
                arrow.active = false
                AudioManager.playSfx("arrowhit")
                if (currentApple != null && MathUtils.calculateDistanceSquared(arrowTip, currentApple.worldPosition) < (GameConstants.ARROW_TARGET_RADIUS + GameConstants.APPLE_FORGIVING_COLLISION_RADIUS).pow(2)) {
                        appleHitThisFrame = true
                }
                return@forEach
            }
            currentApple?.let { apple ->
                val collisionDistanceSq = (GameConstants.ARROW_TARGET_RADIUS + GameConstants.APPLE_FORGIVING_COLLISION_RADIUS).pow(2)
                if (MathUtils.calculateDistanceSquared(arrowTip, apple.worldPosition) < collisionDistanceSq) {
                    Log.i(TAG, "Apple hit!")
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