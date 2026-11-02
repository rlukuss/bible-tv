# Profils familiaux — plan d'implémentation

Spec : [`../specs/2026-11-02-freebox-family-profiles-design.md`](../specs/2026-11-02-freebox-family-profiles-design.md)
Branche : `feat/freebox-family-profiles` · Paquet : `com.tcltv.sahne` (appli `com.yigitcan.matinee`)

## Idée centrale : échanger les réglages d'un profil

Matinee range déjà dans `SettingsStore` tout ce qui fait « l'accueil de quelqu'un » : `profileName`, `profileColor`,
`appOrder`, `hiddenApps`, `shownApps`, `folders`, `showInputs`, `showAddTile`, `showLastPlayed`. Plutôt que de
réécrire `HomeScreen`, `GridHome` et `LauncherViewModel` pour lire un profil, **chaque profil garde une copie de ces
champs** (`ProfileSnapshot`). Changer de profil = dans **une seule** écriture DataStore :

1. copier les champs actuels dans le profil actif ;
2. écrire ceux du profil choisi à leur place ;
3. changer `active_profile_id`.

L'accueil reste celui de Matinee et suit tout seul. Le profil Enfants a en plus `allowedApps`, appliqué par un
filtre dans `LauncherViewModel.apps` et vérifié au lancement.

## Commandes

```bash
./gradlew testPlayDebugUnitTest          # tests unitaires
./gradlew assemblePlayDebug              # APK : app/build/outputs/apk/play/debug/app-play-debug.apk
adb -s 192.168.1.50:5555 install -r app/build/outputs/apk/play/debug/app-play-debug.apk
adb -s 192.168.1.50:5555 logcat -s Matinee:* AndroidRuntime:E
```

(Sous Windows : `gradlew.bat`, avec `JAVA_HOME` sur le JDK 17.) On construit la variante `play` : elle ne cherche
jamais de mise à jour sur le GitHub de l'auteur d'origine (la variante `github` le fait).

---

## Tâche 1 — Modèle et codec des profils (logique pure)

**Fichiers** : créer `data/Profiles.kt`, `test/.../data/ProfilesCodecTest.kt`.

1. Écrire le test d'abord :
   ```kotlin
   class ProfilesCodecTest {
       private val paul = Profile(
           id = "a", name = "Paul", kind = ProfileKind.ADULT, color = 0xFF378ADD.toInt(),
           startPackage = "net.oqee.androidtv",
           snapshot = ProfileSnapshot(appOrder = listOf("com.netflix.ninja"), hiddenApps = setOf("x")),
       )
       private val kids = Profile(
           id = "b", name = "Enfants", kind = ProfileKind.CHILD, color = 0xFFEF9F27.toInt(), startPackage = null,
           allowedApps = setOf("com.google.android.youtube.tvkids", "fr.tfou.max", "com.disney.disneyplus"),
           snapshot = ProfileSnapshot(showInputs = false, showAddTile = false),
       )

       @Test fun roundTrip() = assertEquals(listOf(paul, kids), ProfilesCodec.decode(ProfilesCodec.encode(listOf(paul, kids))))
       @Test fun invalidJsonIsEmpty() = assertEquals(emptyList<Profile>(), ProfilesCodec.decode("{oops"))
       @Test fun blankIsEmpty() = assertEquals(emptyList<Profile>(), ProfilesCodec.decode(""))
       @Test fun missingFieldsTakeDefaults() {
           val p = ProfilesCodec.decode("""[{"id":"c","name":"Léa","kind":"ADULT"}]""").single()
           assertEquals(ProfileSnapshot(), p.snapshot)
           assertEquals(null, p.startPackage)
       }
       @Test fun unknownKindIsSkipped() = assertEquals(0, ProfilesCodec.decode("""[{"id":"d","name":"?","kind":"PET"}]""").size)
   }
   ```
