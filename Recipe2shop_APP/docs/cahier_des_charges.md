# R2SL - Cahier des charges

## 1. Contexte et vision
R2SL (Recipe2shoplist) est une application Android de cuisine et d'organisation des repas. L'app permet de saisir des recettes et des plats, de construire des menus hebdomadaires, puis de generer des listes de courses interactives. L'app est mono-utilisateur, sans backend en ligne. La synchronisation et l'acces aux donnees se font via Google Drive.

## 2. Objectifs
- Permettre la saisie et la gestion des recettes et des plats.
- Faciliter la planification des repas sur une semaine complete.
- Generer des listes de courses precises et exploitables en magasin.
- Offrir une experience ultra lisible, adaptee aux deficients visuels et aux personnes avec difficultes cognitives.
- Proposer des options d'accessibilite claires et un wizard de configuration au premier lancement.

## 3. Public cible
- Cuisiniers du quotidien qui veulent gagner du temps.
- Personnes avec troubles visuels ou cognitifs necessitant une interface tres lisible.
- Utilisateurs voulant exporter/importer leurs recettes et generer des PDFs.

## 4. Fonctionnalites principales

### 4.1 Recettes
- Creation, edition, suppression, duplication.
- Champs requis :
  - Nom, description courte, types (entree, aperitif, plat principal, dessert, encas), tags libres.
  - Temps : travail preparatoire, preparation, cuisson, total.
  - Ingredients avec quantites alternatives (ex: 3 pieces OU 300 g).
  - Etapes avec sous-etapes. Chaque sous-etape contient :
    - instruction normale
    - instruction FALC
- Association des ingredients aux etapes (quantite specifique par etape).

### 4.2 Plats
- Un plat peut etre :
  - lie a une recette
  - compose d'ingredients "tout pret"
  - compose d'autres plats (composition)
- La creation d'une recette cree automatiquement le plat correspondant.

### 4.3 Menus hebdomadaires
- Association de plats a des jours et des repas (matin, midi, soir, etc.).
- Edition rapide par glisser-deposer ou selection simple.

### 4.4 Listes de courses
- Generation depuis un menu hebdomadaire.
- Regroupement par type d'ingredient.
- Coches pour marquer les produits achetes.
- Affichage des recettes et jours lies a un ingredient (detail rapide).

### 4.5 Export / Import
- Export et import de recettes au format JSON formel.
- Generation de PDF type "livre de recettes".
- Mode FALC disponible pour les exports PDF.

### 4.6 Stockage
- Donnees locales dans une base chiffree.
- Synchronisation et sauvegarde via Google Drive.
- Acces aux fichiers depuis l'exterieur de l'app (Drive).

## 5. Accessibilite et inclusion (exigence prioritaire)
L'interface doit etre lisible, contrastée, et simple a comprendre. Cette section est obligatoire et prioritaire.

### 5.1 Lisibilite et perception visuelle
- Taille des textes configurable.
- Espacement configurable (marges, interlignage).
- Contraste eleve par defaut, options de themes a fort contraste.
- Icônes et images claires, contrastees, sans surcharge visuelle.
- Pas de texte important uniquement dans une image.
- Cibles tactiles larges (boutons, interrupteurs).

### 5.2 Accessibilite cognitive
- Navigation simple, peu de niveaux, pas de surcharge d'ecran.
- Libelles courts, phrases simples.
- FALC disponible sur tous les textes proceduraux.
- Eviter les patterns complexes (gestes caches, interactions non evidentes).

### 5.3 Lecture a haute voix (TTS)
- Bouton "lecture" sur chaque etape et sous-etape.
- Lecture possible des listes de courses.
- Controle vitesse/voix.
- Pause, reprendre, stop.

### 5.4 Compatibilite systeme
- Support TalkBack et labels accessibles.
- Respect des tailles systeme et preferences d'accessibilite Android.
- Reduire les animations si option activee.

## 6. Parametres d'accessibilite
L'onglet "Parametres" doit permettre de regler tous les parametres de facon simple.

### 6.1 Liste des parametres
Un ecran de parametres doit permettre d'activer ou ajuster :
- Taille texte (plusieurs niveaux).
- Taille elements UI (boutons, cartes, champs).
- Contraste (standard / eleve / tres eleve).
- Mode FALC global (active partout).
- Lecture a haute voix (active par defaut ou non).
- Vitesse de lecture, voix.
- Reduire animations / transitions.

### 6.2 Ecrans de reglage
- Chaque parametre est regle dans un ecran dedie en plein ecran.
- Exemple pour la taille du texte :
  - Un texte de previsualisation au centre de l'ecran.
  - Un gros bouton "+" en haut pour augmenter.
  - Un gros bouton "-" en bas pour diminuer.
  - Un bouton "Valider" pour confirmer le reglage.

### 6.3 Relancer l'assistant
- L'onglet "Parametres" contient un bouton unique pour relancer l'assistant de parametrage.
- Le wizard peut ecraser les choix actuels apres confirmation.

## 7. Wizard au premier lancement
Objectif : configurer l'accessibilite avec des questions simples.

### 7.1 Conditions
- Demarre a la premiere ouverture.
- Peut etre saute, puis relance depuis les parametres.
- Affiche une previsualisation des choix.

### 7.2 Exemple de questions
- Souhaitez-vous des textes plus grands ?
- Souhaitez-vous des boutons plus grands ?
- Souhaitez-vous un contraste eleve ?
- Souhaitez-vous activer le mode FALC partout ?
- Souhaitez-vous activer la lecture a haute voix ?

## 8. Contraintes techniques
- Android, Kotlin, architecture MVVM.
- Room (base locale chiffree).
- Authentification Google.
- Stockage Google Drive.
- Pas de backend en ligne.

## 9. Exigences non fonctionnelles
- Rapidite d'affichage (liste et details).
- Fonctionnement hors ligne complet.
- Stabilite et robustesse en cas de perte de connexion.
- Mise a jour non bloquante des fichiers Drive.
- Interface disponible en francais (priorite).

## 10. Criteres d'acceptation
- Tous les ecrans sont utilisables avec texte agrandi.
- Mode FALC active partout les textes simplifies.
- TTS disponible sur les instructions et listes.
- Wizard configure correctement les parametres.
- Export JSON et PDF respectent le mode FALC.
- Listes de courses exploitables en magasin (tri + coche + details).

---
Document de travail a relire et amender.
