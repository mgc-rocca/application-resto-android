# Historique des versions

## 1.0.0

- ouverture sur Carte avec l’onglet correspondant sélectionné ;
- filtre Note ≥5 actif au premier affichage de la carte, visible dans le bouton et sous la recherche, modifiable et supprimable sans réactivation pendant la navigation ;
- identité sauge & encre appliquée aux écrans existants, champs sur fond blanc, boutons, navigation, contours et états désactivés harmonisés ;
- mode sombre système conservé avec les mêmes teintes adaptées au contraste ;
- pins restaurants et envies tous en `#31473A` à pleine opacité, formes et détails intérieurs conservés, sans dépendance à la note ;
- couleurs Michelin inchangées, palette des notes conservée hors carte ;
- fond cartographique, icône V0.6, stockage local, sauvegardes et fonctionnalités métier inchangés.

## 0.6.0

- filtres du Journal regroupés dans le même bouton et panneau que la carte, avec prix et note minimale ;
- prix compact en bas à droite des cartes du Journal, séparé des tags défilants, hauteur uniforme conservée ;
- nom du restaurant affiché uniquement dans le bandeau supérieur de sa fiche ;
- moyenne de toutes les visites arrondie à une décimale pour la fiche, les listes, les filtres de notes et les couleurs des pins ;
- partage sans nom répété lorsqu’il est déjà en tête d’adresse, ligne vide avant les tags cuisine/prix ;
- lien Google Maps d’itinéraire construit avec les coordonnées, ou avec l’adresse lorsqu’elles manquent, toujours sans note ni commentaires personnels ;
- nouvelle icône adaptative utilisant l’image boussole-fourchette fournie ;
- aucun changement de schéma Room ni de format de sauvegarde.

## 0.5.0

- cartes du Journal de même hauteur : nom sur la première ligne, cuisines, catégories, prix et Michelin sur une seconde ligne défilante ;
- suppression du bouton « Modifier les tags » : édition depuis le crayon de la fiche ;
- partage sans note personnelle, avec la tranche de prix lorsqu’elle est renseignée ;
- catégorie Prix facultative, à choix unique, disponible à l’ajout et à la modification ;
- libellé « Commentaire » pour les envies ;
- filtres Visités/Envies déplacés dans le panneau de filtres de la carte, nouveau filtre prix et seuls seuils ≥5, ≥6, ≥7, ≥8 ;
- carte prolongée derrière le surplomb du bouton + jusqu’au menu, logo MapLibre conservé ;
- palette exacte de dix notes, du bordeaux au bleu, avec opacités de 14 % à 100 %, partagée par les pins, les badges de note et le slider ;
- chiffres des badges contrastés en thème clair et sombre ;
- compatibilité des sauvegardes conservée, sans changement du schéma Room.

## 0.4.0

- tags personnels « qualité-prix » et « gastro » séparés des cuisines, proposés entre cuisines et Guide Michelin ;
- mêmes filtres Michelin, catégories et cuisines dans le Journal et les Envies ;
- retrait de la date de dernière visite dans les cartes du Journal ;
- formulaire simplifié, choix Envie/Visite coloré selon la sélection, note par slider entier de 1 à 10 ;
- carte sur toute la surface au-dessus du menu, panneau supérieur transparent à 12 %, Paris par défaut ;
- marqueurs sans note chiffrée, filtre par note minimale de la dernière visite ;
- compteur Envies horizontal et grandes distinctions Michelin au-dessus de leur effectif ;
- clic sur une statistique Michelin ouvrant le Journal avec cette catégorie seule, recherche réinitialisée ;
- compatibilité des anciens tags et sauvegardes conservée, sans changement du schéma Room.

## 0.3.0 — préparation de la V1

- journal allégé : retrait du compteur et des adresses dans les cartes ;
- badges Michelin blancs, texte rouge et étoiles dessinées en nombre ;
- filtres cuisine et Michelin sur la carte, palette terre cuite → sauge ;
- bouton de localisation ponctuelle, compatible avec /e/OS sans services Google ;
- formulaire « Nouvelle adresse » simplifié ;
- partage d’une fiche texte vers WhatsApp ou une autre application ;
- sélection de plusieurs tags, également modifiable depuis la fiche restaurant ;
- statistiques distinctes pour Michelin sans étoile, une, deux et trois étoiles.

## 0.2.0

- ajout d’une CI pour la compilation, les tests et Android Lint ;
- correction des restaurants pouvant devenir invisibles ;
- gestion sûre des annulations, rotations, chargements et erreurs ;
- import atomique des photos, limite à 5, orientation EXIF et décodage mémoire réduit ;
- restauration des sauvegardes renforcée et format versionné ;
- carte cadrée sur les marqueurs, caméra restaurée et état hors connexion visible ;
- procédure d’APK personnel et parcours de vérification Fairphone /e/OS.

## 0.1.0

- première version open source.
