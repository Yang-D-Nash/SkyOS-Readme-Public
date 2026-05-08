#!/usr/bin/env python3
"""Audit store screenshot sets for release-blocking asset issues."""

from __future__ import annotations

import hashlib
import sys
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageStat


ROOT = Path(__file__).resolve().parents[1]


@dataclass(frozen=True)
class ScreenshotSet:
    name: str
    path: Path
    expected_count: int
    expected_size: tuple[int, int]
    play_ratio_limit: bool = False


SETS = (
    ScreenshotSet("iOS final", ROOT / "screenshots/final/ios", 6, (1320, 2868)),
    ScreenshotSet("iPad final", ROOT / "screenshots/final/ipad", 7, (2064, 2752)),
    ScreenshotSet("Android raw", ROOT / "store-assets/android/raw", 7, (1080, 2424)),
    ScreenshotSet("Android final raw", ROOT / "screenshots/final/android", 6, (1080, 2424)),
    ScreenshotSet("Google Play phone", ROOT / "screenshots/final/google-play/android-phone", 7, (1242, 2424), True),
    ScreenshotSet("Fold raw", ROOT / "store-assets/fold/raw", 7, (1812, 2176)),
)

LISTING_GRAPHICS = (
    (ROOT / "docs/assets/google-play/skyos-play-icon-512.png", (512, 512)),
    (ROOT / "docs/assets/google-play/skyos-feature-graphic-1024x500.png", (1024, 500)),
)


def pixel_hash(image: Image.Image) -> str:
    return hashlib.sha256(image.convert("RGB").tobytes()).hexdigest()


def has_transparency(image: Image.Image) -> bool:
    if image.mode not in ("RGBA", "LA"):
        return False
    alpha = image.getchannel("A")
    return alpha.getextrema()[0] < 255


def low_variance_score(image: Image.Image) -> int:
    sample = image.convert("RGB").resize((24, 24))
    data = sample.tobytes()
    return len({data[index : index + 3] for index in range(0, len(data), 3)})


def has_debug_blue_tile(image: Image.Image) -> bool:
    rgb = image.convert("RGB")
    columns = 8
    rows = 8
    tile_width = rgb.width // columns
    tile_height = rgb.height // rows

    for row in range(rows):
        for column in range(columns):
            tile = rgb.crop(
                (
                    column * tile_width,
                    row * tile_height,
                    (column + 1) * tile_width,
                    (row + 1) * tile_height,
                )
            )
            stat = ImageStat.Stat(tile.resize((12, 12)))
            red, green, blue = stat.mean
            if blue > 150 and blue > red * 1.8 and blue > green * 1.35 and low_variance_score(tile) < 80:
                return True

    return False


def audit_image(path: Path, expected_size: tuple[int, int], play_ratio_limit: bool) -> list[str]:
    problems: list[str] = []
    image = Image.open(path)
    width, height = image.size

    if image.size != expected_size:
        problems.append(f"expected {expected_size[0]}x{expected_size[1]}, got {width}x{height}")

    if has_transparency(image):
        problems.append("contains transparent pixels")

    if play_ratio_limit and max(width, height) / min(width, height) > 2:
        problems.append("violates Google Play 2:1 side-ratio limit")

    variance = low_variance_score(image)
    if variance < 120:
        problems.append(f"low visual variance ({variance}); likely placeholder or broken capture")

    stat = ImageStat.Stat(image.convert("RGB"))
    mean = tuple(round(value, 1) for value in stat.mean)
    if mean[2] > mean[0] * 1.8 and mean[2] > mean[1] * 1.45 and variance < 180:
        problems.append(f"blue-dominant low-detail image (mean RGB {mean}); check for debug placeholder")

    if has_debug_blue_tile(image):
        problems.append("contains a large blue low-detail tile; likely debug placeholder")

    return problems


def main() -> int:
    failures: list[str] = []

    for screenshot_set in SETS:
        files = sorted(screenshot_set.path.glob("*.png"))
        if len(files) != screenshot_set.expected_count:
            failures.append(
                f"{screenshot_set.name}: expected {screenshot_set.expected_count} PNGs, found {len(files)}"
            )

        seen_pixels: dict[str, Path] = {}
        for file_path in files:
            image = Image.open(file_path)
            for problem in audit_image(file_path, screenshot_set.expected_size, screenshot_set.play_ratio_limit):
                failures.append(f"{screenshot_set.name}/{file_path.name}: {problem}")

            digest = pixel_hash(image)
            if digest in seen_pixels:
                failures.append(
                    f"{screenshot_set.name}/{file_path.name}: pixel duplicate of {seen_pixels[digest].name}"
                )
            else:
                seen_pixels[digest] = file_path

    for file_path, expected_size in LISTING_GRAPHICS:
        if not file_path.exists():
            failures.append(f"{file_path.relative_to(ROOT)}: missing listing graphic")
            continue
        for problem in audit_image(file_path, expected_size, play_ratio_limit=False):
            failures.append(f"{file_path.relative_to(ROOT)}: {problem}")

    if failures:
        print("Store screenshot audit failed:")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print("Store screenshot audit passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
