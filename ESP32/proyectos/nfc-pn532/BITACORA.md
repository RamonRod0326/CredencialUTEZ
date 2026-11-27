# Bitácora — nfc-pn532

Diario de trabajo. Lo más nuevo arriba. Aquí va **el trayecto**: los intentos que
fallaron, las sospechas sin confirmar, el cable que resultó estar malo. Las
*decisiones* van en [`1-decisiones/`](1-decisiones/).

---

## 2026-11-25 — Sesión 1: arranque

### Qué se hizo
- Proyecto creado con `herramientas/nuevo-proyecto.sh`. Herramientas clavadas a
  las mismas versiones del Tamagotchi: PlatformIO `6.1.19`, esptool `5.3.1`.
- **Placa identificada por su MAC antes de borrar nada:** `E0:8C:FE:30:C9:1C`
  → **no** es la C-001 del Tamagotchi (`84:1F:E8:2C:3E:C4`). ESP32-D0WD-V3
  rev3.1, 4 MB, CP2102. Primero supuse que era la C-002 (la refacción del
  inventario); el usuario aclaró que **es nueva** → quedó como **C-011**.
- **Lo que traía cargado:** un diagnóstico I2C del PN532, descartable. Antes de
  borrarlo se escuchó su salida:
  ```
  SDA (P21): ALTO -> cable conectado al PN532
  SCL (P19): ALTO -> cable conectado al PN532
  PN532 en 0x24: SI responde
  ```
- **Borrado completo** con `esptool erase-flash` (4,2 s). Verificado: al
  arrancar solo repite `invalid header: 0xffffffff` → memoria en blanco.
- Inventario: placa nueva C-011, PN532 C-010, 2 tarjetas C-012, llavero C-013.
  La C-002 vuelve a `Disponible` con una entrada de corrección en el LOG.
- Cableado revisado → [`6-esquemas/conexiones-fisicas.md`](6-esquemas/conexiones-fisicas.md).
- [ADR-0001](1-decisiones/ADR-0001-i2c-para-el-pn532.md): I2C, sin `IRQ` ni `RSTO`.
- El usuario **movió SCL de P19 a P22**. Verificado con la
  [prueba 01](pruebas/01-cableado-i2c/), primera carga de firmware del proyecto
  (compilar + cargar: 17 s):
  ```
  SDA en P21: OK   (siente al PN532)
  SCL en P22: OK   (siente al PN532)
  P19 (SCL viejo): OK   (libre)
  Contesta algo en 0x24  <- el PN532
  Veredicto: CABLEADO OK
  ```

### Hallazgos
- 🎯 **El cableado funciona**, medido y no supuesto: el PN532 contestó en
  `0x24`. De paso confirma que **el DIP está en modo I2C**.
- 🟡→✅ **SCL estaba en P19, no en P22** (el de por defecto). Funcionaba, pero
  cualquier ejemplo copiado tal cual habría buscado el reloj en P22. Ya movido
  y verificado.
- ⚠️ **Supuesto falso de mi parte:** asigné la placa a la C-002 sin preguntar.
  Era nueva. Queda la corrección en el LOG; la lección es preguntar antes de
  asignar una pieza que no se ha visto.
- ⚠️ **C-001 y C-011 se ven en la Mac con el mismo puerto**,
  `/dev/cu.usbserial-0001`: los dos CP2102 llevan el número de serie `0001`.
  Solo la MAC las distingue. `/abrir-sesion` aún asume que la placa enchufada
  es la del Tamagotchi.

### Segunda parte: el firmware de lectura
- Decidido con el usuario: **un parpadeo del LED (GPIO2) por cualquier cosa
  detectada**, y volcar lo que traiga la etiqueta para identificarlas.
- [ADR-0002](1-decisiones/ADR-0002-libreria-adafruit-pn532.md): Adafruit PN532
  `1.3.4`, verificada leyendo su código (funciona sin `IRQ`/`RSTO`; por defecto
  busca para siempre; no entrega el tipo de etiqueta).
- `firmware/` escrito y cargado. Lleva `proteger-placa.py`: **antes de cada
  carga lee la MAC y cancela si no es la C-011**. Probado: `✓ Placa correcta`.
- ❌ **El firmware no encontró al PN532:** `requestFrom(): i2cRead returned
  Error 263` cada ~1 s. 263 = tiempo agotado.
