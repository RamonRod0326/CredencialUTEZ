# Entorno de desarrollo — nfc-pn532

Mismo esquema que el Tamagotchi (PlatformIO + framework Arduino), con su
**propia** caja de herramientas: nada se comparte entre proyectos. La explicación
completa de por qué está en
[`tamagotchi/2-entorno/README.md`](../../tamagotchi/2-entorno/README.md).

## Versiones clavadas

| Herramienta | Versión | Para qué |
|---|---|---|
| PlatformIO | `6.1.19` | Compila el firmware y lo sube a la placa |
| esptool | `5.3.1` | Habla directo con el chip: identificarlo, leer su MAC, borrarlo |

Están en [`requirements.txt`](requirements.txt). Se eligieron **las mismas que
el Tamagotchi** en vez de "las últimas": ya están probadas en esta Mac.

## Estado

| Paso | Estado |
|---|---|
| Entorno virtual (`.venv/`) | ✅ 2026-11-25 · Python 3.13.2 |
| PlatformIO + esptool instalados y clavados | ✅ |
| Placa identificada con `esptool` | ✅ C-011 · MAC `E0:8C:FE:30:C9:1C` |
| Flash borrada por completo | ✅ 2026-11-25 |
| `firmware/platformio.ini` | ✅ `espressif32@7.0.1` (Arduino 2.0.17), Adafruit BusIO `1.17.4` |
| Adafruit PN532 `1.3.4` | ✅ Copia local con dos parches en `firmware/lib/` (ver su `LEEME.md`) |
| Protección de placa | ✅ `firmware/proteger-placa.py`: lee la MAC y cancela la carga si no es la C-011 |

## Cómo usarlo

Desde `proyectos/nfc-pn532/`:

```bash
source 2-entorno/.venv/bin/activate      # una vez por terminal; aparece (.venv)
esptool --port /dev/cu.usbserial-0001 read-mac   # ¿qué placa está enchufada?
```

> ⚠️ **Antes de cargar nada, comprueba la MAC.** Esta placa y la del Tamagotchi
> aparecen las dos como `/dev/cu.usbserial-0001`. Si la MAC es
> `84:1F:E8:2C:3E:C4`, tienes enchufado **el regalo**: no le cargues nada de
> este proyecto.
