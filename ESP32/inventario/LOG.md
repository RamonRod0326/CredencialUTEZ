# Bitácora de inventario

Todo movimiento de material se registra aquí, **en orden cronológico inverso**
(lo más nuevo arriba). El [INVENTARIO.md](INVENTARIO.md) dice *cómo están las
cosas hoy*; este log dice *cómo llegaron a estar así*.

## Por qué existe este archivo

Porque el inventario solo tiene el estado actual. Cuando dentro de tres meses te
preguntes *"¿por qué tengo dos ESP32 si compré uno?"* o *"¿este OLED lo quemé yo
o ya venía mal?"*, la respuesta está aquí.

**Regla clave:** un componente marcado `Dañado` en el inventario se ignora por
completo al planear un proyecto. La **única** forma de que vuelva a contar es que
tú escribas aquí una entrada de tipo `REPARACIÓN` o `REPOSICIÓN`. Yo no lo hago
por mi cuenta.

## Tipos de entrada

| Tipo | Cuándo se usa |
|---|---|
| `ALTA` | Entra material nuevo al taller |
| `COMPRA` | Se hizo un pedido (aún no llega) |
| `RECEPCIÓN` | Llegó algo que estaba en tránsito |
| `ASIGNACIÓN` | Un componente se monta en un proyecto (→ `En uso`) |
| `LIBERACIÓN` | Un componente se desmonta y vuelve al cajón (→ `Disponible`) |
| `DAÑO` | Algo se rompió, se quemó o dejó de funcionar |
| `REPARACIÓN` | **Declaras que vas a reparar** algo dañado (→ `A reparar`) |
| `REPOSICIÓN` | **Declaras que vas a reemplazar** algo dañado (→ `A reponer`) |
| `BAJA` | Se descarta definitivamente |
| `PRÉSTAMO` / `DEVOLUCIÓN` | Sale o vuelve prestado |
| `NOTA` | Observación que no cambia el estado |

## Formato de una entrada

```markdown
## AAAA-MM-DD — `TIPO` — Nombre del componente (ID)
- **Qué pasó:** una o dos líneas.
- **Estado:** `Anterior` → `Nuevo`
- **Proyecto:** nombre o `—`
- **Nota:** contexto útil para tu yo del futuro.
```

---

# Historial

## 2026-11-25 — `ALTA` — Llavero NFC (C-013)

- **Qué pasó:** Venía en el kit del PN532.
- **Estado:** — → `En uso`
- **Proyecto:** nfc-pn532 (objeto de prueba)
- **Nota:** Tipo de chip **sin confirmar**. En estos kits suele ser MIFARE
  Classic 1K, pero lo dirá el propio PN532 al leerlo. No lleva pila: se alimenta
  del campo del lector.

## 2026-11-25 — `ALTA` — Tarjetas NFC ×2 (C-012)

- **Qué pasó:** Venían en el kit del PN532.
- **Estado:** — → `En uso`
- **Proyecto:** nfc-pn532 (objetos de prueba)
- **Nota:** Tipo de chip **sin confirmar** (como el llavero C-013). Tener dos
  tarjetas sirve para comprobar que el lector las **distingue**: cada una debe
  dar un UID distinto, y siempre el mismo.

## 2026-11-25 — `ALTA` — ESP32-S NodeMCU v1.1 nueva (C-011)

- **Qué pasó:** Entra una placa nueva, ya cableada al PN532. Es la que se
  identificó por error como C-002 en la entrada de abajo.
- **Estado:** — → `En uso`
- **Proyecto:** nfc-pn532
- **Nota:** Mismo modelo que la C-001. **ESP32-D0WD-V3 rev v3.1**, 4 MB de flash,
  **CP2102**, **MAC `E0:8C:FE:30:C9:1C`**. Venía con un diagnóstico I2C
  descartable; se borró la flash entera.
  ⚠️ **Sale en la Mac con el mismo puerto que la C-001**
  (`/dev/cu.usbserial-0001`). Solo la MAC las distingue.

