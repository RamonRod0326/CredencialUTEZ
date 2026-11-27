# Credencial UTEZ

App Android para usar el celular como credencial de estudiante. El acceso se valida con biometría y NFC contra un lector ESP32 + PN532, y cada uso se guarda con fecha, hora y ubicación.

Hecha con Kotlin, Jetpack Compose, Retrofit y DataStore. minSdk 33. Los datos viven en el backend (`../backend`).

## Estructura

- `domain/`: modelos, validaciones, interfaces de los repositorios y el protocolo NFC.
- `data/`: cliente de la API (Retrofit), sesión (DataStore), fotos, ubicación y manejo de la credencial.
- `nfc/`: servicio HCE que contesta al lector.
- `ui/`: pantallas en Compose.
- `docs/`: protocolo con el lector.

El código del lector está en el repositorio `ESP32` (`proyectos/nfc-pn532`).

## Funciones

- Registro con matrícula (`20253DS000`–`20253DS999`, validada por el servidor), foto, carrera, gustos, intereses y descripción.
- Inicio de sesión con correo o matrícula.
- Credencial digital que se activa 60 s después de verificar la huella o el PIN.
- Lectura NFC firmada con HMAC-SHA256.
- Historial de accesos con ubicación.
- Perfil editable; se puede quitar la foto y la descripción.

## Pruebas

```
gradlew testDebugUnitTest
```

En la versión debug, la pestaña Credencial tiene "Simular lectura" para probar sin el lector.
