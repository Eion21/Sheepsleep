#!/usr/bin/env python3
"""Corta un fragmento de un audio largo y lo convierte en un bucle sin cortes.

Uso:
  python3 make_loop.py ENTRADA SALIDA.ogg [--start 30] [--length 60] [--fade 3]

  ENTRADA   cualquier audio que ffmpeg entienda (mp3, wav, m4a, ogg...)
  --start   segundo donde empieza el fragmento           (por defecto 0)
  --length  duración final del bucle en segundos         (por defecto 60)
  --fade    segundos de mezcla entre el final y el inicio (por defecto 3)

Cómo funciona: se toman length+fade segundos y el sobrante del final se mezcla
(con curvas de potencia constante) encima del principio. Así, cuando el reproductor
vuelve al inicio, el sonido continúa sin salto ni clic.
Requiere ffmpeg y numpy. Salida recomendada: .ogg (Vorbis, ~96 kbps).
"""
import argparse, subprocess, sys
import numpy as np

SR, CH = 44100, 2

def decode(path, start, seconds):
    cmd = ["ffmpeg", "-v", "error", "-ss", str(start), "-t", str(seconds), "-i", path,
           "-f", "f32le", "-ac", str(CH), "-ar", str(SR), "-"]
    raw = subprocess.run(cmd, capture_output=True, check=True).stdout
    return np.frombuffer(raw, dtype="<f4").reshape(-1, CH)

def encode(data, out):
    cmd = ["ffmpeg", "-v", "error", "-y", "-f", "f32le", "-ar", str(SR), "-ac", str(CH), "-i", "-"]
    if out.lower().endswith(".ogg"):
        cmd += ["-c:a", "libvorbis", "-b:a", "96k"]
    cmd.append(out)
    subprocess.run(cmd, input=data.astype("<f4").tobytes(), check=True)

def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("input"); p.add_argument("output")
    p.add_argument("--start", type=float, default=0)
    p.add_argument("--length", type=float, default=60)
    p.add_argument("--fade", type=float, default=3)
    a = p.parse_args()
    if a.fade * 2 >= a.length:
        sys.exit("--fade debe ser menor que la mitad de --length")

    n, f = int(a.length * SR), int(a.fade * SR)
    seg = decode(a.input, a.start, a.length + a.fade)
    if len(seg) < n + f:
        sys.exit(f"El audio no tiene suficiente duración desde el segundo {a.start}: "
                 f"necesito {a.length + a.fade:.1f} s y hay {len(seg) / SR:.1f} s")

    t = np.linspace(0, np.pi / 2, f, dtype=np.float32)[:, None]
    loop = seg[:n].copy()
    loop[:f] = seg[:f] * np.sin(t) + seg[n:n + f] * np.cos(t)   # entra el inicio, sale la cola
    peak = float(np.max(np.abs(loop)))
    if peak > 0.98:                                              # evita saturar al mezclar
        loop *= 0.98 / peak
    encode(loop, a.output)
    print(f"Bucle de {a.length:.0f} s -> {a.output}")

if __name__ == "__main__":
    main()
