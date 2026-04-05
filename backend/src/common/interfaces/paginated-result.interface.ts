export interface PaginatedResult<T> {
  data: T[]; // Generic type --> có thể là bất kỳ data nào (user, food, meal-log, training,...)
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

// format Response khi có pagination
