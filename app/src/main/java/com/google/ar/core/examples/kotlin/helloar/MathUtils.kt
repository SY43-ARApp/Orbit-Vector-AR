package com.google.ar.core.examples.kotlin.helloar

import android.opengl.Matrix
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

object MathUtils {

    fun calculateDistanceSquared(pos1: FloatArray, pos2: FloatArray): Float {
        // Euclidean distance squared (no sqrt)
        val dx = pos1[0] - pos2[0]
        val dy = pos1[1] - pos2[1]
        val dz = pos1[2] - pos2[2]
        return dx * dx + dy * dy + dz * dz
    }

    fun calculateDistance(pos1: FloatArray, pos2: FloatArray): Float {
        // Euclidean distance
        return sqrt(calculateDistanceSquared(pos1, pos2))
    }

    fun rotationMatrixFromTo(from: FloatArray, to: FloatArray, outMatrix: FloatArray) {
        // Rodrigues' rotation formula: axis-angle to matrix

        // Normalize vectors
        val fromNorm = from.clone()
        var len = Matrix.length(fromNorm[0], fromNorm[1], fromNorm[2])
        if (len == 0f) { Matrix.setIdentityM(outMatrix, 0); return }
        fromNorm[0] /= len; fromNorm[1] /= len; fromNorm[2] /= len

        val toNorm = to.clone()
        len = Matrix.length(toNorm[0], toNorm[1], toNorm[2])
        if (len == 0f) { Matrix.setIdentityM(outMatrix, 0); return }
        toNorm[0] /= len; toNorm[1] /= len; toNorm[2] /= len

        // cross product for axis, dot for angle
        val cross = floatArrayOf(
            fromNorm[1] * toNorm[2] - fromNorm[2] * toNorm[1],
            fromNorm[2] * toNorm[0] - fromNorm[0] * toNorm[2],
            fromNorm[0] * toNorm[1] - fromNorm[1] * toNorm[0]
        )
        val dot = fromNorm[0] * toNorm[0] + fromNorm[1] * toNorm[1] + fromNorm[2] * toNorm[2]
        val normCross = sqrt(cross[0] * cross[0] + cross[1] * cross[1] + cross[2] * cross[2])

        if (normCross < 1e-6) { // collinear
            if (dot > 0.9999f) { // and same direction
                Matrix.setIdentityM(outMatrix, 0)
            } else { // opposite direction, rotate 180° around any perpendicular axis
                var axis = floatArrayOf(1f, 0f, 0f)
                if (abs(fromNorm[0]) > 0.9f) axis = floatArrayOf(0f, 1f, 0f)
                // find orthogonal vector for axis
                val ortho = floatArrayOf(
                    fromNorm[1] * axis[2] - fromNorm[2] * axis[1],
                    fromNorm[2] * axis[0] - fromNorm[0] * axis[2],
                    fromNorm[0] * axis[1] - fromNorm[1] * axis[0]
                )
                val n = sqrt(ortho[0] * ortho[0] + ortho[1] * ortho[1] + ortho[2] * ortho[2])
                if (n > 1e-6) {
                    ortho[0] /= n; ortho[1] /= n; ortho[2] /= n
                    Matrix.setRotateM(outMatrix, 0, 180f, ortho[0], ortho[1], ortho[2])
                } else { 
                    Matrix.setIdentityM(outMatrix, 0)
                }
            }
            return
        }

        // axis-angle to matrix (Rodrigues)
        val axis = floatArrayOf(cross[0] / normCross, cross[1] / normCross, cross[2] / normCross)
        val angleRad = acos(dot.coerceIn(-1f, 1f).toDouble())
        val angleDeg = (angleRad * 180.0 / PI).toFloat()
        Matrix.setRotateM(outMatrix, 0, angleDeg, axis[0], axis[1], axis[2])
    }

    fun rotateVectorYaw(vec: FloatArray, yawRad: Float): FloatArray {
        // rotates a 3D vector around the Y axis by yawRad radians
        val cos = kotlin.math.cos(yawRad)
        val sin = kotlin.math.sin(yawRad)
        return floatArrayOf(
            vec[0] * cos - vec[2] * sin,
            vec[1],
            vec[0] * sin + vec[2] * cos
        )
    }
}