2. `./gradlew testPlayDebugUnitTest` → échec (classes absentes).
3. Écrire `data/Profiles.kt` :
   - `enum class ProfileKind { ADULT, CHILD }`
   - `data class ProfileSnapshot(profileName, profileColor, appOrder, hiddenApps, shownApps, folders, showInputs, showAddTile, showLastPlayed)`
     avec les mêmes valeurs par défaut que `LauncherSettings`, et `fun of(s: LauncherSettings)`.
   - `data class Profile(id, name, kind, color, startPackage: String?, allowedApps: Set<String> = emptySet(), snapshot: ProfileSnapshot = ProfileSnapshot())`
   - `object ProfilesCodec { fun encode(List<Profile>): String; fun decode(String): List<Profile> }` avec `org.json`
     comme `AppCustomization.kt` ; les dossiers réutilisent l'encodage existant de `Folder` ; toute exception → liste vide.
   - `fun palette(): List<Int>` : 8 couleurs fixes.
4. Tests verts. Commit : `feat(profiles): add the profile model and its JSON codec`.

## Tâche 2 — Stockage, migration et échange

**Fichiers** : modifier `data/SettingsStore.kt`, créer `data/ProfileSwitch.kt`, tests `ProfileSwitchTest.kt`.

1. Tests d'abord, sur des fonctions pures :
   - `ProfileSwitch.migrate(settings, profiles = emptyList())` → un profil adulte « nom actuel ou "Moi" », snapshot =
     `ProfileSnapshot.of(settings)`, et cet id devient actif.
   - `ProfileSwitch.migrate(...)` avec des profils existants → inchangé.
   - `ProfileSwitch.switch(settings, profiles, activeId, targetId)` → renvoie `(profilesMisÀJour, snapshotÀÉcrire)` :
     le profil actif reçoit `ProfileSnapshot.of(settings)`, et on écrit le snapshot de la cible.
   - cible inconnue → rien ne change.
2. Dans `SettingsStore` :
   - clés `profiles` (string JSON) et `active_profile_id` ; `LauncherSettings` gagne
     `profiles: List<Profile>` et `activeProfileId: String?` ;
   - `suspend fun switchProfile(targetId)` : **un seul** `edit {}` qui applique `ProfileSwitch.switch` ;
   - `suspend fun saveProfiles(list)` ; `suspend fun ensureProfiles()` (appelle `migrate` si vide) ;
   - `fun ProfileSnapshot.writeTo(prefs: MutablePreferences)` à côté des clés existantes.
3. `LauncherViewModel.init` : appeler `store.ensureProfiles()`.
4. Tests verts ; build ; installer ; vérifier que l'accueil de Matinee est identique (migration sans perte).
   Commit : `feat(profiles): store profiles and switch them in one write`.

## Tâche 3 — « Qui regarde ? »

**Fichiers** : créer `data/WhoPolicy.kt`, `ui/WhoScreen.kt` ; modifier `SahneApp.kt`, `LauncherViewModel.kt`,
`system/HomeKeyService.kt` ; test `WhoPolicyTest.kt` ; chaînes `values/strings.xml` et `values-fr/strings.xml`.

1. Test d'abord :
   ```kotlin
   enum class WhoTrigger { COLD_START, SCREEN_ON, PROFILE_PILL }
   // WhoPolicy.shouldAsk(trigger, activeKind: ProfileKind?, profileCount: Int): Boolean
   ```
   - 1 seul profil → jamais ; `COLD_START`/`SCREEN_ON` avec actif adulte ou aucun → oui ;
     avec actif enfant → non ; `PROFILE_PILL` → oui (le code est demandé ailleurs).
2. `LauncherViewModel` : `val whoVisible: StateFlow<Boolean>` ; `fun askWho(trigger)` ; `fun pickProfile(id)` :
   - profil actif enfant et cible différente → `gate.requireForAdmin(R.string.pin_reason_profile)` d'abord
     (protection forcée, même si « protéger les réglages » est désactivé) ;
   - option « code pour les profils adultes » et cible adulte → code aussi ;
   - puis `store.switchProfile(id)`, cacher l'écran, et si `startPackage` → `startSafely(launchIntent)` ;
     échec → notice « La TV n'a pas pu s'ouvrir ».
   - `init` : `askWho(COLD_START)` une fois les profils chargés.
