#!/usr/bin/env python3
"""Genera los sonidos incluidos en Sheepsleep, sin dependencias externas.

Uso:  python3 tools/make_sounds.py [carpeta_salida]

Cada sonido se sintetiza a partir de ruido filtrado y se cierra en un bucle sin
cortes. Sintetizar en vez de descargar evita cualquier problema de licencia: el
resultado es original y se distribuye con la licencia del proyecto.

El truco del bucle: se genera `DURACION + FADE` segundos y el sobrante final se
mezcla con curvas de potencia constante sobre el principio. Al reproducir en
bucle, el final empalma con el inicio sin salto ni clic.

Salida: WAV mono 22 050 Hz 16 bits, ~-20 dBFS de RMS (deja margen para mezclar
varias capas sin saturar).
"""

from __future__ import annotations

import array
import math
import os
import random
import struct
import sys
import wave

SR = 22_050
DURACION = 30.0          # segundos del bucle final
FADE = 2.0               # segundos de mezcla para cerrar el bucle
RMS_OBJETIVO = 0.10      # ~-20 dBFS


# --------------------------------------------------------------------------
# Utilidades de señal
# --------------------------------------------------------------------------

def blanco(n: int, rnd: random.Random) -> list[float]:
    """Ruido blanco uniforme en [-1, 1]."""
    return [rnd.uniform(-1.0, 1.0) for _ in range(n)]


def paso_bajo(x: list[float], corte_hz: float) -> list[float]:
    """Filtro IIR de un polo. Suficiente para dar color sin rings metálicos."""
    a = math.exp(-2.0 * math.pi * corte_hz / SR)
    y = [0.0] * len(x)
    prev = 0.0
    for i, v in enumerate(x):
        prev = (1.0 - a) * v + a * prev
        y[i] = prev
    return y


def paso_alto(x: list[float], corte_hz: float) -> list[float]:
    """Resta del original su versión paso bajo: complementario exacto."""
    low = paso_bajo(x, corte_hz)
    return [v - l for v, l in zip(x, low)]


def banda(x: list[float], low_hz: float, high_hz: float) -> list[float]:
    return paso_alto(paso_bajo(x, high_hz), low_hz)


def normalizar(x: list[float], rms: float = RMS_OBJETIVO) -> list[float]:
    actual = math.sqrt(sum(v * v for v in x) / len(x)) or 1.0
    g = rms / actual
    # Limitador suave: evita que un pico aislado sature al aplicar la ganancia.
    return [math.tanh(v * g * 1.2) * 0.85 for v in x]


def cerrar_bucle(x: list[float], fade_s: float = FADE) -> list[float]:
    """
    Mezcla el final sobrante sobre el principio con potencia constante.

    Se generan DURACION+fade y aquí se recorta a DURACION, repartiendo el rabo
    sobre los primeros `fade` segundos. El resultado empalma sin discontinuidad.

    Además se iguala la última muestra con la primera con un microfundido: en
    sonidos de envolvente lenta (olas) el rabo puede caer en un extremo de la
    onda y dejar un salto de varios miles de muestras, que suena a clic.
    """
    n = int(DURACION * SR)
    f = int(fade_s * SR)
    if len(x) < n + f:
        x = x + [0.0] * (n + f - len(x))
    salida = x[:n]
    for i in range(f):
        t = i / f
        # Curva de potencia constante: la suma de cuadrados se mantiene.
        g_out = math.cos(t * math.pi / 2)
        g_in = math.sin(t * math.pi / 2)
        salida[i] = salida[i] * g_in + x[n + i] * g_out

    # Microfundido cruzado (20 ms) en el empalme: el final baja a cero mientras el
    # principio sube desde cero con la misma curva. El salto desaparece sin oír
    # ninguna pausa, porque las dos rampas se cruzan en el punto de unión.
    micro = int(0.020 * SR)
    for i in range(micro):
        g = i / micro
        salida[n - micro + i] *= 1.0 - g  # rampa de bajada al final
        salida[i] *= g                    # rampa de subida al principio
    return salida


# --------------------------------------------------------------------------
# Sonidos
# --------------------------------------------------------------------------

