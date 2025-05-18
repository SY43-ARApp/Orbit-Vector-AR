package com.google.ar.core.examples.kotlin.helloar.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("register.php")
    suspend fun register(
        @Query("uuid") uuid: String,
        @Query("username") username: String
    ): Response<String>

    @GET("login.php")
    suspend fun login(
        @Query("uuid") uuid: String
    ): Response<String>

    @GET("send_score.php")
    suspend fun sendScore(
        @Query("uuid") uuid: String,
        @Query("score") score: Int
    ): Response<String>

    @GET("get_global_scores.php")
    suspend fun getGlobalScores(
        @Query("limit") limit: Int? = null
    ): Response<List<GlobalScore>>

    @GET("get_user_scores.php")
    suspend fun getUserScores(
        @Query("uuid") uuid: String,
        @Query("limit") limit: Int? = null,
        @Query("order") order: String = "DESC",
        @Query("param") param: String = "score"
    ): Response<List<UserScore>>

    @GET("check_username.php")
    suspend fun checkUsername(
        @Query("username") username: String
    ): Response<CheckUsernameResponse>

    companion object {
        const val BASE_URL = "https://chaelpixserver.ddns.net/apis/ovar/"
    }
}