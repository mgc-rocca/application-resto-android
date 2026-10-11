# Resto — politique de confidentialité

Resto conserve son carnet dans une base Room, sur l’appareil, et ses photographies personnelles dans le stockage privé de l’application. Il n’utilise pas de compte Resto, Firebase, Firestore, Supabase, publicité ou service d’analytique applicative. Les notes personnelles, visites, commentaires, plats, prix, tags et données Michelin ne sont pas envoyés par Resto à Google.

La suppression d’une fiche supprime ses données locales et ses photographies associées. Les sauvegardes ZIP créées par l’utilisateur sont conservées dans l’emplacement choisi par celui-ci. Resto ne peut pas supprimer des copies que l’utilisateur a partagées ou enregistrées ailleurs. La restauration remplace le carnet courant après validation de l’archive. Il faut sauvegarder avant toute désinstallation.

Les fonctionnalités Google Maps et Google Places sont facultatives. Leur utilisation transmet à Google les recherches saisies, les identifiants des établissements demandés et les informations techniques nécessaires aux requêtes. Google peut traiter notamment des identifiants techniques, l’adresse IP et des informations de diagnostic selon la [politique de confidentialité de Google](https://policies.google.com/privacy), qui s’applique à ces services. Les [conditions Google Maps](https://maps.google.com/help/terms_maps.html) s’appliquent également.

L’autorisation de localisation n’est demandée que lorsque l’utilisateur touche « Me localiser ». La position est obtenue ponctuellement via Android. Si une position ou le centre de carte est utilisé pour favoriser la recherche Google, cette zone est transmise à Google. Le refus de localisation laisse la recherche textuelle et les fonctions locales utilisables. Même sans autorisation, Google peut favoriser une région approximative à partir de la connexion réseau.

Resto conserve durablement l’identifiant Google Places facultatif qui associe une fiche à un établissement. Les coordonnées récupérées auprès de Places restent uniquement en mémoire, pendant au plus douze heures d’utilisation, dans un cache de taille limitée. Elles ne sont incluses ni dans Room ni dans les sauvegardes ZIP. La fermeture complète du processus supprime ce cache ; les valeurs expirées sont écartées avant affichage. Les données textuelles Google sont rendues par le composant officiel Places UI Kit ; elles ne sont copiées ni dans les champs personnels ni dans les archives. Les coordonnées personnelles/importées/héritées de la V1 restent distinctes et sont sauvegardées.

Le partage d’une adresse est une action explicite de l’utilisateur. Le texte partagé comporte les informations de sa fiche et un lien d’itinéraire Google Maps, éventuellement accompagné de l’identifiant Places. Le choix et le traitement des données dans l’application destinataire relèvent de celle-ci.

Le dépôt ne contient aucune clé API utilisable. L’exploitant du projet configure ses propres clés Android restreintes. Une clé embarquée dans un APK peut être extraite : les restrictions Cloud constituent sa protection.

Pour obtenir de l’aide ou signaler un problème, ouvrez une demande dans le [dépôt Resto](https://github.com/mgc-rocca/application-resto-android/issues), sans publier de sauvegarde personnelle, de photographie privée ou de clé API. Les adaptations de Resto doivent adapter cette politique à leur propre fonctionnement.
