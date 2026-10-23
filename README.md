# Domino Score

A small Android app for keeping score in domino games.

## Features

- 2–6 players or teams
- Two ways to win:
  - **First to a target score** – highest score wins once someone reaches the target
    (presets 61 / 100 / 150 / 200 / 250, or any number). Good for Block, Draw, All Fives.
  - **Lowest score after N rounds** – e.g. Mexican Train (13 rounds).
- Add a round after each hand; tap any round to fix or delete it; undo the last round
- Leader highlighted, progress toward the target, winner banner with one-tap rematch
  (a tie for first place means another round is played)
- Game is saved automatically – close the app and pick up where you left off
- Past games list with a win count per player
- Light/dark theme, Material You colors on Android 12+

## Install

Every push builds an APK on GitHub Actions: open the latest **Build APK** run under the
*Actions* tab and download the `domino-score-apk` artifact, unzip it and open
`app-release.apk` on your phone (allow installing from unknown sources).

## Build locally

Requires JDK 17+ and the Android SDK (API 35).

```sh
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk
```

The release build is signed with the debug key so it installs without extra setup.
Use your own signing key before publishing to the Play Store.
