import json
import logging
import shutil
import subprocess
from dataclasses import dataclass
from pathlib import Path

logger = logging.getLogger(__name__)
_warned_missing = False


@dataclass(frozen=True)
class MediaMetadata:
    duration_seconds: int | None = None
    video_width: int | None = None
    video_height: int | None = None
    video_codec: str | None = None
    audio_codec: str | None = None
    audio_channels: int | None = None
    bitrate: int | None = None
    frame_rate: str | None = None
    container_format: str | None = None


def parse_ffprobe_output(payload: dict) -> MediaMetadata:
    streams = payload.get("streams", [])
    video = next((s for s in streams if s.get("codec_type") == "video"), {})
    audio = next((s for s in streams if s.get("codec_type") == "audio"), {})
    fmt = payload.get("format", {})
    duration = fmt.get("duration") or video.get("duration")
    return MediaMetadata(
        duration_seconds=round(float(duration)) if duration else None,
        video_width=video.get("width"), video_height=video.get("height"),
        video_codec=video.get("codec_name"), audio_codec=audio.get("codec_name"),
        audio_channels=audio.get("channels"),
        bitrate=int(fmt["bit_rate"]) if fmt.get("bit_rate") else None,
        frame_rate=video.get("avg_frame_rate") or video.get("r_frame_rate"),
        container_format=fmt.get("format_name"),
    )


def probe_media(path: Path) -> MediaMetadata | None:
    global _warned_missing
    executable = shutil.which("ffprobe")
    if not executable:
        if not _warned_missing:
            logger.warning("ffprobe is unavailable; technical media metadata will remain empty")
            _warned_missing = True
        return None
    try:
        completed = subprocess.run(
            [executable, "-v", "error", "-show_streams", "-show_format", "-of", "json", str(path)],
            check=True, capture_output=True, text=True, timeout=60,
        )
        return parse_ffprobe_output(json.loads(completed.stdout))
    except (subprocess.SubprocessError, json.JSONDecodeError, OSError, ValueError) as exc:
        logger.warning("Could not probe %s: %s", path.name, exc)
        return None
