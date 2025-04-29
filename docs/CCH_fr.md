# Cahier des Charges – Projet IHM 2025

## Titre du sujet

**Gravity Arrow – Puzzle AR**

## Membres du groupe

- BEAUJARD Traïan
- RAMALLO Alex

---

## Présentation du produit

**Gravity Arrow** est un jeu mobile Android en réalité augmentée (AR) où le joueur, en position fixe, utilise son téléphone pour tirer des flèches sur des pommes flottantes. Chaque niveau se déroule dans un environnement généré de manière procédurale, où des planètes en 3D appliquent une force gravitationnelle déviant la trajectoire des flèches. 

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