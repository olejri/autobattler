plugins {
    kotlin("jvm") version "1.9.22" apply false
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://jitpack.io")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        val implementation by configurations
        implementation(kotlin("stdlib"))
    }
}

