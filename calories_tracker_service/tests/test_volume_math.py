"""Unit tests for the Step-5 plate-calibrated volume math (no model weights)."""

import numpy as np

from app.core.config import Settings
from app.services.ai_pipeline import AIPipeline


def _pipeline_without_models() -> AIPipeline:
    # Bypass __init__ so torch/ultralytics/transformers are never imported.
    pipe = AIPipeline.__new__(AIPipeline)
    pipe.settings = Settings()
    return pipe


def test_compute_volume_single_food():
    pipe = _pipeline_without_models()

    # 200x200 scene. Background (lowest plane) depth = 10.
    depth_img = np.full((200, 200), 10.0, dtype=np.float32)

    # Plate occupies the top-left 100x100 block at depth 8 (its surface).
    plate_mask = np.zeros((200, 200), dtype=bool)
    plate_mask[0:100, 0:100] = True
    depth_img[0:100, 0:100] = 8.0

    # Food: a separate 10x10 region raised toward the camera (depth 4).
    food_mask = np.zeros((200, 200), dtype=bool)
    food_mask[120:130, 120:130] = True
    depth_img[120:130, 120:130] = 4.0

    food_items = [{"mask": food_mask, "food_title": "rice"}]
    results = pipe._compute_volumes(depth_img, plate_mask, food_items)

    # Hand-computed expectation:
    #   plate_diameter_px = (100 + 100) / 2 = 100
    #   pixel_to_cm = 25 / 100 = 0.25  -> area_per_pixel = 0.0625 cm^2
    #   lowest = 10, avg_plate = 8 -> depth_span = 2
    #   depth_per_unit = 1.5 / 2 = 0.75
    #   height_cm = (10 - 4) * 0.75 - 0.2 = 4.3
    #   volume = 100 px * 4.3 * 0.0625 = 26.875 -> 26.9
    assert len(results) == 1
    assert results[0].food_title == "rice"
    assert results[0].volume_cm3 == 26.9


def test_compute_volume_groups_same_title():
    pipe = _pipeline_without_models()

    depth_img = np.full((200, 200), 10.0, dtype=np.float32)
    plate_mask = np.zeros((200, 200), dtype=bool)
    plate_mask[0:100, 0:100] = True
    depth_img[0:100, 0:100] = 8.0

    mask_a = np.zeros((200, 200), dtype=bool)
    mask_a[120:130, 120:130] = True
    depth_img[120:130, 120:130] = 4.0

    mask_b = np.zeros((200, 200), dtype=bool)
    mask_b[150:160, 150:160] = True
    depth_img[150:160, 150:160] = 4.0

    food_items = [
        {"mask": mask_a, "food_title": "rice"},
        {"mask": mask_b, "food_title": "rice"},
    ]
    results = pipe._compute_volumes(depth_img, plate_mask, food_items)

    # Two identical regions summed under one title -> 2 * 26.875 ≈ 53.75 -> 53.7
    # (float rounding of 4.3 nudges the sum just below 53.75).
    assert len(results) == 1
    assert results[0].food_title == "rice"
    assert results[0].volume_cm3 == 53.7
