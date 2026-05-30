"""Schema cho daily summary / weekly report (P2)."""
from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field


class DailySummaryRequest(BaseModel):
    user_ref: str
    date: str  # YYYY-MM-DD
    user_context: dict[str, Any] | None = None


class DailySummaryResponse(BaseModel):
    user_ref: str
    date: str
    summary: str
    metrics: dict[str, Any] = Field(default_factory=dict)


class WeeklyReportRequest(BaseModel):
    user_ref: str
    week_start: str  # YYYY-MM-DD (thứ Hai)
    user_context: dict[str, Any] | None = None


class WeeklyReportResponse(BaseModel):
    user_ref: str
    week_start: str
    report: dict[str, Any] = Field(default_factory=dict)
