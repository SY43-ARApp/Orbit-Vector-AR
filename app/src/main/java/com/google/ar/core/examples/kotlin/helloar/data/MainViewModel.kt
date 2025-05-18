package com.google.ar.core.examples.kotlin.helloar.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

private const val TAG = "MainViewModel"

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferences = UserPreferences(application)
    private val _uiState = MutableStateFlow(UiState(uuid = userPreferences.uuid))
    val uiState = _uiState.asStateFlow()

    private val api: ApiService by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl(ApiService.BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(ApiService::class.java)
    }

    init {
        Log.d(TAG, "Initializing MainViewModel with UUID: ${userPreferences.uuid}")
        checkLogin()
    }

    private fun checkLogin() {
        viewModelScope.launch {
            try {
                val response = api.login(userPreferences.uuid)
                Log.d(TAG, "Login response: ${response.body()}")
                
                if (response.isSuccessful) {
                    when (response.body()) {
                        "GOOD" -> {
                            Log.d(TAG, "Login successful, updating state")
                            _uiState.value = _uiState.value.copy(
                                isLoggedIn = true,
                                username = userPreferences.username,
                                needsRegistration = false,
                                lastErrorMessage = null
                            )
                            refreshScores()
                        }
                        "UNKNOWN_UUID" -> {
                            Log.d(TAG, "Unknown UUID, needs registration")
                            _uiState.value = _uiState.value.copy(
                                isLoggedIn = false,
                                needsRegistration = true,
                                lastErrorMessage = null
                            )
                        }
                        else -> {
                            Log.e(TAG, "Unexpected login response: ${response.body()}")
                            _uiState.value = _uiState.value.copy(
                                isLoggedIn = false,
                                needsRegistration = true,
                                lastErrorMessage = "Login failed: unexpected response"
                            )
                        }
                    }
                } else {
                    Log.e(TAG, "Login failed: ${response.errorBody()?.string()}")
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = false,
                        needsRegistration = true,
                        lastErrorMessage = "Login failed"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login error", e)
                _uiState.value = _uiState.value.copy(
                    isLoggedIn = false,
                    needsRegistration = true,
                    lastErrorMessage = "Login error: ${e.message}"
                )
            }
        }
    }

    fun register(username: String) {
        Log.d(TAG, "Attempting to register user: $username with UUID: ${userPreferences.uuid}")
        viewModelScope.launch {
            try {
                val response = api.register(userPreferences.uuid, username)
                Log.d(TAG, "Register response: ${response.body()}")
                
                if (response.isSuccessful && response.body()?.startsWith("REGISTERED:") == true) {
                    val registeredUuid = response.body()?.substringAfter("REGISTERED:") ?: ""
                    if (registeredUuid == userPreferences.uuid) {
                        Log.d(TAG, "Registration successful")
                        userPreferences.username = username
                        _uiState.value = _uiState.value.copy(
                            isLoggedIn = true,
                            username = username,
                            needsRegistration = false,
                            lastErrorMessage = null
                        )
                        refreshScores()
                    } else {
                        Log.e(TAG, "UUID mismatch: expected ${userPreferences.uuid}, got $registeredUuid")
                        _uiState.value = _uiState.value.copy(
                            lastErrorMessage = "Registration failed: UUID mismatch"
                        )
                    }
                } else {
                    Log.e(TAG, "Registration failed: ${response.errorBody()?.string()}")
                    _uiState.value = _uiState.value.copy(
                        lastErrorMessage = "Registration failed"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Register error", e)
                _uiState.value = _uiState.value.copy(
                    lastErrorMessage = "Registration error: ${e.message}"
                )
            }
        }
    }

    fun sendScore(score: Int) {
        if (!_uiState.value.isLoggedIn) {
            Log.w(TAG, "Attempted to send score while not logged in")
            return
        }
        
        Log.d(TAG, "Sending score: $score for UUID: ${_uiState.value.uuid}")
        viewModelScope.launch {
            try {
                val response = api.sendScore(_uiState.value.uuid, score)
                Log.d(TAG, "Send score response: ${response.body()}")
                
                if (response.isSuccessful && response.body() == "SCORE_ADDED") {
                    Log.d(TAG, "Score added successfully")
                    _uiState.value = _uiState.value.copy(lastErrorMessage = null)
                    refreshScores()
                } else {
                    Log.e(TAG, "Failed to send score: ${response.errorBody()?.string()}")
                    _uiState.value = _uiState.value.copy(
                        lastErrorMessage = "Failed to send score"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Send score error", e)
                _uiState.value = _uiState.value.copy(
                    lastErrorMessage = "Send score error: ${e.message}"
                )
            }
        }
    }

    fun setGlobalView(isGlobal: Boolean) {
        Log.d(TAG, "Setting global view: $isGlobal")
        _uiState.value = _uiState.value.copy(isGlobalView = isGlobal)
        refreshScores()
    }

    fun refreshScores() {
        Log.d(TAG, "Refreshing scores (Global: ${_uiState.value.isGlobalView})")
        viewModelScope.launch {
            try {
                if (_uiState.value.isGlobalView) {
                    val response = api.getGlobalScores()
                    Log.d(TAG, "Global scores response: ${response.body()}")
                    
                    if (response.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            globalScores = response.body() ?: emptyList(),
                            lastErrorMessage = null
                        )
                    } else {
                        Log.e(TAG, "Failed to get global scores: ${response.errorBody()?.string()}")
                        _uiState.value = _uiState.value.copy(
                            lastErrorMessage = "Failed to get global scores"
                        )
                    }
                } else if (_uiState.value.isLoggedIn) {
                    val response = api.getUserScores(_uiState.value.uuid)
                    Log.d(TAG, "User scores response: ${response.body()}")
                    
                    if (response.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            userScores = response.body() ?: emptyList(),
                            lastErrorMessage = null
                        )
                    } else {
                        Log.e(TAG, "Failed to get user scores: ${response.errorBody()?.string()}")
                        _uiState.value = _uiState.value.copy(
                            lastErrorMessage = "Failed to get user scores"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Refresh scores error", e)
                _uiState.value = _uiState.value.copy(
                    lastErrorMessage = "Refresh scores error: ${e.message}"
                )
            }
        }
    }

    data class UiState(
        val uuid: String,
        val username: String? = null,
        val isLoggedIn: Boolean = false,
        val needsRegistration: Boolean = false,
        val isGlobalView: Boolean = true,
        val globalScores: List<GlobalScore> = emptyList(),
        val userScores: List<UserScore> = emptyList(),
        val lastErrorMessage: String? = null
    )
}