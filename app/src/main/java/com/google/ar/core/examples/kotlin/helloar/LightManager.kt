package com.google.ar.core.examples.kotlin.helloar

import android.opengl.Matrix
import com.google.ar.core.LightEstimate
import com.google.ar.core.examples.java.common.samplerender.Shader

class LightManager {
    // update light for rendering, using default values (not ARCore estimate)
    fun updateLightEstimation(
        lightEstimate: LightEstimate,
        viewMatrix: FloatArray,
        shader: Shader,
        viewInverseMatrix: FloatArray, // inverse of viewMatrix
        sphericalHarmonicsCoefficients: FloatArray, // will be filled
        viewLightDirection: FloatArray // will be filled
    ) {
        // always use fixed light (not ARCore)
        shader.setBool("u_LightEstimateIsValid", false)

        // default light points down (Y+)
        val defaultLightDirectionWorld = floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f)
        val defaultLightIntensity = floatArrayOf(0.8f, 0.8f, 0.8f)

        // transform light direction to view space
        Matrix.multiplyMV(viewLightDirection, 0, viewMatrix, 0, defaultLightDirectionWorld, 0)
        // normalize direction
        val len = Matrix.length(viewLightDirection[0], viewLightDirection[1], viewLightDirection[2])
        if (len != 0f) {
            viewLightDirection[0] /= len; viewLightDirection[1] /= len; viewLightDirection[2] /= len;
        }

        shader.setMat4("u_ViewInverse", viewInverseMatrix)
        shader.setVec4("u_ViewLightDirection", viewLightDirection)
        shader.setVec3("u_LightIntensity", defaultLightIntensity)

        // set ambient light (spherical harmonics) to neutral value
        for (i in sphericalHarmonicsCoefficients.indices) {
            sphericalHarmonicsCoefficients[i] = 0.3f
        }
        // if using ARCore, fill with real harmonics (disabled here)
        // lightEstimate.environmentalHdrAmbientSphericalHarmonics?.let { harmonics ->
        //    for (i in sphericalHarmonicsCoefficients.indices) sphericalHarmonicsCoefficients[i] = harmonics[i]
        // }
        shader.setVec3Array("u_SphericalHarmonicsCoefficients", sphericalHarmonicsCoefficients)
    }
}