import pytest

from app.core.errors import NoPlateFoundError
from app.schemas.inference import FoodVolume


@pytest.mark.asyncio
async def test_health(async_client):
    response = await async_client.get("/health")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["models_loaded"] is True
    assert body["plate_diameter_cm"] == 25.0


@pytest.mark.asyncio
async def test_estimate_volume_success(async_client, fake_pipeline, image_bytes):
    fake_pipeline.result = [
        FoodVolume(food_title="french fries", volume_cm3=150.5),
        FoodVolume(food_title="hamburger", volume_cm3=320.1),
    ]

    response = await async_client.post(
        "/api/v1/estimate-volume",
        files={"image": ("test.png", image_bytes, "image/png")},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["success"] is True
    assert body["data"] == [
        {"food_title": "french fries", "volume_cm3": 150.5},
        {"food_title": "hamburger", "volume_cm3": 320.1},
    ]


@pytest.mark.asyncio
async def test_no_plate_returns_400(async_client, fake_pipeline, image_bytes):
    fake_pipeline.error = NoPlateFoundError()

    response = await async_client.post(
        "/api/v1/estimate-volume",
        files={"image": ("test.png", image_bytes, "image/png")},
    )

    assert response.status_code == 400
    assert response.json()["success"] is False


@pytest.mark.asyncio
async def test_invalid_image_returns_400(async_client):
    response = await async_client.post(
        "/api/v1/estimate-volume",
        files={"image": ("bad.png", b"not-an-image", "image/png")},
    )

    assert response.status_code == 400
