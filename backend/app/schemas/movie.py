from datetime import datetime
from pydantic import BaseModel, ConfigDict


class SubtitleResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    language: str
    format: str
    is_default: bool
    url: str


class MovieResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    title: str
    year: int | None
    duration_seconds: int | None
    description: str | None
    genre: str | None
    poster_url: str
    backdrop_url: str
    stream_url: str
    file_extension: str
    file_size: int
    date_added: datetime
    video_width: int | None
    video_height: int | None
    quality: str | None
    video_codec: str | None
    audio_codec: str | None
    audio_channels: int | None
    bitrate: int | None
    frame_rate: str | None
    container_format: str | None
    subtitle_count: int
    subtitles: list[SubtitleResponse]


class ScanResponse(BaseModel):
    status: str
    scanned_files: int
    added: int
    updated: int
    removed: int
    ignored: int
    missing_directories: list[str]
