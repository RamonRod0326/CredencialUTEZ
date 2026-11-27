// ============================================================
//  config.h — todo lo que se puede ajustar sin tocar la lógica
// ============================================================
//  La clave NO está aquí: está en secrets.h (ignorado por Git).
// ============================================================
#pragma once
#include <stdint.h>

// --- Pines ---------------------------------------------------------
// Bus SPI hacia el PN532 (ADR-0003). Son los pines por defecto del
// ESP32 (VSPI). Ver 6-esquemas/conexiones-fisicas.md.
constexpr int PIN_SCK  = 18;  // reloj: lo marca SIEMPRE el ESP32
constexpr int PIN_MISO = 19;  // datos del PN532 hacia el ESP32
constexpr int PIN_MOSI = 23;  // datos del ESP32 hacia el PN532
constexpr int PIN_SS   = 5;   // "te estoy hablando a ti"
constexpr int PIN_LED  = 2;   // LED azul de la placa (prueba 02)

// --- La app de credenciales (protocolo fijado por la app; ADR-0004) -
// AID: el "nombre" con el que la app se registra en el celular. Al
// mandarle un SELECT con este nombre, Android despierta a la app.
constexpr uint8_t AID_APP[] = {0xF0, 0x55, 0x54, 0x45, 0x5A, 0x43, 0x52, 0x45, 0x44};

// Lo que el lector le cuenta a la app al final: "ID;nombre". La app lo
// guarda en el historial del estudiante.
constexpr const char *LECTOR_TEXTO = "P01;Acceso principal UTEZ";

// Forma de una matrícula: '#' = dígito, 'A' = letra mayúscula.
// "#####AA###" acepta 20253DS007 y las de otras generaciones/carreras.
constexpr const char *FORMATO_MATRICULA = "#####AA###";

// --- Tiempos -------------------------------------------------------
// Cuántos ms sin ver NADA para dar por retirado lo que estaba encima.
// Evita procesar dos veces la misma credencial si el lector la pierde
// un instante mientras sigue apoyada.
constexpr unsigned long AUSENCIA_PARA_RETIRO_MS = 1000;
constexpr unsigned long LED_PERMITIDO_MS = 1000;  // parpadeo largo: permitido
constexpr unsigned long LED_OTRO_MS      = 150;   // parpadeo corto: todo lo demás
constexpr unsigned long CHEQUEO_SALUD_MS = 10000; // ¿sigue vivo el PN532?
constexpr uint8_t REINTENTOS_BUSQUEDA    = 5;     // 0xFF = buscar para siempre (¡no!)
