# Configuration locale

## Fichier local.properties

Pour que l'application fonctionne correctement, vous devez créer un fichier `local.properties` à la racine du module `app` avec vos clés secrètes.

### Étapes de configuration

1. Copiez le fichier template :
   ```bash
   cp local.properties.example local.properties
   ```

2. Éditez `local.properties` et remplissez les valeurs :
   ```properties
   # Google API Key (depuis Firebase Console)
   GOOGLE_API_KEY=VOTRE_CLE_API_ICI
   
   # Google Client IDs (depuis Google Cloud Console)
   GOOGLE_CLIENT_ID_DEBUG=VOTRE_CLIENT_ID_DEBUG_ICI
   GOOGLE_CLIENT_ID_RELEASE=VOTRE_CLIENT_ID_RELEASE_ICI
   ```

### Où trouver ces valeurs ?

- **GOOGLE_API_KEY** : Dans la console Firebase, allez dans Paramètres du projet > Général > Vos applications
- **GOOGLE_CLIENT_ID_DEBUG/RELEASE** : Dans Google Cloud Console, allez dans APIs & Services > Credentials

### Important

⚠️ **Le fichier `local.properties` ne doit JAMAIS être commité dans Git !** Il est déjà dans `.gitignore`.

Si vous avez besoin de partager la configuration avec votre équipe, utilisez un gestionnaire de secrets sécurisé.
