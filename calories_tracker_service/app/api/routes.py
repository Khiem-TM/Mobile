from fastapi import APIRouter, Request, UploadFile

from app.schemas.inference import EstimateVolumeResponse, HealthResponse
from app.utils.image_io import decode_image_bytes

router = APIRouter()


@router.get("/health", response_model=HealthResponse)
async def health(request: Request) -> HealthResponse:
    settings = request.app.state.settings
    pipeline = request.app.state.pipeline
    return HealthResponse(
        status="ok",
        models_loaded=pipeline.loaded,
        plate_diameter_cm=settings.plate_diameter_cm,
    )


@router.post("/api/v1/estimate-volume", response_model=EstimateVolumeResponse)
async def estimate_volume(
    request: Request, image: UploadFile
) -> EstimateVolumeResponse:
    image_bgr = decode_image_bytes(await image.read())
    data = request.app.state.pipeline.estimate_volume(image_bgr=image_bgr)
    return EstimateVolumeResponse(success=True, data=data)
