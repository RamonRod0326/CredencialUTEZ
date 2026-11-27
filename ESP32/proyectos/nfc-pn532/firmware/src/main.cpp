// ============================================================
//  nfc-pn532 — Lector de credenciales de la app
// ============================================================
//  El estudiante acerca su celular. La app (que emula una
//  tarjeta NFC) entrega su matrícula firmada, y el lector:
//
//   1. SELECT    — "¿está ahí la app de credenciales?"
//   2. TOKEN     — le manda un número al azar (nonce) y recibe
//                  matrícula + firma de (nonce + matrícula)
//   3. VALIDA    — repite la firma con la clave compartida; si
//                  coincide, la matrícula viene de la app de verdad
//   4. RESULTADO — le dice a la app "permitido" o "denegado"
//
//  Todo lo demás (tarjetas, llaveros, otros celulares) se
//  detecta pero se ignora: no es una credencial.
//
//  El nonce cambia en cada lectura. Así, grabar una respuesta y
//  repetirla después no sirve: la firma no cuadraría.
//
//  Decisiones: ADR-0003 (SPI) · ADR-0002 (librería) · ADR-0004 (app)
//  Ajustes:    config.h      · Clave: secrets.h (ignorado por Git)
//  El volcado de tarjetas que hacía antes vive en pruebas/07.
// ============================================================

#include <Arduino.h>
#include <SPI.h>
#include <esp_system.h>  // esp_fill_random
#include <Adafruit_PN532.h>

#include "config.h"
#include "hmac_sha256.h"

#if __has_include("secrets.h")
#include "secrets.h"
#else
#error "Falta src/secrets.h con la clave. Copia firmware/secrets.h.ejemplo a firmware/src/secrets.h"
#endif

#ifndef FW_VERSION
#define FW_VERSION "desconocido"
#endif

Adafruit_PN532 nfc(PIN_SS, &SPI);

// Último mensaje que devolvió el PN532 (búfer interno de la librería).
// De ahí se sacan tipo y UID tras activar el objetivo (ADR-0002).
// ⚠️ Depende de la versión 1.3.4 clavada en platformio.ini.
extern byte pn532_packetbuffer[];

struct Objetivo {
  uint8_t  uid[10];
  uint8_t  largo;
  uint16_t atqa;
  uint8_t  sak;  // el byte que dice el tipo
};

enum Resultado { PERMITIDO, DENEGADO, NO_ACTIVADA, NO_ES_APP, NO_ES_CELULAR, SE_CORTO, NUM_RESULTADOS };
const char *NOMBRES_RESULTADO[NUM_RESULTADOS] = {
  "permitidos", "denegados", "no activadas", "no eran la app", "no eran celulares", "cortadas"
};

uint32_t      versionPN532 = 0;
int           contador = 0;
int           porResultado[NUM_RESULTADOS] = {0};
char          ultimaPermitida[11] = "";
bool          hayAlgo = false;
unsigned long llegoEn = 0, ultimaVezVisto = 0, ultimoChequeo = 0;
int           redetecciones = 0;  // veces que se volvió a ver lo que seguía encima


// ================================================================
//  Utilidades
// ================================================================

void imprimirHex(const uint8_t *d, int n) {
  for (int i = 0; i < n; i++) Serial.printf("%02X ", d[i]);
}

const char *nombreDelTipo(uint8_t sak) {
  switch (sak) {
    case 0x08: case 0x88: return "MIFARE Classic 1K";
    case 0x18: return "MIFARE Classic 4K";
    case 0x00: return "Ultralight/NTAG";
    case 0x20: return "celular o tarjeta inteligente";
    case 0x28: return "tarjeta inteligente (con MIFARE)";
    default:   return "tipo desconocido";
  }
}


// ================================================================
//  Firma (HMAC-SHA256) y autoprueba
// ================================================================

// ¿La firma corresponde a (nonce + matrícula) con nuestra clave?
// Solo se comparan los primeros 16 bytes: es lo que manda la app.
bool firmaCorrecta(const uint8_t nonce[8], const uint8_t matricula[10], const uint8_t firma[16]) {
  uint8_t mensaje[18];
  memcpy(mensaje, nonce, 8);
  memcpy(mensaje + 8, matricula, 10);
  uint8_t calculada[32];
  if (!hmacSha256(CLAVE_APP, sizeof(CLAVE_APP), mensaje, sizeof(mensaje), calculada)) return false;
  bool iguales = igualesEnTiempoConstante(calculada, firma, 16);
  memset(calculada, 0, sizeof(calculada));
  return iguales;
}

