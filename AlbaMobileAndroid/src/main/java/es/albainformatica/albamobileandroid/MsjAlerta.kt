package es.albainformatica.albamobileandroid

import android.app.AlertDialog
import android.content.Context


/**
 * Created by jabegines on 10/10/13.
 */
class MsjAlerta(var fContexto: Context) {

    fun alerta(cadena: String) {
        val alertbox = AlertDialog.Builder(fContexto)
        alertbox.setTitle("Advertencia")
        alertbox.setIcon(R.drawable.mensaje)
        alertbox.setMessage(cadena)
        alertbox.setPositiveButton("ACEPTAR") { _, _ -> }
        val alert = alertbox.create()
        alert.show()
    }

    fun informacion(cadena: String) {
        val alertbox = AlertDialog.Builder(fContexto)
        alertbox.setTitle("Informacion")
        alertbox.setIcon(R.drawable.mensaje)
        alertbox.setMessage(cadena)
        alertbox.setPositiveButton("ACEPTAR") { _, _ -> }
        alertbox.show()
    }
}