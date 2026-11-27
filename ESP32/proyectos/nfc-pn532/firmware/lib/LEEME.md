# Librerías con copia local

## `Adafruit_PN532/` — versión 1.3.4 con dos parches de depuración

**Origen:** `adafruit/Adafruit PN532@1.3.4` del registro de PlatformIO,
descargada el 2026-11-25. Licencia BSD (ver `license.txt`), que permite
modificarla y redistribuirla.

**Qué se cambió:** en `Adafruit_PN532.cpp`, función `inListPassiveTarget()`,
dos pares de líneas de depuración estaban **fuera** del `#ifdef PN532DEBUG`
que debía apagarlas, y se metieron dentro:

1. `"Number of tags inlisted:"` + el número: salía en cada búsqueda **vacía**.
2. `"Tag number: "` + el número: salía en cada búsqueda **con algo encima**.

Son los únicos cambios (marcados con `PARCHE nfc-pn532`). Se revisó el
archivo entero buscando más mensajes sueltos: los que quedan son las
funciones de imprimir (`PrintHex`, que existen para eso) y dos avisos de
error de `inDataExchange` que solo salen ante respuestas raras y conviene
conservar.

**Por qué:** el lector usa `inListPassiveTarget()` para detectar porque es la
única función que activa el objetivo **una sola vez** y deja listo el
intercambio de APDUs (ver ADR-0004). Sin el parche, el monitor serie se llena
con esas líneas unas 20 veces por segundo.

**Si algún día se actualiza la librería:** comprobar si el fallo sigue en la
versión nueva. Si ya está corregido, borrar esta copia y volver a ponerla en
`lib_deps`. Ojo también con `pn532_packetbuffer` (ver ADR-0002).
