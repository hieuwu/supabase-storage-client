package com.hieuwu.supabasestorageclient.domain.context

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BucketContext(
    val bucketId: String,
    val path: String = ""
)

class ContextSelectionManager {
    private val _currentContext = MutableStateFlow<BucketContext?>(null)
    val currentContext: StateFlow<BucketContext?> = _currentContext.asStateFlow()

    fun setContext(bucketId: String, path: String) {
        _currentContext.value = BucketContext(bucketId, path)
    }

    fun clearContext() {
        _currentContext.value = null
    }
}
