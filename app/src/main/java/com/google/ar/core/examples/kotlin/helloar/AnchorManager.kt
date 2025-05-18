package com.google.ar.core.examples.kotlin.helloar

import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.examples.kotlin.helloar.GameConstants.ANCHOR_LOST_RESET_THRESHOLD
import com.google.ar.core.examples.kotlin.helloar.GameConstants.LEVEL_ANCHOR_DISTANCE_FORWARD
import com.google.ar.core.examples.kotlin.helloar.GameConstants.LEVEL_ANCHOR_DISTANCE_UP
import com.google.ar.core.examples.kotlin.helloar.GameConstants.MIN_TRACKING_FRAMES_FOR_ANCHOR_PLACEMENT
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.examples.kotlin.helloar.GameConstants.ANCHOR_PLACEMENT_OFFSET_METERS
import com.google.ar.core.examples.kotlin.helloar.GameConstants.ANCHOR_PLACEMENT_HEIGHT_ABOVE_PLANE

class AnchorManager {
    companion object {
        private const val TAG = "AnchorManager"
    }

    private var levelOriginAnchor: Anchor? = null
    private var framesSinceTrackingStable = 0
    private var anchorWasLostFrames = 0
    private var savedAnchorPose: Pose? = null

    fun getAnchor(): Anchor? = levelOriginAnchor
    fun isAnchorTracking(): Boolean = levelOriginAnchor?.trackingState == TrackingState.TRACKING
    fun getAnchorPose(): Pose? = if (isAnchorTracking()) levelOriginAnchor?.pose else null

    // count frames when tracking is stable
    fun updateTrackingStability(cameraTrackingState: TrackingState, currentPuzzleState: PuzzleState) {
        if (cameraTrackingState == TrackingState.TRACKING) 
        {
            if (framesSinceTrackingStable < MIN_TRACKING_FRAMES_FOR_ANCHOR_PLACEMENT + 10) 
            { 
                framesSinceTrackingStable++
            }
        } 
        else {
            framesSinceTrackingStable = 0
        }
    }

    fun updateAnchorLostStatus(currentPuzzleState: PuzzleState): PuzzleState {
        var newPuzzleState = currentPuzzleState

        if (levelOriginAnchor != null && levelOriginAnchor!!.trackingState != TrackingState.TRACKING) 
        {
            anchorWasLostFrames++
            // if playing and lost anchor for too long, update puzzle state
            if (currentPuzzleState == PuzzleState.PLAYING) 
            {
                Log.w(TAG, "Anchor tracking lost during play.")
                newPuzzleState = PuzzleState.WAITING_FOR_ANCHOR
            }
        } else {
            anchorWasLostFrames = 0
        }

        return newPuzzleState
    }


    fun tryPlaceAnchorOnPlane(session: Session, frame: Frame, screenWidth: Int, screenHeight: Int): Boolean {
        if (levelOriginAnchor != null && levelOriginAnchor!!.trackingState == TrackingState.TRACKING) {
            anchorWasLostFrames = 0
            return true
        }

        val hits = frame.hitTest((screenWidth / 2).toFloat(), (screenHeight / 2).toFloat())
        for (hit in hits) {
            val trackable = hit.trackable
            if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose) && trackable.trackingState == TrackingState.TRACKING) {
                val cameraPose = frame.camera.pose
                val forward = floatArrayOf(0f, 0f, -1f)
                val worldForward = cameraPose.rotateVector(forward)
                val offsetPose = hit.hitPose
                    .compose(com.google.ar.core.Pose.makeTranslation(
                        worldForward[0] * ANCHOR_PLACEMENT_OFFSET_METERS,
                        ANCHOR_PLACEMENT_HEIGHT_ABOVE_PLANE,
                        worldForward[2] * ANCHOR_PLACEMENT_OFFSET_METERS
                    ))
                levelOriginAnchor?.detach()
                levelOriginAnchor = session.createAnchor(offsetPose)
                Log.i(TAG, "Anchor placed on plane at offset pose: ${levelOriginAnchor?.pose?.translation?.joinToString()}")
                anchorWasLostFrames = 0
                return true
            }
        }
        return false
    }

    fun detachCurrentAnchor() {
        levelOriginAnchor?.detach()
        levelOriginAnchor = null
        anchorWasLostFrames = 0
        framesSinceTrackingStable = 0 
        Log.i(TAG, "Level anchor detached.")
    }

    fun saveAnchorPoseIfTracking() {
        if (isAnchorTracking()) {
            savedAnchorPose = levelOriginAnchor?.pose
        }
    }

    fun tryRestoreAnchorFromSavedPose(session: Session): Boolean {
        if (savedAnchorPose != null) {
            try {
                levelOriginAnchor?.detach()
                levelOriginAnchor = session.createAnchor(savedAnchorPose!!)
                Log.i(TAG, "Restored anchor from saved pose.")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore anchor from saved pose", e)
            }
        }
        return false
    }
}