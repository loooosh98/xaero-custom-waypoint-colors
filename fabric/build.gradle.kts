plugins {
    id("net.fabricmc.fabric-loom")
}

val mod_version: String by project
val archives_base_name: String by project
val minecraft_version: String by project
val loader_version: String by project
val fabric_version: String by project
val minimap_version: String by project
val worldmap_version: String by project

version = mod_version

base {
    archivesName.set(archives_base_name)
}

val common = project(":common")

val xaerolibSource: Configuration by configurations.creating

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    implementation("net.fabricmc:fabric-loader:$loader_version")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")

    compileOnly("maven.modrinth:xaeros-minimap:fabric-$minecraft_version-$minimap_version")
    compileOnly("maven.modrinth:xaeros-world-map:fabric-$minecraft_version-$worldmap_version")

    compileOnly(common)

    xaerolibSource("maven.modrinth:xaeros-minimap:fabric-$minecraft_version-$minimap_version")
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

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minimap_version", minimap_version)

    from(common.file("src/main/resources"))

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minimap_version" to minimap_version
        )
    }
}

tasks.jar {
    from(common.sourceSets["main"].output.classesDirs)

    from(rootProject.file("LICENSE")) {
        rename { "${it}_$archives_base_name" }
    }
}

java {
    withSourcesJar()
}

tasks.named<Jar>("sourcesJar") {
    from(common.sourceSets["main"].allSource)
}
