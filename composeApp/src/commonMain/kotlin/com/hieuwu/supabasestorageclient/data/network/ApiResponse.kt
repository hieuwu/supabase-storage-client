package com.hieuwu.supabasestorageclient.data.network

sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error(val exception: Throwable) : ApiResponse<Nothing>()
    object Loading : ApiResponse<Nothing>()

    companion object {
        fun <T> of(action: () -> T): ApiResponse<T> =
            runCatching(action).fold(
                onSuccess = { Success(it) },
                onFailure = { Error(it) }
            )

        /** Bridges a [Result] produced by `runCatching` into an [ApiResponse]. */
        fun <T> from(result: Result<T>): ApiResponse<T> = result.fold(
            onSuccess = { Success(it) },
            onFailure = { Error(it) }
        )
        fun exception(e: Throwable): ApiResponse<Nothing> = Error(e)
    }
}
