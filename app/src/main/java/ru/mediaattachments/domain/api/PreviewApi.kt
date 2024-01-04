package ru.mediaattachments.domain.api

import okhttp3.ResponseBody
import retrofit2.Call

private const val mediaBucket = "media"

interface PreviewApi {
    fun loadPreview(
        key: String,
        bucket: String = mediaBucket,
        resize: String? = null,
    ): Call<ResponseBody>
}