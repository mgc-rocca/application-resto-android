# Resto V1.1 — configurer Google Cloud et compiler l’APK

Ces opérations sont à effectuer **vous-même**. Aucun compte, abonnement, paramètre de facturation ou clé n’est créé par le développement. La compilation et le carnet local fonctionnent sans clé ; la carte et la recherche Google seront alors indisponibles.

## 1. Avant la mise à jour : protéger votre carnet

Dans Resto V1, ouvrez **Stats → Sauvegarde → Exporter**, enregistrez un ZIP et conservez une deuxième copie sur votre ordinateur. Ne désinstallez pas Resto pour installer la V1.1. Conservez le certificat qui signait votre APK précédent : changer de certificat empêche Android de mettre à jour l’application en place.

Si l’APK précédent était compilé en Debug dans Android Studio, sauvegardez votre fichier `~/.android/debug.keystore` dans un emplacement privé. Ne le publiez jamais dans GitHub. Pour un APK signé Release, conservez votre ancien fichier `.jks`, son alias et ses mots de passe.

## 2. Choisir un projet Cloud

1. Ouvrez [Google Cloud Console](https://console.cloud.google.com/) et connectez-vous avec votre compte.
2. En haut, cliquez sur le sélecteur de projet, puis **Nouveau projet**.
3. Nommez-le, par exemple `Resto personnel`, puis cliquez sur **Créer**. Vous pouvez également sélectionner un projet existant dédié à Resto.
4. Vérifiez que le nom de ce projet reste sélectionné avant chaque opération suivante.

## 3. Activer la facturation

1. Ouvrez le menu **Facturation**.
2. Associez votre projet à votre compte de facturation ou suivez la création d’un compte de facturation.
3. Utilisez votre adresse de facturation réelle en France : les conditions EEE s’appliquent. Ne contournez pas ces conditions avec une fausse adresse.
4. Lisez et acceptez vous-même les conditions et moyens de paiement demandés. N’activez aucun forfait supplémentaire pour suivre ce guide.

La facturation active est nécessaire même si votre consommation reste dans les franchises gratuites. Les tarifs peuvent changer. Une alerte budgétaire n’interrompt pas automatiquement le service.

## 4. Activer uniquement les trois API utilisées

Dans **API et services → Bibliothèque**, recherchez puis activez :

| API dans la console | Pourquoi elle est nécessaire |
| --- | --- |
| **Maps SDK for Android** | Fond de carte Android |
| **Places API (New)** | Places SDK Android initialisé en mode New ; requêtes d’identifiant/coordonnées |
| **Places UI Kit** | Autocomplétion officielle et affichage officiel du nom/adresse |

Le SDK Android Places est une bibliothèque Gradle ; le service à activer pour ses requêtes New est **Places API (New)**. N’activez pas Places API Legacy, Geocoding, Directions, Routes, Maps JavaScript, Maps Static ou Firebase. Le lien d’itinéraire ouvre Google Maps et n’exige pas Directions API.

## 5. Récupérer vos SHA-1 dans Android Studio

1. Ouvrez le dossier Resto dans Android Studio, puis attendez la synchronisation Gradle.
2. Si un SDK manque, ouvrez **Tools → SDK Manager** et installez **Android SDK Platform 35** et ses outils. Utilisez **JDK 17** dans **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**. Le wrapper du projet gère Gradle.
3. Ouvrez l’onglet **Terminal** au bas d’Android Studio.
4. À la racine du projet, exécutez :

```bash
./gradlew signingReport
```

5. Repérez le bloc **Variant: debug**, puis copiez sa ligne **SHA1**. Ne confondez pas SHA1 et SHA-256.
6. Le nom de package est exactement **`fr.martinrocca.resto`**, en Debug comme en Release.

Pour un certificat de production existant :

```bash
keytool -list -v -keystore /chemin/vers/votre-cle.jks -alias votre_alias
```

`keytool` demande le mot de passe dans le terminal. Relevez **SHA1**. Ne placez pas le mot de passe dans une commande ou dans le dépôt. Un APK Release signé avec un autre certificat nécessite une autre entrée package/SHA-1 dans Cloud. Pour l’installation personnelle hors Play Store, n’utilisez pas une empreinte de Play App Signing qui ne signe pas votre APK local.

## 6. Créer et restreindre les clés

Deux clés distinctes facilitent le diagnostic et les restrictions :

1. Dans **API et services → Identifiants → Créer des identifiants → Clé API**, créez une clé nommée `Resto Maps Android`.
2. Ouvrez sa fiche. Dans **Restrictions relatives aux applications**, sélectionnez **Applications Android**.
3. Cliquez sur **Ajouter un élément**, indiquez `fr.martinrocca.resto` et votre SHA-1 Debug. Ajoutez également une entrée avec le SHA-1 du certificat Release si vous utilisez des APK Release.
4. Dans **Restrictions relatives aux API**, choisissez **Restreindre la clé**, puis uniquement **Maps SDK for Android**. Enregistrez.
5. Créez une deuxième clé nommée `Resto Places Android`. Appliquez les mêmes restrictions Android package/SHA-1.
6. Restreignez cette deuxième clé à **Places API (New)** et **Places UI Kit**. Enregistrez.
7. Attendez quelques minutes avant le test pour laisser les restrictions se propager.

Si la console ou un SDK refuse la configuration : vérifiez le nom exact de l’API, l’empreinte du certificat réellement utilisé et la documentation des restrictions. **Ne rendez pas la clé non restreinte pour corriger un problème.** Une clé Android limitée à ces services est le point de départ prévu par l’intégration.

Les clés contenues dans l’APK peuvent être extraites. Leur protection repose sur package/SHA-1, les restrictions d’API, les quotas et le suivi d’usage. N’envoyez pas vos clés dans une issue GitHub, un commentaire PR ou un ZIP partagé.

## 7. Renseigner les paramètres locaux

À la racine du projet, copiez `local.properties.example` sous le nom `local.properties`. Ce fichier est ignoré par Git. Android Studio y renseigne souvent déjà `sdk.dir` : gardez ce chemin.

```properties
sdk.dir=/home/martin-rocca/Android/Sdk
GOOGLE_MAPS_API_KEY=votre_cle_maps_android_restreinte
GOOGLE_PLACES_API_KEY=votre_cle_places_android_restreinte
```

Ne mettez pas de guillemets autour des clés. Adaptez le chemin à votre ordinateur. La clé Maps passe au manifeste Android ; la clé Places passe à `BuildConfig`. Ces valeurs ne doivent jamais être ajoutées aux fichiers versionnés. Si la clé Places n’est pas renseignée, le build utilise la clé Maps ; dans ce cas une clé unique doit être restreinte aux trois API. Deux clés restent plus claires.

Avant une distribution, vérifiez les liens publics [Conditions](CONDITIONS.md) et [Confidentialité](CONFIDENTIALITE.md). Par défaut, l’APK les ouvre sur `master/docs/`. Pour tester un APK compilé avant la fusion de cette PR, ajoutez :

```properties
RESTO_PUBLIC_DOCUMENTS_REF=codex/resto-v1.1-google-migration
```

Après fusion, revenez à `master` ou retirez cette ligne. Pour une adaptation de Resto, remplacez les URL publiques par celles de votre propre dépôt/service et adaptez les textes.

## 8. Compiler et installer

Dans le Terminal Android Studio, à la racine :

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest --no-daemon
```

L’APK Debug se trouve dans `app/build/outputs/apk/debug/app-debug.apk`. `assembleRelease` compile aussi une version Release ; sans signature configurée, cette sortie est non signée et ne remplace pas directement votre APK installé.

Pour une mise à jour personnelle utilisant votre ancien certificat Debug :

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Vous pouvez aussi copier l’APK sur votre téléphone et l’ouvrir. Android peut demander d’autoriser l’installation depuis l’application de transfert. Installez par-dessus la V1. **Ne désinstallez pas pour contourner un refus de signature** : vérifiez d’abord la clé utilisée et votre sauvegarde.

Pour une version Release signée, utilisez **Build → Generate Signed App Bundle or APK → APK**, choisissez votre certificat existant et son alias, puis **Release**. Vérifiez que son SHA-1 est autorisé dans Cloud. Ne commitez ni le certificat ni ses mots de passe.

## 9. Vérifier les deux services

1. Ouvrez Resto avec Internet actif. Carte doit apparaître, sans filtre actif, avec les anciennes adresses et leurs pins sauge.
2. Déplacez et zoomez ; vérifiez eau bleutée, parcs sauge clair et POI discrets.
3. Touchez **+**, saisissez un nom puis **Rechercher avec Google**. Essayez `Chez Janou Paris`, un petit indépendant que vous connaissez, un café et un restaurant étranger. Google affiche les suggestions dans son propre écran.
4. Sélectionnez l’établissement. L’association est conservée ; **Informations Google Maps** ouvre son nom/adresse dans le composant officiel. Aucune note Google ne doit être affichée.
5. Complétez vos champs personnels, ajoutez une envie ou une visite et enregistrez. Vérifiez le Journal, les tags et la photo.
6. Après fermeture complète de l’application, utilisez **Adresses à localiser → Localiser** pour recharger à la demande une position uniquement Google. Les anciennes coordonnées V1 restent présentes.
7. Refusez la géolocalisation, puis répétez la recherche. Elle doit rester disponible.
8. Coupez Internet et vérifiez ajout manuel, édition, consultation, filtres, Stats et ZIP. Un fond de carte complet hors connexion n’est pas garanti.

Carte grise après 15 secondes : vérifiez Maps SDK activé, facturation, clé Maps, package, SHA-1 et réseau. Suggestions indisponibles : vérifiez Places API (New), Places UI Kit et la clé Places. Sans Play Services valides, Resto doit proposer le Journal. Le [protocole complet](V1.1-TESTS.md) détaille les tests Android et Fairphone.

## 10. Coûts, quotas et alertes

Liste globale Google consultée le 11 octobre 2026, en USD hors taxes, premier palier payant par **1 000 événements** :

| SKU réellement utilisé | Franchise mensuelle publiée | Tarif après franchise, premier palier | Quand |
| --- | ---: | ---: | --- |
| Maps SDK | Illimitée | Sans frais publiés pour ce SKU | Carte native sans Map ID Cloud |
| Places UI Kit — Autocomplete per Session | 10 000 sessions | 10 USD / 1 000 sessions | Ouverture du composant puis recherche ; session gérée par Google |
| Places UI Kit Query | 10 000 requêtes | 1 USD / 1 000 requêtes | Chargement explicite de la fiche Google |
| Place Details Essentials | 10 000 requêtes | 5 USD / 1 000 requêtes | `ID` + `LOCATION`, sur sélection/Localiser en l’absence de cache |

Demander `LOCATION` déclenche Essentials ; ce n’est pas le SKU gratuit « IDs Only ». Il n’y a ni Text Search, Nearby Search, rating, review ou photo API dans Resto V1.1. UI Kit ne facture pas comme notre propre boucle de requêtes Autocomplete : les sessions restent gérées dans le composant. Un nouvel écran/session ou un nouveau chargement explicite peut être facturé.

Un usage personnel normal devrait rester sous ces franchises ; ce n’est pas une garantie, en particulier si vous partagez de nombreux APK ou si des règles tarifaires changent. Les requêtes de tous les APK utilisant votre projet se cumulent. Aucune validation réelle de consommation n’a été possible sans projet/clé.

Dans **Google Maps Platform → Quotas** ou **API et services → API concernée → Quotas et limites système**, commencez avec, si les compteurs sont modifiables :

- Place Details New : **30 requêtes/jour**, ou **10/minute** si seul un plafond par minute est proposé ;
- Places UI Kit : **30 sessions d’autocomplétion/jour** et **30 chargements de fiche/jour**, si ces compteurs existent ; à défaut **10 requêtes/minute** sur les méthodes exposées ;
- Maps SDK : conserver le quota de la console, surveiller l’usage.

Ne supposez pas qu’un quota journalier existe pour chaque service. Un quota par minute seul ne plafonne pas une dépense mensuelle. Les valeurs ci-dessus sont des réglages de départ pour une personne, à ajuster après observation, pas des valeurs garanties de la console.

Dans **Facturation → Budgets et alertes → Créer un budget**, sélectionnez le projet Resto, fixez par exemple **5 €/mois** et des notifications à **50 %, 90 % et 100 %**. Vérifiez les destinataires d’alerte. Une alerte peut être retardée et **ne bloque pas automatiquement la facturation**. Seuls des quotas réellement appliqués ou la désactivation/révocation du service/de la clé bloquent ses nouvelles requêtes.

Sources : [tarifs globaux](https://developers.google.com/maps/billing-and-pricing/pricing), [événements SKU](https://developers.google.com/maps/billing-and-pricing/sku-details), [facturation et quotas Android Places](https://developers.google.com/maps/documentation/places/android-sdk/usage-and-billing), [activation UI Kit](https://developers.google.com/maps/documentation/places/android-sdk/places-ui-kit-get-started), [clés et restrictions](https://developers.google.com/maps/documentation/places/android-sdk/get-api-key).
