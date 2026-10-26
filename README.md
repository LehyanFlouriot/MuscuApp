# MuscuApp 💪

Application Android pour suivre ses séances de musculation **semaine après semaine** : tes exercices, tes séries, tes répétitions (et ta charge), avec un **minuteur de repos** qui sonne même écran éteint.

| Ma semaine | Saisie d'une séance | Repos | Progrès |
|:---:|:---:|:---:|:---:|
| ![Semaine](https://github.com/LehyanFlouriot/MuscuApp/releases/latest/download/1-semaine.png) | ![Saisie](https://github.com/LehyanFlouriot/MuscuApp/releases/latest/download/2-saisie.png) | ![Repos](https://github.com/LehyanFlouriot/MuscuApp/releases/latest/download/3-repos.png) | ![Progrès](https://github.com/LehyanFlouriot/MuscuApp/releases/latest/download/4-progres.png) |

## Installer l'app sur ton téléphone

1. Sur ton téléphone, ouvre la page **[Releases](https://github.com/LehyanFlouriot/MuscuApp/releases/latest)** de ce dépôt.
2. Télécharge **`MuscuApp.apk`**.
3. Ouvre le fichier : Android te demande d'autoriser l'installation depuis ton navigateur la première fois, accepte puis appuie sur **Installer**.

Compatible Android 8.0 et plus récent. Pour mettre à jour l'app, installe simplement le nouvel APK par-dessus l'ancien : tes données sont conservées.

## Ce que fait l'app

### 📅 Ma semaine
- Tous tes exercices, rangés par groupe musculaire, avec ce que tu as fait **cette semaine**.
- Le bouton **+** sur un exercice ouvre la saisie, **pré-remplie avec ta dernière séance** : la plupart du temps il suffit d'ajuster et d'enregistrer.
- Sous chaque exercice, ta performance de la semaine précédente (🎯 *à battre*) et ta progression (▲ +12 %).
- Flèches (ou glissement à gauche / à droite) pour revoir ou compléter les semaines passées.
- Résumé de la semaine : séances, séries, répétitions et volume soulevé.

### ✍️ Saisie d'une séance
- Gros boutons **− / +** pour les séries, les répétitions et le poids (maintenir le bouton pour aller plus vite, toucher le nombre pour le taper).
- Séries différentes (10, 10, 8…) ou poids différents ? *Détailler chaque série*.
- Exercices au poids du corps (pompes, tractions…) et exercices en durée (gainage…).
- Date en un geste : *Aujourd'hui*, *Hier* ou un autre jour.
- Un bouton **Repos** lance le minuteur directement depuis la saisie, entre deux séries.

### ⏱️ Repos
- Grand minuteur circulaire : démarrer, pause, **−15 s / +15 s**.
- **Mes temps de repos** : tes durées favorites en un seul appui (modifiables, avec un nom).
- Le minuteur continue même **écran éteint ou app fermée** : notification avec le décompte (et les boutons +30 s, Pause, Arrêter), puis **sonnerie et vibration** à la fin.
- Chaque exercice peut avoir son propre temps de repos.

### 📈 Progrès
- Nombre de semaines d'entraînement d'affilée 🔥 et séries par semaine.
- Pour chaque exercice : records (charge max, 1RM estimé, max de répétitions, meilleure semaine), graphique sur 12 semaines (volume, charge max ou répétitions) et historique complet.

### ⚙️ Réglages
- Pas du poids des boutons + / − (0,5 à 5 kg), thème clair / sombre, couleurs du fond d'écran.
- **Sauvegarde** : export / import de toutes tes données dans un fichier (pratique pour changer de téléphone).

Tes données restent sur ton téléphone (et dans la sauvegarde automatique Android si elle est activée).

## Développement

Projet Android natif en **Kotlin** avec **Jetpack Compose** et **Material 3**.

```bash
./gradlew testDebugUnitTest   # tests unitaires + captures d'écran (app/build/outputs/roborazzi)
./gradlew assembleRelease     # APK signé : app/build/outputs/apk/release/app-release.apk
```

- `app/src/main/java/.../core` : logique sans dépendance Android (modèle, statistiques, semaines, minuteur), testée par des tests unitaires.
- `app/src/main/java/.../ui` : écrans Compose. `.../timer` : minuteur (alarme exacte + notifications).
- À chaque push sur la branche principale, GitHub Actions construit l'APK et le publie dans les **Releases** (avec les captures d'écran).

### Signature de l'APK

Pour que chaque nouvelle version s'installe par-dessus la précédente, l'APK est toujours signé avec la même clé : `signing/muscuapp.jks`. Cette clé est publique (elle est dans le dépôt), ce qui suffit pour un usage personnel.

Pour utiliser ta propre clé privée, ajoute dans *Settings → Secrets and variables → Actions* du dépôt : `MUSCUAPP_KEYSTORE_BASE64` (le fichier `.jks` encodé en base64), `MUSCUAPP_KEYSTORE_PASSWORD`, `MUSCUAPP_KEY_ALIAS` et `MUSCUAPP_KEY_PASSWORD`. Attention : changer de clé oblige à désinstaller l'app une fois ; exporte une sauvegarde avant.
