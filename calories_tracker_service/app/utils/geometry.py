import numpy as np


def mask_to_bbox(mask: np.ndarray) -> tuple[int, int, int, int]:
    """Return (left, top, right, bottom) pixel bbox of a boolean/0-1 mask."""
    index = np.argwhere(mask)
    if index.size == 0:
        raise ValueError("Cannot compute bbox for an empty mask")
    rows = index[:, 0]
    cols = index[:, 1]
    return int(cols.min()), int(rows.min()), int(cols.max()), int(rows.max())


def mask_diameter_px(mask: np.ndarray) -> float:
    """Average of bbox width and height in pixels — used as the plate diameter."""
    left, top, right, bottom = mask_to_bbox(mask)
    width = right - left + 1
    height = bottom - top + 1
    return (width + height) / 2.0


def mask_centroid(mask: np.ndarray) -> tuple[float, float]:
    """Return (x, y) centroid of a boolean/0-1 mask."""
    index = np.argwhere(mask)
    if index.size == 0:
        raise ValueError("Cannot compute centroid for an empty mask")
    y = float(index[:, 0].mean())
    x = float(index[:, 1].mean())
    return x, y