- 🔍 [Prueba 03](pruebas/03-conversacion-i2c/), conversación I2C a mano:
  ```
  Lineas en reposo: SDA=ALTO SCL=BAJO(!)
  Direccion 0x24: NO contesta
  ```
  **El PN532 tiene el reloj (SCL) retenido en BAJO** y bloquea el bus. Reiniciar
  el ESP32 no lo suelta: el PN532 sigue con corriente. Es el riesgo que
  anticipaba el ADR-0001 al no conectar `RSTO`.
- Sospechas sin confirmar: (a) se colgó porque un reinicio del ESP32 (al cargar
  programas) cortó una conversación a medias; (b) al despertar, el PN532 retiene
  el reloj más tiempo del que el ESP32 tolera.
- Desenchufar y enchufar → **prueba 03 con el PN532 recién encendido: la
  conversación sale perfecta** (`D5 03 32 01 06 07` = PN532, firmware 1.6), una y
  otra vez, y suelta el reloj. Descarta (b) tal como estaba planteada.
- Firmware de nuevo, **sin reiniciar nada**: `PN532 encontrado · firmware 1.6` y
  acto seguido `Error 263`. Se cuelga solo, en el primer paso **tras** la versión.
  Descarta que la culpa sea de los reinicios (a).
- 🔍 [Prueba 04](pruebas/04-busqueda-a-mano/), recién encendido:
  ```
  1. Version                    -> VIVO
  2. Limitar la busqueda a 5    -> ACK: fallo del bus (1 s), SCL suelto
  ```
  Falla **antes de encender la antena** → **no es la corriente**. Falla al
  pedir el "recibido" de un comando corto. Segunda corrida (pausa de 50 ms, sin
  desenchufar): `ACK: RARO` — el PN532 quedó desordenado por la corrida
  anterior; esa corrida no vale como prueba.
- 🎯 **Conclusión:** el enlace I2C entre este ESP32 y este PN532 es frágil. Un
  solo fallo de lectura lo deja inconsistente, y la librería, al insistir, lo
  acaba colgando con el reloj en BAJO. Mecanismo exacto **sin confirmar**; la
  sospecha es el estiramiento de reloj (el PN532 retiene SCL mientras procesa y
  el ESP32 no lo aguanta).

### Tercera parte: SPI
- [ADR-0003](1-decisiones/ADR-0003-spi-para-el-pn532.md): pasar a SPI (reemplaza
  al ADR-0001). El usuario recableó: `SCK→P18 · MISO→P19 · MOSI→P23 · SS→P5`,
  DIP a SPI. Firmware adaptado.
- ❌ El firmware no lo encuentra por SPI. [Prueba 05](pruebas/05-spi-a-mano/):
  ```
  P18 SCK / P19 MISO / P23 MOSI / P5 SS : jalado ARRIBA   (los 4 llegan al módulo)
  P21 / P22                            : suelto          (I2C desconectado, bien)
  Estado: FF  ·  ACK: nunca dijo LISTO, solo FF
  ```
  El PN532 **no contesta nada por SPI**.
- 💡 **Contexto nuevo del usuario:** *"esta placa funcionaba bien el nfc pero de
  repente dejó de leerlo"*. O sea: **con I2C ya funcionó antes**. Eso debilita la
  explicación del ADR-0003 (que I2C sea frágil con el ESP32) y fortalece el
  supuesto que ese ADR dejó anotado como no verificado: **que el módulo tenga un
  defecto propio**. Encaja con lo medido: contesta su versión (lo más simple que
  sabe hacer) pero falla en cuanto se le pide algo más.
- Historia (usuario): soldó él los pines y se ven bien; **nunca a 5 V/VIN**;
  LED `PWR` encendido como siempre; sin golpes ni metal.
- [Prueba 06](pruebas/06-spi-variantes/): **24 variantes de SPI (3 velocidades ×
  4 modos × 2 órdenes) y bit a bit a mano a ~1 kHz: siempre `FF`.** El PN532 no
  toca MISO nunca. Descarta ajustes de software y el circuito SPI del ESP32.
- Razonamiento: hace 30 min el mismo chip contestaba su versión por I2C. Que
  ahora esté mudo del todo apunta más a **cómo quedó puesto** (DIP, MISO/MOSI
  cruzados, `SS`) que a que el chip haya muerto justo en ese rato.
- 📷 Foto del usuario (ampliada): **lado del módulo correcto** — 4 cables en
  `SCK MISO MOSI SS`, `VCC`/`GND` en el conector de 4 pines, `SCL`/`SDA` libres.
  El DIP sale borroso, pero **las dos palancas parecen del mismo lado**. Si es así,
  no es SPI: tanto I2C (1 0) como SPI (0 1) llevan las palancas **opuestas**.
  Sospecha: se movió la 2 a ON y la 1 se quedó en ON → `1 1`, combinación no válida.
