// ============================================================
//  Prueba 05 — ¿Qué contesta el PN532 por SPI, byte a byte?
// ============================================================
//  El firmware no encuentra al PN532 por SPI. Aquí se le pide la
//  versión a mano y se imprime CADA byte que devuelve:
//
//   - todo FF  -> MISO no conectado, o el PN532 no está en SPI
//   - todo 00  -> algo tira de MISO hacia abajo
//   - patrón   -> los cables están bien; el problema es de software
//
//  En SPI el PN532 entiende tres "verbos" (el primer byte):
//    01 = te mando datos · 02 = ¿estás listo? · 03 = dame los datos
//  Y habla con el bit menos significativo primero (LSB first).
// ============================================================

#include <Arduino.h>
#include <SPI.h>

const int PIN_SCK = 18, PIN_MISO = 19, PIN_MOSI = 23, PIN_SS = 5;
const SPISettings AJUSTES(1000000, LSBFIRST, SPI_MODE0);  // 1 MHz, como la librería

const uint8_t VERSION[] = {0x00, 0x00, 0xFF, 0x02, 0xFE, 0xD4, 0x02, 0x2A, 0x00};

void imprimir(const uint8_t *d, int n) {
  for (int i = 0; i < n; i++) Serial.printf("%02X ", d[i]);
}

// ¿Qué hace MISO cuando nadie habla? Con SS en ALTO el PN532 debería
// soltarla: entonces sigue a la resistencia interna que pongamos.
void misoEnReposo() {
  SPI.end();
  pinMode(PIN_MISO, INPUT_PULLDOWN); delay(2); int conAbajo = digitalRead(PIN_MISO);
  pinMode(PIN_MISO, INPUT_PULLUP);   delay(2); int conArriba = digitalRead(PIN_MISO);
  pinMode(PIN_MISO, INPUT);
  Serial.printf("  MISO en reposo: con pull-down=%d, con pull-up=%d -> %s\n", conAbajo, conArriba,
                (conAbajo == 0 && conArriba == 1) ? "suelta (normal)" :
                (conAbajo == 1 && conArriba == 1) ? "algo la jala ARRIBA" : "algo la jala ABAJO");
  SPI.begin(PIN_SCK, PIN_MISO, PIN_MOSI, PIN_SS);
}

uint8_t estado() {
  SPI.beginTransaction(AJUSTES);
  digitalWrite(PIN_SS, LOW);
  delayMicroseconds(100);
  SPI.transfer(0x02);
  uint8_t e = SPI.transfer(0x00);
  digitalWrite(PIN_SS, HIGH);
  SPI.endTransaction();
  return e;
}

void leerDatos(uint8_t *buf, int n) {
  SPI.beginTransaction(AJUSTES);
  digitalWrite(PIN_SS, LOW);
  delayMicroseconds(100);
  SPI.transfer(0x03);
  for (int i = 0; i < n; i++) buf[i] = SPI.transfer(0x00);
  digitalWrite(PIN_SS, HIGH);
  SPI.endTransaction();
}

// Pregunta "¿listo?" y resume lo que contesta.
bool esperar(const char *que) {
  uint8_t visto[8]; int distintos = 0;
  for (int i = 0; i < 100; i++) {
    uint8_t e = estado();
    bool nuevo = true;
    for (int k = 0; k < distintos; k++) if (visto[k] == e) nuevo = false;
    if (nuevo && distintos < 8) visto[distintos++] = e;
    if (e == 0x01) { Serial.printf("  %s: LISTO en la pregunta %d\n", que, i + 1); return true; }
    delay(5);
  }
  Serial.printf("  %s: nunca dijo LISTO. Valores que contesto: ", que);
  imprimir(visto, distintos);
  Serial.println();
  return false;
}

void conversacion() {
  Serial.println("--- Conversacion SPI ---");
  misoEnReposo();
  Serial.printf("  Estado antes de nada: %02X\n", estado());

  // Despertarlo: SS en BAJO 2 ms (lo mismo que hace la librería)
  digitalWrite(PIN_SS, LOW); delay(2); digitalWrite(PIN_SS, HIGH); delay(5);

  SPI.beginTransaction(AJUSTES);
  digitalWrite(PIN_SS, LOW);
  delayMicroseconds(100);
  SPI.transfer(0x01);
  for (uint8_t b : VERSION) SPI.transfer(b);
  digitalWrite(PIN_SS, HIGH);
  SPI.endTransaction();
  Serial.println("  Enviado 'dime tu version'");

  if (!esperar("ACK")) return;
  uint8_t ack[6]; leerDatos(ack, 6);
  Serial.print("  ACK: "); imprimir(ack, 6); Serial.println(" (se espera 00 00 FF 00 FF 00)");

  if (!esperar("Respuesta")) return;
  uint8_t r[12]; leerDatos(r, 12);
  Serial.print("  Respuesta: "); imprimir(r, 12); Serial.println();
  if (r[5] == 0xD5 && r[6] == 0x03)
    Serial.printf("  => PN5%02X, firmware %d.%d  ** CONVERSACION COMPLETA **\n", r[7], r[8], r[9]);
}

// Mira cada pin SIN mandar nada: ¿qué hay al otro lado del cable?
//   pull-down=1 y pull-up=1 -> algo lo jala ARRIBA (resistencia del módulo)
//   pull-down=0 y pull-up=1 -> suelto: nada, o una salida en reposo
//   pull-down=0 y pull-up=0 -> algo lo jala ABAJO
void sondeo() {
  const int pines[] = {18, 19, 23, 5, 21, 22};
  const char *rol[] = {"SCK ", "MISO", "MOSI", "SS  ", "(ex SDA)", "(ex SCL)"};
  Serial.println("--- Sondeo de pines (sin mandar nada) ---");
  for (int i = 0; i < 6; i++) {
    pinMode(pines[i], INPUT_PULLDOWN); delay(3); int a = digitalRead(pines[i]);
    pinMode(pines[i], INPUT_PULLUP);   delay(3); int b = digitalRead(pines[i]);
    pinMode(pines[i], INPUT);
    Serial.printf("  P%-2d %-8s: %s\n", pines[i], rol[i],
                  (a && b) ? "jalado ARRIBA" : (!a && b) ? "suelto" : (!a && !b) ? "jalado ABAJO" : "raro");
  }
}

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println();
  Serial.println("=== nfc-pn532 · Prueba 05: SPI a mano ===");
  sondeo();
  pinMode(PIN_SS, OUTPUT);
  digitalWrite(PIN_SS, HIGH);
  SPI.begin(PIN_SCK, PIN_MISO, PIN_MOSI, PIN_SS);
}

void loop() {
  conversacion();
  Serial.println();
  delay(4000);
}
