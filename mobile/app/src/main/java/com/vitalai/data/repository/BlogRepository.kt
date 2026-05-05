package com.vitalai.data.repository

import com.vitalai.data.remote.BlogApi
import com.vitalai.data.remote.model.BlogDto
import com.vitalai.data.remote.model.BlogPageDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlogRepository @Inject constructor(
    private val blogApi: BlogApi
) {
    suspend fun getBlogs(tag: String? = null, page: Int = 1, limit: Int = 10): Result<BlogPageDto> {
        return try {
            val response = blogApi.getBlogs(tag, page, limit)
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
}
