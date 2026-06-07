package com.vitalai.data.remote

import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.data.remote.model.BlogLikedDto
import com.vitalai.data.remote.model.BlogPageDto
import com.vitalai.data.remote.model.CommentPageDto
import com.vitalai.data.remote.model.CreateCommentRequest
import com.vitalai.data.remote.model.CreateBlogRequest
import com.vitalai.data.remote.model.UpdateCommentRequest
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
        @Query("authorId") authorId: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<ApiResponse<BlogPageDto>>

    @GET("blogs/{id}")
    suspend fun getBlogById(@Path("id") id: String): Response<ApiResponse<BlogDto>>

    @GET("blogs/tags")
    suspend fun getTags(): Response<ApiResponse<List<String>>>

    @POST("blogs/{id}/like")
    suspend fun toggleLike(@Path("id") id: String): Response<ApiResponse<BlogLikedDto>>

    @GET("blogs/{id}/liked")
    suspend fun isLiked(@Path("id") id: String): Response<ApiResponse<BlogLikedDto>>

    @GET("blogs/{id}/comments")
    suspend fun getComments(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<CommentPageDto>>

    @POST("blogs/{id}/comments")
    suspend fun postComment(
        @Path("id") id: String,
        @Body request: CreateCommentRequest
    ): Response<ApiResponse<com.vitalai.data.remote.model.CommentDto>>

    @PATCH("blogs/{id}/comments/{commentId}")
    suspend fun updateComment(
        @Path("id") id: String,
        @Path("commentId") commentId: String,
        @Body request: UpdateCommentRequest
    ): Response<ApiResponse<com.vitalai.data.remote.model.CommentDto>>

    @DELETE("blogs/{id}/comments/{commentId}")
    suspend fun deleteComment(
        @Path("id") id: String,
        @Path("commentId") commentId: String
    ): Response<Unit>

    @GET("user/blogs")
    suspend fun getMyBlogs(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<ApiResponse<BlogPageDto>>

    @GET("user/blogs/{id}")
    suspend fun getMyBlogById(@Path("id") id: String): Response<ApiResponse<BlogDto>>

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
