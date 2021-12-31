package es.albainformatica.albamobileandroid.comunicaciones

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.content.Intent
import android.view.View
import es.albainformatica.albamobileandroid.MsjAlerta
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.entity.NumExportEnt
import kotlinx.android.synthetic.main.pedir_numexport.*

/**
 * Created by jabegines on 5/12/13.
 */
class PedirNumExport : Activity() {
    private lateinit var fNumExportaciones: NumExportaciones
    private var fNumExport = 0

    private lateinit var fRecycler: RecyclerView
    private lateinit var fAdapter: NumExportRvAdapter


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.pedir_numexport)

        fNumExportaciones = NumExportaciones(this)
        inicializarControles()
        prepararRecyclerView()
    }


    private fun inicializarControles() {
        fNumExport = 0

        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.mni_numeroexp)
    }

    fun cancelar(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }

    fun aceptar(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fNumExport > 0) {
            val returnIntent = Intent()
            returnIntent.putExtra("numexport", fNumExport)
            setResult(RESULT_OK, returnIntent)
            finish()
        } else MsjAlerta(this).alerta(getString(R.string.msj_SinNumExp))
    }

    private fun prepararRecyclerView() {
        fAdapter = NumExportRvAdapter(getExportaciones(), this, object: NumExportRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: NumExportEnt) {
                fNumExport = data.numExport
            }
        })

        fRecycler = rvNumExp
        fRecycler.layoutManager = LinearLayoutManager(this)
        fRecycler.adapter = fAdapter
    }


    private fun getExportaciones(): MutableList<NumExportEnt> {
        return fNumExportaciones.abrir()
    }

}