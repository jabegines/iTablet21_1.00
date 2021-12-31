package es.albainformatica.albamobileandroid.maestros

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.EditText
import es.albainformatica.albamobileandroid.R

/**
 * Created by jabegines on 16/03/2016.
 */
class EditarDirClte: Activity() {
    private var fInsertando = false

    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.editar_dir_clte)
        val intent = intent
        fInsertando = intent.getBooleanExtra("nuevo", false)
        // Si estamos editando damos valor a los controles Edit.
        if (!fInsertando) inicializarControles(intent)
    }

    private fun inicializarControles(intent: Intent) {
        val edtDireccion = findViewById<View>(R.id.edtDirDireccion) as EditText
        val edtPoblac = findViewById<View>(R.id.edtDirPobl) as EditText
        val edtCPostal = findViewById<View>(R.id.edtDirCPostal) as EditText
        val edtProvincia = findViewById<View>(R.id.edtDirProvincia) as EditText
        edtDireccion.setText(intent.getStringExtra("direccion"))
        edtPoblac.setText(intent.getStringExtra("poblacion"))
        edtCPostal.setText(intent.getStringExtra("codpostal"))
        edtProvincia.setText(intent.getStringExtra("provincia"))
    }

    fun cancelar(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }

    fun aceptar(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val edtDireccion = findViewById<View>(R.id.edtDirDireccion) as EditText
        val edtPoblac = findViewById<View>(R.id.edtDirPobl) as EditText
        val edtCPostal = findViewById<View>(R.id.edtDirCPostal) as EditText
        val edtProvincia = findViewById<View>(R.id.edtDirProvincia) as EditText
        val returnIntent = Intent()
        returnIntent.putExtra("direccion", edtDireccion.text.toString())
        returnIntent.putExtra("poblacion", edtPoblac.text.toString())
        returnIntent.putExtra("codpostal", edtCPostal.text.toString())
        returnIntent.putExtra("provincia", edtProvincia.text.toString())
        setResult(RESULT_OK, returnIntent)
        finish()
    }
}