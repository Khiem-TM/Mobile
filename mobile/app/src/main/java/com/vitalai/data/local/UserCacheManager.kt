package com.vitalai.data.local

import com.vitalai.data.local.room.dao.ActivityLogDao
import com.vitalai.data.local.room.dao.BodyMetricDao
import com.vitalai.data.local.room.dao.ExerciseDao
import com.vitalai.data.local.room.dao.FoodCacheDao
import com.vitalai.data.local.room.dao.HealthProfileDao
import com.vitalai.data.local.room.dao.MealLogDao
import com.vitalai.data.local.room.dao.PendingSyncActionDao
import com.vitalai.data.local.room.dao.StreakDao
import com.vitalai.data.local.room.dao.UserProfileDao
import com.vitalai.data.local.room.dao.WorkoutSessionDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dọn cache cục bộ thuộc về một tài khoản trước khi tài khoản khác đăng nhập
 * trên cùng thiết bị, để tránh lộ nhật ký ăn uống/tập luyện/chỉ số cơ thể... của
 * tài khoản trước. Catalog chung (exercise, food_cache) được giữ lại, chỉ reset
 * cờ favorite vì cờ này là dữ liệu theo từng user.
 */
@Singleton
class UserCacheManager @Inject constructor(
    private val mealLogDao: MealLogDao,
    private val activityLogDao: ActivityLogDao,
    private val bodyMetricDao: BodyMetricDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val streakDao: StreakDao,
    private val healthProfileDao: HealthProfileDao,
    private val userProfileDao: UserProfileDao,
    private val pendingSyncActionDao: PendingSyncActionDao,
    private val exerciseDao: ExerciseDao,
    private val foodCacheDao: FoodCacheDao,
) {
    suspend fun clearAccountScopedData() {
        mealLogDao.clearAll()
        activityLogDao.clear()
        bodyMetricDao.clearAll()
        workoutSessionDao.clear()
        streakDao.clear()
        healthProfileDao.clear()
        userProfileDao.clear()
        pendingSyncActionDao.clearAll()
        exerciseDao.clearAllFavoriteFlags()
        foodCacheDao.clearFavoriteFlags()
    }

    /** Nếu tài khoản vừa đăng nhập khác với tài khoản đang cache, xoá sạch dữ liệu cũ. */
    suspend fun clearIfDifferentAccount(newUserId: String) {
        val cached = userProfileDao.get()
        if (cached != null && cached.userId != newUserId) {
            clearAccountScopedData()
        }
    }
}
