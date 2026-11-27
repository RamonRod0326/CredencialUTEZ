// ============================================================
//  Prueba 01 — ¿Está bien el cableado I2C del PN532?
// ============================================================
//  Comprueba dos cosas, de la más física a la más lógica:
//
//   1. ¿Llega cada cable? Se mira si el pin "siente" las
//      resistencias pull-up del PN532.
//   2. ¿Contesta el PN532? Se recorre el bus I2C preguntando
//      en cada dirección quién está.
//
//  Si las dos pasan, el cableado está bien.
// ============================================================

#include <Arduino.h>
#include <Wire.h>

const int PIN_SDA       = 21;  // Datos I2C (el de por defecto)
const int PIN_SCL       = 22;  // Reloj I2C (el de por defecto)
const int PIN_SCL_VIEJO = 19;  // Donde estaba el reloj antes
const uint8_t DIR_PN532 = 0x24;

// ¿Hay algo de afuera jalando este pin hacia arriba?
//
// El truco: el ESP32 activa una resistencia interna que jala el
// pin hacia ABAJO (pull-down, débil: ~45 kΩ). Si aun así se lee
// ALTO, es que algo más fuerte lo jala hacia ARRIBA: la
// resistencia pull-up del PN532. O sea, el cable está puesto.
// Si se lee BAJO, no hay nada conectado a ese pin.
bool hayCableAlPN532(int pin) {
  pinMode(pin, INPUT_PULLDOWN);
  delay(5);
  bool alto = (digitalRead(pin) == HIGH);
  pinMode(pin, INPUT);  // se suelta el pin para no molestar al bus
  return alto;
}

void revisarCables() {
  bool sda   = hayCableAlPN532(PIN_SDA);
  bool scl   = hayCableAlPN532(PIN_SCL);
  bool viejo = hayCableAlPN532(PIN_SCL_VIEJO);

  Serial.printf("  SDA en P%d: %s\n", PIN_SDA,
                sda ? "OK   (siente al PN532)" : "FALLA (no hay nada conectado)");
  Serial.printf("  SCL en P%d: %s\n", PIN_SCL,
                scl ? "OK   (siente al PN532)" : "FALLA (no hay nada conectado)");
  Serial.printf("  P%d (SCL viejo): %s\n", PIN_SCL_VIEJO,
                viejo ? "OJO  (todavia hay algo conectado aqui)" : "OK   (libre)");
}

// Recorre las 127 direcciones posibles del bus I2C. En cada una
// "toca la puerta" y mira si alguien contesta.
void buscarEnElBus() {
  Wire.begin(PIN_SDA, PIN_SCL);
  Wire.setClock(100000);  // 100 kHz: la velocidad más lenta y tolerante

  int encontrados = 0;
  bool hayPN532 = false;
  for (uint8_t dir = 1; dir < 127; dir++) {
    Wire.beginTransmission(dir);
    if (Wire.endTransmission() == 0) {  // 0 = alguien contestó
      Serial.printf("  Contesta algo en 0x%02X%s\n", dir,
                    dir == DIR_PN532 ? "  <- el PN532" : "  (no esperado)");
      encontrados++;
      if (dir == DIR_PN532) hayPN532 = true;
    }
  }
  Wire.end();  // se suelta el bus para la siguiente vuelta

  if (encontrados == 0) {
    Serial.println("  Nadie contesta en el bus.");
  }
  Serial.printf("  Veredicto: %s\n",
                hayPN532 ? "CABLEADO OK" : "EL PN532 NO CONTESTA");
}

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println();
  Serial.println("=== nfc-pn532 · Prueba 01: cableado I2C ===");
}

void loop() {
  Serial.println("--- Cables ---");
  revisarCables();
  Serial.println("--- Bus I2C (SDA=P21, SCL=P22) ---");
  buscarEnElBus();
  Serial.println();
  delay(3000);
}
