import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';
import { MuscleGroup } from '../../../common/enums/muscle-group.enum';
import { TrainingIntensity } from '../../../common/enums/training-intensity.enum';

@Entity('exercises')
export class Exercise {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ type: 'varchar', length: 100 })
  name!: string;

  @Column({ type: 'text', nullable: true })
  description!: string;

  @Column({
    name: 'primary_muscle_group',
    type: 'enum',
    enum: MuscleGroup,
  })
  primaryMuscleGroup!: MuscleGroup;

  @Column({
    type: 'enum',
    enum: TrainingIntensity,
  })
  intensity!: TrainingIntensity;

  @Column({ name: 'met_value', type: 'decimal', precision: 5, scale: 2, default: 0 })
  metValue!: number; // Metabolic Equivalent of Task

  @Column({ type: 'text', nullable: true })
  instructions!: string;

  @Column({ name: 'video_url', type: 'text', nullable: true })
  videoUrl!: string;

  @Column({ name: 'image_avt_url', type: 'text', nullable: true, default: null })
  imageAvtUrl!: string | null;

  @Column({ name: 'image_avt_public_id', type: 'varchar', length: 255, nullable: true, default: null })
  imageAvtPublicId!: string | null;

  @Column({ name: 'image_url', type: 'text', array: true, nullable: true, default: null })
  imageUrl!: string[] | null;

  @Column({ name: 'image_public_ids', type: 'text', array: true, nullable: true, default: null })
  imagePublicIds!: string[] | null;

  @CreateDateColumn()
  createdAt!: Date;

  @UpdateDateColumn()
  updatedAt!: Date;
}
