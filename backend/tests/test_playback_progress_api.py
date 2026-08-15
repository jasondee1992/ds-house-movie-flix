from pathlib import Path

from fastapi.testclient import TestClient


def add_movies(client: TestClient, media_dir: Path, *names: str) -> list[int]:
    for name in names:
        (media_dir / f"{name}.mkv").write_bytes(b"movie")
    assert client.post("/api/library/scan").status_code == 200
    movies = {movie["title"]: movie["id"] for movie in client.get("/api/movies").json()}
    return [movies[name] for name in names]


def test_progress_create_update_percentage_and_single_record(client: TestClient, media_dir: Path) -> None:
    movie_id = add_movies(client, media_dir, "Progress Movie")[0]
    empty = client.get(f"/api/movies/{movie_id}/progress")
    assert empty.status_code == 200
    assert empty.json() == {"movie_id": movie_id, "position_ms": 0, "duration_ms": 0,
                            "progress_percent": 0.0, "completed": False, "last_watched_at": None}

    created = client.put(f"/api/movies/{movie_id}/progress",
                         json={"position_ms": 2_538_000, "duration_ms": 5_400_000})
    assert created.status_code == 200
    assert created.json()["progress_percent"] == 47.0
    assert created.json()["completed"] is False

    updated = client.put(f"/api/movies/{movie_id}/progress",
                         json={"position_ms": 2_700_000, "duration_ms": 5_400_000})
    assert updated.json()["progress_percent"] == 50.0
    assert len(client.get("/api/continue-watching").json()) == 1


def test_completion_threshold_and_replay_reset(client: TestClient, media_dir: Path) -> None:
    movie_id = add_movies(client, media_dir, "Completion Movie")[0]
    completed = client.put(f"/api/movies/{movie_id}/progress",
                           json={"position_ms": 900_000, "duration_ms": 1_000_000}).json()
    assert completed["completed"] is True
    assert client.get("/api/continue-watching").json() == []

    replayed = client.put(f"/api/movies/{movie_id}/progress",
                          json={"position_ms": 45_000, "duration_ms": 1_000_000}).json()
    assert replayed["completed"] is False
    assert len(client.get("/api/continue-watching").json()) == 1


def test_continue_watching_order_exclusions_and_path_safety(client: TestClient, media_dir: Path) -> None:
    first, second, tiny, done = add_movies(client, media_dir, "First", "Second", "Tiny", "Done")
    client.put(f"/api/movies/{first}/progress", json={"position_ms": 60_000, "duration_ms": 600_000})
    client.put(f"/api/movies/{second}/progress", json={"position_ms": 70_000, "duration_ms": 600_000})
    client.put(f"/api/movies/{tiny}/progress", json={"position_ms": 29_999, "duration_ms": 600_000})
    client.put(f"/api/movies/{done}/progress", json={"position_ms": 540_000, "duration_ms": 600_000})

    payload = client.get("/api/continue-watching").json()
    assert [item["movie"]["id"] for item in payload] == [second, first]
    assert all("file_path" not in str(item) and "media_directory" not in str(item) for item in payload)
    assert str(media_dir) not in str(payload)


def test_delete_progress_and_invalid_movie(client: TestClient, media_dir: Path) -> None:
    movie_id = add_movies(client, media_dir, "Delete Movie")[0]
    client.put(f"/api/movies/{movie_id}/progress", json={"position_ms": 60_000, "duration_ms": 600_000})
    assert client.delete(f"/api/movies/{movie_id}/progress").status_code == 204
    assert client.get(f"/api/movies/{movie_id}/progress").json()["position_ms"] == 0
    assert client.get("/api/movies/999999/progress").status_code == 404
    assert client.put("/api/movies/999999/progress", json={"position_ms": 0, "duration_ms": 1}).status_code == 404
    assert client.delete("/api/movies/999999/progress").status_code == 404
