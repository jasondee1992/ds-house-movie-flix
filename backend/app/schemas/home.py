from datetime import datetime

from pydantic import BaseModel

from app.schemas.movie import MovieCardResponse


class WatchingCard(BaseModel):
    movie: MovieCardResponse
    position_ms: int
    duration_ms: int
    progress_percent: float
    last_watched_at: datetime


class HomeRow(BaseModel):
    id: str
    title: str
    items: list[MovieCardResponse]


class HomeResponse(BaseModel):
    generated_at: datetime
    hero: MovieCardResponse | None
    continue_watching: list[WatchingCard]
    rows: list[HomeRow]
