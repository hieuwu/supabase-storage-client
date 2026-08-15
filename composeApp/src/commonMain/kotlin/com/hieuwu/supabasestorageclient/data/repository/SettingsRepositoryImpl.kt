package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.domain.model.AppTheme
import com.hieuwu.supabasestorageclient.domain.model.SizeUnit
import com.hieuwu.supabasestorageclient.domain.model.UserSettings
import com.hieuwu.supabasestorageclient.domain.model.ViewMode
import com.hieuwu.supabasestorageclient.domain.model.AskDownloadPathConfig
import com.hieuwu.supabasestorageclient.domain.repository.SettingsRepository
import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepositoryImpl(
    private val settings: Settings,
    private val logger: Logger
) : SettingsRepository {

    companion object {
        private const val KEY_FILE_SIZE_LIMIT = "file_size_limit"
        private const val KEY_FILE_SIZE_UNIT = "file_size_unit"
        private const val KEY_VIEW_MODE = "view_mode"
        private const val KEY_THEME = "theme"
        private const val KEY_FIRST_OPERATION_COMPLETED = "first_operation_completed"
        private const val KEY_ASK_DOWNLOAD_PATH_CONFIG = "ask_download_path_config"
        private const val KEY_DEFAULT_DOWNLOAD_DIRECTORY = "default_download_directory"
        private const val KEY_SESSION_DOWNLOAD_DIRECTORY = "session_download_directory"

        private const val DEFAULT_FILE_SIZE_LIMIT = 10L
        private val DEFAULT_FILE_SIZE_UNIT = SizeUnit.MEGABYTES
        private val DEFAULT_VIEW_MODE = ViewMode.LIST
        private val DEFAULT_THEME = AppTheme.SYSTEM
        private val DEFAULT_ASK_DOWNLOAD_PATH_CONFIG = AskDownloadPathConfig.ASK_EVERYTIME
    }

    init {
        // Clear sessionDownloadDirectory on app launch
        runCatching { settings.remove(KEY_SESSION_DOWNLOAD_DIRECTORY) }
            .onFailure { error ->
                logger.e(error) { "Failed to clear session download directory on launch" }
            }
    }

    private val _settingsFlow = MutableStateFlow(readSettings())

    override fun getSettings(): Flow<UserSettings> = _settingsFlow.asStateFlow()

    /**
     * The emitted flow value is only updated once the write actually landed, so the UI never shows
     * a setting that was not persisted.
     */
    override suspend fun updateSettings(userSettings: UserSettings): Result<Unit> =
        runCatching {
            settings[KEY_FILE_SIZE_LIMIT] = userSettings.fileSizeLimit
            settings[KEY_FILE_SIZE_UNIT] = userSettings.fileSizeUnit.name
            settings[KEY_VIEW_MODE] = userSettings.viewMode.name
            settings[KEY_THEME] = userSettings.theme.name
            settings[KEY_FIRST_OPERATION_COMPLETED] = userSettings.isFirstOperationCompleted
            settings[KEY_ASK_DOWNLOAD_PATH_CONFIG] = userSettings.askDownloadPathConfig.name
            userSettings.defaultDownloadDirectory?.let {
                settings[KEY_DEFAULT_DOWNLOAD_DIRECTORY] = it
            } ?: settings.remove(KEY_DEFAULT_DOWNLOAD_DIRECTORY)
            userSettings.sessionDownloadDirectory?.let {
                settings[KEY_SESSION_DOWNLOAD_DIRECTORY] = it
            } ?: settings.remove(KEY_SESSION_DOWNLOAD_DIRECTORY)
        }.onSuccess {
            _settingsFlow.value = userSettings
        }.onFailure { error ->
            logger.e(error) { "Failed to persist user settings" }
        }

    /**
     * Every read falls back to its default: a stored value can be missing, of the wrong type, or
     * name an enum constant that no longer exists after an app update.
     */
    private fun readSettings(): UserSettings = UserSettings(
        fileSizeLimit = read("fileSizeLimit", DEFAULT_FILE_SIZE_LIMIT) {
            settings.getLong(KEY_FILE_SIZE_LIMIT, DEFAULT_FILE_SIZE_LIMIT)
        },
        fileSizeUnit = read("fileSizeUnit", DEFAULT_FILE_SIZE_UNIT) {
            SizeUnit.valueOf(settings.getString(KEY_FILE_SIZE_UNIT, DEFAULT_FILE_SIZE_UNIT.name))
        },
        viewMode = read("viewMode", DEFAULT_VIEW_MODE) {
            ViewMode.valueOf(settings.getString(KEY_VIEW_MODE, DEFAULT_VIEW_MODE.name))
        },
        theme = read("theme", DEFAULT_THEME) {
            AppTheme.valueOf(settings.getString(KEY_THEME, DEFAULT_THEME.name))
        },
        isFirstOperationCompleted = read("isFirstOperationCompleted", false) {
            settings.getBoolean(KEY_FIRST_OPERATION_COMPLETED, false)
        },
        askDownloadPathConfig = read("askDownloadPathConfig", DEFAULT_ASK_DOWNLOAD_PATH_CONFIG) {
            AskDownloadPathConfig.valueOf(
                settings.getString(KEY_ASK_DOWNLOAD_PATH_CONFIG, DEFAULT_ASK_DOWNLOAD_PATH_CONFIG.name)
            )
        },
        defaultDownloadDirectory = read<String?>("defaultDownloadDirectory", null) {
            settings.getStringOrNull(KEY_DEFAULT_DOWNLOAD_DIRECTORY)
        },
        sessionDownloadDirectory = read<String?>("sessionDownloadDirectory", null) {
            settings.getStringOrNull(KEY_SESSION_DOWNLOAD_DIRECTORY)
        }
    )

    private fun <T> read(name: String, default: T, block: () -> T): T =
        runCatching(block).getOrElse { error ->
            logger.w(error) { "Invalid stored value for $name, falling back to $default" }
            default
        }
}
