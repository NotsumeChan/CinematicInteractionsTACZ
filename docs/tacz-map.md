# Cinematic Interactions: TACZ Addon — mapa de contexto

Mapa de lo que el addon necesita de TACZ y de la API de addons de Cinematic Interactions (CI).
Fuente TACZ: `../TACZ` (MCModderAnchor/TACZ, `1.1.8-hotfix2`, **Forge 1.20.1**).
Fuente CI: `../CinematicInteractions` (`0.18.28`).

## 1. Matriz de versiones (el límite real)

| Loader / MC      | CI (API addons)               | TACZ                                   | ¿Addon posible? |
|------------------|-------------------------------|----------------------------------------|-----------------|
| Fabric 1.21.1    | `fabric/1.21.1` (0.18.25)     | TaCZ Refabricated (no oficial)         | **Sí** (objetivo inicial) |
| Fabric 1.20.1    | —                             | TaCZ Refabricated (no oficial)         | No (no hay CI) |
| Fabric 26.1.x–26.3 | `fabric/26.*`               | — (Renovated es solo NeoForge)         | No (no hay TACZ) |
| Forge 1.20.1     | `forge/1.20.1`                | **TACZ oficial** (este repo)           | **Sí** |
| NeoForge 1.21.1  | raíz del repo (preview API)   | TaCZ NeoForge port (no oficial)        | Sí |
| NeoForge 26.x / 1.21.11 | (verificar)            | TaCZ Renovated (no oficial)            | Por verificar |

Consecuencia: en Fabric hoy solo existe **una** versión en común (1.21.1), y depende de un port no oficial.
Para "muchas versiones rápido" conviene un núcleo común + capas finas por loader (como CI), y sumar
Forge 1.20.1 (TACZ oficial) y NeoForge 1.21.1 como siguientes objetivos.

La API de CI es idéntica entre versiones salvo `ResourceLocation` → `Identifier` en 26.x.

## 2. TACZ: lo que importa

### Bloques (`com.tacz.guns.block`, registro en `init/ModBlocks.java`)

| ID                    | Clase                 | Forma   | Propiedades extra        | Raíz (`getRootPos`) |
|-----------------------|-----------------------|---------|--------------------------|---------------------|
| `tacz:gun_smith_table`| `GunSmithTableBlockB` | 2×1×1   | `PART` (`BED_PART`)      | `FOOT`; desde `HEAD` → `pos.relative(FACING.getOpposite())` |
| `tacz:workbench_a`    | `GunSmithTableBlockA` | 1×1×1   | —                        | la misma pos |
| `tacz:workbench_b`    | `GunSmithTableBlockB` | 2×1×1   | `PART`                   | igual que arriba |
| `tacz:workbench_c`    | `GunSmithTableBlockC` | 1×2×1   | `HALF` (`DOUBLE_BLOCK_HALF`) | mitad inferior (verificar) |

- Todas extienden `AbstractGunSmithTableBlock`: `FACING = HORIZONTAL_FACING`, `RenderShape.ENTITYBLOCK_ANIMATED`
  (el modelo lo dibuja un BER con modelo Bedrock, no un modelo de bloque JSON).
- `use()` en servidor abre el menú del block entity **de la raíz** y escribe en el buffer el `BlockId` de la mesa.
- `B` usa `getClockWise()` del jugador para `FACING` (no `getOpposite()` como `A`): el lado "de trabajo" difiere por tipo.
  Calibrar `sides` por bloque en juego.

### Block entity (`GunSmithTableBlockEntity`)

- Uno solo para los 4 bloques. Guarda `BlockId` (NBT `"BlockId"`): **qué tipo de mesa definida por gunpack** es.
- `createMenu` → `new GunSmithTableMenu(id, inv, getId())`.

### Menú (`inventory/GunSmithTableMenu`, `MenuType` `tacz:gun_smith_table_menu`)

- **No tiene slots.** `quickMoveStack` devuelve vacío. Nada de `slotClick` sirve aquí.
- `getBlockId()` → tipo de mesa. `filter` sale de `TimelessAPI.getCommonBlockIndex(blockId)`.
- `doCraft(recipeId, player)` (servidor):
  1. valida que la receta pase el filtro de la mesa y que su `tab` exista en `blockIndex.getData().getTabs()`
     (con la mesa por defecto y `SyncConfig.ENABLE_TABLE_FILTER` apagado, no filtra);
  2. busca los ingredientes en **todo el inventario del jugador** (`ITEM_HANDLER`), salvo en creativo;
  3. **suelta el resultado como `ItemEntity` en el jugador** (y + 0.5, pickup delay 0);
  4. `broadcastFullState()` + `ServerMessageCraft(containerId)` al cliente.

### Red (`network/message`)

- `ClientMessageCraft(ResourceLocation recipeId, int menuId)` C→S: solo crafta si
  `player.containerMenu.containerId == menuId && containerMenu instanceof GunSmithTableMenu`.
  Se envía con `NetworkHandler.CHANNEL.sendToServer(...)` (Forge). **En Refabricated cambia: verificar.**
- `ServerMessageCraft(menuId)` S→C: llama `GunSmithTableScreen.updateIngredientCount()` **solo si la pantalla
  abierta es `GunSmithTableScreen`**. Con la escena de CI eso no se cumple: la escena debe recalcular
  sus conteos sola (escuchar cambios de inventario o recalcular por tick).

### Lógica de UI a replicar (`client/gui/GunSmithTableScreen`)

