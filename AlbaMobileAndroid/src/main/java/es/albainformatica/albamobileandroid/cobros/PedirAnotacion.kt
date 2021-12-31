package es.albainformatica.albamobileandroid.cobros

import es.albainformatica.albamobileandroid.ponerCeros
import android.app.Activity
import android.widget.EditText
import android.widget.DatePicker
import android.os.Bundle
import android.content.Intent
import android.view.View
import es.albainformatica.albamobileandroid.R

/**
 * Created by jabegines on 26/12/2017.
 */
class PedirAnotacion: Activity() {
    private lateinit var edtAnotacion: EditText
    private lateinit var dpFechaVto: DatePicker


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.pedir_anotacion)
        inicializarControles()
    }

    private fun inicializarControles() {
        edtAnotacion = findViewById<View>(R.id.edt_Anotacion) as EditText
        dpFechaVto = findViewById<View>(R.id.dpFechaVto) as DatePicker
    }


    fun cancelar(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }

    fun hacerAnotacion(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        returnIntent.putExtra("anotacion", edtAnotacion.text.toString())
        val day = dpFechaVto.dayOfMonth
        val month = dpFechaVto.month + 1
        val year = dpFechaVto.year
        val numCeros: Byte = 2
        var sFechaVto = ponerCeros(day.toString(), numCeros)
        sFechaVto += '/'.toString() + ponerCeros(month.toString(), numCeros)
        sFechaVto += "/$year"
        returnIntent.putExtra("fechavto", sFechaVto)
        setResult(RESULT_OK, returnIntent)
        finish()
    }
}