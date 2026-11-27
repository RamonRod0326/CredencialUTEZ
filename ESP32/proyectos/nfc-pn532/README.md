# nfc-pn532

**Lector de credenciales universitarias por NFC.** El estudiante acerca su
celular con la app; el lector lee su matrícula, comprueba la firma y dice si
pasa. Hecho con un ESP32 y un lector **PN532**.

> **NFC** (*Near Field Communication*, comunicación de campo cercano) es la
> tecnología del pago con el celular: el lector crea un campo magnético débil,
> la tarjeta se alimenta de él (no lleva pila) y le contesta con sus datos.

- **Estado:** 🟢 **Funciona con la app real** (2026-11-25): lee, valida y responde
- **Inicio:** 2026-11-25

## Material

Del [inventario general](../../inventario/INVENTARIO.md):

| ID | Pieza | Papel |
|---|---|---|
| [C-011](3-componentes/esp32-c011.md) | ESP32-S NodeMCU v1.1 · MAC `E0:8C:FE:30:C9:1C` | El cerebro |
| [C-010](3-componentes/pn532-nfc-v3.md) | Lector NFC PN532 Kit V3 (Fydun) | El lector |
| C-012 ×2 · C-013 | 2 tarjetas y 1 llavero NFC (del kit) | Lo que se lee |

Además se probará con **el celular** (no entra al inventario: no es material
del taller).

> ⚠️ **Esta placa y la del Tamagotchi aparecen con el mismo puerto**
> (`/dev/cu.usbserial-0001`). Antes de cargar firmware, comprueba la MAC:
> si sale `84:1F:E8:2C:3E:C4`, tienes enchufado **el regalo**.

## Organización

Las carpetas siguen las [6 fases del taller](../../metodologia/README.md):

| Carpeta | Qué contiene | Estado |
|---|---|---|
| [`1-decisiones/`](1-decisiones/) | ADRs: por qué hicimos lo que hicimos | 🟢 4 ADRs (I2C → SPI, librería, app) |
| [`2-entorno/`](2-entorno/) | El entorno aislado y cómo levantarlo | 🟢 Versiones clavadas |
| [`3-componentes/`](3-componentes/) | Ficha de cada pieza | 🟢 Las dos fichas |
| [`4-requerimientos/`](4-requerimientos/) | Qué tiene que hacer | 🟢 v0.3: 7 de 9 verificados |
| [`5-investigacion/`](5-investigacion/) | Lo averiguado, con fuentes | 🟡 Preguntas abiertas |
| [`6-esquemas/`](6-esquemas/) | El mapa: lógico y físico | 🟢 Físico (SPI) verificado |
| [`pruebas/`](pruebas/) | Pruebas desechables, una pregunta cada una | 🟢 01–07 |
| [`firmware/`](firmware/) | El lector de credenciales | 🟢 Probado con la app |

## Cómo se usa

```bash
cd proyectos/nfc-pn532
source 2-entorno/.venv/bin/activate
cp firmware/secrets.h.ejemplo firmware/src/secrets.h   # una vez: pon la clave real
pio run -d firmware -t upload        # comprueba la MAC antes de cargar
pio device monitor -d firmware       # 'i' + Enter = estado sin reiniciar
```

⚠️ **La clave nunca va al repositorio.** Vive en `firmware/src/secrets.h`, que
está en `.gitignore`.