## 2026-11-25 — `NOTA` — Corrección: la placa del nfc-pn532 no era la C-002

- **Qué pasó:** En la entrada `ASIGNACIÓN` de abajo se **supuso** que la placa
  conectada era la C-002 porque era la única refacción de ese modelo. El usuario
  aclaró que es **una placa nueva** → se da de alta como C-011.
- **Estado (C-002):** `En uso` → `Disponible`
- **Proyecto:** —
- **Nota:** La C-002 sigue en algún cajón y su MAC **sigue sin conocerse**. La
  entrada de abajo se conserva tal cual: es el registro de un supuesto que
  resultó falso. Lección: preguntar antes de asignar una pieza que no se ha
  visto.

## 2026-11-25 — `ALTA` — Módulo NFC PN532 Kit V3, Fydun (C-010)

- **Qué pasó:** Entra al taller un lector NFC para el proyecto nuevo
  `nfc-pn532`. Llega ya cableado por I2C a la C-002.
- **Estado:** — → `En uso`
- **Proyecto:** nfc-pn532
- **Specs observadas:** placa roja con chip NXP **PN532**, antena impresa en el
  borde, interruptor DIP de 2 posiciones para elegir la interfaz (HSU / I2C /
  SPI), conector de 4 pines `SCL SDA VCC GND` y otro de 8 para SPI.
  **Contesta en I2C en `0x24`**, medido el 2026-11-25.
- **Nota:** Alimentado a **3,3 V** a propósito: así sus líneas de datos se
  quedan a 3,3 V, que es lo que toleran los pines del ESP32. ⚠️ El DIP se cambia
  **solo sin corriente**: el chip lo lee al encender. Pendiente saber si el kit
  trajo tarjetas o llaveros; si sí, se dan de alta aparte.

## 2026-11-25 — `ASIGNACIÓN` — ESP32-S NodeMCU v1.1 (C-002)

- **Qué pasó:** Se identifica con `esptool` y se asigna al proyecto `nfc-pn532`.
  Venía con un firmware de diagnóstico I2C (descartable) y se le **borró la
  flash entera**.
- **Estado:** `Disponible` → `En uso`
- **Proyecto:** nfc-pn532
- **Nota:** Identidad confirmada: **ESP32-D0WD-V3 rev v3.1**, 4 MB de flash,
  **CP2102**, **MAC `E0:8C:FE:30:C9:1C`**. Es la misma placa que la C-001, como
  se suponía. Se asume que es la C-002 porque es la única refacción de ese
  modelo en el inventario.
  ⚠️ **Sale en la Mac con el mismo puerto que la C-001**
  (`/dev/cu.usbserial-0001`), porque los dos CP2102 llevan el número de serie
  `0001`. **Solo la MAC las distingue.** Comprobarla antes de cada carga de
  firmware: un descuido aquí sobrescribe el Tamagotchi.

## 2026-08-30 — `COMPRA` — Elevador MT3608 (C-009)

- **Qué pasó:** Hace falta para alimentar con batería. Decidido en el
  [ADR-0014](../proyectos/tamagotchi/1-decisiones/ADR-0014-alimentacion-por-bateria.md).
- **Estado:** — → `Por comprar`
- **Proyecto:** tamagotchi
- **Por qué:** la placa C-001 monta un **`AMS1117-3.3`** (leído en el chip:
  `1117 3.3 BWQ53`), que necesita **4,4 V** a la entrada. La batería llega a
  4,2 V como mucho, así que no alcanza ni recién cargada. El MT3608 eleva los
  3,0–4,2 V de la batería a 5 V y entra por `VIN`, el mismo camino que el USB.
- **Nota:** ⚠️ **Es ajustable y NO viene a 5 V de fábrica.** Hay que ponerlo a
  5 V con el polímetro y **sin nada conectado a la salida** antes de acercarlo al
  ESP32. Puede venir a 20 V. Es el único paso del montaje que puede matar la placa.
  ⚠️ Los módulos corrientes **no sacan el pin `EN`**, así que el interruptor C-008
  tendrá que llevar la corriente y no solo la orden.

