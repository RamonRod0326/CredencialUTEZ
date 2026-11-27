package mx.edu.utez.credencialnfc.domain.util

enum class Campo { NOMBRE, MATRICULA, CORREO, PASSWORD, CONFIRMAR, CARRERA, FOTO, GUSTOS, INTERESES, DESCRIPCION }

object ValidadorRegistro {
    const val MAX_NOMBRE = 80
    const val MAX_DESCRIPCION = 300
    const val MAX_ETIQUETAS = 10
    private val CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun nombre(valor: String): String? {
        val texto = valor.trim()
        return when {
            texto.isEmpty() -> "Escribe tu nombre completo."
            texto.split(Regex("\\s+")).size < 2 -> "Escribe tu nombre y al menos un apellido."
            texto.length > MAX_NOMBRE -> "Máximo $MAX_NOMBRE caracteres."
            else -> null
        }
    }

    fun matricula(valor: String): String? = when {
        valor.isBlank() -> "Escribe tu matrícula."
        !ValidadorMatricula.tieneFormatoValido(valor) -> "Formato: 20253DSxxx (xxx de 000 a 999)."
        else -> null
    }

    fun correo(valor: String): String? = when {
        valor.isBlank() -> "Escribe tu correo electrónico."
        !CORREO.matches(valor.trim()) -> "Escribe un correo válido."
        else -> null
    }

    fun password(valor: String): String? = when {
        valor.length < 8 -> "Mínimo 8 caracteres."
        valor.none { it.isLetter() } || valor.none { it.isDigit() } -> "Debe incluir letras y números."
        else -> null
    }

    fun confirmacion(password: String, confirmacion: String): String? =
        if (password != confirmacion) "Las contraseñas no coinciden." else null

    fun carrera(valor: String): String? = if (valor.isBlank()) "Selecciona tu carrera." else null

    fun foto(uri: String?): String? = if (uri == null) "Agrega una fotografía para tu credencial." else null

    fun gustos(lista: List<String>): String? = if (lista.isEmpty()) "Elige o escribe al menos un gusto." else null

    fun intereses(lista: List<String>): String? = if (lista.isEmpty()) "Elige o escribe al menos un interés." else null

    fun descripcion(valor: String): String? =
        if (valor.trim().length > MAX_DESCRIPCION) "Máximo $MAX_DESCRIPCION caracteres." else null
}
