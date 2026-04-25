#!/usr/bin/env python3
"""Scale app-icon artwork inside a 1024 canvas (Android adaptive foreground only).

iOS AppIcon uses the *unpadded* 1024 master; Android applies this script to the same
design to avoid the circular mask clipping the round mark, then that PNG is
`ic_launcher_foreground_src` with a small dimen inset.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("source", type=Path, help="1024x1024 (or square) source PNG")
    p.add_argument("out", type=Path, help="output 1024x1024 PNG")
    p.add_argument(
        "--scale",
        type=float,
        default=0.86,
        help="content scale relative to 1024 side (0..1); default 0.86 leaves ~7%% margin per side",
    )
    args = p.parse_args()
    if not 0.3 <= args.scale <= 1.0:
        print("scale must be between 0.3 and 1.0", file=sys.stderr)
        return 2
    with Image.open(args.source) as im:
        im = im.convert("RGBA")
        w, h = im.size
        if w != h:
            print("source should be square", file=sys.stderr)
        side = 1024
        new_side = int(round(min(w, h) * args.scale))
        resized = im.resize((new_side, new_side), Image.Resampling.LANCZOS)
        canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
        x = (side - new_side) // 2
        y = (side - new_side) // 2
        canvas.paste(resized, (x, y), resized)
        canvas.save(args.out, "PNG", optimize=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
