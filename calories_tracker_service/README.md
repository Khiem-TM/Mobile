# Food Volume Estimation FastAPI Service

A standalone FastAPI microservice that takes a single food photo and returns the
recognised dishes together with an estimated physical volume (cm³). It is meant
to be called by a NestJS backend.

The pipeline uses three pre-trained models and runs on CPU by default:

```text
image
-> YOLOv8n-seg            (segment the plate/bowl + objects on it)
-> nateraw/food (ViT)     (name each food crop)
-> Depth-Anything-V2-Small (relative depth map)
-> plate-calibrated pixel integration
-> volume per food (cm³)
```

### How the plate/food split works

`yolov8n-seg.pt` is trained on COCO, which has a `bowl`/`dining table` class but
**no generic `plate` or `food` class**. So:

- **Plate** = the largest mask whose class is in `PLATE_CLASSES` (`bowl`,
  `dining table`). If none is found the API returns **HTTP 400**.
- **Food** = every other (non-utensil) mask whose centroid sits on the plate.
  Each food crop is named by `nateraw/food` (Food-101 labels). If nothing is
  detected on the plate, the plate interior is treated as one food region.

### Volume algorithm (Step 5)

1. **Horizontal scale** — assume the plate's real diameter is `PLATE_DIAMETER_CM`
   (25 cm). The plate's pixel diameter (avg of bbox width/height) gives
   `pixel_to_cm`, hence the ground area of one pixel.
2. **Vertical scale** — relative depth is unitless, so it is calibrated with the
   plate: the depth gap between the lowest plane and the plate surface maps to
   the plate's real depth (`PLATE_DEPTH_CM`).
3. **Integration** — each food pixel is a vertical column; its height above the
   plate (minus `PLATE_THICKNESS_CM`) times the per-pixel ground area, summed
   over the mask, gives cm³.

## Install

```bash
cd fastapi_service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

First run downloads the model weights (yolov8n-seg + the two Hugging Face models).

## Test

```bash
pytest -q          # fast: unit volume math + route tests (no model weights)
```

## Run

```bash
uvicorn app.main:app --reload --port 8000
```

## API

Health:

```bash
curl http://localhost:8000/health
```

Estimate volume from an image:

```bash
curl -X POST http://localhost:8000/api/v1/estimate-volume \
  -F "image=@tests/fixtures/test.jpg"
```

Response:

```json
{
  "success": true,
  "data": [
    { "food_title": "french fries", "volume_cm3": 150.5 },
    { "food_title": "hamburger", "volume_cm3": 320.1 }
  ]
}
```

## Limitations

- Detection is bounded by what COCO-trained YOLOv8 can segment.
- Volume is an estimate: it assumes a 25 cm round plate and relies on relative
  (not metric) depth calibrated against the plate.
- Accuracy degrades for top-down-only views, occluded food, or non-standard
  plate sizes; tune the `PLATE_*` constants per deployment.
