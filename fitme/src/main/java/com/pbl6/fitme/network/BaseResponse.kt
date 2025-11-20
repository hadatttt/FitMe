package com.pbl6.fitme.network

// Backend is inconsistent: some endpoints return `result`, others return `data`.
// Keep both fields nullable and prefer `result` when present.
data class BaseResponse<T>(
    val code: Int,
    val message: String,
    val result: T? = null,
    val data: T? = null
)
