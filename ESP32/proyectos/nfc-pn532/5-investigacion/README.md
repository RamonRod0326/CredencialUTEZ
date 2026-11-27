# Investigación — nfc-pn532

Lo que hubo que averiguar, con fecha y fuente. Por ahora solo hay preguntas.

## Preguntas abiertas

| Pregunta | Para qué hace falta | Estado |
|---|---|---|
| ¿Qué librería usar para el PN532? | ADR-0002 | ✅ Adafruit 1.3.4, verificada leyendo su código |
| ¿De qué tipo son las 2 tarjetas y el llavero (MIFARE Classic, NTAG…)? | RF-04 e inventario | 🟡 Cuántas: ✅ (2 + 1). Tipo: lo dirá el PN532, o una app de lectura NFC en el celular |
| ¿Cómo se presenta el celular ante el lector? | RF-02 | ✅ SAK `0x20` (ISO 14443-4), UID aleatorio `08 …`; con la app contesta APDUs |
| ¿Qué tan aleatorio es `esp_fill_random()` sin WiFi? | S-05 | 🔴 Según Espressif, solo con la radio o `bootloader_random_enable()`. Sin investigar aquí |
| ¿Por qué la especificación decía que Adafruit falla por I2C "en el core 4.x"? | ADR-0004 | 🟡 Aquí falla igual en el 2.0.17: no parece cosa del core |
| ¿Es estable el I2C del ESP32 con el PN532? | ADR-0003 | ✅ **No**: se cuelga (pruebas 03–04). Con SPI, sí. Mecanismo exacto sin confirmar (¿estiramiento de reloj?) |
