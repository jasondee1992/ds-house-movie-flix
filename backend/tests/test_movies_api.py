from pathlib import Path

from fastapi.testclient import TestClient


def test_scan_and_movie_endpoints(client: TestClient, media_dir: Path) -> None:
    movie_file = media_dir / "Interstellar.mkv"
    movie_file.write_bytes(b"movie")
    (media_dir / "Interstellar.jpg").write_bytes(b"poster")

    scan = client.post("/api/library/scan")
    assert scan.status_code == 200
    assert scan.json()["added"] == 1

    movies = client.get("/api/movies")
    assert movies.status_code == 200
    payload = movies.json()
    assert len(payload) == 1
    assert payload[0]["title"] == "Interstellar"
    assert "file_path" not in payload[0]
    assert "media_directory" not in payload[0]

    movie_id = payload[0]["id"]
    details = client.get(f"/api/movies/{movie_id}")
    assert details.status_code == 200
    assert details.json()["poster_url"] == f"/api/movies/{movie_id}/poster"
    assert details.json()["backdrop_url"] == f"/api/movies/{movie_id}/backdrop"
    assert details.json()["stream_url"] == f"/api/movies/{movie_id}/stream"
    assert details.json()["subtitle_count"] == 0
    assert "F:\\" not in str(details.json())

    poster = client.get(f"/api/movies/{movie_id}/poster")
    assert poster.status_code == 200
    assert poster.headers["content-type"].startswith("image/jpeg")
    assert client.get(f"/api/movies/{movie_id}/backdrop").status_code == 404

    assert client.get("/api/movies/999999").status_code == 404


def test_empty_library_and_scan_are_useful(client: TestClient) -> None:
    assert client.get("/api/movies").json() == []
    result = client.post("/api/library/scan").json()
    assert result["status"] == "ok"
    assert result["scanned_files"] == 0


def test_bundled_artwork_is_used_when_matching_movie_has_no_images(
    client: TestClient, media_dir: Path,
) -> None:
    (media_dir / "Minions Monsters (2026).mp4").write_bytes(b"movie")
    client.post("/api/library/scan")
    movie_id = client.get("/api/movies").json()[0]["id"]

    poster = client.get(f"/api/movies/{movie_id}/poster")
    backdrop = client.get(f"/api/movies/{movie_id}/backdrop")

    assert poster.status_code == 200
    assert poster.headers["content-type"].startswith("image/jpeg")
    assert backdrop.status_code == 200
    assert backdrop.headers["content-type"].startswith("image/webp")
