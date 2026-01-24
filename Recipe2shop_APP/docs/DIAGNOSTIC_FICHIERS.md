# 🔍 Guide de Diagnostic des Fichiers - TherapIA

## 📋 Vue d'ensemble

Ce guide explique comment utiliser le système de diagnostic des fichiers intégré dans TherapIA pour identifier et résoudre les problèmes de stockage et de corruption de données qui peuvent causer des plantages au lancement de l'application.

## 🚀 Fonctionnalités du Système

### **1. Analyse Complète des Fichiers**
- **Répertoire de données internes** : Analyse de tous les fichiers de l'application
- **Répertoire de cache** : Vérification des fichiers temporaires
- **Fichiers de base de données** : Contrôle de l'intégrité des données
- **Fichiers de configuration** : Validation des paramètres utilisateur
- **Fichiers de logs** : Analyse des fichiers de journalisation
- **Fichiers de profil utilisateur** : Vérification des données personnelles

### **2. Détection des Problèmes**
- **Fichiers corrompus** : Détection des fichiers vides ou invalides
- **Fichiers suspects** : Identification des fichiers de taille anormale
- **Permissions incorrectes** : Vérification des droits de lecture/écriture
- **Fichiers manquants** : Détection des fichiers de configuration absents
- **Espace disque insuffisant** : Alerte en cas de manque d'espace

### **3. Nettoyage Intelligent**
- **Cache automatique** : Suppression des fichiers temporaires
- **Logs anciens** : Nettoyage des logs de plus de 7 jours
- **Fichiers temporaires** : Suppression des fichiers avec préfixes temp_
- **Préservation des données** : Les données importantes sont protégées

## 📱 Utilisation sur l'Appareil

### **Accès au Diagnostic des Fichiers**
1. **Ouvrir l'application** TherapIA
2. **Naviguer vers** le fragment de diagnostic d'authentification
3. **Utiliser les nouveaux boutons** :
   - 📁 **Analyser les Fichiers** : Lance l'analyse complète
   - 🗑️ **Nettoyer les Fichiers** : Supprime les fichiers temporaires

### **Interprétation des Résultats**

#### ✅ **Statut SUCCESS**
- Tous les fichiers sont intègres
- Aucun problème de stockage détecté
- L'application devrait fonctionner normalement

#### ⚠️ **Statut WARNING**
- Certains fichiers ont des avertissements
- Fichiers anciens ou de grande taille détectés
- Nettoyage recommandé

#### ❌ **Statut ERROR**
- Fichiers corrompus ou manquants
- Problèmes de permissions
- Action corrective nécessaire

## 🔧 Détails Techniques

### **Types de Fichiers Analysés**

#### **Répertoire de Données Internes**
- **Emplacement** : `/data/data/com.therapia_solutions.therapia/files/`
- **Contenu** : Fichiers de configuration, données utilisateur, profils
- **Seuils** : Fichiers > 100MB ou permissions incorrectes = WARNING

#### **Répertoire de Cache**
- **Emplacement** : `/data/data/com.therapia_solutions.therapia/cache/`
- **Contenu** : Fichiers temporaires, images en cache
- **Seuils** : Cache > 200MB ou 80% de fichiers anciens = WARNING

#### **Fichiers de Base de Données**
- **Emplacement** : `/data/data/com.therapia_solutions.therapia/databases/`
- **Contenu** : Bases de données SQLite, fichiers de schéma
- **Seuils** : Fichiers vides ou corrompus = ERROR

#### **Fichiers de Configuration**
- **Fichiers** : `therapist_profile.json`, `app_preferences.json`, `user_settings.json`
- **Contenu** : Paramètres utilisateur, préférences d'application
- **Seuils** : Fichiers manquants = WARNING, JSON invalide = ERROR

#### **Fichiers de Logs**
- **Emplacement** : `/data/data/com.therapia_solutions.therapia/files/logs/`
- **Contenu** : Logs d'erreurs, traces de débogage
- **Seuils** : Fichiers > 10MB ou total > 50MB = WARNING

### **Validation d'Intégrité**

