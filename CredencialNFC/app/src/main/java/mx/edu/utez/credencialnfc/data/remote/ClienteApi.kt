package mx.edu.utez.credencialnfc.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import mx.edu.utez.credencialnfc.data.local.SesionDataStore
import mx.edu.utez.credencialnfc.domain.model.ErrorDeDominio
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class SesionExpirada : Exception("Tu sesión terminó. Inicia sesión de nuevo.")

class ClienteApi(val urlBase: String, sesion: SesionDataStore) {

    private val gson = Gson()

    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { cadena ->
            val token = runBlocking { sesion.token() }
            val peticion = cadena.request().newBuilder().apply {
                if (token != null) header("Authorization", "Bearer $token")
            }.build()
            cadena.proceed(peticion)
        }
        .build()

    val api: ApiCredencial = Retrofit.Builder()
        .baseUrl(urlBase)
        .client(http)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ApiCredencial::class.java)

    fun urlCompleta(ruta: String?): String? = ruta?.let { urlBase.trimEnd('/') + it }

    suspend fun <T> llamar(peticion: suspend ApiCredencial.() -> Response<T>): T {
        val respuesta = try {
            api.peticion()
        } catch (e: IOException) {
            throw ErrorDeDominio("No se pudo conectar con el servidor. Revisa que esté encendido y en la misma red.")
        }
        if (respuesta.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            return respuesta.body() ?: (Unit as T)
        }
        if (respuesta.code() == 401 && respuesta.raw().request.url.encodedPath.let { !it.endsWith("/login") }) {
            throw SesionExpirada()
        }
        val mensaje = runCatching {
            gson.fromJson(respuesta.errorBody()?.charStream(), ErrorDto::class.java)?.mensaje
        }.getOrNull()
        throw ErrorDeDominio(mensaje ?: "El servidor respondió con un error (${respuesta.code()}).")
    }
}
