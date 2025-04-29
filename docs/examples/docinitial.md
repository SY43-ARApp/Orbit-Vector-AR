
---

## Technologies et outils utilisés

Le développement de **Gravity Arrow** s'appuiera sur un ensemble de technologies modernes orientées Android :

- **Android Studio** avec **Kotlin** pour le développement natif Android.
- **Google ARCore** pour la gestion de la réalité augmentée.
- **Jetpack Compose** pour la construction d’interfaces utilisateur fluides, réactives et modulaires.
- Base de données locale et distante (**API PHP + MySQL**) pour :
  - la gestion des comptes utilisateurs (connexion, score, progression),
  - le support multijoueur asynchrone (classements en ligne, enregistrement des données).
- Outils graphiques (**Photoshop/Figma**) pour la conception des écrans et assets visuels.

Cette stack technique permet de viser une bonne expérience utilisateur tout en exploitant les capacités immersives des appareils modernes.
<div style="page-break-before: always;"></div>

# Contraintes de Conception et d'Implémentation du Jeu

## 1. Interface Jetpack Compose Kotlin pour les Menus

### 1. Écran Principal (Main Screen)
- **Titre** : Le titre du jeu est affiché de manière proéminente en haut de l'écran.
- **Bouton Jouer** : Un grand bouton au centre de l'écran pour démarrer le jeu.
- **Arrière-plan** : L'arrière-plan peut être :
  - Une image statique représentant le thème du jeu.
  - Un flux vidéo en direct utilisant ARCore pour afficher les environs.

### 2. Écran de Connexion (Login Screen)
- **Connexion via Google Play Services** : Un bouton permettant de s'authentifier avec Google Play Services.
- **Connexion avec un Compte Personnel** :
  - Champs de saisie pour le nom d'utilisateur et le mot de passe.
  - Un bouton "Se Connecter".
- **Option d'Inscription** : Un bouton pour naviguer vers un écran d'inscription afin de créer un nouveau compte.

### 3. Écran de Menu (Menu Screen)
- **Bouton Jouer** : Permet de naviguer vers l'écran de jeu.
- **Bouton Classement** : Permet de naviguer vers l'écran des classements pour consulter les scores.
- **Bouton Paramètres** : Permet de naviguer vers un écran de paramètres pour ajuster les préférences du jeu (son, langue, etc.).
- **Bouton Profil** : Permet de consulter et modifier les informations du profil utilisateur (nom, avatar, etc.).

### 4. Écran de Boutique (Shop Screen)
- **Affichage de la Monnaie** : Affiche le solde de pièces du joueur en haut de l'écran.
- **Liste des Articles** : Une liste ou une grille défilante affichant les articles avec leur nom, leur prix et un bouton "Acheter".
- **Bouton Retour** : Un bouton de retour en haut à gauche pour naviguer.

### 5. Classement et Historique des Parties (Leaderboard and Game History)
- **Page de Classement** :
  - Affiche les meilleurs scores récupérés via l'API `get_global_scores.php`.
  - Inclut des options pour filtrer les scores par période ou d'autres critères.
- **Page d'Historique des Parties** :
  - Affiche l'historique des scores du joueur récupéré via l'API `get_user_scores.php`.
  - Inclut des options de tri par score ou par date.

### 6. Écran de Jeu (Game Screen)
- **Commandes** :
  - **Joystick** : Positionné en bas à gauche pour contrôler la direction des flèches.
  - **Curseur** : Positionné en bas à droite pour ajuster la masse des flèches.
- **Position Fixe du Joueur** : Le joueur reste immobile et interagit avec l'environnement via les commandes et la caméra du téléphone.
- **Éléments de l'Interface Utilisateur (UI)** :
  - **Affichage du Score** : Positionné en haut au centre pour montrer le score actuel.
  - **Nombre de Flèches Restantes** : Affiché en haut à droite pour indiquer le nombre de flèches restantes.
  - **Bouton Pause** : Un petit bouton en haut à gauche pour mettre le jeu en pause.
  - **Interaction avec la Caméra** : Le jeu utilise la caméra du téléphone pour la réalité augmentée.

## 2. Interaction avec la Base de Données Serveur

### Aspects Techniques et Outils Utilisés

#### 1. **Base de Données MySQL**
- **Pourquoi MySQL ?**
  - MySQL est un système de gestion de base de données relationnelle robuste, largement utilisé et bien documenté.
  - Il offre des performances élevées pour les opérations de lecture/écriture, ce qui est essentiel pour gérer les scores et les utilisateurs pour l'aspect compétitif du jeu.
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

- **Fonctionnalités à **Implémenter** :**
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
