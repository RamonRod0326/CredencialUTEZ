package mx.edu.utez.credencialnfc

import android.app.Application
import mx.edu.utez.credencialnfc.di.ContenedorApp

class CredencialApp : Application() {
    lateinit var contenedor: ContenedorApp
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = ContenedorApp(this)
    }
}
