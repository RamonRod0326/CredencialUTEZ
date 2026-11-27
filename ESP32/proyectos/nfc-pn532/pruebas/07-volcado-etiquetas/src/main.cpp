// ============================================================
//  nfc-pn532 — Lector de etiquetas NFC
// ============================================================
//  Al acercar cualquier cosa NFC (tarjeta, llavero, celular):
//
//   1. Enciende el LED azul mientras la lee.
//   2. Dice qué tipo de etiqueta es y su UID (su número de serie).
//   3. Vuelca lo que trae guardado, si se puede leer.
//
//  Cada objeto cuenta UNA vez: para volver a leerlo hay que
//  retirarlo y acercarlo de nuevo.
//
//  Decisiones: ADR-0003 (SPI, sin IRQ ni RSTO; reemplaza al I2C)
//              ADR-0002 (librería Adafruit PN532)
//  Cableado:   6-esquemas/conexiones-fisicas.md
// ============================================================

#include <Arduino.h>
#include <SPI.h>
#include <Adafruit_PN532.h>

#ifndef FW_VERSION
#define FW_VERSION "desconocido"
#endif

// --- Pines ------------------------------------------------------
// Bus SPI: son los pines por defecto del ESP32 (VSPI), así que la
// librería los usa sola. Se dejan escritos para que se vea a dónde va
// cada cable (6-esquemas/conexiones-fisicas.md).
const int PIN_SCK  = 18;  // reloj: lo marca SIEMPRE el ESP32 (ADR-0003)
const int PIN_MISO = 19;  // datos del PN532 hacia el ESP32
const int PIN_MOSI = 23;  // datos del ESP32 hacia el PN532
const int PIN_SS   = 5;   // "te estoy hablando a ti"
const int PIN_LED  = 2;   // LED azul de la placa (verificado en la prueba 02)

// --- Ajustes ----------------------------------------------------
const unsigned long LED_MINIMO_MS     = 200;    // para que el parpadeo se vea siempre
const int           FALLOS_PARA_RETIRO = 3;     // búsquedas vacías seguidas = "lo retiraron"
const unsigned long CHEQUEO_SALUD_MS  = 10000;  // cada cuánto se confirma que el PN532 vive
const uint16_t      ESPERA_MAX_MS     = 1000;   // tope de espera por respuesta del PN532

// Cuántas veces reintenta el PN532 buscar una etiqueta antes de
// contestar "no hay nada". OJO: el valor de fábrica (0xFF) significa
// "buscar para siempre", y el programa se quedaría congelado hasta
// que alguien acerque algo. Con un número pequeño, el PN532 contesta
// enseguida y el programa sigue vivo.
const uint8_t REINTENTOS_BUSQUEDA = 5;

// Por SPI. IRQ y RSTO no están conectados: la librería pregunta
// "¿ya tienes respuesta?" por el propio bus.
Adafruit_PN532 nfc(PIN_SS, &SPI);

// La librería guarda la última respuesta del PN532 en este búfer
// interno. De ahí se saca el TIPO de etiqueta (los bytes ATQA y SAK),
// que la librería recibe pero no entrega.
// ⚠️ Depende de la versión 1.3.4 clavada en platformio.ini (ADR-0002).
extern byte pn532_packetbuffer[];

// Lo que se sabe de una etiqueta recién detectada
struct Etiqueta {
  uint8_t  uid[10];  // número de serie: 4, 7 o 10 bytes
  uint8_t  largo;
  uint16_t atqa;     // "respuesta a la llamada": pista del tipo
  uint8_t  sak;      // "respuesta a la selección": EL dato que dice el tipo
};

enum Familia { CLASSIC, TIPO2, INTELIGENTE, OTRA };

uint32_t      versionPN532   = 0;
bool          habiaAlgo      = false;
unsigned long llegoEn        = 0;   // cuándo llegó lo que está encima
int           sectorDeCorte  = -1;  // dónde se cortó la última lectura Classic

// Registro de etiquetas DISTINTAS vistas desde el arranque. Sirve para
// saber cuántos objetos diferentes ha visto el lector, no cuántas veces.
struct Vista { uint8_t uid[10]; uint8_t largo; uint8_t sak; int veces; };
const int MAX_VISTAS = 16;
Vista vistas[MAX_VISTAS];
int   numVistas = 0;

void registrarVista(const uint8_t *uid, uint8_t largo, uint8_t sak) {
  for (int i = 0; i < numVistas; i++)
    if (vistas[i].largo == largo && memcmp(vistas[i].uid, uid, largo) == 0) {
      vistas[i].veces++;
      return;
    }
  if (numVistas < MAX_VISTAS) {
    memcpy(vistas[numVistas].uid, uid, largo);
    vistas[numVistas].largo = largo;
    vistas[numVistas].sak = sak;
    vistas[numVistas].veces = 1;
    numVistas++;
  }
}
int           fallosSeguidos = 0;
unsigned long ultimoChequeo  = 0;
int           contador       = 0;


