# Configuration des Clients OAuth pour Debug et Release

## Problème

Google Cloud Console ne permet qu'une seule empreinte SHA-1 par client OAuth dans l'interface. Pour utiliser l'authentification Google à la fois en mode debug et release, il faut créer **deux clients OAuth séparés**.

## Solution : Deux Clients OAuth

### 1. Créer les deux clients dans Google Cloud Console

#### Client OAuth pour DEBUG
1. Allez dans **Google Cloud Console** → **APIs & Services** → **Credentials**
2. Cliquez sur **+ CREATE CREDENTIALS** → **OAuth client ID**
3. Sélectionnez **Application type**: Android
4. Entrez un **Name**: `R2SL Android Debug`
5. Entrez le **Package name**: `com.frombeyond.r2sl`
6. Entrez le **SHA-1 certificate fingerprint**: 
   ```
   ed:25:8f:5f:30:33:74:ea:cd:79:6b:88:db:8c:9d:bd:a6:e4:d1:14
   ```
7. Cliquez sur **CREATE**
8. **Copiez le Client ID** (format: `XXXXX-XXXXX.apps.googleusercontent.com`)

#### Client OAuth pour RELEASE
1. Dans le même écran, cliquez sur **+ CREATE CREDENTIALS** → **OAuth client ID**
2. Sélectionnez **Application type**: Android
3. Entrez un **Name**: `R2SL Android Release`
4. Entrez le **Package name**: `com.frombeyond.r2sl`
5. Entrez le **SHA-1 certificate fingerprint**: 
   ```
   74:4f:0c:59:b0:ab:7f:9b:65:e1:43:63:d2:2d:23:b3:47:5f:37:d9
   ```
6. Cliquez sur **CREATE**
7. **Copiez le Client ID** (format: `XXXXX-XXXXX.apps.googleusercontent.com`)

### 2. Configurer l'application

#### Étape 1 : Mettre à jour `build.gradle.kts`

Remplacez les placeholders dans `app/build.gradle.kts` :

```kotlin
buildTypes {
    debug {
        buildConfigField("String", "GOOGLE_CLIENT_ID_DEBUG", "\"VOTRE_CLIENT_ID_DEBUG_ICI\"")
    }
    
    release {
        buildConfigField("String", "GOOGLE_CLIENT_ID_RELEASE", "\"VOTRE_CLIENT_ID_RELEASE_ICI\"")
    }
}
```

**Exemple avec de vrais Client IDs :**
```kotlin
buildTypes {
    debug {
        buildConfigField("String", "GOOGLE_CLIENT_ID_DEBUG", "\"603389607045-xxxxxxxxxxxxx.apps.googleusercontent.com\"")
    }
    
    release {
        buildConfigField("String", "GOOGLE_CLIENT_ID_RELEASE", "\"603389607045-yyyyyyyyyyyyy.apps.googleusercontent.com\"")
    }
}
```

#### Étape 2 : Vérifier la configuration

Le code dans `FirebaseConfigManager.kt` a été mis à jour pour :
1. D'abord essayer d'utiliser le Client ID depuis `BuildConfig` (selon le mode debug/release)
2. Sinon, utiliser le Client ID depuis `google-services.json`

### 3. Alternative : Utiliser google-services.json

Si vous préférez utiliser `google-services.json` (recommandé pour Firebase) :

1. Dans **Firebase Console**, ajoutez votre application Android avec le package `com.frombeyond.r2sl`
2. Téléchargez le `google-services.json`
3. Le fichier contiendra automatiquement les clients OAuth configurés dans Firebase
4. Firebase peut gérer plusieurs clients OAuth dans le même fichier

### 4. Vérification

Pour vérifier que la bonne configuration est utilisée :

1. Lancez l'application en mode **DEBUG** → Le Client ID debug doit être utilisé
2. Lancez l'application en mode **RELEASE** → Le Client ID release doit être utilisé
3. Vérifiez les logs dans l'application pour confirmer le Client ID utilisé

## Notes importantes

- ⚠️ **Ne partagez jamais vos Client IDs** publiquement
- ✅ Les Client IDs peuvent être inclus dans le code (ils ne sont pas secrets)
- ✅ Chaque build type (debug/release) utilisera automatiquement le bon Client ID
- ✅ Les deux clients doivent être configurés dans le même projet Google Cloud

## Résumé des empreintes SHA-1

- **DEBUG**: `ed:25:8f:5f:30:33:74:ea:cd:79:6b:88:db:8c:9d:bd:a6:e4:d1:14`
- **RELEASE**: `74:4f:0c:59:b0:ab:7f:9b:65:e1:43:63:d2:2d:23:b3:47:5f:37:d9`
