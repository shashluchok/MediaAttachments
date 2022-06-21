package ru.scheduled.mediaattachmentslibrary

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PreviewApi {
    abstract fun loadPreview(
        bucket: String = "media",
        key: String,
        resize: String? = null,
    ): Call<ResponseBody>
}

