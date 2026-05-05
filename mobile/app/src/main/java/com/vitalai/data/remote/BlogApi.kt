package com.vitalai.data.remote

import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.data.remote.model.BlogPageDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BlogApi {
    @GET("blogs")
    suspend fun getBlogs(
        @Query("tag") tag: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<ApiResponse<BlogPageDto>>

    @GET("blogs/{id}")
    suspend fun getBlogById(@Path("id") id: String): Response<ApiResponse<BlogDto>>
}
