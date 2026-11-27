package mx.edu.utez.credencialnfc

import mx.edu.utez.credencialnfc.domain.util.ValidadorRegistro
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidadorRegistroTest {

    @Test
    fun nombre() {
        assertNull(ValidadorRegistro.nombre("Ana López"))
        assertNotNull(ValidadorRegistro.nombre(""))
        assertNotNull(ValidadorRegistro.nombre("Ana"))
        assertNotNull(ValidadorRegistro.nombre("A ".repeat(50)))
    }

    @Test
    fun correo() {
        assertNull(ValidadorRegistro.correo("20253ds07@utez.edu.mx"))
        assertNotNull(ValidadorRegistro.correo("sin-arroba.com"))
        assertNotNull(ValidadorRegistro.correo("a@b"))
        assertNotNull(ValidadorRegistro.correo(""))
    }

    @Test
    fun password() {
        assertNull(ValidadorRegistro.password("clave1234"))
        assertNotNull(ValidadorRegistro.password("corta1"))
        assertNotNull(ValidadorRegistro.password("solamenteletras"))
        assertNotNull(ValidadorRegistro.password("12345678"))
        assertNull(ValidadorRegistro.confirmacion("clave1234", "clave1234"))
        assertNotNull(ValidadorRegistro.confirmacion("clave1234", "clave12345"))
    }

    @Test
    fun matricula() {
        assertNull(ValidadorRegistro.matricula("20253DS042"))
        assertNotNull(ValidadorRegistro.matricula("20253DS42"))
        assertNotNull(ValidadorRegistro.matricula(""))
    }

    @Test
    fun perfil() {
        assertNotNull(ValidadorRegistro.carrera(""))
        assertNotNull(ValidadorRegistro.foto(null))
        assertNull(ValidadorRegistro.foto("file:///foto.jpg"))
        assertNotNull(ValidadorRegistro.gustos(emptyList()))
        assertNull(ValidadorRegistro.intereses(listOf("Robótica")))
        assertNull(ValidadorRegistro.descripcion(""))
        assertNotNull(ValidadorRegistro.descripcion("x".repeat(301)))
    }
}
