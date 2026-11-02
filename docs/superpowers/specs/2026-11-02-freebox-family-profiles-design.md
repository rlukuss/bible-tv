# Profils familiaux pour le Freebox Player POP — design

Date : 2026-11-02 · Branche : `feat/freebox-family-profiles` · Base : Matinee 1.6.9 (fork `rlukuss/matinee`)

## Objectif

Faire de Matinee l'écran d'accueil du Freebox Player POP de la famille, à la place de l'accueil Free (OQEE), avec un
profil par personne :

| Profil | Type | Au choix du profil | Applis |
|---|---|---|---|
| Paul | adulte | son accueil Matinee | toutes |
| Marie | adulte | son accueil Matinee | toutes |
| Léa | adulte | son accueil Matinee | toutes |
| Enfants | enfant | accueil Enfants | YouTube Kids, Tfou Max, Disney+ seulement, pas de TV en direct |

Le Player : Freebox Player POP, Android 10 (API 29), Amlogic 32 bits, 1920×1080. Matinee 1.6.9 tel quel y tourne
(≈ 58 Mo, sans plantage), en français, et y trouve déjà « TV en direct ».

## Hors périmètre

- Supprimer l'accueil Free : il est **désactivé** (`pm disable-user`), jamais désinstallé.
- Limiter les chaînes dans OQEE, l'historique de lecture par profil.
- Un vrai contrôle parental : c'est un garde-fou (voir « Limites »).
- Remonter ces changements à l'auteur de Matinee.

## Ce qui existe déjà dans Matinee et qu'on réutilise

- `data/Parental.kt`, `ParentalGate.kt`, `ui/Pin.kt` : code parent à 4 chiffres (haché, salé), blocage après 5
  erreurs, réglages et édition de l'accueil protégés par le code.
- `system/HomeKeyService.kt` : service d'accessibilité qui filtre les touches (`flagRequestFilterKeyEvents`) et
  ramène sur Matinee avec Accueil ; `data/KeyBindings.kt` sait associer « Ne rien faire » à une touche.
- `data/LiveTv.kt` : la tuile « TV en direct ».
- `data/SettingsStore.kt` (DataStore) : réglages, dont `profileName` et `hiddenApps`, aujourd'hui pour un seul profil.

## Ce qu'on ajoute

### 1. Profils — `data/Profiles.kt`

```kotlin
enum class ProfileKind { ADULT, CHILD }

data class Profile(
    val id: String,                 // UUID
    val name: String,
    val kind: ProfileKind,
    val color: Int,                 // couleur du rond (ARGB), choisie dans une liste fixe de 8 couleurs
    val appOrder: List<String>,     // ordre des applis sur l'accueil
    val hiddenApps: Set<String>,    // adultes : applis masquées
    val allowedApps: Set<String>,   // enfants : les seules applis visibles et lançables
)
```

- Stockés dans le DataStore de `SettingsStore`, une clé `profiles` (JSON) et une clé `active_profile_id`.
- `ProfilesCodec` (JSON ↔ liste) est séparé du stockage pour être testé sans Android.
- **Migration** : au premier lancement de la nouvelle version, s'il n'y a aucun profil, on crée un profil adulte à
  partir des réglages actuels (`profileName`, ordre des applis, `hiddenApps`). Rien n'est perdu.
- **Profils de départ** : lus au build dans `family.properties` (racine du dépôt, **exclu de git** : le fork est
  public, les prénoms restent sur le PC) et créés au premier lancement s'il n'y a encore aucun profil. Les adultes
  reprennent l'accueil actuel (migration ci-dessous), Enfants reçoit son accueil réduit. Fichier de la famille :
  ```properties
  profile.1=Paul|adult
  profile.2=Marie|adult
  profile.3=Léa|adult
  profile.4=Enfants|child|com.google.android.youtube.tvkids,fr.tfou.max,com.disney.disneyplus
  ```
  (`prénom|adult ou child|applis autorisées, enfants seulement`). Sans fichier : la migration seule.
- Les profils se modifient ensuite sur la TV : Réglages → Profils (protégé par le code parent).
- Un profil Enfants exige qu'un code parent existe : sinon, sa création propose d'abord d'en choisir un.

### 2. « Qui regarde ? » — `ui/WhoScreen.kt`