3. Mise en veille : `HomeKeyService` écoute déjà des broadcasts ; ajouter `Intent.ACTION_SCREEN_ON` à son
   `IntentFilter` et, à réception, démarrer `MainActivity` avec `EXTRA_ASK_WHO`. `MainActivity.handleRequest` →
   `viewModel.askWho(SCREEN_ON)`. Le simple économiseur d'écran n'émet pas `SCREEN_ON` : rien à faire.
4. `ui/WhoScreen.kt` : titre « Qui regarde ? », rangée centrée de ronds (couleur, initiale ou photo via
   `ProfilePhoto`, prénom dessous), focus initial sur le profil actif, réutiliser le style de `ui/Components.kt` et
   `ui/Glass.kt`. Back ne ferme pas l'écran.
5. `SahneApp` : si `whoVisible`, afficher `WhoScreen` à la place de `HomeScreen` (le `PinGate` reste au-dessus).
6. `TopBar.ProfileMenu` : ajouter l'entrée « Changer de profil » → `askWho(PROFILE_PILL)`.
7. Tests verts, build, essai sur le Player (démarrage, pastille, veille). Commit : `feat(profiles): ask who's watching`.

## Tâche 4 — Gérer les profils dans les réglages

**Fichiers** : modifier `ui/settings/SettingsScreen.kt`, `LauncherViewModel.kt`, chaînes.

1. Nouvelle section « Profils » (dans « Écran d'accueil ») : liste des profils, « Ajouter un profil ».
2. Fiche profil : prénom (champ texte existant de Matinee), adulte ou enfant, couleur (8 ronds), « Démarrer avec »
   (aucune / liste des applis, OQEE proposé en premier pour un adulte), pour un enfant « Applis autorisées »
   (liste à cocher des applis installées), supprimer (pas le profil actif, pas le dernier).
3. Créer un profil Enfants sans code parent → ouvrir d'abord la création du code (écran existant de « Contrôle
   parental »).
4. Option « Code pour les profils adultes » dans « Contrôle parental ».
5. **Créer, modifier ou supprimer un profil demande toujours le code parent** (demande de la famille), même juste
   après l'avoir tapé pour ouvrir les réglages et même si « protéger les réglages » est désactivé :
   `gate.requireForApp(R.string.pin_reason_profile_edit, locked = true)` sur chacune de ces actions.
   Build, essai. Commit : `feat(profiles): manage profiles in settings`.

## Tâche 5 — Accueil du profil Enfants

**Fichiers** : créer `data/ChildAccess.kt` ; modifier `LauncherViewModel.kt`, `ui/home/HomeScreen.kt`
(le moins possible) ; test `ChildAccessTest.kt`.

1. Test d'abord : `ChildAccess.visible(apps, profile)` → adulte : inchangé ; enfant : seulement les paquets de
   `allowedApps`, sans dossiers ; `ChildAccess.canLaunch(packageName, profile)`.
2. `LauncherViewModel.apps` : appliquer `ChildAccess.visible` avec le profil actif. `launchApp` et `openLiveTv` :
   refuser si `!canLaunch` (et pas de TV en direct pour un enfant).
3. À la création d'un profil Enfants, son snapshot met `showInputs = false`, `showAddTile = false`,
   `showLastPlayed = false` : la tuile TV en direct, l'ajout d'appli et la reprise disparaissent sans toucher
   `HomeScreen`. Vérifier dans `HomeScreen`/`GridHome` qu'aucune autre entrée n'ouvre « toutes les applis » ; la
   masquer si `activeKind == CHILD`.
4. Édition de l'accueil et menu d'appli (appui long) : déjà sous `requireForAdmin` ; forcer le code en profil enfant.
5. Tests verts, build, essai. Commit : `feat(profiles): a reduced home for children`.

