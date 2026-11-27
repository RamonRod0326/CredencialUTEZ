package mx.edu.utez.credencialnfc

import mx.edu.utez.credencialnfc.data.nfc.ConfigCredencial
import mx.edu.utez.credencialnfc.domain.nfc.FuenteCredencial
import mx.edu.utez.credencialnfc.domain.nfc.LectorReferencia
import mx.edu.utez.credencialnfc.domain.nfc.ProcesadorApdu
import mx.edu.utez.credencialnfc.domain.nfc.ProtocoloCredencial
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.random.Random

class ProtocoloCredencialTest {

    private class FuentePrueba(var matricula: String?) : FuenteCredencial {
        var seleccionado = false
        var resultado: Pair<Boolean, String>? = null
        override fun matriculaActiva() = matricula
        override fun alSeleccionar() { seleccionado = true }
        override fun alRecibirResultado(permitido: Boolean, lector: String) { resultado = permitido to lector }
    }

    private val clave = Random.nextBytes(32)
    private fun lector(clave: ByteArray = this.clave, bloqueadas: Set<String> = emptySet()) =
        LectorReferencia(clave, "P01;Puerta principal", bloqueadas) { Random.nextBytes(8) }

    @Test
    fun vectorDePruebaCoincideConFirmware() {
        assumeTrue("Falta credencial.clave en local.properties", ConfigCredencial.claveConfigurada())
        val firma = ProtocoloCredencial.firmar(ConfigCredencial.claveLector(), byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8), "20253DS007")
        assertEquals("E94D03DA7FBE6442643A46CE1C38C373", ProtocoloCredencial.bytesAHex(firma))
    }

    @Test
    fun credencialActivaEsPermitidaYRecibeResultado() {
        val fuente = FuentePrueba("20253DS007")
        val procesador = ProcesadorApdu(clave, fuente)
        val r = lector().leer(procesador::procesar)
        assertEquals(LectorReferencia.Resultado.Permitido("20253DS007"), r)
        assertTrue(fuente.seleccionado)
        assertEquals(true to "P01;Puerta principal", fuente.resultado)
    }

    @Test
    fun credencialInactivaNoResponde() {
        val fuente = FuentePrueba(null)
        val r = lector().leer(ProcesadorApdu(clave, fuente)::procesar)
        assertEquals(LectorReferencia.Resultado.NoActivada, r)
        assertFalse(fuente.seleccionado)
    }

    @Test
    fun claveDistintaEsDenegada() {
        val fuente = FuentePrueba("20253DS007")
        val r = lector(clave = Random.nextBytes(32)).leer(ProcesadorApdu(clave, fuente)::procesar)
        assertTrue(r is LectorReferencia.Resultado.Denegado)
        assertEquals(false, fuente.resultado?.first)
    }

    @Test
    fun matriculaBloqueadaEsDenegada() {
        val fuente = FuentePrueba("20253DS007")
        val r = lector(bloqueadas = setOf("20253DS007")).leer(ProcesadorApdu(clave, fuente)::procesar)
        assertEquals(LectorReferencia.Resultado.Denegado("20253DS007", "Credencial bloqueada"), r)
    }

    @Test
    fun tokenSinSelectNoSeEntrega() {
        val procesador = ProcesadorApdu(clave, FuentePrueba("20253DS007"))
        val obtener = byteArrayOf(0x80.toByte(), 0x10, 0, 0, 8) + ByteArray(8) + byteArrayOf(0)
        assertArrayEquals(ProtocoloCredencial.SW_NO_ACTIVA, procesador.procesar(obtener))
    }

    @Test
    fun aidDesconocidoYComandosInvalidos() {
        val procesador = ProcesadorApdu(clave, FuentePrueba("20253DS007"))
        val otroAid = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x02, 0x11, 0x22)
        assertArrayEquals(ProtocoloCredencial.SW_AID_DESCONOCIDO, procesador.procesar(otroAid))
        assertArrayEquals(ProtocoloCredencial.SW_LONGITUD, procesador.procesar(byteArrayOf(0x00)))
    }
}
