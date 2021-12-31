package es.albainformatica.albamobileandroid.actividades

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import es.albainformatica.albamobileandroid.R

/**
 * Created by jabegines on 11/10/13.
 */
class PedirPassword: Activity() {
    private var bPassSupervisor: Boolean = false


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.pedir_password)
        bPassSupervisor = false
    }

    fun cancelar(view: View?) {
        view?.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }

    fun aceptar(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val edtPass = findViewById<View>(R.id.edtPassword) as EditText
        var sPassw = edtPass.text.toString()
        if (sPassw != "") {
            if (sPassw.substring(0, 1) == "@") {
                bPassSupervisor = true
                sPassw = sPassw.substring(1, sPassw.length)
            }
            val returnIntent = Intent()
            returnIntent.putExtra("password", sPassw)
            returnIntent.putExtra("supervisor", bPassSupervisor)
            setResult(RESULT_OK, returnIntent)
            finish()
        }
    }

    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            cancelar(null)
            // Al pulsar la tecla arroba "@", entraremos en modo supervisor, o sea,
            // entenderemos que el password introducido es el del supervisor.
        }
        // Para las demás cosas, se reenvía el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }
}