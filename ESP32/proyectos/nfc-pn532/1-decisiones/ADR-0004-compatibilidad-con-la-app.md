# ADR-0004 — Adoptar el protocolo de la app, no su especificación de hardware

- **Estado:** `Aceptado`
- **Fecha:** 2026-11-25
- **Reemplaza a:** —
- **Relacionado con:** ADR-0002 (librería) · ADR-0003 (SPI) · requerimientos v0.3

---

## Contexto

El objetivo real del proyecto apareció a mitad de la sesión 1: un **lector de
credenciales universitarias**. El estudiante acerca su celular Android; una app
ya terminada emula una tarjeta NFC (HCE) y entrega su **matrícula firmada**.

El usuario compartió una especificación escrita por otro agente de IA. Mezcla
dos cosas de naturaleza muy distinta:

1. **El protocolo de la app** (AID, APDUs, firma HMAC). Lo impone la app, que
   **no se puede cambiar**.
2. **Decisiones de hardware y de herramientas** (I2C con SCL en P19, Arduino IDE
   con core 4.x, LEDs externos en GPIO25/26, buzzer en GPIO27, controlador
   propio en vez de Adafruit, clave en `config.h`).

El usuario aclaró que **lo segundo no es obligatorio**: lo único que importa es
ser compatible con la app y leer solo sus credenciales.

## Qué sabíamos y qué no

**Lo que sabíamos con certeza:**
- El vector de prueba de la especificación cuadra: HMAC-SHA256(clave,
  nonce ‖ matrícula), primeros 16 bytes (comprobado en Python el 2026-11-25).
- SPI + Adafruit funciona aquí; I2C + Adafruit **no** (pruebas 03–06). La propia
  especificación lo reconocía como "problema conocido" en el core 4.x; aquí
  también pasa en el core 2.0.17.
- En Adafruit 1.3.4, `inDataExchange()` usa el n.º de objetivo que **solo**
  guarda `inListPassiveTarget()`. Detectar con `readPassiveTargetID()` y luego
  llamar a `inListPassiveTarget()` **activaría el celular dos veces**: justo lo
  que la especificación advertía que lo deja en pausa.
- **Probado con la app real** (2026-11-25): SELECT `90 00`, token de 28 bytes,
  firma ✓, RESULTADO `90 00`, acceso permitido. Sin huella: `69 85`. Tarjeta
  bancaria: `6A 82`. Tarjeta MIFARE: se ignora.

**Lo que asumimos sin verificar:**
- Que el formato de matrícula general (`#####AA###`) cubre todas las válidas.
- Que la app no exige nada que la especificación no diga (p. ej. tiempos).

**Lo que quedó pendiente de averiguar:**
- Qué tan impredecible es el nonce **sin WiFi**. Según la documentación de
  Espressif, `esp_fill_random()` solo es verdaderamente aleatorio con la radio
  encendida o con `bootloader_random_enable()`. Para un lector de prácticas
  basta; antes de un uso real, hay que resolverlo.

## Opciones consideradas

### Opción A — Seguir la especificación al pie de la letra

- **A favor:** coincide con lo que el otro agente ya diseñó.
- **En contra:** obliga a volver a I2C, que aquí se cuelga. Obliga a escribir un
  controlador propio sin probar, a comprar LEDs y un buzzer que no están en el
  inventario, y a dejar las versiones clavadas de PlatformIO. Además pone la
  clave en un archivo que sí se versiona.
- **Costo:** alto, y con riesgo de volver al cuelgue.

### Opción B — Adoptar solo lo que impone la app

- **A favor:** se mantiene lo que **ya está medido** que funciona.
- **En contra:** el código no se parece al que imaginaba la especificación.
- **Costo:** bajo.

## Decisión

**Elegimos la opción B.**

- **Se adopta de la especificación (la app lo exige):** AID `F0 55 54 45 5A 43
  52 45 44`, la secuencia SELECT → TOKEN → RESULTADO, HMAC-SHA256 truncado a 16
  bytes, comparación en tiempo constante, nonce de 8 bytes por lectura, la
  autoprueba con el vector conocido y el texto `"P01;Acceso principal UTEZ"`.
- **Se adopta porque es buena idea, aunque no lo exija la app:**
  - HMAC hecho a mano (RFC 2104), para que funcione igual en cualquier core.
  - **Una sola activación por lectura**: se detecta con `inListPassiveTarget()`.
  - Ajustes en `config.h`.
- **Se mantiene distinto:**
  - SPI (ADR-0003), PlatformIO con versiones clavadas y la librería Adafruit.
  - El LED de la placa: parpadeo **largo** = permitido, **corto** = lo demás.
  - **La clave va en `src/secrets.h`, ignorado por Git**, nunca en `config.h`.
- **Se relaja:** la matrícula se valida con la forma general `#####AA###` en
  vez del prefijo fijo `20253DS`. La firma ya garantiza que viene de la app, y
  un prefijo fijo rechazaría a otras generaciones.
- **Se deja fuera:** la lista de matrículas bloqueadas (el usuario no la pidió).

Además, `inListPassiveTarget()` imprimía mensajes de depuración sin control
(un fallo de la librería), así que la librería se usa como **copia local con
dos parches** en `firmware/lib/` (ver `firmware/lib/LEEME.md`).

## Consecuencias

**Lo que esto nos habilita:**
- Un lector funcionando con la app real, en el mismo día.

**Lo que esto nos cierra o nos cuesta:**
- La librería ya no se baja del registro: si se actualiza, hay que revisar a
  mano si los parches siguen haciendo falta.

**Lo que hay que vigilar:**
- Si la app cambia su protocolo, este lector deja de entenderla. El protocolo
  vive en `leerCredencial()` y en `config.h`.
- Si alguna matrícula válida sale con "formato ✗", ajustar `FORMATO_MATRICULA`.
- La clave apareció en texto plano en una conversación. Si es la de producción,
  conviene cambiarla en la app y en `secrets.h`.

## Cómo revertir

Barato. El protocolo es independiente del bus: pasar a I2C o a LEDs externos
solo toca `config.h` y la línea del constructor.
