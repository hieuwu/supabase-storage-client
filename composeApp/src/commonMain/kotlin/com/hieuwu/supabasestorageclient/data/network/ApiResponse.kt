package com.hieuwu.supabasestorageclient.data.network

sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error(val exception: Throwable) : ApiResponse<Nothing>()
    object Loading : ApiResponse<Nothing>()

    companion object {
        fun <T> of(action: () -> T): ApiResponse<T> {
            return try {
                Success(action())
            } catch (e: Exception) {
                Error(e)
            }
        }
        fun exception(e: Throwable): ApiResponse<Nothing> = Error(e)
    }
}
