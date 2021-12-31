package es.albainformatica.albamobileandroid.ventas

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.Configuracion
import es.albainformatica.albamobileandroid.DatosVtaSeries
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.dao.SeriesDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.maestros.SeriesRvAdapter
import kotlinx.android.synthetic.main.vtas_selecc_serie.*


class SeleccSerieActivity: AppCompatActivity() {
    private lateinit var fRecycler: RecyclerView
    private lateinit var fAdapter: SeriesRvAdapter
    private lateinit var fConfiguracion: Configuracion
    private lateinit var prefs: SharedPreferences
    private var fEmpresaActual: Int = 0

    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.vtas_selecc_serie)

        fConfiguracion = Comunicador.fConfiguracion
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        fEmpresaActual = prefs.getInt("ultima_empresa", 0)

        inicializarControles()
        prepararRecycler()
    }


    private fun inicializarControles() {
        fRecycler = rvSeries

        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.series)
    }


    private fun prepararRecycler() {
        fAdapter  = SeriesRvAdapter(getSeries(), this, object: SeriesRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosVtaSeries) {

                val returnIntent = Intent()
                returnIntent.putExtra("serie", data.serie)
                setResult(RESULT_OK, returnIntent)
                finish()
            }
        })

        fRecycler.layoutManager = GridLayoutManager(this, 2)
        fRecycler.adapter = fAdapter
    }


    private fun getSeries(): MutableList<DatosVtaSeries> {
        val seriesDao: SeriesDao? = MyDatabase.getInstance(this)?.seriesDao()

        return seriesDao?.getAllSeriesEmpresa(fEmpresaActual, fConfiguracion.ejercicio().toInt()) ?: emptyList<DatosVtaSeries>().toMutableList()
    }


}