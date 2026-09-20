# Resto

Resto est un carnet gastronomique Android personnel, local et sans compte. Il permet de mémoriser les restaurants visités, les adresses à essayer et l’historique détaillé de chaque repas.

## Fonctionnalités

- Journal avec cartes de même hauteur sur deux lignes (nom, puis tags), prix compact en bas à droite, recherche dans les noms, adresses, tags et commentaires ;
- même bouton et panneau de filtres dans le Journal et sur la carte : Michelin, qualité-prix, gastro, cuisines, note minimale et prix ;
- filtres Michelin, catégories et cuisines dans les Envies ;
- liste d’envies transformée automatiquement lors de la première visite ;
- plusieurs visites par restaurant, avec une note unique de 1 à 10 choisie sur un slider ;
- note du restaurant calculée sur toutes ses visites, affichée à une décimale dès la deuxième visite ;
- sélection de la date de visite dans un calendrier ;
- réutilisation et normalisation des tags de cuisine déjà créés ;
- tranche de prix facultative par restaurant : <15€, 15€ - 40€, 40€-80€, >80€ ;
- jusqu’à 5 photos par visite, copiées dans le stockage privé de l’application ;
- carte MapLibre avec fond OpenFreeMap, affichée sous un panneau transparent, centrée sur Paris au premier affichage ;
- marqueurs sans note chiffrée, palette de dix couleurs et opacités partagée avec les notes du Journal et le slider ;
- panneau de filtres de la carte : visites/envies, cuisine, catégories, Michelin, prix et note minimale (≥5, ≥6, ≥7 ou ≥8) ;
- localisation ponctuelle à la demande, via Android (sans dépendance aux services Google) ;
- partage texte sans répétition du nom, avec adresse, tags et lien Google Maps d’itinéraire ;
- recherche et autocomplétion d’adresses avec Geoapify ;
- statistiques locales, avec accès au Journal filtré depuis chaque cartouche Michelin ;
- modification et suppression confirmée des restaurants et visites ;
- export et restauration d’une sauvegarde ZIP versionnée (JSON + photos).

Toutes les données métier sont conservées dans une base Room locale. Aucune donnée du journal n’est envoyée à un serveur par l’application. Seules les recherches d’adresses sont transmises à Geoapify quand une clé est configurée, et les tuiles de la carte sont chargées depuis OpenFreeMap.

## Configuration

Prérequis : Android Studio récent, JDK 17 et SDK Android 35.

1. Ouvrir ce dossier comme projet Gradle.
2. Copier `local.properties.example` vers `local.properties`.
3. Renseigner le chemin du SDK Android.
4. Facultatif : ajouter une clé Geoapify pour activer l’autocomplétion.

```properties
sdk.dir=/chemin/vers/Android/Sdk
GEOAPIFY_API_KEY=votre_cle
```

Sans clé Geoapify, l’ajout manuel reste disponible. `local.properties` est ignoré par Git afin de ne pas versionner la clé.

## Compiler et tester

```bash
./gradlew --stop
./gradlew testDebugUnitTest --no-daemon --max-workers=1
./gradlew lintDebug --no-daemon --max-workers=1
./gradlew assembleDebug --no-daemon --max-workers=1
```

L’APK de développement est généré dans `app/build/outputs/apk/debug/app-debug.apk`. La version minimale prise en charge est Android 8.0 (API 26).

## APK personnel et mises à jour

Cette version vise une installation personnelle, pas une publication sur le Play Store. Elle conserve donc `targetSdk 35` et produit un APK debug simple à installer.

Avant chaque mise à jour :

1. exporter un ZIP depuis **Stats** et vérifier qu’il est bien présent ;
2. conserver toujours la même clé de signature ; Android Studio utilise normalement `~/.android/debug.keystore` pour les APK debug ;
3. sauvegarder ce fichier de clé dans un emplacement privé et ne jamais l’ajouter à Git ;
4. installer la nouvelle version par-dessus l’ancienne, sans désinstaller l’application.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Si Android refuse la mise à jour pour une signature différente, ne désinstallez qu’après avoir exporté le ZIP. Il faudra ensuite installer le nouvel APK puis restaurer la sauvegarde.

