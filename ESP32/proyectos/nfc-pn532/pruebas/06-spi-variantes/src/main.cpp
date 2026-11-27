// ============================================================
//  Prueba 06 — ¿Contesta el PN532 con ALGUNA variante de SPI?
// ============================================================
//  La prueba 05 solo obtuvo FF. Antes de culpar al módulo, se
//  prueba todo lo que podría estar mal "de ajuste":
//
//   A) Bit a bit A MANO, muy despacio (~1 kHz), sin el circuito
//      SPI del ESP32. Descarta cualquier problema de configuración.
//   B) 3 velocidades × 4 modos de reloj × 2 órdenes de bits, con un
//      despertar largo (SS en BAJO 10 ms + 100 ms de espera).
//
//  Siempre con los mismos papeles: SCK, MOSI y SS salen del ESP32;
//  MISO entra. Nunca dos salidas pelean por un cable.
//
//  Éxito = cualquier byte distinto de FF.
// ============================================================

#include <Arduino.h>
#include <SPI.h>

const int PIN_SCK = 18, PIN_MISO = 19, PIN_MOSI = 23, PIN_SS = 5;
const uint8_t VERSION[] = {0x00, 0x00, 0xFF, 0x02, 0xFE, 0xD4, 0x02, 0x2A, 0x00};

// ---------- A) A mano, bit a bit (LSB primero, modo 0) ----------
uint8_t byteAMano(uint8_t sale) {
  uint8_t entra = 0;
  for (int i = 0; i < 8; i++) {
    digitalWrite(PIN_MOSI, (sale >> i) & 1);  // poner el bit
    delayMicroseconds(500);
    digitalWrite(PIN_SCK, HIGH);              // flanco de subida: ambos leen
    if (digitalRead(PIN_MISO)) entra |= (1 << i);
    delayMicroseconds(500);
    digitalWrite(PIN_SCK, LOW);
  }
  return entra;
}

void pruebaAMano() {
  Serial.println("A) Bit a bit a mano (~1 kHz):");
  SPI.end();
  pinMode(PIN_SCK, OUTPUT);  digitalWrite(PIN_SCK, LOW);
  pinMode(PIN_MOSI, OUTPUT); digitalWrite(PIN_MOSI, LOW);
  pinMode(PIN_SS, OUTPUT);   digitalWrite(PIN_SS, HIGH);
  pinMode(PIN_MISO, INPUT);

  // Despertar largo
  digitalWrite(PIN_SS, LOW); delay(10); digitalWrite(PIN_SS, HIGH); delay(100);

  // Estado antes de nada
  digitalWrite(PIN_SS, LOW); delay(2);
  byteAMano(0x02);
  uint8_t e0 = byteAMano(0x00);
  digitalWrite(PIN_SS, HIGH);
  Serial.printf("   estado inicial: %02X\n", e0);

  // Mandar 'dime tu version'
  digitalWrite(PIN_SS, LOW); delay(2);
  byteAMano(0x01);
  for (uint8_t b : VERSION) byteAMano(b);
  digitalWrite(PIN_SS, HIGH);
  delay(20);

  // Preguntar estado unas veces
  Serial.print("   estados tras el comando: ");
  for (int i = 0; i < 8; i++) {
    digitalWrite(PIN_SS, LOW); delay(1);
    byteAMano(0x02);
    Serial.printf("%02X ", byteAMano(0x00));
    digitalWrite(PIN_SS, HIGH);
    delay(10);
  }
  Serial.println();

  // Y leer datos aunque no diga listo, por si acaso
  digitalWrite(PIN_SS, LOW); delay(1);
  byteAMano(0x03);
  Serial.print("   datos: ");
  for (int i = 0; i < 12; i++) Serial.printf("%02X ", byteAMano(0x00));
  digitalWrite(PIN_SS, HIGH);
  Serial.println();
}

// ---------- B) Variantes con el circuito SPI del ESP32 ----------
uint8_t estado(const SPISettings &s) {
  SPI.beginTransaction(s);
  digitalWrite(PIN_SS, LOW); delayMicroseconds(500);
  SPI.transfer(0x02);
  uint8_t e = SPI.transfer(0x00);
  digitalWrite(PIN_SS, HIGH);
  SPI.endTransaction();
  return e;
}

void variantes() {
  Serial.println("B) Variantes (velocidad x modo x orden): primer valor != FF visto");
  SPI.begin(PIN_SCK, PIN_MISO, PIN_MOSI, PIN_SS);
  pinMode(PIN_SS, OUTPUT); digitalWrite(PIN_SS, HIGH);
  const uint32_t vel[] = {1000000, 400000, 100000};
  const uint8_t modos[] = {SPI_MODE0, SPI_MODE1, SPI_MODE2, SPI_MODE3};
  const uint8_t orden[] = {LSBFIRST, MSBFIRST};
  int conRespuesta = 0;
  for (uint32_t v : vel) for (int m = 0; m < 4; m++) for (int o = 0; o < 2; o++) {
    SPISettings s(v, orden[o], modos[m]);
    digitalWrite(PIN_SS, LOW); delay(10); digitalWrite(PIN_SS, HIGH); delay(100);
    uint8_t primero = estado(s);
    SPI.beginTransaction(s);
    digitalWrite(PIN_SS, LOW); delayMicroseconds(500);
    SPI.transfer(0x01);
    for (uint8_t b : VERSION) SPI.transfer(b);
    digitalWrite(PIN_SS, HIGH);
    SPI.endTransaction();
    uint8_t visto = primero;
    for (int i = 0; i < 20 && visto == 0xFF; i++) { delay(5); visto = estado(s); }
    if (visto != 0xFF) conRespuesta++;
    Serial.printf("   %7lu Hz  modo %d  %s : %02X%s\n", (unsigned long)v, m,
                  o == 0 ? "LSB" : "MSB", visto, visto != 0xFF ? "  <-- CONTESTA" : "");
  }
  Serial.printf("   Variantes con alguna respuesta: %d de 24\n", conRespuesta);
}

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println();
  Serial.println("=== nfc-pn532 · Prueba 06: variantes de SPI ===");
  pruebaAMano();
  variantes();
  Serial.println("=== Fin ===");
}

void loop() {}
