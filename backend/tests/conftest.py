from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.core.config import Settings
from app.main import create_app


@pytest.fixture
def media_dir(tmp_path: Path) -> Path:
    directory = tmp_path / "media"
    directory.mkdir()
    return directory


@pytest.fixture
def client(tmp_path: Path, media_dir: Path) -> TestClient:
    database_path = (tmp_path / "test.db").as_posix()
    app = create_app(
        Settings(
            database_url=f"sqlite:///{database_path}",
            media_dirs=(media_dir,),
        )
    )
    return TestClient(app)

