# Cinematic Interactions: TACZ Addon

First-person [Cinematic Interactions](https://github.com/dcvke/CinematicInteractions) scenes for the gun smith tables of
TACZ ([TaCZ Refabricated](https://modrinth.com/mod/tacz-refabricated) on Fabric).

Target: **Fabric 1.21.1** (Cinematic Interactions ≥ 0.18.25, TaCZ Refabricated 0.7.1). Context and mapping of both mods:
[docs/tacz-map.md](docs/tacz-map.md).

## Build

Java 21. The Cinematic Interactions JAR is not on a Maven repository: build it and copy it to `libs/`.

```sh
(cd ../CinematicInteractions/fabric/1.21.1 && sh gradlew remapJar)
cp "../CinematicInteractions/fabric/1.21.1/build/libs/Cinematic Interaction-Fabric-1.21.1-0.18.25.jar" libs/cinematic-fabric-1.21.1-0.18.25.jar
sh gradlew build
```

## Branches

`dev` → `qa` → `nightly` → `main` (releases).
