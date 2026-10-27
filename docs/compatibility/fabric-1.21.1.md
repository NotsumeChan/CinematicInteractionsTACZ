# Fabric 1.21.1

**Status: ✅ works** — tested in game on October 27, 2026 (dev client and a dedicated test server).

## Versions

| Component | Version | Source |
|---|---|---|
| Minecraft | 1.21.1 | |
| Fabric Loader | 0.17.2 | TaCZ Refabricated 0.7.1 requires ≥ 0.17.2 |
| Fabric API | 0.116.17+1.21.1 | |
| Java | 21 (Temurin 21.0.13) | |
| Cinematic Interactions | 0.18.25 (Fabric 1.21.1) | built from [`dcvke/CinematicInteractions@8a6d9c0`](https://github.com/dcvke/CinematicInteractions/commit/8a6d9c0), `fabric/1.21.1` |
| TaCZ Refabricated | `1.21.1-0.7.1-forge1.1.8-hotfix2` | Modrinth, version id `veM4XOhW` |
| Forge Config API Port | v21.1.6-1.21.1-Fabric | Modrinth, version id `N5qzq0XV` |
| Gunpack | TAC Zero Default Pack 1.1.8 (`tacz_default_gun`) | ships with TACZ |
| Add-on | 0.1.0 | `dev` branch |

The add-on accepts TACZ `>=0.7.1-alpha <0.8.0-alpha` (any 0.7.x from 0.7.1); only the version above was played.

## Checked

| What | Result |
|---|---|
| Gun smith table scene (both blocks, every tab) | ✅ |
| Ammo workbench scene | ✅ |
| Attachment workbench scene | ✅ |
| Crafting through the scene (materials taken, item handed) | ✅ |
| Tables side by side, moving quickly from one to the next | ✅ the scene only uses its own table's menu |
| TACZ window blocked outside the scene (far away, sneaking, interact key) | ✅ |
| Shoot / aim with left / right click | ✅ with the add-on's key fix |
| Spanish (`es_cl`) falling back to `es_es` | ✅ |
| Dedicated server start | ✅ |

## Known limits

- Table layouts are fitted to the default gunpack's models; gunpacks with very different table models may place parts
  oddly.
- TACZ drops the crafted item at the player's feet (TACZ's own behaviour); it is picked up at once.
- Not tested: multiplayer with several players at once, other gunpacks, Iris/Sodium.
