# CLAUDE.md

Context for working on this repository with Claude Code (or any assistant). Read this first, then `docs/dev/`.

## What this is

**Cinematic Interactions: TACZ Addon** (`cinematic_tacz`): a Fabric 1.21.1 / 1.20.1, NeoForge 1.21.1 and Forge 1.20.1 add-on that gives TACZ's gun smith tables
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
./gradlew build                                  # every node: versions/<node>/build/libs/CinematicInteractions-TACZ-<Loader>-<mc>-0.1.0.jar
./gradlew :1.21.1-fabric:runClient               # dev game of a node: run/<node>/client
./gradlew :1.21.1-fabric:runServer               # dev server: run/<node>/server (EULA accepted by the owner)
```

- `libs/Cinematic Interaction-…-0.18.30.jar`, one per node (`cinematic_jar` in `versions/<node>/gradle.properties`),
  come from Cinematic Interactions at the commit pinned in the release workflow: `./gradlew :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:build -x test
  -Porg.gradle.java.installations.paths=<../.jdk/jdk-17…>,<../.jdk/jdk-21…> -Porg.gradle.java.installations.auto-download=false`
  (its 1.20.1 targets need a JDK 17: `../.jdk/jdk-17.0.21+9`, next to the 21). Our 1.20.1 node compiles with 21 at release 17.
- In-game checks: the owner presses **F2** (screenshots in `run/<node>/client/screenshots/`) and the log is
  `run/<node>/client/logs/latest.log`. Lines `[Cinematic] ADDON at …: no scene (…)` explain why a scene fell back.
- Stonecutter (`settings.gradle`, `stonecutter.gradle`): nodes `<mc>-<loader>` as Cinematic Interactions' targets,
  `1.21.1-fabric` (the code in `src/`, `vcsVersion`), `1.20.1-fabric`, `1.21.1-neoforge` and `1.20.1-forge`.
  `build.<loader>.gradle` (Loom for Fabric, ModDevGradle for NeoForge, its legacy plugin for Forge) builds each in `versions/<node>/` with that folder's `gradle.properties`
  (Java, Parchment, loader/API versions, host JARs). Loader files live in `src/<loader>/resources` (`fabric.mod.json`,
  each loader's `cinematic_tacz.mixins.json`) and `src/<neoforge|forge>/templates` (`neoforge.mods.toml`, `mods.toml`); loader-only
  classes (`KeyMappingMixin`, `ModMenuIntegration`; `GunSmithTableMenuDataMixin`; `ServerPlayerMixin`) are wrapped
  whole in `//? if <loader> {`.
- The Forge node runs on Java 17: pass `-Porg.gradle.java.installations.paths=$PWD/../.jdk/jdk-17.0.21+9` (otherwise
  the foojay plugin in `settings.gradle` downloads one into `~/.gradle/jdks`). The host JARs are remapped from SRG
  (`modImplementation`, through a `flatDir` of `libs/`); Forge finds TACZ's jar-in-jar libraries itself, in dev too. Code that differs
  goes behind `//?` comments (`//? if >=1.21 {`, loader constants `fabric`/`neoforge`/`forge`) or a rename in
  `stonecutter.gradle`'s `replacements`. Each node has its own dev games, `run/<node>/client|server`.
- Releases: `.github/workflows/release.yml` builds on push to `nightly` (rolling pre-release `nightly`) and `main`
  (release `v<mod_version>`, skipped if it exists). It builds Cinematic Interactions from the commit pinned there: keep
  it in step with `cinematic_jar`. It is pinned to the fork
  `chomusuke-png/CinematicInteractions` (e1d9ff1), where the 1.20.1-forge target builds on GitHub's runners.
- Feeding a script to `runServer` through stdin makes the server read the rest as console commands: run it alone.

More: [docs/dev/architecture.md](docs/dev/architecture.md), [docs/dev/roadmap.md](docs/dev/roadmap.md),
[docs/tacz-map.md](docs/tacz-map.md) (Spanish mapping of both mods).
