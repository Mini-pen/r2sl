# 🔧 Guide de dépannage - Authentification Google OAuth

## Problème : "Authentification échouée - Utilisateur a annulé" (Code 0)

Le code d'erreur **0** lors de la connexion Google indique généralement un problème de **configuration OAuth** plutôt qu'une annulation réelle par l'utilisateur.

## ✅ Checklist de vérification

### 1. Vérifier les Client IDs dans `app/build.gradle.kts`

Ouvrez `app/build.gradle.kts` et vérifiez que les Client IDs sont bien configurés :

```kotlin
buildTypes {
    debug {
        buildConfigField("String", "GOOGLE_CLIENT_ID_DEBUG", "\"VOTRE_VRAI_CLIENT_ID_DEBUG\"")
    }
    
    release {
        buildConfigField("String", "GOOGLE_CLIENT_ID_RELEASE", "\"VOTRE_VRAI_CLIENT_ID_RELEASE\"")
    }
}
```

**⚠️ Important** : Remplacez `"YOUR_DEBUG_CLIENT_ID_HERE"` et `"YOUR_RELEASE_CLIENT_ID_HERE"` par vos vrais Client IDs.

### 2. Vérifier le SHA-1 fingerprint dans Google Cloud Console

Le SHA-1 fingerprint doit être configuré dans Google Cloud Console pour chaque Client ID.

#### Pour l'APK Release :
```
SHA-1: 74:4f:0c:59:b0:ab:7f:9b:65:e1:43:63:d2:2d:23:b3:47:5f:37:d9
```

#### Pour l'APK Debug :
```
SHA-1: ed:25:8f:5f:30:33:74:ea:cd:79:6b:88:db:8c:9d:bd:a6:e4:d1:14
```

**Étapes** :
1. Allez dans [Google Cloud Console](https://console.cloud.google.com/)
2. Sélectionnez votre projet
3. **APIs & Services** → **Credentials**
4. Cliquez sur votre **OAuth 2.0 Client ID** (ou créez-en un si nécessaire)
5. Dans **SHA certificate fingerprints**, ajoutez les deux empreintes ci-dessus
6. **Note** : Si vous avez deux clients séparés (un pour debug, un pour release), ajoutez l'empreinte correspondante à chacun

### 3. Vérifier le Package Name

Le package name de l'application doit correspondre à celui configuré dans Google Cloud Console :

```
Package Name: com.frombeyond.r2sl
```

**Vérification** :
- Dans `app/build.gradle.kts` : `applicationId = "com.frombeyond.r2sl"`
- Dans Google Cloud Console : Le package name doit être identique

### 4. Vérifier les scopes Google Drive

Les scopes suivants doivent être activés dans Google Cloud Console :
- `https://www.googleapis.com/auth/drive.file` (créer et modifier des fichiers)
- `https://www.googleapis.com/auth/drive.metadata.readonly` (lire les métadonnées)

**Étapes** :
1. Google Cloud Console → **APIs & Services** → **Library**
2. Recherchez "Google Drive API"
3. Assurez-vous qu'elle est **activée**

### 5. Rebuilder l'APK après modification

Après avoir modifié `build.gradle.kts`, vous devez **rebuilder l'APK** :

```powershell
.\gradlew.bat clean assembleRelease
```

## 🔍 Diagnostic avancé

### Vérifier les logs de l'application

L'application enregistre des logs détaillés dans :
- **Logcat** (Android Studio) : Filtrez par `GoogleAuthManager` ou `FirebaseConfigManager`
- **Fichiers de logs** : Vérifiez les logs d'authentification dans l'application

### Messages d'erreur courants

| Code | Signification | Solution |
|------|---------------|----------|
| 0 | Configuration invalide | Vérifier Client ID, SHA-1, package name |
| 10 | Erreur de configuration | Client ID invalide ou non trouvé |
| 12500/12501 | Utilisateur a annulé | Vérifier que les scopes sont bien demandés |
| 7 | Erreur de réseau | Vérifier la connexion Internet |

### Test avec l'APK Debug

Pour tester plus facilement, utilisez d'abord l'APK Debug :

```powershell
.\gradlew.bat assembleDebug
```

L'APK sera dans : `app\build\outputs\apk\debug\app-debug.apk`

## 📝 Configuration complète recommandée

### 1. Dans Google Cloud Console

Créez **deux OAuth 2.0 Client IDs** :

**Client Debug** :
- Application type : Android
- Package name : `com.frombeyond.r2sl`
- SHA-1 : `ed:25:8f:5f:30:33:74:ea:cd:79:6b:88:db:8c:9d:bd:a6:e4:d1:14`

**Client Release** :
- Application type : Android
- Package name : `com.frombeyond.r2sl`
- SHA-1 : `74:4f:0c:59:b0:ab:7f:9b:65:e1:43:63:d2:2d:23:b3:47:5f:37:d9`

### 2. Dans `app/build.gradle.kts`

```kotlin
buildTypes {
    debug {
        buildConfigField("String", "GOOGLE_CLIENT_ID_DEBUG", "\"XXXXX-XXXXX.apps.googleusercontent.com\"")
    }
    
    release {
        buildConfigField("String", "GOOGLE_CLIENT_ID_RELEASE", "\"YYYYY-YYYYY.apps.googleusercontent.com\"")
    }
}
```

### 3. Rebuilder et tester

```powershell
.\gradlew.bat clean assembleRelease
```

## 🆘 Si le problème persiste

1. Vérifiez que vous utilisez le **bon APK** (debug vs release)
2. Vérifiez que le **SHA-1** correspond bien au keystore utilisé
3. Vérifiez que le **package name** est exactement `com.frombeyond.r2sl`
4. Attendez quelques minutes après avoir modifié la configuration dans Google Cloud Console (propagation)
5. Désinstallez complètement l'application avant de réinstaller le nouvel APK
