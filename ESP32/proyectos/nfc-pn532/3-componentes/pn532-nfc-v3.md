# Componente — Módulo NFC PN532 Kit V3 (Fydun)

- **ID en inventario:** C-010
- **Categoría:** Comunicación
- **Fecha de la ficha:** 2026-11-25
- **Datasheet:** chip NXP PN532 — [hoja de datos](https://www.nxp.com/docs/en/nxp/data-sheets/PN532_C1.pdf)
  y [manual de usuario UM0701-02](https://www.nxp.com/docs/en/user-guide/141520.pdf).
  *Enlaces de referencia, no consultados en esta sesión.* Del módulo (la placa
  roja) **no hay hoja de datos oficial**: es un clon del diseño "V3" de Elechouse.

---

## Qué hace, en una frase

Es un **lector de tarjetas sin contacto**: crea un campo magnético débil a
13,56 MHz, la tarjeta o el llavero se alimenta de ese campo (no llevan pila) y
le contesta con sus datos. Solo funciona a pocos centímetros.

> **NFC** (*Near Field Communication*, comunicación de campo cercano) es la misma
> tecnología del pago con el celular y de muchas tarjetas de transporte. Lo
> "cercano" es literal: el campo se debilita muy rápido con la distancia, y por
> eso hay que acercar la tarjeta casi hasta tocar.

## Alimentación

| Concepto | Valor | Nota |
|---|---|---|
| Alimentación | 3,3 V o 5 V | **Aquí: 3,3 V** desde el pin `3V3` del ESP32 |
| **Voltaje lógico** | **3,3 V** alimentado a 3,3 V | ⚠️ A 5 V no está garantizado; por eso no se usa |
| Consumo típico | ~100–150 mA con la antena activa | Dato de referencia, **sin medir** |
| Consumo pico | Sin medir | El regulador de la placa ESP32 (1 A) va sobrado |

**¿Necesita conversor de nivel lógico con el ESP32?** **No**, siempre que se
alimente a 3,3 V. Así todo el módulo habla a 3,3 V, igual que el ESP32.

## Cómo se comunica

- **Protocolo:** **SPI** ([ADR-0003](../1-decisiones/ADR-0003-spi-para-el-pn532.md)),
  modo 0, LSB primero, 1 MHz. Antes I2C ([ADR-0001](../1-decisiones/ADR-0001-i2c-para-el-pn532.md),
  reemplazado: se colgaba). También sabe HSU (UART), según el interruptor DIP.
- **Dirección I2C:** **`0x24`** ✅ medida el 2026-11-25.
  (Algunos documentos dicen `0x48`: es el mismo número escrito de otra forma,
  con un bit extra al final. Las librerías de Arduino usan `0x24`.)
- **Qué lee:** tarjetas ISO 14443A/B (MIFARE Classic, MIFARE Ultralight, NTAG),
  FeliCa, y puede hablar con celulares en modo NFC.

### El interruptor DIP

| Modo | 1 | 2 |
|---|---|---|
| HSU (UART) | OFF | OFF |
| I2C | ON | OFF |
| **SPI** ← actual | **OFF** | **ON** |

## Conexiones

Detalle, diagrama y veredicto en
[`6-esquemas/conexiones-fisicas.md`](../6-esquemas/conexiones-fisicas.md).

| Pin del PN532 | Va a (ESP32) | Función |
|---|---|---|
| `GND` (conector de 4) | `GND` | Tierra común |
| `VCC` (conector de 4) | `3V3` | Alimentación |
| `SCK` | `P18` | Reloj SPI (lo marca el ESP32) |
| `MISO` | `P19` | Datos PN532 → ESP32 |
| `MOSI` | `P23` | Datos ESP32 → PN532 |
| `SS` | `P5` | Selección |
| `SCL`, `SDA`, `IRQ`, `RSTO` | — | Sin conectar |

**¿Requiere componentes extra?** No. El módulo trae resistencias hacia arriba
en sus líneas (comprobado en la prueba 05: los 4 pines SPI "jalados ARRIBA").

## Precauciones

- ⚠️ **Alimentarlo a 3,3 V.** A 5 V las líneas de datos podrían quedar a 5 V y
  los pines del ESP32 no lo toleran.
- ⚠️ **Mover el DIP solo sin corriente.** El chip lo lee al encender; cambiarlo
  en marcha no hace nada y confunde.
- ⚠️ **Metal cerca de la antena** (la franja blanca del borde) apaga la lectura.
  No es avería: no lo pongas sobre una superficie metálica.

## Cómo probarlo aislado

1. ✅ **¿Contesta en el bus?** — Hecho el 2026-11-25: contesta en `0x24`.
2. **¿Está vivo por dentro?** — Pedirle su versión de firmware y que la imprima.
3. **¿Lee?** — Acercar una tarjeta y ver su UID (su número de serie) en el
   monitor serie.

**Cómo se ve el éxito:** el paso 2 imprime algo como `PN532 v1.6`, y en el paso
3 la misma tarjeta da **siempre el mismo UID**.

## Librería a usar

- **Adafruit PN532 1.3.4** ([ADR-0002](../1-decisiones/ADR-0002-libreria-adafruit-pn532.md)),
  como **copia local con dos parches** de depuración en `firmware/lib/`
  ([ADR-0004](../1-decisiones/ADR-0004-compatibilidad-con-la-app.md)).

## Notas y sorpresas

- **2026-11-25** — La placa venía con un diagnóstico I2C ya cargado que
  confirmó el `0x24` antes del borrado.
- **2026-11-25** — ⚠️ **El DIP es diminuto y fácil de pasar por alto.** Está
  junto al conector de 4 pines (`SCL SDA VCC GND`), con `ON KE` impreso arriba y
  `1 2` abajo: palanca hacia `ON` = 1. La tabla de al lado se lee con la placa
  girada. Al pasar a SPI no se movió porque no se había ubicado, y el PN532 se
  quedó mudo (pruebas 05 y 06: todo `FF`).
- **2026-11-25** — **Por I2C se colgaba** reteniendo SCL en BAJO tras contestar
  su versión. Por SPI, la misma secuencia funciona a la primera. Ver pruebas 03–06.
- **2026-11-25** — Con celulares (HCE) contesta APDUs sin problema: lectura
  completa de la app de credenciales en ~1 s.
