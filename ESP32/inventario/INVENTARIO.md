# Inventario general de componentes

> **Este archivo es de uso general.** Todos los proyectos leen de aquí.
> Ningún proyecto tiene su propio inventario: si algo se usa en un proyecto,
> se marca aquí en la columna `Proyecto`.

**Última actualización:** 2026-11-25
**Reglas de uso:** ver [README.md](README.md) · **Historial de movimientos:** ver [LOG.md](LOG.md)

---

## Cómo leer la columna `Estado`

| Estado | ¿Cuenta como disponible para planear? | Significado corto |
|---|---|---|
| `Disponible` | ✅ Sí | Está en el cajón, libre |
| `Reservado` | ⚠️ Sí, pero apartado | Apartado para un proyecto, aún no montado |
| `En uso` | ❌ No | Montado y funcionando en un proyecto |
| `A reparar` | 🔜 Sí (a futuro) | Está dañado **pero** en el LOG dijiste que lo repararías |
| `A reponer` | 🔜 Sí (a futuro) | Está dañado **pero** en el LOG dijiste que comprarías otro |
| `Por comprar` | 🔜 Sí (a futuro) | Identificado como necesario, aún no comprado |
| `En tránsito` | 🔜 Sí (a futuro) | Ya comprado, viene en camino |
| `Prestado` | ❌ No | Se lo prestaste a alguien |
| `Dañado` | ❌ **No — ignorar** | Solo existe para tener el registro histórico |
| `Baja` | ❌ No | Se tiró, se perdió o se regaló. Registro histórico |

> **Regla dura:** un componente en estado `Dañado` **no existe** para efectos de
> planeación. Solo pasa a `A reparar` o `A reponer` si tú lo declaras en el
> [LOG.md](LOG.md). Yo nunca cambio ese estado por mi cuenta.

---

## Componentes

| ID | Componente | Categoría | Estado | Cant. | Lógica | Proyecto | Specs clave | Notas |
|---|---|---|---|---|---|---|---|---|
| C-001 | ESP32 genérico — **USB-C** | Microcontrolador | **En uso** | 1 | 3.3 V | tamagotchi | **ESP32-D0WD-V3 rev3.1**, 2×240 MHz, 4 MB flash, WiFi+BT, USB-Serie **CP2102** | ✅ Funcionando. MAC `84:1F:E8:2C:3E:C4` · `/dev/cu.usbserial-0001` |
| C-002 | ESP32 genérico — **USB-C** | Microcontrolador | Disponible | 1 | 3.3 V | — | Idéntica a C-001 (sin confirmar) | Refacción. **Sin identificar**: su MAC aún no se conoce. El 2026-11-25 se confundió con la C-011 ([LOG](LOG.md)) |
| C-003 | ESP32 **DEVKIT v1** — micro/mini-USB | Microcontrolador | Disponible | 1 | 3.3 V | — | ESP32-WROOM-32, WiFi 2.4 GHz + BT | ⚠️ Sin LED al conectarla. **Sin probar con un cable bueno** — no asumir que está dañada |
| C-004 | TFT IPS 1.3" **GMT130-v1.0** | Pantalla | Disponible | **4** | 3.3 V | tamagotchi | **240×240 px, color, IPS**, ✅ **ST7789**, SPI, 7 pines sin `CS` | ✅ Especificaciones confirmadas |
| C-005 | Módulo **joystick analógico** 2 ejes (tipo KY-023) | Entrada | Disponible | 1 | **3.3 V** ⚠️ | tamagotchi | 2 potenciómetros 10 kΩ + pulsador. Pines `GND`, `+5V`, `VRx`, `VRy`, `SW` | ⚠️ **Alimentar a 3.3 V, NO a 5 V** — ver [LOG](LOG.md) |
| C-006 | **Batería LiPo** 1 celda, bolsa | Alimentación | Disponible | 1 | **3,0–4,2 V** ⚠️ | tamagotchi | `3.7V 1000 mAh 3.7 Wh`, lote 202407. Una celda | ⚠️ **No se puede apagar**: siempre tiene tensión. Ya soldada a `B+`/`B-` de C-007 |
| C-007 | Cargador **TP4056** con protección | Alimentación | Disponible | 1 | **= la batería** ⚠️ | tamagotchi | Pastillas `B+ B- OUT+ OUT-`. Carga por USB a 4,2 V. Lleva protección (sobredescarga y corto) | ⚠️ **`OUT+` NO da 5 V**, da 3,0–4,2 V. `OUT+`/`OUT-` aún sin soldar |
| C-008 | **Interruptor deslizante** SPDT tipo SS12D00 | Entrada | Disponible | 1 | — | tamagotchi | 3 patas; **la del medio es la común**. Suelen ser de 0,5 A | Cable rojo ya soldado a la pata del medio. ⚠️ 0,5 A va justo contra los 500 mA de pico del ESP32 |
| C-009 | **Elevador** MT3608 (step-up ajustable) | Alimentación | **Por comprar** | 1 | ent. 2–24 V · sal. hasta 28 V | tamagotchi | Eleva los 3,0–4,2 V de la batería a 5 V para entrar por `VIN` | ⚠️ **Ajustable: NO viene a 5 V.** Calibrar con polímetro y sin carga ANTES de conectarlo. [ADR-0014](../proyectos/tamagotchi/1-decisiones/ADR-0014-alimentacion-por-bateria.md) |
| C-010 | Módulo **NFC PN532** Kit V3 (Fydun) | Comunicación | **En uso** | 1 | 3.3 V (alimentado a `3V3`) | nfc-pn532 | Chip NXP **PN532**, 13,56 MHz. Interfaz por DIP: HSU / **I2C** / SPI. I2C en **`0x24`** | ✅ Contesta por I2C (2026-11-25). DIP en I2C. ⚠️ Mover el DIP **solo sin corriente** |
| C-011 | ESP32 genérico — ESP-32S NodeMCU v1.1 | Microcontrolador | **En uso** | 1 | 3.3 V | nfc-pn532 | **ESP32-D0WD-V3 rev3.1**, 2×240 MHz, 4 MB flash, WiFi+BT, USB-Serie **CP2102** | ✅ Identificada 2026-11-25. MAC `E0:8C:FE:30:C9:1C` · ⚠️ **Mismo puerto que C-001** (`/dev/cu.usbserial-0001`): distinguirlas por la MAC |
| C-012 | **Tarjeta NFC** (del kit PN532) | Comunicación | **En uso** | **2** | — (pasiva) | nfc-pn532 | 13,56 MHz, sin pila. Tipo de chip **sin confirmar** | Objeto de prueba. Cada una debe dar un UID distinto |
| C-013 | **Llavero NFC** (del kit PN532) | Comunicación | **En uso** | 1 | — (pasivo) | nfc-pn532 | 13,56 MHz, sin pila. Tipo de chip **sin confirmar** | Objeto de prueba |

