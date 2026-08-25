plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    // Makes version- and loader-specific properties apply from `stonecutter.properties.toml`.
    properties {
        tags(version, loader)
    }

    // Adds constants for Stonecutter comments (i.e. `//? if fabric {`).
    constants {
        match(loader, "fabric", "neoforge")
    }

    swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"

    replacements {
        // Official mappings renamed ResourceLocation to Identifier in 1.21.11.
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }

        // The 26.x rendering rework: HUD/screen drawing goes through the extractor.
        // Deliberately surgical — a bare "GuiGraphics" rule would also mangle
        // NeoForge's getGuiGraphics() getter, which kept its name.
        string(current.parsed >= "26.1") {
            replace(".gui.GuiGraphics;", ".gui.GuiGraphicsExtractor;")
        }
        string(current.parsed >= "26.1") {
            replace("GuiGraphics ctx", "GuiGraphicsExtractor ctx")
        }

        // 26.2 moved the current-screen field behind the Gui accessor.
        string(current.parsed >= "26.2") {
            replace("Minecraft.getInstance().screen", "Minecraft.getInstance().gui.screen()")
        }
        string(current.parsed >= "26.2") {
            replace("mc.screen", "mc.gui.screen()")
        }
    }
}
