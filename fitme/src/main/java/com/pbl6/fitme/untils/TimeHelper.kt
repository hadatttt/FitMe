package com.pbl6.fitme.untils


class TimerHelper(private val durationMillis: Long = 30000L, private val limitAds: Int = 20) {

    private var startTime: Long = System.currentTimeMillis()
    private var countReload = 0
    var isTimeReached: Boolean = false
        private set

    fun checkTimeReached(): Boolean {
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed >= durationMillis && !isTimeReached && countReload <= limitAds) {
            isTimeReached = false
            countReload += 1
            resetTimer()
            return true
        }
        return isTimeReached
    }

    fun resetTimer() {
        startTime = System.currentTimeMillis()
    }
}
