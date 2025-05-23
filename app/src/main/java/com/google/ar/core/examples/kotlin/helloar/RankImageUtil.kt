package com.google.ar.core.examples.kotlin.helloar

import com.google.ar.core.examples.kotlin.helloar.R

object RankImageUtil {
    fun getRankImageRes(playerPos: Int, totalPlayers: Int): Int {
        if (playerPos <= 1) return R.drawable.top1
        if (playerPos == 2) return R.drawable.top2
        if (playerPos == 3) return R.drawable.top3

        val buckets = 25
        val others = totalPlayers - 3
        if (others <= 0) return R.drawable.style_1_01

        val posInOthers = playerPos - 3
        // Reverse: best = 25, worst = 1
        val bucket = buckets - (((posInOthers - 1) * buckets) / others)
        val clampedBucket = bucket.coerceIn(1, buckets)
        val resName = "style_1_%02d".format(clampedBucket)
        val resId = try {
            val field = R.drawable::class.java.getField(resName)
            field.getInt(null)
        } catch (e: Exception) {
            R.drawable.style_1_01
        }
        return resId
    }
}
