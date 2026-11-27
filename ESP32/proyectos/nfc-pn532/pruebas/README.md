# Pruebas — nfc-pn532

Programas pequeños y **desechables**, cada uno para responder **una** pregunta.
El código se puede tirar; la respuesta se queda anotada aquí y en la bitácora.

Cada prueba es un proyecto de PlatformIO independiente. Para cargar una, desde
`proyectos/nfc-pn532/`:

```bash
source 2-entorno/.venv/bin/activate
esptool --port /dev/cu.usbserial-0001 read-mac      # ⚠️ debe ser E0:8C:FE:30:C9:1C
pio run -d pruebas/01-cableado-i2c -t upload        # compila y carga
pio device monitor -d pruebas/01-cableado-i2c       # ver lo que dice (Ctrl+C para salir)
```

| # | Pregunta | Respuesta | Fecha |
|---|---|---|---|
| [01](01-cableado-i2c/) | ¿Están bien los cables I2C del PN532? | ✅ **Sí.** SDA en P21 y SCL en P22 llegan, P19 libre, el PN532 contesta en `0x24` | 2026-11-25 |
| [02](02-led/) | ¿Tiene la placa un LED que el código controle (GPIO2)? | ✅ **Sí**: el LED azul parpadea (lo confirmó el usuario mirando) | 2026-11-25 |
| [03](03-conversacion-i2c/) | ¿Dónde se atora la conversación con el PN532? | 🟡 Colgado: **SCL pegado a BAJO**. Recién encendido: la versión sale perfecta, una y otra vez | 2026-11-25 |
| [04](04-busqueda-a-mano/) | ¿Qué cuelga al PN532: la antena o preguntar mientras trabaja? | 🔴 **No es la antena**: falla al pedir el ACK de un comando corto, *antes* de encenderla. Tras un fallo, el PN532 queda desordenado (`ACK: RARO`) | 2026-11-25 |
| [05](05-spi-a-mano/) | ¿Qué contesta el PN532 por SPI, byte a byte? | 🔴 **Todo `FF`**: nunca contesta (el DIP seguía en I2C). Los 4 cables llegan al módulo | 2026-11-25 |
| [06](06-spi-variantes/) | ¿Contesta con ALGUNA variante de SPI (velocidad, modo, orden, a mano)? | 🔴→✅ Primero **0 de 24**: el DIP seguía en I2C. Con el DIP en SPI: **contesta** (modo 0, LSB = `01`) | 2026-11-25 |
| [07](07-volcado-etiquetas/) | ¿Qué trae guardado una tarjeta o etiqueta? | ✅ Herramienta: tipo, UID y volcado de MIFARE Classic / Ultralight / NTAG. Fue el primer firmware del proyecto | 2026-11-25 |

## Cómo funciona la prueba 01 (y por qué sirve)

Comprueba de lo más físico a lo más lógico, para que si algo falla se sepa
**dónde**:

1. **¿Llega cada cable?** Pone el pin con una resistencia interna débil hacia
   abajo (*pull-down*). Si aun así se lee ALTO, es que la resistencia del PN532
   lo está jalando hacia arriba → el cable está puesto. Es como saber que alguien
   sostiene una cuerda porque, aunque tiras suave, no cede.
2. **¿Contesta el chip?** Pregunta en las 127 direcciones del bus I2C quién
   está. Solo debe contestar `0x24`.

Si el paso 1 pasa y el 2 falla, los cables llegan pero algo más está mal: el
DIP fuera de I2C, o SDA y SCL cruzados.
