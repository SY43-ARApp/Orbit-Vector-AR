package com.google.ar.core.examples.kotlin.helloar

import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.Pose
import com.google.ar.core.examples.kotlin.helloar.GameConstants.APPLE_TARGET_RADIUS
import com.google.ar.core.examples.kotlin.helloar.GameConstants.CLUSTER_MAX_RADIUS_APPLE
import com.google.ar.core.examples.kotlin.helloar.GameConstants.CLUSTER_MAX_RADIUS_PLANETS
import com.google.ar.core.examples.kotlin.helloar.GameConstants.CLUSTER_MIN_DIST_PLANETS_FROM_ANCHOR
import com.google.ar.core.examples.kotlin.helloar.GameConstants.CLUSTER_VERTICAL_SPREAD_FACTOR
import com.google.ar.core.examples.kotlin.helloar.GameConstants.INITIAL_PLANET_COUNT
import com.google.ar.core.examples.kotlin.helloar.GameConstants.LEVELS_PER_NEW_MOON
import com.google.ar.core.examples.kotlin.helloar.GameConstants.LEVELS_PER_NEW_PLANET
import com.google.ar.core.examples.kotlin.helloar.GameConstants.MAX_PLANETS_CAP
import com.google.ar.core.examples.kotlin.helloar.GameConstants.MOON_ORBIT_RADIUS_MAX
import com.google.ar.core.examples.kotlin.helloar.GameConstants.MOON_ORBIT_RADIUS_MIN
import com.google.ar.core.examples.kotlin.helloar.GameConstants.MOON_ORBIT_SPEED_MAX
import com.google.ar.core.examples.kotlin.helloar.GameConstants.MOON_ORBIT_SPEED_MIN
import com.google.ar.core.examples.kotlin.helloar.GameConstants.MOON_START_LEVEL
import com.google.ar.core.examples.kotlin.helloar.GameConstants.PLANET_MASS_SCALE_FACTOR
import com.google.ar.core.examples.kotlin.helloar.GameConstants.PLANET_TARGET_RADIUS_MAX
import com.google.ar.core.examples.kotlin.helloar.GameConstants.PLANET_TARGET_RADIUS_MIN
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class LevelGenerator(private val assetLoader: AssetLoader) {
    companion object {
        private const val TAG = "LevelGenerator"
    }

    private var currentLevelCluster: LevelCluster? = null

    fun generateLevelLayout(anchor: Anchor, gameState: GameState) {
        Log.i(TAG, "Generating level layout for level ${gameState.level} around anchor.")
        val anchorPose = anchor.pose

        // --- Difficulty scaling parameters ---
        val level = gameState.level
        val numPlanetsToSpawn = kotlin.math.min(
            INITIAL_PLANET_COUNT + (level -1) / LEVELS_PER_NEW_PLANET + (level / 4), 
            MAX_PLANETS_CAP
        )

        val clusterMaxRadiusPlanets = CLUSTER_MAX_RADIUS_PLANETS + (level * 0.12f).coerceAtMost(3.0f)
        val planetTargetRadiusMin = PLANET_TARGET_RADIUS_MIN + (level * 0.01f).coerceAtMost(0.25f)
        val planetTargetRadiusMax = PLANET_TARGET_RADIUS_MAX + (level * 0.03f).coerceAtMost(1.2f)
        val planetMassScale = PLANET_MASS_SCALE_FACTOR + (level * 400f)
        val appleClusterRadius = CLUSTER_MAX_RADIUS_APPLE + (level * 0.03f).coerceAtMost(2.0f)
        val appleRadius = APPLE_TARGET_RADIUS

        val placedObjectLocalsAndRadii = mutableListOf<Pair<FloatArray, Float>>() // world positions for checking
        val planetLocals = mutableListOf<Triple<FloatArray, Float, Int>>() // localPos, mass, textureIdx
        val planetRadiiList = mutableListOf<Float>()

        // --- Place apple ---
        var appleLocalPos: FloatArray? = null
        for (i in 0..100) { // placement attempts
            val r = Random.nextFloat() * appleClusterRadius
            val theta = Random.nextFloat() * 2f * PI.toFloat()
            val phi = Random.nextFloat() * PI.toFloat() * CLUSTER_VERTICAL_SPREAD_FACTOR - (PI.toFloat() * CLUSTER_VERTICAL_SPREAD_FACTOR / 2f)
            val localX = r * cos(theta) * cos(phi)
            val localY = r * sin(phi)
            val localZ = r * sin(theta) * cos(phi)
            val currentLocalPos = floatArrayOf(localX, localY, localZ)
            val worldPos = anchorPose.transformPoint(currentLocalPos)

            var tooClose = false
            for ((otherWorldPos, otherRadius) in placedObjectLocalsAndRadii) {
                val minDistSq = (appleRadius + otherRadius + 0.2f).pow(2) // 0.2f buffer
                if (MathUtils.calculateDistanceSquared(worldPos, otherWorldPos) < minDistSq) {
                    tooClose = true
                    break
                }
            }
            if (!tooClose) {
                appleLocalPos = currentLocalPos
                placedObjectLocalsAndRadii.add(Pair(worldPos, appleRadius))
                break
            }
        }
        if (appleLocalPos == null) {
            Log.w(TAG, "Failed to place apple without collision, placing at anchor origin (local).")
            appleLocalPos = floatArrayOf(0f, 0f, 0f) 
            placedObjectLocalsAndRadii.add(Pair(anchorPose.transformPoint(appleLocalPos), appleRadius))
        }

        // --- Place planets ---
        var planetsSuccessfullyPlaced = 0
        var strongPlanetPlaced = false 

        val anchorWorldPos = anchorPose.translation
        val appleWorldPos = anchorPose.transformPoint(appleLocalPos)
        val anchorToAppleVec = floatArrayOf(
            appleWorldPos[0] - anchorWorldPos[0],
            appleWorldPos[1] - anchorWorldPos[1],
            appleWorldPos[2] - anchorWorldPos[2]
        )
        val anchorToAppleLen = sqrt(anchorToAppleVec[0].pow(2) + anchorToAppleVec[1].pow(2) + anchorToAppleVec[2].pow(2))
        if (anchorToAppleLen > 0.0001f) {
            anchorToAppleVec[0] /= anchorToAppleLen
            anchorToAppleVec[1] /= anchorToAppleLen
            anchorToAppleVec[2] /= anchorToAppleLen
        }
        val minBlockAngleCos = kotlin.math.cos(18.0 * PI / 180.0).toFloat() // Approx 18 deg cone

        for (pIdx in 0 until numPlanetsToSpawn) {
            val isStrongPlanet = !strongPlanetPlaced && (level >= 3) && (pIdx == numPlanetsToSpawn - 1 || Random.nextFloat() < 0.25f)
            val planetRad = if (isStrongPlanet)
                (planetTargetRadiusMax * 0.85f) + Random.nextFloat() * (planetTargetRadiusMax * 0.15f) // Larger strong planets
            else
                Random.nextFloat() * (planetTargetRadiusMax - planetTargetRadiusMin) + planetTargetRadiusMin

            var planetLocalAttemptPos: FloatArray? = null
            for (attempt in 0..300) { // Max placement attempts per planet
                val r = CLUSTER_MIN_DIST_PLANETS_FROM_ANCHOR + Random.nextFloat() * (clusterMaxRadiusPlanets - CLUSTER_MIN_DIST_PLANETS_FROM_ANCHOR)
                val theta = Random.nextFloat() * 2f * PI.toFloat()
                // Spread planets more horizontally than vertically to make game feel more natural in AR
                val phi = Random.nextFloat() * PI.toFloat() * CLUSTER_VERTICAL_SPREAD_FACTOR - (PI.toFloat() * CLUSTER_VERTICAL_SPREAD_FACTOR / 2f)
                val localX = r * cos(theta) * cos(phi)
                val localY = r * sin(phi) // Vertical position
                val localZ = r * sin(theta) * cos(phi)
                val currentLocalPos = floatArrayOf(localX, localY, localZ)
                val worldPos = anchorPose.transformPoint(currentLocalPos)

                var tooClose = false
                for ((otherWorldPos, otherRadius) in placedObjectLocalsAndRadii) {
                    val minDistSq = (planetRad + otherRadius + 0.2f).pow(2) // 0.2f buffer
                    if (MathUtils.calculateDistanceSquared(worldPos, otherWorldPos) < minDistSq) {
                        tooClose = true
                        break
                    }
                }
                if (tooClose) continue

                // Avoid placing planets directly between anchor and apple
                val anchorToPlanetVec = floatArrayOf(
                    worldPos[0] - anchorWorldPos[0],
                    worldPos[1] - anchorWorldPos[1],
                    worldPos[2] - anchorWorldPos[2]
                )
                val anchorToPlanetLen = sqrt(anchorToPlanetVec[0].pow(2) + anchorToPlanetVec[1].pow(2) + anchorToPlanetVec[2].pow(2))
                if (anchorToPlanetLen > 0.0001f) {
                    anchorToPlanetVec[0] /= anchorToPlanetLen
                    anchorToPlanetVec[1] /= anchorToPlanetLen
                    anchorToPlanetVec[2] /= anchorToPlanetLen
                }
                val dotProduct = anchorToAppleVec[0] * anchorToPlanetVec[0] + anchorToAppleVec[1] * anchorToPlanetVec[1] + anchorToAppleVec[2] * anchorToPlanetVec[2]

                if (dotProduct > minBlockAngleCos && anchorToPlanetLen < anchorToAppleLen) { // Planet is in cone towards apple and closer than apple
                    continue // Try another position
                }

                planetLocalAttemptPos = currentLocalPos
                placedObjectLocalsAndRadii.add(Pair(worldPos, planetRad))
                break
            }

            if (planetLocalAttemptPos != null) {
                val mass = planetMassScale * planetRad.pow(2.0f) * (if (isStrongPlanet) 1.3f else 1.0f) // Strong planets are heavier
                val textureIdx = planetsSuccessfullyPlaced % kotlin.math.max(1, assetLoader.planetTextures.size)
                planetLocals.add(Triple(planetLocalAttemptPos, mass, textureIdx))
                planetRadiiList.add(planetRad)
                planetsSuccessfullyPlaced++
                if (isStrongPlanet) strongPlanetPlaced = true
            } else {
                Log.w(TAG, "Failed to place planet ${pIdx + 1} after multiple attempts.")
            }
        }

        // --- Place moons (moving planets) ---
        val moons = mutableListOf<Moon>()
        if (level >= GameConstants.MOON_START_LEVEL) {
            val moonShouldSpawn = (level - GameConstants.MOON_START_LEVEL) % GameConstants.LEVELS_PER_NEW_MOON == 0
            val moonCount = kotlin.math.min(
                ((level - GameConstants.MOON_START_LEVEL) / GameConstants.LEVELS_PER_NEW_MOON + 1),
                GameConstants.MAX_MOONS_CAP
            )
            if (moonShouldSpawn) {
                // add a moon, skip adding a planet for this level
                if (planetLocals.isNotEmpty()) {
                    planetLocals.removeAt(planetLocals.lastIndex)
                    planetRadiiList.removeAt(planetRadiiList.lastIndex)
                }
                for (mIdx in 0 until moonCount) {
                    // random point around anchor, not too close to apple
                    val orbitRadius = Random.nextFloat() * (MOON_ORBIT_RADIUS_MAX - MOON_ORBIT_RADIUS_MIN) + MOON_ORBIT_RADIUS_MIN
                    val orbitSpeed = Random.nextFloat() * (MOON_ORBIT_SPEED_MAX - MOON_ORBIT_SPEED_MIN) + MOON_ORBIT_SPEED_MIN
                    val orbitPhase = Random.nextFloat() * 2f * PI.toFloat()
                    val moonRad = Random.nextFloat() * (GameConstants.MOON_TARGET_RADIUS_MAX - GameConstants.MOON_TARGET_RADIUS_MIN) + GameConstants.MOON_TARGET_RADIUS_MIN
                    val moonMass = GameConstants.MOON_MASS_SCALE_FACTOR * moonRad.pow(2.0f)
                    val theta = Random.nextFloat() * 2f * PI.toFloat()
                    val phi = Random.nextFloat() * PI.toFloat() * 0.5f - (PI.toFloat() * 0.25f)
                    val centerLocal = floatArrayOf(
                        (orbitRadius + 0.5f) * cos(theta) * cos(phi),
                        (orbitRadius + 0.5f) * sin(phi),
                        (orbitRadius + 0.5f) * sin(theta) * cos(phi)
                    )
                    val centerWorld = anchorPose.transformPoint(centerLocal)
                    val textureIdx = mIdx % kotlin.math.max(1, assetLoader.moonTextures.size)
                    moons.add(Moon(centerWorld, orbitRadius, orbitSpeed, orbitPhase, moonMass, textureIdx, moonRad))
                }
            }
        }

        Log.i(TAG, "Level generation complete. Apple OK. $planetsSuccessfullyPlaced/$numPlanetsToSpawn planets placed.")
        currentLevelCluster = LevelCluster(
            planetLocals = planetLocals,
            planetRadii = planetRadiiList,
            appleLocal = appleLocalPos,
            appleRadius = appleRadius,
            moons = moons
        )
    }

    fun getCurrentPlanetsWorld(anchorPose: Pose?): List<Planet> {
        val cluster = currentLevelCluster ?: return emptyList()
        val pose = anchorPose ?: return emptyList()

        return cluster.planetLocals.mapIndexedNotNull { idx, triple ->
            if (idx >= cluster.planetRadii.size) {
                Log.e(TAG, "Mismatch between planetLocals and planetRadii sizes.")
                return@mapIndexedNotNull null
            }
            val (localPos, mass, textureIdx) = triple
            val worldPos = pose.transformPoint(localPos)
            Planet(worldPos, mass, textureIdx, cluster.planetRadii[idx])
        }
    }

    fun getCurrentAppleWorld(anchorPose: Pose?): Apple? {
        val cluster = currentLevelCluster ?: return null
        val pose = anchorPose ?: return null
        val worldPos = pose.transformPoint(cluster.appleLocal)
        return Apple(worldPos, cluster.appleRadius)
    }

    fun getCurrentMoonsWorld(timeSeconds: Float): List<Moon> {
        val cluster = currentLevelCluster ?: return emptyList()
        return cluster.moons.map { moon ->
            moon.copy(currentAngle = (moon.currentAngle + moon.orbitSpeed * timeSeconds) % (2 * Math.PI).toFloat())
        }
    }

    fun clearLevelLayout() {
        currentLevelCluster = null
        Log.d(TAG, "Level layout cleared.")
    }
}