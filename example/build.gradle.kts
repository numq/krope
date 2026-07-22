plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.library.core)
                implementation(projects.library.text)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val jvmMain by getting {}
    }
}