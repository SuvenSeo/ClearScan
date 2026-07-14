#!/usr/bin/env python3
"""Render synthetic scan-like PNG fixtures for OCR corpus entries and wire imageFile."""

from __future__ import annotations

import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageEnhance

CORPUS_DIR = Path(__file__).resolve().parents[2] / "app/src/test/resources/ocr-corpus"

SINHALA_FONT = Path("/usr/share/fonts/truetype/noto/NotoSansSinhala-Regular.ttf")
TAMIL_FONT = Path("/usr/share/fonts/truetype/noto/NotoSansTamil-Regular.ttf")
FALLBACK_FONT = Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")

PAGE_WIDTH = 1240
PAGE_HEIGHT = 1754
MARGIN = 72
LINE_SPACING = 1.45


def font_for(language: str, size: int = 42) -> ImageFont.FreeTypeFont:
    path = SINHALA_FONT if language == "sinhala" else TAMIL_FONT
    if not path.exists():
        path = FALLBACK_FONT
    return ImageFont.truetype(str(path), size=size)


def wrap_lines(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.ImageFont, max_width: int) -> list[str]:
    lines: list[str] = []
    for paragraph in text.replace("\t", "  ").splitlines() or [""]:
        if not paragraph.strip():
            lines.append("")
            continue
        words = paragraph.split(" ")
        current = ""
        for word in words:
            candidate = word if not current else f"{current} {word}"
            bbox = draw.textbbox((0, 0), candidate, font=font)
            if bbox[2] - bbox[0] <= max_width:
                current = candidate
            else:
                if current:
                    lines.append(current)
                current = word
        if current:
            lines.append(current)
    return lines


def render_scan(text: str, language: str, out_path: Path) -> None:
    image = Image.new("RGB", (PAGE_WIDTH, PAGE_HEIGHT), color=(245, 242, 235))
    draw = ImageDraw.Draw(image)
    # Paper edge + slight shadow vignette feel
    draw.rectangle(
        (36, 36, PAGE_WIDTH - 36, PAGE_HEIGHT - 36),
        outline=(210, 205, 195),
        width=2,
    )
    font = font_for(language, size=40 if language == "sinhala" else 38)
    max_width = PAGE_WIDTH - (MARGIN * 2)
    lines = wrap_lines(draw, text, font, max_width)
    y = MARGIN
    sample = draw.textbbox((0, 0), "Ag", font=font)
    line_height = int((sample[3] - sample[1]) * LINE_SPACING)
    for line in lines:
        if y + line_height > PAGE_HEIGHT - MARGIN:
            break
        draw.text((MARGIN, y), line, font=font, fill=(28, 28, 32))
        y += line_height

    # Mild paper texture / scan noise
    noise = ImageEnhance.Contrast(image).enhance(1.05)
    noise = noise.filter(ImageFilter.SMOOTH)
    noise.save(out_path, format="PNG", optimize=True)


def main() -> None:
    index_path = CORPUS_DIR / "index.json"
    index = json.loads(index_path.read_text(encoding="utf-8"))
    entries = index.get("entries", [])
    rendered = 0
    for entry_name in entries:
        entry_path = CORPUS_DIR / entry_name
        data = json.loads(entry_path.read_text(encoding="utf-8"))
        image_name = f"{data['id']}.png"
        image_path = CORPUS_DIR / image_name
        render_scan(data["expectedText"], data["language"], image_path)
        data["imageFile"] = image_name
        data["description"] = data.get("description", "")
        if "synthetic scan fixture" not in data["description"].lower():
            data["description"] = (
                data["description"].rstrip(".")
                + ". Synthetic rendered scan fixture (Unicode text → PNG)."
            )
        entry_path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        rendered += 1
        print(f"rendered {image_name}")
    print(f"done: {rendered} PNG fixtures")


if __name__ == "__main__":
    main()
