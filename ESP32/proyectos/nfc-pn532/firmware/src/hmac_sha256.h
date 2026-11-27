// ============================================================
//  HMAC-SHA256 hecho a mano (RFC 2104)
// ============================================================
//  Qué es: una "firma" que solo puede calcular quien tiene la
//  clave. La app firma (nonce + matrícula); el lector repite la
//  cuenta con la misma clave y compara. Si coinciden, la
//  matrícula viene de la app de verdad y no de una copia.
//
//  Por qué a mano: así funciona igual en cualquier versión del
//  core del ESP32 (la especificación avisaba que el atajo de
//  mbedtls no existe en el core 4.x). Solo usa SHA-256 "pelado".
//
//  La receta: HMAC = H( (K xor opad) || H( (K xor ipad) || mensaje ) )
//  con bloques de 64 bytes, ipad = 0x36 repetido y opad = 0x5C.
// ============================================================
#pragma once
#include <string.h>
#include <mbedtls/md.h>

inline bool hmacSha256(const uint8_t *clave, size_t largoClave,
                       const uint8_t *mensaje, size_t largoMensaje,
                       uint8_t salida[32]) {
  const mbedtls_md_info_t *sha256 = mbedtls_md_info_from_type(MBEDTLS_MD_SHA256);
  if (sha256 == nullptr) return false;

  // 1. La clave se lleva a 64 bytes: si es más larga se resume, si es
  //    más corta se rellena con ceros.
  uint8_t k[64] = {0};
  if (largoClave > 64) mbedtls_md(sha256, clave, largoClave, k);
  else memcpy(k, clave, largoClave);

  uint8_t ipad[64], opad[64];
  for (int i = 0; i < 64; i++) {
    ipad[i] = k[i] ^ 0x36;
    opad[i] = k[i] ^ 0x5C;
  }

  mbedtls_md_context_t ctx;
  mbedtls_md_init(&ctx);
  bool ok = mbedtls_md_setup(&ctx, sha256, 0) == 0;

  // 2. Resumen interior: H(ipad || mensaje)
  uint8_t interior[32];
  ok = ok && mbedtls_md_starts(&ctx) == 0
          && mbedtls_md_update(&ctx, ipad, 64) == 0
          && mbedtls_md_update(&ctx, mensaje, largoMensaje) == 0
          && mbedtls_md_finish(&ctx, interior) == 0;

  // 3. Resumen exterior: H(opad || interior)
  ok = ok && mbedtls_md_starts(&ctx) == 0
          && mbedtls_md_update(&ctx, opad, 64) == 0
          && mbedtls_md_update(&ctx, interior, 32) == 0
          && mbedtls_md_finish(&ctx, salida) == 0;

  mbedtls_md_free(&ctx);
  // Borrar de la memoria lo que deriva de la clave
  memset(k, 0, sizeof(k));
  memset(ipad, 0, sizeof(ipad));
  memset(opad, 0, sizeof(opad));
  return ok;
}

// Compara SIN cortar al primer byte distinto. Si cortara, el tiempo
// que tarda revelaría cuántos bytes acertó un atacante, y podría ir
// adivinando la firma byte a byte.
inline bool igualesEnTiempoConstante(const uint8_t *a, const uint8_t *b, size_t n) {
  uint8_t diferencia = 0;
  for (size_t i = 0; i < n; i++) diferencia |= a[i] ^ b[i];
  return diferencia == 0;
}
