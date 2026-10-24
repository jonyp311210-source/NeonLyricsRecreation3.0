# SkyLyrics (stage 1)

Fabric, Minecraft 1.21.11. Client-only.

## Build
1. Open https://fabricmc.net/develop, pick 1.21.11, and copy the exact versions into gradle.properties.
2. Add the Gradle wrapper: run `gradle wrapper` once (or copy gradlew + gradle/ from the Fabric template).
3. `./gradlew build` -> jar in build/libs/. Or `./gradlew runClient` to test.

## What works (stage 2)
- Reads the song playing in Windows (Spotify etc.) - Windows only, no login needed
- Now-playing card with real title, artist and progress; choose a corner in the Display tab
- Synced lyrics from LRCLIB shown on screen, with size and timing-offset controls
- J opens the menu; settings save to config/skylyrics.json

## Not built yet
- Album art, real audio spectrum
- Lyrics floating in the 3D sky
- Custom skies, hats/cosmetics, death animations, saved lines

## Build with no install (GitHub)
1. Create a free GitHub repo and upload everything in this folder (keep the .github folder).
2. Open the Actions tab, wait for the "build" run to finish, open it, download "skylyrics-jar".
3. Put the jar plus Fabric API in your mods folder.
