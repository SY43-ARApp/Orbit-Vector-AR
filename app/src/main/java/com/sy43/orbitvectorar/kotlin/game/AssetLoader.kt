package com.sy43.orbitvectorar.kotlin.game

import android.util.Log
import com.sy43.orbitvectorar.java.common.samplerender.Mesh
import com.sy43.orbitvectorar.java.common.samplerender.SampleRender
import com.sy43.orbitvectorar.java.common.samplerender.Texture
import java.io.IOException
import com.sy43.orbitvectorar.R
class AssetLoader {
    companion object {
        private const val TAG = "AssetLoader"
    }

    lateinit var planetMesh: Mesh
    lateinit var appleMesh: Mesh
    lateinit var arrowMesh: Mesh
    lateinit var trajectoryDotMesh: Mesh

    val planetTextures: MutableList<Texture> = mutableListOf()
    val moonTextures: MutableList<Texture> = mutableListOf()
    lateinit var appleTexture: Texture
    lateinit var arrowTexture: Texture
    lateinit var trajectoryDotTexture: Texture

    var assetsLoaded = false

    @Throws(IOException::class)
    fun loadAssets(render: SampleRender) {
        if (assetsLoaded) return

        // load meshes from asset files
        planetMesh = Mesh.createFromAsset(render, GameConstants.PLANET_OBJ_FILE)
        appleMesh = Mesh.createFromAsset(render, GameConstants.APPLE_OBJ_FILE)
        arrowMesh = Mesh.createFromAsset(render, GameConstants.ARROW_OBJ_FILE)
        trajectoryDotMesh = Mesh.createFromAsset(render, GameConstants.TRAJECTORY_DOT_OBJ_FILE)

        // load planet textures
        planetTextures.clear()
        GameConstants.PLANET_TEXTURE_FILES.forEach {
            planetTextures.add(
                Texture.createFromAsset(
                    render,
                    it,
                    Texture.WrapMode.REPEAT,
                    Texture.ColorFormat.SRGB
                )
            )
        }
        // load moon textures
        moonTextures.clear()
        GameConstants.MOON_TEXTURE_FILES.forEach {
            moonTextures.add(
                Texture.createFromAsset(
                    render,
                    it,
                    Texture.WrapMode.REPEAT,
                    Texture.ColorFormat.SRGB
                )
            )
        }
        if (planetTextures.isEmpty()) {
            Log.e(TAG, "No planet textures loaded! Will crash bc nobody added fallback lol.")
        }
        if (moonTextures.isEmpty()) {
            Log.e(TAG, "No moon textures loaded! Will crash if moons are used.")
        }

        // load apple texture
        appleTexture = Texture.createFromAsset(
            render,
            GameConstants.APPLE_TEXTURE_FILE,
            Texture.WrapMode.REPEAT,
            Texture.ColorFormat.SRGB
        )
        // load arrow texture
        arrowTexture = Texture.createFromAsset(
            render,
            GameConstants.ARROW_TEXTURE_FILE,
            Texture.WrapMode.REPEAT,
            Texture.ColorFormat.SRGB
        )
        // load trajectory dot texture
        trajectoryDotTexture = Texture.createFromAsset(
            render,
            GameConstants.TRAJECTORY_DOT_TEXTURE_FILE,
            Texture.WrapMode.CLAMP_TO_EDGE,
            Texture.ColorFormat.SRGB
        )
        assetsLoaded = true
        Log.i(TAG, "Game assets loaded successfully.")
    }
}