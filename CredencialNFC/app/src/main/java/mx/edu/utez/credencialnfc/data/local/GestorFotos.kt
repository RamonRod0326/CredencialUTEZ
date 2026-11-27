package mx.edu.utez.credencialnfc.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

class GestorFotos(private val context: Context) {
    private val carpetaFotos get() = File(context.filesDir, "fotos").apply { mkdirs() }
    private val carpetaCamara get() = File(context.cacheDir, "camara").apply { mkdirs() }

    fun crearUriParaCamara(): Uri {
        val archivo = File(carpetaCamara, "captura_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    }

    suspend fun guardarFoto(origen: Uri): String = withContext(Dispatchers.IO) {
        val fuente = ImageDecoder.createSource(context.contentResolver, origen)
        val bitmap = ImageDecoder.decodeBitmap(fuente) { decodificador, info, _ ->
            val ancho = info.size.width
            val alto = info.size.height
            val escala = min(1f, LADO_MAXIMO / max(ancho, alto).toFloat())
            decodificador.setTargetSize(
                (ancho * escala).toInt().coerceAtLeast(1),
                (alto * escala).toInt().coerceAtLeast(1),
            )
            decodificador.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val destino = File(carpetaFotos, "perfil_${UUID.randomUUID()}.jpg")
        destino.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, CALIDAD, it) }
        bitmap.recycle()
        limpiarCamara()
        Uri.fromFile(destino).toString()
    }

    fun eliminar(uri: String?) {
        if (uri == null) return
        val ruta = Uri.parse(uri).takeIf { it.scheme == "file" }?.path ?: return
        val archivo = File(ruta)
        if (archivo.parentFile?.canonicalPath == carpetaFotos.canonicalPath) archivo.delete()
    }

    private fun limpiarCamara() {
        carpetaCamara.listFiles()?.forEach { it.delete() }
    }

    private companion object {
        const val LADO_MAXIMO = 1024f
        const val CALIDAD = 85
    }
}