### Vérification rapide sur Fairphone /e/OS

- ouvrir le journal, les envies, la carte et les statistiques ;
- ajouter puis modifier un restaurant et une visite ;
- joindre jusqu’à 5 photos, faire pivoter l’écran et rouvrir la visite ;
- couper le réseau et vérifier que l’application reste utilisable hors carte/recherche ;
- exporter un ZIP, ajouter une donnée temporaire, puis restaurer le ZIP ;
- fermer complètement l’application et vérifier les données après réouverture.

Pour la version 0.3.0, vérifier aussi :

- combiner cuisine et Michelin sur la carte, puis réinitialiser les filtres ;
- autoriser une position approximative ou précise, refuser l’autorisation et essayer avec la localisation désactivée ;
- quitter la carte pendant une recherche de position : la demande doit s’arrêter ;
- ajouter plusieurs tags successifs, retirer un tag, enregistrer et rouvrir la fiche ;
- partager vers WhatsApp : nom, adresse, cuisines, distinction Michelin et lien d’itinéraire Google Maps (sans note personnelle depuis la V0.5) ;
- vérifier que les compteurs Michelin ne mélangent pas les catégories et ne comptent que les restaurants visités.

Pour la version 0.4.0 :

- ouvrir une ancienne adresse portant « qualité-prix » ou « gastro » : ces tags apparaissent comme catégories, jamais comme cuisines ;
- ajouter et modifier cuisines et catégories indépendamment, enregistrer puis rouvrir la fiche ;
- vérifier les filtres Michelin, catégories et cuisines dans Journal et Envies et leur conservation après rotation (depuis la V0.6, le Journal les regroupe dans un panneau) ;
- choisir Envie puis J’y suis allé : seul le choix actif est coloré, le titre « La visite » n’apparaît pas dans ce formulaire ;
- tester le slider à 1, 5 et 10, en glissant et en touchant directement la piste ;
- ouvrir la carte sur Paris, rechercher une adresse, se localiser et faire pivoter l’écran : la caméra se conserve et les commandes restent accessibles ;
- appliquer « ≥ 8 » : seules les adresses dont la moyenne arrondie est au moins 8 apparaissent ; les envies non notées sont masquées jusqu’à réinitialisation ;
- depuis Stats, toucher successivement Michelin et chaque nombre d’étoiles, avec une recherche et des filtres déjà actifs dans le Journal : la liste doit correspondre au compteur choisi.

Pour la version 0.5.0 :

- comparer les cartes du Journal sans tags, avec plusieurs tags et avec Michelin : hauteur identique, nom sur une ligne, tags sur une seule ligne défilante (prix séparé en bas à droite depuis la V0.6) ;
- ajouter une tranche de prix, modifier les cuisines et les catégories, enregistrer et rouvrir : le prix reste présent ; choisir un autre prix remplace le précédent, toucher le prix sélectionné le retire ;
- depuis le crayon de la fiche, modifier les tags et le prix ; le bouton « Modifier les tags » a disparu ;
- partager un restaurant visité : le prix figure dans le texte, jamais la note ni les commentaires ;
- vérifier « Commentaire » dans le formulaire Envie ;
- ouvrir les filtres de la carte, combiner Visités/Envies, prix et note, faire pivoter l’écran et réinitialiser ;
- vérifier sur téléphone que la carte descend derrière le bouton + jusqu’à la surface du menu, avec le logo MapLibre visible juste au-dessus de Journal ;
- comparer les couleurs de plusieurs notes dans le Journal, sur les pins et sur le slider, en thème clair et sombre.

Pour la version 0.6.0 :