- 📷 Segunda foto, con el DIP nítido: **palanca 1 hacia `ON`, palanca 2 abajo →
  `1 0` = I2C.** El DIP **nunca se cambió a SPI**: el usuario no había ubicado
  el interruptor (lo encontró al mandar esta foto). Mi sospecha de la primera
  foto (`1 1`) era errónea. Esto explica **todo** el silencio por SPI de las
  pruebas 05 y 06: el PN532 seguía escuchando en I2C, con esos cables quitados.
- Sin probar, a propósito: invertir MISO/MOSI por software para ver si están
  cruzados. Si no lo están, dos salidas pelearían por el mismo cable.

### Cuarta parte: leyendo de verdad, y aparece el objetivo real
- **SPI funciona** con la secuencia completa (despertar, versión, reintentos,
  búsqueda en 42 ms). Los pasos que colgaban al PN532 por I2C pasan sin problema.
  **El módulo no estaba dañado.** Lo que obligó a desenchufar dos veces fue el
  DIP, que seguía en I2C.
- Lecturas (registro en `logs/`, ignorado por Git):
  - Una MIFARE Classic 1K, UID `A9 EE AA E6`: vacía, candados de fábrica.
    Completa cuando se deja ~1,1 s; si se retira antes, sale "incompleta".
  - Celular: SAK `0x20`, UID aleatorio `08 …` distinto cada vez (como se esperaba).
  - Una tarjeta bancaria (prueba del usuario): SAK `0x20`, UID fijo, detectada
    **varias veces seguidas**. Por confirmar si el lector la redetecta sola
    mientras sigue encima. *(Su UID no se anota aquí a propósito.)*
- Corregido: el "sector 1 cerrado" de una lectura fue un fallo pasajero; en la
  lectura completa, 0 sectores cerrados.
- ❓ Solo apareció **un** UID de etiqueta en las dos rondas: falta saber si la
  tarjeta B y el llavero no se detectan o si comparten UID.
- Firmware: ahora marca la hora de cada detección, cuándo se va el objeto, en
  qué sector se corta una lectura, y `i` lista las etiquetas distintas.
- 📄 **El usuario compartió la especificación del objetivo real:** un lector de
  credenciales universitarias. El celular Android emula una tarjeta (HCE) y el
  lector la valida con APDUs y HMAC-SHA256. La especificación trae una **clave
  compartida** que **no** se copia al repositorio: irá en un archivo ignorado.
  Explica por qué la placa venía con SCL en P19 y un diagnóstico I2C.

### Quinta parte: el lector de credenciales funciona
- Decidido con el usuario: se adopta **solo el protocolo** de la app; el
  hardware se queda como está (SPI, PlatformIO, LED de placa) → [ADR-0004](1-decisiones/ADR-0004-compatibilidad-con-la-app.md).
- Firmware reescrito: SELECT → TOKEN (nonce) → HMAC-SHA256 → RESULTADO.
  `config.h` + `src/secrets.h` (ignorado por Git). El volcado viejo pasó a la
  prueba 07. Autoprueba criptográfica: **OK**.
- La librería imprimía mensajes de depuración sin control → copia local con
  dos parches en `firmware/lib/` (ver su `LEEME.md`).
- ✅ **Con la app real:** acceso permitido (firma válida, RESULTADO `90 00`).
  Sin huella → `69 85`. Tarjeta bancaria → `6A 82` (ignorada). MIFARE →
  ignorada. Una sola lectura por acercamiento. *(La matrícula no se anota aquí.)*

### 🔜 En qué me quedé
- 🔌 **Estado físico:** C-011 enchufada, PN532 en **SPI** (DIP 1 OFF, 2 ON),
  con el **lector de credenciales** cargado y funcionando. Sin commit todavía.
- [ ] 👀 Confirmar mirando: parpadeo largo (permitido) vs corto (lo demás).
- [ ] Probar RF-08: desconectar el PN532 y ver que el monitor avisa.
- [ ] Nonce sin WiFi (S-05, ADR-0004).
- [ ] Si la clave es la de producción: cambiarla (apareció en una conversación).
- [ ] Hacer commit (revisar antes que `src/secrets.h` NO entre).
- [ ] Buscar la C-002 en el cajón; la tarjeta B y el llavero nunca se leyeron.
