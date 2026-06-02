from pydantic import BaseModel


class FoodVolume(BaseModel):
    """A single recognised food item and its estimated physical volume."""

    food_title: str
    volume_cm3: float


class EstimateVolumeResponse(BaseModel):
    """Response returned to the NestJS backend."""

    success: bool
    data: list[FoodVolume]


class HealthResponse(BaseModel):
    status: str
    models_loaded: bool
    plate_diameter_cm: float
