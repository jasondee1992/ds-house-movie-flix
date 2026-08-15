import os
import re
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.movie import Movie, Subtitle
from app.services.media_probe import probe_media

SUPPORTED_VIDEO_EXTENSIONS = {".mp4", ".mkv", ".avi", ".mov", ".m4v", ".webm"}
IMAGE_EXTENSIONS = (".jpg", ".jpeg", ".png")
SUBTITLE_EXTENSIONS = {".srt", ".vtt", ".ass", ".ssa"}
YEAR_PATTERN = re.compile(r"^(?P<title>.+?)\s*\((?P<year>(?:19|20)\d{2})\)\s*$")


@dataclass
class ScanResult:
    scanned_files: int = 0
    added: int = 0
    updated: int = 0
    removed: int = 0
    ignored: int = 0
    missing_directories: list[str] = field(default_factory=list)


def normalize_path(path: Path) -> str:
    return os.path.normcase(str(path.resolve()))


def clean_title(stem: str) -> str:
    title = re.sub(r"[._]+", " ", stem)
    title = re.sub(r"\s+-\s+|(?<=\w)-(?=\w)", " ", title)
    return re.sub(r"\s+", " ", title).strip() or stem


def parse_title_year(stem: str) -> tuple[str, int | None]:
    match = YEAR_PATTERN.match(stem.strip())
    if match:
        return clean_title(match.group("title")), int(match.group("year"))
    return clean_title(stem), None


def _case_insensitive_files(directory: Path) -> dict[str, Path]:
    try:
        return {entry.name.lower(): entry for entry in directory.iterdir() if entry.is_file()}
    except OSError:
        return {}


def find_poster(movie_path: Path) -> Path | None:
    files = _case_insensitive_files(movie_path.parent)
    names = [f"poster{ext}" for ext in IMAGE_EXTENSIONS]
    names += [f"folder{ext}" for ext in IMAGE_EXTENSIONS] + [f"cover{ext}" for ext in IMAGE_EXTENSIONS]
    names += [f"{movie_path.stem}{ext}" for ext in IMAGE_EXTENSIONS]
    names += [f"{movie_path.stem}.poster{ext}" for ext in IMAGE_EXTENSIONS]
    return next((files[name.lower()].resolve() for name in names if name.lower() in files), None)


def find_backdrop(movie_path: Path) -> Path | None:
    files = _case_insensitive_files(movie_path.parent)
    names = [f"{base}{ext}" for base in ("backdrop", "background", "fanart", "hero") for ext in IMAGE_EXTENSIONS]
    names += [f"{movie_path.stem}.{kind}{ext}" for kind in ("backdrop", "background", "fanart", "hero") for ext in IMAGE_EXTENSIONS]
    return next((files[name.lower()].resolve() for name in names if name.lower() in files), None)


def subtitle_language(path: Path) -> str:
    tokens = {token.lower() for token in re.split(r"[._\-\s]+", path.stem) if token}
    mappings = {
        "English": {"english", "eng", "en"},
        "Filipino": {"filipino", "tagalog", "tl", "fil"},
        "Spanish": {"spanish", "spa", "es"},
        "French": {"french", "fra", "fre", "fr"},
    }
    for label, hints in mappings.items():
        if tokens & hints:
            return label
    return "Unknown"


def discover_subtitles(movie_path: Path) -> list[Path]:
    directories = [movie_path.parent]
    try:
        directories += [p for p in movie_path.parent.iterdir() if p.is_dir() and p.name.lower() == "subs"]
    except OSError:
        pass
    found: dict[str, Path] = {}
    for directory in directories:
        try:
            for path in directory.iterdir():
                if path.is_file() and path.suffix.lower() in SUBTITLE_EXTENSIONS and not path.name.startswith("."):
                    found[normalize_path(path)] = path.resolve()
        except OSError:
            continue
    return sorted(found.values(), key=lambda p: p.name.lower())


