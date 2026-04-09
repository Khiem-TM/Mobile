import { MealLog } from '../entities/meal-log.entity';
import { MealLogItem } from '../entities/meal-log-item.entity';
import { MealType } from '../../../common/enums/meal-type.enum';

export interface IMealLogsRepository {
  findOrCreate(userId: string, date: string, type: MealType): Promise<MealLog>;
  findById(id: string): Promise<MealLog | null>;
  findByUser(userId: string, date?: string): Promise<MealLog[]>;
  updateLog(id: string, data: Partial<MealLog>): Promise<MealLog>;
  deleteLog(id: string): Promise<void>;
  saveItem(item: Partial<MealLogItem>): Promise<MealLogItem>;
  updateItem(itemId: string, data: Partial<MealLogItem>): Promise<MealLogItem>;
  removeItem(itemId: string): Promise<void>;
  findItemById(itemId: string): Promise<MealLogItem | null>;
  updateImage(id: string, imageUrl: string | null, imagePublicId: string | null): Promise<MealLog>;
}
