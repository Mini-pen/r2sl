# TherapIA - Application de Gestion Thérapeutique

## 📱 Description

TherapIA est une application Android moderne conçue pour les **thérapeutes et professionnels de santé mentale**. Elle offre une solution complète de gestion de cabinet thérapeutique avec une interface intuitive et des fonctionnalités avancées.

## ✨ Fonctionnalités Principales

### 🔐 **Authentification & Sécurité** ✅ IMPLÉMENTÉ
- **Connexion Google OAuth 2.0** : Authentification sécurisée via Google
- **Firebase Authentication** : Gestion des sessions et sécurité
- **Interface d'authentification** : Boutons de connexion/déconnexion sur tous les écrans
- **Synchronisation des états** : UI cohérente sur tous les fragments

### 🏠 **Accueil** ✅ IMPLÉMENTÉ
- **Interface d'accueil** : Message de bienvenue personnalisé
- **Bouton de connexion** : Authentification Google directement accessible
- **État d'authentification** : Affichage dynamique selon l'état de connexion

### 📊 **Tableau de Bord** ✅ IMPLÉMENTÉ
- **Vue d'ensemble** : Interface de base prête pour les statistiques
- **Navigation** : Intégrée dans le système de navigation principal

### 👥 **Gestion des Patients** ✅ IMPLÉMENTÉ
- **Interface de base** : Fragment prêt pour la gestion des patients
- **Navigation** : Intégrée dans le menu principal

### 📅 **Agenda & Planification** ✅ IMPLÉMENTÉ
- **Interface de base** : Fragment prêt pour la gestion des rendez-vous
- **Navigation** : Intégrée dans le menu principal

### 🤖 **Sandrine.AI** ✅ IMPLÉMENTÉ
- **Interface de base** : Fragment prêt pour l'intégration IA
- **Navigation** : Intégrée dans le menu principal

### 📚 **Bibliothèque de Ressources** ✅ IMPLÉMENTÉ
- **Interface de base** : Fragment prêt pour les ressources thérapeutiques
- **Navigation** : Intégrée dans le menu principal

### ⚙️ **Paramètres** ✅ IMPLÉMENTÉ
- **Gestion du compte** : Affichage des informations utilisateur connecté
- **Boutons d'authentification** : Connexion/déconnexion intégrés
- **Interface complète** : Section d'authentification avec profil utilisateur

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
│   ├── java/com/example/therapia/
│   │   ├── auth/           # Gestion de l'authentification
│   │   ├── data/           # Couche de données (Room)
│   │   ├── ui/             # Interface utilisateur
│   │   │   ├── home/       # Écran d'accueil
│   │   │   ├── dashboard/  # Tableau de bord
│   │   │   ├── patients/   # Gestion des patients
│   │   │   ├── agenda/     # Planification
│   │   │   ├── sandrine_ai/# Interface IA
│   │   │   ├── library/    # Bibliothèque
│   │   │   └── settings/   # Paramètres
│   │   └── MainActivity.kt # Activité principale
│   └── res/                # Ressources (layouts, strings, etc.)
```

## 🚀 État d'Avancement

### **✅ Phase 1 : Infrastructure - TERMINÉE**
- [x] Configuration du projet Android
- [x] Architecture MVVM mise en place
- [x] Navigation et menus implémentés
- [x] Interface utilisateur de base créée
- [x] Authentification Google OAuth 2.0 complète
- [x] Intégration Firebase Auth
- [x] Synchronisation des états d'authentification

### **🔄 Phase 2 : Fonctionnalités de Base - EN COURS**
- [x] Interface d'authentification complète
- [ ] Gestion complète des patients (CRUD)
- [ ] Système de planification des séances
- [ ] Prise de notes chiffrées
- [ ] Sauvegarde et restauration des données

### **⏳ Phase 3 : Fonctionnalités Avancées - PLANIFIÉE**
- [ ] Notifications et rappels
- [ ] Statistiques et graphiques
- [ ] Export sécurisé des données
- [ ] Mode hors ligne complet
- [ ] Intégration Sandrine.AI

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

**Dernière mise à jour** : Décembre 2024  
**Version actuelle** : 1.0.0 (Authentification complète)  
**Statut** : Phase 1 terminée, Phase 2 en cours
