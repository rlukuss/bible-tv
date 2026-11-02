# Installer Bible TV avec un agent IA

Ce fichier s'adresse à l'agent IA (Claude Code, Codex, Cursor…) à qui l'on confie ce dépôt avec une phrase du type :

> « Installe Bible TV sur ma TV, son adresse IP est 192.168.1.50. »

Suis les étapes dans l'ordre. Ce qui doit se faire **sur la TV, avec la télécommande**, c'est la personne qui le fait : dis-lui exactement quoi faire, puis attends sa réponse.

## Appareils pris en charge

| Appareil | Pris en charge | Remarque |
|---|---|---|
| Android TV, Google TV (Sony, Philips, TCL, Hisense Android, Chromecast avec Google TV, Nvidia Shield…) | Oui | Android 8 ou plus récent. |
| Freebox Player POP (Android TV 10) | Oui | Ajouter `--disable-free-launcher` pour couper l'accueil OQEE de Free. |
| TV LG (webOS) | Pas directement | webOS n'installe pas d'applis Android. Brancher sur la TV LG un boîtier Android TV ou Google TV (par exemple Chromecast avec Google TV), puis installer sur ce boîtier. |
| Fire TV (Amazon) | Non testé | Android en dessous, mais l'accueil d'Amazon ne se remplace pas. |

Si la personne parle d'une TV LG, explique-le avant toute autre étape.

## 1. Ce que tu dois demander

- **L'adresse IP de la TV.** Sur la TV : Paramètres → Réseau et Internet → le réseau connecté. Ou dans l'interface de la box.
- **Les profils de la famille** : un prénom pour chacun, adulte ou enfant. Pour un enfant, quelles applis autoriser.
- **La ville pour la météo.**

Ne demande jamais le code parental. La personne le règle elle-même sur la TV (étape 6).

## 2. Préparer la TV (la personne le fait)

1. Paramètres → Préférences de l'appareil → À propos → appuyer **7 fois** sur « Build » (ou « Version du logiciel ») pour activer les options pour les développeurs.
2. Paramètres → Préférences de l'appareil → **Options pour les développeurs** → activer **Débogage USB** et, s'il existe, **Débogage réseau / ADB sur le réseau**.
   Sur la Freebox Player POP : Paramètres → Préférences de l'appareil → Options pour les développeurs → **Débogage ADB**.
3. Au premier branchement, la TV demande d'autoriser l'ordinateur : choisir **Autoriser** (et « Toujours autoriser »).

## 3. Préparer l'ordinateur

Il faut **JDK 17** et le **SDK Android** avec :

- `platform-tools` (adb), `platforms;android-35`, `build-tools;35.0.0`
- `ndk;29.0.14206865` et `cmake;3.31.6` : le récepteur AirPlay est en C.

Avec `sdkmanager` :

```bash
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0" "ndk;29.0.14206865" "cmake;3.31.6"
```

Crée `local.properties` à la racine avec `sdk.dir=<chemin du SDK>` si `ANDROID_HOME` n'est pas défini. Demande avant chaque téléchargement important : le SDK et le NDK font plusieurs Go.

## 4. Écrire `family.properties`

À la racine du dépôt. Ce fichier est ignoré par git et ne doit **jamais** être commité.

```properties
profile.1=Paul|adult
profile.2=Marie|adult
profile.3=Enfants|child|com.google.android.youtube.tvkids,com.disney.disneyplus
city=Lyon|45.764|4.8357
```

- Pour trouver les noms de paquet des applis enfants : `adb -s <IP>:5555 shell pm list packages`.
- Pour les coordonnées de la ville, utilise le géocodage d'Open-Meteo : `https://geocoding-api.open-meteo.com/v1/search?name=Lyon&language=fr`.
- Sans `family.properties`, l'appli démarre avec un seul profil, et la météo est sur Paris. Les profils se gèrent ensuite dans Paramètres → Écran d'accueil → Profils.

## 5. Installer

```bash
tools/install.sh <IP de la TV>
```

Sur une Freebox Player POP :

```bash
tools/install.sh <IP de la TV> --disable-free-launcher
```

Le script se connecte à la TV, compile, installe, fait de Bible TV l'écran d'accueil et l'ouvre. Sous Windows, lance-le depuis Git Bash.

## 6. Finir sur la TV (la personne le fait)

1. **Code parental** : Paramètres de Bible TV → Contrôle parental. Il protège le profil Enfants et la gestion des profils.
2. **Bouton Accueil** (recommandé, indispensable sur Google TV) : Paramètres → Écran d'accueil → « Le bouton Accueil ouvre Matinee », puis activer le service dans l'écran d'accessibilité qui s'ouvre. Il bloque aussi les touches de la télécommande dans le profil Enfants.
3. **Réactivation automatique** (conseillé) : Paramètres → Bluetooth et télécommande → ADB automatique. Bible TV peut alors rallumer le service du bouton Accueil après une mise à jour.
4. **Accès aux notifications** : Paramètres → Écran d'accueil → Accès aux notifications. Il sert à la reprise de lecture.

## Règles pour l'agent

- Ne tape jamais de mot de passe ni de code PIN sur la TV, et ne les écris dans aucun fichier.
- Ne commite jamais `family.properties`, `secrets.properties`, ni aucune clé.
- **Ne désinstalle jamais** une appli système. Pour la cacher, utilise `pm disable-user --user 0 <paquet>`, et `pm enable <paquet>` pour la remettre.
- Les réglages d'accessibilité et de sécurité se font par la personne, sur la TV.
- Si la personne utilise la TV en même temps que toi, arrête d'envoyer des touches.

## Revenir en arrière

- Freebox : `tools/restore-free-launcher.sh <IP>` remet l'accueil de Free.
- Toute TV : `adb -s <IP>:5555 uninstall com.yigitcan.matinee`, puis choisir l'accueil d'origine quand la TV le demande.