def _is_hidden_or_system(path: Path) -> bool:
    if path.name.startswith("."):
        return True
    attributes = getattr(path.stat(), "st_file_attributes", 0)
    return bool(attributes & (0x2 | 0x4))


def _iter_visible_files(root: Path):
    for current, directories, files in os.walk(root):
        directories[:] = [name for name in directories if not _is_hidden_or_system(Path(current) / name)]
        for name in files:
            path = Path(current) / name
            if not _is_hidden_or_system(path):
                yield path


def _sync_subtitles(session: Session, movie: Movie, paths: list[Path]) -> bool:
    existing = {subtitle.normalized_path: subtitle for subtitle in movie.subtitles}
    discovered = {normalize_path(path): path for path in paths}
    changed = False
    for key, path in discovered.items():
        language = subtitle_language(path)
        if key not in existing:
            movie.subtitles.append(Subtitle(file_name=path.name, file_path=str(path), normalized_path=key,
                                            language=language, format=path.suffix.lower().lstrip("."), is_default=False))
            changed = True
        else:
            subtitle = existing[key]
            if subtitle.file_name != path.name or subtitle.language != language:
                subtitle.file_name, subtitle.language = path.name, language
                changed = True
    for key, subtitle in existing.items():
        if key not in discovered:
            session.delete(subtitle)
            changed = True
    return changed


def _values_changed(movie: Movie, values: dict) -> bool:
    for key, value in values.items():
        stored = getattr(movie, key)
        if key == "date_modified" and stored is not None and value is not None:
            if stored.tzinfo is None:
                stored = stored.replace(tzinfo=timezone.utc)
        if stored != value:
            return True
    return False


def _enrich(movie: Movie, path: Path) -> None:
    metadata = probe_media(path)
    if metadata:
        for field_name in metadata.__dataclass_fields__:
            setattr(movie, field_name, getattr(metadata, field_name))


def scan_media_directories(session: Session, media_dirs: tuple[Path, ...]) -> ScanResult:
    result, now = ScanResult(), datetime.now(timezone.utc)
    for configured_root in media_dirs:
        if not configured_root.is_dir():
            result.missing_directories.append(str(configured_root)); continue
        root, root_key = configured_root.resolve(), normalize_path(configured_root)
        existing = {m.normalized_path: m for m in session.scalars(select(Movie).where(Movie.media_root == root_key)).all()}
        discovered: set[str] = set()
        for path in _iter_visible_files(root):
            if path.suffix.lower() not in SUPPORTED_VIDEO_EXTENSIONS:
                result.ignored += 1; continue
            result.scanned_files += 1
            path, path_key = path.resolve(), normalize_path(path)
            discovered.add(path_key)
            stat = path.stat(); modified = datetime.fromtimestamp(stat.st_mtime, tz=timezone.utc)
            title_source = path.parent.name if YEAR_PATTERN.match(path.parent.name.strip()) else path.stem
            title, year = parse_title_year(title_source)
            poster, backdrop = find_poster(path), find_backdrop(path)
            values = dict(title=title, year=year, file_name=path.name, file_path=str(path), normalized_path=path_key,
                          media_root=root_key, media_directory=str(path.parent), file_extension=path.suffix.lower(),
                          file_size=stat.st_size, date_modified=modified,
                          poster_path=str(poster) if poster else None, backdrop_path=str(backdrop) if backdrop else None)
            movie = existing.get(path_key)
            if movie is None:
                movie = Movie(**values, date_added=now); session.add(movie); session.flush(); _enrich(movie, path)
                _sync_subtitles(session, movie, discover_subtitles(path)); result.added += 1
            else:
                changed = _values_changed(movie, values)
                for key, value in values.items(): setattr(movie, key, value)
                subtitle_changed = _sync_subtitles(session, movie, discover_subtitles(path))
                if changed:
                    _enrich(movie, path)
                if changed or subtitle_changed:
                    movie.updated_at = now; result.updated += 1
        for key, movie in existing.items():
            if key not in discovered:
                session.delete(movie); result.removed += 1
    session.commit()
    return result