def lluvia(rnd: random.Random) -> list[float]:
    """
    Lluvia: siseo de banda ancha (cuerpo) más gotas sueltas (transitorios).

    El siseo solo suena a estática; las gotas son lo que el oído reconoce como
    lluvia. Se reparten al azar con más densidad en el rango medio. La primera
    versión se iba al 58% de agudos y sonaba a sartén; ahora lleva un lecho grave
    que la asienta.
    """
    n = int((DURACION + FADE) * SR)
    base = banda(blanco(n, rnd), 400, 6000)
    base = [v * 0.42 for v in base]

    # Lecho grave: la lluvia contra el cristal tiene cuerpo, no solo siseo.
    lecho = banda(blanco(n, rnd), 80, 500)
    for i in range(n):
        base[i] += lecho[i] * 0.45

    # Gotas: impulsos cortos con decaimiento exponencial rápido.
    for _ in range(int(n / SR * 90)):
        pos = rnd.randrange(0, n - 2000)
        amp = rnd.uniform(0.25, 0.9)
        frec = rnd.uniform(1200, 5200)
        largo = rnd.randint(250, 900)
        for j in range(largo):
            env = math.exp(-6.0 * j / largo)
            base[pos + j] += amp * env * 0.30 * math.sin(
                2 * math.pi * frec * j / SR + rnd.random() * 0.4
            )
    return base


def olas(rnd: random.Random) -> list[float]:
    """
    Olas: retumbo grave más la espuma del rompiente.
    
    La primera versión era 83% graves y 0,4% agudos: en el altavoz de un móvil
    (que no baja de ~300 Hz) sonaba a un soplido lejano sin nada de mar. El
    componente agudo del agua no es decorativo: es lo que se reconoce como ola.
    """
    n = int((DURACION + FADE) * SR)
    rosa = banda(blanco(n, rnd), 20, 900)

    # Retumbo: integración del ruido, da el cuerpo grave.
    marron = [0.0] * n
    acc = 0.0
    for i, v in enumerate(rosa):
        acc = acc * 0.996 + v * 0.04
        marron[i] = acc

    # Espuma: banda alta que solo aparece cuando la ola rompe.
    espuma = banda(blanco(n, rnd), 1500, 8000)

    # Envolvente de oleaje: dos senos desfasados para que no sea periódica.
    periodo1 = rnd.uniform(9.0, 12.0)
    periodo2 = rnd.uniform(13.0, 17.0)
    for i in range(n):
        t = i / SR
        env = 0.40 + 0.32 * math.sin(2 * math.pi * t / periodo1) \
                   + 0.18 * math.sin(2 * math.pi * t / periodo2 + 1.3)
        env = max(0.05, env)
        marron[i] *= env * 0.75
        # La espuma sube con la ola y desaparece en el remanso.
        foam = max(0.0, env - 0.55) ** 1.6
        marron[i] += espuma[i] * foam * 2.6
    return marron


def viento(rnd: random.Random) -> list[float]:
    """
    Viento: ruido de banda media con el filtro abriéndose y cerrándose.

    Lo que distingue al viento de un siseo fijo es que el timbre cambia: hay
    rachas que silban (agudo) y remansos sordos.
    """
    n = int((DURACION + FADE) * SR)
    crudo = blanco(n, rnd)
    salida = [0.0] * n

    # Dos filtros en paralelo con corte modulado: grave (cuerpo) y agudo (silbido).
    acc_g = 0.0
    acc_a = 0.0
    periodo = rnd.uniform(6.0, 9.0)
    for i in range(n):
        t = i / SR
        # 0..1: cuánto sopla en este instante.
        racha = 0.5 + 0.5 * math.sin(2 * math.pi * t / periodo + 0.7)
        racha = racha ** 1.7

        a_g = math.exp(-2 * math.pi * (180 + 260 * racha) / SR)
        a_a = math.exp(-2 * math.pi * (900 + 2600 * racha) / SR)
        acc_g = (1 - a_g) * crudo[i] + a_g * acc_g
        acc_a = (1 - a_a) * crudo[i] + a_a * acc_a
        salida[i] = acc_g * 1.0 + acc_a * (0.15 + 0.55 * racha)
    return salida


def fogata(rnd: random.Random) -> list[float]:
    """
    Fogata: retumbo grave más chasquidos de madera.

    La primera versión era 80% graves y 0,4% agudos: los chasquidos se perdían en
    el altavoz del móvil y solo quedaba un zumbido. Aquí los chasquidos pesan
    mucho más y se les suma un siseo de brasas en la banda media.
    """
    n = int((DURACION + FADE) * SR)
    base = banda(blanco(n, rnd), 40, 500)
    marron = [0.0] * n
    acc = 0.0
    for i, v in enumerate(base):
        acc = acc * 0.995 + v * 0.05
        marron[i] = acc * 0.85

    # Brasas: siseo suave que da presencia en medios sin tapar los chasquidos.
    brasas = banda(blanco(n, rnd), 700, 4000)
    for i in range(n):
        marron[i] += brasas[i] * 0.28

    # Chasquidos: entre 4 y 14 por segundo, con amplitud muy irregular.
    for _ in range(int(n / SR * 9)):
        pos = rnd.randrange(0, n - 4000)
        amp = rnd.uniform(0.25, 0.95) ** 1.5
        largo = rnd.randint(120, 700)
        frec = rnd.uniform(1800, 7000)
        for j in range(largo):
            env = math.exp(-9.0 * j / largo)
            marron[pos + j] += amp * env * 1.5 * (
                math.sin(2 * math.pi * frec * j / SR) * 0.65
                + rnd.uniform(-1, 1) * 0.35
            )
    return marron


