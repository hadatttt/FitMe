package com.pbl6.fitme.settings

enum class SettingType {
    TITLE_ONLY,
    TITLE_WITH_VALUE
}

data class SettingOption(
    val title: String,
    val value: String? = null,
    val type: SettingType = SettingType.TITLE_ONLY,
    val onClick: (() -> Unit)? = null
)