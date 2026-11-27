# ADR-0003 — Hablar con el PN532 por SPI

- **Estado:** `Aceptado`
- **Fecha:** 2026-11-25
- **Reemplaza a:** [ADR-0001](ADR-0001-i2c-para-el-pn532.md) (I2C)
- **Relacionado con:** [ADR-0002](ADR-0002-libreria-adafruit-pn532.md) · pruebas
  [03](../pruebas/03-conversacion-i2c/) y [04](../pruebas/04-busqueda-a-mano/)

---

## Contexto

El ADR-0001 eligió I2C porque ya estaba cableado y el PN532 contestaba a su
dirección. Al escribir el primer firmware de lectura, el PN532 **se colgó**: se
quedó reteniendo el reloj (SCL) en BAJO, el bus entero quedó bloqueado, y solo
se recuperaba quitándole la corriente.

El ADR-0001 ya preveía esto en "Lo que hay que vigilar": *"que el PN532 deje de
responder... Si sigue, pasar a SPI"*.

## Qué sabíamos y qué no

**Lo que sabíamos con certeza** (medido el 2026-11-25):
- Cables y dirección `0x24`: bien (prueba 01).
- Recién encendido, el PN532 contesta su versión por I2C **perfectamente y de
  forma repetida** (prueba 03).
- Con la librería, contesta la versión y **se cuelga en el comando siguiente**,
  sin que nada reinicie al ESP32. Así que no es culpa de los reinicios.
- A mano (prueba 04): falla al pedir el "recibido" de un comando corto que **no
  enciende la antena**. Así que no es la corriente de la antena.
- Tras un solo fallo de lectura, el PN532 queda **desordenado** (`ACK: RARO`)
  hasta quitarle la corriente.

**Lo que asumimos sin verificar:**
- Que la causa es el **estiramiento de reloj**: en I2C, el PN532 retiene SCL
  mientras procesa, y el ESP32 se rinde antes de tiempo. Encaja con todo lo
  medido, pero **no se confirmó** con una prueba limpia.
- Que la versión de I2C del PN532 en este módulo no tiene un defecto propio.

**Lo que quedó pendiente de averiguar:**
- Si una pausa antes de cada pregunta bastaría para que I2C funcione. Se dejó a
  medias a propósito (ver "Opciones").

## Opciones consideradas

### Opción A — Seguir en I2C con pausas antes de cada pregunta

- **A favor:** cero cables.
- **En contra:** hay que calcular a ojo cuánto tarda cada comando, y la búsqueda
  de tarjetas tarda lo que tarde. Implica modificar la librería. **Cada pausa mal
  calculada es un cuelgue que obliga a desenchufar.**
- **Costo:** bajo en cables, alto en incertidumbre.

### Opción B — SPI

- **A favor:** el reloj lo genera **siempre el ESP32**, así que el PN532 no puede
  retenerlo y este fallo **no puede ocurrir**. Además, la línea `SS` reinicia cada
  conversación: un corte a medias no deja nada pendiente en el bus. Es el modo
  más usado de la librería con el ESP32.
- **En contra:** 4 cables de datos en vez de 2.
- **Costo:** mover el DIP y 4 cables.

### Opción C — HSU (UART)

- **A favor:** tampoco tiene reloj compartido; solo 2 cables de datos.
- **En contra:** en este clon no está rotulado qué pin hace de `TX`/`RX` en modo
  HSU, y es el camino menos usado de la librería.
- **Costo:** mover el DIP, 2 cables y averiguar los pines.

## Decisión

**Elegimos SPI (opción B).**

Lo que más pesó: con SPI, este fallo **no puede ocurrir por construcción**. No
depende de acertar con unas pausas. Los cables de SPI están rotulados en el
módulo, sin ambigüedad.

Cableado (pines por defecto del bus SPI del ESP32, el llamado VSPI):
`SCK→P18 · MISO→P19 · MOSI→P23 · SS→P5`. `VCC` y `GND` no cambian.
`IRQ` y `RSTO` siguen sin conectar, como en el ADR-0001.

## Consecuencias

**Lo que esto nos habilita:**
- Usar la librería tal cual, sin pausas ni modificaciones.
- Reiniciar el ESP32 sin miedo a dejar al PN532 colgado.

**Lo que esto nos cierra o nos cuesta:**
- P18, P19, P23 y P5 quedan ocupados.
- P5 es un pin de **arranque** (*strapping*): el ESP32 mira su nivel al
  encender. Con `SS` en reposo en ALTO no molesta, pero conviene saberlo si algún
  día la placa no arranca con algo conectado ahí.

**Lo que hay que vigilar:**
- Que el PN532 deje de responder con SPI. Si pasa, la causa **no** era I2C, y la
  sospecha pasaría al módulo o a la alimentación.

## Cómo revertir

Barato: sin corriente, DIP a I2C (1 ON, 2 OFF), quitar los 4 cables SPI y
volver a poner `SDA→P21` y `SCL→P22`. En el código, una línea (el constructor).
