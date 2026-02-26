package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.domain.model.AppTheme
import com.hieuwu.supabasestorageclient.domain.model.SizeUnit
import com.hieuwu.supabasestorageclient.domain.model.UserSettings
import com.hieuwu.supabasestorageclient.domain.model.ViewMode
import com.hieuwu.supabasestorageclient.domain.repository.SettingsRepository
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepositoryImpl(
    private val settings: Settings
) : SettingsRepository {

    companion object {
        private const val KEY_FILE_SIZE_LIMIT = "file_size_limit"
        private const val KEY_FILE_SIZE_UNIT = "file_size_unit"
        private const val KEY_VIEW_MODE = "view_mode"
        private const val KEY_THEME = "theme"

        private const val DEFAULT_FILE_SIZE_LIMIT = 10L
        private val DEFAULT_FILE_SIZE_UNIT = SizeUnit.MEGABYTES
        private val DEFAULT_VIEW_MODE = ViewMode.LIST
        private val DEFAULT_THEME = AppTheme.SYSTEM
    }

    private val _settingsFlow = MutableStateFlow(readSettings())

    override fun getSettings(): Flow<UserSettings> = _settingsFlow.asStateFlow()

    override suspend fun updateSettings(userSettings: UserSettings) {
        settings[KEY_FILE_SIZE_LIMIT] = userSettings.fileSizeLimit
        settings[KEY_FILE_SIZE_UNIT] = userSettings.fileSizeUnit.name
        settings[KEY_VIEW_MODE] = userSettings.viewMode.name
        settings[KEY_THEME] = userSettings.theme.name
        _settingsFlow.value = userSettings
    }

    private fun readSettings(): UserSettings {
        val fileSizeLimit = settings.getLong(KEY_FILE_SIZE_LIMIT, DEFAULT_FILE_SIZE_LIMIT)
        val fileSizeUnit = try {
            SizeUnit.valueOf(settings.getString(KEY_FILE_SIZE_UNIT, DEFAULT_FILE_SIZE_UNIT.name))
        } catch (e: Exception) {
            DEFAULT_FILE_SIZE_UNIT
        }
        val viewMode = try {
            ViewMode.valueOf(settings.getString(KEY_VIEW_MODE, DEFAULT_VIEW_MODE.name))
        } catch (e: Exception) {
            DEFAULT_VIEW_MODE
        }
        val theme = try {
            AppTheme.valueOf(settings.getString(KEY_THEME, DEFAULT_THEME.name))
        } catch (e: Exception) {
            DEFAULT_THEME
        }

        return UserSettings(
            fileSizeLimit = fileSizeLimit,
            fileSizeUnit = fileSizeUnit,
            viewMode = viewMode,
            theme = theme
        )
    }
}