// ================================================================
//  Utilidades para imprimir
// ================================================================

// Bytes en hexadecimal: "04 A1 B2 ..."
void imprimirHex(const uint8_t *d, int n) {
  for (int i = 0; i < n; i++) Serial.printf("%02X ", d[i]);
}

// Los mismos bytes como letras, si son imprimibles. Así se ven a
// simple vista los textos y direcciones web guardados.
void imprimirTexto(const uint8_t *d, int n) {
  for (int i = 0; i < n; i++) Serial.print(isprint(d[i]) ? (char)d[i] : '.');
}

bool estaVacio(const uint8_t *d, int n) {
  for (int i = 0; i < n; i++)
    if (d[i] != 0) return false;
  return true;
}


// ================================================================
//  Tipo de etiqueta
// ================================================================
// El SAK es un byte que la etiqueta manda al ser seleccionada. Cada
// familia usa valores fijos, así que es como leer la etiqueta de la ropa.

const char *nombreDelTipo(uint8_t sak) {
  switch (sak) {
    case 0x08: return "MIFARE Classic 1K";
    case 0x88: return "MIFARE Classic 1K (Infineon)";
    case 0x09: return "MIFARE Mini";
    case 0x18: return "MIFARE Classic 4K";
    case 0x00: return "Tipo 2 (MIFARE Ultralight / NTAG)";
    case 0x20: return "ISO 14443-4 (celular, tarjeta bancaria o inteligente)";
    case 0x28: return "ISO 14443-4 que además imita a una MIFARE Classic";
    default:   return "desconocido";
  }
}

Familia familia(uint8_t sak) {
  switch (sak) {
    case 0x08: case 0x88: case 0x09: case 0x18: return CLASSIC;
    case 0x00:                                  return TIPO2;
  }
  return (sak & 0x20) ? INTELIGENTE : OTRA;
}


// ================================================================
//  Hablar con el PN532
// ================================================================

// Lo arranca y no sigue hasta que conteste. Si no contesta, lo dice
// cada 2 s en vez de quedarse mudo (RF-03).
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
                   "MOSI=P23, SS=P5) y que el DIP esté en SPI. Reintento en 2 s...");
    delay(2000);
  }
}

// ¿Hay algo cerca? Si sí, rellena 'e' y devuelve true.
bool buscarEtiqueta(Etiqueta &e) {
  if (!nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, e.uid, &e.largo, ESPERA_MAX_MS))
    return false;
  // Formato de la respuesta: [9..10] ATQA · [11] SAK (ver ADR-0002)
  e.atqa = (pn532_packetbuffer[9] << 8) | pn532_packetbuffer[10];
  e.sak  = pn532_packetbuffer[11];
  return true;
}

// Tras un intento fallido de abrir un sector, la tarjeta se "duerme"
// y hay que volver a llamarla. Devuelve false si ya no está, o si
// contesta otra distinta (la cambiaron a media lectura).
bool reseleccionar(const Etiqueta &e) {
  Etiqueta otra;
  if (!buscarEtiqueta(otra)) return false;
  return otra.largo == e.largo && memcmp(otra.uid, e.uid, e.largo) == 0;
}


// ================================================================
//  MIFARE Classic
// ================================================================
// La memoria son 16 "sectores" de 4 "bloques" de 16 bytes. Cada
// sector tiene su propia cerradura: para leerlo hay que presentar una
// clave de 6 bytes. Solo se prueban claves PÚBLICAS: la de fábrica y
// las del estándar NFC. Si ninguna abre, el sector se reporta como
// cerrado y se sigue con el siguiente.

const int NUM_CLAVES = 3;
const uint8_t CLAVES[NUM_CLAVES][6] = {
  {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF},  // de fábrica
  {0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5},  // pública NFC: directorio (sector 0)
  {0xD3, 0xF7, 0xD3, 0xF7, 0xD3, 0xF7},  // pública NFC: datos
};
const char *NOMBRES_CLAVE[NUM_CLAVES] = {
  "de fábrica", "pública NFC (directorio)", "pública NFC (datos)"
};

// El último bloque de cada sector es la "cerradura": claves y
// permisos. Los bytes 6-8 dicen los permisos; FF 07 80 es de fábrica.
bool cerraduraDeFabrica(const uint8_t *b) {
  return b[6] == 0xFF && b[7] == 0x07 && b[8] == 0x80;
}

