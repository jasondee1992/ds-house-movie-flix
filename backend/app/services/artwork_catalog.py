import re
from pathlib import Path


ARTWORK_DIRECTORY = Path(__file__).resolve().parent.parent / "static" / "artwork"

_ARTWORK = {
    ("jackass best and last", 2026): {
        "poster": "jackass-best-and-last-2026-poster.jpg",
        "backdrop": "jackass-best-and-last-2026-backdrop.jpg",
    },
    ("minions monsters", 2026): {
        "poster": "minions-and-monsters-2026-poster.jpg",
        "backdrop": "minions-and-monsters-2026-backdrop.webp",
    },
}


def _normalize_title(title: str) -> str:
    normalized = re.sub(r"[^a-z0-9]+", " ", title.lower()).strip()
    return re.sub(r"\s+(?:19|20)\d{2}$", "", normalized).strip()


def bundled_artwork(title: str, year: int | None, kind: str) -> Path | None:
    """Return curated artwork when a movie folder has no local images."""
    normalized = _normalize_title(title)
    candidates = ((normalized, year), (normalized, None))
    for key in candidates:
        file_name = _ARTWORK.get(key, {}).get(kind)
        if file_name:
            path = ARTWORK_DIRECTORY / file_name
            if path.is_file():
                return path
    return None
