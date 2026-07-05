val gdxVersion = "1.12.1"

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    api("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}

plugins {
    kotlin("plugin.serialization") version "1.9.22"
}

kotlin {
    jvmToolchain(17)
}

