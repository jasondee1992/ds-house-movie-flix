# HomeFlix API

The Phase 4 API scans external media directories and provides safe ID-based direct streaming for movies and sidecar subtitles. Media files are always treated as read-only.

## Setup

Requires Python 3.11 or newer. From the repository root:

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r backend\requirements.txt
Copy-Item backend\.env.example backend\.env
```

Edit `backend/.env`:

```env
HOMEFLIX_MEDIA_DIR=F:\MOVIES
HOMEFLIX_DATABASE_URL=sqlite:///./homeflix.db
```

The media directory is optional. A missing or unset directory produces a usable empty library. Multiple directories use the platform path separator: semicolon on Windows and colon on Linux. A Synology path can be `/volume1/MOVIES`.

Start the API from `backend/` so the relative SQLite location is predictable:

```powershell
cd backend
..\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Tables are created automatically. Scanning is explicit so API startup stays quick even with large libraries:

```powershell
Invoke-RestMethod http://localhost:8000/api/library/scan -Method Post
```

Use `http://localhost:8000/docs` to scan and inspect the movie endpoints interactively.

## Scanner behavior

The scanner recursively discovers `.mp4`, `.mkv`, `.avi`, `.mov`, `.m4v`, and `.webm` files case-insensitively. It ignores hidden entries and unsupported files. Normalized absolute paths are stored only internally as unique keys. New files are inserted, changed files are updated, unchanged files retain their rows, and deleted files are removed on a successful scan of their media root. An unavailable root is reported in `missing_directories` and its existing rows are retained to avoid data loss during a temporary drive/NAS outage.

The video's parent folder is its primary media directory. A terminal parenthesized year, such as `Movie Name (2026)`, is parsed conservatively. Posters (`poster.*`, `folder.jpg`, `cover.jpg`, or a matching movie name), horizontal backdrops (`backdrop.*`, `background.*`, `fanart.*`, or `hero.*`), and `.srt`, `.vtt`, `.ass`, and `.ssa` subtitles beside the video or in `Subs/` are discovered without exposing their paths.

When `ffprobe` is on `PATH`, scans also collect duration, dimensions, codecs, channels, bitrate, frame rate, and container. If it is absent or a file cannot be probed, scanning continues and those nullable fields remain empty.

Existing Phase 2 databases are upgraded additively at startup. Movie rows and IDs are retained; nullable media columns and the `subtitles` table are added without recreation.

## Streaming

`GET /api/movies/{movie_id}/stream` supports full responses and single HTTP byte ranges. `HEAD` returns the same useful size, MIME, and `Accept-Ranges` headers without a body. Invalid or unsatisfiable ranges return `416` with `Content-Range: bytes */TOTAL`. Movie data is read in bounded 1 MiB chunks and is never loaded entirely into memory.

Sidecar subtitles are available at `GET /api/movies/{movie_id}/subtitles/{subtitle_id}`. Both endpoints resolve database-owned paths and confirm the canonical file remains inside a configured media root. Neither accepts client-provided paths or exposes absolute paths.

## Tests

```powershell
cd backend
..\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
..\.venv\Scripts\python.exe -m pytest -q --basetemp=.test-tmp
```
