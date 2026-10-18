#!/usr/bin/env python3
"""Genera catalog.json a partir de una carpeta de audios y un meta.json.

Uso:  python3 make_catalog.py CARPETA_DE_AUDIOS [salida.json]

CARPETA/rain_light.ogg           <- el nombre sin extensión es el id (a-z, 0-9, _)
CARPETA/meta.json                <- {"rain_light": {"title": "...", "category": "...",
                                     "author": "...", "license": "CC0-1.0", "sourceUrl": "..."}}
"""
import hashlib, json, re, shutil, subprocess, sys
from pathlib import Path

EXT = {".ogg", ".opus", ".mp3", ".m4a", ".wav", ".flac"}
ID_RE = re.compile(r"^[a-z0-9_]{1,64}$")

def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()

def duration(path: Path):
    if not shutil.which("ffprobe"):
        return None
    out = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(path)],
        capture_output=True, text=True)
    try:
        return round(float(out.stdout.strip()))
    except ValueError:
        return None

def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__); return 1
    folder = Path(sys.argv[1])
    out_path = Path(sys.argv[2]) if len(sys.argv) > 2 else folder / "catalog.json"
    meta = json.loads((folder / "meta.json").read_text(encoding="utf-8"))
    sounds, errors = [], []
    for f in sorted(folder.iterdir()):
        if f.suffix.lower() not in EXT:
            continue
        sid = f.stem
        if not ID_RE.match(sid):
            errors.append(f"{f.name}: el nombre debe ser minúsculas, números y _"); continue
        if f.suffix != f.suffix.lower():
            errors.append(f"{f.name}: usa la extensión en minúsculas"); continue
        m = meta.get(sid)
        if not m or not m.get("license") or not m.get("author"):
            errors.append(f"{f.name}: falta autor o licencia en meta.json"); continue
        entry = {
            "id": sid, "title": m.get("title", sid), "category": m.get("category", ""),
            "file": f.name, "sizeBytes": f.stat().st_size, "sha256": sha256(f),
            "author": m["author"], "license": m["license"], "sourceUrl": m.get("sourceUrl", ""),
        }
        d = duration(f)
        if d is not None:
            entry["durationSec"] = d
        sounds.append(entry)
    for e in errors:
        print("ERROR:", e, file=sys.stderr)
    out_path.write_text(json.dumps({"version": 1, "sounds": sounds}, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"{len(sounds)} sonido(s) -> {out_path}")
    return 1 if errors else 0

if __name__ == "__main__":
    sys.exit(main())
