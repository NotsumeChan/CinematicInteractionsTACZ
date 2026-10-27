# Roadmap and open work

State on 2026-10-27: everything below "Done" builds and was tested in game on Fabric 1.21.1; the animations are a first
version the owner tested and gave feedback on.

## Done

- Scenes for the three tables, TACZ isolated in `tacz/`, scene-only window, Spanish fallback, TACZ mouse keys,
  per-tab gun scale and diagonal, menu ownership check and re-request, TACZ version range.
- Animations v1: forge (hammer blows, sparks, smoke, auto leave, draw + inspect), press (strokes, smoke, falling rounds).
- Docs: README (English), `docs/compatibility/`, `docs/tacz-map.md`, these dev notes.

## Next (owner's feedback on animations v1)

1. **Inspect right after crafting.** Now: leave scene → wait until the scene is over and the gun is picked up →
   wait 25 ticks (`AfterCraft.DRAW`) → inspect; it feels slow. TACZ's `inspect()` (`LocalPlayerInspect`) only checks
   `clientStateLock`, not the draw. Plan: select the gun slot as soon as the gun is in the inventory (during the
   rise/outro, so TACZ's draw plays while the camera comes back), inspect as soon as `Scene.active()` is false;
   shorten the rise (`FORGE_LENGTH`).
2. **Hammer attached to the hand.** Now the hammer is drawn at the hand *target* and the hand lags behind. Cinematic
   Interactions' `AnvilStation` draws it at the real hand (`Hands.position(true)`, package-private) and sets
   `handsStiffness()` to 55 while moving. Plan: an `@Invoker` mixin on `dev.dyslexic.cinematic.client.Hands#position`
   (a mod's own method name, not remapped) and the same stiffness.
3. **Press: only the ram moves.** Now the whole bone `loader` (frame included) moves. TACZ (`BedrockModel`): cubes
   without rotation are `BedrockCubeBox` in `part.cubes` (public `minX..maxZ`, converted coordinates); each rotated cube
   becomes its own child `BedrockPart` (pivot in `x/y/z`, rotation) in `part.children`; `addChild` is public. Plan: on
   first use, move the ram column's cubes/children (bedrock column around x ≈ -3, z ≈ 6.5: cubes
   `[-3.5,8.5,6] 1×9×1`, `[-3.5,20.5,6] 1×4×1`, `[-3.75,22,5.75]`, the small ones at y 17.5–19.25) into a new child
   part and stroke only that. Check `convertOrigin`/`convertPivot` in `BedrockModel` for the coordinate conversion.
   Bone and cubes are the default gunpack's (`ponytail:` note it).
4. **Stonecutter** (kikugie's Gradle plugin): one codebase for several Minecraft versions/loaders. Start with
   `fabric 1.21.1` as the only node so the build is unchanged, version-specific code behind `//?` comments. Next
   candidates: Forge 1.20.1 (original TACZ, Cinematic Interactions has `forge/1.20.1`), NeoForge 1.21.1.
5. Attachment bench animation: no idea chosen yet (ideas: the attachment flies onto a hovering gun and clicks on).

## Requests for Cinematic Interactions' owner

- A public recipe panel API (generalise `TableRecipeBook`: `RecipePanel` + `Source` of entries; `ClientStation.recipePanel()`
  so `SceneScreen` stops checking `instanceof TableStation`). Then the scene could use the real recipe book.
- Optional: multi-block stations (a definition mapping several blocks to one root), making the "own table" aim
  workaround unnecessary.

## Decisions pending

- License: `fabric.mod.json` says MIT, there is no LICENSE file; TACZ and TaCZ Refabricated are GPL-3.0.
- Whether the forge scene should close after every gun (now it does, for the inspect) or stay for more crafts.
