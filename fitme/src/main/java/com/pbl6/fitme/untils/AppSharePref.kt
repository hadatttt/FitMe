package com.pbl6.fitme.untils

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

class AppSharePref(private val context: Context) {
    companion object {
        private const val PREF_LAST_BATTLE_DATE = "lastBattleDate"
        private const val PREF_BATTLES_TODAY = "battlesToday"
        private const val PREF_IS_RATE_APP = "isRateApp"
        private const val PREF_SESSION_SHOW_RATE_APP = "sessionShowRateApp"
        private const val PREF_SESSION_VALID_SHOW_RATE_APP = "sessionValidShowRateApp"

        private const val PREF_DIAMOND_COUNT = "diamondCount"
        private const val PREF_BASE_URL = "base_url"

        private const val PREF_SERVER_IP = "server_ip"
        const val DEFAULT_IP = "10.48.170.90:8080"

        private const val PREF_WIN_COUNT = "winCount"
        private const val PREF_TOTAL_MATCHES = "totalMatches"
        private const val PREF_ITEM_COUNT = "itemCount"
        private const val PREF_RANKING = "ranking"
        private const val PREF_NAME = "playerName"
        private val KEY_UNLOCKED_FEATURES = "unlockedFeatures"
        private const val PREF_BOT_RANKING_MAP = "botRankingMap"
        private const val PREF_CURRENT_STREAK = "currentStreak"
        private const val PREF_MAX_STREAK = "maxStreak"
        private const val PREF_LAST_STREAK_DATE = "lastStreakDate"
        private const val PREF_SPIN = "spinCount"

        private const val PREF_MAGIC = "magicCount"

        private const val PREF_RELOAD = "reloadCount"
        private const val PREF_SEARCH = "searchCount"
        private const val PREF_NAME_DAILY = "daily_pref"
        private const val KEY_SAVED_DATE = "saved_date"
        private const val PREF_REWARD_LEVEL = "rewardLevel"
    }

    private val sharePref by lazy {
        context.getSharedPreferences("TrackingSharePref", Context.MODE_PRIVATE)
    }
    var serverIp: String
        get() = sharePref.getString(PREF_SERVER_IP, DEFAULT_IP) ?: DEFAULT_IP
        set(value) {
            sharePref.edit { putString(PREF_SERVER_IP, value.trim()) }
        }
    val apiUrl: String
        get() {
            var ip = serverIp
            // Phòng hờ user nhập cả "http://" thì xóa đi để tránh lỗi double
            ip = ip.replace("http://", "").replace("https://", "").replace("/", "")

            return "http://$ip/api/"
        }
    var playerName: String
        get() = sharePref.getString(PREF_NAME, "Player") ?: "Player"
        set(value) {
            sharePref.edit { putString(PREF_NAME, value) }
        }
    var unlockFeature: String
        get() = sharePref.getString(PREF_NAME, "Player") ?: "Player"
        set(value) {
            sharePref.edit { putString(PREF_NAME, value) }
        }

    var diamondCount: Int
        get() = sharePref.getInt(PREF_DIAMOND_COUNT, 0)
        set(value) {
            sharePref.edit { putInt(PREF_DIAMOND_COUNT, value) }
        }
    var magicCount: Int
        get() = sharePref.getInt(PREF_MAGIC, 5)
        set(value) {
            sharePref.edit { putInt(PREF_MAGIC, value) }
        }
    var searchCount: Int
        get() = sharePref.getInt(PREF_SEARCH, 3)
        set(value) {
            sharePref.edit { putInt(PREF_SEARCH, value) }
        }
    var reloadCount: Int
        get() = sharePref.getInt(PREF_RELOAD, 5)
        set(value) {
            sharePref.edit { putInt(PREF_RELOAD, value) }
        }
    var spinCount: Int
        get() = sharePref.getInt(PREF_SPIN, 5)
        set(value) {
            sharePref.edit { putInt(PREF_SPIN, value) }
        }

    var isRateApp: Boolean
        get() = sharePref.getBoolean(PREF_IS_RATE_APP, false)
        set(value) {
            sharePref.edit { putBoolean(PREF_IS_RATE_APP, value) }
        }

    var sessionShowRateApp: Int
        get() = sharePref.getInt(PREF_SESSION_SHOW_RATE_APP, -1)
        set(value) {
            sharePref.edit { putInt(PREF_SESSION_SHOW_RATE_APP, value) }
        }

    var sessionValidShowRateApp: Int
        get() = sharePref.getInt(PREF_SESSION_VALID_SHOW_RATE_APP, -1)
        set(value) {
            sharePref.edit { putInt(PREF_SESSION_VALID_SHOW_RATE_APP, value) }
        }