#### **Fichiers JSON**
- Vérification de la syntaxe JSON valide
- Détection des fichiers vides ou corrompus
- Test de lecture/écriture

#### **Fichiers de Base de Données**
- Vérification de la taille (fichiers vides suspects)
- Test d'ouverture et de lecture
- Validation des permissions

#### **Fichiers de Cache**
- Détection des fichiers anciens (> 7 jours)
- Identification des fichiers de grande taille
- Vérification de l'utilité

## 🚨 Résolution des Problèmes Courants

### **Problème : Plantage au Lancement**
1. **Lancer l'analyse des fichiers**
2. **Identifier les fichiers corrompus** (statut ERROR)
3. **Utiliser le nettoyage automatique**
4. **Relancer l'analyse** pour vérifier

### **Problème : Fichiers de Grande Taille**
1. **Vérifier la catégorie** (Cache, Logs, etc.)
2. **Utiliser le nettoyage** si approprié
3. **Surveiller la récurrence** du problème

### **Problème : Permissions Incorrectes**
1. **Identifier les fichiers** avec permissions manquantes
2. **Redémarrer l'application** pour réinitialiser
3. **Vérifier l'espace disque** disponible

### **Problème : Fichiers Manquants**
1. **Vérifier les fichiers de configuration** manquants
2. **Recréer les paramètres** utilisateur
3. **Relancer l'authentification** si nécessaire

## 📊 Informations Affichées

### **Résumé Global**
- **Total fichiers** : Nombre total de fichiers analysés
- **Taille totale** : Espace utilisé par tous les fichiers
- **Statut global** : SUCCESS, WARNING, ou ERROR

### **Détails par Catégorie**
- **Statut individuel** : Pour chaque type de fichier
- **Nombre de fichiers** : Dans chaque catégorie
- **Taille utilisée** : Espace occupé par catégorie
- **Problèmes détectés** : Liste des fichiers suspects

### **Liste Détaillée des Fichiers**
- **Nom du fichier** : Avec icône (📁 dossier, 📄 fichier)
- **Taille** : En unités lisibles (B, KB, MB, GB)
- **Date de modification** : Format dd/MM/yyyy HH:mm
- **Permissions** : D (dossier), R (lecture), W (écriture)
- **Chemin complet** : Localisation exacte du fichier

## 🔄 Nettoyage Automatique

### **Fichiers Supprimés**
- **Cache** : Tous les fichiers du répertoire cache
- **Logs anciens** : Fichiers de plus de 7 jours
- **Fichiers temporaires** : Préfixes temp_, tmp_, .tmp

### **Fichiers Préservés**
- **Configuration utilisateur** : therapist_profile.json
- **Préférences** : app_preferences.json
- **Base de données** : Fichiers de données importants
- **Fichiers système** : Configuration de l'application

### **Résultat du Nettoyage**
- **Fichiers supprimés** : Nombre de fichiers nettoyés
- **Espace libéré** : Quantité d'espace récupérée
- **Analyse post-nettoyage** : Vérification automatique

## 💡 Bonnes Pratiques

### **Utilisation Régulière**
- **Lancer l'analyse** après chaque mise à jour
- **Nettoyer régulièrement** pour éviter l'accumulation
- **Surveiller l'espace disque** disponible

### **En Cas de Problème**
- **Analyser d'abord** avant de nettoyer
- **Sauvegarder** les données importantes
- **Tester l'application** après nettoyage

### **Maintenance Préventive**
- **Nettoyage hebdomadaire** recommandé
- **Surveillance** des fichiers de grande taille
- **Vérification** des permissions

## 📝 Logs et Traçabilité

### **Enregistrement Automatique**
- **Toutes les analyses** sont loggées
- **Erreurs détectées** sont tracées
- **Actions de nettoyage** sont documentées

### **Consultation des Logs**
- **Utiliser "Voir les Logs"** pour l'historique
- **Filtrer par "FILE_"** pour les logs de fichiers
- **Exporter les logs** pour analyse externe

---

**Dernière mise à jour** : 4 septembre 2025  
**Version** : 1.0.0  
**Statut** : Fonctionnel et testé
