from pathlib import Path

from fastapi.testclient import TestClient


def _add_movies(client: TestClient, media_dir: Path) -> dict[str, int]:
    for name, genre in (("Action One", "Action, Adventure"), ("Comedy One", "Comedy"),
                        ("Action Two", "Action")):
        folder = media_dir / name
        folder.mkdir()
        (folder / f"{name}.mp4").write_bytes(b"movie")
        (folder / "genre.txt").write_text(genre, encoding="utf-8")
    assert client.post("/api/library/scan").status_code == 200
    return {item["title"]: item["id"] for item in client.get("/api/movies").json()}


def test_home_feed_is_compact_grouped_and_contains_progress(client: TestClient, media_dir: Path) -> None:
    movies = _add_movies(client, media_dir)
    client.put(f"/api/movies/{movies['Action One']}/progress",
               json={"position_ms": 60_000, "duration_ms": 600_000})
    response = client.get("/api/home?row_limit=2")
    assert response.status_code == 200
    assert "stale-while-revalidate" in response.headers["cache-control"]
    payload = response.json()
    assert payload["continue_watching"][0]["movie"]["id"] == movies["Action One"]
    assert "subtitles" not in payload["continue_watching"][0]["movie"]
    rows = {row["title"]: row for row in payload["rows"]}
    assert len(rows["Recently Added"]["items"]) == 2
    assert {item["title"] for item in rows["Action"]["items"]} == {"Action One", "Action Two"}


def test_movie_search_genre_and_pagination(client: TestClient, media_dir: Path) -> None:
    _add_movies(client, media_dir)
    search = client.get("/api/movies?q=action&genre=adventure&limit=1")
    assert [item["title"] for item in search.json()] == ["Action One"]
    assert search.headers["x-next-offset"] == "1"
    page = client.get("/api/movies?limit=2&offset=2")
    assert len(page.json()) == 1
    assert page.headers["x-next-offset"] == ""
