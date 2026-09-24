# Pane

Pane is a GUI library for Fabric and NeoForge. It gives you a set of widgets,
plus a markup layer if you'd rather write your UI in something close to HTML and
CSS. Everything is drawn through Minecraft's own render pipeline. It doesn't
bundle native libraries or a separate renderer, so it won't conflict with other
UI mods.

Download it from [Modrinth](https://modrinth.com/mod/pane). The
[Pane wiki](https://ethrocky.net/pane/docs) covers getting started, the widget
catalog and data binding, and has the PML/PSS markup reference.

Luminary, my scenic lighting mod, uses Pane for its whole editor. The tool
windows, keyframe timeline and painting canvas all run live on top of the game.

## Repo layout

A single source tree builds Fabric and NeoForge jars for every Minecraft version
from 1.21 to 26.2 (except 1.21.2 on NeoForge). The build is set up with
[Stonecutter](https://codeberg.org/stonecutter/stonecutter) and the CC0
[multiloader template](https://codeberg.org/stonecutter/template-multiloader).

- `src/main/java/net/ethrocky/pane/`
  - `core/`, `widget/` and `markup/` are the version-independent part of Pane.
    Nothing in them imports Minecraft, so Stonecutter leaves them alone.
  - Everything else is the runtime glue. It's written for the newest version,
    26.2, in Mojang mappings, and Stonecutter comments handle the older ones.
    Loader-specific code sits behind `//? if fabric` / `//? if neoforge` guards
    on the entry points.
- `stonecutter.properties.toml` has the dependency pins for every Minecraft version.
- `build.fabric.gradle.kts` and `build.neoforge.gradle.kts` have the build logic
  for each loader.

Version checks in the source:

- `>=1.21.9`: input records (`MouseButtonInfo`/`KeyEvent`/`CharacterEvent`),
  keybind `Category`, `HudElementRegistry` (older versions use `HudRenderCallback`).
- `>=1.21.11`: official mappings renamed `ResourceLocation` to `Identifier`.
- `>=26.1`: `GuiGraphics` -> `GuiGraphicsExtractor` + `extractRenderState`,
  `Window.handle()`, scaled mouse coords, Fabric's `KeyMappingHelper`.
- `>=26.2`: `Minecraft.screen` moved behind `gui.screen()`.

## Building

```
./gradlew chiseledBuild                             # every version + loader
./gradlew :1.21.1-fabric:build                      # one target
./gradlew "Set active project to 1.21.1-neoforge"   # switch the IDE-visible version
./gradlew :1.21.1-neoforge:runClient                # run one target
```

## License

MIT.
