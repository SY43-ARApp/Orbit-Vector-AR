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
        @Query("score") score: Int,
        @Query("arrows_thrown") arrowsThrown: Int,
        @Query("planets_hit") planetsHit: Int,
        @Query("levels_passed") levelsPassed: Int
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

    @GET("get_player_rank.php")
    suspend fun getPlayerRank(
        @Query("uuid") uuid: String
    ): Response<PlayerRankResponse>

    @GET("get_all_skins.php")
    suspend fun getSkins(
    ): Response<List<Skin>>

    @GET("get_user_skins.php")
    suspend fun getUserSkins(
        @Query("uuid") uuid: String
    ): Response<List<UserSkins>>

    @GET("send_user_skin.php")
    suspend fun sendUserSkins(
        @Query("uuid") uuid: String,
        @Query("skinId") skinId: Int
    ): Response<String>

    @GET("get_user_money.php")
    suspend fun getMoney(
        @Query("uuid") uuid: String
    ): Response<UserMoney>

    //todo : faire une page pour récupérer le meilleur score du joueur ?
    @GET("")
    suspend fun getBestScores(
        @Query("uuid") uuid : String
    ): Response<UserBestScore>

    @GET("update_money.php")
    suspend fun updateMoney(
        @Query("uuid") uuid: String,
        @Query("money") money: Int?
    ): Response<String>

    companion object {
        const val BASE_URL = "https://chaelpixserver.ddns.net/apis/ovar/"
    }
}