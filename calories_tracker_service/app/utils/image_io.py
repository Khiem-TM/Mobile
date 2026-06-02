import cv2
import numpy as np

from app.core.errors import ImageDecodeError


def decode_image_bytes(data: bytes) -> np.ndarray:
    image_array = np.frombuffer(data, dtype=np.uint8)
    image_bgr = cv2.imdecode(image_array, cv2.IMREAD_COLOR)
    if image_bgr is None:
        raise ImageDecodeError()
    return image_bgr
