package mx.edu.utez.credencialnfc

import mx.edu.utez.credencialnfc.domain.util.ValidadorMatricula
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasDominioTest {

    @Test
    fun matriculasValidas() {
        assertTrue(ValidadorMatricula.tieneFormatoValido("20253DS000"))
        assertTrue(ValidadorMatricula.tieneFormatoValido("20253DS999"))
        assertTrue(ValidadorMatricula.tieneFormatoValido(" 20253ds007 "))
    }

    @Test
    fun matriculasInvalidas() {
        assertFalse(ValidadorMatricula.tieneFormatoValido("20253DS12"))
        assertFalse(ValidadorMatricula.tieneFormatoValido("20253DS1000"))
        assertFalse(ValidadorMatricula.tieneFormatoValido("20243DS100"))
        assertFalse(ValidadorMatricula.tieneFormatoValido("20253XX100"))
        assertFalse(ValidadorMatricula.tieneFormatoValido(""))
    }
}
