from pathlib import Path

from sqlalchemy import select

from app.db.database import Database
from app.models.movie import Movie
from app.services.media_scanner import (clean_title, discover_subtitles, find_backdrop,
    find_poster, parse_title_year, scan_media_directories)
from app.models.movie import Subtitle


def test_scanner_discovers_nested_mixed_case_and_handles_rescan(tmp_path: Path) -> None:
    media = tmp_path / "movies"
    nested = media / "Marvel"
    nested.mkdir(parents=True)
    first = media / "Avengers.Endgame.2019.mp4"
    second = nested / "Iron_Man.MKV"
    first.touch()
    second.touch()
    (media / "README.txt").touch()
    (media / ".hidden.mkv").touch()
    (media / "subtitle.srt").touch()

    database = Database(f"sqlite:///{(tmp_path / 'scanner.db').as_posix()}")
    database.create_tables()
    with database.session_factory() as session:
        initial = scan_media_directories(session, (media,))
        movies = session.scalars(select(Movie).order_by(Movie.title)).all()
        assert initial.scanned_files == 2
        assert initial.added == 2
        assert initial.ignored == 2
        assert [movie.title for movie in movies] == ["Avengers Endgame 2019", "Iron Man"]

        rescan = scan_media_directories(session, (media,))
        assert rescan.added == 0
        assert rescan.updated == 0
        assert session.query(Movie).count() == 2

        second.unlink()
        removed = scan_media_directories(session, (media,))
        assert removed.removed == 1
        assert session.query(Movie).count() == 1


def test_scanner_missing_directory_is_safe(tmp_path: Path) -> None:
    database = Database(f"sqlite:///{(tmp_path / 'missing.db').as_posix()}")
    database.create_tables()
    missing = tmp_path / "not-there"
    with database.session_factory() as session:
        result = scan_media_directories(session, (missing,))
    assert result.scanned_files == 0
    assert result.missing_directories == [str(missing)]


def test_title_cleaning_is_conservative() -> None:
    assert clean_title("The_Dark_Knight") == "The Dark Knight"
    assert clean_title("Spider-Man - Homecoming") == "Spider Man Homecoming"


def test_terminal_parenthesized_year_parsing_is_conservative() -> None:
    assert parse_title_year("Minions Monsters (2026)") == ("Minions Monsters", 2026)
    assert parse_title_year("The Dark Knight (2008)") == ("The Dark Knight", 2008)
    assert parse_title_year("2001 A Space Odyssey") == ("2001 A Space Odyssey", None)


def test_folder_artwork_and_subtitles_are_discovered(tmp_path: Path) -> None:
    folder = tmp_path / "Interstellar (2014)"; subs = folder / "Subs"; subs.mkdir(parents=True)
    movie = folder / "Interstellar (2014).MP4"; movie.touch()
    (folder / "POSTER.JPG").touch(); (folder / "backdrop.png").touch()
    (folder / "Interstellar (2014).SRT").touch(); (subs / "English.vTt").touch(); (subs / "tl.ASS").touch()
    assert find_poster(movie).name == "POSTER.JPG"
    assert find_backdrop(movie).name == "backdrop.png"
    assert {p.suffix.lower() for p in discover_subtitles(movie)} == {".srt", ".vtt", ".ass"}


def test_subtitle_deduplication_and_deleted_cleanup(tmp_path: Path, monkeypatch) -> None:
    monkeypatch.setattr("app.services.media_scanner.probe_media", lambda path: None)
    media = tmp_path / "movies"; folder = media / "Film (2020)"; (folder / "Subs").mkdir(parents=True)
    (folder / "Film (2020).mp4").touch(); subtitle = folder / "Subs" / "English.srt"; subtitle.touch()
    database = Database(f"sqlite:///{(tmp_path / 'subs.db').as_posix()}"); database.create_tables()
    with database.session_factory() as session:
        scan_media_directories(session, (media,)); scan_media_directories(session, (media,))
        assert session.query(Subtitle).count() == 1
        subtitle.unlink(); scan_media_directories(session, (media,))
        assert session.query(Subtitle).count() == 0
