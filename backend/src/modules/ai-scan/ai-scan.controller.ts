import {
  Controller,
  Post,
  UseGuards,
  UseInterceptors,
  UploadedFile,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import {
  ApiTags,
  ApiBearerAuth,
  ApiOperation,
  ApiConsumes,
  ApiBody,
} from '@nestjs/swagger';
import { buildMulterOptions } from '../../common/utils/multer.config';
import { JwtAuthGuard } from '../../common/guards/jwt.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../common/interfaces/jwt-payload.interface';
import { AiScanService } from './ai-scan.service';
import { AiScanResultDto } from './dto/ai-scan-result.dto';

@ApiTags('ai-scan')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('ai-scan')
export class AiScanController {
  constructor(private readonly aiScanService: AiScanService) {}

  @ApiOperation({
    summary: 'Analyze a food photo and return detected items with DB matches',
  })
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      properties: { image: { type: 'string', format: 'binary' } },
      required: ['image'],
    },
  })
  @Post('analyze')
  @UseInterceptors(FileInterceptor('image', buildMulterOptions('ai-scans')))
  analyzeImage(
    @UploadedFile() file: Express.Multer.File,
    @CurrentUser() user: JwtPayload,
  ): Promise<AiScanResultDto[]> {
    return this.aiScanService.analyzeImage(file, user.sub);
  }
}
