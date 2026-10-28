# Roadmap and open work

State on 2026-10-27: everything below "Done" builds and was tested in game on Fabric 1.21.1; the animations are a first
version the owner tested and gave feedback on.

## Done

- Scenes for the three tables, TACZ isolated in `tacz/`, scene-only window, Spanish fallback, TACZ mouse keys,
  per-tab gun scale and diagonal, menu ownership check and re-request, TACZ version range.
- Animations v1: forge (hammer blows, sparks, smoke, auto leave, draw + inspect), press (strokes, smoke, falling rounds).
- Docs: README (English), `docs/compatibility/`, `docs/tacz-map.md`, these dev notes.

## Next (owner's feedback on animations v1)

1. **Inspect right after crafting.** Done, tested in game: the gun is selected as soon as it is picked up
   (during the rise/outro, so TACZ draws it while the camera comes back), inspected once `Scene.active()` is false and
   the gun is in the main hand; the rise is 0.4 s (`FORGE_LENGTH` 4.45).
2. **Hammer attached to the hand.** Done, tested in game (since then the forge takes the table's own hammer and drill
   instead of Cinematic Interactions' hammer prop, waiting for an in-game test): the hammer is drawn at the real hand
   (`mixin/HandsAccessor`, an `@Invoker` on Cinematic Interactions' package-private `Hands#position`) and
   `handsStiffness()` is 55 while forging; the handle is slanted as in that anvil and the arm lifts it (`HAMMER_LIFT`).
3. **Press: only the ram moves.** Done: `TaczClient.press` offsets only the `loader`
   children pivoted on the column axis (bedrock x -3, z 6.5: shaft, cap, collars); TACZ makes each rotated cube such a
   child, so no re-parenting was needed. Tested in game. The lever also turns down round its hinge (`LEVER_SWING` 30°)
   and the right hand follows its knob (waiting for an in-game test). Default gunpack only (`ponytail:` noted).
4. **Stonecutter** 0.9.8 (kikugie's Gradle plugin): done, one node `1.21.1` (Fabric), built JAR identical in content.
   Groovy DSL kept (`hard_mode` silences the Kotlin DSL advice). Next nodes: Forge 1.20.1 (original TACZ, Cinematic
   Interactions has `forge/1.20.1`), NeoForge 1.21.1; a second loader needs its own Loom variant per node (see the
   Stonecutter wiki and its `loom-back-compat` plugin), and its TACZ/CI JARs.
5. **Attachment bench animation.** Done, waiting for an in-game test: the bench is a big resin printer, so the materials
   fall into the vat, the player bends down and taps its screen, the build plate dips into the resin, peels twice and
   rises with the part, which floats out. A first version with a small hammer and a screwdriver was dropped (did not
   suit the machine). The forge ends in smoke and fire now; a weld and screwdriver version of it was tried and dropped
   for the table's own hammer and drill.
6. **Settings.** Done, waiting for an in-game test, the way Cinematic Interactions now does them on Fabric (it dropped
   Cloth Config): its own JSON file, `config/cinematic_tacz-client.json`, written on first start with every value,
   keys `section.name`; one list of options (`Settings.ALL`), each with a translation key and a `.tooltip` saying what
   it does; its own screen (`SettingsScreen`) behind Mod Menu's button (`modCompileOnly` + `suggests`, never required;
   `modLocalRuntime` in the dev runs). Options: scene per table (`AddonScenesMixin` on `AddonScenes.available`; off,
   Cinematic Interactions opens TACZ's window), crafting animations (off: crafted at once), draw + inspect after forging
   (off: the forge scene stays open, which settles that "decision pending" per player), particles.
7. **More versions: every target Cinematic Interactions has.** Done, the four nodes tested in game (it builds four from shared sources with Stonecutter:
   1.20.1 Forge and Fabric, 1.21.1 NeoForge and Fabric). Every one has a TACZ: Forge 1.20.1 the original TACZ
   1.1.8-hotfix2 (Modrinth `timeless-and-classics-zero`, `AzCBJlex`), Fabric 1.20.1 TaCZ Refabricated
   1.20.1-0.7.1 (`3ShZi2cZ`, branch `1.20.1` of its repo), NeoForge 1.21.1 an unofficial port by MUKSC 1.1.8-hotfix-r7
   (`tacz-1.21.1`, `62q6mB5Q`, github.com/MUKSC/TACZ-1.21.1), Fabric 1.21.1 the current one.
   - **Fabric 1.20.1**: done, tested in game. Nodes renamed `<mc>-<loader>` like Cinematic Interactions'
     targets, per-node `versions/<node>/gradle.properties`, Loom 1.17.21 (Cinematic Interactions' JARs need it), both
     nodes on its 0.18.30 JARs from main (the `fabric/1.21.1` tree is gone from our build). Differences: recipe holders
     (`Tacz.tabs`), `Screen` background before 1.20.2, three renames in `stonecutter.gradle`, `Mth.clamp` (Java 17).
   - **NeoForge 1.21.1**: done, tested in game. `build.neoforge.gradle` (ModDevGradle 2.0.148, NeoForge
     21.1.255, as Cinematic Interactions), `src/neoforge/templates/META-INF/neoforge.mods.toml`, its own mixin list
     without `KeyMappingMixin` (NeoForge already gives one button to several mappings). Loader code behind `//?`:
     the `@Mod` constructor (`CinematicTacz`, which starts `CinematicTaczClient.init` on the client), the craft packet
     (`PacketDistributor.sendToServer`), the settings as a config spec (`config/cinematic_tacz-client.toml`, the same
     options with comments, the mods list's Config button opening `SettingsScreen`), the client tick event. MUKSC's port
     keeps every TACZ class and method this add-on uses; only its recipe type is a deferred holder (a rename in
     `stonecutter.gradle`).
   - **Forge 1.20.1**: done, tested in game. `build.forge.gradle` (ModDevGradle's legacy Forge plugin
     2.0.148, Forge 47.4.10, Java 17, a mixin refmap and the `MixinConfigs` manifest attribute, as Cinematic
     Interactions), `src/forge/templates/META-INF/mods.toml`, `pack.mcmeta`, its own mixin list. Loader code: the `@Mod`
     constructor, `ForgeConfigSpec` settings with the Config button (`ConfigScreenHandler`), the client tick event,
     TACZ's `NetworkHandler` for the craft packet. `ServerPlayerMixin` opens a table's menu with
     `NetworkHooks.openScreen` and its type (TACZ's client menu factory reads it unconditionally; Cinematic Interactions
     opens menus with a plain `openMenu`). The renderer hook is not remapped (`remap = false`): TACZ's SRG JAR keeps
     `render`, only its bridge is `m_6922_`. Forge loads TACZ's jar-in-jar libraries itself, in the dev runs too.
8. **Fabric 26.1.2.** Done, tested in game (2026-10-28). Found in the test: the dev game needs Fabric Loader 0.19.5
   (`loader_version` in the node's properties), and the two-block tables open their menu from the root block
   (`Tacz.openMenu`, the definition's opener: from 26.1 only the root has a block entity). TACZ: the unofficial TaCZ Refabricated port by
   q14433686-arch, 1.1.8+fabric.26.1.2.R3-hotfix4 (GitHub releases only; it also has 26.2-26.4 branches); Cinematic
   Interactions: its `multiversion/` 26.1.2 target. One node, `26.1.2-fabric`, from the same sources (see `CLAUDE.md`).
   To check in game: recipes in every table (they now come from TACZ's synced `CommonAssetsManager`), press, printer
   plate and the forge's tools (renderer hook on `submit`), shoot/aim with the mouse without `KeyMappingMixin`, the
   draw after forging when the gun lands outside the hotbar.

## Requests for Cinematic Interactions' owner

- A public recipe panel API (generalise `TableRecipeBook`: `RecipePanel` + `Source` of entries; `ClientStation.recipePanel()`
  so `SceneScreen` stops checking `instanceof TableStation`). Then the scene could use the real recipe book.
- A per-scene switch in the add-on API (an add-on's scene turned off by the player, so the block keeps its window);
  `AddonScenesMixin` does it for now.
- On NeoForge (and Forge), open an add-on station's menu the way the block does, with its extra data (or let the
  definition write it); `GunSmithTableMenuDataMixin` does it for TACZ for now.
- Optional: multi-block stations (a definition mapping several blocks to one root), making the "own table" aim
  workaround unnecessary.

## Decisions pending

- License: `fabric.mod.json` says MIT, there is no LICENSE file; TACZ and TaCZ Refabricated are GPL-3.0.
- Whether the forge scene should close after every gun by default (now it does, for the inspect; a setting turns it off).
