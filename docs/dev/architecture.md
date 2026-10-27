# Architecture

## Files

```
src/main/java/dev/notsume/cinematictacz/
  CinematicTacz.java            common init: one AddonStations.Definition per TACZ table
  tacz/Tacz.java                TACZ bridge (common): table ids, shapes, front side, root block, table type,
                                tabs + recipes converted to own records (Tab, Recipe, Input), key category
  tacz/TaczClient.java          TACZ bridge (client): craft packet, table window check, inspect, press stroke
  client/CinematicTaczClient.java  client init: AddonScenes per table, AfterCraft tick
  client/GunSmithScene.java     the scene (extends AddonMenuStation<AbstractContainerMenu>)
  client/CraftFx.java           effects: press stroke, printer plate and forge tool states, smoke/sparks/fire
  client/AfterCraft.java        after forging: select the new gun, then inspect
  mixin/HandsAccessor.java      Cinematic Interactions' real hand position (package-private there), for the forge's tools
  mixin/MinecraftMixin.java     TACZ's table window only behind a scene ("step closer" otherwise)
  mixin/KeyMappingMixin.java    a mouse button reaches every key mapping on it when one is TACZ's (shoot/aim)
  mixin/LanguageManagerMixin.java  es_* (es_cl, es_mx …) loads es_es before en_us
  mixin/GunSmithTableBlockEntityMixin.java  (both sides) a table's second block answers with its root's table type
  mixin/TaczTableRendererMixin.java  optional (require 0): ammo press stroke while crafting
```

Only `tacz/`, `TaczTableRendererMixin` and `GunSmithTableBlockEntityMixin` import `com.tacz.*`. A different TACZ port is adapted there.

## How a scene works

1. `CinematicTacz` registers the 4 blocks (`tacz:gun_smith_table`, `workbench_a|b|c`) with `GunSmithTableMenu` and the
   working side from `Tacz.front` (A/C face the placer → `FACING`; B and gun_smith_table lie across → `FACING.cw`).
2. Cinematic Interactions opens the scene and the server opens TACZ's menu behind it. `GunSmithScene.tick` loads the
   recipes only from a menu that is **this table's** (`Tacz.menuType(menu)` equals `Tacz.keptType(level, root)`); if it
   never arrives (fast moves between tables) it re-uses the table (`askForMenu`, max 3 times).
3. TACZ's menu has **no slots**: crafting sends `ClientMessageCraft(recipeId, containerId)` (TACZ's packet). The server
   takes materials from the whole inventory and drops the result as an item entity at the player's feet.
4. `Layout` (per shape) places the grid, the hovering chosen part, the ingredients, camera and hands, in blocks from the
   footprint centre at its foot (`out` towards the player, `up` from the floor). Values were fitted by eye to the
   default gunpack's models from F2 screenshots.
   - SINGLE (`workbench_a`, ammo bench): grid on the front mat, ingredients between the hands.
   - WIDE (`gun_smith_table`, `workbench_b`): desk at 15 px; `center()` never below 1.0 or Cinematic Interactions sees
     the work blocked by the other block. Guns drawn at 30° (`DIAGONAL`) and scaled per tab (`tabScale`).
   - TALL (`workbench_c`, attachment bench): upright board on the front face (`workOnTop() == false`).
5. Cursor: grid cell under the cursor; off the grid but on the own table (either block) `aim` stays set, otherwise
   Cinematic Interactions would treat the other block as a neighbouring station and "move over".

## Crafting animations (`GunSmithScene` + `CraftFx` + `AfterCraft`)

| Table | Timeline (`moveTime()` seconds) |
|---|---|
| WIDE forge | 0–0.4 the preview sinks into `anvil()`, materials gather and stay there · 0.1–0.4 the right hand reaches for the table's own hammer on its tool wall (`TaczClient.toolRest`), takes it (`CraftFx.holding` → `TaczClient.hide` takes it out of the model, `TaczClient.drawTool` draws it in the hand) and has it on the work by 0.7 · blows at 1.1/1.5/1.9 (sparks, smoke, anvil sound) · hangs it back by 2.3 · takes the drill off the desk (2.55), bit down on the work by 2.85, two holes at 3.05/3.45 (sparks, grindstone whirr), lays it back by 3.95 · 4.05 `FORGED`: big smoke and fire (extinguish hiss), sends the craft and `AfterCraft.expect` · 4.05–4.45 the finished part rises out of the smoke, then `Scene.leave()` · `AfterCraft` selects the new gun as soon as it is picked up (TACZ draws it during the outro) and calls `TaczClient.inspect` once the scene is over |
| SINGLE press | materials into `pressMouth()` · strokes at 0.6/1.0 (`CraftFx.press`: ram down, lever turned down round its hinge; smoke, piston sound), the right hand on the lever knob (`TaczClient.leverKnob`) from 0.3 s · 4 rounds fall to the mat · crafts at the end (1.7) |
| TALL printer | 0–0.5 materials and preview fall into the resin (`vat()`) · 0.3–1.25 the camera bends down (`bend`, `freeCamera`) and the right hand taps the screen at 0.8 · 1.1–1.7 build plate down (`CraftFx.printer` → `TaczClient.print`: plate cubes of bone `crane`, model print `workpiece` hidden) · two peels to 2.5 · up to 3.1 with the part under it · part floats to `showcase()`, chime · crafts at 3.6 |

`beforeFrame()` lands blows/strokes on time; `handsOverride()` drives the hands while forging.

## Gotchas found

- TACZ gives both blocks of a two-block table a block entity, but sets the table type only on the root's. Cinematic
  Interactions opens the **clicked** block's menu, so the upper half of the attachment bench opened a default
  (gun smith table) menu: "no recipes". `GunSmithTableBlockEntityMixin` makes the second block report its root's type.

- Fabric reads TACZ `0.7.1-forge1.1.8-hotfix2` as a **pre-release** of 0.7.1: the dependency is
  `>=0.7.1-alpha <0.8.0-alpha` (checked with Fabric Loader's own `VersionPredicate`).
- Loom remaps mixin targets in vanilla classes but **not methods of another mod**: `TaczTableRendererMixin` lists the
  dev descriptor and the production one. In TaCZ Refabricated's jar the typed method is still called `render`, with
  intermediary types (`class_4587` PoseStack, `class_4597` MultiBufferSource); `method_3569` is only the bridge.
  Check a production jar with `javap -p` before guessing.
- TaCZ Refabricated's runtime needs night-config/LuaJ nested in its jar; the dev runs copy the real jars to
  `run/<side>/mods` (`hostMods` configuration) instead of putting them on the classpath.
- TACZ binds shoot/aim to the same buttons as attack/use; vanilla `KeyMapping` gives a key to one mapping only.
- TACZ table models are shared per table type: a bone change must be set before one table is drawn and undone after.