**Resumen:** 4 microcontroladores (C-001 tamagotchi · C-011 nfc-pn532 · C-002 y C-003 libres, sin identificar), 4 pantallas, 1 lector NFC + 3 etiquetas NFC. ✅ Sin bloqueantes de material.

---

## Lista de compras derivada

| ID | Componente | ¿Para qué proyecto? | Prioridad | Notas |
|---|---|---|---|---|
| — | ~~Cable USB-A → USB-C~~ | tamagotchi | ✅ Resuelto | Ya se encontró un cable C-a-C que sí pasa datos. **Márcalo físicamente** |
| — | Protoboard + jumpers hembra-hembra | tamagotchi | 🟡 Media | Para conectar la pantalla sin soldar. Confirmar si ya tienes |

---

## Categorías válidas

Usa **solo** estas categorías para que las búsquedas y los filtros funcionen:

| Categoría | Ejemplos |
|---|---|
| `Microcontrolador` | ESP32, ESP8266, Arduino Nano, Raspberry Pi Pico |
| `Pantalla` | OLED SSD1306, TFT ST7789, e-ink, LCD 16x2 |
| `Sensor` | DHT22 (temp/humedad), LDR (luz), HC-SR04 (distancia), MPU6050 |
| `Actuador` | Servo SG90, motor DC, buzzer, relé, vibrador |
| `Entrada` | Botón, encoder rotatorio, joystick, potenciómetro, touch |
| `Alimentación` | Batería LiPo, cargador TP4056, regulador AMS1117, power bank |
| `Comunicación` | Módulo LoRa, RFID RC522, nRF24L01, transceptor |
| `Pasivo` | Resistencias, capacitores, inductores, diodos, cristales |
| `Semiconductor` | Transistores, MOSFETs, ICs, optoacopladores |
| `Prototipado` | Protoboard, jumpers, headers, PCB perforada, cable |
| `Mecánico` | Carcasa, tornillería, filamento 3D, imanes, standoffs |
| `Herramienta` | Multímetro, cautín, fuente de poder, pinzas, osciloscopio |
| `Consumible` | Estaño, flux, termorretráctil, cinta Kapton |

---

## Sobre la columna `Lógica` (importante si estás aprendiendo)

Es el **voltaje al que trabajan los pines de datos** del componente. Es la causa
número uno de que la gente queme placas.

- El **ESP32 es 3.3 V**. Sus pines *no toleran 5 V*.
- Si un componente dice `5 V` en esta columna y lo conectas directo al ESP32,
  probablemente lo dañes → y acabará en estado `Dañado` en esta tabla.
- Cuando quieras mezclar 3.3 V y 5 V necesitas un **level shifter** (conversor de
  nivel lógico). Yo te aviso cada vez que esto aplique.

`5 V (tolera 3.3)` significa que el componente se *alimenta* con 5 V pero entiende
señales de 3.3 V — eso sí es seguro.

> 🎉 **Buena noticia con tu material actual:** el ESP32 y la GMT130 trabajan los
> dos a 3.3 V. Se conectan directo, sin conversor de nivel y sin riesgo de
> quemar nada por voltaje.
