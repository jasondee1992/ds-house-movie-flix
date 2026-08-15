from fastapi import APIRouter, Depends, HTTPException, Response
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.api.routes.movies import to_response
from app.db.dependencies import get_session
from app.models.movie import Movie, PlaybackProgress, utc_now
from app.schemas.progress import ContinueWatchingResponse, ProgressResponse, ProgressUpdate

router = APIRouter(tags=["playback progress"])
COMPLETION_PERCENT = 90.0
CONTINUE_WATCHING_MINIMUM_MS = 30_000


def _percent(position_ms: int, duration_ms: int) -> float:
    if duration_ms <= 0:
        return 0.0
    return round(min(100.0, max(0.0, position_ms * 100.0 / duration_ms)), 1)


def _response(movie_id: int, progress: PlaybackProgress | None) -> ProgressResponse:
    if progress is None:
        return ProgressResponse(movie_id=movie_id, position_ms=0, duration_ms=0,
                                progress_percent=0.0, completed=False, last_watched_at=None)
    return ProgressResponse(movie_id=movie_id, position_ms=progress.position_ms,
                            duration_ms=progress.duration_ms,
                            progress_percent=_percent(progress.position_ms, progress.duration_ms),
                            completed=progress.completed, last_watched_at=progress.last_watched_at)


def _require_movie(movie_id: int, session: Session) -> Movie:
    movie = session.get(Movie, movie_id)
    if movie is None:
        raise HTTPException(status_code=404, detail="Movie not found")
    return movie


@router.get("/movies/{movie_id}/progress", response_model=ProgressResponse)
def get_progress(movie_id: int, session: Session = Depends(get_session)) -> ProgressResponse:
    _require_movie(movie_id, session)
    progress = session.scalar(select(PlaybackProgress).where(PlaybackProgress.movie_id == movie_id))
    return _response(movie_id, progress)


@router.put("/movies/{movie_id}/progress", response_model=ProgressResponse)
def update_progress(movie_id: int, update: ProgressUpdate,
                    session: Session = Depends(get_session)) -> ProgressResponse:
    _require_movie(movie_id, session)
    progress = session.scalar(select(PlaybackProgress).where(PlaybackProgress.movie_id == movie_id))
    if progress is None:
        progress = PlaybackProgress(movie_id=movie_id)
        session.add(progress)
    # A new play near the beginning naturally clears a previous completed state.
    progress.position_ms = update.position_ms
    progress.duration_ms = update.duration_ms
    progress.completed = _percent(update.position_ms, update.duration_ms) >= COMPLETION_PERCENT
    progress.last_watched_at = utc_now()
    session.commit()
    session.refresh(progress)
    return _response(movie_id, progress)


@router.delete("/movies/{movie_id}/progress", status_code=204)
def delete_progress(movie_id: int, session: Session = Depends(get_session)) -> Response:
    _require_movie(movie_id, session)
    progress = session.scalar(select(PlaybackProgress).where(PlaybackProgress.movie_id == movie_id))
    if progress is not None:
        session.delete(progress)
        session.commit()
    return Response(status_code=204)


@router.get("/continue-watching", response_model=list[ContinueWatchingResponse])
def continue_watching(session: Session = Depends(get_session)) -> list[ContinueWatchingResponse]:
    query = (select(PlaybackProgress)
             .where(PlaybackProgress.completed.is_(False),
                    PlaybackProgress.position_ms >= CONTINUE_WATCHING_MINIMUM_MS)
             .options(selectinload(PlaybackProgress.movie).selectinload(Movie.subtitles))
             .order_by(PlaybackProgress.last_watched_at.desc()))
    return [ContinueWatchingResponse(movie=to_response(item.movie), position_ms=item.position_ms,
            duration_ms=item.duration_ms, progress_percent=_percent(item.position_ms, item.duration_ms),
            last_watched_at=item.last_watched_at) for item in session.scalars(query).all()]
