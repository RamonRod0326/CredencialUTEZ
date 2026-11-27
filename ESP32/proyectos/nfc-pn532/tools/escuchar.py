"""Escucha el monitor serie de la placa y lo guarda en un archivo.

Sirve para dejar la placa hablando mientras haces pruebas con las manos, y
leer después qué pasó.

Por defecto NO reinicia la placa: un reinicio a mitad de una conversación
puede dejar al PN532 colgado (prueba 03). En su lugar le manda 'i' para que
diga su estado. Con --reiniciar sí la reinicia, para ver el arranque.

Uso (desde proyectos/nfc-pn532/):
    2-entorno/.venv/bin/python tools/escuchar.py logs/archivo.log [minutos] [--reiniciar]
"""
import sys
import time

import serial

PUERTO = "/dev/cu.usbserial-0001"
args = [a for a in sys.argv[1:] if not a.startswith("--")]
reiniciar = "--reiniciar" in sys.argv
destino = args[0]
minutos = float(args[1]) if len(args) > 1 else 10

p = serial.Serial()
p.port, p.baudrate, p.timeout = PUERTO, 115200, 0.2
p.dtr = p.rts = False
p.open()
if reiniciar:
    p.rts = True      # pulso de reset: RTS está unido al pin EN
    time.sleep(0.15)
    p.rts = False
else:
    p.write(b"i")     # "¿cómo estás?" sin reiniciar

fin = time.time() + minutos * 60
with open(destino, "w", encoding="utf-8") as f:
    while time.time() < fin:
        datos = p.read(4096)
        if datos:
            f.write(datos.decode("utf-8", "replace"))
            f.flush()
p.close()
