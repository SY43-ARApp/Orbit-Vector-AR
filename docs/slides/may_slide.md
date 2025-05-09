---
title: "Orbit Vector AR"
separator: /s
verticalSeparator: /v
theme: solarized
revealOptions:
  transition: 'fade'
---

<!-- TODO : REPLACE picsum -->

<!-- .slide: data-background="./img/background.png" style="color:white-->

<img src="img/gametitle.png" style="width: 30%; height: auto;" />

---

<h3 style="color:white">
Perrin Antoine, Beaujard Traïan, Martin, Alex  
</h3>

<h4 style="color:white">
SY43 - P25
</h4>

/s

## Contextualisation du projet

### Jeu casual Puzzle en AR : Orbit Vector AR

<div style="display: flex; justify-content: center; align-items: flex-start; gap: 2vw;">
  <div style=" line-height: 1.2;">
    <ul>
      <li>Atteindre la cible (pomme) avec une flèche</li>
      <li>Utiliser la gravité des planètes pour dévier la flèche</li>
      <li>Partie finie quand plus de flèches</li>
      <li>Score sauvegardé en ligne sur BDD</li>
      <li>Classement en ligne</li>
    </ul>
  </div>

  <img src="https://picsum.photos/800/1200" alt="demo jeu gif" style="width: 30%; height: auto;" />
</div>

/v

### Avancée globale

<img src="https://picsum.photos/1400/600" alt="screen github project" style="width: 90%; height: auto;" />

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

# Gameplay AR
<br>
<img src="https://picsum.photos/800/1400" alt="previewGameplay" style="width: 20%; height: auto; display: block; margin: 0 auto;" />

/v

## AR Core

- Quick présentation de AR Core

<br>

### Fonctionnalités utiles d'AR Core
<div style="font-size: 2vw; line-height: 1.2;">
<ul>
  <li>Caméra</li>
  <li>Suivi de mouvement (position et orientation)</li>
  <li>Cartographie de l'environnement</li>
  <li>Estimation de la lumière ambiante</li>
  <li>Détection de surfaces planes</li>
  <li>Depth Map</li>
  <li>Ancrage d'objets virtuels dans le monde réel</li>
</ul>
</div>

/v

<div style="display: flex; justify-content: center; align-items: flex-start; gap: 2vw;">
  <img src="https://picsum.photos/800/1200" alt="screen repo template" style="width: 30%; height: auto;" />
  <img src="https://picsum.photos/800/1200" alt="screen repo template" style="width: 30%; height: auto;" />
  <img src="https://picsum.photos/800/1200" alt="screen repo template" style="width: 30%; height: auto;" />
</div>


/v
---
J'ai donc dans un premier mis en place l'exemple d'AR Core, testé les apis. Puis crée un repo template github pour notre projet;

<img src="https://picsum.photos/1000/800" alt="screen repo template" style="width: 50%; height: auto; display: block; margin: 0 auto;" />


/v
## Ce que j'ai fait

### Sommaire

<div style="font-size: 2vw; line-height: 1.2;">
<ul>
  <li>Placement de la Pomme (cible)</li>
  <li>Lancement de la flèche (physiques)</li>
  <li>Création et placement des planètes (+ physiques)</li>
  <li>Gestion des colisions et intéractions entre les objets</li>
  <li>Gestion des flèches, scores, boucle de jeu</li>
  <li>Générateur de niveau procédural</li>
  <li>Prévisualisation 3d de la trajectoire</li>
  <li>Implémenté les modèles 3D et textures</li>
</ul>
</div>

### Placement des objets

/v

#### Système d'ancrage

/v

#### Prototypage

/v

#### Occlusion

/v

#### Ajout des modèles et textures

/v

### Physiques - Gravité et Attraction

/v

#### Physique des flèches

/v

#### Physique des planètes

/v

#### Prévisualisation trajectoire de la flèche

/v

### Générateur de niveaux procéduraux

/v

## Ce que je vais faire 

<div style="font-size: 2vw; line-height: 1.2;">
<ul>
  <li>UI dans le menu AR</li>
  <li>Envoyer les scores à la BDD</li>
  <li>Intégrer changement de menu JetpackCompose -> Scene AR</li>
  <li>Améliorations et équilibrages</li>
  <li>Publication sur Playstore</li>
</ul>
</div>