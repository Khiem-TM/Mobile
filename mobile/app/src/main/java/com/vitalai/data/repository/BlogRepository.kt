package com.vitalai.data.repository

import com.vitalai.data.remote.BlogApi
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.data.remote.model.BlogLikedDto
import com.vitalai.data.remote.model.BlogPageDto
import com.vitalai.data.remote.model.CommentDto
import com.vitalai.data.remote.model.CommentPageDto
import com.vitalai.data.remote.model.CreateCommentRequest
import com.vitalai.data.remote.model.CreateBlogRequest
import com.vitalai.data.remote.model.UpdateCommentRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlogRepository @Inject constructor(
    private val blogApi: BlogApi
) {
    suspend fun getBlogs(
        tag: String? = null,
        authorId: String? = null,
        page: Int = 1,
        limit: Int = 10
    ): Result<BlogPageDto> {
        return try {
            val response = blogApi.getBlogs(tag = tag, authorId = authorId, page = page, limit = limit)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tải bài viết (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBlogById(id: String): Result<BlogDto> {
        return try {
            val response = blogApi.getBlogById(id)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Không tìm thấy bài viết"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTags(): Result<List<String>> {
        return try {
            val response = blogApi.getTags()
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLike(id: String): Result<BlogLikedDto> {
        return try {
            val response = blogApi.toggleLike(id)
            val body = response.body()?.data
            if (response.isSuccessful && body != null && body.likesCount != null) Result.success(body)
            else Result.failure(Exception("Không cập nhật được lượt thích (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isLiked(id: String): Result<Boolean> {
        return try {
            val response = blogApi.isLiked(id)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body.liked)
            else Result.success(false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComments(id: String, page: Int = 1, limit: Int = 20): Result<CommentPageDto> {
        return try {
            val response = blogApi.getComments(id, page, limit)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Không tải được bình luận (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postComment(id: String, content: String): Result<CommentDto> {
        return try {
            val response = blogApi.postComment(id, CreateCommentRequest(content))
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Không gửi được bình luận (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(blogId: String, commentId: String): Result<Unit> {
        return try {
            val response = blogApi.deleteComment(blogId, commentId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Không xóa được bình luận (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateComment(blogId: String, commentId: String, content: String): Result<CommentDto> {
        return try {
            val response = blogApi.updateComment(blogId, commentId, UpdateCommentRequest(content))
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Không sửa được bình luận (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyBlogs(page: Int = 1, limit: Int = 50): Result<BlogPageDto> {
        return try {
            val response = blogApi.getMyBlogs(page, limit)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Không tải được bài viết của tôi (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyBlogById(id: String): Result<BlogDto> {
        return try {
            val response = blogApi.getMyBlogById(id)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Không tải được bài viết của tôi (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createBlog(request: CreateBlogRequest): Result<BlogDto> {
        return try {
            val response = blogApi.createBlog(request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Không tạo được bài viết (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBlog(id: String, request: CreateBlogRequest): Result<BlogDto> {
        return try {
            val response = blogApi.updateBlog(id, request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Không cập nhật được bài viết (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMyBlog(id: String): Result<Unit> {
        return try {
            val response = blogApi.deleteMyBlog(id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Không xóa được bài viết (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
