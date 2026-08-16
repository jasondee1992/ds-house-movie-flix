# HomeFlix

HomeFlix is a local-network, television-first media library. Phase 5 adds persistent playback progress, resume/start-over playback, and a Continue Watching TV row.

## Repository layout

- `android-tv/` — Kotlin and Jetpack Compose for TV client
- `backend/` — FastAPI, SQLAlchemy, SQLite, scanner, and tests
- `docs/` — development and emulator instructions

## Phase 5 quick start

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r backend\requirements.txt
Copy-Item backend\.env.example backend\.env
```

Set `HOMEFLIX_MEDIA_DIR` in the untracked `backend/.env` to any external Windows or NAS root, such as `F:\MOVIES`, `D:\MOVIES`, or `/volume1/MOVIES`. If it is unset or unavailable, HomeFlix starts with an empty library.

```powershell
cd backend
..\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
Invoke-RestMethod http://localhost:8000/api/library/scan -Method Post
```

Browse the API at `http://localhost:8000/docs`. Build the TV APK from the repository root:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :android-tv:app:assembleDebug
```

The APK is generated at `android-tv/app/build/outputs/apk/debug/app-debug.apk`. The emulator uses `http://10.0.2.2:8000/`; physical TVs should build with `-PHOMEFLIX_API_BASE_URL=http://<LAN-IP>:8000/`.

See [backend/README.md](backend/README.md) and [docs/development.md](docs/development.md) for complete configuration and test instructions.

## Phase 5 behavior and limitations

- No TMDB, scraping, or external metadata provider is used; missing descriptions and genres stay empty.
- Technical fields remain empty when `ffprobe` is unavailable.
- The Android TV app scans and refreshes the library automatically every 15 seconds while it is running; the manual scan API remains available.
- Progress is stored per movie for the current single-user setup. Profiles, favorites, ratings, and multi-user history remain future work.
- Movies become completed at 90% watched and leave Continue Watching. Items under 30 seconds are omitted.
- Active playback saves every 15 seconds and on pause, backgrounding, disposal, and Back.
- Playback is direct-play only: HomeFlix does not transcode unsupported codecs or containers.
- Embedded tracks are exposed by Media3 when the container/device decoder supports them; the backend does not extract tracks.
- Known pre-existing issue: video playback works on Google TV emulator, but audio output remains unverified/unresolved.
