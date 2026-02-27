# Recipe2shoplist (R2SL) - Application de Gestion de Recettes et Listes de Courses

## 📱 Description

Recipe2shoplist est une application Android moderne conçue pour **gérer vos recettes et créer automatiquement vos listes de courses**. Elle offre une solution complète de gestion culinaire avec une interface intuitive et des fonctionnalités avancées.

## ✨ Fonctionnalités Principales

### 📝 **Gestion de Recettes** ✅ IMPLÉMENTÉ
- **Création et édition** : Interface complète pour créer et modifier des recettes
- **Métadonnées** : Favoris, notes, dates de création/modification
- **Export PDF** : Génération de PDF pour partager vos recettes
- **Import/Export** : Import de packs de recettes et export individuel

### 🛒 **Listes de Courses** ✅ IMPLÉMENTÉ
- **Génération automatique** : Création depuis les recettes
- **Gestion des quantités** : Soustraction des quantités restantes à la maison
- **Marquage des articles** : Cocher les articles achetés ou annulés
- **Export PDF** : Génération de PDF pour vos listes

### 📅 **Menus Hebdomadaires** ✅ IMPLÉMENTÉ
- **Planification** : Organisation des repas de la semaine
- **Navigation multi-repas** : Gestion de plusieurs recettes par créneau
- **Visualisation** : Vue d'ensemble de la semaine

### 🔐 **Authentification & Sécurité** ✅ IMPLÉMENTÉ
- **Connexion Google OAuth 2.0** : Authentification sécurisée via Google
- **Firebase Authentication** : Gestion des sessions et sécurité
- **Sauvegarde cloud** : Synchronisation avec Google Drive

### ⚙️ **Paramètres** ✅ IMPLÉMENTÉ
- **Gestion du compte** : Affichage des informations utilisateur connecté
- **Accessibilité** : Options d'accessibilité complètes
- **Développeur** : Outils de diagnostic et maintenance

## 🏗️ Architecture Technique

### **Technologies Utilisées**
- **Kotlin** : Langage de programmation principal
- **AndroidX** : Bibliothèques Android modernes
- **Material Design 3** : Interface utilisateur moderne
- **Navigation Component** : Gestion de la navigation entre écrans
- **MVVM** : Architecture Model-View-ViewModel
- **Room Database** : Base de données locale (architecture créée)
- **SQLCipher** : Chiffrement de la base de données
- **Firebase Auth** : Authentification et gestion des sessions
- **Google Sign-In** : OAuth 2.0 pour l'authentification
- **Coroutines** : Programmation asynchrone
- **KSP** : Traitement des annotations Room

### **Structure du Projet**
```
app/
├── src/main/
│   ├── java/com/frombeyond/r2sl/
│   │   ├── auth/              # Gestion de l'authentification
│   │   ├── data/              # Couche de données (Room + fichiers JSON)
│   │   ├── ui/                # Interface utilisateur
│   │   │   ├── home/          # Écran d'accueil
│   │   │   ├── recipes/       # Gestion des recettes
│   │   │   ├── shoppinglists/ # Listes de courses
│   │   │   ├── weeklymenu/    # Menus hebdomadaires
│   │   │   ├── accessibility/ # Options d'accessibilité
│   │   │   └── settings/      # Paramètres
│   │   └── MainActivity.kt    # Activité principale
│   └── res/                   # Ressources (layouts, strings, etc.)
```

## 🚀 État d'Avancement

### **✅ Phase 1 : Infrastructure - TERMINÉE**
- [x] Configuration du projet Android
- [x] Architecture MVVM mise en place
- [x] Navigation et menus implémentés
- [x] Interface utilisateur de base créée
- [x] Authentification Google OAuth 2.0 complète
- [x] Intégration Firebase Auth
- [x] Gestion des recettes (CRUD complet)
- [x] Gestion des listes de courses
- [x] Menus hebdomadaires

### **✅ Phase 2 : Fonctionnalités Avancées - TERMINÉE**
- [x] Métadonnées des recettes (favoris, notes)
- [x] Export PDF des recettes
- [x] Import de packs de recettes
- [x] Options d'accessibilité
- [x] Sauvegarde et restauration locale
- [x] Synchronisation Google Drive

### **⏳ Phase 3 : Améliorations Futures - PLANIFIÉE**
- [ ] Synchronisation cloud automatique
- [ ] Partage de recettes entre utilisateurs
- [ ] Suggestions de recettes
- [ ] Mode hors ligne complet
- [ ] Statistiques et graphiques

## 🔧 Configuration et Installation

### **Prérequis**
- Android Studio Arctic Fox ou plus récent
- Android SDK API 24+
- Compte Google pour l'authentification
- Projet Firebase configuré

### **Installation**
1. Cloner le repository
2. Ouvrir le projet dans Android Studio
3. Configurer Firebase (fichier `google-services.json`)
4. Synchroniser les dépendances Gradle
5. Compiler et installer sur un appareil/émulateur

### **Configuration Firebase**
- Créer un projet Firebase
- Activer l'authentification Google
- Télécharger `google-services.json`
- Remplacer le fichier temporaire

## 📱 Captures d'Écran

*Les captures d'écran seront ajoutées ici pour montrer l'interface utilisateur*

## 🤝 Contribution

Ce projet est en développement actif. Les contributions sont les bienvenues !

## 📄 Licence

[À définir]

## 📞 Contact

[Informations de contact à ajouter]

---

**Dernière mise à jour** : Janvier 2025  
**Version actuelle** : 1.0.0  
**Statut** : Phase 1 et 2 terminées, Phase 3 planifiée
