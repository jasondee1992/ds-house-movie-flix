from pathlib import Path

from fastapi.testclient import TestClient
from sqlalchemy import select

from app.models.movie import Movie, Subtitle
from app.services.media_stream import ByteRange, InvalidRange, parse_range_header


def _scan_movie(client: TestClient, media_dir: Path, name: str = "Sample (2026).mp4") -> tuple[int, bytes]:
    payload = bytes(range(256)) * 32
    (media_dir / name).write_bytes(payload)
    assert client.post("/api/library/scan").status_code == 200
    movie = next(item for item in client.get("/api/movies").json() if item["title"] == Path(name).stem)
    return movie["id"], payload


def test_range_parser() -> None:
    assert parse_range_header("bytes=0-99", 1000) == ByteRange(0, 99)
    assert parse_range_header("bytes=500-", 1000) == ByteRange(500, 999)
    assert parse_range_header("bytes=-100", 1000) == ByteRange(900, 999)
    assert parse_range_header("bytes=900-2000", 1000) == ByteRange(900, 999)
    for value in ("bytes=", "items=0-1", "bytes=-0", "bytes=-1-2", "bytes=1000-", "bytes=10-5"):
        try:
            parse_range_header(value, 1000)
            raise AssertionError(f"accepted invalid range {value}")
        except InvalidRange:
            pass


def test_full_head_and_partial_movie_streaming(client: TestClient, media_dir: Path) -> None:
    movie_id, payload = _scan_movie(client, media_dir)
    full = client.get(f"/api/movies/{movie_id}/stream")
    assert full.status_code == 200 and full.content == payload
    assert full.headers["accept-ranges"] == "bytes"
    assert full.headers["content-length"] == str(len(payload))
    assert full.headers["content-type"].startswith("video/mp4")

    head = client.head(f"/api/movies/{movie_id}/stream")
    assert head.status_code == 200 and head.content == b""
    assert head.headers["content-length"] == str(len(payload))

    cases = [("bytes=0-1023", 0, 1023), ("bytes=1024-2047", 1024, 2047),
             ("bytes=4096-", 4096, len(payload) - 1)]
    for header, start, end in cases:
        response = client.get(f"/api/movies/{movie_id}/stream", headers={"Range": header})
        assert response.status_code == 206
        assert response.content == payload[start:end + 1]
        assert response.headers["content-range"] == f"bytes {start}-{end}/{len(payload)}"
        assert response.headers["content-length"] == str(end - start + 1)
        assert response.headers["accept-ranges"] == "bytes"


def test_invalid_missing_and_unsafe_movie_streams(client: TestClient, media_dir: Path, tmp_path: Path) -> None:
    movie_id, payload = _scan_movie(client, media_dir)
    invalid = client.get(f"/api/movies/{movie_id}/stream", headers={"Range": "bytes=99999-"})
    assert invalid.status_code == 416
    assert invalid.headers["content-range"] == f"bytes */{len(payload)}"
    assert "file_path" not in invalid.text and str(media_dir) not in invalid.text
    assert client.get("/api/movies/999999/stream").status_code == 404

    (media_dir / "Sample (2026).mp4").unlink()
    missing = client.get(f"/api/movies/{movie_id}/stream")
    assert missing.status_code == 404 and str(media_dir) not in missing.text

    outside = tmp_path / "outside.mp4"; outside.write_bytes(b"secret")
    database = client.app.state.database
    with database.session_factory() as session:
        movie = session.get(Movie, movie_id); movie.file_path = str(outside); session.commit()
    unsafe = client.get(f"/api/movies/{movie_id}/stream")
    assert unsafe.status_code == 404 and b"secret" not in unsafe.content


def test_subtitle_streaming_ownership_content_type_and_safety(client: TestClient, media_dir: Path, tmp_path: Path) -> None:
    first_id, _ = _scan_movie(client, media_dir)
    subtitle_file = media_dir / "English.srt"; subtitle_file.write_text("1\n00:00:00,000 --> 00:00:01,000\nHello\n", encoding="utf-8")
    second_folder = media_dir / "Other (2025)"; second_folder.mkdir()
    (second_folder / "Other (2025).mkv").write_bytes(b"other")
    (second_folder / "Other.vtt").write_text("WEBVTT\n", encoding="utf-8")
    client.post("/api/library/scan")
    movies = client.get("/api/movies").json()
    first = next(item for item in movies if item["id"] == first_id)
    second = next(item for item in movies if item["title"] == "Other (2025)")
    subtitle_id = first["subtitles"][0]["id"]
    valid = client.get(f"/api/movies/{first_id}/subtitles/{subtitle_id}")
    assert valid.status_code == 200 and b"Hello" in valid.content
    assert valid.headers["content-type"].startswith("application/x-subrip")
    assert client.get(f"/api/movies/{first_id}/subtitles/999999").status_code == 404
    other_subtitle = second["subtitles"][0]["id"]
    assert client.get(f"/api/movies/{first_id}/subtitles/{other_subtitle}").status_code == 404

    subtitle_file.unlink()
    assert client.get(f"/api/movies/{first_id}/subtitles/{subtitle_id}").status_code == 404
    outside = tmp_path / "outside.srt"; outside.write_text("secret")
    with client.app.state.database.session_factory() as session:
        subtitle = session.get(Subtitle, subtitle_id); subtitle.file_path = str(outside); session.commit()
    unsafe = client.get(f"/api/movies/{first_id}/subtitles/{subtitle_id}")
    assert unsafe.status_code == 404 and "secret" not in unsafe.text
