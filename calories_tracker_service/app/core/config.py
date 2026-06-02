from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

DATA_DIR = Path(__file__).resolve().parent.parent / "data"


class Settings(BaseSettings):
    app_name: str = "Food Volume Estimation API"
    app_version: str = "0.2.0"
    debug: bool = False

    # --- Inference device ---
    # "cpu" by default to keep RAM low; set to "cuda" if a GPU is available.
    device: str = "cpu"

    # --- Models ---
    yolo_model_path: str = "yolov8n-seg.pt"
    food_model_id: str = "nateraw/food"
    depth_model_id: str = "depth-anything/Depth-Anything-V2-Small-hf"

    # COCO classes treated as the "plate" (no generic plate class exists in COCO).
    plate_classes: tuple[str, ...] = ("bowl", "dining table")
    # COCO classes ignored when collecting food masks.
    ignore_classes: tuple[str, ...] = (
        "fork",
        "knife",
        "spoon",
        "cup",
        "wine glass",
        "person",
        "dining table",
        "bowl",
    )

    # YOLO detection confidence threshold.
    min_conf: float = 0.25
    # Longest image side fed to the models (downscale larger inputs to save RAM).
    max_image_side: int = 1024

    # --- Physical calibration assumptions (cm) ---
    plate_diameter_cm: float = 25.0
    plate_depth_cm: float = 1.5
    plate_thickness_cm: float = 0.2

    data_dir: Path = DATA_DIR

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")


settings = Settings()
