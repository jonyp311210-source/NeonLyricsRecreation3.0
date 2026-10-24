# SkyLyrics (stage 1)

Fabric, Minecraft 1.21.11. Client-only.

## Build
1. Open https://fabricmc.net/develop, pick 1.21.11, and copy the exact versions into gradle.properties.
2. Add the Gradle wrapper: run `gradle wrapper` once (or copy gradlew + gradle/ from the Fabric template).
3. `./gradlew build` -> jar in build/libs/. Or `./gradlew runClient` to test.

## What works
- Now-playing card (art placeholder, title, artist, progress, spectrum) with a demo track
- J opens a tabbed settings menu (Lyrics / Display / Player); settings save to config/skylyrics.json

## Not built yet
- Real music source (Spotify login or OS media session)
- Synced lyrics and the 3D sky lyrics renderer
- Custom skies, hats/cosmetics, death animations, saved lines

## Build with no install (GitHub)
1. Create a free GitHub repo and upload everything in this folder (keep the .github folder).
2. Open the Actions tab, wait for the "build" run to finish, open it, download "skylyrics-jar".
3. Put the jar plus Fabric API in your mods folder.
