# Références de compatibilité Resto V1

Les fichiers `app/src/androidTest/assets/resto-stable-v1.zip` et
`resto-stable-v1.db` ont été **exportés par l’APK du commit stable V1**
`3413bcc3d57a14771c00c9da83b8455701fcbf30` le 11 octobre 2026.
Preuve : [CI 25, job stable-v1-export](https://github.com/mgc-rocca/application-resto-android/actions/runs/38133216363),
rapport d’un test exécuté sans échec et artefact `stable-v1-reference`.

| Fichier conservé dans le dépôt | SHA-256 |
| --- | --- |
| `resto-stable-v1.zip` | `bab450b434ec8df95f2b31c0ec15602c1a31fecb9ce609fdda90f052b5348ccb` |
| `resto-stable-v1.db` | `e79c21560154b465fc551cac946a640cd2a0bb85c5eeb0434a61b277a1414076` |

Les données sont **fictives**, issues de `resto-v1-fixture.zip` : deux
restaurants, une visite avec sous-notes/commentaire, un tag et sa relation,
une envie, un plat/prix, une distinction Michelin et une petite photo PNG.
Le ZIP et la base de référence ont ensuite été produits par le sérialiseur et
Room de la V1, et non fabriqués en imitant son format. Ils ne contiennent ni
clé API, ni carnet personnel, ni donnée provenant de Google.

La CI prépare un worktree détaché au commit stable, y copie uniquement
`StableV1ExportTest.kt` et le ZIP d’entrée, puis compile l’APK V1 et exécute
ce test sur un émulateur AOSP API 35. Le test utilise une base nommée et des
dossiers photos/cache isolés ; il ne lit jamais `resto.db`.

Après restauration de l’entrée, la V1 exporte le ZIP et checkpoint sa base
SQLite. Le test transfère ces deux fichiers dans `/data/local/tmp/` avec
`UiAutomation.executeShellCommandRw`, en vérifiant leurs octets, avant que
Gradle désinstalle l’APK. `export-stable-v1.sh` les récupère puis vérifie le
ZIP, sa version, la signature SQLite, `user_version`, `quick_check` et les
clés étrangères. Un message d’erreur ADB ne peut ainsi être traité comme
une base ou une archive valide. Les fichiers temporaires de l’émulateur sont
supprimés après récupération.

Le job V1.1 télécharge la nouvelle référence avant ses tests. Les copies
versionnées permettent aussi d’exécuter les tests V1.1 localement sans lancer
un export V1. Les futures générations peuvent différer en SHA-256 à cause
des dates d’export et des noms de photos générés ; les tests comparent les
données et relations, pas un hash entre deux exports.

Ce protocole ne remplace pas la validation manuelle d’un ZIP exporté par
votre V1 installée. Réservez les tests instrumentés à un émulateur ou un
appareil de test sans carnet personnel : Gradle peut désinstaller l’APK et
effacer ses données à la fin des tests.
