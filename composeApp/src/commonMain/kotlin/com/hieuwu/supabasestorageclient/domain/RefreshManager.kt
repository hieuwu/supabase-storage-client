package com.hieuwu.supabasestorageclient.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class RefreshManager {
    private val _refreshBuckets = MutableSharedFlow<Unit>()
    val refreshBuckets: SharedFlow<Unit> = _refreshBuckets.asSharedFlow()

    suspend fun triggerRefreshBuckets() {
        _refreshBuckets.emit(Unit)
    }
}
