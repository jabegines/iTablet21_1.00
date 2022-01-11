package es.albainformatica.albamobileandroid.ventas

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.DatosVtaFtos
import es.albainformatica.albamobileandroid.MsjAlerta
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.maestros.Formatos
import es.albainformatica.albamobileandroid.maestros.FormatosRvAdapter
import kotlinx.android.synthetic.main.pedir_dosis.*


class PedirDosis: AppCompatActivity() {
    private lateinit var fRecycler: RecyclerView
    private lateinit var fAdapter: FormatosRvAdapter
    private lateinit var fFormatos: Formatos

    private var fArticulo = 0
    private var fFormatoId: Short = 0
    private var fDosis = ""


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.pedir_dosis)

        fFormatos = Formatos(this)

        val i = intent
        fArticulo = i.getIntExtra("articuloId", 0)

        inicializarControles()
        prepararRecycler()
    }



    private fun inicializarControles() {
        fRecycler = rvFormatos
        edtCantDosis.setText("0")
    }


    private fun prepararRecycler() {
        fAdapter = FormatosRvAdapter(getFormatos(), this, object : FormatosRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosVtaFtos) {
                fFormatoId = data.codigo
                fDosis = data.dosis1
            }
        })

        fRecycler.layoutManager = GridLayoutManager(this, 2)
        fRecycler.adapter = fAdapter
    }

    private fun getFormatos(): MutableList<DatosVtaFtos> {
        val lFormatos: MutableList<DatosVtaFtos> = arrayListOf()
        if (fFormatos.todosLosFormatos()) {
            do {
                val dVtasFtos = DatosVtaFtos()
                dVtasFtos.codigo = fFormatos.cursor.getShort(fFormatos.cursor.getColumnIndex("codigo"))
                dVtasFtos.descripcion = fFormatos.cursor.getString(fFormatos.cursor.getColumnIndex("descr"))
                dVtasFtos.dosis1 = fFormatos.cursor.getString(fFormatos.cursor.getColumnIndex("dosis1"))
                lFormatos.add(dVtasFtos)

            }  while (fFormatos.cursor.moveToNext())

            fFormatos.cursor.moveToFirst()
        }

        return lFormatos
    }


    fun sumarCantDosis(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        var queCantidad = edtCantDosis.text.toString().toInt()
        queCantidad++
        edtCantDosis.setText(queCantidad.toString())
    }

    fun restarCantDosis(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        var queCantidad = edtCantDosis.text.toString().toInt()
        queCantidad--
        edtCantDosis.setText(queCantidad.toString())
    }

    fun cancelarPedirDosis(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }

    fun aceptarPedirDosis(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (puedoSalvar()) {
            // Calculamos aquí el número de piezas y lo enviamos de vuelta
            val numPiezas = (edtCantDosis.text.toString().toInt() * fDosis.toDouble()).toString()

            val returnIntent = Intent()
            returnIntent.putExtra("formatoId", fFormatoId)
            returnIntent.putExtra("piezas", numPiezas)
            setResult(RESULT_OK, returnIntent)
            finish()
        }
    }


    private fun puedoSalvar(): Boolean {

        var resultado = true

        if (fFormatoId == 0.toShort()) {
            MsjAlerta(this).alerta("No ha indicado ningún formato")
            resultado = false

        } else if (edtCantDosis.text.toString() == "0") {
            MsjAlerta(this).alerta("No ha indicado ninguna cantidad")
            resultado = false
        }

        return resultado
    }

}