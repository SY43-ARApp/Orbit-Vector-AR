# Cahier des Charges – Projet IHM 2025

## Titre du sujet

**Gravity Arrow – Puzzle AR**

## Membres du groupe

- BEAUJARD Traïan
- RAMALLO Alex

---

## Présentation du produit

**Gravity Arrow** est un jeu mobile Android en réalité augmentée (AR) où le joueur, en position fixe, utilise son téléphone pour tirer des flèches sur des pommes flottantes. Chaque niveau se déroule dans un environnement généré de manière procédurale, où des planètes en pseudo 3D appliquent une force gravitationnelle déviant la trajectoire des flèches. 

Le jeu allie physique, stratégie et précision dans un format puzzle AR accessible et rejouable à volonté. Le joueur dispose d’un nombre limité de flèches, gagne des points en réussissant les niveaux, et ces points sont convertis en pièces pour acheter des éléments cosmétiques (flèches, pommes, planètes). Un système de score et de classement en ligne renforce l’aspect compétitif et la rejouabilité.

---

## Technologies et outils utilisés

Le développement de **Gravity Arrow** s'appuiera sur un ensemble de technologies modernes orientées Android :

- **Android Studio** avec **Kotlin** pour le développement natif Android.
- **Google ARCore** pour la gestion de la réalité augmentée.
- **Jetpack Compose** pour la construction d’interfaces utilisateur fluides, réactives et modulaires.
- Base de données locale et distante (**SQLite / API PHP + SQL**) pour :
  - la gestion des comptes utilisateurs (connexion, score, progression),
  - le support multijoueur asynchrone (classements en ligne, enregistrement des données).
- Outils graphiques (**Photoshop/Figma**) pour la conception des écrans et assets visuels.

Cette stack technique permet de viser une bonne expérience utilisateur tout en exploitant les capacités immersives des appareils modernes.

# Contraintes de Conception et d'Implémentation du Jeu

## 1. Interface Jetpack Compose Kotlin pour les Menus

### 1. Main Screen
- **Title**: The game title is displayed prominently at the top of the screen.
- **Play Button**: A large button in the center of the screen to start the game.
- **Background**: The background can either be:
  - A static image representing the game theme.
  - A live camera feed using ARCore to show the surroundings.

### 2. Login Screen
- **Google Play Services Login**: A button to authenticate using Google Play Services.
- **Custom Account Login**:
  - Input fields for username and password.
  - A "Login" button.
- **Register Option**: A button to navigate to a registration screen for creating a new account.

### 3. Menu Screen
- **Play Button**: Navigates to the game screen.
- **Leaderboard Button**: Navigates to the leaderboard screen to view scores.
- **Other Options**: Placeholder for additional features like settings or profile.

### 4. Shop Screen
- **Currency Display**: Shows the player's coin balance at the top of the screen.
- **Item List**: A scrollable list or grid displaying items with their name, price, and a "Buy" button.
- **Back Button**: A back button at the top-left corner for navigation.

### 5. Leaderboard and Game History
- **Leaderboard Page**:
  - Displays top scores fetched from the `get_global_scores.php` API.
  - Includes options to filter scores by time or other criteria.
- **Game History Page**:
  - Displays the user's score history fetched from the `get_user_scores.php` API.
  - Includes sorting options by score or time.

### 6. Game Screen
- **Controls**:
  - **Joystick**: Positioned at the bottom-left to control arrow direction.
  - **Slider**: Positioned at the bottom-right to adjust arrow mass.
- **Fixed Player Position**: The player remains stationary, interacting with the environment through controls and the phones camera.
- **UI Elements**:
  - **Score Display**: Positioned at the top-center to show the current score.
  - **Arrow Count**: Displayed at the top-right to indicate remaining arrows.
  - **Pause Button**: A small button at the top-left to pause the game.
  - **Camera Interaction**: The game utilizes the phone's camera for augmented reality.

## 2. Interaction avec la Base de Données Serveur

### Aspects Techniques et Outils Utilisés

#### 1. **Base de Données MySQL**
- **Pourquoi MySQL ?**
  - MySQL est un système de gestion de base de données relationnelle robuste, largement utilisé et bien documenté.
  - Il offre des performances élevées pour les opérations de lecture/écriture, ce qui est essentiel pour gérer les scores et les utilisateurs dans un jeu compétitif.
  - La compatibilité avec PHP permet une intégration fluide avec les APIs.

- **Structure de la Base de Données :**
  - **Table `user` :**
    - Stocke les informations des utilisateurs, y compris leur `uuid`, leur nom, et leur meilleur score (`bestscore`).
    - Utilisation d'un `uuid` comme clé primaire pour garantir l'unicité et faciliter l'identification des utilisateurs.
  - **Table `scores` :**
    - Enregistre les scores des utilisateurs avec un horodatage.
    - Relation avec la table `user` via une clé étrangère (`uuid`).

