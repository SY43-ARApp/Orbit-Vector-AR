package com.google.ar.core.examples.kotlin.helloar.data

data class GlobalScore(
    val username: String,
    val score: Int? = 0
) {
    val actualScore: Int
        get() = score ?: 0
}

data class User(
    val money :Int?,
    val bestScore : Int?
)


data class UserScore(
    val score: Int? = 0,
    val time: String,
    val arrows_thrown: Int? = null,
    val planets_hit: Int? = null,
    val levels_passed: Int? = null
) {
    val actualScore: Int
        get() = score ?: 0
}

data class RegisterResponse(
    val uuid: String
)

data class CheckUsernameResponse(
    val available: Boolean,
    val message: String? = null
)

data class PlayerRankResponse(
    val rank: Int,
    val totalPlayers: Int
)

data class Skin
(
    val id: Int,
    val price: Int,
    val minimalScore: Int,
    val idType : Int
)

data class UserSkins(
    val skinId: Int
)