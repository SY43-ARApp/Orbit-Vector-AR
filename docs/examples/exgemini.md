<style>
p {
  text-align: justify;
}
img {
  display: block;
  margin-left: auto;
  margin-right: auto;
  max-width: 100%;
}
h1, h2, h3, h4, h5, h6 {
  /* text-align: center; */ /* Commenté car peut surcharger visuellement */
  margin-top: 2em;
  margin-bottom: 1em;
  text-align: initial; /* Gardé car demandé, même si centré est courant pour H1 */
  color: #333; /* Ajout pour un look plus pro */
  border-bottom: 1px solid #eee; /* Soulignement léger pour séparer */
  padding-bottom: 0.3em;
}
h1 {
  text-align: center; /* H1 centré est acceptable */
  border-bottom: 2px solid #ccc;
}
ul, ol {
  margin-left: 2em;
  line-height: 1.6; /* Améliore la lisibilité */
}
table {
  margin-left: auto;
  margin-right: auto;
  border-collapse: collapse; /* Style de tableau plus propre */
  width: 80%; /* Largeur indicative */
  margin-top: 1em;
  margin-bottom: 1em;
}
th, td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}
th {
  background-color: #f2f2f2; /* En-tête de tableau */
}
/* Style pour la table des matières */
.toc {
  border: 1px solid #eee;
  background-color: #f9f9f9;
  padding: 1em;
  margin-bottom: 2em;
}
.toc ul {
  list-style-type: none;
  margin-left: 0;
  padding-left: 0;
}
.toc ul ul {
  margin-left: 1.5em;
}
.toc a {
  text-decoration: none;
  color: #0056b3;
}
.toc a:hover {
  text-decoration: underline;
}

/* Utile pour forcer les sauts de page à l'impression ou PDF */
.page-break { 
  page-break-before: always; 
}

</style>

# Cahier des Charges – Orbit Vector AR
**Auteurs:** BEAUJARD Traïan, RAMALLO Alex
**Version:** 1.0
**Date:** 2024-04-29 

<img src="img/app_title.png" alt="Titre de l'application Orbit Vector AR" style="max-width: 350px; width: 100%; height: auto;">

<div class="toc">

