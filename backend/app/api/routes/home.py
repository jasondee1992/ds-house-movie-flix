from datetime import datetime, timezone

from fastapi import APIRouter, Depends, Query, Response
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.api.routes.movies import to_card_response
from app.api.routes.progress import CONTINUE_WATCHING_MINIMUM_MS, _percent
from app.db.dependencies import get_session
from app.models.movie import Movie, PlaybackProgress
from app.schemas.home import HomeResponse, HomeRow, WatchingCard

router = APIRouter(tags=["home"])


def _genres(value: str | None) -> list[str]:
    return [item.strip() for item in (value or "").split(",") if item.strip()]


@router.get("/home", response_model=HomeResponse)
def home_feed(response: Response, row_limit: int = Query(12, ge=1, le=30),
              session: Session = Depends(get_session)) -> HomeResponse:
    movies = session.scalars(select(Movie).order_by(Movie.date_added.desc(), Movie.id.desc())).all()
    recent = movies[:row_limit]
    progress = session.scalars(select(PlaybackProgress)
        .where(PlaybackProgress.completed.is_(False),
               PlaybackProgress.position_ms >= CONTINUE_WATCHING_MINIMUM_MS)
        .options(selectinload(PlaybackProgress.movie))
        .order_by(PlaybackProgress.last_watched_at.desc()).limit(row_limit)).all()
    watching = [WatchingCard(movie=to_card_response(item.movie), position_ms=item.position_ms,
        duration_ms=item.duration_ms, progress_percent=_percent(item.position_ms, item.duration_ms),
        last_watched_at=item.last_watched_at) for item in progress]
    rows = [HomeRow(id="recently-added", title="Recently Added",
                    items=[to_card_response(movie) for movie in recent])]
    genres = sorted({genre for movie in movies for genre in _genres(movie.genre)}, key=str.casefold)
    for genre in genres:
        items = [movie for movie in movies if genre.casefold() in
                 {name.casefold() for name in _genres(movie.genre)}][:row_limit]
        rows.append(HomeRow(id=f"genre-{genre.casefold().replace(' ', '-')}", title=genre,
                            items=[to_card_response(movie) for movie in items]))
    response.headers["Cache-Control"] = "private, max-age=15, stale-while-revalidate=60"
    return HomeResponse(generated_at=datetime.now(timezone.utc),
        hero=to_card_response(recent[0]) if recent else None, continue_watching=watching, rows=rows)
