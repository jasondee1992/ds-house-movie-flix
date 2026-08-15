from datetime import datetime, timezone

from sqlalchemy import BigInteger, Boolean, DateTime, ForeignKey, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.database import Base


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class Movie(Base):
    __tablename__ = "movies"

    id: Mapped[int] = mapped_column(primary_key=True)
    title: Mapped[str] = mapped_column(String(500))
    file_name: Mapped[str] = mapped_column(String(1000))
    file_path: Mapped[str] = mapped_column(Text)
    normalized_path: Mapped[str] = mapped_column(Text, unique=True, index=True)
    media_root: Mapped[str] = mapped_column(Text, index=True)
    file_extension: Mapped[str] = mapped_column(String(16))
    file_size: Mapped[int] = mapped_column(BigInteger)
    date_added: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    date_modified: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    poster_path: Mapped[str | None] = mapped_column(Text, nullable=True)
    backdrop_path: Mapped[str | None] = mapped_column(Text, nullable=True)
    media_directory: Mapped[str | None] = mapped_column(Text, nullable=True)
    year: Mapped[int | None] = mapped_column(Integer, nullable=True)
    duration_seconds: Mapped[int | None] = mapped_column(Integer, nullable=True)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    genre: Mapped[str | None] = mapped_column(String(250), nullable=True)
    video_width: Mapped[int | None] = mapped_column(Integer, nullable=True)
    video_height: Mapped[int | None] = mapped_column(Integer, nullable=True)
    video_codec: Mapped[str | None] = mapped_column(String(64), nullable=True)
    audio_codec: Mapped[str | None] = mapped_column(String(64), nullable=True)
    audio_channels: Mapped[int | None] = mapped_column(Integer, nullable=True)
    bitrate: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    frame_rate: Mapped[str | None] = mapped_column(String(32), nullable=True)
    container_format: Mapped[str | None] = mapped_column(String(128), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, onupdate=utc_now)
    subtitles: Mapped[list["Subtitle"]] = relationship(
        back_populates="movie", cascade="all, delete-orphan", order_by="Subtitle.id"
    )


class Subtitle(Base):
    __tablename__ = "subtitles"

    id: Mapped[int] = mapped_column(primary_key=True)
    movie_id: Mapped[int] = mapped_column(ForeignKey("movies.id", ondelete="CASCADE"), index=True)
    file_name: Mapped[str] = mapped_column(String(1000))
    file_path: Mapped[str] = mapped_column(Text)
    normalized_path: Mapped[str] = mapped_column(Text, unique=True, index=True)
    language: Mapped[str] = mapped_column(String(100), default="Unknown")
    format: Mapped[str] = mapped_column(String(16))
    is_default: Mapped[bool] = mapped_column(Boolean, default=False)
    movie: Mapped[Movie] = relationship(back_populates="subtitles")