## Table des Matières
- [Cahier des Charges – Orbit Vector AR](#cahier-des-charges--orbit-vector-ar)
  - [Table des Matières](#table-des-matières)
  - [1. Introduction](#1-introduction)
    - [1.1 Contexte du Projet](#11-contexte-du-projet)
    - [1.2 Objectifs du Projet](#12-objectifs-du-projet)
      - [1.2.1 Objectifs Principaux](#121-objectifs-principaux)
      - [1.2.2 Objectifs Secondaires](#122-objectifs-secondaires)
    - [1.3 Périmètre du Projet](#13-périmètre-du-projet)
    - [1.4 Public Cible](#14-public-cible)
    - [1.5 Définitions et Glossaire](#15-définitions-et-glossaire)
  - [2. Analyse Centrée Utilisateur](#2-analyse-centrée-utilisateur)
    - [2.1 Démarche Adoptée](#21-démarche-adoptée)
    - [2.2 Personas](#22-personas)
    - [2.3 Scénarios d'Usage (Use Cases)](#23-scénarios-dusage-use-cases)
    - [2.4 Besoins et Attentes des Utilisateurs](#24-besoins-et-attentes-des-utilisateurs)
  - [3. Exigences Fonctionnelles](#3-exigences-fonctionnelles)
    - [3.1 Cœur du Gameplay](#31-cœur-du-gameplay)
    - [3.2 Fonctionnalités de Réalité Augmentée](#32-fonctionnalités-de-réalité-augmentée)
    - [3.3 Génération Procédurale des Niveaux](#33-génération-procédurale-des-niveaux)
    - [3.4 Système de Contrôle](#34-système-de-contrôle)
    - [3.5 Système de Score et Progression](#35-système-de-score-et-progression)
    - [3.6 Gestion des Ressources (Flèches)](#36-gestion-des-ressources-flèches)
    - [3.7 Conditions de Victoire et de Défaite](#37-conditions-de-victoire-et-de-défaite)
    - [3.8 Économie Interne (Pièces)](#38-économie-interne-pièces)
    - [3.9 Boutique et Personnalisation](#39-boutique-et-personnalisation)
    - [3.10 Classement en Ligne (Leaderboard)](#310-classement-en-ligne-leaderboard)
    - [3.11 Authentification Utilisateur](#311-authentification-utilisateur)
    - [3.12 Monétisation (Achats In-App)](#312-monétisation-achats-in-app)
  - [4. Exigences Non Fonctionnelles](#4-exigences-non-fonctionnelles)
    - [4.1 Performance](#41-performance)
    - [4.2 Utilisabilité et Ergonomie](#42-utilisabilité-et-ergonomie)
    - [4.3 Fiabilité et Stabilité](#43-fiabilité-et-stabilité)
    - [4.4 Compatibilité](#44-compatibilité)
    - [4.5 Maintenabilité](#45-maintenabilité)
    - [4.6 Sécurité](#46-sécurité)
  - [5. Interface Utilisateur et Expérience Utilisateur (UI/UX)](#5-interface-utilisateur-et-expérience-utilisateur-uiux)
    - [5.1 Principes de Conception Généraux](#51-principes-de-conception-généraux)
    - [5.2 Navigation dans l'Application](#52-navigation-dans-lapplication)
    - [5.3 Maquettes des Écrans Principaux](#53-maquettes-des-écrans-principaux)
      - [5.3.1 Écran Titre](#531-écran-titre)
      - [5.3.2 Écran de Connexion](#532-écran-de-connexion)
      - [5.3.3 Menu Principal](#533-menu-principal)
      - [5.3.4 Écran de Jeu](#534-écran-de-jeu)
      - [5.3.5 Écran de Boutique](#535-écran-de-boutique)
      - [5.3.6 Écrans de Fin de Partie (Victoire / Game Over)](#536-écrans-de-fin-de-partie-victoire--game-over)
    - [5.4 Feedback Utilisateur](#54-feedback-utilisateur)
  - [6. Spécifications Techniques](#6-spécifications-techniques)
    - [6.1 Plateforme Cible](#61-plateforme-cible)
    - [6.2 Technologie de Réalité Augmentée](#62-technologie-de-réalité-augmentée)
    - [6.3 Outils de Développement](#63-outils-de-développement)
    - [6.4 Langages de Programmation](#64-langages-de-programmation)
    - [6.5 Services Externes et API](#65-services-externes-et-api)
    - [6.6 Gestion des Données](#66-gestion-des-données)
  - [7. Contraintes du Projet](#7-contraintes-du-projet)
    - [7.1 Contraintes Techniques](#71-contraintes-techniques)
    - [7.2 Contraintes Temporelles](#72-contraintes-temporelles)
    - [7.3 Contraintes de Ressources](#73-contraintes-de-ressources)
  - [8. Plan de Travail (Planning Prévisionnel)](#8-plan-de-travail-planning-prévisionnel)
    - [8.1 Phases du Projet](#81-phases-du-projet)
    - [8.2 Jalons Clés et Livrables Associés](#82-jalons-clés-et-livrables-associés)
    - [8.3 Répartition Prévisionnelle des Tâches](#83-répartition-prévisionnelle-des-tâches)
  - [9. Livrables du Projet](#9-livrables-du-projet)
  - [10. Critères d'Acceptation](#10-critères-dacceptation)
  - [11. Annexes](#11-annexes)

</div>

<div class="page-break"></div>

## 1. Introduction

### 1.1 Contexte du Projet
Ce document constitue le Cahier des Charges (CdC) pour le développement du jeu mobile "Orbit Vector AR". Le projet s'inscrit dans le cadre du mini-projet d'Interface Homme-Machine [Préciser le cadre exact si nécessaire : cours, module, année...]. Il vise à concevoir et développer une application Android exploitant la technologie de la réalité augmentée pour offrir une expérience de jeu de puzzle innovante. Le joueur interagit avec des éléments virtuels (planètes, flèches, cibles) superposés à son environnement réel via la caméra de son smartphone.

### 1.2 Objectifs du Projet
(Reprend ce que tu as écrit, éventuellement en le détaillant un peu plus)

Le but de ce projet est de créer un jeu mobile offrant une expérience unique et originale en combinant puzzle, physique et réalité augmentée.

#### 1.2.1 Objectifs Principaux
- **Développer un jeu mobile fonctionnel sur Android utilisant la Réalité Augmentée (ARCore).** L'application doit détecter les surfaces de l'environnement réel et y ancrer les éléments du jeu.
- **Implémenter un gameplay de puzzle basé sur la physique.** Le joueur doit lancer des flèches dont la trajectoire est influencée par l'attraction gravitationnelle de planètes virtuelles pour atteindre une cible (pomme).
- **Intégrer une génération procédurale des niveaux.** Les niveaux (position des planètes, de la cible) doivent être générés dynamiquement en fonction de l'environnement détecté pour assurer la rejouabilité.
- **Réaliser une interface utilisateur (UI) intuitive et adaptée au contexte mobile et AR.**

#### 1.2.2 Objectifs Secondaires
- **Mettre en place un système de score persistant.** Le score du joueur doit être sauvegardé localement ou en ligne.
- **Intégrer un classement en ligne (Leaderboard).** Permettre aux joueurs de comparer leurs scores (via Google Play Services ou un serveur dédié).
- **Développer un système de personnalisation cosmétique.** Offrir aux joueurs la possibilité d'acheter des skins (flèches, planètes, cibles) avec une monnaie virtuelle gagnée en jeu.
- **Explorer la monétisation via des achats In-App** (optionnel, selon le temps disponible).
- **Assurer une expérience utilisateur fluide et immersive.**

### 1.3 Périmètre du Projet
**Inclus dans le projet :**
- Développement de l'application Android.
- Intégration ARCore pour la détection de l'environnement et le placement des objets.
- Moteur physique pour la gravité et la trajectoire des flèches.
- Génération procédurale simple des niveaux.
- Interface utilisateur pour le menu principal, le jeu, la boutique, le score.
- Système de contrôle (visée par téléphone, joystick/slider virtuel).
- Système de score et de monnaie virtuelle basique.
- Boutique avec quelques items cosmétiques.
- Authentification via Google Play Services (privilégié).
- Leaderboard via Google Play Services.

**Exclus du projet (sauf si le temps le permet) :**
- Version iOS.
- Fonctionnalités multijoueurs.
- Scénario ou narration complexe.
- Système de monétisation par publicité ou achats In-App complexes (paiements réels).
- Localisation dans plusieurs langues (Français et/ou Anglais par défaut).
- Support hors-ligne complet (si leaderboard et login serveur sont implémentés).

### 1.4 Public Cible
Le jeu s'adresse principalement aux :
- Joueurs occasionnels sur mobile ("casual gamers").
- Utilisateurs de smartphones Android compatibles ARCore.
- Personnes intéressées par les nouvelles technologies (AR) et les jeux de puzzle/physique.
- Tranche d'âge : Adolescents et jeunes adultes (12-35 ans), mais potentiellement plus large.

### 1.5 Définitions et Glossaire
- **AR (Augmented Reality / Réalité Augmentée) :** Technologie superposant des éléments virtuels (images 2D/3D, informations) au monde réel perçu par l'utilisateur, généralement via la caméra d'un appareil mobile ou des lunettes spécifiques.
- **ARCore :** Plateforme de développement Google permettant de créer des expériences de réalité augmentée sur Android.
- **Génération Procédurale :** Création de contenu (ici, les niveaux de jeu) de manière algorithmique plutôt que manuelle, permettant une grande variété et rejouabilité.
- **Gameplay :** Ensemble des règles et interactions qui définissent comment un joueur joue à un jeu.
- **UI (User Interface / Interface Utilisateur) :** Ensemble des éléments graphiques et textuels permettant à l'utilisateur d'interagir avec l'application.
- **UX (User Experience / Expérience Utilisateur) :** Ressenti global de l'utilisateur lors de l'interaction avec l'application.
- **SDK (Software Development Kit) :** Ensemble d'outils d'aide à la programmation pour une plateforme ou technologie spécifique.
- **API (Application Programming Interface) :** Interface logicielle permettant à des programmes de communiquer entre eux.
- **GDD (Game Design Document) :** Document décrivant la conception d'un jeu vidéo. (Référence : Voir Annexe X ou document séparé).
- **IAP (In-App Purchase / Achat Intégré) :** Transaction effectuée depuis une application mobile pour acquérir du contenu ou des fonctionnalités supplémentaires.

<div class="page-break"></div>

## 2. Analyse Centrée Utilisateur
*(Section requise par le professeur)*

### 2.1 Démarche Adoptée
L'analyse centrée utilisateur suivra une démarche itérative inspirée des méthodes agiles et du Design Thinking. Elle comprendra les étapes suivantes :
1.  **Identification du public cible** (voir section 1.4).
2.  **Création de personas** représentatifs des utilisateurs clés.
3.  **Définition de scénarios d'usage** décrivant comment les utilisateurs interagissent avec l'application pour atteindre leurs objectifs.
4.  **Recueil des besoins et attentes** via brainstorming interne, analyse de jeux similaires et potentiellement des questionnaires ou entretiens si le temps le permet.
5.  **Conception et prototypage** (maquettes UI/UX) basés sur ces analyses.
6.  **Évaluation (idéalement) :** Tests utilisateurs sur les maquettes ou prototypes pour recueillir du feedback et itérer sur la conception (cette étape pourrait être limitée dans le cadre du mini-projet).

### 2.2 Personas
*(Exemples à développer)*

*   **Persona 1 : Léo, l'Explorateur Technophile (19 ans)**
    *   **Profil :** Étudiant en informatique, possède un smartphone Android récent compatible AR. Aime tester les nouvelles technologies et les jeux innovants. Joue de manière occasionnelle mais apprécie les défis intellectuels (puzzles).
    *   **Objectifs :** Découvrir une expérience AR ludique, être challengé par les puzzles physiques, comparer ses scores avec ses amis.
    *   **Frustrations :** Jeux AR qui manquent de profondeur, interfaces peu intuitives, mauvaise détection de l'environnement.

*   **Persona 2 : Chloé, la Joueuse Casual (28 ans)**
    *   **Profil :** Graphiste, utilise son smartphone pour se détendre pendant ses pauses ou trajets. Aime les jeux simples à prendre en main mais avec une bonne rejouabilité. Apprécie la personnalisation visuelle.
    *   **Objectifs :** Passer un bon moment sans stress, avoir un jeu facile d'accès, pouvoir personnaliser son expérience (skins).
    *   **Frustrations :** Jeux trop compliqués dès le début, tutoriels absents ou trop longs, obligation de payer pour progresser.

### 2.3 Scénarios d'Usage (Use Cases)
*(Exemples à développer, basés sur le GDD et les personas)*

*   **Scénario 1 : Première partie de Léo**
    1. Léo télécharge et lance Orbit Vector AR.
    2. Il choisit de se connecter avec son compte Google Play Jeux.
    3. Il suit le court tutoriel interactif expliquant la visée AR et l'influence des planètes.
    4. L'application détecte une surface plane (sa table basse).
    5. Le premier niveau apparaît en AR sur la table.
    6. Léo vise avec son téléphone, ajuste la force/masse et tire sa première flèche.
    7. Il rate, ajuste sa visée en tenant compte de la gravité d'une planète proche.
    8. Il réussit à toucher la pomme au second essai.
    9. Il passe au niveau suivant, procéduralement généré et légèrement plus complexe.
    10. Il termine sa session et voit son score ajouté au classement.

*   **Scénario 2 : Session de personnalisation de Chloé**
    1. Chloé lance le jeu après avoir accumulé des pièces lors de sessions précédentes.
    2. Elle navigue vers le Menu Principal, puis vers la Boutique.
    3. Elle parcourt les skins disponibles pour les flèches.
    4. Elle prévisualise un skin "Flèche Arc-en-ciel".
    5. Elle utilise ses pièces virtuelles pour acheter le skin.
    6. Elle équipe le nouveau skin.
    7. Elle lance une nouvelle partie pour essayer son skin personnalisé.

### 2.4 Besoins et Attentes des Utilisateurs
*   **Facilité de prise en main :** Contrôles intuitifs, tutoriel clair.
*   **Expérience AR stable :** Bonne détection des surfaces, tracking fiable des objets virtuels.
*   **Gameplay engageant :** Challenge progressif, physique intéressante, satisfaction à réussir un tir complexe.
*   **Rejouabilité :** Niveaux variés grâce à la génération procédurale, objectif de battre son score ou le classement.
*   **Performance :** Fluidité de l'application, temps de chargement raisonnables.
*   **Esthétique agréable :** Graphismes clairs et attrayants, interface soignée.
*   **Personnalisation (pour certains) :** Possibilité de modifier l'apparence des éléments de jeu.
*   **Fair-play :** Si monétisation, qu'elle soit cosmétique et non "pay-to-win".

<div class="page-break"></div>

## 3. Exigences Fonctionnelles
*(Détaille CE QUE l'application doit FAIRE, basé sur le GDD)*

### 3.1 Cœur du Gameplay
- L'application doit permettre au joueur de lancer des projectiles (flèches) depuis une position fixe.
- Les flèches doivent suivre une trajectoire balistique influencée par :
    - La force/vitesse initiale définie par le joueur.
    - La masse de la flèche (modifiable par le joueur).
    - L'attraction gravitationnelle simulée des objets célestes (planètes) placés dans le niveau.
- Le but de chaque niveau est de toucher une cible spécifique (pomme) avec une flèche.
- La physique de l'attraction (type loi de Newton simplifiée) doit être implémentée et configurable.

### 3.2 Fonctionnalités de Réalité Augmentée
- L'application doit utiliser ARCore pour :
    - Détecter les surfaces planes horizontales (voire verticales si pertinent) dans l'environnement réel de l'utilisateur.
    - Permettre à l'utilisateur de choisir une surface pour ancrer le niveau de jeu.
    - Afficher les éléments virtuels du jeu (planètes, flèche de visée, cible, trajectoire prédictive ?) de manière stable et correctement positionnée par rapport à l'environnement réel.
- L'application doit gérer les conditions d'éclairage et de tracking AR (feedback si le tracking est perdu).

### 3.3 Génération Procédurale des Niveaux
- L'application doit générer dynamiquement la disposition des planètes (nombre, position, masse/taille) et de la cible pour chaque nouveau niveau.
- La difficulté des niveaux doit augmenter progressivement (par exemple, en augmentant le nombre de planètes, leur force gravitationnelle, ou la distance/complexité du tir).
- L'algorithme de génération doit s'assurer que chaque niveau généré est théoriquement solvable.

### 3.4 Système de Contrôle
- **Visée :** L'orientation du téléphone doit déterminer la direction initiale du tir de la flèche.
- **Force/Vitesse :** Un contrôle UI (Joystick Haut/Bas ou Slider) doit permettre au joueur d'ajuster la force/vitesse de lancement de la flèche.
- **Direction (Ajustement fin) :** Un contrôle UI (Joystick Gauche/Droite) doit permettre d'ajuster finement l'angle horizontal du tir.
- **Masse :** Un contrôle UI (Slider) doit permettre de modifier la masse de la flèche, influençant sa sensibilité à la gravité.
- Un bouton ou geste de "Tir" doit déclencher le lancement de la flèche.

### 3.5 Système de Score et Progression
- Le joueur gagne des points à chaque niveau réussi.
- Le calcul du score peut dépendre de facteurs comme :
    - Le nombre de flèches utilisées.
    - Le temps pris (moins probable pour un puzzle).
    - La difficulté du niveau.
- Le score total du joueur augmente au fil des niveaux réussis lors d'une même session de jeu.
- Le meilleur score du joueur doit être sauvegardé (localement ou via service externe).

### 3.6 Gestion des Ressources (Flèches)
- Le joueur commence une session avec un nombre défini de flèches (X).
- Chaque tir consomme une flèche.
- Le joueur peut gagner des flèches supplémentaires en réussissant des niveaux ou en atteignant certains paliers de score [À définir].

### 3.7 Conditions de Victoire et de Défaite
- **Victoire (Niveau) :** Le joueur touche la cible (pomme) avec une flèche. L'application passe au niveau suivant.
- **Défaite (Session) :** Le joueur n'a plus de flèches disponibles. La partie se termine, le score final est enregistré.

### 3.8 Économie Interne (Pièces)
- À la fin d'une session de jeu (défaite), le score final du joueur est converti en une monnaie virtuelle (Pièces). Le taux de conversion est à définir.
- Les pièces sont sauvegardées sur le compte du joueur.

### 3.9 Boutique et Personnalisation
- Une section "Boutique" doit être accessible depuis le menu principal.
- La boutique doit lister des items cosmétiques (skins) pour :
    - Les flèches.
    - Les cibles (pommes).
    - Les planètes.
- Chaque item doit avoir un coût en Pièces.
- Le joueur doit pouvoir acheter des items s'il possède assez de pièces.
- Le joueur doit pouvoir équiper/déséquiper les skins possédés.
- Les skins équipés doivent être visibles en jeu.

### 3.10 Classement en Ligne (Leaderboard)
- L'application doit intégrer un classement des meilleurs scores.
- Le classement doit être accessible depuis le menu principal.
- Il doit afficher le pseudo du joueur et son score.
- L'intégration se fera préférentiellement via Google Play Games Services.

### 3.11 Authentification Utilisateur
- L'application doit proposer une méthode de connexion pour sauvegarder la progression, les achats et le score en ligne.
- L'authentification via Google Play Games Services est la méthode privilégiée pour sa simplicité d'intégration et son adoption sur Android.
- Une option de jeu "Invité" (sans sauvegarde en ligne) peut être envisagée.

### 3.12 Monétisation (Achats In-App)
- *(Optionnel)* Si implémenté, l'application permettra l'achat de Pièces virtuelles ou de packs de skins spécifiques avec de l'argent réel via Google Play Billing Library.
- Les achats doivent être sécurisés et conformes aux politiques de Google Play.

<div class="page-break"></div>

## 4. Exigences Non Fonctionnelles
*(Détaille COMMENT l'application doit fonctionner, ses qualités)*

### 4.1 Performance
- **Fluidité :** L'application doit viser un taux de rafraîchissement stable (cible : 30 FPS minimum, idéalement 60 FPS) pendant le jeu, y compris avec l'AR active.
- **Réactivité :** Les contrôles et l'interface utilisateur doivent répondre rapidement aux actions du joueur.
- **Temps de chargement :** Le lancement de l'application et le chargement des niveaux doivent être rapides (quelques secondes maximum).
- **Consommation de ressources :** L'application doit être optimisée pour ne pas vider excessivement la batterie ou surchauffer l'appareil. L'utilisation CPU/GPU/RAM doit être monitorée.

### 4.2 Utilisabilité et Ergonomie
- **Intuitivité :** Les contrôles et la navigation doivent être faciles à comprendre et à utiliser, même pour un joueur novice. Un tutoriel initial est nécessaire.
- **Lisibilité :** Les textes et éléments d'interface doivent être clairs et lisibles sur différentes tailles d'écrans de smartphones.
- **Accessibilité :** Considérer des options de base (taille de police ajustable si possible, contrastes suffisants).
- **Confort d'utilisation AR :** Le jeu doit être jouable sans nécessiter de mouvements excessifs ou inconfortables de la part du joueur (jeu en position relativement fixe).

### 4.3 Fiabilité et Stabilité
- **Stabilité :** L'application ne doit pas planter ou freezer fréquemment. Les erreurs doivent être gérées proprement (ex: perte de tracking AR, problème de connexion réseau).
- **Robustesse AR :** Le tracking de l'environnement et l'ancrage des objets virtuels doivent être aussi stables que possible, même avec des mouvements modérés du téléphone ou des changements légers d'éclairage.
- **Sauvegarde des données :** La progression du joueur (score, pièces, items achetés) doit être sauvegardée de manière fiable (localement et/ou en ligne).

### 4.4 Compatibilité
- **Plateforme :** Android.
- **Version Android :** Définir la version minimale supportée (ex: Android 7.0 Nougat ou supérieur, en fonction des dépendances ARCore).
- **Compatibilité ARCore :** L'application ne fonctionnera que sur les appareils supportant officiellement ARCore (une vérification doit être faite au lancement).
- **Tailles d'écran :** L'interface doit s'adapter correctement à différentes résolutions et ratios d'écrans de smartphones Android.

### 4.5 Maintenabilité
- **Qualité du code :** Le code source doit être clair, commenté, organisé (architecture logicielle type MVVM, MVC ou autre) et suivre les bonnes pratiques de développement Android/Unity (si utilisé).
- **Modularité :** Les différents composants (AR, physique, UI, jeu) doivent être conçus de manière modulaire pour faciliter les mises à jour et les corrections de bugs.
- **Gestion des dépendances :** Utilisation d'un gestionnaire de dépendances (Gradle) pour les bibliothèques externes.

### 4.6 Sécurité
- **Données utilisateur :** Si des données personnelles sont collectées (email via Google Login), elles doivent être gérées conformément au RGPD et aux politiques de confidentialité.
- **Achats In-App :** Si implémentés, les transactions doivent être sécurisées via les mécanismes fournis par Google Play Billing.
- **Anti-triche :** Des mesures basiques pour éviter la triche sur les scores ou la monnaie virtuelle peuvent être envisagées (validation côté serveur si un backend est utilisé, obfuscation simple sinon).

<div class="page-break"></div>

## 5. Interface Utilisateur et Expérience Utilisateur (UI/UX)
*(Détaille l'apparence et l'interaction, en lien avec le GDD)*

### 5.1 Principes de Conception Généraux
- **Cohérence :** Maintenir un style visuel et une logique d'interaction cohérents à travers tous les écrans de l'application.
- **Simplicité :** Éviter la surcharge d'informations, privilégier des interfaces épurées, surtout en mode jeu AR où l'environnement réel est déjà présent.
- **Feedback :** Fournir des retours visuels et/ou sonores clairs pour les actions importantes (tir, succès, échec, achat, perte de tracking AR).
- **Adaptation Mobile/AR :** Concevoir pour une utilisation tactile sur smartphone, en tenant compte des contraintes de la visualisation AR (ne pas cacher la vue, placer les UI de manière non intrusive).

### 5.2 Navigation dans l'Application
La navigation suivra le flux décrit dans le GDD :
1.  **Écran Titre** -> (Login si nécessaire) -> **Menu Principal**
2.  **Menu Principal** -> **Jeu**
3.  **Menu Principal** -> **Boutique**
4.  **Menu Principal** -> **Classement**
5.  **Menu Principal** -> **Options/Paramètres** (si pertinent)
6.  **Jeu** -> **Écran Victoire (Niveau)** -> **Jeu (Niveau Suivant)**
7.  **Jeu** -> **Écran Game Over** -> **Menu Principal** (ou retour Écran Titre)
8.  **Boutique** -> **Menu Principal**
9.  **Classement** -> **Menu Principal**

*(Référence au diagramme UML de navigation du GDD : `img/ui_navigation_uml.png`)*

### 5.3 Maquettes des Écrans Principaux
*(Décrire ou insérer/référencer les maquettes fournies dans le GDD)*

#### 5.3.1 Écran Titre
- **Contenu :** Logo/Titre du jeu, bouton "Jouer" ou "Se Connecter", potentiellement un bouton "Options" ou "Quitter".
- **Arrière-plan :** Option 1: Vue caméra AR (nécessite permissions tôt). Option 2: Arrière-plan personnalisé thématique (plus simple au démarrage). [Décision à prendre basée sur les images GDD].
- *(Référence GDD : `img/title_screen_ar.jpg`, `img/title_screen_customBg.jpg`)*

#### 5.3.2 Écran de Connexion
- **Contenu :** Bouton "Se connecter avec Google Play Jeux". Option "Jouer en tant qu'invité". Affichage de l'état de la connexion.
- *(Référence GDD : maquette à créer ou décrire, inspirée des standards Google Login)*

#### 5.3.3 Menu Principal
- **Contenu :** Boutons principaux de navigation ("Jouer", "Boutique", "Classement"), affichage du pseudo du joueur et du solde de Pièces, potentiellement bouton "Options".
- *(Référence GDD : maquette à créer ou décrire)*

#### 5.3.4 Écran de Jeu
- **Vue principale :** Affichage de la caméra AR avec les éléments du jeu (planètes, cible) ancrés dans l'environnement.
- **Interface Superposée (HUD) :**
    - Indicateur de visée (réticule ou représentation de la flèche).
    - Contrôles (Joystick/Sliders pour direction/force/masse).
    - Bouton "Tirer".
    - Affichage du nombre de flèches restantes.
    - Affichage du score actuel.
    - Indicateur de statut AR (tracking ok/perdu).
- *(Référence GDD : `img/game_screen.jpg`)*

#### 5.3.5 Écran de Boutique
- **Contenu :** Liste des items cosmétiques (avec icône/preview, nom, prix), organisée par catégories (Flèches, Cibles, Planètes). Affichage du solde de Pièces du joueur. Boutons "Acheter" et "Équiper".
- *(Référence GDD : maquette à créer ou décrire)*

#### 5.3.6 Écrans de Fin de Partie (Victoire / Game Over)
- **Écran Victoire (Niveau) :** Message de succès, points gagnés pour ce niveau, bouton "Niveau Suivant". (Apparaît brièvement).
- **Écran Game Over :** Message de fin de partie, score final atteint, conversion du score en Pièces, bouton "Classement", bouton "Rejouer" ou "Menu Principal".
- *(Référence GDD : maquettes à créer ou décrire)*

### 5.4 Feedback Utilisateur
- **Visuel :**
    - Changement d'état des boutons (pressé, désactivé).
    - Prévisualisation de la trajectoire (si implémenté).
    - Animations lors du tir, de l'impact, de la réussite du niveau.
    - Indicateur clair lorsque le tracking AR est perdu ou en cours d'initialisation.
    - Effets visuels lors de l'achat ou de l'équipement d'un skin.
- **Sonore :**
    - Effets sonores pour le tir, l'impact, la collision, la navigation UI.
    - Musique d'ambiance discrète (menu, jeu).
    - Sons de notification pour succès/échec.

<div class="page-break"></div>

## 6. Spécifications Techniques
*(Détaille les outils et technologies utilisés)*

### 6.1 Plateforme Cible
- **Système d'exploitation :** Android
- **Version minimale :** Android [Numéro de version à définir, ex: 7.0 (Nougat) / API Level 24 ou supérieur, vérifier prérequis ARCore]
- **Architecture :** ARM64 (requis par ARCore généralement), ARMv7 peut être supporté selon les choix.

### 6.2 Technologie de Réalité Augmentée
- **Framework :** Google ARCore SDK.
- **Fonctionnalités ARCore utilisées :** Détection de plans (horizontaux), Ancrage (Anchors), Tracking de mouvement, Estimation de la lumière (optionnel).

### 6.3 Outils de Développement
- **Moteur de jeu / IDE :** [Choisir l'un ou l'autre]
    - **Option A : Unity Engine** (avec le package AR Foundation / ARCore XR Plugin). Fortement recommandé pour les jeux 3D et la gestion AR simplifiée.
    - **Option B : Android Studio** (avec Kotlin/Java et le SDK ARCore natif ou Sceneform). Plus complexe pour la partie jeu 3D/physique.
- **Logiciels graphiques :** Photoshop (pour le titre, UI), [Blender/Maya/3ds Max ?] (pour les modèles 3D si nécessaires : flèche, planète, pomme).
- **Gestion de version :** Git (avec une plateforme comme GitHub, GitLab, Bitbucket).

### 6.4 Langages de Programmation
- **Si Unity :** C#
- **Si Android Studio :** Kotlin (préféré) ou Java.

### 6.5 Services Externes et API
- **Google Play Services :**
    - **Google Play Games Services :** Pour l'authentification (Login) et les Classements (Leaderboards). Potentiellement les Succès (Achievements).
    - **Google Play Billing Library :** Pour les achats In-App (si implémentés).
- **Backend (Optionnel) :**
    - Si l'authentification custom SQL/PHP est choisie (non recommandé), un serveur backend avec une base de données (MySQL/PostgreSQL) et des API REST (PHP/Node.js/Python...) sera nécessaire. Ceci sort probablement du cadre d'un mini-projet.

### 6.6 Gestion des Données
- **Données locales :**
    - Préférences utilisateur (skins équipés, paramètres sonores...). Utilisation de `SharedPreferences` (Android Studio) ou `PlayerPrefs` (Unity).
    - Sauvegarde locale du meilleur score / état de jeu (si jeu hors ligne ou avant synchro serveur). Fichiers locaux ou base de données SQLite.
- **Données en ligne (via Google Play Services ou Backend) :**
    - Profil joueur (ID, pseudo).
    - Meilleurs scores (pour le classement).
    - Monnaie virtuelle (Pièces).
    - Inventaire des skins achetés.

<div class="page-break"></div>

## 7. Contraintes du Projet

### 7.1 Contraintes Techniques
- **Compatibilité matérielle :** L'application ne fonctionnera que sur les smartphones Android compatibles ARCore. La liste est maintenue par Google.
- **Performance AR :** Les performances dépendent fortement de la puissance du smartphone et de la complexité de la scène 3D/physique. Des optimisations seront nécessaires.
- **Conditions environnementales :** La qualité de l'expérience AR dépend de l'environnement (bon éclairage, surfaces texturées).

### 7.2 Contraintes Temporelles
- **Date limite de soumission du Cahier des Charges :** Mercredi 30 Avril [Année].
- **Date limite de finalisation du projet :** [Préciser la date de rendu finale du mini-projet].
- Le temps de développement est limité au cadre du mini-projet. Les fonctionnalités seront priorisées.

### 7.3 Contraintes de Ressources
- **Équipe :** 2 personnes (BEAUJARD Traïan, RAMALLO Alex).
- **Budget :** A priori nul (utilisation d'outils gratuits ou licences éducatives). Pas d'achat d'assets prévu initialement.
- **Matériel :** Nécessite au moins un smartphone Android compatible ARCore pour les tests.

<div class="page-break"></div>

## 8. Plan de Travail (Planning Prévisionnel)
*(Section requise par le professeur. À remplir avec des estimations)*

### 8.1 Phases du Projet
1.  **Phase 1 : Conception et Planification (Terminée en partie)**
    *   Définition du concept (GDD).
    *   Rédaction du Cahier des Charges.
    *   Choix technologiques finaux (Moteur/IDE).
    *   Mise en place de l'environnement de développement et Git.
2.  **Phase 2 : Développement du Cœur de Jeu (Core Gameplay)**
    *   Mise en place du projet (Unity/Android Studio).
    *   Intégration ARCore basique (détection de surface, placement d'objets test).
    *   Implémentation du système de tir et de la physique de base (balistique simple).
    *   Implémentation de la physique d'attraction des planètes.
    *   Création des assets 3D/2D de base (flèche, planète, cible).
3.  **Phase 3 : Développement des Fonctionnalités Principales**
    *   Implémentation de la génération procédurale des niveaux.
    *   Développement du système de score et de gestion des flèches.
    *   Mise en place des conditions de victoire/défaite et du flux de jeu (niveaux successifs).
4.  **Phase 4 : Développement de l'Interface Utilisateur et Fonctionnalités Secondaires**
    *   Création et intégration des écrans UI (Menu, Jeu HUD, Boutique, Fin de partie).
    *   Implémentation de l'authentification (Google Play Services).
    *   Implémentation du classement (Google Play Services).
    *   Implémentation de la boutique et du système de monnaie/skins.
5.  **Phase 5 : Tests, Optimisation et Finalisation**
    *   Tests fonctionnels et de performance sur différents appareils (si possible).
    *   Débogage.
    *   Optimisation (graphismes, code, consommation batterie).
    *   Peaufinage (effets visuels/sonores, UX).
    *   Préparation des livrables (build, documentation).

### 8.2 Jalons Clés et Livrables Associés
*(Exemple à adapter avec des dates réalistes)*

- **Jalon 1 (Date : 30/04/YYYY) :** Cahier des Charges validé.
    - Livrable : Version finale du CdC.
- **Jalon 2 (Date : XX/05/YYYY) :** Prototype AR fonctionnel.
    - Livrable : Build avec détection de surface, placement d'objets et tir simple.
- **Jalon 3 (Date : XX/05/YYYY) :** Cœur de jeu implémenté.
    - Livrable : Build avec physique d'attraction, niveaux basiques, conditions victoire/défaite.
- **Jalon 4 (Date : XX/06/YYYY) :** Fonctionnalités principales terminées.
    - Livrable : Build avec génération procédurale, score, UI de base, authentification/classement.
- **Jalon 5 (Date : XX/06/YYYY) :** Projet finalisé.
    - Livrable : Application complète (APK/AAB), code source, documentation, rapport/présentation.

### 8.3 Répartition Prévisionnelle des Tâches
*(Exemple très simplifié, à discuter et détailler entre vous)*

- **Traïan B. :** Responsable principal du développement AR, physique, génération procédurale. Backend (si nécessaire).
- **Alex R. :** Responsable principal du développement UI/UX, intégration services externes (Google Play), système de boutique/monnaie. Assets graphiques.
- **Les deux :** Conception, tests, débogage, documentation.

<div class="page-break"></div>

## 9. Livrables du Projet
La réalisation de ce projet aboutira aux livrables suivants :

1.  **Le Cahier des Charges (ce document).**
2.  **Le Game Design Document (GDD) (en annexe ou séparé).**
3.  **Le code source complet du projet,** versionné avec Git et incluant les commentaires nécessaires.
4.  **L'application Android compilée** sous forme de fichier APK (ou AAB pour une éventuelle publication).
5.  **Une documentation technique succincte** expliquant l'architecture du projet, les choix techniques majeurs et comment compiler/exécuter l'application.
6.  **Un rapport final et/ou une présentation orale** résumant le travail effectué, les difficultés rencontrées et les résultats obtenus (selon les exigences du module).
7.  **(Optionnel) Les assets graphiques et sonores** créés spécifiquement pour le projet.

<div class="page-break"></div>

## 10. Critères d'Acceptation
Le projet sera considéré comme réussi si les critères suivants sont remplis :

- **Fonctionnalité de base :** L'application se lance, détecte les surfaces via ARCore et permet de jouer au moins un niveau de puzzle physique.
- **Gameplay principal :** Le joueur peut viser, ajuster les paramètres (force/masse) et tirer des flèches dont la trajectoire est visiblement affectée par la gravité des planètes pour atteindre une cible.
- **Génération procédurale :** L'application génère des niveaux différents à chaque nouvelle partie.
- **Progression :** Un système de score et de gestion des flèches est fonctionnel, menant à une condition de défaite (plus de flèches).
- **Interface Utilisateur :** Les écrans principaux (Titre, Menu, Jeu, Game Over) sont présents et fonctionnels.
- **Stabilité :** L'application est suffisamment stable pour permettre une session de jeu complète sur un appareil compatible.
- **Respect des consignes :** Le projet répond aux exigences du mini-projet (utilisation IHM, CdC, livrables...).
- **(Objectif+) Authentification et Classement :** La connexion via Google Play Services et l'affichage d'un classement fonctionnel sont implémentés.
- **(Objectif++) Boutique :** La boutique avec achat/équipement de skins cosmétiques est fonctionnelle.

<div class="page-break"></div>

## 11. Annexes
*(Optionnel : peut contenir des liens vers des documents externes, des maquettes détaillées non incluses dans le corps, etc.)*

- Annexe A : Game Design Document (GDD) vX.X [Lien ou document joint]
- Annexe B : Maquettes UI détaillées [Liens ou images]
- ...