// Al arrancar: la firma conocida debe ACEPTARSE, y la misma con un
// solo bit cambiado debe RECHAZARSE. Lo segundo prueba que la
// comparación no dice "sí" a todo.
bool autoprueba() {
  const uint8_t nonce[8] = {1, 2, 3, 4, 5, 6, 7, 8};
  const uint8_t matricula[10] = {'2', '0', '2', '5', '3', 'D', 'S', '0', '0', '7'};
  bool acepta = firmaCorrecta(nonce, matricula, FIRMA_DE_PRUEBA);
  uint8_t alterada[16];
  memcpy(alterada, FIRMA_DE_PRUEBA, 16);
  alterada[15] ^= 0x01;
  bool rechaza = !firmaCorrecta(nonce, matricula, alterada);
  return acepta && rechaza;
}

bool claveVacia() {
  for (uint8_t b : CLAVE_APP)
    if (b != 0) return false;
  return true;
}

// '#' = dígito, 'A' = letra mayúscula (config.h)
bool formatoValido(const uint8_t m[10]) {
  if (strlen(FORMATO_MATRICULA) != 10) return false;
  for (int i = 0; i < 10; i++) {
    char f = FORMATO_MATRICULA[i];
    if (f == '#' && !(m[i] >= '0' && m[i] <= '9')) return false;
    if (f == 'A' && !(m[i] >= 'A' && m[i] <= 'Z')) return false;
  }
  return true;
}


// ================================================================
//  PN532
// ================================================================

void conectarPN532() {
  while (true) {
    nfc.begin();
    uint32_t v = nfc.getFirmwareVersion();
    if (v != 0) {
      versionPN532 = v;
      Serial.printf("PN532 encontrado · chip PN5%02X · firmware %d.%d\n",
                    (v >> 24) & 0xFF, (v >> 16) & 0xFF, (v >> 8) & 0xFF);
      nfc.setPassiveActivationRetries(REINTENTOS_BUSQUEDA);
      return;
    }
    Serial.println("✗ No encuentro el PN532. Revisa los cables (SCK=P18, MISO=P19, "
                   "MOSI=P23, SS=P5) y que el DIP esté en SPI (1 OFF, 2 ON). Reintento en 2 s...");
    delay(2000);
  }
}

// Detecta Y ACTIVA lo que haya encima, con UN SOLO comando
// (InListPassiveTarget). Activarlo dos veces deja al celular en pausa
// y ya no contesta (lo advertía la especificación; ADR-0004).
bool buscar(Objetivo &o) {
  if (!nfc.inListPassiveTarget()) return false;
  // [7] cuántos · [8] n.º de objetivo · [9..10] ATQA · [11] SAK · [12] largo UID · [13..] UID
  o.atqa  = (pn532_packetbuffer[9] << 8) | pn532_packetbuffer[10];
  o.sak   = pn532_packetbuffer[11];
  o.largo = min<uint8_t>(pn532_packetbuffer[12], sizeof(o.uid));
  memcpy(o.uid, pn532_packetbuffer + 13, o.largo);
  return true;
}

// Manda un APDU (un "mensaje" de tarjeta inteligente) y recoge la
// respuesta. Sus 2 últimos bytes son el código de estado: 90 00 = bien.
bool apdu(const uint8_t *envio, uint8_t largoEnvio, uint8_t *resp, uint8_t &largoResp) {
  if (!nfc.inDataExchange((uint8_t *)envio, largoEnvio, resp, &largoResp)) return false;
  return largoResp >= 2;
}

uint16_t estadoDe(const uint8_t *resp, uint8_t largo) {
  return (resp[largo - 2] << 8) | resp[largo - 1];
}


// ================================================================
//  El protocolo de la app
// ================================================================

