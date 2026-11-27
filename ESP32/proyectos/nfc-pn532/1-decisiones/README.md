# Decisiones del proyecto nfc-pn532 (ADRs)

Ninguna decisión se borra. Cuando una cambia, se escribe un ADR nuevo que
reemplaza al viejo, y el viejo se conserva íntegro con su razonamiento original.

| ADR | Decisión | Estado | Fecha |
|---|---|---|---|
| [0001](ADR-0001-i2c-para-el-pn532.md) | Hablar con el PN532 por I2C (sin `IRQ` ni `RSTO`) | `Reemplazado por ADR-0003` | 2026-11-25 |
| [0002](ADR-0002-libreria-adafruit-pn532.md) | Usar la librería Adafruit PN532 `1.3.4` | `Aceptado` | 2026-11-25 |
| [0003](ADR-0003-spi-para-el-pn532.md) | Hablar con el PN532 por **SPI** (reemplaza a 0001) | `Aceptado` | 2026-11-25 |
| [0004](ADR-0004-compatibilidad-con-la-app.md) | Adoptar el **protocolo** de la app, no su especificación de hardware | `Aceptado` | 2026-11-25 |

Plantilla: [`metodologia/plantillas/ADR-plantilla.md`](../../../metodologia/plantillas/ADR-plantilla.md)
