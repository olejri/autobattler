package com.autobattler.model

/**
 * Abstraction over where saves are stored.
 * Swap implementations for local vs. Steam Cloud.
 */
interface SaveManager {
    fun save(commander: Commander)
    fun load(): Commander?
    fun hasSave(): Boolean
    fun deleteSave()
}

