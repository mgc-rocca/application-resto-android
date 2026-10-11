# Resto

Carnet gastronomique Android personnel, local et sans compte. La V1.1 remplace la recherche Geoapify et la carte MapLibre/OpenFreeMap par Google Maps Platform, en conservant le carnet Room et les parcours V1.

## Utiliser Resto V1.1

- Ouverture sur **Carte**, sans filtre actif par défaut. Les filtres personnels de note, Michelin, cuisine, catégories et prix restent disponibles.
- Journal local, Envies, Statistiques, visites, notes personnelles 1–10 et moyennes, commentaires, calendrier, tags normalisés et jusqu’à cinq photos personnelles par visite.
- Pins V1 sauge opaques et uniformes ; les envies gardent leur + ; aucune couleur de pin selon la note.
- Recherche Google avec le composant officiel Places UI Kit, suggestions multilignes et recherche internationale, sans imposer la géolocalisation ; recherche élargie disponible pour les établissements spécialisés.
- Association Google facultative ; ajout et édition manuels utilisables hors connexion.
- Informations Google Maps à la demande dans le composant officiel, sans note, avis ou photo Google.
- Partage des adresses et liens d’itinéraire Google Maps sans API Directions.
- Sauvegarde ZIP version 2 et restauration des ZIP versions 1 et 2, avec photos et relations préservées.
- Thème clair indépendant du mode sombre Android ; palette sauge/encre réelle de la V1 et rouge Michelin conservés.

Room reste la seule base applicative. Il n’existe ni compte Resto ni Firebase/Firestore/Supabase. Le carnet n’est pas envoyé à Google : seules la carte et les recherches externes utilisent ses services.

## Premier démarrage et configuration

Le [guide Google Cloud pas à pas](docs/GOOGLE-CLOUD.md) explique projet, facturation à effectuer personnellement, activation des trois API nécessaires, restrictions Android/package/SHA-1, fichier local, compilation, installation et coûts.

Sans clé, Resto compile et le carnet reste utilisable ; la carte explique son indisponibilité et propose le Journal. Sans services Google compatibles, la dégradation locale suit le même principe. La compatibilité Fairphone 6 /e/OS 4.0 n’a pas été validée sur appareil : voir le [protocole et rapport de tests](docs/V1.1-TESTS.md).

## Compromis EEE à connaître

Un compte de facturation français relève des conditions EEE. Les noms/adresses Google sont affichés par UI Kit et ne sont **pas copiés durablement dans les champs personnels**. L’utilisateur renseigne les champs de son carnet, puis peut associer un ID Places. Une simple confirmation ne constitue pas une autorisation de stockage permanent de données Google.

Les coordonnées Google restent en mémoire, pendant 12 h maximum, sans export ZIP. Après fermeture complète, **Adresses à localiser → Localiser** recharge uniquement la position choisie. Les anciennes coordonnées V1, personnelles ou importées restent disponibles. Le fond Google Maps n’est pas une cartographie hors ligne garantie.

Les [arbitrages et l’architecture](docs/V1.1-ARCHITECTURE.md) détaillent la provenance des coordonnées, la migration additive Room 1 → 2 et les SDK utilisés. Les anciens IDs Geoapify restent uniquement des données historiques compatibles avec les sauvegardes.

## Compiler et tester

Prérequis : Android Studio, JDK 17 et SDK Android 35. Ouvrir ce dossier comme projet Gradle et conserver le wrapper fourni.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest --no-daemon
```

APK Debug : `app/build/outputs/apk/debug/app-debug.apk`. Pour exécuter les tests Room/ZIP sur un **émulateur de test** connecté :

```bash
./gradlew connectedDebugAndroidTest --no-daemon
```

Réservez `connectedDebugAndroidTest` à un émulateur ou un appareil de test sans carnet personnel : Gradle peut désinstaller l’APK après les tests et supprimer ses données. Sur votre téléphone quotidien, effectuez uniquement les parcours manuels avec un APK installé en mise à jour et la même signature.

La CI de PR exécute les tests JVM, le lint, compile Debug et Release, puis lance les tests instrumentés sur émulateur. Les recherches Google réelles nécessitent toujours vos clés et les tests manuels décrits dans le rapport.

## Mettre à jour sans perdre le carnet

Avant toute mise à jour, exportez un ZIP depuis Stats. Installez avec le **même certificat** et le **même package** (`fr.martinrocca.resto`), par-dessus l’APK existant, sans désinstallation. Ne publiez jamais le certificat, ses mots de passe, les clés API ou une sauvegarde personnelle. La clé d’un APK est extractible ; les restrictions Google Cloud sont essentielles.

La restauration conserve le comportement V1 : remplacement du carnet courant après validation du ZIP, pas fusion de deux carnets. Les archives V1 restent acceptées. Conservez une copie externe du ZIP avant une désinstallation ou un changement de signature.

## Informations publiques

[Conditions d’utilisation](docs/CONDITIONS.md) · [Confidentialité](docs/CONFIDENTIALITE.md) · [Licence MIT](LICENSE) · [Changelog](CHANGELOG.md).

Dépôt principal : branche **master**. Les évolutions sont proposées par branche et PR ; aucune fusion automatique dans le cadre de la V1.1.
