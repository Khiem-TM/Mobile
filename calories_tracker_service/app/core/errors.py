class AppError(Exception):
    def __init__(self, detail: str, status_code: int = 500):
        super().__init__(detail)
        self.detail = detail
        self.status_code = status_code


class NoPlateFoundError(AppError):
    """No plate/bowl detected in the image -> client error (bad input image)."""

    def __init__(self, detail: str = "No plate or bowl detected in the image"):
        super().__init__(detail=detail, status_code=400)


class ModelLoadError(AppError):
    def __init__(self, detail: str = "Failed to load AI models"):
        super().__init__(detail=detail, status_code=500)


class ImageDecodeError(AppError):
    def __init__(self, detail: str = "Could not decode image bytes"):
        super().__init__(detail=detail, status_code=400)
