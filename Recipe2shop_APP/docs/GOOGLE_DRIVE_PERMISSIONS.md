# 🔐 Configuration des Permissions Google Drive pour R2SL

## 📋 Checklist Complète

### 1. ✅ Google Cloud Console - Activer l'API

1. Allez sur [Google Cloud Console](https://console.cloud.google.com/)
2. Sélectionnez votre projet (ou créez-en un)
3. Allez dans **APIs & Services** → **Library** (Bibliothèque)
4. Recherchez et activez :
   - ✅ **Google Drive API** (v3)
   - ✅ **Google+ API** (pour l'authentification - si nécessaire)

### 2. ✅ Google Cloud Console - Configurer les Scopes OAuth

Les scopes OAuth doivent être configurés dans votre **OAuth consent screen** :

1. Allez dans **APIs & Services** → **OAuth consent screen**
2. Configurez l'écran de consentement :
   - **User Type** : External (ou Internal si vous avez un compte Google Workspace)
   - **App name** : R2SL
   - **User support email** : Votre email
   - **Developer contact information** : Votre email
3. Dans **Scopes**, ajoutez les scopes suivants :
   - ✅ `https://www.googleapis.com/auth/drive.file` (Créer et modifier des fichiers)
   - ✅ `https://www.googleapis.com/auth/drive.metadata.readonly` (Lire les métadonnées)

### 3. ✅ Code Application - Demander les Scopes

Le code doit demander les scopes lors de la connexion Google. Vérifiez que `GoogleAuthManager` demande bien les scopes :

**Fichier** : `app/src/main/java/com/frombeyond/r2sl/auth/GoogleAuthManager.kt`

```kotlin
val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestIdToken(clientId)
    .requestEmail()
    .requestProfile()
    .requestScopes(Scope(DriveScopes.DRIVE_FILE))  // ✅ Scope pour créer/modifier des fichiers
    .requestScopes(Scope("https://www.googleapis.com/auth/drive.metadata.readonly"))  // ✅ Scope pour lire les métadonnées
    .build()
```

### 4. ✅ Permissions Android (AndroidManifest.xml)

Les permissions suivantes sont déjà configurées dans `AndroidManifest.xml` :

```xml
<!-- Permissions nécessaires pour l'authentification Google -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.GET_ACCOUNTS" />

<!-- Permissions pour Google Drive API -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### 5. ✅ Vérification des Permissions dans l'Application

L'application vérifie automatiquement les permissions via `PermissionChecker` :

```kotlin
val permissionChecker = PermissionChecker(context)
val status = permissionChecker.checkGoogleDrivePermissions()

if (!status.isGranted) {
    // Demander les permissions manquantes
    // L'utilisateur devra se reconnecter avec les nouveaux scopes
}
```

## 🔧 Scopes Google Drive Utilisés

### Scope Principal : `DRIVE_FILE`
- **URL** : `https://www.googleapis.com/auth/drive.file`
- **Permissions** : 
  - ✅ Créer des fichiers et dossiers
  - ✅ Modifier des fichiers créés par l'application
  - ✅ Supprimer des fichiers créés par l'application
  - ⚠️ **Limitation** : Accès uniquement aux fichiers créés par l'application

### Scope Secondaire : `DRIVE_METADATA_READONLY`
- **URL** : `https://www.googleapis.com/auth/drive.metadata.readonly`
- **Permissions** :
  - ✅ Lire les métadonnées des fichiers (nom, taille, date, etc.)
  - ✅ Lister les fichiers et dossiers
  - ❌ **Pas d'accès** au contenu des fichiers

## ⚠️ Important : Scope DRIVE_FILE vs DRIVE

- **`DRIVE_FILE`** (recommandé) : Accès uniquement aux fichiers créés par l'app
  - Plus sécurisé
  - Suffisant pour la plupart des cas d'usage
  - ✅ Utilisé actuellement dans R2SL

- **`DRIVE`** (accès complet) : Accès à tous les fichiers du Drive
  - Plus puissant mais moins sécurisé
  - Nécessite une validation supplémentaire de Google
  - ⚠️ Non recommandé sauf besoin spécifique

## 🔄 Processus de Connexion avec Permissions

1. **Première connexion** :
   - L'utilisateur se connecte avec Google
   - L'application demande les scopes Drive
   - L'utilisateur accepte les permissions dans l'écran de consentement Google

2. **Vérification** :
   - L'application vérifie si les scopes sont accordés
   - Si manquants, propose de se reconnecter

3. **Utilisation** :
   - L'application peut créer/modifier des fichiers dans Google Drive
   - Les fichiers sont créés dans le dossier `R2SL/` (ou configuré dans `BackupPathConfig`)

## 🛠️ Dépannage

### Problème : "Insufficient permissions"
- ✅ Vérifiez que l'API Google Drive est activée
- ✅ Vérifiez que les scopes sont dans l'OAuth consent screen
- ✅ Vérifiez que les scopes sont demandés dans `GoogleAuthManager`
- ✅ Demandez à l'utilisateur de se reconnecter

### Problème : "Access denied"
- ✅ Vérifiez que l'utilisateur a accepté les permissions
- ✅ Vérifiez que le Client ID OAuth est correct
- ✅ Vérifiez que les SHA-1 sont bien configurés

### Problème : "API not enabled"
- ✅ Activez Google Drive API dans Google Cloud Console
- ✅ Attendez quelques minutes pour la propagation

## 📝 Résumé des Actions Requises

1. ✅ **Google Cloud Console** :
   - Activer Google Drive API
   - Configurer OAuth consent screen avec les scopes Drive

2. ✅ **Code Application** :
   - ✅ Les scopes sont maintenant demandés dans `GoogleAuthManager` (corrigé)
   - ✅ Les permissions Android sont déjà dans `AndroidManifest.xml`

3. ✅ **Test** :
   - Connectez-vous avec Google
   - Vérifiez que les permissions Drive sont accordées
   - Testez la création d'un fichier dans Drive
