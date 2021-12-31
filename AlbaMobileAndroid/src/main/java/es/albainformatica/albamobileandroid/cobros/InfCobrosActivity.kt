package es.albainformatica.albamobileandroid.cobros

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*


class InfCobrosActivity: Activity() {
    private lateinit var fCobros: CobrosClase
    private lateinit var fConfiguracion: Configuracion
    private lateinit var adapterCobros: SimpleCursorAdapter
    private lateinit var fRecycler: RecyclerView
    private lateinit var fRecResumen: RecyclerView
    private lateinit var fAdapter: InfCobrosRvAdapter
    private lateinit var fAdpResumen: InfCResRvAdapter

    private var fFtoDecImpIva: String = ""
    private var fDesdeFecha: String = ""
    private var fHastaFecha: String = ""


    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.inf_cobros)

        val intent = intent
        fDesdeFecha = intent.getStringExtra("desdeFecha") ?: ""
        fHastaFecha = intent.getStringExtra("hastaFecha") ?: ""
        fCobros = CobrosClase(this)
        fConfiguracion = Comunicador.fConfiguracion
        inicializarControles()
    }


    private fun inicializarControles() {
        fFtoDecImpIva = fConfiguracion.formatoDecImptesIva()

        fRecycler = findViewById(R.id.rvInfCobros)
        fRecResumen = findViewById(R.id.rvResDivInfC)
        prepararRecycler()
        prepararRecResumen()
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.inf_cobros)
    }



    private fun prepararRecycler() {
        fAdapter = InfCobrosRvAdapter(getCobros(), this, object: InfCobrosRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosInfCobros) {
            }
        })

        fRecycler.layoutManager = LinearLayoutManager(this)
        fRecycler.adapter = fAdapter
    }


    private fun getCobros(): MutableList<DatosInfCobros> {
        val lCobros = fCobros.abrirEntreFechas(fDesdeFecha, fHastaFecha)

        var fTotal = 0.0
        for (cobro in lCobros) {
            fTotal += cobro.cobro.replace(',', '.').toDouble()
        }
        val tvTotal = findViewById<TextView>(R.id.tvicTotal)
        tvTotal.text = String.format(fFtoDecImpIva, fTotal)

        return lCobros
    }

    private fun prepararRecResumen() {
        fAdpResumen  = InfCResRvAdapter(getResumDivisas(), this, object: InfCResRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosResCobros) {
            }
        })

        fRecResumen.layoutManager = GridLayoutManager(this, 2)
        fRecResumen.adapter = fAdpResumen
    }


    private fun getResumDivisas(): MutableList<DatosResCobros> {

        return fCobros.abrirResDivisas(fDesdeFecha, fHastaFecha)
    }


}