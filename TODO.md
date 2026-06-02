# TODO – Recipe2shoplist (R2SL)

Liste des tâches à réaliser. Cocher au fur et à mesure.

---

## Prochaine vague (juin 2025)

### 1. Assistant « Nouvelle recette »
- [x] Assistant pas à pas (nom + ingrédients)
- [x] Sauvegarde puis ouverture recette OU ajout au repas selon contexte
- [x] `NewRecipeWizardFragment` + navigation

### 2. Bouton « Nouvelle recette »
- [x] Choix assistant / édition complète via `RecipeCreationLauncher`

### 3. Raccourci depuis le sélecteur de recettes (menu)
- [x] `recipe_selector_new_recipe` → `RecipeCreationLauncher`

### 4. Popup visualisation jour (menu)
- [x] `dialog_day_meals.xml` + lignes par plat avec boutons compacts

### 5. Export menu en PDF (grille)
- [x] `WeeklyMenuPdfGenerator` + bouton export semaine

### 6. Export liste de courses PDF
- [x] Emojis, cases ☐/☑, sources repas en fin de ligne

### 7. Recopie de menu
- [x] `MenuStorageManager.copyDayToFollowingDays` + UI dans dialog jour

### 8. Mise à jour d’une liste de courses
- [x] Appui long + `ShoppingListGenerator`

### 9. Rayons modifiables et choix forcé
- [x] Liste éditable dans Paramètres (`RayonsManager` + UI)
- [x] Choix parmi la liste (plus de saisie libre) : édition recette, ajout manuel liste
- [x] Option « Autre » pour ajouter un rayon à la volée
- [x] Suppression avec réaffectation dans les recettes
- [x] Inclure `rayons_list.json` dans le backup ZIP global

---

## Historique (avant juin 2025)

### 1. Filtres dans le sélecteur de menu des jours
- [x] Ajouter des filtres dans le sélecteur de menu des jours pour faciliter le choix des recettes

### 2. Bouton supprimer un ingrédient
- [ ] Remplacer le bouton actuel "supprimer un ingrédient" (trop facile à cliquer par erreur) par un bouton rouge avec une icône de poubelle

### 3. Nombre de portions
- [ ] Ajouter la notion de **nombre de portions** dans les recettes et dans le choix des menus
- [x] Lors du clic sur le menu déjà sélectionné : portions +/− (dialogue repas)
- [ ] Lors du choix d’un repas, par défaut noter "1 portion"
- [ ] Lors de la génération de la liste des courses : calcul proportionnel complet
- [ ] Chiffres entiers + icône info pour fractions
- [ ] Rappel des quantités par recette dans le détail ingrédient

### 4. Liste préétablie de rayons (ancienne spec)
- [x] Voir section 9 ci-dessus (implémenté juin 2025)

### 5. Bouton "+" onglet recettes
- [ ] Bouton "+" dans l’onglet recettes coupé : revoir la taille et les marges

### 6. Centrage des icônes dans les boutons
- [ ] Icônes mal calées dans les boutons : qu’elles soient toujours centrées

### 7. Taille des icônes portrait / paysage
- [ ] Les icônes changent de taille en portrait et paysage et débordent en repassant en portrait

### 8. Tablette et icônes
- [ ] Corriger le plantage sur tablette
- [ ] Mettre la même icône partout (harmonisation)

### 9. Emojis sur les ingrédients
- [x] Dictionnaire + choix emoji à l’édition (partiellement fait)
- [ ] Export/import JSON dictionnaire depuis paramètres (si incomplet)
- [ ] default_emoji_dict.json + bouton charger par défaut (vérifier état)

---

*Dernière mise à jour : 2025-06-02*
