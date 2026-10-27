<div align="center">

# 🎬🔫 Cinematic Interactions: TACZ Addon

**TACZ's gun smith tables in first person, the Cinematic Interactions way.**

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat-square)
![Loader](https://img.shields.io/badge/Loader-Fabric-DBD0B4?style=flat-square)
![Status](https://img.shields.io/badge/status-dev-orange?style=flat-square)
![Vibecoded](https://img.shields.io/badge/vibecoded-🤖%20Claude%20Code-8A63D2?style=flat-square)

</div>

---

An add-on that bridges two mods: instead of opening TACZ's window, you step up to the table, the camera leans over it,
and the guns, ammo and attachments lie right there on the bench. Pick a part, touch it again, and watch it come
together from its materials.

> [!IMPORTANT]
> **This add-on is unofficial** and not affiliated with the authors of Cinematic Interactions or TACZ. It only connects
> the two: **the ideas, design and credit of each mod belong to their authors** (see [Credits](#-credits)).

> [!NOTE]
> **Vibecoded project.** The code was written with AI assistance ([Claude Code](https://claude.com/claude-code)) and
> tested in game by [NotsumeChan](https://github.com/NotsumeChan). Review and test it before using it on a server or in
> a serious modpack.

## ✨ What it does

| Table | Scene |
|---|---|
| 🔫 **Gun smith table** (`gun_smith_table`) | Guns rest on the desk at a slant, like on a rack; rifles are drawn smaller than pistols so a page fits |
| 🧨 **Ammo workbench** (`ammo_workbench`) | Ammo on the free mat; the chosen one hovers above, its materials between your hands |
| 🔭 **Attachment workbench** (`attachment_workbench`) | A tall bench: attachments hang from its front, like a tool board |

- **TACZ's own recipes**: each table's tabs and filters from the gunpack are respected, and the server still validates
  and hands out every craft (the add-on sends TACZ's own craft packet).
- **Crafting animations**: at the gun smith table you take its own hammer and drill, forge and drill the part, and it
  rises out of the smoke, then the new gun is drawn and inspected; the ammo press strokes down, its lever in your hand,
  and the rounds tumble out; the attachment workbench is a resin printer that prints the part out of its vat.
- **Settings** (`config/cinematic_tacz-client.json` and Mod Menu's button on Fabric, the mods list's Config button on NeoForge and Forge): each table's scene, the animations, the
  draw and inspect after forging, the particles.
- **Material counter** `have/need` in green or red; parts you can't craft yet are see-through.
- **Scene only**: TACZ's window never opens outside the scene (from afar, sneaking, or with TACZ's interact key); a
  notice asks you to step closer.
- **Shoot and aim with the usual mouse buttons** on Fabric (see [Technical notes](#-technical-notes)).
- **Spanish for every variant**: playing in `es_cl`, `es_mx`, `es_ar`…, anything without its own translation falls back
  to `es_es` instead of English (TACZ and its gunpacks only ship `es_es`).

### 🎮 Scene controls

| Action | Control |
|---|---|
| Choose a part | Left click |
| Craft the chosen part | Left click again |
| Next category | Right click |
| Change page | Mouse wheel |
| Leave | As in Cinematic Interactions (`E`, walking away…) |

## 📦 Requirements

| Mod | Version | Link |
|---|---|---|
One JAR per Minecraft version and loader.

**Fabric** (1.21.1 or 1.20.1)

| Mod | Version | Link |
|---|---|---|
| Fabric Loader | ≥ 0.17.2 | [fabricmc.net](https://fabricmc.net/) |
| Fabric API | 0.116.x (1.21.1), ≥ 0.92.12 (1.20.1) | [Modrinth](https://modrinth.com/mod/fabric-api) |
| **Cinematic Interactions** (Fabric) | ≥ 0.18.25 | [dcvke/CinematicInteractions](https://github.com/dcvke/CinematicInteractions) |
| **TaCZ Refabricated** | 0.7.x, from 0.7.1 | [Modrinth](https://modrinth.com/mod/tacz-refabricated) |
| Forge Config API Port | v21.1.x (1.21.1), v8.0.x (1.20.1); required by TACZ | [Modrinth](https://modrinth.com/mod/forge-config-api-port) |
| Mod Menu | optional, for the settings button | [Modrinth](https://modrinth.com/mod/modmenu) |

**NeoForge** (1.21.1)

| Mod | Version | Link |
|---|---|---|
| NeoForge | ≥ 21.1.255 | [neoforged.net](https://neoforged.net/) |
| **Cinematic Interactions** (NeoForge) | ≥ 0.18.30 | [dcvke/CinematicInteractions](https://github.com/dcvke/CinematicInteractions) |
| **TaCZ 1.21.1 NeoForge Port** (unofficial, by MUKSC) | 1.1.8 | [Modrinth](https://modrinth.com/mod/tacz-1.21.1) |

**Forge** (1.20.1)

| Mod | Version | Link |
|---|---|---|
| Forge | ≥ 47.4.10 | [minecraftforge.net](https://files.minecraftforge.net/) |
| **Cinematic Interactions** (Forge) | ≥ 0.18.30 | [dcvke/CinematicInteractions](https://github.com/dcvke/CinematicInteractions) |
| **TaCZ** (Timeless and Classics Zero) | 1.1.8 | [Modrinth](https://modrinth.com/mod/timeless-and-classics-zero) |

The settings are in the mods list's **Config** button on NeoForge and Forge (`config/cinematic_tacz-client.toml`).

## ✅ Tested versions

Every combination tested in game, with exact versions and what was checked, lives in
**[docs/compatibility](docs/compatibility/README.md)**.

| Minecraft | Loader | Status | Details |
|---|---|---|---|
| 1.21.1 | Fabric | ✅ Works | [fabric-1.21.1.md](docs/compatibility/fabric-1.21.1.md) |
| 1.20.1 | Fabric | ✅ Works | |
| 1.21.1 | NeoForge | ✅ Works | |
| 1.20.1 | Forge | ✅ Works (not in the automatic releases yet: build it locally) | |

The add-on only loads with **TACZ 0.7.1 or later within 0.7.x** (`>=0.7.1-alpha <0.8.0-alpha` in `fabric.mod.json`;
the `-alpha` is needed because Fabric reads `0.7.1-forge1.1.8-hotfix2` as a pre-release of 0.7.1). With any other
version the game says so at startup instead of failing at the table. Other Fabric versions of TACZ may work if they keep
the same classes (`com.tacz.guns.*`), but they are **untested**. Everything the add-on takes from TACZ is isolated in
[`tacz/Tacz.java`](src/main/java/dev/notsume/cinematictacz/tacz/Tacz.java) and
[`tacz/TaczClient.java`](src/main/java/dev/notsume/cinematictacz/tacz/TaczClient.java), plus the optional
[`TaczTableRendererMixin`](src/main/java/dev/notsume/cinematictacz/mixin/TaczTableRendererMixin.java) for the ammo
press: a port with other names is adapted there. Each table's placement is fitted to the default gunpack's models; gunpacks with very different models
may need adjustments.

## 🛠️ Building

Java 21 (the 1.20.1 JAR is compiled for Java 17). One codebase builds every version with
[Stonecutter](https://stonecutter.kikugie.dev/): a node per Minecraft version and loader, `1.21.1-fabric` and
`1.20.1-fabric`, `1.21.1-neoforge`, `1.20.1-forge`. The Cinematic Interactions JARs are not on any Maven repository: build its targets (it needs a JDK 17
for 1.20.1) and copy them to `libs/`.

```sh
(cd ../CinematicInteractions && ./gradlew :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:build -x test)
cp ../CinematicInteractions/versions/1.2*/build/libs/*-0.18.30.jar libs/
./gradlew build                       # a JAR per node in versions/<node>/build/libs/
./gradlew :1.21.1-fabric:runClient    # test game with TACZ and Cinematic Interactions
```

`runClient` and `runServer` copy TaCZ Refabricated and Forge Config API Port into `run/<node>/<side>/mods`, as a normal
install would, because they carry nested libraries the dev classpath doesn't load.

## 🔧 Technical notes

- **TACZ mouse keys on Fabric.** TACZ binds *shoot* and *aim* to the same buttons as *attack* and *use*. Forge lets one
  button trigger several key mappings; Fabric (vanilla) only one, so with TaCZ Refabricated either the gun or the hand
  never got the click. The add-on makes the button reach every mapping sharing it when one of them is TACZ's
  ([`KeyMappingMixin`](src/main/java/dev/notsume/cinematictacz/mixin/KeyMappingMixin.java)).
- **Add-on API.** Built on Cinematic Interactions' public add-on API (`AddonStations`, `AddonScenes`,
  `AddonMenuStation`). TACZ's tables have no slots, so the scene sends TACZ's craft packet instead of slot clicks.
- **Mapping of both mods** for development: [docs/tacz-map.md](docs/tacz-map.md) (in Spanish).

## 🌿 Branches

`dev` → `qa` → `nightly` → `main`. Everything lands in `dev` first; `main` gets tested releases.

## 🙏 Credits

- **[Cinematic Interactions](https://github.com/dcvke/CinematicInteractions)** by Southware: the idea of cinematic
  scenes, their camera, hands and the add-on API all of this is built on.
- **[TACZ — Timeless and Classics Zero](https://github.com/MCModderAnchor/TACZ)** by the TACZ Dev Team: the guns, the
  tables, their models and the default gunpack.
- **[TaCZ Refabricated](https://modrinth.com/mod/tacz-refabricated)** by Sh1roCu: the Fabric port of TACZ.

All rights to the ideas, design and assets of those mods belong to their authors.

## 📄 License

The add-on declares MIT in `fabric.mod.json`. Cinematic Interactions is MIT; TACZ and TaCZ Refabricated are GPL-3.0.
