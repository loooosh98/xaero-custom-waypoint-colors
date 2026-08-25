plugins {
    id("net.fabricmc.fabric-loom") version "1.16.1" apply false
}

extra["MINECRAFT_VERSION"]     = "26.2"
extra["FABRIC_LOADER_VERSION"] = "0.19.2"
extra["FABRIC_API_VERSION"]    = "0.152.1+26.2"

extra["MINIMAP_VERSION"]       = "26.2.0"
extra["WORLDMAP_VERSION"]      = "1.42.0"

extra["MOD_VERSION"]           = "1.0.6-26.2"
extra["MAVEN_GROUP"]           = "com.xaerocustomcolors"
extra["ARCHIVE_NAME"]          = "xaero-custom-waypoint-colors"

subprojects {
    apply(plugin = "java")

    val MOD_VERSION: String by rootProject.extra
    val MAVEN_GROUP: String by rootProject.extra

    group = MAVEN_GROUP
    version = MOD_VERSION

    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://api.modrinth.com/maven")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(25)
    }

    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
}
