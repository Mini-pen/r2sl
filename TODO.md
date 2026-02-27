# TODO – Recipe2shoplist (R2SL)

Liste des tâches à réaliser. Cocher au fur et à mesure.

---

## 1. Filtres dans le sélecteur de menu des jours
- [x] Ajouter des filtres dans le sélecteur de menu des jours pour faciliter le choix des recettes

---

## 2. Bouton supprimer un ingrédient
- [ ] Remplacer le bouton actuel "supprimer un ingrédient" (trop facile à cliquer par erreur) par un bouton rouge avec une icône de poubelle

---

## 3. Nombre de portions
- [ ] Ajouter la notion de **nombre de portions** dans les recettes et dans le choix des menus
- [ ] Lors du choix d’un repas, par défaut noter "1 portion"
- [ ] Lors du clic sur le menu déjà sélectionné : à côté des 4 icônes, afficher le nombre de portions sélectionné, avec un **+** au-dessus et un **-** en dessous
- [ ] Lors de la génération de la liste des courses : prendre en compte le nombre de portions pour le calcul proportionnel
- [ ] Garder des **chiffres entiers** sur la liste de courses (ex. recette pour 4 avec 1 œuf, pour 2 portions → afficher 1 œuf entier avec une icône "i" en rond orange ; cliquer dessus rappelle que seul 1/2 œuf est nécessaire)
- [ ] Dans le rappel des recettes utilisées (clic sur un ingrédient de la liste) : afficher la **quantité avant la recette** (ex. 5 œufs répartis en 3 recettes → voir d’un coup d’œil la répartition)

---

## 4. Liste préétablie de rayons
- [ ] Liste préétablie de rayons à choisir pour faciliter l’ajout d’ingrédients et garder la liste de courses cohérente
- [ ] Bloc dans l’onglet **Paramètres** pour éditer cette liste de rayons
- [ ] Lors du choix : option **"Autre"** ouvrant une boîte de dialogue pour ajouter un rayon à la volée (visible aussi dans la liste des paramètres)
- [ ] Lors de la suppression d’un rayon : demander à quel nouveau rayon réaffecter les ingrédients concernés et appliquer la modification dans les recettes existantes
- [ ] Pré-remplir la liste avec les rayons déjà présents et appliquer le bon rayon à **tous** les ingrédients des listes par défaut existantes

---

## 5. Bouton "+" onglet recettes
- [ ] Bouton "+" dans l’onglet recettes coupé : revoir la taille et les marges

---

## 6. Centrage des icônes dans les boutons
- [ ] Icônes mal calées dans les boutons : qu’elles soient toujours centrées

---

## 7. Taille des icônes portrait / paysage
- [ ] Les icônes changent de taille en portrait et paysage et débordent en repassant en portrait

---

## 8. Tablette et icônes
- [ ] Corriger le plantage sur tablette (pas de souci sur téléphone ; possible problème de version Android)
- [ ] Mettre la même icône partout (harmonisation)

---

## 9. Emojis sur les ingrédients
- [ ] Ajouter des emojis sur les ingrédients
- [ ] Dictionnaire interne associant de nombreux ingrédients à des emojis
- [ ] Dictionnaire chargeable et exportable en JSON depuis les paramètres
- [ ] Lors de la saisie d’un nouvel ingrédient dans une recette : pouvoir choisir un emoji
- [ ] Par défaut : si le dictionnaire associe des mots de l’ingrédient à des emojis, les proposer en haut de la liste ; le reste = tous les emojis du dictionnaire
- [ ] En parcourant les recettes existantes, préparer une première version du dictionnaire : **default_emoji_dict.json**
- [ ] Bouton **"Charger les emojis par défaut"** dans les paramètres

---

*Dernière mise à jour : 2025-01-24*
