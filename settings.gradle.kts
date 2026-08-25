pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"

    // Applies the right Loom variant per Minecraft version (26.1+ vs older) in one build.
    id("dev.kikugie.loom-back-compat") version "0.4.2"

    // Resolves JDK toolchains automatically (21 for 1.21.x, 25 for 26.x).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        /**
         * Creates version nodes for multiple loaders: subprojects named
         * `versions/{project}-{loader}`, each using `build.{loader}.gradle.kts`.
         */
        fun match(project: String, vararg loaders: String, version: String = project) {
            for (loader in loaders) version("$project-$loader", version).buildscript("build.$loader.gradle.kts")
        }

        match("1.21", "fabric", "neoforge")
        match("1.21.1", "fabric", "neoforge")
        match("1.21.2", "fabric")                    // NeoForge skipped MC 1.21.2 entirely
        match("1.21.3", "fabric", "neoforge")
        match("1.21.4", "fabric", "neoforge")
        match("1.21.5", "fabric", "neoforge")
        match("1.21.6", "fabric", "neoforge")
        match("1.21.7", "fabric", "neoforge")
        match("1.21.8", "fabric", "neoforge")
        match("1.21.9", "fabric", "neoforge")
        match("1.21.10", "fabric", "neoforge")
        match("1.21.11", "fabric", "neoforge")
        match("26.1", "fabric", "neoforge", version = "26.1.2")
        match("26.2", "fabric", "neoforge")
        vcsVersion = "26.2-fabric"
    }
}

rootProject.name = "pane"