bool enviarResultado(bool permitido) {
  const size_t n = min<size_t>(strlen(LECTOR_TEXTO), 50);
  uint8_t envio[5 + 50] = {0x80, 0x20, (uint8_t)(permitido ? 0x01 : 0x00), 0x00, (uint8_t)n};
  memcpy(envio + 5, LECTOR_TEXTO, n);
  uint8_t resp[64], largo = sizeof(resp);
  return apdu(envio, 5 + n, resp, largo) && estadoDe(resp, largo) == 0x9000;
}

Resultado leerCredencial(char matriculaTexto[11]) {
  uint8_t resp[64], largo;

  // 1. SELECT por AID: despierta a la app dentro del celular
  uint8_t select[5 + sizeof(AID_APP) + 1] = {0x00, 0xA4, 0x04, 0x00, (uint8_t)sizeof(AID_APP)};
  memcpy(select + 5, AID_APP, sizeof(AID_APP));
  select[sizeof(select) - 1] = 0x00;
  largo = sizeof(resp);
  if (!apdu(select, sizeof(select), resp, largo)) {
    Serial.println("  1. SELECT de la app ........ sin respuesta");
    return NO_ES_APP;
  }
  uint16_t estado = estadoDe(resp, largo);
  Serial.printf("  1. SELECT de la app ........ %02X %02X", estado >> 8, estado & 0xFF);
  if (estado == 0x6985) { Serial.println("  (no activada)"); return NO_ACTIVADA; }
  if (estado != 0x9000) { Serial.println("  (no es la app)"); return NO_ES_APP; }
  Serial.println("  (credencial activa)");

  // 2. TOKEN: un nonce nuevo en cada lectura
  uint8_t nonce[8];
  esp_fill_random(nonce, sizeof(nonce));
  uint8_t token[5 + 8 + 1] = {0x80, 0x10, 0x00, 0x00, 0x08};
  memcpy(token + 5, nonce, 8);
  token[13] = 0x00;
  largo = sizeof(resp);
  if (!apdu(token, sizeof(token), resp, largo)) {
    Serial.println("  2. Pedir token .............. sin respuesta");
    return SE_CORTO;
  }
  estado = estadoDe(resp, largo);
  Serial.print("  2. Pedir token .............. nonce ");
  imprimirHex(nonce, 8);
  Serial.printf("→ %d bytes, %02X %02X\n", largo, estado >> 8, estado & 0xFF);
  if (estado != 0x9000 || largo != 28) {
    Serial.println("     ✗ Se esperaban 28 bytes: matrícula (10) + firma (16) + 90 00");
    return DENEGADO;
  }

  // 3. VALIDAR: [0..9] matrícula ASCII · [10..25] firma
  const uint8_t *matricula = resp, *firma = resp + 10;
  for (int i = 0; i < 10; i++)
    matriculaTexto[i] = isprint(matricula[i]) ? (char)matricula[i] : '?';
  matriculaTexto[10] = '\0';
  bool formato = formatoValido(matricula);
  bool firmaOk = firmaCorrecta(nonce, matricula, firma);
  Serial.printf("  3. Matrícula %s ...... formato %s · firma %s\n", matriculaTexto,
                formato ? "✓" : "✗", firmaOk ? "✓ válida" : "✗ NO válida");
  bool permitido = formato && firmaOk;

  // 4. RESULTADO: la app lo guarda en el historial del estudiante
  bool entregado = enviarResultado(permitido);
  Serial.printf("  4. Resultado a la app ...... %s\n",
                entregado ? "recibido (90 00)" : "sin confirmar (¿lo retiraron?)");
  return permitido ? PERMITIDO : DENEGADO;
}


// ================================================================
//  Atender una detección
// ================================================================