## Tâche 6 — Touches bloquées en profil Enfants

**Fichiers** : créer `system/KidKeyFilter.kt` ; modifier `system/HomeKeyService.kt` ; test `KidKeyFilterTest.kt`.

1. **D'abord, sur le Player** : activer le service « Matinee Home button », utiliser « apprendre une touche » de
   Matinee (Réglages → Bluetooth et télécommande) sur chaque bouton raccourci Free, noter les `keyCode` reçus.
   Si un bouton n'arrive jamais au service, le noter : il ne pourra pas être bloqué (README).
2. Test d'abord : `KidKeyFilter.blocks(keyCode, isChild, pinScreenOnTop)` :
   - adulte → jamais ; enfant → CH+/CH−, 0–9, ASSIST, SEARCH et les codes Free relevés ;
   - enfant + écran de code au premier plan → les chiffres passent ;
   - HOME, BACK, flèches, OK, volume → jamais bloqués.
3. Le service lit l'état « enfant » dans un `SharedPreferences` léger (`profile_state`, clé `is_child`) écrit par
   `SettingsStore.switchProfile`, et un booléen `pinScreenOnTop` écrit par `PinGate` (affiché/caché). Comme
   `KeyBindingStore`, un broadcast `ACTION_PROFILE_CHANGED` invalide le cache.
4. Dans `onKeyEvent`, juste après `learn(event)` et avant HOME : `if (KidKeyFilter.blocks(...)) return true`.
5. Si le service est coupé en profil enfant : la notice existante de Matinee + le texte « Touches non protégées ».
6. Tests verts, build, essai : en Enfants, Netflix/CH+/chiffres sans effet ; en Paul, tout marche.
   Commit : `feat(profiles): block the remote's shortcuts for children`.

## Tâche 7 — Profils de départ (`family.properties`) · à faire juste après la tâche 2, avant d'installer celle-ci

Numérotée 7 pour garder les références, mais réalisée dans le même passage que la tâche 2 : sinon la migration crée
un profil unique au premier lancement et le seed n'a plus lieu.

**Fichiers** : `.gitignore`, `app/build.gradle.kts`, créer `data/FamilySeed.kt`, test `FamilySeedTest.kt`,
modifier `SettingsStore.ensureProfiles`.

1. `.gitignore` : ajouter `family.properties` et `secrets.properties`. Vérifier avec `git check-ignore`.
2. Test d'abord : `FamilySeed.parse(lines)` → 4 profils pour l'exemple de la spec ; ligne mal formée ignorée ;
   `child` sans applis → profil enfant vide ; texte vide → liste vide.
3. `build.gradle.kts` : lire `family.properties` s'il existe, joindre les lignes `profile.N` dans
   `buildConfigField("String", "FAMILY", …)` (échappé), sinon `""`.
4. `ensureProfiles()` : aucun profil → `FamilySeed.parse(BuildConfig.FAMILY)` ; adultes = snapshot de l'accueil
   actuel ; enfants = snapshot réduit (tâche 5) ; premier adulte actif. Seed vide → migration (tâche 2).
