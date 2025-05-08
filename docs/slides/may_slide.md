---
title: "Orbit Vector"
separator: /s
verticalSeparator: /v
theme: solarized
revealOptions:
  transition: 'fade'
---


<!-- .slide: data-background="./img/background.png" -->

# Orbit Vector

---

### Perrin Antoine, Beaujard Traïan, Martin, Alex
#### SY43 - P25

/s

## Introduction

- Why this subject ?

/s

# UI

## UI Slide 1

/v

## UI Slide 2

/v

## UI Slide 3

/s

# Base de Données

/v

## Structure de la Base de Données

### Table USER
- **uuid** (Primary Key) : Identifiant unique de l'utilisateur
- **name** : Nom de l'utilisateur
- **bestscore** : Meilleur score de l'utilisateur

/v

### Table SCORE
- **id** (Primary Key) : Identifiant unique du score
- **uuid** (Foreign Key) : Référence vers l'utilisateur
- **time** : Date et heure du score
- **score** : Valeur du score

/v

## Relations
- Un utilisateur peut avoir plusieurs scores (1:N)
- Chaque score appartient à un seul utilisateur
- La clé étrangère uuid dans SCORE référence la clé primaire de USER

/v

## MCD

![MCD](img/MCD.png)

/s

### Connection entre Kotlin et Base de Données

### API PHP

- Utilisation du site hébergé par Traïan : [OrbitVectorAPI](http://chaelpixserver.ddns.net/apis/ovar/)
- Le PHP interagit avec la base de données MySQL
- Les requêtes sont envoyées via des appels HTTP GET

/v

## PHP -> Kotlin

- Utilisation de la bibliothèque Retrofit pour les appels HTTP
- Les réponses sont traitées en JSON
- Les données sont envoyées sous forme de paramètres d'URL (GET pour des requêtes simples)
- Les réponses sont transformées en objets Kotlin facilement manipulables
- Exemple de requête : `http://chaelpixserver.ddns.net/apis/ovar/register.php?uuid=1234&username=JohnDoe

/v

### Endpoints PHP

<div style="text-align: center; transform: translateX(-5%);">
    <table style="font-size: 0.8em;">
        <thead>
            <tr>
                <th>Endpoint</th>
                <th>Méthode</th>
                <th>Paramètres</th>
                <th>Réponse</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td>register.php</td>
                <td>GET</td>
                <td>uuid, username</td>
                <td>"REGISTERED:{UUID}"</td>
            </tr>
            <tr>
                <td>login.php</td>
                <td>GET</td>
                <td>uuid</td>
                <td>"GOOD"/"UNKNOWN_UUID"</td>
            </tr>
            <tr>
                <td>send_score.php</td>
                <td>GET</td>
                <td>uuid, score</td>
                <td>"SCORE_ADDED"/"FAIL"</td>
            </tr>
            <tr>
                <td>get_global_scores.php</td>
                <td>GET</td>
                <td>-</td>
                <td>JSON scores</td>
            </tr>
            <tr>
                <td>get_user_scores.php</td>
                <td>GET</td>
                <td>uuid</td>
                <td>JSON scores</td>
            </tr>
        </tbody>
    </table>
</div>


/v

### Exemple Code API

`ApiService.kt`

```kotlin [30:]
@GET("get_user_scores.php")
    suspend fun getUserScores(
        @Query("uuid") uuid: String,
        @Query("limit") limit: Int? = null,
        @Query("order") order: String = "DESC",
        @Query("param") param: String = "score"
    ): Response<List<UserScore>>
```

/v

### Utilisation de l'API dans le code Kotlin
`MainViewModel.kt`
```kotlin [191:]
else if (_uiState.value.isLoggedIn) {
    val response = api.getUserScores(_uiState.value.uuid)
    Log.d(TAG, "User scores response: ${response.body()}")
    
    if (response.isSuccessful) {
        _uiState.value = _uiState.value.copy(
            userScores = response.body() ?: emptyList(),
            lastErrorMessage = null
        )
    }
    [...]
}

```

/v

### Résultat JSON

```json
[
  {
    "score": 1000,
    "time": "2025-05-08 16:21:51"
  },
  {
    "score": 500,
    "time": "2025-05-08 16:21:58"
  },
  {
    "score": 300,
    "time": "2025-05-08 16:22:01"
  }
]
```

/s

# Gameplay

## Gameplay Slide 1

/v

## Gameplay Slide 2

/v

## Gameplay Slide 3