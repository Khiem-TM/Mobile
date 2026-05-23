package com.vitalai.data.remote

import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.data.remote.model.BlogPageDto
import com.vitalai.data.remote.model.CreateBlogRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST
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

    @GET("user/blogs")
    suspend fun getMyBlogs(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<ApiResponse<BlogPageDto>>

    @POST("user/blogs")
    suspend fun createBlog(@Body request: CreateBlogRequest): Response<ApiResponse<BlogDto>>

    @PATCH("user/blogs/{id}")
    suspend fun updateBlog(
        @Path("id") id: String,
        @Body request: CreateBlogRequest
    ): Response<ApiResponse<BlogDto>>

    @DELETE("user/blogs/{id}")
    suspend fun deleteMyBlog(@Path("id") id: String): Response<Unit>
}
