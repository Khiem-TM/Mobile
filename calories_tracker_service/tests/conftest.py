from pathlib import Path

import cv2
import numpy as np
import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient

from app.core.config import Settings
from app.main import app
from app.schemas.inference import FoodVolume


class FakePipeline:
    """Stand-in for AIPipeline so route tests never load real model weights."""

    def __init__(self) -> None:
        self.loaded = True
        self.result: list[FoodVolume] = []
        self.error: Exception | None = None

    def estimate_volume(self, image_bgr: np.ndarray) -> list[FoodVolume]:
        if self.error is not None:
            raise self.error
        return self.result


@pytest.fixture
def project_root() -> Path:
    return Path(__file__).resolve().parent.parent


@pytest.fixture
def fixtures_dir(project_root: Path) -> Path:
    return project_root / "tests" / "fixtures"


@pytest.fixture
def image_bytes() -> bytes:
    img = np.zeros((20, 20, 3), dtype=np.uint8)
    ok, buf = cv2.imencode(".png", img)
    assert ok
    return buf.tobytes()


@pytest.fixture
def fake_pipeline() -> FakePipeline:
    return FakePipeline()


@pytest_asyncio.fixture
async def async_client(fake_pipeline: FakePipeline):
    # Inject fakes directly instead of running the real lifespan (which would
    # download and load the three models).
    app.state.settings = Settings()
    app.state.pipeline = fake_pipeline
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        yield client
