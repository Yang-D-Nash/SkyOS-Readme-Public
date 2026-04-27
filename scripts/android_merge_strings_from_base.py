#!/usr/bin/env python3
"""
Merge androidApp/src/main/res/values/strings.xml (canonical key order)
into every values-*/strings.xml: keep existing per-locale text, add missing
string/plurals from base so all locales share the same keys.
"""
from __future__ import annotations

import copy
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def load_root(path: Path) -> ET.Element:
    tree = ET.parse(path)
    return tree.getroot()


def by_name_map(root: ET.Element) -> dict[str, ET.Element]:
    m: dict[str, ET.Element] = {}
    for el in list(root):
        if not isinstance(el, ET.Element):
            continue
        if el.tag in ("string", "plurals") and "name" in el.attrib:
            m[el.attrib["name"]] = el
    return m


def merge_locale(base_root: ET.Element, locale_path: Path) -> None:
    if not locale_path.is_file():
        print(f"skip missing {locale_path}", file=sys.stderr)
        return
    loc_root = load_root(locale_path)
    loc_map = by_name_map(loc_root)
    new_root = ET.Element("resources")

    for child in list(base_root):
        if not isinstance(child, ET.Element):
            continue
        if child.tag in ("string", "plurals") and "name" in child.attrib:
            n = child.attrib["name"]
            if n in loc_map:
                new_root.append(copy.deepcopy(loc_map[n]))
            else:
                new_root.append(copy.deepcopy(child))
        else:
            # e.g. comments; preserve from locale if we had a matching structure
            # base typically has no comments between strings
            pass

    for child in list(loc_root):
        if not isinstance(child, ET.Element):
            continue
        if child.tag in ("string", "plurals") and "name" in child.attrib:
            n = child.attrib["name"]
            if n not in (c.attrib.get("name") for c in new_root if "name" in c.attrib):
                # Orphaned translation (not in base) — keep at end
                new_root.append(copy.deepcopy(child))

    tree = ET.ElementTree(new_root)
    ET.indent(new_root, "    ")

    with open(locale_path, "wb") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n'.encode("utf-8"))
        f.write(ET.tostring(new_root, encoding="utf-8", xml_declaration=False))


def main() -> int:
    res = Path("androidApp/src/main/res")
    base = res / "values" / "strings.xml"
    if not base.is_file():
        print(f"Base not found: {base}", file=sys.stderr)
        return 1
    base_root = load_root(base)
    for p in sorted(res.glob("values-*/strings.xml")):
        if p == base:
            continue
        merge_locale(base_root, p)
        print(f"merged: {p}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
