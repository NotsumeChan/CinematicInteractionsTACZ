# CLAUDE.md

Context for working on this repository with Claude Code (or any assistant). Read this first, then `docs/dev/`.

## What this is

**Cinematic Interactions: TACZ Addon** (`cinematic_tacz`): a Fabric 1.21.1 add-on that gives TACZ's gun smith tables
first-person scenes through Cinematic Interactions' add-on API. Vibecoded with Claude Code; tested in game by the owner.

Sibling folders (same parent directory, `minecraft/`):

| Folder | What | Touch? |
|---|---|---|
| `CinematicTACZ/` | **this add-on** | yes |
| `CinematicInteractions/` | host mod (friend's repo, `dcvke/CinematicInteractions`); we build its `fabric/1.21.1` JAR | only build; API changes go to the friend as requests |
| `TACZ-Refabricated/` | source of the TACZ Fabric port (`Sh1roCu/TACZ-Refabricated`, branch `1.21.1`) | read only, for reference |
| `TACZ/` | original TACZ (Forge 1.20.1) | read only |
| `.jdk/` | local Temurin 21 (no system JDK) | |

## Rules from the owner

- **Never commit or push.** The owner commits. Branch flow: `dev` → `qa` → `nightly` → `main`; work happens on `dev`.
- Do not install anything system-wide; local tools go in `minecraft/.jdk` or the project.
- Allowed to modify files in the `minecraft/` folder (both base repos included), but changes to `CinematicInteractions`
  belong to its owner: propose them instead (see `docs/dev/roadmap.md`).
- README and `docs/compatibility` are in English; talk to the owner in Spanish.
- Mark deliberate shortcuts with a `ponytail:` comment (ceiling + upgrade path).

## Build, run, test

```sh
export JAVA_HOME=$PWD/../.jdk/current           # fish: set -x JAVA_HOME $PWD/../.jdk/current
./gradlew build                                  # build/libs/CinematicInteractions-TACZ-Fabric-1.21.1-0.1.0.jar
./gradlew runClient                              # dev game: run/client
./gradlew runServer                              # dev server: run/server (EULA accepted by the owner)
```

- `libs/cinematic-fabric-1.21.1-0.18.25.jar` comes from `../CinematicInteractions/fabric/1.21.1` (`sh gradlew remapJar`;
  it needs `test-mods/polymorph/polymorph-fabric-1.1.0+1.21.1.jar`, downloaded from Modrinth, git-ignored there).
- In-game checks: the owner presses **F2** (screenshots in `run/client/screenshots/`) and the log is
  `run/client/logs/latest.log`. Lines `[Cinematic] ADDON at …: no scene (…)` explain why a scene fell back.
- Feeding a script to `runServer` through stdin makes the server read the rest as console commands: run it alone.

More: [docs/dev/architecture.md](docs/dev/architecture.md), [docs/dev/roadmap.md](docs/dev/roadmap.md),
[docs/tacz-map.md](docs/tacz-map.md) (Spanish mapping of both mods).