| Método | Qué hace | En la escena |
|--------|----------|--------------|
| `classifyRecipes()` | Agrupa `GunSmithTableRecipe` por tab según `CommonBlockIndex.getData().getTabs()` | Mismas pestañas/categorías en la mesa |
| `getPlayerIngredientCount(recipe)` | Cuenta ingredientes del inventario | Indicador de "se puede craftear" |
| `isSuitableForMainHand` / filtro | Filtra recetas por lo que hay en la mano principal | Opcional |
| `renderLeftModel(recipe)` | Muestra el modelo del arma resultante | Preview 3D sobre la mesa |
| `addCraftButton()` | Envía `ClientMessageCraft` | La acción de "craftear" de la escena |

Clases de receta: `crafting/GunSmithTableRecipe` (`getInputs()` → `GunSmithTableIngredient{getIngredient(), getCount()}`,
`getResultItem(...)`, `getTab()`, `getId()`), `GunSmithTableResult`.

## 3. Encaje con la API de addons de CI (`dev.dyslexic.cinematic.api`)

- `AddonStations.Definition(id, blockId, menuClass, sides)`: **un bloque = una definición**, así que hacen falta 4
  (`cinematic_tacz:gun_smith_table`, `…:workbench_a|b|c`), todas con `menuClass = GunSmithTableMenu.class`.
- `sides = state -> List.of(<lado de trabajo según FACING>)`. Devolver lista vacía desactiva la escena para ese
  estado, pero entonces se abre el menú normal: **pendiente** decidir qué pasa al clicar la parte no raíz
  (`HEAD` / mitad superior), porque `AddonScenes.create` exige `station.pos == pos` clicado.
- Escena cliente: `AddonScenes.register(id, (pos, state, player, side) -> new TaczTableScene(...))`
  extendiendo `AddonMenuStation<GunSmithTableMenu>` con `inputSlots = 0`.
  Métodos a implementar: `approach`, `lean`, `rest`, `updateCursor`, `handTarget`, `heldItemPoint`, `act`
  (antes `click`), `renderScene`, `renderWorld`, `restPoint`, `hint`.
- `act()` **no** usa `slotClick`: envía `ClientMessageCraft(recipeId, menu.containerId)` por la red de TACZ.
  La API marca "custom network actions" como no expuestas aún, pero aquí no hace falta: el paquete es de TACZ.
- El resultado cae como ítem al jugador (no en un slot): la animación de salida/"resultado" es cosmética.
- Sin quick-move, sin slots de entrada; el "drawer" de inventario de CI sirve solo como vista.

## 4. Pendiente de verificar en TaCZ Refabricated (Fabric 1.21.1)

Fuente: `Sh1roCu/TACZ-Refabricated` (upstream) / `q14433686-arch/TaCZ_Refabricated_Unofficial`.

- [ ] Paquetes y nombres de clase (¿siguen en `com.tacz.guns`?).
- [ ] Cómo se envía `ClientMessageCraft` (en 1.21.1 Fabric debería ser un `CustomPacketPayload`).
- [ ] `doCraft`: sustituto de `ForgeCapabilities.ITEM_HANDLER`.
- [ ] `MenuType` (`ExtendedScreenHandlerType` en Fabric) y datos de apertura (`BlockId`).
- [ ] IDs de bloque idénticos (`tacz:gun_smith_table`, `workbench_a|b|c`).
- [ ] Dependencias: Forge Config API Port.

## 5. Estructura propuesta del addon

- Nombre: **Cinematic Interactions: TACZ Addon**, mod id `cinematic_tacz`.
- Dependencias requeridas (ambos lados): `cinematic`, `tacz`. Compilar contra los JARs en `libs/` (`modCompileOnly`).
- Primer objetivo: `fabric/1.21.1`. Mantener la lógica de escena (layout, recetas, conteos) libre de APIs de loader
  para portarla a `forge/1.20.1` y `neoforge/1.21.1` cambiando solo registro + envío del paquete.

## 6. Verificado en TaCZ Refabricated 1.21.1 (`../TACZ-Refabricated`, rama `1.21.1`, `ff068d2`)

- Versión `0.7.1-forge1.1.8-hotfix2`, Fabric Loader 0.17.2, Fabric API `0.116.5+1.21.1`, mappings **Yarn** + Parchment
  (CI usa Mojang + Parchment: compilar el addon con Mojang y consumir TACZ como `modCompileOnly`, Loom remapea).
- Paquetes iguales: `com.tacz.guns.*`. IDs de bloque/menú iguales (`tacz:gun_smith_table`, `workbench_a|b|c`,
  `tacz:gun_smith_table_menu`).
- `GunSmithTableMenu.TYPE` = `ExtendedScreenHandlerType` con `ResourceLocation.STREAM_CODEC` (el `BlockId`).
- `ClientMessageCraft(ResourceLocation recipeId, int menuId)` es un `record ... implements CustomPacketPayload`
  (`tacz:client_craft`). Enviar con `ClientPlayNetworking.send(new ClientMessageCraft(id, menu.containerId))`.
- `ServerMessageCraft(int menuId)` (`tacz:server_craft`): solo refresca `GunSmithTableScreen`.
- `doCraft` usa `player.tacz$getItemHandler(null)` (interfaz inyectada) en vez de la capability de Forge;
  el resultado sigue cayendo como `ItemEntity` sobre el jugador.
- `RecipeHolder<?>` (1.21): la receta se obtiene con `recipeManager.byKey(id)` → `holder.value()`.
