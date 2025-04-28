package com.google.ar.core.examples.kotlin.helloar

import com.google.ar.core.Session
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.core.Frame
import com.google.ar.core.Camera

/**
 * Utility singleton for AR data and helpers.
 */
object ARUtils {
    /**
     * Returns all currently tracked planes.
     */
    fun getTrackedPlanes(session: Session): List<Plane> =
        session.getAllTrackables(Plane::class.java).filter { it.trackingState == TrackingState.TRACKING }

    /**
     * Returns true if at least one plane is tracked.
     */
    fun hasSurface(session: Session): Boolean =
        getTrackedPlanes(session).isNotEmpty()

    /**
     * Returns the center pose of the first detected plane, or null if none.
     */
    fun getFirstPlaneCenter(session: Session): Pose? =
        getTrackedPlanes(session).firstOrNull()?.centerPose

    /**
     * Example: spawn the default ARCore pawn at the center of the first detected plane.
     */
    fun trySpawnDefaultPawn(session: Session, engine: GameEngine) {
        val pose = getFirstPlaneCenter(session) ?: return
        if (engineInitializedPawn) return
        engine.addObject(DefaultPawnObject(pose))
        engineInitializedPawn = true
    }

    /**
     * Enable/disable grid visualization (to be implemented as needed).
     */
    var gridDebugEnabled = true

    // Prevent double-spawn
    private var engineInitializedPawn = false
}

/**
 * Example default pawn object (spawns ARCore pawn model at given pose).
 */
class DefaultPawnObject(val pose: Pose) : GameObject() {
    override fun update(session: Session, frame: Frame, camera: Camera) {
        // TODO: Render pawn at pose (integrate with renderer as needed)
    }
}