5. Écrire `family.properties` avec la famille, installer, vérifier les 4 profils (Paul porte l'accueil actuel).
   Commit : `feat(profiles): seed the family's profiles at build time`.

## Tâche 8 — Mode « Extrême »

**Fichiers** : `data/DeviceProfile.kt`, `data/SettingsStore.kt`, `ui/settings/SettingsScreen.kt`, chaînes, et les
endroits qui lisent `glassEffects` / `wallpaperAnimated` / `reduceMotion` / l'image de fond.

1. `PerformanceTier.EXTREME` (choix manuel seulement, jamais détecté). Le choisir écrit `glassEffects=false`,
   `wallpaperAnimated=false`, `adaptiveBackground=false`, `reduceMotion=true` ; `WallpaperBackground` et
   `CinematicBackground` ne dessinent rien en Extrême.
2. Build, essai sur le POP (fluidité en allant d'une appli à l'autre). Commit : `feat(performance): add an extreme tier`.

## Tâche 9 — Suggestions selon l'appli sélectionnée

**Fichiers** : `.gitignore` (fait en 7), `build.gradle.kts` (`secrets.properties` → `BuildConfig.TMDB_KEY`,
`YOUTUBE_KEY`), créer `data/Picks.kt`, `ui/home/PicksPanel.kt` ; modifier `LauncherViewModel.kt`, `GridHero.kt`,
`HomeScreen.kt` ; le profil gagne `youtubeQuery` (codec + fiche profil) ; tests `PicksTest.kt`.

1. Test d'abord : `Picks.providerFor(packageName)` (Netflix 8, Disney+ 337, Prime Video 119, Canal+ 381,
   Apple TV+ 350, Paramount+ 531, Max 1899) ; `Picks.parseTmdb(json)` et `Picks.parseYouTube(json)` sur des
   réponses enregistrées dans `test/resources` ; cache : frais < 6 h, sinon rechargé.
2. `Picks` : `HttpURLConnection` comme `WeatherClient`, résultats et affiches (w342) en cache disque ; clé vide →
   aucune requête.
3. `LauncherViewModel` : `picksFor(focusedPackage)` (StateFlow, rien en profil enfant).
4. `PicksPanel` : 5 à 8 affiches ; grille → dans `GridHero` à la place de la reprise ; rangée → panneau de droite.
   Reprise de la même appli en premier. OK sur une affiche → lancer l'appli ; vidéo YouTube →
   `https://www.youtube.com/watch?v=<id>` avec le paquet YouTube.
5. Tests verts, build, essai avec les vraies clés. Commit : `feat(home): suggest what's popular on the focused app`.

## Tâche 10 — Économiseur « En cours de lecture »

**Fichiers** : créer `system/MatineeDream.kt`, `ui/dream/NowPlaying.kt`, `ui/dream/PosterSaver.kt`,
`data/SpotifyClient.kt`, `tools/spotify-login.ps1` ; manifeste (`<service … BIND_DREAM_SERVICE>`) ; le profil gagne
`saver` (POSTERS ou CLOCK) ; tests `SpotifyClientTest.kt`.

1. Test d'abord : `SpotifyClient.parseCurrentlyPlaying(json)` (titre, artistes, pochette, position, durée ; 204 →
   rien) ; `SpotifyClient.parseToken(json)` garde le nouveau `refresh_token` quand il y en a un.
2. `tools/spotify-login.ps1` : PKCE dans le navigateur, petit serveur sur 127.0.0.1:8888, écrit
   `spotify_refresh_token` dans `secrets.properties` sans l'afficher. Vous le lancez vous-même.
3. `SpotifyClient` : jeton du build au premier usage, puis le plus récent en `SharedPreferences` privé.
4. `MatineeDream` (Compose dans un `DreamService`) : session média de la TV en cours → `NowPlaying` ; sinon, Spotify
   (5 s) → `NowPlaying` ; sinon le `saver` du profil actif. Respecte Extrême (pas d'animation).
5. Choisir l'économiseur par adb (commande de la spec), essai : musique sur la TV, sur le téléphone, rien.
   Commit : `feat(saver): add a now playing screensaver`.

## Tâche 11 — README et remise sur le Player

1. README du fork : section « Freebox Player POP » : installer, rendre Matinee accueil, désactiver/réactiver
   l'accueil Free et les applis retirées (commandes de la spec), `family.properties` et `secrets.properties`,
   connexion Spotify, choisir l'économiseur, remettre le code parent à zéro (commande adb qui efface seulement les
   clés `pin_*`), boutons non bloquables relevés en tâche 6.
2. Installer la version finale, dérouler la liste d'essais de la spec.
   Commit : `docs: set up Matinee on a Freebox Player POP`.

## Ce qui n'est pas dans ce plan

Pousser la branche sur GitHub et ouvrir une PR : seulement si vous le demandez.
