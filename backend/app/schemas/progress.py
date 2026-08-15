from datetime import datetime

from pydantic import BaseModel, Field

from app.schemas.movie import MovieResponse


class ProgressUpdate(BaseModel):
    position_ms: int = Field(ge=0)
    duration_ms: int = Field(ge=0)


class ProgressResponse(BaseModel):
    movie_id: int
    position_ms: int
    duration_ms: int
    progress_percent: float
    completed: bool
    last_watched_at: datetime | None


class ContinueWatchingResponse(BaseModel):
    movie: MovieResponse
    position_ms: int
    duration_ms: int
    progress_percent: float
    last_watched_at: datetime