void imprimirBloque(int n, const uint8_t *d, const char *nota) {
  Serial.printf("    Bloque %2d: ", n);
  imprimirHex(d, 16);
  Serial.print("| ");
  imprimirTexto(d, 16);
  if (nota) Serial.printf("  <- %s", nota);
  Serial.println();
}

// Devuelve false si la tarjeta se retiró antes de terminar.
bool volcarClassic(const Etiqueta &e) {
  const int sectores = (e.sak == 0x09) ? 5 : 16;  // 4K: solo los primeros 16
  int conDatos = 0, vacios = 0, cerrados = 0;

  for (int s = 0; s < sectores; s++) {
    const int primero = s * 4;

    // Probar las claves públicas, una por una
    int clave = -1;
    for (int k = 0; k < NUM_CLAVES && clave < 0; k++) {
      if (nfc.mifareclassic_AuthenticateBlock((uint8_t *)e.uid, e.largo, primero, 0,
                                              (uint8_t *)CLAVES[k])) {
        clave = k;
      } else if (!reseleccionar(e)) {
        sectorDeCorte = s;
        return false;
      }
    }
    if (clave < 0) {
      Serial.printf("  Sector %2d: cerrado (no abre con ninguna clave pública)\n", s);
      cerrados++;
      continue;
    }

    uint8_t b[4][16];
    for (int i = 0; i < 4; i++)
      if (!nfc.mifareclassic_ReadDataBlock(primero + i, b[i])) {
        sectorDeCorte = s;
        return false;
      }

    // Un sector vacío y de fábrica no dice nada: se cuenta y no se
    // imprime, para que lo interesante no se pierda entre ceros.
    // El sector 0 se imprime siempre: su bloque 0 es el de fábrica.
    bool vacio = estaVacio(b[0], 16) && estaVacio(b[1], 16) && estaVacio(b[2], 16);
    if (s > 0 && vacio && clave == 0 && cerraduraDeFabrica(b[3])) {
      vacios++;
      continue;
    }

    Serial.printf("  Sector %2d (abre con la clave %s)\n", s, NOMBRES_CLAVE[clave]);
    for (int i = 0; i < 4; i++) {
      const char *nota = nullptr;
      if (primero + i == 0)  nota = "de fábrica: UID y datos del fabricante";
      else if (i == 3)       nota = "cerradura: claves y permisos";
      else if (!estaVacio(b[i], 16)) conDatos++;
      imprimirBloque(primero + i, b[i], nota);
    }
  }

  Serial.printf("  Resumen: %d bloques con datos · %d sectores vacíos (no se muestran) · "
                "%d cerrados\n", conDatos, vacios, cerrados);
  return true;
}


// ================================================================
//  Tipo 2 (MIFARE Ultralight / NTAG)
// ================================================================
// Memoria en "páginas" de 4 bytes, sin cerraduras. Las páginas 0-2
// llevan el UID y los bloqueos; la 3 dice el formato y el tamaño.

bool volcarTipo2(const Etiqueta &e) {
  uint8_t cc[4];
  if (!nfc.ntag2xx_ReadPage(3, cc)) return false;

  // Si la página 3 empieza con E1, la etiqueta tiene formato NFC y
  // su byte 2 dice cuántos bytes de datos tiene (en grupos de 8).
  bool formatoNFC = (cc[0] == 0xE1);
  int paginas = formatoNFC ? 4 + cc[2] * 2 : 16;  // 16 = MIFARE Ultralight
  Serial.printf("  Memoria: %d páginas de 4 bytes%s\n", paginas,
                formatoNFC ? " (formato NFC)" : "");

  int omitidas = 0;
  for (int p = 0; p < paginas; p += 4) {
    uint8_t linea[16];
    int n = 0;
    for (int q = p; q < paginas && q < p + 4; q++, n += 4)
      if (!nfc.ntag2xx_ReadPage(q, linea + n)) return false;

    if (p >= 4 && estaVacio(linea, n)) {
      omitidas += n / 4;
      continue;
    }
    Serial.printf("    Págs %3d-%3d: ", p, p + n / 4 - 1);
    imprimirHex(linea, n);
    Serial.print("| ");
    imprimirTexto(linea, n);
    if (p == 0) Serial.print("  <- UID, bloqueos y formato");
    Serial.println();
  }
  if (omitidas) Serial.printf("  (%d páginas vacías no se muestran)\n", omitidas);
  return true;
}


// ================================================================
//  Celulares y tarjetas inteligentes
// ================================================================

void explicarInteligente(const Etiqueta &e) {
  Serial.println("  Es un aparato 'inteligente' (celular, tarjeta bancaria, pasaporte...).");
  Serial.println("  No guarda datos sueltos como una etiqueta: dentro lleva aplicaciones");
  Serial.println("  con las que hay que conversar. Eso queda fuera de esta etapa.");
  if (e.largo == 4 && e.uid[0] == 0x08)
    Serial.println("  Su UID empieza con 08: es ALEATORIO. Cambiará cada vez que lo acerques.");
}


