rootProject.name = "krope"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":benchmark")
project(":benchmark").projectDir = file("benchmark")

include(":example")
project(":example").projectDir = file("example")

include(":library:core")
project(":library:core").projectDir = file("library/core")

include(":library:diff")
project(":library:diff").projectDir = file("library/diff")

include(":library:io")
project(":library:io").projectDir = file("library/io")

include(":library:text")
project(":library:text").projectDir = file("library/text")