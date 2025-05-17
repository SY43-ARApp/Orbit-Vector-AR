package com.google.ar.core.examples.kotlin.helloar

import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.examples.kotlin.helloar.GameConstants.ANCHOR_LOST_RESET_THRESHOLD
import com.google.ar.core.examples.kotlin.helloar.GameConstants.LEVEL_ANCHOR_DISTANCE_FORWARD
import com.google.ar.core.examples.kotlin.helloar.GameConstants.LEVEL_ANCHOR_DISTANCE_UP
import com.google.ar.core.examples.kotlin.helloar.GameConstants.MIN_TRACKING_FRAMES_FOR_ANCHOR_PLACEMENT

class AnchorManager {
    companion object {
        private const val TAG = "AnchorManager"
    }

    private var levelOriginAnchor: Anchor? = null
    private var framesSinceTrackingStable = 0
    private var anchorWasLostFrames = 0

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

    fun attemptPlaceOrRestoreAnchor(session: Session, camera: Camera): Boolean {
        // anchor ok
        if (levelOriginAnchor != null && levelOriginAnchor!!.trackingState == TrackingState.TRACKING) 
        {
            anchorWasLostFrames = 0 
            return true
        }

        // missing anchor
        if (levelOriginAnchor == null || anchorWasLostFrames > ANCHOR_LOST_RESET_THRESHOLD) 
        {
            levelOriginAnchor?.detach()
            levelOriginAnchor = null

            // place anchor when camera is tracking and has been stable for enough frames
            if (camera.trackingState == TrackingState.TRACKING && framesSinceTrackingStable >= MIN_TRACKING_FRAMES_FOR_ANCHOR_PLACEMENT) 
            {
                val cameraPose = camera.pose
                val translationInCameraFrame = floatArrayOf(0f, LEVEL_ANCHOR_DISTANCE_UP, -LEVEL_ANCHOR_DISTANCE_FORWARD)
                val anchorPoseInWorld = cameraPose.compose(Pose.makeTranslation(translationInCameraFrame))

                return try {
                    levelOriginAnchor = session.createAnchor(anchorPoseInWorld)
                    Log.i(TAG, "Level anchor created/restored at ${anchorPoseInWorld.translation.joinToString()}")
                    anchorWasLostFrames = 0 
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create level anchor", e)
                    false
                }
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
}