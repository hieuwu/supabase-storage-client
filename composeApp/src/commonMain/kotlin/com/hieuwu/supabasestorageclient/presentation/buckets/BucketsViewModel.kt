package com.hieuwu.supabasestorageclient.presentation.buckets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.context.ContextSelectionManager
import com.hieuwu.supabasestorageclient.domain.usecase.GetBucketsUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.EmptyBucketUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.DeleteBucketUseCase
import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import com.hieuwu.supabasestorageclient.domain.usecase.RefreshBucketsUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.*
import com.hieuwu.supabasestorageclient.domain.RefreshManager
import com.hieuwu.supabasestorageclient.observability.analytics.AnalyticsSurfaces
import com.hieuwu.supabasestorageclient.observability.analytics.AppAnalytics
import com.hieuwu.supabasestorageclient.observability.analytics.ItemTypes
import com.hieuwu.supabasestorageclient.observability.analytics.StorageActions
import com.hieuwu.supabasestorageclient.observability.analytics.logContentRefreshed
import com.hieuwu.supabasestorageclient.observability.analytics.logItemStarred
import com.hieuwu.supabasestorageclient.observability.analytics.logItemUnstarred
import com.hieuwu.supabasestorageclient.observability.analytics.logStorageAction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class BucketsViewModel(
    private val getBucketsUseCase: GetBucketsUseCase,
    private val refreshBucketsUseCase: RefreshBucketsUseCase,
    private val emptyBucketUseCase: EmptyBucketUseCase,
    private val deleteBucketUseCase: DeleteBucketUseCase,
    private val getStarredItemsUseCase: GetStarredItemsUseCase,
    private val toggleStarUseCase: ToggleStarUseCase,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val contextSelectionManager: ContextSelectionManager,
    private val refreshManager: RefreshManager,
    private val logger: Logger
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    private val _manualState = MutableStateFlow(ManualBucketsState())

    val uiState: StateFlow<BucketsUiState> = combine(
        refreshTrigger.flatMapLatest {
            combine(
                getStarredItemsUseCase(),
                getUserSettingsUseCase(),
                flow { emit(getBucketsUseCase()) }
            ) { stars, settings, result ->
                Triple(stars, settings, result)
            }
        },
        _manualState
    ) { (stars, settings, result), manual ->
        result.fold(
            onSuccess = { buckets ->
                val starredIds = stars.filter { it.isBucket }.map { it.id }.toSet()
                val updatedBuckets = buckets.map { it.copy(isStarred = starredIds.contains(it.id)) }
                BucketsUiState.Content(
                    buckets = updatedBuckets,
                    viewMode = settings.viewMode,
                    showEmptyConfirmation = manual.showEmptyConfirmation,
                    showDeleteConfirmation = manual.showDeleteConfirmation,
                    selectedBucket = manual.selectedBucket,
                    successMessage = manual.successMessage,
                    error = manual.error
                )
            },
            onFailure = { error ->
                BucketsUiState.Error(error.message ?: "Unknown error")
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BucketsUiState.Loading)

    init {
        contextSelectionManager.clearContext()
        viewModelScope.launch {
            refreshManager.refreshBuckets.collect {
                refreshTrigger.emit(Unit)
            }
        }
    }

    fun refreshBuckets() {
        viewModelScope.launch {
            _manualState.update { it.copy(error = null) }
            AppAnalytics.logContentRefreshed(AnalyticsSurfaces.BUCKETS)
            refreshBucketsUseCase()
                .onSuccess {
                    refreshTrigger.emit(Unit)
                }
                .onFailure { error ->
                    logger.e(error) { "Failed to refresh buckets" }
                    _manualState.update { it.copy(error = error.message) }
                }
        }
    }

    fun onEmptyBucketClick(bucket: Bucket) {
        _manualState.update { it.copy(selectedBucket = bucket, showEmptyConfirmation = true) }
    }

    fun onDeleteBucketClick(bucket: Bucket) {
        _manualState.update { it.copy(selectedBucket = bucket, showDeleteConfirmation = true) }
    }

    fun confirmEmptyBucket() {
        val bucketId = _manualState.value.selectedBucket?.id ?: return
        viewModelScope.launch {
            _manualState.update { it.copy(showEmptyConfirmation = false) }
            emptyBucketUseCase(bucketId)
                .onSuccess {
                    AppAnalytics.logStorageAction(StorageActions.EMPTY_BUCKET)
                    _manualState.update {
                        it.copy(
                            successMessage = "Bucket emptied successfully",
                            selectedBucket = null
                        )
                    }
                    refreshTrigger.emit(Unit)
                }
                .onFailure { error ->
                    AppAnalytics.logStorageAction(StorageActions.EMPTY_BUCKET, error)
                    logger.e(error) { "Failed to empty bucket $bucketId" }
                    _manualState.update {
                        it.copy(
                            error = "Failed to empty bucket: ${error.message}",
                            selectedBucket = null
                        )
                    }
                }
        }
    }

    fun confirmDeleteBucket() {
        val bucketId = _manualState.value.selectedBucket?.id ?: return
        viewModelScope.launch {
            _manualState.update { it.copy(showDeleteConfirmation = false) }
            deleteBucketUseCase(bucketId)
                .onSuccess {
                    AppAnalytics.logStorageAction(StorageActions.DELETE_BUCKET)
                    _manualState.update {
                        it.copy(
                            successMessage = "Bucket deleted successfully",
                            selectedBucket = null
                        )
                    }
                    refreshTrigger.emit(Unit)
                }
                .onFailure { error ->
                    AppAnalytics.logStorageAction(StorageActions.DELETE_BUCKET, error)
                    logger.e(error) { "Failed to delete bucket $bucketId" }
                    _manualState.update {
                        it.copy(
                            error = "Failed to delete bucket: ${error.message}",
                            selectedBucket = null
                        )
                    }
                }
        }
    }

    fun dismissDialogs() {
        _manualState.update {
            it.copy(
                showEmptyConfirmation = false,
                showDeleteConfirmation = false,
                selectedBucket = null
            )
        }
    }

    fun clearMessages() {
        _manualState.update { it.copy(successMessage = null, error = null) }
    }

    fun toggleStar(bucket: Bucket) {
        viewModelScope.launch {
            val starredItem = StarredItem(
                id = bucket.id,
                fileName = bucket.name,
                bucketId = bucket.id,
                path = null,
                isFolder = false,
                isBucket = true,
                starredAt = Clock.System.now()
            )
            toggleStarUseCase(starredItem, bucket.isStarred).fold(
                onSuccess = {
                    if (bucket.isStarred) {
                        AppAnalytics.logItemUnstarred(ItemTypes.BUCKET, AnalyticsSurfaces.BUCKETS)
                    } else {
                        AppAnalytics.logItemStarred(ItemTypes.BUCKET, AnalyticsSurfaces.BUCKETS)
                    }
                    val message = if (bucket.isStarred) "Unstarred successfully" else "Starred successfully"
                    _manualState.update { it.copy(successMessage = message) }
                },
                onFailure = { error ->
                    _manualState.update { it.copy(error = error.message) }
                }
            )
        }
    }
}

data class ManualBucketsState(
    val showEmptyConfirmation: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val selectedBucket: Bucket? = null,
    val successMessage: String? = null,
    val error: String? = null
)
