# Resto

Resto est un carnet gastronomique Android personnel, local et sans compte. Il permet de mémoriser les restaurants visités, les adresses à essayer et l’historique détaillé de chaque repas.

## Fonctionnalités

- Journal avec recherche dans les noms, adresses, tags et commentaires ;
- filtres Michelin et cuisines, avec fiche détaillée par restaurant ;
- liste d’envies transformée automatiquement lors de la première visite ;
- plusieurs visites par restaurant, avec une note unique de 1 à 10 ;
- sélection de la date de visite dans un calendrier ;
- réutilisation et normalisation des tags de cuisine déjà créés ;
- jusqu’à 5 photos par visite, copiées dans le stockage privé de l’application ;
- carte MapLibre avec fond OpenFreeMap, marqueurs colorés par note et filtre visites/envies ;
- recherche et autocomplétion d’adresses avec Geoapify ;
- statistiques locales ;
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
./gradlew testDebugUnitTest assembleDebug
```

L’APK de développement est généré dans `app/build/outputs/apk/debug/app-debug.apk`. La version minimale prise en charge est Android 8.0 (API 26).

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

La base Room est la source de vérité. Un restaurant est considéré comme visité s’il possède au moins une visite ; la note affichée dans les listes et sur la carte est celle de la visite la plus récente.

La base est actuellement en version 1. Toute évolution de schéma doit fournir une migration Room, conserver le JSON exporté dans `app/schemas` et ajouter un test de migration ; aucune migration destructive ne doit être utilisée.
