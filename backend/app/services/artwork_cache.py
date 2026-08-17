from hashlib import sha256
from pathlib import Path

from PIL import Image, ImageOps


CACHE_DIR = Path(".cache") / "artwork"


def optimized_artwork(source: Path, size: tuple[int, int], kind: str) -> Path:
    """Return a cached, display-sized JPEG without modifying the source artwork."""
    stat = source.stat()
    key = sha256(
        f"{source.resolve()}:{stat.st_mtime_ns}:{stat.st_size}:{size}:{kind}:v1".encode()
    ).hexdigest()
    destination = CACHE_DIR / f"{key}.jpg"
    if destination.is_file():
        return destination

    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    with Image.open(source) as image:
        image = ImageOps.exif_transpose(image)
        image.thumbnail(size, Image.Resampling.LANCZOS)
        if image.mode != "RGB":
            background = Image.new("RGB", image.size, "#202027")
            if "A" in image.getbands():
                background.paste(image, mask=image.getchannel("A"))
            else:
                background.paste(image.convert("RGB"))
            image = background
        temporary = destination.with_suffix(".tmp")
        image.save(temporary, format="JPEG", quality=82, optimize=True, progressive=True)
        temporary.replace(destination)
    return destination
