package com.wordcraft.game.data.repository

import android.content.Context
import android.content.SharedPreferences

class GamePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wordcraft_prefs", Context.MODE_PRIVATE)

    var highestUnlockedLevel: Int
        get() = prefs.getInt(KEY_HIGHEST_UNLOCKED, 1)
        set(value) = prefs.edit().putInt(KEY_HIGHEST_UNLOCKED, value).apply()

    var currentLevelId: Int
        get() = prefs.getInt(KEY_CURRENT_LEVEL, 1)
        set(value) = prefs.edit().putInt(KEY_CURRENT_LEVEL, value).apply()

    var coins: Int
        get() = prefs.getInt(KEY_COINS, 200)
        set(value) = prefs.edit().putInt(KEY_COINS, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS, value).apply()

    fun getBonusWordsFound(levelId: Int): Set<String> {
        return prefs.getStringSet("${KEY_BONUS_WORDS}_$levelId", emptySet()) ?: emptySet()
    }

    fun addBonusWord(levelId: Int, word: String) {
        val current = getBonusWordsFound(levelId).toMutableSet()
        current.add(word)
        prefs.edit().putStringSet("${KEY_BONUS_WORDS}_$levelId", current).apply()
    }

    fun isLevelCompleted(levelId: Int): Boolean {
        return prefs.getBoolean("${KEY_COMPLETED}_$levelId", false)
    }

    fun setLevelCompleted(levelId: Int, completed: Boolean) {
        prefs.edit().putBoolean("${KEY_COMPLETED}_$levelId", completed).apply()
    }

    companion object {
        private const val KEY_HIGHEST_UNLOCKED = "highest_unlocked_level"
        private const val KEY_CURRENT_LEVEL = "current_level_id"
        private const val KEY_COINS = "coins"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_BONUS_WORDS = "bonus_words"
        private const val KEY_COMPLETED = "completed"
    }
}