- ouvrir les filtres du Journal : même bouton et même panneau que sur la carte ; combiner Michelin, cuisine, prix et note, vérifier le compteur du bouton puis réinitialiser ;
- faire pivoter l’écran avec le panneau ouvert, puis accéder au Journal depuis une statistique Michelin : les filtres restent cohérents ;
- comparer une carte avec beaucoup de tags et une carte sans prix : les hauteurs restent identiques, le prix reste visible en petit en bas à droite pendant le défilement des autres tags ;
- ouvrir la fiche : le nom apparaît dans le bandeau supérieur, la note moyenne au-dessus des informations et les notes individuelles dans l’historique ;
- saisir deux visites notées 7 et 8 : la fiche et le Journal affichent 7,5 ; le filtre ≥7 inclut le restaurant, ≥8 l’exclut ; modifier ou supprimer une visite recalcule la moyenne ;
- partager une adresse dont le début contient déjà le nom, sur une ligne séparée ou avant une virgule : le nom n’est pas ajouté une seconde fois, une ligne vide sépare l’adresse des cuisines et du prix ;
- ouvrir le lien partagé : destination GPS si disponible, sinon nom/adresse, sans imposer de moyen de transport ;
- installer la mise à jour sur le Fairphone et vérifier la nouvelle icône boussole-fourchette dans le lanceur.

La nouvelle icône utilise le PNG fourni, inchangé, avec un fond blanc et une marge adaptative pour les différents masques Android. Voir les [consignes officielles pour les icônes adaptatives](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).

Le partage suit le [format officiel des liens Google Maps d’itinéraire](https://developers.google.com/maps/documentation/urls/get-started#directions). Aucun appel réseau ni clé Google n’est nécessaire pour construire le lien ; le destinataire choisit son itinéraire en l’ouvrant.

« qualité-prix » et « gastro » sont des catégories personnelles cumulables, indépendantes du classement officiel Michelin. Elles restent enregistrées comme tags dans les sauvegardes existantes.

Le prix est un tag réservé, unique et facultatif, distinct des cuisines. Il suit les mêmes sauvegardes que les autres tags, sans migration Room. Les adresses sans prix sont masquées lorsqu’une tranche est sélectionnée dans les filtres.

La permission de localisation est demandée uniquement au toucher de « Me localiser ». L’application ne suit pas la position en arrière-plan et ne l’enregistre pas dans le journal. Le fond de carte utilise OpenFreeMap ; les requêtes saisies pour chercher une adresse sont envoyées à Geoapify. Les notes personnelles, commentaires et photos ne sont pas inclus dans le partage texte.

## Sauvegardes

L’écran **Stats** permet d’exporter un ZIP puis de le restaurer via le sélecteur de fichiers Android. La restauration valide d’abord le format, les relations entre les données et la présence de chaque photo. La base existante n’est remplacée que si la sauvegarde est cohérente.

Le ZIP n’est pas chiffré : il doit être conservé dans un emplacement privé. Le format 1 restera lisible lors des évolutions futures de l’application.

## Architecture

- `data/local` : entités, relations, DAO et base Room ;
- `data/repository` : transactions et règles métier ;
- `data/remote/geoapify` : autocomplétion d’adresses ;
- `data/photo` : import et suppression des photos privées ;
- `data/backup` : export et restauration ZIP ;
- `domain/model` : modèle utilisé par l’interface ;
- `ui` : écrans Jetpack Compose ;
- `navigation` : routes et barre de navigation ;
- `theme` : identité visuelle beige, anthracite et vert sauge.

La base Room est la source de vérité. Un restaurant est considéré comme visité s’il possède au moins une visite. Sa note est la moyenne de toutes ses visites, arrondie à une décimale ; les badges et filtres utilisent cette même valeur. Les couleurs utilisent le niveau entier le plus proche dans la palette validée (7,5 → niveau 8). Les notes individuelles restent inchangées. Le Journal reste trié par date de dernière visite ; la statistique globale conserve sa moyenne de toutes les visites.

La base est actuellement en version 1. Toute évolution de schéma doit fournir une migration Room, conserver le JSON exporté dans `app/schemas` et ajouter un test de migration ; aucune migration destructive ne doit être utilisée.
