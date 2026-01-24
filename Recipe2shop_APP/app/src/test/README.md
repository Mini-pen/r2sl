# Tests Unitaires - TherapIA

## Vue d'ensemble

Cette suite de tests unitaires couvre l'ensemble de l'application TherapIA avec une approche complète et structurée.

## Structure des Tests

### Tests Unitaires (`src/test/`)

#### Classes Utilitaires
- **ConfigurationManagerTest** : Tests pour la gestion de configuration Firebase/Google
- **AuthLoggerTest** : Tests pour le système de logging d'authentification
- **FileDiagnosticTest** : Tests pour l'analyse des fichiers système
- **PermissionCheckerTest** : Tests pour la vérification des permissions Android
- **PerformanceTest** : Tests de performance et de charge

#### Gestionnaires de Données
- **ProfileStorageManagerTest** : Tests pour la gestion des données de profil
- **AppSettingsManagerTest** : Tests pour la gestion des paramètres de l'application

#### Système d'Authentification
- **GoogleAuthManagerTest** : Tests pour l'authentification Google

#### Tests d'Intégration
- **TestManagerIntegrationTest** : Tests d'intégration pour le système de tests
- **TestConfigurationTest** : Tests de configuration et de validation

### Tests Instrumentés (`src/androidTest/`)

#### Fragments UI
- **HomeFragmentTest** : Tests pour le fragment d'accueil
- **ProfileFragmentTest** : Tests pour le fragment de profil

## Couverture de Tests

### Fonctionnalités Testées

1. **Configuration et Authentification**
   - Vérification des fichiers de configuration
   - Gestion des erreurs d'authentification
   - Validation des permissions

2. **Gestion des Données**
   - Sauvegarde et chargement des profils
   - Gestion des paramètres de l'application
   - Persistance des données

3. **Interface Utilisateur**
   - Affichage des éléments UI
   - Interactions utilisateur
   - Navigation entre fragments

4. **Performance et Fiabilité**
   - Temps d'exécution des opérations
   - Gestion de la mémoire
   - Gestion des erreurs

### Métriques de Qualité

- **Couverture de Code** : > 80%
- **Temps d'Exécution** : < 30 secondes
- **Tests de Performance** : < 1 seconde par test
- **Tests d'Intégration** : < 60 secondes

## Exécution des Tests

### Commandes Gradle

```bash
# Exécuter tous les tests unitaires
./gradlew test

# Exécuter tous les tests instrumentés
./gradlew connectedAndroidTest

# Exécuter un test spécifique
./gradlew test --tests "ConfigurationManagerTest"

# Générer le rapport de couverture
./gradlew jacocoTestReport
```

### Script d'Exécution

```bash
# Exécuter la suite complète de tests
./run-tests.bat
```

## Configuration

### Fichier de Configuration
- `test-config.properties` : Configuration des paramètres de test

### Dépendances de Test
- JUnit 4 : Framework de test principal
- Mockito : Framework de mocking
- Espresso : Tests d'interface utilisateur
- Jacoco : Couverture de code

## Bonnes Pratiques

### Règles de Nommage
- Classes de test : `[ClassName]Test`
- Méthodes de test : `test[MethodName]_[Scenario]`
- Variables de test : `test[VariableName]`

### Structure des Tests
1. **Given** : Configuration des données de test
2. **When** : Exécution de l'action à tester
3. **Then** : Vérification des résultats

### Gestion des Mocks
- Utiliser `@Mock` pour les dépendances
- Configurer les mocks dans `@Before`
- Vérifier les interactions avec `verify()`

## Maintenance

### Ajout de Nouveaux Tests
1. Créer la classe de test dans le package approprié
2. Suivre la convention de nommage
3. Ajouter des tests pour tous les cas d'usage
4. Documenter les tests complexes

### Mise à Jour des Tests
1. Vérifier que les tests existants passent
2. Mettre à jour les tests affectés par les changements
3. Ajouter des tests pour les nouvelles fonctionnalités
4. Exécuter la suite complète de tests

## Rapports

### Génération des Rapports
- **Rapport HTML** : `app/build/reports/tests/test/index.html`
- **Rapport XML** : `app/build/reports/tests/test/TEST-*.xml`
- **Couverture Jacoco** : `app/build/reports/jacoco/test/html/index.html`

### Interprétation des Résultats
- **Tests Passés** : Fonctionnalités validées
- **Tests Échoués** : Problèmes à corriger
- **Couverture** : Pourcentage de code testé
- **Performance** : Temps d'exécution des tests

## Support

Pour toute question ou problème concernant les tests :
1. Vérifier la documentation des tests
2. Consulter les logs d'exécution
3. Exécuter les tests individuellement
4. Vérifier la configuration des mocks
