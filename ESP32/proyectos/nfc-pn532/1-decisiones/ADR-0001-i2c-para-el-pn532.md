# ADR-0001 — Hablar con el PN532 por I2C

- **Estado:** `Reemplazado por ADR-0003` (2026-11-25: el PN532 se colgaba por I2C)
- **Fecha:** 2026-11-25
- **Reemplaza a:** —
- **Relacionado con:** RF-01, RF-02 · [`6-esquemas/conexiones-fisicas.md`](../6-esquemas/conexiones-fisicas.md)

---

## Contexto

El módulo PN532 (C-010) sabe hablar **tres idiomas** con el ESP32, y se elige
cuál con un interruptor DIP de dos posiciones: **HSU** (puerto serie, UART),
**I2C** y **SPI**. Hay que escoger uno antes de escribir código, porque cambia
los cables, la librería y cómo se inicializa todo.

El proyecto es un banco de pruebas para aprender: lo que importa es que funcione
con pocos cables y sea fácil de entender, no la velocidad. Un número de tarjeta
(UID) son 4 a 7 bytes; cualquier bus va sobrado.

Al llegar a esta decisión, **el usuario ya tenía el módulo cableado en I2C**.

## Qué sabíamos y qué no

**Lo que sabíamos con certeza:**
- El cableado I2C actual funciona: el 2026-11-25 el PN532 contestó en la
  dirección `0x24` (medido con el firmware que traía la placa).
- Por lo tanto el DIP ya está en modo I2C.
- El módulo trae sus propias resistencias pull-up (las líneas leían ALTO en reposo).

**Lo que asumimos sin verificar:**
- Que el I2C del ESP32 sea estable con el PN532 durante lecturas continuas. Hay
  comentarios en foros sobre inestabilidad de esta combinación; aquí **no** se
  ha visto ni confirmado nada.
- Que no hace falta `IRQ` ni `RSTO` para pruebas.

**Lo que quedó pendiente de averiguar:**
- Qué librería usar (→ ADR-0002) y cómo se comporta con el ESP32.

## Opciones consideradas

### Opción A — I2C

- **A favor:** 2 cables de datos (4 en total). Ya cableado y **comprobado**.
  Comparte bus con otros sensores I2C si un día hacen falta.
- **En contra:** el más lento de los tres (irrelevante aquí). Sin `RSTO`, si el
  PN532 se cuelga solo se recupera quitando la corriente.
- **Costo:** cero; ya está hecho.

### Opción B — SPI

- **A favor:** el más rápido; con fama de ser el más robusto con este chip.
- **En contra:** 4 cables de datos (6 en total) y recablear lo que ya funciona.
- **Costo:** recablear + cambiar DIP.

### Opción C — HSU (UART)

- **A favor:** 2 cables de datos; el ESP32 tiene un puerto serie libre (UART2).
- **En contra:** hay que vigilar no usar `TX`/`RX` (los del USB). Sin ventaja
  clara sobre I2C para esto.
- **Costo:** recablear + cambiar DIP.

## Decisión

**Elegimos I2C (opción A).**

Ya está cableado y está **medido** que contesta. Para un banco de pruebas, el
menor número de cables y no tocar lo que funciona pesa más que la velocidad.

También se decide **no conectar `IRQ` ni `RSTO`** por ahora: el código preguntará
periódicamente si hay tarjeta, que es suficiente para pruebas.

## Consecuencias

**Lo que esto nos habilita:**
- Empezar a escribir firmware ya, sin tocar cables.

**Lo que esto nos cierra o nos cuesta:**
- Si el PN532 se cuelga, el ESP32 no puede reiniciarlo: hay que desenchufar.

**Lo que hay que vigilar:**
- Que el PN532 deje de responder de vez en cuando, o que el arranque falle una
  vez sí y otra no. Primera respuesta: conectar `RSTO`. Si sigue, pasar a SPI
  con un ADR nuevo que reemplace a este.

## Cómo revertir

Barato: **sin corriente**, mover el DIP y recablear 4–6 cables. Del lado del
código, cambiar la línea que crea el objeto del lector.