## 2026-08-30 — `ALTA` — Batería LiPo 1000 mAh (C-006)

- **Qué pasó:** El usuario aporta una celda LiPo de bolsa para que el Tamagotchi
  deje de depender del cable USB.
- **Estado:** — → `Disponible`
- **Proyecto:** tamagotchi *(pendiente de asignar hasta resolver la conversión)*
- **Specs observadas** (serigrafía de la propia celda): `3.7V · 1000 mAh ·
  3.7 Wh · 202407`. Una sola celda, con el `+` marcado junto al cable rojo.
- **Nota:** ⚠️ Ya viene **soldada a `B+`/`B-`** del cargador C-007. Una LiPo
  **no se puede apagar**: sus terminales tienen tensión siempre. Cortocircuitar
  rojo con negro, aunque sea un instante, puede hincharla o incendiarla.

## 2026-08-30 — `ALTA` — Cargador TP4056 con protección (C-007)

- **Qué pasó:** Módulo cargador aportado junto con la batería, ya soldado a ella.
- **Estado:** — → `Disponible`
- **Proyecto:** tamagotchi
- **Specs observadas:** placa azul con conector USB, chip `4056` y un segundo
  chip pequeño de protección. Cuatro pastillas rotuladas **`B+` `B-` `OUT+`
  `OUT-`**. `B+`/`B-` **ya soldadas** a la celda; `OUT+`/`OUT-` **libres**.
- **Nota:** ⚠️ **`OUT+` NO da 5 V.** Da la tensión de la batería, entre 3,0 y
  4,2 V según lo cargada que esté. Es el punto que decide todo el esquema de
  alimentación (ver `6-esquemas/alimentacion-por-bateria.md`).
  Que existan `OUT` separadas de `B` indica que **sí lleva protección** contra
  sobredescarga y cortocircuito, que es la versión buena de este módulo.

## 2026-08-30 — `ALTA` — Interruptor deslizante SPDT (C-008)

- **Qué pasó:** Interruptor de dos posiciones para encender y apagar el aparato
  sin desconectar la batería.
- **Estado:** — → `Disponible`
- **Proyecto:** tamagotchi
- **Specs observadas:** tipo `SS12D00`, tres patas en línea. **La pata del medio
  es la común** y ya tiene soldado un cable rojo. Las dos de los extremos son
  los dos destinos; para encender/apagar solo se usa **una** de ellas.
- **Nota:** ⚠️ Estos interruptores se venden con **0,5 A** de corriente máxima, y
  el ESP32 llega a **500 mA de pico** al transmitir. Va justo. Pendiente de
  decidir si lleva la corriente o solo una señal de encendido.

## 2026-08-29 — `NOTA` — ✅ C-001 identificada y funcionando

- **Qué pasó:** La placa fue detectada, identificada e interrogada con `esptool`.
  **La causa del bloqueo eran cables USB-C de solo carga**, no la placa.
- **Estado:** `Disponible` → `En uso`
- **Proyecto:** tamagotchi
- **Nota:** Identidad confirmada: **ESP32-D0WD-V3 rev v3.1**, 2 núcleos @ 240 MHz,
  4 MB de flash, Wi-Fi + BT, chip USB-Serie **CP2102** (Silicon Labs), puerto
  `/dev/cu.usbserial-0001`. **MAC `84:1F:E8:2C:3E:C4`** — sirve para distinguirla
  de la C-002, que es idéntica por fuera.
  El `-S` de "ESP32-S" era marca comercial del vendedor: es el ESP32 clásico, no
  un S2 ni un S3. Auto-reset funcionando, así que no hace falta apretar BOOT al
  programar.

## 2026-08-29 — `NOTA` — ⚠️ Cables USB-C de solo carga (causa raíz del bloqueo)

- **Qué pasó:** Varios de los cables C-a-C disponibles **llevan corriente pero no
  datos**. Con ellos el LED de la placa enciende pero la Mac no ve nada.
