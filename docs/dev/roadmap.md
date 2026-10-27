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
6. **Settings**, the way Cinematic Interactions does them (`fabric/1.21.1`: `client/Settings.java`, `ClientConfig`,
   `ModMenuIntegration`, `ClothScreen`, `SettingsScreen`): a client file `config/cinematic_tacz-client.json` (same keys
   in a `.toml` on Forge/NeoForge) built from one spec, every value with a comment and a translation key and grouped in
   sections; Mod Menu opens a Cloth Config screen when Cloth Config is installed and an own plain screen otherwise; Cloth
   Config and Mod Menu are `modCompileOnly` + `suggests`, never required. `ClientConfig` is not part of its add-on API:
   copy the pattern (or ask its owner to expose it). Candidates: scene per table, crafting animations, draw + inspect
   after forging, whether the forge scene closes after each gun (see "Decisions pending"), smoke/sparks.

## Requests for Cinematic Interactions' owner

- A public recipe panel API (generalise `TableRecipeBook`: `RecipePanel` + `Source` of entries; `ClientStation.recipePanel()`
  so `SceneScreen` stops checking `instanceof TableStation`). Then the scene could use the real recipe book.
- Optional: multi-block stations (a definition mapping several blocks to one root), making the "own table" aim
  workaround unnecessary.

## Decisions pending

- License: `fabric.mod.json` says MIT, there is no LICENSE file; TACZ and TaCZ Refabricated are GPL-3.0.
- Whether the forge scene should close after every gun (now it does, for the inspect) or stay for more crafts.
