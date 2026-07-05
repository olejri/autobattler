package com.autobattler.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.autobattler.AutobattlerGame

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Autobattler")
        setWindowedMode(1920, 1080)
        setForegroundFPS(60)
        useVsync(true)
    }
    Lwjgl3Application(AutobattlerGame(), config)
}

