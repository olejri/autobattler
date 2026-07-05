package com.autobattler.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter

/**
 * Centralized font factory using FreeType for crisp, pixel-perfect text at any size.
 *
 * Usage:
 *   val font = Fonts.create(20)          // 20px regular
 *   val bold = Fonts.create(28, bold=true) // 28px bolder
 *
 * All fonts created here must be disposed individually or via [disposeAll].
 */
object Fonts {

    private const val FONT_PATH = "fonts/segoeui.ttf"
    private const val FALLBACK_FONT_PATH = "fonts/OpenSans-Regular.ttf"

    private val generated = mutableListOf<BitmapFont>()
    private var generator: FreeTypeFontGenerator? = null

    /**
     * Create a crisp BitmapFont at [size] pixels.
     * [borderWidth] adds a subtle outline for readability on dark backgrounds.
     */
    fun create(
        size: Int,
        color: Color = Color.WHITE,
        borderWidth: Float = 0f,
        borderColor: Color = Color.BLACK
    ): BitmapFont {
        val gen = getGenerator()
        return if (gen != null) {
            val param = FreeTypeFontParameter().apply {
                this.size = size
                this.color = color
                this.borderWidth = borderWidth
                this.borderColor = borderColor
                this.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
                this.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
                this.genMipMaps = false
                this.shadowOffsetX = 0
                this.shadowOffsetY = 0
            }
            val font = gen.generateFont(param)
            generated.add(font)
            font
        } else {
            // Fallback: BitmapFont scaled (blurry but functional)
            BitmapFont().apply {
                data.setScale(size / 15f)
                this.color = color
                generated.add(this)
            }
        }
    }

    private fun getGenerator(): FreeTypeFontGenerator? {
        if (generator != null) return generator
        val paths = listOf(FONT_PATH, FALLBACK_FONT_PATH)
        for (path in paths) {
            try {
                val file = Gdx.files.internal(path)
                if (file.exists()) {
                    generator = FreeTypeFontGenerator(file)
                    return generator
                }
                // Try classpath
                val cp = Gdx.files.classpath(path)
                if (cp.exists()) {
                    generator = FreeTypeFontGenerator(cp)
                    return generator
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun disposeAll() {
        generated.forEach { it.dispose() }
        generated.clear()
        generator?.dispose()
        generator = null
    }
}

