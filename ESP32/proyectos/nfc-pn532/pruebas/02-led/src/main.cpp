// ============================================================
//  Prueba 02 — ¿Tiene la placa un LED que el código controle?
// ============================================================
//  Estas placas suelen traer dos LEDs:
//   - uno ROJO de encendido: está prendido siempre que hay
//     corriente, y el código no lo puede tocar;
//   - uno AZUL conectado a GPIO2: ese sí lo controla el código.
//  Pero no todas lo traen. Esta prueba lo enciende y apaga
//  cada medio segundo: si un LED parpadea, existe y sirve.
// ============================================================

#include <Arduino.h>

const int PIN_LED = 2;  // GPIO2: donde lo ponen casi todas estas placas

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println();
  Serial.println("=== nfc-pn532 · Prueba 02: LED en GPIO2 ===");
  pinMode(PIN_LED, OUTPUT);
}

void loop() {
  digitalWrite(PIN_LED, HIGH);
  Serial.println("LED: encendido");
  delay(500);
  digitalWrite(PIN_LED, LOW);
  Serial.println("LED: apagado");
  delay(500);
}
