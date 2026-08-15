from collections.abc import Generator

from sqlalchemy import create_engine, inspect, text
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker


class Base(DeclarativeBase):
    pass


class Database:
    def __init__(self, url: str) -> None:
        connect_args = {"check_same_thread": False} if url.startswith("sqlite") else {}
        self.engine = create_engine(url, connect_args=connect_args)
        self.session_factory = sessionmaker(bind=self.engine, expire_on_commit=False)

    def create_tables(self) -> None:
        from app.models.movie import Movie, Subtitle  # noqa: F401

        Base.metadata.create_all(self.engine)
        # Phase 2 shipped without a migration framework. Additive columns preserve
        # the existing table, rows, and stable movie IDs.
        columns = {column["name"] for column in inspect(self.engine).get_columns("movies")}
        additions = {
            "backdrop_path": "TEXT", "media_directory": "TEXT",
            "video_width": "INTEGER", "video_height": "INTEGER",
            "video_codec": "VARCHAR(64)", "audio_codec": "VARCHAR(64)",
            "audio_channels": "INTEGER", "bitrate": "BIGINT",
            "frame_rate": "VARCHAR(32)", "container_format": "VARCHAR(128)",
        }
        with self.engine.begin() as connection:
            for name, sql_type in additions.items():
                if name not in columns:
                    connection.execute(text(f"ALTER TABLE movies ADD COLUMN {name} {sql_type}"))

    def session(self) -> Generator[Session, None, None]:
        with self.session_factory() as session:
            yield session
