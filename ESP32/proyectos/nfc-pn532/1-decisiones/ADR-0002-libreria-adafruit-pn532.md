# ADR-0002 — Usar la librería Adafruit PN532 `1.3.4`

- **Estado:** `Aceptado`
- **Fecha:** 2026-11-25
- **Reemplaza a:** —
- **Relacionado con:** [ADR-0001](ADR-0001-i2c-para-el-pn532.md) · RF-01 a RF-05

---

## Contexto

Para hablar con el PN532 hay que mandarle mensajes con un formato muy concreto
(cabecera, longitud, suma de comprobación, confirmación...). Una **librería**
es código ya escrito que se encarga de eso, para que el programa pueda decir
simplemente "¿hay una tarjeta?".

Condiciones: tiene que funcionar por **I2C** en un **ESP32** y **sin los pines
`IRQ` ni `RSTO`**, que se decidió no conectar en el ADR-0001.

## Qué sabíamos y qué no

**Lo que sabíamos con certeza** (verificado leyendo el código de la versión
1.3.4 el 2026-11-25):
- Por I2C pregunta "¿ya tienes respuesta?" leyendo un byte de estado por el
  propio bus (`isready()`): **no necesita `IRQ`**.
- Sin `RSTO` se salta el reinicio por hardware sin fallar (`reset()` comprueba
  `_reset != -1`).
- El constructor I2C llama a `pinMode()` sobre `IRQ` y `RSTO` aunque se pase
  `-1`. En el ESP32 eso solo genera un aviso interno, no toca ningún pin.
- **Trampa:** por defecto el PN532 busca etiquetas **para siempre**
  (reintentos = `0xFF`). Sin cambiarlo, el programa se congela hasta que
  alguien acerca algo. Se arregla con `setPassiveActivationRetries()`.
- **Hueco:** recibe el tipo de etiqueta (bytes ATQA y SAK) pero no lo entrega.
  Se queda en un búfer interno, `pn532_packetbuffer`, que es una variable
  global y se puede leer desde fuera.
- En el registro de PlatformIO es la **única** opción mantenida, para Arduino,
  que habla I2C (última versión: 2025-02-18).

**Lo que asumimos sin verificar:**
- Que funcione estable en este ESP32 con lecturas seguidas (S-03).

**Lo que quedó pendiente de averiguar:**
- Cómo se comporta con el celular (RF-05).

## Opciones consideradas

### Opción A — Adafruit PN532 `1.3.4` (+ Adafruit BusIO `1.17.4`)

- **A favor:** mantenida, muy usada, con ejemplos para MIFARE Classic y NTAG.
  Funciona sin `IRQ` ni `RSTO` (verificado en su código).
- **En contra:** no entrega el tipo de etiqueta; hay que leerlo de su búfer
  interno. Por defecto busca para siempre.
- **Costo:** bajo.

### Opción B — Escribir los mensajes al PN532 a mano

- **A favor:** se aprende el protocolo a fondo; control total.
- **En contra:** mucho más código y muchas más formas de equivocarse, antes de
  leer la primera tarjeta.
- **Costo:** alto.

### Opción C — Otras del registro

- `ESP PN532 UART`: solo habla por UART, no I2C. **Descartada.**
- `Bonezegei_PN532`: publicada el 2026-11-03, sin historial que la respalde.
  **Descartada por ahora.**
- La de Seeed Studio solo está en el registro para mbed, no para Arduino.

## Decisión

**Elegimos Adafruit PN532 `1.3.4`**, con Adafruit BusIO `1.17.4`, las dos
clavadas en `firmware/platformio.ini`.

Pesó más que nada que se pudo **comprobar en su código** que funciona sin
`IRQ` ni `RSTO`, en vez de suponerlo.

## Consecuencias

**Lo que esto nos habilita:**
- Leer UID, MIFARE Classic y Tipo 2 (Ultralight/NTAG) con funciones ya hechas.

**Lo que esto nos cierra o nos cuesta:**
- El programa lee `pn532_packetbuffer` directamente para saber el tipo. Eso
  **ata el código a la versión 1.3.4**: si la librería cambia ese búfer, el tipo
  saldría mal sin ningún error que avise.

**Lo que hay que vigilar:**
- ⚠️ **Si algún día se actualiza la librería**, comprobar que una tarjeta
  conocida sigue saliendo con el mismo tipo (SAK) que hoy.
- Lecturas que fallan de vez en cuando, o un PN532 que deja de contestar → ver
  "Lo que hay que vigilar" del ADR-0001.

## Cómo revertir

Medio. Cambiar de librería implica reescribir las funciones que buscan y leen
etiquetas (unas 100 líneas de `firmware/src/main.cpp`). Los cables no cambian.
