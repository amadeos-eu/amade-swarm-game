# amade-swarm-game

**amade-swarm-game** is the classic swarm from space on the [amadeOS](https://github.com/datazuul/amadeos) desktop: insects swoop in along curving paths, take their places in a formation and dive at your fighter at the bottom of the screen. Built on [amade-game-engine](https://github.com/datazuul/amade-game-engine); runs on other operating systems as well.

![License](https://img.shields.io/badge/license-EUPL--1.2-blue)
![Java](https://img.shields.io/badge/Java-25-orange)

## Playing

- **Enter**, **Space** or a **click** (or F2) starts a game.
- **Left** and **right** (or A, D) move the fighter, **Space** (or Ctrl, up) shoots.
- Or the **mouse**: the fighter follows the pointer, a **left click** shoots. A key takes over from the mouse until it moves again.
- Forty insects fly in each stage: **bees** (50 points in the formation, 100 in flight), **butterflies** (80 and 160) and **bosses** (150 and 400). A boss takes **two hits**; the first changes its colour.
- **Two shots** fly at a time. Insects that dive at the fighter shoot, and ram it if they can. A diver that misses flies out at the bottom and comes back in from the top.
- Every third stage is a **challenge**: the insects only fly past, nobody shoots, and hitting all forty brings a bonus of 10,000 points.
- Three fighters, an **extra fighter** at 20,000 points, then every 60,000. A lost fighter's successor waits until every diver is back in the formation. Each stage is a little faster than the one before.
- **P** or **Esc** pauses, and so does leaving the window.
- **Best scores**, ten of them. On the title screen the computer plays.
- **F11** switches to full screen and back. *Game → Graphics* chooses how the field reaches the screen: **Direct** (at the screen's full size) or **Pixels** (the field at 640 × 400, enlarged by whole factors). The insects are pixel art and keep their hard edges in both. **F12** shows how long each frame takes.
- Sound effects and music can be switched off under *Game → Sound*.

The keys can be changed in `~/.config/amadeos/swarm/swarm.properties`, with the names Java gives them, several separated by spaces:

```properties
keys.left=LEFT A
keys.right=RIGHT D
keys.fire=SPACE CONTROL UP
keys.pause=P ESCAPE
```

Texts in English, Deutsch, Français, Español and Italiano.

## Files

| What | Where |
|---|---|
| Settings (graphics, sound, keys) | `~/.config/amadeos/swarm/swarm.properties` |
| Name for high scores | `~/.config/amadeos/swarm/player.properties` |
| Best scores | `~/.local/share/amadeos/games/swarm/classic.scores` |

## Building

```bash
./gradlew test          # the window test needs a display (xvfb-run on servers)
./gradlew run
./gradlew deb           # build/distributions/amade-swarm-game_<version>_all.deb
./gradlew installToAmadeOs   # copies the package to ../amadeos/config/packages.chroot/
```

The insects, the fighter and the explosions are pixel art in one sprite sheet, `src/main/resources/eu/amadeos/games/swarm/sprites.png`, drawn by `tools/MakeSprites.java` from pictures written as text (see the comment at its top); it also turns each insect into its sixteen directions. The stages, with their flight paths, are in the text file `stages.txt` next to it: change them without compiling. The icon is drawn by `tools/MakeIcon.java` from the sheet. `./gradlew screenshots -Pshots=<folder>` draws the handbook's screenshots.

With `../amade-game-engine` (and `../amade-commons`) checked out next to this repository, Gradle builds those checkouts instead of the JitPack versions.

## Structure

- `domain`: the rules without user interface or time of their own. `Game` (the insects as entities and the systems that move them, the fighter, the pools of shots, the grid for collisions, lives, stages; chance from a seeded random generator; it reports `Event`s), the components `Body`, `Insect`, `Slot`, `Flight`, `Homing` and `Gun`, `Kind` (bee, butterfly, boss), `Formation` (where the slots are: swaying, then breathing), `Stages` (reads `stages.txt`), `Fighter`, `Shot`, `Steering` (what the player wants in a step), `Field` (sizes, speeds, points), `Pilot` (plays the title screen's demo), `GameResult`.
- `repository`: `SwarmSettings` (graphics, sound, keys).
- `ui.game`: the running game on the loop thread. `SwarmGame` (loop, controls and mouse, scenes: title, game, pause, end), `SwarmPainter` (stars, insects and fighter from the sprite sheet, shots, explosions, drawn between update steps), `Hud` (score, stage, fighters left, points, and the texts), `Effects` (events become explosions, sparks, sounds and music; the stars), `SoundBank`, `Music` (the tunes, in MML), `Arena` (what the presenter may ask of the game).
- `ui.main`: Model-View-Presenter for everything around the field: `SwarmPresenter`, `SwarmView`, `SwarmWindow`.

From the engine: `GameLoop`, `Entities` and `Entity`, `FlightPath`, `TextData`, `Grid`, `SpriteSheet` and `Animation`, `Vector` and `Angles`, `Pool`, `Circle`, `SeededRandom`, `GameCanvas`, `Particles`, `Sound`, `Tune` and `Mixer`, `Keyboard`, `Mouse` and `Controls`, `SceneStack`, `FullScreen`, high scores.

Comments of the form `// --8<-- [start:name]` mark the excerpts the [Game Developer's Handbook](https://datazuul.github.io/amade-game-engine/) quotes; chapter 24 is about this game.

## Licence

[EUPL-1.2](LICENSE)
