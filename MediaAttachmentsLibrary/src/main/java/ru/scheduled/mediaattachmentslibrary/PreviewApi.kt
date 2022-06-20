package ru.scheduled.mediaattachmentslibrary

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface PreviewApi {
    @GET
    abstract fun loadPreview(
        @Query("bucket") bucket: String = "media",
        @Query("key") key: String,
        @Query("resize") resize: String? = null,
    ): Call<ResponseBody>
}