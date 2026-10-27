<div align="center">

# 🎬🔫 Cinematic Interactions: TACZ Addon

**Las mesas de armas de TACZ, en primera persona y al estilo de Cinematic Interactions.**

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat-square)
![Loader](https://img.shields.io/badge/Loader-Fabric-DBD0B4?style=flat-square)
![Estado](https://img.shields.io/badge/estado-dev-orange?style=flat-square)
![Vibecoded](https://img.shields.io/badge/vibecoded-🤖%20Claude%20Code-8A63D2?style=flat-square)

</div>

---

Un addon que une dos mods: en vez de abrir la ventana de TACZ, te acercas a la mesa, la cámara se inclina sobre ella y
las armas, la munición y los accesorios están ahí, sobre el tablero. Eliges una pieza, la vuelves a tocar y la ves
armarse con sus materiales.

> [!IMPORTANT]
> **Este addon no es oficial** y no está afiliado a los autores de Cinematic Interactions ni de TACZ. Solo conecta
> ambos mods: **las ideas, el diseño y el mérito de cada mod pertenecen a sus autores** (ver [Créditos](#-créditos)).

> [!NOTE]
> **Proyecto vibecodeado.** El código fue escrito con asistencia de IA ([Claude Code](https://claude.com/claude-code))
> y probado en el juego por [NotsumeChan](https://github.com/NotsumeChan). Revisa y prueba antes de usarlo en un
> servidor o modpack serio.

## ✨ Qué hace

| Mesa | Escena |
|---|---|
| 🔫 **Mesa de armas** (`gun_smith_table`) | Las armas descansan en diagonal sobre el escritorio, como en un armero; los rifles se ven más pequeños que las pistolas para que quepan |
| 🧨 **Mesa de munición** (`ammo_workbench`) | La munición sobre el tapete libre; la elegida flota arriba y sus materiales entre tus manos |
| 🔭 **Mesa de accesorios** (`attachment_workbench`) | Mesa alta: los accesorios cuelgan de su frente, como en un tablero de herramientas |

- **Las mismas recetas que TACZ**: respeta las pestañas y filtros de cada mesa del gunpack, y el servidor sigue siendo
  quien valida y entrega el crafteo (se usa el paquete de crafteo del propio TACZ).
- **Animación de creación**: los materiales se juntan en la pieza y esta aparece con un pequeño golpe.
- **Contador de materiales** `tengo/necesito` en verde o rojo; las piezas que no puedes fabricar se ven translúcidas.
- **Solo escena**: la ventana de TACZ no se abre por fuera de la escena (de lejos, agachado o con la tecla de
  interacción de TACZ); un aviso te pide acercarte.
- **Disparar y apuntar con los clics de siempre** en Fabric (ver [Notas técnicas](#-notas-técnicas)).
- **Español para todas las variantes**: si juegas en `es_cl`, `es_mx`, `es_ar`…, lo que no tenga traducción propia
  usa `es_es` en vez de caer al inglés (TACZ y sus gunpacks solo traen `es_es`).

### 🎮 Controles en la escena

| Acción | Control |
|---|---|
| Elegir una pieza | Clic izquierdo |
| Fabricar la elegida | Clic izquierdo otra vez |
| Siguiente categoría | Clic derecho |
| Cambiar de página | Rueda del mouse |
| Salir | Los mismos de Cinematic Interactions (`E`, moverse…) |

## 📦 Requisitos

| Mod | Versión | Enlace |
|---|---|---|
| Minecraft | 1.21.1 | |
| Fabric Loader | ≥ 0.17.2 | [fabricmc.net](https://fabricmc.net/) |
| Fabric API | 0.116.x | [Modrinth](https://modrinth.com/mod/fabric-api) |
| **Cinematic Interactions** (Fabric) | ≥ 0.18.25 | [dcvke/CinematicInteractions](https://github.com/dcvke/CinematicInteractions) |
| **TaCZ Refabricated** | 0.7.1 | [Modrinth](https://modrinth.com/mod/tacz-refabricated) |
| Forge Config API Port | v21.1.x (lo pide TACZ) | [Modrinth](https://modrinth.com/mod/forge-config-api-port) |

## ✅ Versiones probadas

Combinación con la que se desarrolló y probó en el juego (27 de octubre de 2026):

| Componente | Versión probada |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader / Fabric API | 0.17.2 / 0.116.17+1.21.1 |
| Cinematic Interactions | 0.18.25 para Fabric 1.21.1, compilado desde [`8a6d9c0`](https://github.com/dcvke/CinematicInteractions/commit/8a6d9c0) |
| TaCZ Refabricated | `1.21.1-0.7.1-forge1.1.8-hotfix2` (Modrinth) |
| Forge Config API Port | v21.1.6 |
| Gunpack | TAC Zero Default Pack 1.1.8 (`tacz_default_gun`) |

Otras versiones de TACZ para Fabric pueden funcionar si conservan las mismas clases (`com.tacz.guns.*`), pero **no
están probadas**. Todo lo que el addon toma de TACZ está aislado en
[`tacz/Tacz.java`](src/main/java/dev/notsume/cinematictacz/tacz/Tacz.java) y
[`tacz/TaczClient.java`](src/main/java/dev/notsume/cinematictacz/tacz/TaczClient.java): un port con otros nombres se
adapta ahí. Las posiciones de cada mesa están ajustadas a los modelos del gunpack por defecto; otros gunpacks con
modelos muy distintos pueden necesitar ajustes.

## 🛠️ Compilar

Java 21. El JAR de Cinematic Interactions no está en ningún repositorio Maven: hay que compilarlo y copiarlo a `libs/`.

```sh
(cd ../CinematicInteractions/fabric/1.21.1 && sh gradlew remapJar)
cp "../CinematicInteractions/fabric/1.21.1/build/libs/Cinematic Interaction-Fabric-1.21.1-0.18.25.jar" libs/cinematic-fabric-1.21.1-0.18.25.jar
./gradlew build        # JAR en build/libs/
./gradlew runClient    # juego de prueba con TACZ y Cinematic Interactions
```

`runClient` y `runServer` copian TaCZ Refabricated y Forge Config API Port a `run/<lado>/mods`, como en una
instalación normal, porque traen librerías internas que el classpath de desarrollo no carga.

## 🔧 Notas técnicas

- **Clics de TACZ en Fabric.** TACZ asigna *disparar* y *apuntar* a los mismos botones que *atacar* y *usar*. Forge deja
  que un botón active varias asignaciones; Fabric (vanilla) solo una, así que con TaCZ Refabricated el arma o la mano se
  quedaban sin clic. El addon hace que el botón llegue a todas las asignaciones que lo comparten cuando una es de TACZ
  ([`KeyMappingMixin`](src/main/java/dev/notsume/cinematictacz/mixin/KeyMappingMixin.java)).
- **API de addons.** Usa la API pública de Cinematic Interactions (`AddonStations`, `AddonScenes`,
  `AddonMenuStation`). Como las mesas de TACZ no tienen slots, la escena manda el paquete de crafteo de TACZ en vez de
  clics de slot.
- **Mapeo de ambos mods** para desarrollar: [docs/tacz-map.md](docs/tacz-map.md).

## 🌿 Ramas

`dev` → `qa` → `nightly` → `main`. Todo entra primero a `dev`; `main` recibe versiones probadas.

## 🙏 Créditos

- **[Cinematic Interactions](https://github.com/dcvke/CinematicInteractions)** por Southware: la idea de las escenas
  cinemáticas, su cámara, sus manos y la API de addons sobre la que se construye todo esto.
- **[TACZ — Timeless and Classics Zero](https://github.com/MCModderAnchor/TACZ)** por el TACZ Dev Team: las armas, las
  mesas, sus modelos y el gunpack por defecto.
- **[TaCZ Refabricated](https://modrinth.com/mod/tacz-refabricated)** por Sh1roCu: el port de TACZ a Fabric.

Todos los derechos sobre las ideas, el diseño y los recursos de esos mods pertenecen a sus autores.

## 📄 Licencia

El addon se declara MIT en `fabric.mod.json`. Cinematic Interactions es MIT; TACZ y TaCZ Refabricated son GPL-3.0.
