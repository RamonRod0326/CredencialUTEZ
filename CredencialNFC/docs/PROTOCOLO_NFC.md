# Protocolo NFC app - lector

El celular emula una tarjeta (HCE) y el lector PN532 le manda comandos APDU (ISO 7816-4 sobre ISO 14443-4).

No se usa el UID para identificar al estudiante porque Android genera uno aleatorio en cada lectura.

## AID

```
F0 55 54 45 5A 43 52 45 44
```

Registrado en `app/src/main/res/xml/servicio_apdu.xml`.

## Comandos

| Paso | Lector -> celular | Celular -> lector |
|---|---|---|
| 1 | `00 A4 04 00 09 F05554455A43524544 00` (SELECT) | `90 00` si está activa, `69 85` si no |
| 2 | `80 10 00 00 08 <nonce> 00` | `<matrícula 10 B> <firma 16 B> 90 00` |
| 3 | `80 20 <01/00> 00 <Lc> <"ID;Nombre">` | `90 00` |

- El nonce son 8 bytes aleatorios que genera el lector en cada lectura.
- firma = primeros 16 bytes de `HMAC-SHA256(clave, nonce || matrícula)`.
- En el paso 3, `01` es permitido y `00` denegado. Con eso la app guarda el registro en el historial.

## Códigos

| Código | Significado |
|---|---|
| `90 00` | OK |
| `69 85` | Credencial no activada |
| `6A 82` | AID desconocido |
| `67 00` | Longitud incorrecta |
| `6D 00` | Instrucción no soportada |
| `6E 00` | Clase no soportada |

## Activación

La credencial solo contesta durante 60 s después de verificar la identidad con la biometría del teléfono. Al terminar una lectura se desactiva.

## Clave

La app y el lector usan la misma clave de 32 bytes. En la app va en `local.properties` (`credencial.clave=...`) y en el lector en `secrets.h`; ninguno de los dos archivos se sube al repositorio. Con backend, lo ideal es que cada estudiante tenga su propia clave.

## Vector de prueba

- nonce: `01 02 03 04 05 06 07 08`
- matrícula: `20253DS007`
- firma: `E94D03DA7FBE6442643A46CE1C38C373`

Lo revisa `ProtocoloCredencialTest`.
