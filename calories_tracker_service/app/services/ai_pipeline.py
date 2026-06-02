"""End-to-end AI pipeline: YOLOv8-seg + nateraw/food + Depth-Anything-V2.

The whole pipeline runs on CPU by default and loads every model exactly once
(at application start-up) so that per-request work stays light on RAM.
"""

from __future__ import annotations

import gc

import cv2
import numpy as np

from app.core.config import Settings
from app.core.errors import ModelLoadError, NoPlateFoundError
from app.schemas.inference import FoodVolume
from app.utils.geometry import mask_centroid, mask_diameter_px, mask_to_bbox


class AIPipeline:
    def __init__(self, settings: Settings):
        self.settings = settings
        self._loaded = False

        # Imports are local so that importing this module (e.g. in tests that
        # monkeypatch the pipeline) does not force torch/ultralytics to load.
        try:
            import torch
            from transformers import pipeline as hf_pipeline
            from ultralytics import YOLO

            # Keep CPU thread count modest to bound memory / contention.
            if settings.device == "cpu":
                torch.set_num_threads(max(1, min(4, torch.get_num_threads())))

            hf_device = 0 if settings.device.startswith("cuda") else -1

            self._torch = torch
            self.yolo = YOLO(settings.yolo_model_path)
            self.food_clf = hf_pipeline(
                "image-classification",
                model=settings.food_model_id,
                device=hf_device,
            )
            self.depth = hf_pipeline(
                "depth-estimation",
                model=settings.depth_model_id,
                device=hf_device,
            )
            self._loaded = True
        except Exception as exc:  # pragma: no cover - depends on env/weights
            raise ModelLoadError(f"Failed to load AI models: {exc}") from exc

    @property
    def loaded(self) -> bool:
        return self._loaded

    # ------------------------------------------------------------------ #
    # Main entry point
    # ------------------------------------------------------------------ #
    def estimate_volume(self, image_bgr: np.ndarray) -> list[FoodVolume]:
        with self._torch.inference_mode():
            return self._run(image_bgr)

    def _run(self, image_bgr: np.ndarray) -> list[FoodVolume]:
        # --- Step 1: preprocess (downscale large images to save RAM) ---
        image_bgr = self._resize_max_side(image_bgr)
        image_rgb = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2RGB)
        height, width = image_bgr.shape[:2]

        # --- Step 2: YOLOv8-seg -> plate mask + food masks ---
        plate_mask, food_items = self._segment(image_bgr, height, width)

        # --- Step 3: classify each food crop -> food_title ---
        for item in food_items:
            item["food_title"] = self._classify(image_rgb, item["mask"])

        # --- Step 4: depth map (true-depth oriented) ---
        depth_img = self._depth_map(image_rgb, height, width)

        # --- Step 5: physical volume in cm3 ---
        results = self._compute_volumes(depth_img, plate_mask, food_items)

        # Free intermediates before returning.
        del depth_img, plate_mask, food_items
        gc.collect()
        return results

    # ------------------------------------------------------------------ #
    # Step 1
    # ------------------------------------------------------------------ #
    def _resize_max_side(self, image_bgr: np.ndarray) -> np.ndarray:
        h, w = image_bgr.shape[:2]
        longest = max(h, w)
        limit = self.settings.max_image_side
        if longest <= limit:
            return image_bgr
        scale = limit / float(longest)
        new_size = (int(round(w * scale)), int(round(h * scale)))
        return cv2.resize(image_bgr, new_size, interpolation=cv2.INTER_AREA)

    # ------------------------------------------------------------------ #
    # Step 2
    # ------------------------------------------------------------------ #
    def _segment(
        self,
        image_bgr: np.ndarray,
        height: int,
        width: int,
    ) -> tuple[np.ndarray, list[dict]]:
        result = self.yolo.predict(
            source=image_bgr,
            conf=self.settings.min_conf,
            device=self.settings.device,
            verbose=False,
        )[0]

        if result.masks is None or len(result.masks) == 0:
            raise NoPlateFoundError()

        names = result.names
        cls_ids = result.boxes.cls.int().tolist()
        masks_data = result.masks.data.cpu().numpy()  # (N, mh, mw), float 0..1

        plate_candidates: list[tuple[float, np.ndarray]] = []  # (area, mask)
        others: list[tuple[str, np.ndarray]] = []  # (class_name, mask)

        for cls_id, raw_mask in zip(cls_ids, masks_data):
            mask = cv2.resize(
                raw_mask, (width, height), interpolation=cv2.INTER_NEAREST
            ).astype(bool)
            if not mask.any():
                continue
            class_name = names[cls_id]
            if class_name in self.settings.plate_classes:
                plate_candidates.append((float(mask.sum()), mask))
            if class_name not in self.settings.ignore_classes:
                others.append((class_name, mask))

        if not plate_candidates:
            raise NoPlateFoundError()

        # Plate = largest plate-like mask.
        plate_mask = max(plate_candidates, key=lambda c: c[0])[1]

        # Food = every non-ignored mask whose centroid lies on the plate.
        food_items: list[dict] = []
        for class_name, mask in others:
            cx, cy = mask_centroid(mask)
            if plate_mask[int(round(cy)), int(round(cx))]:
                food_items.append({"class_name": class_name, "mask": mask})

        if not food_items:
            # No object on the plate -> treat the plate interior as a single
            # food region so the request still returns a usable estimate.
            food_items.append({"class_name": "food", "mask": plate_mask.copy()})

        return plate_mask, food_items

    # ------------------------------------------------------------------ #
    # Step 3
    # ------------------------------------------------------------------ #
    def _classify(self, image_rgb: np.ndarray, mask: np.ndarray) -> str:
        from PIL import Image

        left, top, right, bottom = mask_to_bbox(mask)
        crop = image_rgb[top : bottom + 1, left : right + 1]
        pil_crop = Image.fromarray(crop)
        preds = self.food_clf(pil_crop)
        label = preds[0]["label"] if preds else "food"
        return label.replace("_", " ").strip()

    # ------------------------------------------------------------------ #
    # Step 4
    # ------------------------------------------------------------------ #
    def _depth_map(
        self,
        image_rgb: np.ndarray,
        height: int,
        width: int,
    ) -> np.ndarray:
        from PIL import Image

        output = self.depth(Image.fromarray(image_rgb))
        pred = output["predicted_depth"]
        if hasattr(pred, "detach"):
            pred = pred.detach().squeeze().cpu().numpy()
        pred = np.asarray(pred, dtype=np.float32)
        if pred.shape != (height, width):
            pred = cv2.resize(pred, (width, height), interpolation=cv2.INTER_LINEAR)

        # Depth-Anything outputs INVERSE depth (larger = closer to camera).
        # Flip it so larger = farther (table/plate-bottom), matching the
        # plate-calibrated volume formula where max(depth) is the lowest plane.
        depth_img = float(pred.max()) - pred
        return depth_img

    # ------------------------------------------------------------------ #
    # Step 5 - plate-calibrated pixel integration
    # ------------------------------------------------------------------ #
    def _compute_volumes(
        self,
        depth_img: np.ndarray,
        plate_mask: np.ndarray,
        food_items: list[dict],
    ) -> list[FoodVolume]:
        s = self.settings

        # (a) Horizontal scale: assume the plate's real diameter is 25 cm.
        # The plate's pixel diameter (avg of its bbox width/height) calibrates
        # how many cm one pixel spans, hence the ground area of one pixel.
        plate_diameter_px = mask_diameter_px(plate_mask)
        pixel_to_cm = s.plate_diameter_cm / plate_diameter_px
        area_per_pixel_cm2 = pixel_to_cm * pixel_to_cm

        # (b) Vertical scale: relative depth has no unit, so calibrate it with
        # the plate itself. The depth gap between the lowest plane and the
        # plate surface corresponds to the plate's real depth (plate_depth_cm).
        lowest = float(depth_img.max())
        avg_plate = float(depth_img[plate_mask].mean())
        depth_span = lowest - avg_plate
        depth_per_unit = s.plate_depth_cm / depth_span if depth_span > 1e-6 else 0.0

        # (c) Integrate each food region. Every pixel is a vertical column whose
        # height is how far the food rises above the plate surface (minus the
        # plate thickness to remove the baseline). Summing column volumes
        # (height_cm * one-pixel ground area) over the mask gives cm3.
        grouped: dict[str, float] = {}
        for item in food_items:
            mask = item["mask"]
            d = depth_img[mask]
            height_cm = np.clip(
                (lowest - d) * depth_per_unit - s.plate_thickness_cm,
                a_min=0.0,
                a_max=None,
            )
            volume_cm3 = float(height_cm.sum()) * area_per_pixel_cm2
            title = item["food_title"]
            grouped[title] = grouped.get(title, 0.0) + volume_cm3

        return [
            FoodVolume(food_title=title, volume_cm3=round(volume, 1))
            for title, volume in grouped.items()
        ]
