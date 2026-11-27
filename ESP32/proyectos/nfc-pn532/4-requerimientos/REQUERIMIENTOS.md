# Requerimientos — nfc-pn532

- **Versión:** 0.3
- **Fecha:** 2026-11-25
- **Estado:** `Estable`. Confirmado con el usuario: lo único obligatorio es ser
  compatible con la app y leer solo sus credenciales (ADR-0004).

---

## El proyecto en una frase

Un lector de credenciales universitarias: el estudiante acerca su celular con
la app, y el lector lee su matrícula, comprueba que es auténtica y dice si pasa.

## Para qué lo quiero

Para que funcione con una **app Android ya terminada** que emula una tarjeta
NFC. El protocolo lo impone la app y **no se puede cambiar**. De paso, es un
proyecto de aprendizaje: entender NFC, APDUs y firmas criptográficas.

---

## Requerimientos funcionales

| ID | Requerimiento | Prioridad | Criterio de aceptación | Estado |
|---|---|---|---|---|
| RF-01 | Al arrancar, encuentra el PN532 y lo dice | Debe | Monitor: `PN532 encontrado · firmware 1.6` | ✅ 2026-11-25 |
| RF-02 | Lee la matrícula de la app y comprueba su firma | Debe | SELECT `90 00` → token de 28 bytes → firma ✓ → `ACCESO PERMITIDO · <matrícula>` | ✅ 2026-11-25, con la app real |
| RF-03 | Le dice el resultado a la app | Debe | El RESULTADO recibe `90 00` | ✅ 2026-11-25 |
| RF-04 | Distingue la credencial sin activar (falta la huella) | Debe | SELECT `69 85` → aviso de "no activada" | ✅ 2026-11-25 |
| RF-05 | Ignora todo lo que no sea la app | Debe | Tarjetas, llaveros, otros celulares y tarjetas bancarias: "no es una credencial", sin intentar leerlas | ✅ 2026-11-25 (MIFARE ignorada; bancaria `6A 82`) |
| RF-06 | Una sola lectura por acercamiento | Debe | Dejado quieto encima, se procesa **una** vez | ✅ 2026-11-25 (tarjeta: 79 redetecciones → 1 lectura; celular: 0) |
| RF-07 | LED: parpadeo largo = permitido, corto = lo demás | Debería | Visible a simple vista | 👀 Falta confirmarlo mirando |
| RF-08 | Si el PN532 deja de responder, lo dice | Debería | Con el PN532 desconectado, el monitor avisa en vez de colgarse | Implementado, **sin probar** |
| RF-09 | Autoprueba criptográfica al arrancar | Debería | `Autoprueba criptográfica: OK` (vector de la especificación + firma alterada rechazada) | ✅ 2026-11-25 |

## Requerimientos no funcionales

| ID | Requerimiento | Valor objetivo | Cómo se mide | Estado |
|---|---|---|---|---|
| RNF-01 | Recompilable en un año | Versiones clavadas (`platformio.ini`, `requirements.txt`, librería en `lib/`) | Recompilar desde cero | ✅ |
| RNF-02 | La clave nunca entra al repositorio | Solo en `src/secrets.h`, ignorado por Git | `git check-ignore` | ✅ |
| RNF-03 | Lectura rápida | Menos de 1,5 s desde que se acerca hasta el veredicto | Hora en el monitor | ✅ ~1 s |

---

## Fuera de alcance

- ❌ Lista de matrículas bloqueadas (la especificación la traía; no hace falta).
- ❌ LEDs externos y buzzer: se usa el LED de la placa.
- ❌ Leer el contenido de tarjetas y llaveros: eso vive en la **prueba 07**.
- ❌ WiFi, pantalla y batería.
- ❌ Usar la placa C-001: es el Tamagotchi.

## Supuestos

| # | Supuesto | ¿Verificado? | Si es falso, ¿qué se rompe? |
|---|---|---|---|
| S-01 | El DIP del PN532 está en SPI (1 OFF, 2 ON) | ✅ 2026-11-25 | El PN532 no contesta (todo `FF`) |
| S-02 | El protocolo de la especificación es el que usa la app de verdad | ✅ 2026-11-25 (lectura completa con la app) | — |
| S-03 | La forma `#####AA###` cubre todas las matrículas válidas | No | Alguien válido saldría "formato ✗" → ajustar `config.h` |
| S-04 | El celular se presenta con UID aleatorio | ✅ (`08 …`, distinto cada vez) | — |
| S-05 | El nonce es suficientemente impredecible sin WiFi | **No** (ver ADR-0004) | Se debilita la protección contra respuestas pregrabadas |

## Riesgos conocidos

| Riesgo | Probabilidad | Impacto | Plan si ocurre |
|---|---|---|---|
| Cargar firmware de este proyecto en la C-001 (mismo puerto) | Media | Alto | `proteger-placa.py` comprueba la MAC antes de cada carga |
| La clave se filtra (apareció en una conversación) | Media | Alto | Cambiarla en la app y en `secrets.h` antes de un uso real |
| Actualizar la librería y perder los parches | Baja | Bajo | `firmware/lib/LEEME.md` |

---

## Historial de cambios

| Fecha | Cambio | Motivo |
|---|---|---|
| 2026-11-25 | v0.1: banco de pruebas (borrador) | Arranque del proyecto |
| 2026-11-25 | v0.2: las 3 etiquetas del kit y el celular | El usuario confirmó el contenido del kit |
| 2026-11-25 | **v0.3: lector de credenciales de la app** | Apareció el objetivo real; RF reescritos y verificados con la app (ADR-0004) |
