import re
from pathlib import Path


_GENRES = {
    ("avengers age of ultron", 2015): "Action, Adventure, Science Fiction",
    ("avengers endgame", 2019): "Action, Adventure, Science Fiction",
    ("captain america brave new world", 2025): "Action, Adventure, Science Fiction",
    ("captain america civil war", 2016): "Action, Adventure, Science Fiction",
    ("deep water", 2026): "Horror, Thriller, Action",
    ("doctor strange in the multiverse of madness", 2022): "Action, Adventure, Fantasy",
    ("eternals", 2021): "Action, Adventure, Fantasy",
    ("fuze", 2025): "Thriller, Crime",
    ("insidious", 2010): "Horror, Mystery, Thriller",
    ("insidious chapter 2", 2013): "Horror, Mystery, Thriller",
    ("insidious chapter 3", 2015): "Horror, Mystery, Thriller",
    ("insidious the last key", 2018): "Horror, Mystery, Thriller",
    ("jackass best and last", 2026): "Comedy, Documentary",
    ("john wick", 2014): "Action, Thriller",
    ("michael", 2026): "Drama, Music, Biography",
    ("minions monsters", 2026): "Animation, Adventure, Comedy, Family",
    ("mortal kombat ii", 2026): "Action, Adventure, Fantasy",
    ("nobody", 2021): "Action, Thriller",
    ("ready or not here i come", 2026): "Horror, Comedy, Thriller",
    ("signal one", 2026): "Science Fiction, Drama",
    ("spider man homecoming", 2017): "Action, Adventure, Science Fiction",
    ("spider man no way home", 2021): "Action, Adventure, Science Fiction",
    ("the avengers", 2012): "Action, Adventure, Science Fiction",
    ("the fantastic four first steps", 2025): "Action, Adventure, Science Fiction",
    ("the marvels", 2023): "Action, Adventure, Science Fiction",
    ("thunderbolts", 2025): "Action, Adventure, Drama",
    ("war machine", 2026): "Action, Science Fiction, Thriller",
    ("young washington", 2026): "Drama, History, Biography",
}


def normalize_movie_name(value: str) -> str:
    value = re.sub(r"\s*\((?:19|20)\d{2}\)\s*$", "", value.strip())
    return re.sub(r"[^a-z0-9]+", " ", value.lower()).strip()


def sidecar_genre(movie_path: Path) -> str | None:
    files = {path.name.lower(): path for path in movie_path.parent.iterdir() if path.is_file()}
    for name in ("genre.txt", "genres.txt"):
        path = files.get(name)
        if path:
            try:
                value = path.read_text(encoding="utf-8-sig").strip()
                return value[:250] or None
            except OSError:
                return None
    return None


def catalog_genre(folder_name: str, year: int | None) -> str | None:
    return _GENRES.get((normalize_movie_name(folder_name), year))