- **Estado:** —
- **Proyecto:** tamagotchi
- **Nota:** Se registra aquí porque **el cable es material de taller**, y este es
  exactamente el tipo de cosa que uno vuelve a sufrir en un año.
  🔖 **Regla:** ante un "no me detecta la placa", conectar ese cable a un celular.
  Si la compu no ve el celular, el cable no pasa datos y no hay nada más que
  investigar. Cuesta 20 segundos.
  💡 Vale la pena marcar físicamente el cable que sí funciona.

## 2026-08-29 — `NOTA` — GMT130 (C-004): especificaciones confirmadas

- **Qué pasó:** Verificación física del módulo. Confirmado: **`Driver IC: ST7789`,
  3.3 V, 7 pines** (`GND · VCC · SCK · SDA · RES · DC · BLK`).
- **Estado:** `Disponible` → `Disponible`
- **Proyecto:** tamagotchi
- **Nota:** Los 3 supuestos del S-07 quedan resueltos de una vez. Consecuencias:
  (1) la librería `TFT_eSPI` es compatible con seguridad; (2) al ser 3.3 V se
  conecta directo al ESP32 sin conversor de nivel; (3) al no tener `CS`, la
  pantalla acapara el bus SPI — irrelevante hoy, importante si algún día se
  agrega otro dispositivo SPI. Esto desbloqueó la fase 6: el plano físico ya está
  escrito en `6-esquemas/conexiones-fisicas.md`.

## 2026-08-29 — `NOTA` — C-003 (DEVKIT v1) tampoco es detectada

- **Qué pasó:** Se conectó la C-003 (mini-USB) a través del hub UGreen. **No
  aparece.** El árbol USB solo muestra el hub VIA Labs, el DisplayLink y el
  Billboard. Cero puertos serie nuevos.
- **Estado:** sin cambios
- **Proyecto:** tamagotchi
- **Nota:** Descarta que el problema sea exclusivo del USB-C (la hipótesis de las
  resistencias CC), porque la C-003 es mini-USB y no puede tener ese problema.
  Con dos placas distintas y dos conectores distintos fallando igual, el
  sospechoso pasa a ser lo que tienen en común: **el cable o el puerto del hub**.
  Sigue descartado que sea un driver: un dispositivo sin driver igual aparecería
  en el árbol USB como no reconocido. Aquí no aparece *nada*, lo que significa
  que la Mac no ve conexión eléctrica alguna.

## 2026-08-29 — `NOTA` — Nueva hipótesis sobre por qué C-001 no se detecta

- **Qué pasó:** Al declarar el inventario salió un dato clave: **C-001 y C-002
  tienen puerto USB-C**. Eso abre una hipótesis nueva y muy probable que no
  estaba sobre la mesa en el primer diagnóstico.
- **Estado:** sin cambios
- **Proyecto:** tamagotchi
- **Nota:** Muchísimas placas ESP32 baratas con USB-C **omiten dos resistencias
  de 5.1 kΩ** que el estándar USB-C exige en las líneas CC. Sin ellas, una
  computadora con puertos USB-C **no reconoce que hay algo conectado** y ni
  siquiera le entrega corriente por un cable C-a-C. La misma placa funciona
  perfecto con un cable A-a-C. El síntoma coincide exactamente con lo observado:
  cero enumeración, dispositivo invisible.
  **Prueba decisiva:** usar la C-003 (DEVKIT v1, mini-USB), que por su conector
  no puede tener este problema. Si esa sí aparece, la hipótesis queda confirmada
  y el bloqueante se resuelve con un cable, no con una placa nueva.

## 2026-08-29 — `ALTA` — TFT IPS 1.3" GMT130-v1.0 (C-004) ×4