    var winCount: Int
        get() = sharePref.getInt(PREF_WIN_COUNT, 0)
        set(value) {
            sharePref.edit { putInt(PREF_WIN_COUNT, value) }
        }

    var totalMatches: Int
        get() = sharePref.getInt(PREF_TOTAL_MATCHES, 0)
        set(value) {
            sharePref.edit { putInt(PREF_TOTAL_MATCHES, value) }
        }

    var itemCount: Int
        get() = sharePref.getInt(PREF_ITEM_COUNT, 0)
        set(value) {
            sharePref.edit { putInt(PREF_ITEM_COUNT, value) }
        }

    var ranking: Int
        get() = sharePref.getInt(PREF_RANKING, 1000)
        set(value) {
            sharePref.edit { putInt(PREF_RANKING, value) }
        }

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var currentStreak: Int
        get() = sharePref.getInt(PREF_CURRENT_STREAK, 0)
        set(value) = sharePref.edit { putInt(PREF_CURRENT_STREAK, value) }

    var maxStreak: Int
        get() = sharePref.getInt(PREF_MAX_STREAK, 0)
        set(value) = sharePref.edit { putInt(PREF_MAX_STREAK, value) }

    var lastStreakDate: String
        get() = sharePref.getString(PREF_LAST_STREAK_DATE, sdf.format(Date()))
            ?: sdf.format(Date())
        set(value) = sharePref.edit { putString(PREF_LAST_STREAK_DATE, value) }
    var battlesToday: Int
        get() {
            val today = sdf.format(Date())
            val lastDate = sharePref.getString(PREF_LAST_BATTLE_DATE, today) ?: today

            if (lastDate != today) {
                sharePref.edit {
                    putString(PREF_LAST_BATTLE_DATE, today)
                    putInt(PREF_BATTLES_TODAY, 0)
                }
                return 0
            }
            return sharePref.getInt(PREF_BATTLES_TODAY, 0)
        }
        set(value) {
            val today = sdf.format(Date())
            sharePref.edit {
                putString(PREF_LAST_BATTLE_DATE, today)
                putInt(PREF_BATTLES_TODAY, value)
            }
        }

    fun addBattleToday() {
        battlesToday = battlesToday + 1
    }

    val winRate: Float
        get() = if (totalMatches > 0) (winCount.toFloat() / totalMatches) * 100 else 0f

    fun getBotRankingMap(): Map<String, Int> {
        val jsonString = sharePref.getString(PREF_BOT_RANKING_MAP, null) ?: return emptyMap()

        val map = mutableMapOf<String, Int>()
        try {
            val json = JSONObject(jsonString)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.getInt(key)
            }
        } catch (e: Exception) {
        }

        return map.entries.sortedByDescending { it.value }.associate { it.key to it.value }
    }

    fun saveBotRankingMap(bots: Map<String, Int>) {
        val json = JSONObject(bots)
        sharePref.edit { putString(PREF_BOT_RANKING_MAP, json.toString()) }
    }

    fun getUnlockedFeatures(): Set<String> {
        return sharePref.getStringSet(KEY_UNLOCKED_FEATURES, emptySet()) ?: emptySet()
    }

    fun saveUnlockedFeatures(features: Set<String>) {
        sharePref.edit { putStringSet(KEY_UNLOCKED_FEATURES, features) }
    }

    fun addUnlockedFeature(featureId: String) {
        val current = getUnlockedFeatures().toMutableSet()
        current.add(featureId)
        saveUnlockedFeatures(current)
    }

    fun isFeatureUnlocked(featureId: String): Boolean {
        return getUnlockedFeatures().contains(featureId)
    }

    fun clearUnlockedFeatures() {
        sharePref.edit { remove(KEY_UNLOCKED_FEATURES) }
    }

    fun saveToday(context: Context) {
        val today = LocalDate.now().toString() // yyyy-MM-dd
        context.getSharedPreferences(PREF_NAME_DAILY, Context.MODE_PRIVATE).edit {
                putString(KEY_SAVED_DATE, today)
            }
    }

    fun isTodaySaved(context: Context): Boolean {
        val prefs = context.getSharedPreferences(
            PREF_NAME_DAILY, Context.MODE_PRIVATE
        )
        val savedDate = prefs.getString(KEY_SAVED_DATE, null)
        val today = LocalDate.now().toString()
        return savedDate == today
    }
    var rewardLevel: Int
        get() = sharePref.getInt(PREF_REWARD_LEVEL, 0)
        set(value) {
            sharePref.edit { putInt(PREF_REWARD_LEVEL, value) }
        }
}