- Une rangée de ronds (couleur ou photo + prénom), navigation aux flèches, OK pour choisir.
- **Quand il s'affiche** (`WhoPolicy`, logique pure testée) :
  - au premier affichage de Matinee après le démarrage du Player ;
  - au réveil après une mise en veille (écran éteint puis rallumé), **si le dernier profil était adulte** ;
  - depuis la pastille de profil en haut à droite (« Changer de profil »).
- **Si le dernier profil était Enfants**, au réveil on reste sur Enfants : en sortir demande le code parent.
- Le simple économiseur d'écran (sans mise en veille) ne ramène pas « Qui regarde ? ».
- Option « Code pour les profils adultes » (désactivée par défaut) : demande le code parent pour choisir un profil
  adulte depuis « Qui regarde ? ».
- **Créer, modifier ou supprimer un profil demande toujours le code parent**, à chaque fois, quels que soient les
  autres réglages.
- Profil Enfants : ses quelques applis remplissent toute la largeur de la rangée (fait).

### 3. Profils adultes

- Après le choix, on arrive sur l'accueil Matinee du profil (la famille ne veut pas être envoyée sur OQEE).
- Accueil ramène sur l'accueil Matinee du profil, comme aujourd'hui, avec ses applis et son ordre.

### 4. Profil Enfants

- Accueil réduit : seules les applis de `allowedApps` (YouTube Kids, Tfou Max, Disney+), pas de tuile
  « TV en direct », pas de « toutes les applis », pas de bouton d'ajout.
- Le code parent est demandé pour : changer de profil, ouvrir les réglages, modifier l'accueil
  (via `ParentalGate.requireForAdmin`, avec la protection forcée dans ce profil).
- **Touches bloquées** (`KidKeyFilter`, logique pure testée), dans `HomeKeyService`, seulement dans ce profil :
  - les boutons raccourcis Free (Netflix, Prime, OQEE…), dont les codes seront relevés sur le Player avec la
    fonction « apprendre une touche » de Matinee ;
  - `KEYCODE_CHANNEL_UP`, `KEYCODE_CHANNEL_DOWN`, `KEYCODE_0` à `KEYCODE_9`, `KEYCODE_ASSIST`, `KEYCODE_SEARCH`.
  - Exception : les chiffres passent quand l'écran de code de Matinee est au premier plan.

### 5. Remplacer l'accueil Free

Depuis le PC, une fois l'appli installée et le service d'accessibilité activé :

```bash
adb shell cmd package set-home-activity com.yigitcan.matinee/com.tcltv.sahne.MainActivity
adb shell pm disable-user --user 0 net.oqee.launcher
```

Pour revenir en arrière (noté dans le README du fork) :

```bash
adb shell pm enable net.oqee.launcher
adb shell cmd package set-home-activity net.oqee.launcher/net.oqee.tvlauncher.ui.home.HomeActivity
```

On ne touche jamais à `net.oqee.androidtv` (TV en direct), aux paquets `fr.freebox.*`, `org.dtvkit.inputsource`
ni `com.nes.*`.

### 6. Météo

