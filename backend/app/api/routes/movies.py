from pathlib import Path
import mimetypes

from fastapi import APIRouter, Depends, Header, HTTPException, Query, Request, Response
from fastapi.responses import FileResponse, StreamingResponse
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.db.dependencies import get_session
from app.models.movie import Movie, Subtitle
from app.schemas.movie import MovieCardResponse, MovieResponse, SubtitleResponse
from app.services.media_stream import (ByteRange, InvalidRange, iter_file_range, parse_range_header,
    resolve_trusted_file, subtitle_content_type, video_content_type)
from app.services.artwork_catalog import bundled_artwork
from app.services.artwork_cache import optimized_artwork

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
    subtitles = [SubtitleResponse(id=item.id, language=item.language, format=item.format,
        is_default=item.is_default, url=f"/api/movies/{movie.id}/subtitles/{item.id}") for item in movie.subtitles]
    return MovieResponse(
        id=movie.id, title=movie.title, year=movie.year, duration_seconds=movie.duration_seconds,
        description=movie.description, genre=movie.genre,
        poster_url=f"/api/movies/{movie.id}/poster", backdrop_url=f"/api/movies/{movie.id}/backdrop",
        stream_url=f"/api/movies/{movie.id}/stream",
        file_extension=movie.file_extension, file_size=movie.file_size, date_added=movie.date_added,
        video_width=movie.video_width, video_height=movie.video_height,
        quality=quality_label(movie.video_width, movie.video_height), video_codec=movie.video_codec,
        audio_codec=movie.audio_codec, audio_channels=movie.audio_channels, bitrate=movie.bitrate,
        frame_rate=movie.frame_rate, container_format=movie.container_format,
        subtitle_count=len(subtitles), subtitles=subtitles,
    )


def to_card_response(movie: Movie) -> MovieCardResponse:
    return MovieCardResponse(
        id=movie.id, title=movie.title, year=movie.year,
        duration_seconds=movie.duration_seconds, genre=movie.genre,
        poster_url=f"/api/movies/{movie.id}/poster",
        backdrop_url=f"/api/movies/{movie.id}/backdrop",
        quality=quality_label(movie.video_width, movie.video_height),
    )


def _movie_query():
    return select(Movie).options(selectinload(Movie.subtitles))


@router.get("", response_model=list[MovieResponse])
def list_movies(response: Response, q: str | None = Query(None, max_length=200),
                genre: str | None = Query(None, max_length=100),
                limit: int | None = Query(None, ge=1, le=100),
                offset: int = Query(0, ge=0),
                session: Session = Depends(get_session)) -> list[MovieResponse]:
    query = _movie_query()
    if q and (term := q.strip()):
        query = query.where(Movie.title.ilike(f"%{term}%"))
    if genre and (genre_term := genre.strip()):
        query = query.where(Movie.genre.ilike(f"%{genre_term}%"))
    query = query.order_by(Movie.title.collate("NOCASE")).offset(offset)
    if limit is not None:
        query = query.limit(limit)
    movies = session.scalars(query).all()
    response.headers["Cache-Control"] = "private, max-age=30, stale-while-revalidate=120"
    response.headers["X-Result-Count"] = str(len(movies))
    response.headers["X-Next-Offset"] = str(offset + len(movies)) if limit and len(movies) == limit else ""
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
    kind = "poster" if attribute == "poster_path" else "backdrop"
    image = Path(value) if value and Path(value).is_file() else bundled_artwork(movie.title, movie.year, kind)
    if image is None: raise HTTPException(status_code=404, detail="Image not found")
    image_types = {
        ".avif": "image/avif", ".bmp": "image/bmp", ".gif": "image/gif",
        ".heic": "image/heic", ".heif": "image/heif", ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg", ".jfif": "image/jpeg", ".png": "image/png",
        ".svg": "image/svg+xml", ".tif": "image/tiff", ".tiff": "image/tiff",
        ".webp": "image/webp",
    }
    media_type = image_types.get(image.suffix.lower()) or mimetypes.guess_type(image.name)[0] or "application/octet-stream"
    try:
        target_size = (400, 600) if kind == "poster" else (1280, 720)
        image = optimized_artwork(image, target_size, kind)
        media_type = "image/jpeg"
    except (OSError, ValueError):
        # Unsupported/corrupt artwork remains available in its original form.
        pass
    return FileResponse(image, media_type=media_type, headers={"Cache-Control": "public, max-age=86400"})


