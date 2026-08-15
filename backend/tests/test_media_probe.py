import json
from pathlib import Path
from app.services.media_probe import parse_ffprobe_output, probe_media


def test_ffprobe_metadata_parsing() -> None:
    metadata = parse_ffprobe_output({"streams": [
        {"codec_type": "video", "codec_name": "h264", "width": 1920, "height": 1080, "avg_frame_rate": "24000/1001"},
        {"codec_type": "audio", "codec_name": "aac", "channels": 6}],
        "format": {"duration": "5342.4", "bit_rate": "8000000", "format_name": "mov,mp4"}})
    assert metadata.duration_seconds == 5342
    assert (metadata.video_width, metadata.video_height) == (1920, 1080)
    assert metadata.audio_channels == 6


def test_missing_ffprobe_is_safe(monkeypatch, tmp_path: Path) -> None:
    monkeypatch.setattr("app.services.media_probe.shutil.which", lambda _: None)
    assert probe_media(tmp_path / "movie.mp4") is None