#### 2. **PHP pour les APIs**
- **Pourquoi PHP ?**
  - PHP est un langage côté serveur simple et efficace pour créer des APIs RESTful.
  - Il est compatible avec MySQL et permet une gestion facile des requêtes HTTP.
  - Sa simplicité permet un développement rapide et une maintenance aisée.

- **Fonctionnalités Implémentées :**
  - **`login.php` :** Authentifie les utilisateurs en récupérant leur `uuid` à partir de leur nom.
  - **`register.php` :** Permet l'enregistrement de nouveaux utilisateurs avec un `uuid` unique.
  - **`send_score.php` :** Enregistre les scores des utilisateurs et met à jour leur meilleur score si nécessaire.
  - **`get_user_scores.php` :** Récupère les scores d'un utilisateur spécifique, avec des options de tri et de limite.
  - **`get_global_scores.php` :** Récupère les meilleurs scores globaux, triés par ordre décroissant.

#### 3. **Connexion à la Base de Données avec `DB.php`**
- **Pourquoi une Connexion Centralisée ?**
  - Centraliser la connexion à la base de données dans un fichier unique (`DB.php`) permet de réduire la duplication de code et de simplifier la gestion des erreurs.
  - L'utilisation d'un fichier `.env` pour stocker les informations sensibles (hôte, utilisateur, mot de passe) améliore la sécurité et facilite la configuration.

- **Gestion des Erreurs :**
  - Si la connexion échoue, un code de réponse HTTP 500 est renvoyé pour indiquer une erreur côté serveur.

#### 4. **SQL et Optimisation**
- **Pourquoi SQL ?**
  - SQL est le langage standard pour interagir avec les bases de données relationnelles comme MySQL.
  - Il permet de structurer et de manipuler efficacement les données.

- **Optimisations :**
  - Utilisation de `LIMIT` pour réduire la charge sur le serveur lors de la récupération des scores.
  - Indexation des colonnes clés (`uuid`, `score`) pour accélérer les requêtes.
  - Vérifications des paramètres (`order`, `param`) pour éviter les injections SQL.

#### 5. **JSON pour les Réponses**
- **Pourquoi JSON ?**
  - JSON est un format léger et lisible par les humains, idéal pour échanger des données entre le serveur et le client.
  - Il est facilement manipulable dans les applications Android, ce qui simplifie l'intégration avec le jeu.

- **Exemple de Réponse :**
  - Les APIs renvoient des données structurées en JSON, comme les scores globaux ou les scores d'un utilisateur spécifique.

### Contraintes et Solutions

1. **Sécurité :**
   - Les mots de passe doivent être hachés avant d'être stockés (non encore implémenté dans les fichiers actuels).
   - Les UUID garantissent l'unicité et réduisent les risques d'exposition des identifiants sensibles.

2. **Performance :**
   - Les requêtes SQL sont optimisées pour gérer un grand nombre d'utilisateurs et de scores.
   - Les limites (`LIMIT`) et tris (`ORDER BY`) réduisent la charge sur le serveur.

3. **Scalabilité :**
   - La structure de la base de données est extensible, permettant d'ajouter de nouvelles fonctionnalités (ex. historique des parties, classements par région).

4. **Gestion des Erreurs :**
   - Les scripts PHP incluent des vérifications pour valider les entrées utilisateur et éviter les erreurs inattendues.

## 3. Logique du Jeu et Réalité Augmentée

*(Cette section couvrira les contraintes liées à la logique du jeu, y compris l'intégration d'ARCore, la gestion des forces gravitationnelles, et la génération procédurale des niveaux.)*


---

## Analyse centrée utilisateur

*(Analyse à intégrer ici)*

---

## Plan de travail

### Semaine 1-2 :
- Constitution de l’équipe, rédaction du CCH.
- Réflexion sur le gameplay, maquettes papier/Figma des premiers écrans.

### Semaine 3-4 :
- Mise en place du projet Android Studio avec ARCore.
- Développement du prototype de tir avec contrôle joystick et slider.

### Semaine 5-6 :
- Ajout des effets gravitationnels (planètes).
- Implémentation de la génération procédurale.
- Début de l’intégration des menus via Jetpack Compose.

### Semaine 7-8 :
- Intégration de la base de données : création de comptes, enregistrement des scores.
- Connexion au leaderboard en ligne.
- Tests utilisateurs et ajustements ergonomiques.

### Semaine 9 :
- Finalisation des éléments graphiques et cosmétiques.
- Préparation de la soutenance (slides, démo).
- Livraison du livrable final (code + rapport).


## Notes

Le projet suivra les recommandations ergonomiques du cours, incluant :
- Organisation logique des écrans (layout Z, zones prioritaires, etc.).
- Hiérarchisation des informations par contraste, taille, proximité (lois de Gestalt).
- Choix typographiques et structurels favorisant lisibilité et accessibilité.