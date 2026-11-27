// ============================================================
//  Prueba 04 — ¿Qué cuelga al PN532 al buscar tarjetas?
// ============================================================
//  El PN532 contesta su versión, pero se cuelga (reloj SCL en
//  BAJO) en cuanto empieza a buscar tarjetas. Dos sospechosos:
//
//   A) Preguntarle "¿ya terminaste?" MIENTRAS busca. En I2C puede
//      contestar reteniendo el reloj; si tarda más de lo que el
//      ESP32 aguanta, el ESP32 corta a medias y lo deja colgado.
//   B) La corriente: encender la antena pide ~100-150 mA de golpe.
//
//  Pasos (sin tarjetas cerca):
//   1. Versión                          (¿vivo?)
//   2. Limitar la búsqueda a 5 intentos (comando corto)
//   3. Buscar SIN preguntar: esperar 1 s callado y luego leer
//      -> si se cuelga aquí, es B (la antena), no A
//   4. Versión                          (¿sigue vivo?)
//   5. Buscar preguntando cada 10 ms, como hace la librería
//      -> si se cuelga aquí y no en 3, es A
//   6. Versión                          (¿sigue vivo?)
// ============================================================

#include <Arduino.h>
#include <Wire.h>

// Pausa antes de la PRIMERA pregunta tras mandar un comando.
//   Corrida 1 (2026-11-25): 0 ms  -> el paso 2 falla al pedir el ACK.
//   Corrida 2 (2026-11-25): 50 ms -> ¿funciona si se le da tiempo?
const int PAUSA_ANTES_DE_PREGUNTAR_MS = 50;

const int PIN_SDA = 21, PIN_SCL = 22;
const uint8_t DIR = 0x24;

// Mensajes completos (preámbulo, longitud, comprobaciones, cierre)
const uint8_t VERSION[]  = {0x00, 0x00, 0xFF, 0x02, 0xFE, 0xD4, 0x02, 0x2A, 0x00};
// RFConfiguration: reintentos de búsqueda = 5 (en vez de "para siempre")
const uint8_t LIMITAR[]  = {0x00, 0x00, 0xFF, 0x06, 0xFA, 0xD4, 0x32, 0x05, 0xFF, 0x01, 0x05, 0xF0, 0x00};
// InListPassiveTarget: buscar 1 tarjeta tipo A (MIFARE, NTAG, celulares)
const uint8_t BUSCAR[]   = {0x00, 0x00, 0xFF, 0x04, 0xFC, 0xD4, 0x4A, 0x01, 0x00, 0xE1, 0x00};

bool lineasBien(const char *cuando) {
  Wire.end();
  pinMode(PIN_SDA, INPUT);
  pinMode(PIN_SCL, INPUT);
  delay(2);
  bool sda = digitalRead(PIN_SDA), scl = digitalRead(PIN_SCL);
  Serial.printf("    Lineas %s: SDA=%s SCL=%s\n", cuando, sda ? "ALTO" : "BAJO(!)", scl ? "ALTO" : "BAJO(!)");
  Wire.begin(PIN_SDA, PIN_SCL);
  Wire.setClock(100000);
  return sda && scl;
}

bool enviar(const uint8_t *m, size_t n) {
  Wire.beginTransmission(DIR);
  Wire.write(m, n);
  return Wire.endTransmission() == 0;
}

// Una sola pregunta de estado. Devuelve 1 listo, 0 no listo, -1 fallo del bus.
int estado(unsigned long &us) {
  unsigned long t = micros();
  int n = Wire.requestFrom(DIR, (uint8_t)1);
  us = micros() - t;
  if (n != 1) return -1;
  return Wire.read() == 0x01 ? 1 : 0;
}

int leer(uint8_t *buf, int n) {
  int llegaron = Wire.requestFrom(DIR, (uint8_t)n);
  for (int i = 0; i < llegaron; i++) buf[i] = Wire.read();
  return llegaron;
}

void imprimir(const uint8_t *d, int n) {
  for (int i = 0; i < n; i++) Serial.printf("%02X ", d[i]);
}

// Espera el "recibido" preguntando cada 2 ms (llega en < 1 ms).
bool ack() {
  unsigned long us;
  delay(PAUSA_ANTES_DE_PREGUNTAR_MS);
  for (int i = 0; i < 50; i++) {
    int e = estado(us);
    if (e < 0) { Serial.printf("    ACK: fallo del bus (%lu us)\n", us); return false; }
    if (e == 1) {
      uint8_t b[7];
      leer(b, 7);
      bool ok = b[3] == 0xFF && b[4] == 0x00 && b[5] == 0xFF;
      Serial.printf("    ACK: %s\n", ok ? "recibido" : "RARO");
      return ok;
    }
    delay(2);
  }
  Serial.println("    ACK: nunca llego");
  return false;
}

