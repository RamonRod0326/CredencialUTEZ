package mx.edu.utez.credencialnfc.domain.nfc

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object ProtocoloCredencial {
    val AID = hexABytes("F05554455A43524544")

    const val CLA_ISO: Byte = 0x00
    const val CLA_PROPIETARIA: Byte = 0x80.toByte()
    const val INS_SELECT: Byte = 0xA4.toByte()
    const val INS_TOKEN: Byte = 0x10
    const val INS_RESULTADO: Byte = 0x20

    const val LONGITUD_NONCE = 8
    const val LONGITUD_MATRICULA = 10
    const val LONGITUD_FIRMA = 16

    val SW_OK = byteArrayOf(0x90.toByte(), 0x00)
    val SW_NO_ACTIVA = byteArrayOf(0x69, 0x85.toByte())
    val SW_AID_DESCONOCIDO = byteArrayOf(0x6A, 0x82.toByte())
    val SW_LONGITUD = byteArrayOf(0x67, 0x00)
    val SW_INS_NO_SOPORTADA = byteArrayOf(0x6D, 0x00)
    val SW_CLA_NO_SOPORTADA = byteArrayOf(0x6E, 0x00)

    fun firmar(clave: ByteArray, nonce: ByteArray, matricula: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(clave, "HmacSHA256"))
        mac.update(nonce)
        mac.update(matricula.toByteArray(Charsets.US_ASCII))
        return mac.doFinal().copyOf(LONGITUD_FIRMA)
    }

    fun hexABytes(hex: String): ByteArray {
        val limpio = hex.replace(" ", "")
        require(limpio.length % 2 == 0) { "Hex inválido" }
        return ByteArray(limpio.length / 2) { i -> limpio.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }

    fun bytesAHex(bytes: ByteArray): String = bytes.joinToString("") { "%02X".format(it) }
}

interface FuenteCredencial {
    fun matriculaActiva(): String?
    fun alSeleccionar()
    fun alRecibirResultado(permitido: Boolean, lector: String)
}

class ProcesadorApdu(private val clave: ByteArray, private val fuente: FuenteCredencial) {
    private var seleccionado = false

    fun reiniciar() {
        seleccionado = false
    }

    fun procesar(apdu: ByteArray): ByteArray {
        if (apdu.size < 4) return ProtocoloCredencial.SW_LONGITUD
        val cla = apdu[0]
        val ins = apdu[1]
        return when {
            cla == ProtocoloCredencial.CLA_ISO && ins == ProtocoloCredencial.INS_SELECT -> seleccionar(apdu)
            !seleccionado -> ProtocoloCredencial.SW_NO_ACTIVA
            cla != ProtocoloCredencial.CLA_PROPIETARIA -> ProtocoloCredencial.SW_CLA_NO_SOPORTADA
            ins == ProtocoloCredencial.INS_TOKEN -> entregarToken(apdu)
            ins == ProtocoloCredencial.INS_RESULTADO -> recibirResultado(apdu)
            else -> ProtocoloCredencial.SW_INS_NO_SOPORTADA
        }
    }

    private fun seleccionar(apdu: ByteArray): ByteArray {
        seleccionado = false
        if (apdu.size < 5) return ProtocoloCredencial.SW_LONGITUD
        val lc = apdu[4].toInt() and 0xFF
        if (apdu.size < 5 + lc) return ProtocoloCredencial.SW_LONGITUD
        val aid = apdu.copyOfRange(5, 5 + lc)
        if (!aid.contentEquals(ProtocoloCredencial.AID)) return ProtocoloCredencial.SW_AID_DESCONOCIDO
        if (fuente.matriculaActiva() == null) return ProtocoloCredencial.SW_NO_ACTIVA
        seleccionado = true
        fuente.alSeleccionar()
        return ProtocoloCredencial.SW_OK
    }

