package es.albainformatica.albamobileandroid.ventas

import es.albainformatica.albamobileandroid.nuevoAlertBuilder
import android.app.Activity
import android.widget.EditText
import android.os.Bundle
import es.albainformatica.albamobileandroid.Comunicador
import android.content.Intent
import android.widget.TextView
import android.content.DialogInterface
import android.view.View
import es.albainformatica.albamobileandroid.R

/**
 * Created by jabegines on 21/11/2017.
 */
class TextoLinea : Activity() {
    private lateinit var fDocumento: Documento
    private lateinit var fTextoLinea: String
    private var fSalvar: Boolean = false
    private lateinit var edtObserv: EditText


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.editar_texto)

        fDocumento = Comunicador.fDocumento
        val i = intent
        fTextoLinea = i.getStringExtra("textolinea") ?: ""
        fSalvar = i.getBooleanExtra("salvar", true)
        inicializarControles()
    }

    private fun inicializarControles() {
        edtObserv = findViewById(R.id.edtObserv)
        edtObserv.setText(fTextoLinea)
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.btn_textolin)
    }

    fun cancelar(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }

    fun salvar(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        fDocumento.fTextoLinea = edtObserv.text.toString()

        // Si hemos cambiado algo en el texto, preguntamos para marcar la línea con el flag 'articulo cambiado'.
        if (fDocumento.fHayArtHabituales) {
            if (edtObserv.text.toString() != fDocumento.textoArtHabitual()) {
                val aldDialog =
                    nuevoAlertBuilder(this, "Marcar", "¿Marcar el texto como modificado?", true)
                aldDialog.setPositiveButton("Si") { _: DialogInterface?, _: Int ->
                    val returnIntent = Intent()
                    if (fSalvar) {
                        fDocumento.fFlag5 = 1
                    } else {
                        returnIntent.putExtra("textoLinea", edtObserv.text.toString())
                        returnIntent.putExtra("flag5", 1)
                    }
                    setResult(RESULT_OK, returnIntent)
                    finish()
                }
                aldDialog.setNegativeButton("No") { _: DialogInterface?, _: Int ->
                    val returnIntent = Intent()
                    if (fSalvar) {
                        fDocumento.fFlag5 = 0
                    } else {
                        returnIntent.putExtra("textoLinea", edtObserv.text.toString())
                        returnIntent.putExtra("flag5", 0)
                    }
                    setResult(RESULT_OK, returnIntent)
                    finish()
                }
                val alert = aldDialog.create()
                alert.show()
            }
        } else {
            val returnIntent = Intent()
            if (fSalvar) {
                fDocumento.fFlag5 = 0
            } else {
                returnIntent.putExtra("textoLinea", edtObserv.text.toString())
                returnIntent.putExtra("flag5", 0)
            }
            setResult(RESULT_OK, returnIntent)
            finish()
        }
    }
}