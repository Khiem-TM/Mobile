export type UserRole = 'user' | 'admin';
export type BlogStatus = 'pending' | 'approved' | 'rejected' | 'draft';
export type Granularity = 'day' | 'week' | 'month';

export interface AdminUser {
  id: string;
  email: string;
  display_name?: string;
  displayName?: string;
  avatar_url?: string | null;
  avatarUrl?: string | null;
  role: UserRole;
  is_verified?: boolean;
  isVerified?: boolean;
  is_active?: boolean;
  isActive?: boolean;
  created_at?: string;
  createdAt?: string;
  updated_at?: string;
  updatedAt?: string;
  healthProfile?: Record<string, unknown> | null;
  recentSessions?: TrainingSession[];
  adminSummary?: {
    mealLogCount?: number;
    blogCount?: number;
    aiScanCount?: number;
    chatSessionCount?: number;
  };
}

export interface Food {
  id: string;
  name: string;
  name_en?: string | null;
  nameEn?: string | null;
  brand?: string | null;
  category?: string | null;
  description?: string | null;
  food_type?: 'ingredient' | 'dish' | 'product';
  foodType?: 'ingredient' | 'dish' | 'product';
  serving_size_g?: number;
  servingSizeG?: number;
  serving_unit?: string;
  servingUnit?: string;
  calories_per_100g?: number;
  caloriesPer100g?: number;
  protein_per_100g?: number;
  proteinPer100g?: number;
  fat_per_100g?: number;
  fatPer100g?: number;
  carbs_per_100g?: number;
  carbsPer100g?: number;
  fiber_per_100g?: number | null;
  fiberPer100g?: number | null;
  image_urls?: string[] | null;
  imageUrls?: string[] | null;
  is_verified?: boolean;
  isVerified?: boolean;
  is_active?: boolean;
  isActive?: boolean;
  is_custom?: boolean;
  isCustom?: boolean;
  created_at?: string;
  createdAt?: string;
}

export interface Exercise {
  id: string;
  name: string;
  description?: string | null;
  instructions?: string | null;
  exerciseType?: 'SPORT' | 'GYM' | string;
  exercise_type?: 'SPORT' | 'GYM' | string;
  category?: string | null;
  muscleGroup?: string | null;
  difficultyLevel?: string | null;
  metValue?: number;
  imageAvtUrl?: string | null;
  videoUrl?: string | null;
  isActive?: boolean;
  equipment?: string | null;
  formTips?: string | null;
  secondaryMuscleGroups?: string[] | null;
  defaultSets?: number | null;
  defaultReps?: number | null;
  defaultWeightKg?: number | null;
  targetMuscleGroup?: string | null;
  restTimeSeconds?: number | null;
  defaultDurationMinutes?: number | null;
  defaultIntensityLevel?: string | null;
  movementType?: string | null;
  estimatedCaloriesPerMinute?: number | null;
  createdAt?: string;
}

export interface Blog {
  id: string;
  title: string;
  author?: string | null;
  author_id?: string | null;
  authorId?: string | null;
  authorUser?: AdminUser | null;
  thumbnailUrl?: string | null;
  status?: BlogStatus;
  rejectionReason?: string | null;
  tags?: string[] | string | null;
  likesCount?: number;
  viewCount?: number;
  commentCount?: number;
  blocks?: BlogBlock[];
  createdAt?: string;
  created_at?: string;
  deletedAt?: string | null;
}

export interface BlogBlock {
  id?: string;
  order?: number;
  type: 'text' | 'image';
  text_content?: string | null;
  textContent?: string | null;
  image_url?: string | null;
  imageUrl?: string | null;
}

export interface BlogComment {
  id: string;
  blog_id?: string;
  blogId?: string;
  author_id?: string | null;
  authorId?: string | null;
  authorUser?: AdminUser | null;
  content: string;
  createdAt?: string;
  created_at?: string;
  updatedAt?: string;
  deletedAt?: string | null;
}

export interface TrainingSession {
  id: string;
  sessionDate?: string;
  createdAt?: string;
  totalDurationMinutes?: number;
  totalCaloriesBurned?: number;
  items?: Array<Record<string, unknown>>;
}

export interface AdminStats {
  totalUsers?: number;
  activeUsers?: number;
  inactiveUsers?: number;
  verifiedUsers?: number;
  totalFoods?: number;
  pendingFoods?: number;
  totalBlogs?: number;
  pendingBlogs?: number;
  rejectedBlogs?: number;
  totalExercises?: number;
  activeExercises?: number;
  totalTrainingSessions?: number;
  totalMealLogs?: number;
  totalAiScans?: number;
  totalChatSessions?: number;
}

export interface Paginated<T> {
  users?: T[];
  foods?: T[];
  exercises?: T[];
  items?: T[];
  total: number;
  page: number;
  limit: number;
}

export interface AnalyticsRange {
  fromDate: string;
  toDate: string;
  granularity: Granularity;
}

export interface AnalyticsResponse {
  range?: AnalyticsRange;
  totals?: Record<string, number>;
  series?: Array<Record<string, string | number>>;
  stats?: AdminStats;
  users?: AnalyticsResponse;
  nutrition?: AnalyticsResponse;
  training?: AnalyticsResponse;
  blogs?: AnalyticsResponse;
  ai?: AnalyticsResponse;
  scans?: Array<Record<string, string | number>>;
  chatSessions?: Array<Record<string, string | number>>;
  messages?: Array<Record<string, string | number>>;
}

export interface HealthResponse {
  status?: string;
  services?: Record<
    string,
    {
      status?: string;
      message?: string;
      statusCode?: number;
      brokers?: string;
    }
  >;
}

export interface AdminSession {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  user: AdminUser;
}

export interface ApiErrorShape {
  message: string;
  statusCode?: number;
  details?: unknown;
}