    private fun entregarToken(apdu: ByteArray): ByteArray {
        if (apdu.size < 5) return ProtocoloCredencial.SW_LONGITUD
        val lc = apdu[4].toInt() and 0xFF
        if (lc != ProtocoloCredencial.LONGITUD_NONCE || apdu.size < 5 + lc) return ProtocoloCredencial.SW_LONGITUD
        val matricula = fuente.matriculaActiva() ?: return ProtocoloCredencial.SW_NO_ACTIVA
        val nonce = apdu.copyOfRange(5, 5 + lc)
        val firma = ProtocoloCredencial.firmar(clave, nonce, matricula)
        return matricula.toByteArray(Charsets.US_ASCII) + firma + ProtocoloCredencial.SW_OK
    }

    private fun recibirResultado(apdu: ByteArray): ByteArray {
        val permitido = apdu[2].toInt() == 0x01
        val lector = if (apdu.size >= 5) {
            val lc = apdu[4].toInt() and 0xFF
            if (apdu.size < 5 + lc) return ProtocoloCredencial.SW_LONGITUD
            String(apdu.copyOfRange(5, 5 + lc), Charsets.UTF_8)
        } else ""
        seleccionado = false
        fuente.alRecibirResultado(permitido, lector)
        return ProtocoloCredencial.SW_OK
    }
}

class LectorReferencia(
    private val clave: ByteArray,
    private val lector: String,
    private val bloqueadas: Set<String> = emptySet(),
    private val generarNonce: () -> ByteArray,
) {
    sealed interface Resultado {
        data class Permitido(val matricula: String) : Resultado
        data class Denegado(val matricula: String?, val motivo: String) : Resultado
        data object NoActivada : Resultado
    }

    fun leer(telefono: (ByteArray) -> ByteArray): Resultado {
        val aid = ProtocoloCredencial.AID
        val select = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte()) + aid + byteArrayOf(0x00)
        val r1 = telefono(select)
        if (r1.estado() == 0x6985) return Resultado.NoActivada
        if (r1.estado() != 0x9000) return Resultado.Denegado(null, "Aplicación no encontrada")

        val nonce = generarNonce()
        val obtener = byteArrayOf(0x80.toByte(), 0x10, 0x00, 0x00, nonce.size.toByte()) + nonce + byteArrayOf(0x00)
        val r2 = telefono(obtener)
        if (r2.estado() == 0x6985) return Resultado.NoActivada
        val esperado = ProtocoloCredencial.LONGITUD_MATRICULA + ProtocoloCredencial.LONGITUD_FIRMA + 2
        if (r2.estado() != 0x9000 || r2.size != esperado) return Resultado.Denegado(null, "Respuesta inválida")

        val matricula = String(r2.copyOfRange(0, ProtocoloCredencial.LONGITUD_MATRICULA), Charsets.US_ASCII)
        val firma = r2.copyOfRange(ProtocoloCredencial.LONGITUD_MATRICULA, esperado - 2)
        val resultado = when {
            !Regex("^20253DS\\d{3}$").matches(matricula) -> Resultado.Denegado(matricula, "Matrícula inválida")
            !java.security.MessageDigest.isEqual(firma, ProtocoloCredencial.firmar(clave, nonce, matricula)) ->
                Resultado.Denegado(matricula, "Firma inválida")
            matricula in bloqueadas -> Resultado.Denegado(matricula, "Credencial bloqueada")
            else -> Resultado.Permitido(matricula)
        }

        val datos = lector.toByteArray(Charsets.UTF_8)
        val p1: Byte = if (resultado is Resultado.Permitido) 0x01 else 0x00
        telefono(byteArrayOf(0x80.toByte(), 0x20, p1, 0x00, datos.size.toByte()) + datos)
        return resultado
    }

    private fun ByteArray.estado(): Int =
        if (size < 2) -1 else ((this[size - 2].toInt() and 0xFF) shl 8) or (this[size - 1].toInt() and 0xFF)
}