void atender(const Objetivo &o) {
  const unsigned long inicio = millis();
  digitalWrite(PIN_LED, HIGH);  // encendido = procesando
  contador++;

  Serial.println();
  Serial.println("----------------------------------------------------------------");
  Serial.printf("Detectado #%d  (t = %.1f s) · %s · UID ", contador, inicio / 1000.0,
                nombreDelTipo(o.sak));
  imprimirHex(o.uid, o.largo);
  if (o.largo == 4 && o.uid[0] == 0x08) Serial.print("(aleatorio)");
  Serial.println();

  char matricula[11] = "";
  // Solo los aparatos que hablan APDUs (bit 0x20 del SAK) pueden ser la app
  Resultado r = (o.sak & 0x20) ? leerCredencial(matricula) : NO_ES_CELULAR;
  porResultado[r]++;

  switch (r) {
    case PERMITIDO:
      Serial.printf("  ✅ ACCESO PERMITIDO · %s\n", matricula);
      strcpy(ultimaPermitida, matricula);
      break;
    case DENEGADO:
      Serial.printf("  ❌ ACCESO DENEGADO · %s (firma o formato no válidos)\n", matricula);
      break;
    case NO_ACTIVADA:
      Serial.println("  ⚠ CREDENCIAL NO ACTIVADA: verifica tu huella en la app y vuelve a acercar");
      break;
    case NO_ES_APP:
      Serial.println("  · No es una credencial de la app (otra tarjeta u otro celular)");
      break;
    case NO_ES_CELULAR:
      Serial.println("  · No es una credencial: es una tarjeta o etiqueta, no la app");
      break;
    case SE_CORTO:
      Serial.println("  ✗ Se cortó a media lectura: vuelve a acercarlo y déjalo quieto");
      break;
    default:
      break;
  }

  // Parpadeo largo = permitido; corto = cualquier otra cosa
  const unsigned long duracion = (r == PERMITIDO) ? LED_PERMITIDO_MS : LED_OTRO_MS;
  while (millis() - inicio < duracion) delay(10);
  digitalWrite(PIN_LED, LOW);
}


// ================================================================
//  Orden 'i': estado sin reiniciar la placa
// ================================================================

void atenderOrdenes() {
  if (!Serial.available() || Serial.read() != 'i') return;
  Serial.printf("[estado] firmware " FW_VERSION " · PN532 firmware %d.%d · %d detecciones · "
                "encendida hace %lu s\n", (versionPN532 >> 16) & 0xFF, (versionPN532 >> 8) & 0xFF,
                contador, millis() / 1000);
  Serial.print("[estado]");
  for (int i = 0; i < NUM_RESULTADOS; i++)
    Serial.printf(" %s: %d ·", NOMBRES_RESULTADO[i], porResultado[i]);
  Serial.printf(" última permitida: %s\n", ultimaPermitida[0] ? ultimaPermitida : "—");
}


// ================================================================

void setup() {
  Serial.begin(115200);
  delay(300);
  pinMode(PIN_LED, OUTPUT);
  digitalWrite(PIN_LED, LOW);

  Serial.println();
  Serial.println("=== nfc-pn532 · lector de credenciales · firmware " FW_VERSION " ===");

  if (claveVacia())
    Serial.println("⚠ La clave en secrets.h está en ceros: ninguna credencial será válida.");
  Serial.printf("Autoprueba criptográfica: %s\n", autoprueba() ? "OK" : "FALLO");

  SPI.begin(PIN_SCK, PIN_MISO, PIN_MOSI, PIN_SS);
  conectarPN532();
  Serial.println("Listo: acerca el celular con la app de credenciales.");
}

void loop() {
  atenderOrdenes();

  Objetivo o;
  if (buscar(o)) {
    ultimaVezVisto = millis();
    if (!hayAlgo) {  // recién llegó: se atiende UNA vez
      hayAlgo = true;
      llegoEn = millis();
      redetecciones = 0;
      atender(o);
      ultimaVezVisto = millis();
    } else {
      redetecciones++;
    }
    return;
  }

  // Se da por retirado tras un rato sin verlo (config.h), no al primer
  // fallo: así un parpadeo de la señal no provoca una segunda lectura.
  if (hayAlgo && millis() - ultimaVezVisto > AUSENCIA_PARA_RETIRO_MS) {
    Serial.printf("  · se fue (t = %.1f s; estuvo %.1f s, se volvió a ver %d veces mientras estaba)\n",
                  ultimaVezVisto / 1000.0, (ultimaVezVisto - llegoEn) / 1000.0, redetecciones);
    hayAlgo = false;
  }

  if (!hayAlgo && millis() - ultimoChequeo > CHEQUEO_SALUD_MS) {
    ultimoChequeo = millis();
    if (nfc.getFirmwareVersion() == 0) {
      Serial.println("⚠ El PN532 dejó de responder. Reconectando...");
      conectarPN532();
    }
  }
}