- **Qué pasó:** Se registran 4 pantallas TFT que ya tenías.
- **Estado:** — → `Disponible`
- **Proyecto:** una se reservará para el tamagotchi
- **Nota:** 240×240 píxeles, IPS, a color. Es **muy superior** a la OLED de 0.96"
  que se había supuesto: en una OLED caben ~4 líneas de 20 caracteres; aquí caben
  del orden de 15 líneas de ~30. Eso cambia por completo la viabilidad de mostrar
  letras (RF-04) y hace pensable mostrar la portada del álbum más adelante.
  Tener 4 unidades quita presión: se puede experimentar sin miedo a quemar la única.

## 2026-08-29 — `ALTA` — ESP32 DEVKIT v1, mini-USB (C-003)

- **Qué pasó:** Se registra una tercera placa, más vieja, con puerto mini-USB.
- **Estado:** — → `Disponible`
- **Proyecto:** — (se usará para diagnóstico)
- **Nota:** 🎯 Esta es la pieza más valiosa del inventario **hoy mismo**. Al no
  ser USB-C, permite descartar de un solo intento la hipótesis del cable/CC.

## 2026-08-29 — `ALTA` — ESP32-S NodeMCU USB-C (C-002)

- **Qué pasó:** Se registra una segunda placa idéntica a C-001.
- **Estado:** — → `Disponible`
- **Proyecto:** —
- **Nota:** Sirve de refacción y de control experimental: si C-001 no aparece
  pero C-002 sí con el mismo cable, C-001 está dañada.

## 2026-08-29 — `NOTA` — ESP32-S NodeMCU (C-001)

- **Qué pasó:** Primer intento de detectar la placa desde la Mac (macOS 26.5.2,
  Apple Silicon). **No fue detectada.** El árbol USB completo solo muestra el
  dock DisplayLink, sus hubs, la webcam y el instalador de driver del propio dock.
  No aparece ningún chip USB-Serial (CP2102 / CH340 / FTDI) ni ningún puerto
  `/dev/cu.usbserial*`.
- **Estado:** `Disponible` → `Disponible` *(sin cambio: no hay evidencia de daño,
  lo más probable es cable o conexión)*
- **Proyecto:** tamagotchi
- **Nota:** Ojo — sí apareció un dispositivo del fabricante `wch.cn` (el mismo que
  hace los chips CH340), pero al abrirlo resultó ser el instalador de drivers del
  **dock DisplayLink**, no la placa. Falso positivo descartado. Pendiente probar
  con otro cable USB (que sea de datos, no de solo carga) y conectando directo a
  la Mac en vez de al dock.

## 2026-08-29 — `ALTA` — Módulo joystick analógico de 2 ejes (C-005)

- **Qué pasó:** Se registra un joystick que ya tenías, para los controles de
  reproducción del Tamagotchi (RF-08, RF-09, RF-10) y la navegación entre modos.
- **Estado:** — → `Disponible`
- **Proyecto:** tamagotchi
- **Specs observadas:** 5 pines rotulados `GND`, `+5V`, `VRx`, `VRy`, `SW`. Es la
  forma estándar del módulo tipo KY-023: **dos potenciómetros de 10 kΩ** (uno por
  eje) y un **pulsador** que cierra a `GND` al empujar la palanca hacia abajo.
- **Nota:** ⚠️ El rótulo `+5V` es engañoso. **Se alimentará a 3,3 V, no a 5 V.**
  A 5 V las salidas `VRx`/`VRy` llegarían a 5 V y los pines del ESP32 no toleran
  más de 3,3 V. Al ser potenciómetros, funcionan igual con menos tensión: solo
  cambia el rango de lectura, no el comportamiento.
  Pendiente confirmar midiendo que en reposo cada eje queda cerca de la mitad
  del rango.

## 2026-08-29 — `ALTA` — ESP32-S NodeMCU (C-001)

- **Qué pasó:** Se registra la placa que ya tenías al arrancar el taller.
- **Estado:** — → `Disponible`
- **Proyecto:** — *(se asignará al Tamagotchi cuando la conectemos)*
- **Nota:** Falta confirmar el modelo exacto (cuántos pines, qué chip USB-Serial
  trae). Eso se documenta en
  `proyectos/tamagotchi/3-componentes/esp32-nodemcu.md` en cuanto la placa
  responda.