Ville par défaut : Paris, 48.8566, 2.3522 (ou la ville d'une ligne `city=` de family.properties) — fait (commit `feat(weather): default to Paris`).

### 7. Repris de free-my-tv-family

#### 7a. Mode performance « Extrême »

Un 4ᵉ niveau `PerformanceTier.EXTREME` (Paramètres → Appareil → Profil de performances), pour tout le Player : coupe
fond animé, verre, lumière, animations (`reduceMotion`) et images de fond. Les affiches (7b) restent, en petit.

#### 7b. Films, séries et vidéos selon l'appli sélectionnée

- Appli sélectionnée Netflix, Disney+, Prime Video, Canal+, Apple TV+, Paramount+ ou Max (si installée) : 5 à 8
  affiches des titres populaires sur ce service **en France** (TMDB `discover` avec `with_watch_providers` et
  `watch_region=FR`), jamais sponsorisés.
- Appli sélectionnée YouTube : vidéos du **thème YouTube du profil** (nouveau champ du profil, YouTube Data API v3),
  ouvertes directement dans YouTube.
- Place : grille → le bandeau du haut (à la place de la reprise) ; rangée → le panneau de droite. Si un titre est en
  cours dans cette appli, la reprise passe d'abord.
- Ouvrir une affiche ouvre l'appli (pas le titre : les liens directs ne sont pas fiables).
- Profil Enfants : pas de suggestions. Sans clé : la fonction est absente. Cache disque de 6 h.

#### 7c. Économiseur d'écran « En cours de lecture »

- Nouveau `DreamService` (`system/MatineeDream.kt`), choisi par adb :
  `settings put secure screensaver_components com.yigitcan.matinee/com.tcltv.sahne.system.MatineeDream`.
- Musique en cours → écran façon CarPlay : pochette, titre, artiste, progression.
  - Spotify sur la TV : les sessions média que Matinee lit déjà (`MediaHistoryRecorder`).
  - Spotify sur un téléphone : API Spotify `me/player/currently-playing`, toutes les 5 s pendant l'économiseur.
- Sinon, selon le profil : affiches de films qui défilent (TMDB) ou horloge fine.
- Connexion Spotify : une fois, sur le PC, avec `tools/spotify-login.ps1` (PKCE, Redirect URI
  `http://127.0.0.1:8888/callback`), qui écrit `spotify_refresh_token` dans `secrets.properties`. Un seul compte.
  Spotify change le jeton à chaque usage : Matinee garde le plus récent sur le Player et n'utilise celui du build
  qu'au premier lancement.

#### 7d. Les clés

`secrets.properties` (racine, **exclu de git**) : `tmdb_key`, `youtube_key`, `spotify_client_id`,
`spotify_refresh_token`, lus au build dans `BuildConfig`. Jamais affichés ni commités ; c'est vous qui les collez.

## Applis retirées du Player (fait le 2026-11-02)

- Désinstallées : Google Meet, Max, Molotov, Pluto TV, Google TV (films), Free Aktu, Okoo, Free My TV.
- Désactivées (`pm enable <paquet>` pour revenir) : Play Jeux (`com.google.android.play.games`), RMC BFM Play
  (`com.nextinteractive.rmcbfmplay.tv.free`), france.tv (`fr.francetv.pluzz`).

## En cas de problème

| Situation | Ce qui se passe |
|---|---|
| Matinee plante | Android relance l'accueil par défaut, Matinee. S'il plante en boucle : la commande « revenir en arrière » ci-dessus, depuis le PC |
| Une appli enfant est désinstallée | Sa tuile disparaît, les autres restent |
| Code parent oublié | Remise à zéro du code seul depuis le PC (commande adb notée dans le README), les profils restent |
| Le service d'accessibilité est coupé (mise à jour Free…) | Matinee le signale déjà (« prompt when the TV turns it off ») ; le profil Enfants affiche en plus « Touches non protégées » |

## Limites (à dire à la famille)

- Si les boutons raccourcis Free sont traités par le système avant les services d'accessibilité, Matinee ne pourra pas
  les bloquer. On le vérifiera en premier sur le Player ; si c'est le cas, on le notera dans le README.
- Depuis « Qui regarde ? », un enfant peut choisir un profil adulte, sauf si l'option « Code pour les profils
  adultes » est activée.
- Les réglages Android restent accessibles depuis une appli ouverte (ex. menu d'une appli).

## Tests

- **Unitaires** (`app/src/test`, comme `InputCatalogTest`) :
  - `ProfilesCodecTest` : aller-retour JSON, champs manquants, JSON invalide → liste vide ;
  - `ProfilesMigrationTest` : réglages d'un seul profil → un profil adulte équivalent ;
  - `ChildAccessTest` : une appli est visible et lançable seulement si elle est dans `allowedApps` ;
  - `KidKeyFilterTest` : touches bloquées en profil Enfants, laissées en adulte, chiffres permis sur l'écran de code ;
  - `WhoPolicyTest` : démarrage, réveil adulte, réveil enfant, économiseur d'écran.
- **Sur le Player**, en suivant `adb logcat` :
  1. démarrage → « Qui regarde ? » ;
  2. Paul → accueil de Paul ;
  3. Enfants → 3 tuiles ; boutons Netflix, CH+, chiffres sans effet ;
  4. sortir du profil Enfants : code faux 5 fois → blocage ;
  5. veille puis réveil : adulte → « Qui regarde ? », enfant → reste sur Enfants ;
  6. commande « revenir en arrière » → l'accueil Free revient.

## Organisation du code

- Un commit par partie : profils + migration, « Qui regarde ? », profil Enfants, touches bloquées, README.
- Les fichiers d'origine de Matinee sont modifiés le moins possible, pour récupérer facilement les mises à jour de
  l'auteur (`git fetch upstream`).
