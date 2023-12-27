package ru.scheduled.mediaattachmentslibrary

import okhttp3.ResponseBody
import retrofit2.Call

private const val mediaBucket = "media"

interface PreviewApi {
    fun loadPreview(
        bucket: String = mediaBucket,
        key: String,
        resize: String? = null,
    ): Call<ResponseBody>
}

