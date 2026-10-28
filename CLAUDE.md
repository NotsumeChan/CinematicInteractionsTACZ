# CLAUDE.md

Context for working on this repository with Claude Code (or any assistant). Read this first, then `docs/dev/`.

## What this is

**Cinematic Interactions: TACZ Addon** (`cinematic_tacz`): a Fabric 1.21.1 / 1.20.1, NeoForge 1.21.1 and Forge 1.20.1 add-on that gives TACZ's gun smith tables
first-person scenes through Cinematic Interactions' add-on API. Vibecoded with Claude Code; tested in game by the owner.

Sibling folders (same parent directory, `minecraft/`):

| Folder | What | Touch? |
|---|---|---|
| `CinematicTACZ/` | **this add-on** | yes |
| `CinematicInteractions/` | host mod (friend's repo, `dcvke/CinematicInteractions`; `origin` is the fork `chomusuke-png/CinematicInteractions`); we build its four targets' JARs | only build; API changes go to the friend as requests |
| `TACZ-Refabricated/` | source of the TACZ Fabric port (`Sh1roCu/TACZ-Refabricated`, branch `1.21.1`) | read only, for reference |
| `TACZ/` | original TACZ (Forge 1.20.1) | read only |
| `.jdk/` | local Temurin 21 (`current`) and 17 (no system JDK) | |
| `CinematicKaleidoscope/` | the next add-on (Kaleidoscope Cookery), planned from this one | its own `CLAUDE.md` |
| `KaleidoscopeCookery/` | Kaleidoscope Cookery source | read only |

State: **1.0.0**, released from `main` for all four nodes; the owner does not expect to change much more. A fifth node, **Fabric 26.1.2**, builds and was tested in game by the owner (2026-10-28).

### Fabric 26.1.2

- TACZ: the unofficial TaCZ Refabricated port by q14433686-arch (`../TACZ-Refabricated-26`, branch `26.1.2`), only on
  its GitHub releases: `tacz_jar`/`tacz_url` in `versions/26.1.2-fabric/gradle.properties`, the release workflow
  downloads it. Cinematic Interactions: `multiversion/` project, `./gradlew :26.1.2:build` with a JDK 25
  (`../.jdk/jdk-25.0.5+8`; pass it in `-Porg.gradle.java.installations.paths` for our node too).
- Differences: renames in `stonecutter.gradle` (`Identifier`, `GuiGraphicsExtractor`, `SceneBuffers` …; Stonecutter
  merges every rule both ways, so a rule never repeats another's source), recipes from TACZ's own synced
  `CommonAssetsManager` (26.x clients have no recipe manager), the table renderer hooked on `submit` (TACZ captures the
  model there at once), `KeyMappingMixin` left out (`mixins_exclude`: vanilla gives a key to every mapping on it),
  `AfterCraft` swaps a gun outside the hotbar once the table menu is closed (no pick by slot), and the stations open
  the menu from the root block (`Tacz.openMenu`, the definition's opener): from 26.1 only the root of a two-block
  table has a block entity, so using the other half got no menu and the server closed the session.

## Rules from the owner

- **Never commit or push.** The owner commits. Branch flow: `dev` → `qa` → `main`; work happens on `dev`.
- Do not install anything system-wide; local tools go in `minecraft/.jdk` or the project.
- Allowed to modify files in the `minecraft/` folder (both base repos included), but changes to `CinematicInteractions`
  belong to its owner: propose them instead (see `docs/dev/roadmap.md`).
- README and `docs/compatibility` are in English; talk to the owner in Spanish.
- Mark deliberate shortcuts with a `ponytail:` comment (ceiling + upgrade path).

## Build, run, test

```sh
export JAVA_HOME=$PWD/../.jdk/current           # fish: set -x JAVA_HOME $PWD/../.jdk/current
./gradlew build                                  # every node: versions/<node>/build/libs/CinematicInteractions-TACZ-<Loader>-<mc>-<mod_version>.jar
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
  `1.21.1-fabric` (the code in `src/`, `vcsVersion`), `1.20.1-fabric`, `1.21.1-neoforge`, `1.20.1-forge` and
  `26.1.2-fabric` (Java 25, unobfuscated: `build.fabric.gradle` applies the plain Loom without mappings and plain
  configurations instead of `mod*`; its TACZ and Cinematic Interactions JARs are in `libs/`, see "Fabric 26.1.2" below).
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
- Releases: `.github/workflows/release.yml` builds on push to `main` (release `v<mod_version>`,
  skipped if it exists; bump `mod_version` to publish). It builds Cinematic Interactions from the commit pinned there: keep
  it in step with `cinematic_jar`. It is pinned to the fork
  `chomusuke-png/CinematicInteractions` (e1d9ff1), where the 1.20.1-forge target builds on GitHub's runners.
- Feeding a script to `runServer` through stdin makes the server read the rest as console commands: run it alone.

More: [docs/dev/architecture.md](docs/dev/architecture.md), [docs/dev/roadmap.md](docs/dev/roadmap.md),
[docs/tacz-map.md](docs/tacz-map.md) (Spanish mapping of both mods).
