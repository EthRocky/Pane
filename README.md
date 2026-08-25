# Pane

A GUI library for Fabric and NeoForge: a modern component kit with an optional
HTML/CSS-like authoring layer, rendered entirely through Minecraft's own pipeline.
No native libraries, no bundled renderer, no conflicts with other UI mods.

- **Download:** [Modrinth](https://modrinth.com/mod/pane)
- **Docs:** [the Pane wiki](https://ethrocky.net/pane/docs) — getting started,
  widget catalog, data binding, PML/PSS markup reference

Pane powers the whole editor in Luminary, my scenic lighting tool: tool windows,
a keyframe timeline and a painting canvas, all running live over the game.

## How this repo is laid out

One annotated source tree builds Fabric and NeoForge jars for every Minecraft
version from 1.21 through 26.2 (27 targets — NeoForge skipped MC 1.21.2), using
[Stonecutter](https://codeberg.org/stonecutter/stonecutter) and the CC0
[multiloader template](https://codeberg.org/stonecutter/template-multiloader).

- `src/main/java/net/ethrocky/pane/`
  - `core/`, `widget/`, `markup/` — the version-agnostic kit; no Minecraft
    imports, so it passes through Stonecutter untouched.
  - the runtime glue, written in Mojang mappings in the newest (26.2) shape.
    Version deltas are Stonecutter comments; loader deltas are `//? if fabric` /
    `//? if neoforge` guards on the entry points.
- `stonecutter.properties.toml` — every per-version dependency pin.
- `build.fabric.gradle.kts` / `build.neoforge.gradle.kts` — per-loader build logic.

Version boundaries encoded in the source:

- `>=1.21.9` — input records (MouseButtonInfo/KeyEvent/CharacterEvent), keybind
  Category, HudElementRegistry (older versions: HudRenderCallback).
- `>=1.21.11` — official mappings renamed ResourceLocation → Identifier.
- `>=26.1` — GuiGraphics → GuiGraphicsExtractor + extractRenderState,
  `Window.handle()`, scaled mouse coords, fabric KeyMappingHelper.
- `>=26.2` — `Minecraft.screen` moved behind `gui.screen()`.

## Building

```
./gradlew chiseledBuild                             # every version + loader
./gradlew :1.21.1-fabric:build                      # one target
./gradlew "Set active project to 1.21.1-neoforge"   # switch the IDE-visible version
./gradlew :1.21.1-neoforge:runClient                # run one target
```

## License

MIT.
