#!/usr/bin/env python3

import json
import random
import textwrap
from pathlib import Path
from datetime import date
from PIL import Image, ImageDraw, ImageFont

# Card dimensions; Galaxy widget size (4x2 cells ≈ 392x180dp, rendered at 2x)
CARD_W = 784
CARD_H = 360
CORNER_RADIUS = 40
PADDING = 52

# Colors
BG_COLOR = (18, 18, 20)
SURFACE_COLOR = (28, 28, 32)
ACCENT_COLOR = (196, 160, 100)
TEXT_PRIMARY = (235, 230, 220)
TEXT_SECONDARY = (140, 135, 128)
TEXT_IPA = (160, 145, 115)

FONT_PATHS = {
    "serif_bold": [
        "/System/Library/Fonts/Supplemental/Georgia Bold.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSerif-Bold.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf",
    ],
    "serif": [
        "/System/Library/Fonts/Supplemental/Georgia.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSerif-Regular.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf",
    ],
    "ipa": [
        "/Library/Fonts/Arial Unicode.ttf",
        "/Library/Fonts/Arial Unicode MS.ttf",
        "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/opentype/noto/NotoSans-Regular.ttf",
        "/System/Library/Fonts/Supplemental/Georgia.ttf",
    ],
    "mono": [
        "/System/Library/Fonts/Supplemental/Courier New.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationMono-Regular.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
    ],
}


def load_font(key: str, size: int) -> ImageFont.FreeTypeFont:
    for path in FONT_PATHS[key]:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def round_rect(draw: ImageDraw.ImageDraw, xy, radius: int, **kwargs):
    x0, y0, x1, y1 = xy
    draw.rounded_rectangle([x0, y0, x1, y1], radius=radius, **kwargs)


def draw_card(data: dict, out_path: Path):
    img = Image.new("RGBA", (CARD_W, CARD_H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    round_rect(draw, [0, 0, CARD_W, CARD_H], CORNER_RADIUS, fill=(*SURFACE_COLOR, 190))
    draw.rounded_rectangle([0, PADDING, 6, CARD_H - PADDING], radius=3, fill=ACCENT_COLOR)

    x = PADDING + 20
    y = PADDING

    font_pos = load_font("mono", 26)
    pos_text = data["pos"].lower() if data["pos"] else "noun"
    draw.text((x, y), pos_text, font=font_pos, fill=TEXT_SECONDARY)
    y += 36

    font_word = load_font("serif_bold", 80)
    word_display = data["word"].capitalize()
    draw.text((x, y), word_display, font=font_word, fill=TEXT_PRIMARY)
    word_bbox = draw.textbbox((x, y), word_display, font=font_word)
    y += (word_bbox[3] - word_bbox[1]) + 6

    if data["ipa"]:
        font_ipa = load_font("ipa", 32)
        draw.text((x, y), data["ipa"], font=font_ipa, fill=TEXT_IPA)
        y += 44

    y += 12
    draw.line([(x, y), (CARD_W - PADDING, y)], fill=(50, 48, 46), width=1)
    y += 20

    available_h = (CARD_H - PADDING) - y
    text_w = CARD_W - x - PADDING

    for def_size in (34, 30, 26, 22):
        font_def = load_font("serif", def_size)
        line_h = def_size + 8
        max_chars = text_w // (def_size * 0.52)
        wrapped = textwrap.fill(data["definition"], width=int(max_chars))
        lines = wrapped.count("\n") + 1
        if lines * line_h <= available_h:
            break

    draw.multiline_text((x, y), wrapped, font=font_def, fill=TEXT_PRIMARY, spacing=8)

    img.save(out_path, "PNG")
    print(f"  Saved → {out_path}")


def generate_daily(words_path: Path, out_dir: Path, count: int = 5):
    entries = json.loads(words_path.read_text())
    today = date.today().isoformat()

    # Deterministic seed on date, re-runs on the same day give the same cards
    rng = random.Random(today)
    selected = rng.sample(entries, min(count, len(entries)))

    out_dir.mkdir(parents=True, exist_ok=True)

    manifest = {"date": today, "cards": []}

    for i, entry in enumerate(selected, 1):
        word = entry["word"]
        print(f"[{i}/{len(selected)}] {word}")
        data = {
            "word": word,
            "ipa": entry.get("ipa", ""),
            "pos": entry.get("pos", "noun"),
            "definition": entry.get("definition", ""),
        }
        # Filename ist kept random to avoid any accidental deletion of unwanted images
        filename = f"vbm_3dca06b7_{today}_{i:02d}_{word}.png"
        draw_card(data, out_dir / filename)
        manifest["cards"].append({"word": word, "file": filename, **data})

    manifest_path = out_dir / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False))
    print(f"\nManifest → {manifest_path}")
    return manifest


if __name__ == "__main__":
    root = Path(__file__).parent.parent
    generate_daily(
        words_path=root / "words.json",
        out_dir=root / "output",
        count=5,
    )
