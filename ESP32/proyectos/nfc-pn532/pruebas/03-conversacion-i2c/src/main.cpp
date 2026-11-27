// ============================================================
//  Prueba 03 — ¿Dónde se atora la conversación con el PN532?
// ============================================================
//  El firmware da "Error 263" (tiempo agotado) al pedirle datos
//  al PN532. Aquí se hace la conversación A MANO, sin librería,
//  paso a paso y midiendo cuánto tarda cada paso:
//
//   1. ¿Las líneas están en ALTO en reposo?
//   2. ¿Contesta a su dirección? (lo que ya pasaba en la prueba 01)
//   3. Mandarle "dime tu versión" (GetFirmwareVersion)
//   4. Preguntar "¿ya tienes respuesta?" hasta que diga que sí
//   5. Leer su "recibido" (ACK) y luego la respuesta
//   6. ¿Las líneas quedaron en ALTO, o alguien las retiene?
// ============================================================

#include <Arduino.h>
#include <Wire.h>

const int PIN_SDA = 21, PIN_SCL = 22;
const uint8_t DIR = 0x24;

// Mensaje GetFirmwareVersion completo, tal como lo define NXP:
// preámbulo, inicio, longitud y su comprobación, dirección (D4),
// comando (02), comprobación de datos, cierre.
const uint8_t PEDIR_VERSION[] = {0x00, 0x00, 0xFF, 0x02, 0xFE, 0xD4, 0x02, 0x2A, 0x00};

void lineas(const char *cuando) {
  pinMode(PIN_SDA, INPUT);
  pinMode(PIN_SCL, INPUT);
  delay(2);
  Serial.printf("  Lineas %s: SDA=%s SCL=%s\n", cuando,
                digitalRead(PIN_SDA) ? "ALTO" : "BAJO(!)",
                digitalRead(PIN_SCL) ? "ALTO" : "BAJO(!)");
}

void imprimir(const uint8_t *d, int n) {
  for (int i = 0; i < n; i++) Serial.printf("%02X ", d[i]);
}

// Lee 'n' bytes y cuenta cuánto tardó. Devuelve cuántos llegaron.
int leer(uint8_t *buf, int n, unsigned long &us) {
  unsigned long t = micros();
  int llegaron = Wire.requestFrom(DIR, (uint8_t)n);
  us = micros() - t;
  for (int i = 0; i < llegaron; i++) buf[i] = Wire.read();
  return llegaron;
}

// Pregunta "¿listo?" (byte de estado 0x01) hasta 'intentos' veces.
bool esperarListo(int intentos) {
  uint8_t b;
  unsigned long us;
  for (int i = 1; i <= intentos; i++) {
    int n = leer(&b, 1, us);
    if (i <= 3 || n == 0 || b == 0x01)
      Serial.printf("    intento %2d: %s  (%lu us)\n", i,
                    n == 0 ? "NADA (fallo del bus)" : (b == 0x01 ? "LISTO (01)" : "aun no (00)"), us);
    if (n == 1 && b == 0x01) return true;
    delay(5);
  }
  return false;
}

void conversacion() {
  Serial.println("--- Conversacion ---");
  lineas("en reposo");

  Wire.begin(PIN_SDA, PIN_SCL);
  Wire.setClock(100000);
  Serial.printf("  Tiempo maximo de espera del bus (Wire): %u ms\n", Wire.getTimeOut());

  Wire.beginTransmission(DIR);
  Serial.printf("  Direccion 0x24: %s\n", Wire.endTransmission() == 0 ? "contesta" : "NO contesta");

  Wire.beginTransmission(DIR);
  Wire.write(PEDIR_VERSION, sizeof(PEDIR_VERSION));
  unsigned long t = micros();
  int r = Wire.endTransmission();
  Serial.printf("  Enviar 'dime tu version': %s (codigo %d, %lu us)\n",
                r == 0 ? "OK" : "FALLO", r, micros() - t);

  Serial.println("  Esperando el 'recibido' (ACK):");
  if (esperarListo(40)) {
    uint8_t ack[7];
    unsigned long us;
    int n = leer(ack, 7, us);
    Serial.print("  ACK: ");
    imprimir(ack, n);
    Serial.printf(" (%d bytes; se espera 01 00 00 FF 00 FF 00)\n", n);

    Serial.println("  Esperando la respuesta:");
    if (esperarListo(40)) {
      uint8_t resp[14];
      n = leer(resp, 14, us);
      Serial.print("  Respuesta: ");
      imprimir(resp, n);
      Serial.println();
      // [0] estado · [1..5] cabecera · [6] D5 = "respuesta" · [7] 03 = "a versión"
      if (n == 14 && resp[6] == 0xD5 && resp[7] == 0x03)
        Serial.printf("  => PN5%02X, firmware %d.%d  ** CONVERSACION COMPLETA **\n",
                      resp[8], resp[9], resp[10]);
    }
  }

  Wire.end();
  lineas("al terminar");
  Serial.println();
}

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println();
  Serial.println("=== nfc-pn532 · Prueba 03: conversacion I2C a mano ===");

  // No tocar el PN532 hasta recibir una 'g' por el monitor serie.
  // Así se puede observar la PRIMERA conversación tras encenderlo,
  // que es la que importa: la que deja al PN532 sano o colgado.
  lineas("al encender (antes de tocar nada)");
  while (Serial.read() != 'g') {
    Serial.println("Esperando la senal 'g' para empezar...");
    delay(2000);
  }
}

void loop() {
  conversacion();
  delay(5000);
}
