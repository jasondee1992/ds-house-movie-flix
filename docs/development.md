# HomeFlix Phase 2 development

## Configure and run the backend

Create `backend/.env` from `.env.example` and set an external movie folder:

```env
HOMEFLIX_MEDIA_DIR=E:\MOVIES
HOMEFLIX_DATABASE_URL=sqlite:///./homeflix.db
```

Nothing is copied from that directory. Multiple roots can be separated with `;` on Windows or `:` on Linux. Missing directories are reported safely and result in an empty library on a fresh database.

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r backend\requirements.txt
cd backend
..\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Trigger a scan with Swagger at `http://localhost:8000/docs`, or:

```powershell
Invoke-RestMethod http://localhost:8000/api/library/scan -Method Post
```

Inspect the collection at `GET /api/movies`. Rescans update changed files without duplicating stable path-based records and remove records for deleted files when their root is available.

## Google TV emulator

1. Open **Tools > Device Manager** in Android Studio.
2. Choose **Add a new device > Create Virtual Device > TV**.
3. Select **Google TV (1080p)**; use **Android TV (1080p)** only if Google TV is unavailable.
4. Install/select an **API 35 x86_64 Google TV** image on Intel/AMD Windows. API 34 is acceptable.
5. Configure at least **2 GB RAM** (3–4 GB preferred), 512 MB VM heap, and **hardware/automatic graphics**.
6. Start the TV AVD. Do not use a Pixel/phone AVD for primary validation.

Arrow keys emulate the D-pad, Enter selects, and Escape sends Back. Start the backend before the app. `10.0.2.2` maps from the emulator to the host computer.

## Build, test, and install

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :android-tv:app:assembleDebug
.\gradlew.bat :android-tv:app:testDebugUnitTest
.\gradlew.bat :android-tv:app:lintDebug
adb install -r android-tv\app\build\outputs\apk\debug\app-debug.apk
```

For a physical TV:

```powershell
.\gradlew.bat :android-tv:app:assembleDebug -PHOMEFLIX_API_BASE_URL=http://192.168.1.10:8000/
```

Keep the trailing slash. The TV and server must share a trusted LAN, TCP 8000 must be allowed on the host's private firewall profile, and ADB/sideloading must be enabled on the TV.

## Backend tests

```powershell
.\.venv\Scripts\python.exe -m pip install -r backend\requirements-dev.txt
cd backend
..\.venv\Scripts\python.exe -m pytest -q --basetemp=.test-tmp
```

**Video playback is NOT implemented in Phase 2.** The disabled details action makes that limitation explicit.