// ================================================================
//  Atender una detección
// ================================================================

void atender(const Etiqueta &e) {
  const unsigned long inicio = millis();
  digitalWrite(PIN_LED, HIGH);  // encendido = leyendo: no retires la tarjeta
  contador++;

  Serial.println();
  Serial.println("----------------------------------------------------------------");
  Serial.printf("Detectado #%d  (t = %.1f s)\n", contador, inicio / 1000.0);
  Serial.printf("  Tipo: %s   (SAK 0x%02X, ATQA 0x%04X)\n", nombreDelTipo(e.sak), e.sak, e.atqa);
  Serial.printf("  UID (%d bytes): ", e.largo);
  imprimirHex(e.uid, e.largo);
  Serial.println();

  registrarVista(e.uid, e.largo, e.sak);
  sectorDeCorte = -1;
  bool completo = true;
  switch (familia(e.sak)) {
    case CLASSIC:     completo = volcarClassic(e); break;
    case TIPO2:       completo = volcarTipo2(e);   break;
    case INTELIGENTE: explicarInteligente(e);      break;
    case OTRA:        Serial.println("  No sé leer el contenido de este tipo."); break;
  }
  if (!completo)
    Serial.printf("  ✗ Lectura incompleta: se cortó en el sector %d de 15 "
                  "(¿se movió o se retiró antes de que se apagara el LED?)\n", sectorDeCorte);

  Serial.printf("Leído en %lu ms. Ya puedes retirarlo.\n", millis() - inicio);

  while (millis() - inicio < LED_MINIMO_MS) delay(10);
  digitalWrite(PIN_LED, LOW);
}


// ================================================================

void setup() {
  Serial.begin(115200);
  delay(300);
  pinMode(PIN_LED, OUTPUT);
  digitalWrite(PIN_LED, LOW);

  Serial.println();
  Serial.println("=== nfc-pn532 · firmware " FW_VERSION " ===");

  SPI.begin(PIN_SCK, PIN_MISO, PIN_MOSI, PIN_SS);
  conectarPN532();

  // Dato para el diseño: cuánto tarda una búsqueda sin nada encima
  Etiqueta e;
  const unsigned long t0 = millis();
  const bool algo = buscarEtiqueta(e);
  Serial.printf("Una búsqueda %s tarda %lu ms.\n",
                algo ? "(con algo encima)" : "sin nada encima", millis() - t0);

  Serial.println("Listo: acerca una tarjeta, el llavero o el celular.");
}

// Orden 'i' por el monitor serie: la placa dice su estado sin tener
// que reiniciarla.
void atenderOrdenes() {
  if (!Serial.available() || Serial.read() != 'i') return;
  Serial.printf("[estado] firmware " FW_VERSION " · PN532 firmware %d.%d · "
                "%d detecciones · encendida hace %lu s\n",
                (versionPN532 >> 16) & 0xFF, (versionPN532 >> 8) & 0xFF,
                contador, millis() / 1000);
  Serial.printf("[estado] %d etiqueta(s) distinta(s):\n", numVistas);
  for (int i = 0; i < numVistas; i++) {
    Serial.print("           ");
    imprimirHex(vistas[i].uid, vistas[i].largo);
    Serial.printf(" · %s · %d vez/veces\n", nombreDelTipo(vistas[i].sak), vistas[i].veces);
  }
}

void loop() {
  atenderOrdenes();
  Etiqueta e;
  if (buscarEtiqueta(e)) {
    fallosSeguidos = 0;
    if (!habiaAlgo) {  // recién llegó: se atiende una sola vez
      habiaAlgo = true;
      llegoEn = millis();
      atender(e);
    }
    return;
  }

  if (fallosSeguidos < FALLOS_PARA_RETIRO) fallosSeguidos++;
  if (fallosSeguidos < FALLOS_PARA_RETIRO) return;
  if (habiaAlgo)
    Serial.printf("  · se fue (t = %.1f s; estuvo %.1f s encima)\n",
                  millis() / 1000.0, (millis() - llegoEn) / 1000.0);
  habiaAlgo = false;  // ya lo retiraron

  // Sin nada encima: de vez en cuando, confirmar que el PN532 sigue vivo
  if (millis() - ultimoChequeo > CHEQUEO_SALUD_MS) {
    ultimoChequeo = millis();
    if (nfc.getFirmwareVersion() == 0) {
      Serial.println("⚠ El PN532 dejó de responder. Reconectando...");
      conectarPN532();
    }
  }
}
