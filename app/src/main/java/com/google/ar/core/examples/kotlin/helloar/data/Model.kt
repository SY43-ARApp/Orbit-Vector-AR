package com.google.ar.core.examples.kotlin.helloar.data

data class GlobalScore(
    val username: String,
    val score: Int? = 0
) {
    val actualScore: Int
        get() = score ?: 0
}

data class UserScore(
    val score: Int? = 0,
    val time: String
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