// Pregunta cada 'cadaMs' hasta que esté listo, registrando cada intento.
bool esperarPreguntando(int cadaMs, int maxIntentos) {
  unsigned long us, maxUs = 0;
  int noListo = 0;
  for (int i = 1; i <= maxIntentos; i++) {
    int e = estado(us);
    if (us > maxUs) maxUs = us;
    if (e < 0) {
      Serial.printf("    pregunta %d: FALLO DEL BUS tras %lu us (antes: %d 'aun no', la mas lenta %lu us)\n",
                    i, us, noListo, maxUs);
      return false;
    }
    if (e == 1) {
      Serial.printf("    listo en la pregunta %d (antes: %d 'aun no', la mas lenta %lu us)\n",
                    i, noListo, maxUs);
      return true;
    }
    noListo++;
    delay(cadaMs);
  }
  Serial.printf("    nunca estuvo listo (%d preguntas)\n", maxIntentos);
  return false;
}

void leerRespuestaBusqueda() {
  uint8_t r[20];
  int n = leer(r, 20);
  Serial.print("    Respuesta: ");
  imprimir(r, n > 12 ? 12 : n);
  // [6] D5 · [7] 4B = "respuesta a buscar" · [8] cuántas encontró
  if (r[6] == 0xD5 && r[7] == 0x4B)
    Serial.printf(" => encontro %d tarjeta(s)\n", r[8]);
  else
    Serial.println(" => formato inesperado");
}

bool pedirVersion(const char *paso) {
  Serial.printf("%s Version\n", paso);
  if (!enviar(VERSION, sizeof(VERSION))) { Serial.println("    no se pudo enviar"); return false; }
  if (!ack()) return false;
  unsigned long us;
  for (int i = 0; i < 20; i++) {
    if (estado(us) == 1) {
      uint8_t r[14];
      leer(r, 14);
      bool ok = r[6] == 0xD5 && r[7] == 0x03;
      Serial.printf("    %s\n", ok ? "VIVO (contesto su version)" : "respuesta rara");
      return ok;
    }
    delay(2);
  }
  Serial.println("    no contesto");
  return false;
}

void experimento() {
  Serial.println("\n=== Experimento ===");
  if (!lineasBien("al empezar")) return;

  if (!pedirVersion("1.")) { lineasBien("tras el fallo"); return; }

  Serial.println("2. Limitar la busqueda a 5 intentos");
  if (!enviar(LIMITAR, sizeof(LIMITAR)) || !ack()) { lineasBien("tras el fallo"); return; }
  if (!esperarPreguntando(2, 50)) { lineasBien("tras el fallo"); return; }
  { uint8_t r[10]; leer(r, 10); Serial.println("    hecho"); }

  Serial.println("3. Buscar SIN preguntar: 1 s callado, luego una sola pregunta");
  if (!enviar(BUSCAR, sizeof(BUSCAR)) || !ack()) { lineasBien("tras el fallo"); return; }
  delay(1000);
  if (!esperarPreguntando(10, 5)) { lineasBien("tras el fallo"); return; }
  leerRespuestaBusqueda();
  lineasBien("tras buscar callado");

  if (!pedirVersion("4.")) { lineasBien("tras el fallo"); return; }

  Serial.println("5. Buscar preguntando cada 10 ms (como la libreria)");
  if (!enviar(BUSCAR, sizeof(BUSCAR)) || !ack()) { lineasBien("tras el fallo"); return; }
  if (!esperarPreguntando(10, 300)) { lineasBien("tras el fallo"); return; }
  leerRespuestaBusqueda();
  lineasBien("tras buscar preguntando");

  pedirVersion("6.");
  Serial.println("=== Fin: los 6 pasos completos ===");
}

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println();
  Serial.println("=== nfc-pn532 · Prueba 04: que cuelga al PN532 al buscar ===");
  pinMode(PIN_SDA, INPUT);
  pinMode(PIN_SCL, INPUT);
  delay(2);
  Serial.printf("Lineas al encender: SDA=%s SCL=%s\n",
                digitalRead(PIN_SDA) ? "ALTO" : "BAJO(!)", digitalRead(PIN_SCL) ? "ALTO" : "BAJO(!)");
  while (Serial.read() != 'g') {
    Serial.println("Esperando la senal 'g'...");
    delay(2000);
  }
  Wire.begin(PIN_SDA, PIN_SCL);
  Wire.setClock(100000);
  experimento();
}

void loop() {}
