package es.albainformatica.albamobileandroid.ventas

import android.app.Activity
import es.albainformatica.albamobileandroid.maestros.LotesClase
import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.Configuracion
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.entity.LotesEnt
import kotlinx.android.synthetic.main.buscar_lotes.*

/**
 * Created by jabegines on 14/10/13.
 */
class BuscarLotes: Activity() {
    private lateinit var fLotes: LotesClase
    private lateinit var fConfiguracion: Configuracion

    private lateinit var fRecyclerView: RecyclerView
    private lateinit var fAdapter: LotesRvAdapter

    private var fArticulo = 0
    private var fFtoCantidad: String = ""
    private var fMantenerVista = false


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.buscar_lotes)

        fConfiguracion = Comunicador.fConfiguracion

        val i = intent
        fArticulo = i.getIntExtra("articulo", 0)
        fFtoCantidad = i.getStringExtra("formatocant") ?: ""
        fMantenerVista = i.getBooleanExtra("mantenerVista", false)
        fLotes = LotesClase(this)
        inicializarControles()
    }

    private fun inicializarControles() {
        fRecyclerView = rvLotes
        fRecyclerView.layoutManager = LinearLayoutManager(this)
        prepararRecyclerView()

        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.buscar_lote)
    }


    private fun prepararRecyclerView() {
        fAdapter = LotesRvAdapter(getLotes(), fFtoCantidad, this, object: LotesRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: LotesEnt) {
                // Tomamos el campo lote de la fila en la que hemos pulsado.
                val sLote = data.lote
                val returnIntent = Intent()
                returnIntent.putExtra("lote", sLote)
                setResult(RESULT_OK, returnIntent)
                finish()
            }
        })
    }

    private fun getLotes(): List<LotesEnt> {
        fLotes.getAllLotesArticulo(fArticulo, fConfiguracion.sumarStockEmpresas())

        // Si no tenemos datos cerramos la actividad y si sólo tenemos un registro lo devolvemos (si así lo tenemos configurado)
        if (fLotes.lLotes.count() == 0) finish()
        else if (fLotes.lLotes.count() == 1) {
            if (!fMantenerVista) {
                val returnIntent = Intent()
                returnIntent.putExtra("lote", fLotes.lLotes[0].lote)
                setResult(RESULT_OK, returnIntent)
                finish()
            }
        }

        return fLotes.lLotes
    }


}