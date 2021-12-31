package es.albainformatica.albamobileandroid.comunicaciones

import android.app.Activity
import android.widget.EditText
import android.os.Bundle
import android.content.Intent
import android.view.View
import es.albainformatica.albamobileandroid.R

class PedirConfigFtp: Activity() {
    private lateinit var edtServidorFtp: EditText
    private lateinit var edtUsuarioFtp: EditText
    private lateinit var edtPasswordFtp: EditText

    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.config_ftp)
        inicializarControles()
    }

    private fun inicializarControles() {
        edtServidorFtp = findViewById<View>(R.id.edtServidorFtp) as EditText
        edtUsuarioFtp = findViewById<View>(R.id.edtUsuarioFtp) as EditText
        edtPasswordFtp = findViewById<View>(R.id.edtPasswordFtp) as EditText
    }

    fun cancelarConfFtp(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }

    fun aceptarConfFtp(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        returnIntent.putExtra("servidorftp", edtServidorFtp.text.toString())
        returnIntent.putExtra("usuarioftp", edtUsuarioFtp.text.toString())
        returnIntent.putExtra("passwordftp", edtPasswordFtp.text.toString())
        setResult(RESULT_OK, returnIntent)
        finish()
    }
}