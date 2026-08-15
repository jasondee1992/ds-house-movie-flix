from pathlib import Path

from fastapi import APIRouter, Depends, HTTPException, Response
from fastapi.responses import FileResponse
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.db.dependencies import get_session
from app.models.movie import Movie
from app.schemas.movie import MovieResponse, SubtitleResponse

router = APIRouter(prefix="/movies", tags=["movies"])


def quality_label(width: int | None, height: int | None) -> str | None:
    if not width and not height: return None
    largest = max(width or 0, height or 0)
    smallest = min(width or 0, height or 0)
    if largest >= 3800 or smallest >= 2100: return "4K"
    if smallest >= 1400: return "1440p"
    if smallest >= 1000: return "1080p"
    if smallest >= 700: return "720p"
    if smallest: return f"{smallest}p"
    return None


def to_response(movie: Movie) -> MovieResponse:
    subtitles = [SubtitleResponse.model_validate(item) for item in movie.subtitles]
    return MovieResponse(
        id=movie.id, title=movie.title, year=movie.year, duration_seconds=movie.duration_seconds,
        description=movie.description, genre=movie.genre,
        poster_url=f"/api/movies/{movie.id}/poster", backdrop_url=f"/api/movies/{movie.id}/backdrop",
        file_extension=movie.file_extension, file_size=movie.file_size, date_added=movie.date_added,
        video_width=movie.video_width, video_height=movie.video_height,
        quality=quality_label(movie.video_width, movie.video_height), video_codec=movie.video_codec,
        audio_codec=movie.audio_codec, audio_channels=movie.audio_channels, bitrate=movie.bitrate,
        frame_rate=movie.frame_rate, container_format=movie.container_format,
        subtitle_count=len(subtitles), subtitles=subtitles,
    )


def _movie_query():
    return select(Movie).options(selectinload(Movie.subtitles))


@router.get("", response_model=list[MovieResponse])
def list_movies(session: Session = Depends(get_session)) -> list[MovieResponse]:
    movies = session.scalars(_movie_query().order_by(Movie.title.collate("NOCASE"))).all()
    return [to_response(movie) for movie in movies]


@router.get("/{movie_id}", response_model=MovieResponse)
def get_movie(movie_id: int, session: Session = Depends(get_session)) -> MovieResponse:
    movie = session.scalar(_movie_query().where(Movie.id == movie_id))
    if movie is None: raise HTTPException(status_code=404, detail="Movie not found")
    return to_response(movie)


def _image(movie_id: int, attribute: str, session: Session) -> FileResponse:
    movie = session.get(Movie, movie_id)
    if movie is None: raise HTTPException(status_code=404, detail="Movie not found")
    value = getattr(movie, attribute)
    if not value or not Path(value).is_file(): raise HTTPException(status_code=404, detail="Image not found")
    image = Path(value)
    types = {".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".png": "image/png"}
    return FileResponse(image, media_type=types.get(image.suffix.lower(), "image/jpeg"))


@router.get("/{movie_id}/poster", response_model=None)
def get_poster(movie_id: int, session: Session = Depends(get_session)) -> FileResponse | Response:
    try: return _image(movie_id, "poster_path", session)
    except HTTPException as error:
        if error.detail != "Image not found": raise
        placeholder = '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="600"><rect width="400" height="600" fill="#202027"/><text x="200" y="320" text-anchor="middle" fill="#e50914" font-size="72">H</text></svg>'
        return Response(content=placeholder, media_type="image/svg+xml")


@router.get("/{movie_id}/backdrop", response_model=None)
def get_backdrop(movie_id: int, session: Session = Depends(get_session)) -> FileResponse:
    return _image(movie_id, "backdrop_path", session)
