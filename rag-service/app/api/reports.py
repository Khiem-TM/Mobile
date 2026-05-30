"""Daily summary / weekly report endpoints (P2).

NestJS scheduler gọi POST để sinh; mobile (qua NestJS gateway) gọi GET để đọc.
"""
from __future__ import annotations

from datetime import date as date_cls

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.summary_service import SummaryService
from app.db.rag_repo import RagRepo
from app.db.session import get_session
from app.deps import get_summary_service, verify_internal_secret
from app.schemas.reports import (
    DailySummaryRequest,
    DailySummaryResponse,
    WeeklyReportRequest,
    WeeklyReportResponse,
)

router = APIRouter(tags=["reports"], dependencies=[Depends(verify_internal_secret)])


@router.post("/summary/daily", response_model=DailySummaryResponse)
async def generate_daily(
    req: DailySummaryRequest,
    summary: SummaryService = Depends(get_summary_service),
    session: AsyncSession = Depends(get_session),
) -> DailySummaryResponse:
    text, metrics = await summary.generate_daily(req.user_ref, req.date, req.user_context)
    await session.commit()
    return DailySummaryResponse(user_ref=req.user_ref, date=req.date, summary=text, metrics=metrics)


@router.get("/summary/daily/{user_ref}", response_model=DailySummaryResponse)
async def get_daily(
    user_ref: str,
    date: str,
    session: AsyncSession = Depends(get_session),
) -> DailySummaryResponse:
    row = await RagRepo(session).get_daily_summary(user_ref, date_cls.fromisoformat(date))
    if not row:
        raise HTTPException(status_code=404, detail="not found")
    return DailySummaryResponse(user_ref=user_ref, date=date, summary=row.summary or "", metrics=row.metrics or {})


@router.post("/report/weekly", response_model=WeeklyReportResponse)
async def generate_weekly(
    req: WeeklyReportRequest,
    summary: SummaryService = Depends(get_summary_service),
    session: AsyncSession = Depends(get_session),
) -> WeeklyReportResponse:
    report = await summary.generate_weekly(req.user_ref, req.week_start, req.user_context)
    await session.commit()
    return WeeklyReportResponse(user_ref=req.user_ref, week_start=req.week_start, report=report)


@router.get("/report/weekly/{user_ref}", response_model=WeeklyReportResponse)
async def get_weekly(
    user_ref: str,
    week_start: str,
    session: AsyncSession = Depends(get_session),
) -> WeeklyReportResponse:
    row = await RagRepo(session).get_weekly_report(user_ref, date_cls.fromisoformat(week_start))
    if not row:
        raise HTTPException(status_code=404, detail="not found")
    return WeeklyReportResponse(user_ref=user_ref, week_start=week_start, report=row.report or {})