@router.get("/{movie_id}/poster", response_model=None)
def get_poster(movie_id: int, session: Session = Depends(get_session)) -> FileResponse | Response:
    try: return _image(movie_id, "poster_path", session)
    except HTTPException as error:
        if error.detail != "Image not found": raise
        placeholder = '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="600"><rect width="400" height="600" fill="#202027"/><text x="200" y="320" text-anchor="middle" fill="#e50914" font-size="72">H</text></svg>'
        return Response(content=placeholder, media_type="image/svg+xml",
                        headers={"Cache-Control": "public, max-age=86400"})


@router.get("/{movie_id}/backdrop", response_model=None)
def get_backdrop(movie_id: int, session: Session = Depends(get_session)) -> FileResponse:
    return _image(movie_id, "backdrop_path", session)


def _trusted_movie(movie_id: int, request: Request, session: Session) -> tuple[Movie, Path]:
    movie = session.get(Movie, movie_id)
    if movie is None:
        raise HTTPException(status_code=404, detail="Movie not found")
    path = resolve_trusted_file(movie.file_path, request.app.state.settings.media_dirs)
    if path is None:
        raise HTTPException(status_code=404, detail="Movie file unavailable")
    return movie, path


def _stream_response(path: Path, range_header: str | None, head: bool = False) -> Response:
    size = path.stat().st_size
    headers = {"Accept-Ranges": "bytes"}
    status_code = 200
    byte_range = ByteRange(0, max(0, size - 1))
    if range_header:
        try:
            byte_range = parse_range_header(range_header, size)
        except InvalidRange:
            return Response(status_code=416, headers={"Accept-Ranges": "bytes", "Content-Range": f"bytes */{size}"})
        status_code = 206
        headers["Content-Range"] = f"bytes {byte_range.start}-{byte_range.end}/{size}"
    headers["Content-Length"] = str(byte_range.length if size else 0)
    content_type = video_content_type(path)
    if head:
        return Response(status_code=status_code, headers=headers, media_type=content_type)
    return StreamingResponse(iter_file_range(path, byte_range), status_code=status_code,
                             headers=headers, media_type=content_type)


@router.get("/{movie_id}/stream", response_model=None)
def stream_movie(movie_id: int, request: Request, range_header: str | None = Header(None, alias="Range"),
                 session: Session = Depends(get_session)) -> Response:
    _, path = _trusted_movie(movie_id, request, session)
    return _stream_response(path, range_header)


@router.head("/{movie_id}/stream", response_model=None)
def head_movie(movie_id: int, request: Request, range_header: str | None = Header(None, alias="Range"),
               session: Session = Depends(get_session)) -> Response:
    _, path = _trusted_movie(movie_id, request, session)
    return _stream_response(path, range_header, head=True)


@router.get("/{movie_id}/subtitles/{subtitle_id}", response_model=None)
def stream_subtitle(movie_id: int, subtitle_id: int, request: Request,
                    session: Session = Depends(get_session)) -> FileResponse:
    movie = session.get(Movie, movie_id)
    if movie is None:
        raise HTTPException(status_code=404, detail="Movie not found")
    subtitle = session.get(Subtitle, subtitle_id)
    if subtitle is None or subtitle.movie_id != movie_id:
        raise HTTPException(status_code=404, detail="Subtitle not found")
    path = resolve_trusted_file(subtitle.file_path, request.app.state.settings.media_dirs)
    if path is None:
        raise HTTPException(status_code=404, detail="Subtitle file unavailable")
    return FileResponse(path, media_type=subtitle_content_type(path), filename=subtitle.file_name)
