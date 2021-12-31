package es.albainformatica.albamobileandroid.ventas

import android.app.Activity
import android.widget.EditText
import android.os.Bundle
import android.content.Intent
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.view.View
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.ancho_nota_clte

/**
 * Created by jabegines on 27/12/2017.
 */
class EditarNota: Activity() {
    private var fNota: String = ""
    private lateinit var edtTexto: EditText


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.editar_texto)

        val i = intent
        fNota = i.getStringExtra("nota") ?: ""
        inicializarControles()
    }

    private fun inicializarControles() {
        edtTexto = findViewById<View>(R.id.edtObserv) as EditText
        edtTexto.filters = arrayOf<InputFilter>(LengthFilter(ancho_nota_clte))
        edtTexto.setText(fNota)
    }

    fun cancelar(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }

    fun salvar(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        returnIntent.putExtra("nota", edtTexto.text.toString())
        setResult(RESULT_OK, returnIntent)
        finish()
    }
}