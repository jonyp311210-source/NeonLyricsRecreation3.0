# SkyLyrics

A beautiful Minecraft Fabric mod that shows synchronized lyrics as a modern floating karaoke bar (like the one in the screenshot), with sky-mode floating bubbles, word-by-word highlighting, and a clean settings menu.

## Features

- **Floating Karaoke Bar** – A sleek dark capsule that shows the current line with the active word highlighted in accent color (exactly like the picture you sent).
- **Word reveal modes**:
  - **Progressive** – words appear/highlight one after another.
  - **Whole line** – wait until the previous word finishes before showing the next full line.
- **Split Left / Right** – put current lyrics on one side of the screen and previous on the other.
- **Sky Mode** – lyrics float in the 3D world following your camera.
- **Follow Mode** – lyrics stay in front of you smoothly.
- **Customizable** position, size, opacity, fade previous lines, timing offset, etc.
- Works with any media player that exposes MediaSession (Spotify, YouTube Music, browser, etc. on Windows/Linux).

## Building

```bash
git clone https://github.com/yourname/skylyrics.git
cd skylyrics
./gradlew build
```

The built jar will be in `build/libs/skylyrics-0.6.0.jar`.

## Usage

- Press **J** (default) to open the settings menu.
- Make sure a song is playing in a supported media player.
- Lyrics are fetched automatically from public sources (LRCLIB etc.).

## Settings Overview

### Lyrics tab
- Show lyrics
- Lyrics mode: Follow / Sky / Screen (the floating bar)
- Split lyrics (Left / Right)
- Line position (Center / Left / Right)
- Vertical height
- Previous lines fade out
- **Word reveal style**: Progressive / Whole line
- Lyrics size & Opacity
- Timing offset

### Display tab
- Now-playing card
- Card position
- Song title intro

## License

MIT


## Lyrics display options

Open the SkyLyrics menu with **J** (or use the keybind settings) to adjust the display.
The Lyrics tab includes split left/right lyrics, left/center/right alignment, vertical
position, size, opacity, timing offset, word reveal style, and a toggle for whether
previous lines are dimmed. Split mode divides the active lyric into left and right
halves, each in its own bar. Previous-line visibility is controlled by "Show previous /
next"; "Previous lines fade out" controls whether the previous line is dim or brighter.
