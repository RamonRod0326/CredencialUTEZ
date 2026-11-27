package mx.edu.utez.credencialnfc.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import mx.edu.utez.credencialnfc.CredencialApp
import mx.edu.utez.credencialnfc.data.nfc.ConfigCredencial
import mx.edu.utez.credencialnfc.domain.nfc.ProcesadorApdu

class ServicioCredencialNfc : HostApduService() {
    private lateinit var procesador: ProcesadorApdu

    override fun onCreate() {
        super.onCreate()
        val gestor = (application as CredencialApp).contenedor.gestorCredencial
        procesador = ProcesadorApdu(ConfigCredencial.claveLector(), gestor)
    }

    override fun processCommandApdu(apdu: ByteArray, extras: Bundle?): ByteArray = procesador.procesar(apdu)

    override fun onDeactivated(reason: Int) {
        procesador.reiniciar()
    }
}