def bosque(rnd: random.Random) -> list[float]:
    """
    Bosque nocturno: aire suave de fondo más grillos.

    Los grillos son pulsos agudos repetidos a ~4-5 kHz con una cadencia regular,
    que es lo que el oído identifica como noche de verano.
    """
    n = int((DURACION + FADE) * SR)
    aire = banda(blanco(n, rnd), 100, 1800)
    aire = [v * 0.35 for v in aire]

    # Tres grillos con tono y cadencia propios, en distintos momentos.
    for _ in range(3):
        frec = rnd.uniform(4200, 5400)
        cadencia = rnd.uniform(0.55, 0.95)   # segundos entre chirridos
        inicio = rnd.uniform(0, 3)
        # El grillo no está toda la pieza: entra y sale.
        tramo_ini = rnd.uniform(0, DURACION * 0.4)
        tramo_fin = tramo_ini + rnd.uniform(8.0, 18.0)
        t = inicio
        while t < DURACION + FADE - 0.3:
            if tramo_ini <= t <= tramo_fin:
                pos = int(t * SR)
                # Un chirrido: 3-5 pulsos rapidísimos.
                for p in range(rnd.randint(3, 5)):
                    ppos = pos + int(p * 0.045 * SR)
                    if ppos + 700 >= n:
                        break
                    for j in range(700):
                        env = math.exp(-30.0 * j / 700)
                        aire[ppos + j] += 0.16 * env * math.sin(
                            2 * math.pi * frec * j / SR
                        )
            t += cadencia
    return aire


def ruido_color(rnd: random.Random, tipo: str) -> list[float]:
    """
    Ruidos de color, recalculados con bucle limpio.

    Los originales venían con un salto entre la última y la primera muestra, que
    al repetir sonaba a chasquido cada 10 s.
    """
    n = int((DURACION + FADE) * SR)
    if tipo == "white":
        return banda(blanco(n, rnd), 20, 10_000)

    if tipo == "pink":
        # Aproximación de Voss-McCartney: suma de generadores a distintas escalas.
        filas = 16
        valores = [0.0] * filas
        total = 0.0
        salida = [0.0] * n
        for i in range(n):
            # Se actualiza una fila al azar en cada muestra.
            k = (i & -i).bit_length() - 1
            if k < filas:
                total -= valores[k]
                valores[k] = rnd.uniform(-1, 1)
                total += valores[k]
            salida[i] = total / math.sqrt(filas)
        # Se quita el extremo grave para que no retumbe en altavoces pequeños.
        return paso_alto(salida, 25)

    # Marrón: integración del blanco con fuga para no derivar.
    crudo = blanco(n, rnd)
    salida = [0.0] * n
    acc = 0.0
    for i, v in enumerate(crudo):
        acc = acc * 0.9975 + v * 0.05
        salida[i] = acc
    return paso_alto(salida, 20)


# --------------------------------------------------------------------------
# Escritura
# --------------------------------------------------------------------------

def escribir_wav(ruta: str, muestras: list[float]) -> None:
    datos = array.array("h")
    for v in muestras:
        s = int(max(-1.0, min(1.0, v)) * 32767)
        datos.append(s)
    with wave.open(ruta, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(datos.tobytes())


def main() -> int:
    salida = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/res/raw"
    os.makedirs(salida, exist_ok=True)

    generadores = {
        "white_noise": lambda r: ruido_color(r, "white"),
        "pink_noise": lambda r: ruido_color(r, "pink"),
        "brown_noise": lambda r: ruido_color(r, "brown"),
        "rain": lluvia,
        "waves": olas,
        "wind": viento,
        "fire": fogata,
        "forest": bosque,
    }

    for nombre, fn in generadores.items():
        # Semilla fija por sonido: el resultado es reproducible entre ejecuciones.
        rnd = random.Random(nombre)
        print(f"  {nombre}...", end=" ", flush=True)
        señal = fn(rnd)
        # Normalizar antes de cerrar el bucle: la ganancia es lineal y no altera la
        # continuidad. Aplicarla después desplazaba el punto de empalme.
        señal = normalizar(señal)
        señal = cerrar_bucle(señal)
        ruta = os.path.join(salida, f"{nombre}.wav")
        escribir_wav(ruta, señal)
        kb = os.path.getsize(ruta) // 1024
        print(f"{kb} KB")

    print(f"\nListo: {len(generadores)} sonidos en {salida}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
