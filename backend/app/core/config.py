import os
from dataclasses import dataclass, field
from pathlib import Path

from dotenv import load_dotenv

load_dotenv()


@dataclass(frozen=True)
class Settings:
    api_name: str = "HomeFlix API"
    version: str = "0.3.0"
    database_url: str = field(
        default_factory=lambda: os.getenv("HOMEFLIX_DATABASE_URL", "sqlite:///./homeflix.db")
    )
    media_dirs: tuple[Path, ...] = field(default_factory=lambda: _media_dirs_from_env())


def _media_dirs_from_env() -> tuple[Path, ...]:
    value = os.getenv("HOMEFLIX_MEDIA_DIR", "").strip()
    if not value:
        return ()
    return tuple(Path(item.strip()).expanduser() for item in value.split(os.pathsep) if item.strip())


settings = Settings()
