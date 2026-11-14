package com.fisalia.files.data.local

import android.content.Context
import com.fisalia.files.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme(val titleRes: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark)
}

class SettingsPreferences private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("fisalia_settings_prefs", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(loadTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, true))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(prefs.getBoolean(KEY_SHOW_HIDDEN, false))
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()

    private val _wallpaperBackground = MutableStateFlow(prefs.getBoolean(KEY_WALLPAPER_BG, false))
    val wallpaperBackground: StateFlow<Boolean> = _wallpaperBackground.asStateFlow()

    private val _isGridView = MutableStateFlow(prefs.getBoolean(KEY_IS_GRID_VIEW, false))
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private fun loadTheme(): AppTheme {
        val themeName = prefs.getString(KEY_APP_THEME, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        return try {
            AppTheme.valueOf(themeName)
        } catch (_: Exception) {
            AppTheme.SYSTEM
        }
    }

    fun setTheme(newTheme: AppTheme) {
        prefs.edit().putString(KEY_APP_THEME, newTheme.name).apply()
        _theme.value = newTheme
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _dynamicColor.value = enabled
    }

    fun setShowHiddenFiles(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_HIDDEN, enabled).apply()
        _showHiddenFiles.value = enabled
    }

    fun setWallpaperBackground(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WALLPAPER_BG, enabled).apply()
        _wallpaperBackground.value = enabled
    }

    fun setIsGridView(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_GRID_VIEW, enabled).apply()
        _isGridView.value = enabled
    }

    fun getFavorites(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun toggleFavorite(path: String) {
        val current = getFavorites().toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    fun getSafeFolderPin(): String? {
        return prefs.getString(KEY_SAFE_PIN, null)
    }

    fun setSafeFolderPin(pin: String) {
        prefs.edit().putString(KEY_SAFE_PIN, pin).apply()
    }

    companion object {
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_SHOW_HIDDEN = "show_hidden_files"
        private const val KEY_WALLPAPER_BG = "wallpaper_background"
        private const val KEY_IS_GRID_VIEW = "view_mode_grid"
        private const val KEY_FAVORITES = "favorite_files"
        private const val KEY_SAFE_PIN = "safe_folder_pin"

        @Volatile
        private var instance: SettingsPreferences? = null

        fun getInstance(context: Context): SettingsPreferences {
            return instance ?: synchronized(this) {
                instance ?: SettingsPreferences(context).also { instance = it }
            }
        }
    }
}
