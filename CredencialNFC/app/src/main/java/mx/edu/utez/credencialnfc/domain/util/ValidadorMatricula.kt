package mx.edu.utez.credencialnfc.domain.util

object ValidadorMatricula {
    const val LONGITUD = 10

    private val FORMATO = Regex("^20253DS\\d{3}$")

    fun normalizar(matricula: String): String = matricula.trim().uppercase()

    fun tieneFormatoValido(matricula: String): Boolean = FORMATO.matches(normalizar(matricula))
}
