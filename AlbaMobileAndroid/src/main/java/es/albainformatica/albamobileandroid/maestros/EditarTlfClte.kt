package es.albainformatica.albamobileandroid.maestros

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.EditText
import es.albainformatica.albamobileandroid.R

/**
 * Created by jabegines on 15/03/2016.
 */
class EditarTlfClte: Activity() {
    private var fInsertando = false

    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.editar_tlf_clte)

        val intent = intent
        fInsertando = intent.getBooleanExtra("nuevo", false)
        // Si estamos editando damos valor a los controles Edit.
        if (!fInsertando) inicializarControles(intent)
    }

    private fun inicializarControles(intent: Intent) {
        val edtContacto = findViewById<View>(R.id.edtTlfContacto) as EditText
        val edtTlf1 = findViewById<View>(R.id.edtTlfTelf1) as EditText
        val edtTlf2 = findViewById<View>(R.id.edtTlfTelf2) as EditText
        val edtEmail = findViewById<View>(R.id.edtTlfEmailCont) as EditText
        val edtObs = findViewById<View>(R.id.edtTlfObs) as EditText
        edtContacto.setText(intent.getStringExtra("contacto"))
        edtTlf1.setText(intent.getStringExtra("telefono1"))
        edtTlf2.setText(intent.getStringExtra("telefono2"))
        edtEmail.setText(intent.getStringExtra("email"))
        edtObs.setText(intent.getStringExtra("observ"))
    }

    fun cancelar(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }

    fun aceptar(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val edtContacto = findViewById<View>(R.id.edtTlfContacto) as EditText
        val edtTlf1 = findViewById<View>(R.id.edtTlfTelf1) as EditText
        val edtTlf2 = findViewById<View>(R.id.edtTlfTelf2) as EditText
        val edtEmail = findViewById<View>(R.id.edtTlfEmailCont) as EditText
        val edtObs = findViewById<View>(R.id.edtTlfObs) as EditText
        val returnIntent = Intent()
        returnIntent.putExtra("contacto", edtContacto.text.toString())
        returnIntent.putExtra("telefono1", edtTlf1.text.toString())
        returnIntent.putExtra("telefono2", edtTlf2.text.toString())
        returnIntent.putExtra("email", edtEmail.text.toString())
        returnIntent.putExtra("observ", edtObs.text.toString())
        setResult(RESULT_OK, returnIntent)
        finish()
    }
}