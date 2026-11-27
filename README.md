# Credencial Digital UTEZ

Proyecto Integrador Móvil.

## Integrantes

| Nombre | Matrícula |
|---|---|
| Ramón Rodríguez Memije | 20253DS086 |
| Gerardo García Rodríguez | 20253DS066 |
| Angel Said Brito Ibarra | 20253DS090 |

## Descripción

App Android que sustituye la credencial física de estudiante. El alumno se registra con su matrícula y su foto, y para usar la credencial confirma su identidad con la biometría del teléfono (huella o PIN). Después acerca el celular a un lector NFC (ESP32 + PN532), que valida una firma criptográfica y responde si el acceso se permite. Cada acceso queda guardado con fecha, hora y ubicación GPS.

Sensores que usa: **NFC** (emulación de tarjeta, HCE), **biometría** y **GPS**.

## Estructura

```
CredencialUTEZ/
├── CredencialNFC/   App Android (Kotlin, Jetpack Compose, Retrofit)
├── backend/         API REST (Spring Boot 3.5, Java 21, JPA, H2/MySQL)
└── ESP32/           Lector NFC (ESP32 + PN532, PlatformIO)
```

## Cómo ejecutarlo

### 1. Backend

Requisitos: Java 21 o superior y Maven.

```
cd backend
mvn spring-boot:run
```

Queda escuchando en `http://localhost:8080`. Por defecto usa una base H2 en el archivo `backend/data/`, así que no hay que instalar nada. Al arrancar carga las matrículas permitidas (`20253DS000` a `20253DS999`) y crea las cuentas del equipo:

| Integrante | Matrícula | Correo | Contraseña |
|---|---|---|---|
| Ramón Rodríguez Memije | 20253DS086 | 20253ds086@utez.edu.mx | proyecto123 |
| Gerardo García Rodríguez | 20253DS066 | 20253ds066@utez.edu.mx | proyecto123 |
| Angel Said Brito Ibarra | 20253DS090 | 20253ds090@utez.edu.mx | proyecto123 |

Se puede iniciar sesión con la matrícula o con el correo. Cualquier otra matrícula del listado se puede registrar desde la app.

Para usar MySQL:

```
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

(ajusta usuario y contraseña en `src/main/resources/application-mysql.properties`).

Consola de la base H2: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/credencial`, usuario `sa`, sin contraseña).

### 2. App

1. Abrir `CredencialNFC/` en Android Studio.
2. Indicar la dirección del backend en `CredencialNFC/local.properties`:
   - Emulador: no hace falta, usa `http://10.0.2.2:8080/`.
   - Celular físico (misma red WiFi que la laptop): `api.url=http://<IP de la laptop>:8080/`
   - La clave de la credencial (pídela al equipo; no está en el repositorio): `credencial.clave=<64 caracteres hexadecimales>`
3. Ejecutar con Run ▶.

Para el celular físico, el firewall de Windows debe permitir conexiones entrantes al puerto 8080.

### 3. Lector

Ver `ESP32/proyectos/nfc-pn532/README.md`. La clave del lector (`secrets.h`) debe ser la misma que `credencial.clave` en `CredencialNFC/local.properties`.

## Endpoints

Base: `http://<servidor>:8080/api`. Las rutas marcadas con (auth) necesitan el encabezado `Authorization: Bearer <token>`, que se obtiene al registrarse o iniciar sesión.

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/salud` | Comprueba que el servidor está activo | 200 |
| POST | `/auth/verificar` | Revisa que la matrícula esté permitida y que matrícula y correo estén libres | 204, 400, 403, 409 |
| POST | `/auth/registro` | Crea la cuenta y devuelve token + perfil | 201, 400, 403, 409 |
| POST | `/auth/login` | Inicia sesión con correo o matrícula | 200, 401 |
| POST | `/auth/logout` (auth) | Cierra la sesión (invalida el token) | 204 |
| GET | `/estudiantes/yo` (auth) | Perfil del estudiante | 200, 401 |
| PUT | `/estudiantes/yo` (auth) | Actualiza nombre, carrera, gustos, intereses y descripción | 200, 400 |
| PUT | `/estudiantes/yo/foto` (auth) | Sube la foto (`multipart/form-data`, campo `foto`) | 200, 400, 413 |
| DELETE | `/estudiantes/yo/foto` (auth) | Elimina la foto | 200 |
| DELETE | `/estudiantes/yo` (auth) | Elimina la cuenta | 204 |
| GET | `/fotos/{nombre}` | Descarga una foto de perfil | 200, 404 |
| GET | `/accesos` (auth) | Historial de accesos del estudiante | 200 |
| POST | `/accesos` (auth) | Registra un acceso | 201, 400 |
| DELETE | `/accesos/{id}` (auth) | Elimina un registro del historial | 204, 404 |

Los errores siempre regresan `{"estado": <código>, "mensaje": "<texto>"}`.

### Ejemplos

Registro:

```json
POST /api/auth/registro
{
  "nombreCompleto": "Ana López Ruiz",
  "matricula": "20253DS007",
  "correo": "ana@utez.edu.mx",
  "password": "clave1234",
  "carrera": "TSU en Desarrollo de Software Multiplataforma",
  "gustos": ["Música"],
  "intereses": ["Desarrollo móvil"],
  "descripcion": ""
}
```

Registrar acceso:

```json
POST /api/accesos
{
  "fechaHora": 1795749700229,
  "latitud": 18.8505,
  "longitud": -99.2216,
  "precisionMetros": 12.5,
  "lectorId": "P01",
  "lectorUbicacion": "Acceso principal UTEZ",
  "resultado": "PERMITIDO"
}
```
