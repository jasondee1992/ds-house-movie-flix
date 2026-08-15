import re
from dataclasses import dataclass
from pathlib import Path
from collections.abc import Iterator

CHUNK_SIZE = 1024 * 1024
RANGE_PATTERN = re.compile(r"^bytes=(\d*)-(\d*)$")
VIDEO_CONTENT_TYPES = {
    ".mp4": "video/mp4", ".m4v": "video/mp4", ".webm": "video/webm",
    ".mkv": "video/x-matroska", ".avi": "video/x-msvideo", ".mov": "video/quicktime",
}
SUBTITLE_CONTENT_TYPES = {
    ".srt": "application/x-subrip; charset=utf-8", ".vtt": "text/vtt; charset=utf-8",
    ".ass": "text/plain; charset=utf-8", ".ssa": "text/plain; charset=utf-8",
}


class InvalidRange(ValueError):
    pass


@dataclass(frozen=True)
class ByteRange:
    start: int
    end: int

    @property
    def length(self) -> int:
        return self.end - self.start + 1


def parse_range_header(value: str, size: int) -> ByteRange:
    match = RANGE_PATTERN.fullmatch(value.strip())
    if not match or size <= 0:
        raise InvalidRange
    start_text, end_text = match.groups()
    if not start_text and not end_text:
        raise InvalidRange
    if not start_text:
        suffix = int(end_text)
        if suffix <= 0:
            raise InvalidRange
        return ByteRange(max(0, size - suffix), size - 1)
    start = int(start_text)
    if start >= size:
        raise InvalidRange
    end = int(end_text) if end_text else size - 1
    if end < start:
        raise InvalidRange
    return ByteRange(start, min(end, size - 1))


def resolve_trusted_file(stored_path: str, media_roots: tuple[Path, ...]) -> Path | None:
    try:
        candidate = Path(stored_path).resolve(strict=True)
        roots = [root.resolve(strict=True) for root in media_roots]
    except (OSError, RuntimeError):
        return None
    if not candidate.is_file() or not any(candidate.is_relative_to(root) for root in roots):
        return None
    return candidate


def iter_file_range(path: Path, byte_range: ByteRange, chunk_size: int = CHUNK_SIZE) -> Iterator[bytes]:
    remaining = byte_range.length
    with path.open("rb") as handle:
        handle.seek(byte_range.start)
        while remaining:
            chunk = handle.read(min(chunk_size, remaining))
            if not chunk:
                break
            remaining -= len(chunk)
            yield chunk


def video_content_type(path: Path) -> str:
    return VIDEO_CONTENT_TYPES.get(path.suffix.lower(), "application/octet-stream")


def subtitle_content_type(path: Path) -> str:
    return SUBTITLE_CONTENT_TYPES.get(path.suffix.lower(), "text/plain; charset=utf-8")
