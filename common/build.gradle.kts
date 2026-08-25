plugins {
    id("net.fabricmc.fabric-loom")
}

val MINECRAFT_VERSION: String by rootProject.extra
val FABRIC_LOADER_VERSION: String by rootProject.extra
val MINIMAP_VERSION: String by rootProject.extra
val WORLDMAP_VERSION: String by rootProject.extra
val ARCHIVE_NAME: String by rootProject.extra

base {
    archivesName.set("$ARCHIVE_NAME-common")
}

val xaerolibSource: Configuration by configurations.creating

dependencies {
    minecraft("com.mojang:minecraft:$MINECRAFT_VERSION")
    implementation("net.fabricmc:fabric-loader:$FABRIC_LOADER_VERSION")

    compileOnly("maven.modrinth:xaeros-minimap:fabric-$MINECRAFT_VERSION-$MINIMAP_VERSION")
    compileOnly("maven.modrinth:xaeros-world-map:fabric-$MINECRAFT_VERSION-$WORLDMAP_VERSION")

    xaerolibSource("maven.modrinth:xaeros-minimap:fabric-$MINECRAFT_VERSION-$MINIMAP_VERSION")
}

val unpackXaerolib by tasks.registering(Sync::class) {
    from(provider { xaerolibSource.map { zipTree(it).matching { include("META-INF/jars/xaerolib-*.jar") } } })
    eachFile { relativePath = RelativePath(true, name) }
    includeEmptyDirs = false
    into(layout.buildDirectory.dir("xaerolib"))
}

dependencies {
    compileOnly(fileTree(layout.buildDirectory.dir("xaerolib")) {
        include("*.jar")
        builtBy(unpackXaerolib)
    })
}
