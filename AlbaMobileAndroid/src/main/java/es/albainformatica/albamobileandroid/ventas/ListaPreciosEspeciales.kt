package es.albainformatica.albamobileandroid.ventas

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.RatingArtDao
import es.albainformatica.albamobileandroid.database.MyDatabase

/**
 * Created by jabegines on 16/02/2018.
 */
class ListaPreciosEspeciales : Activity() {
    private var fCliente = 0
    private lateinit var fConfiguracion: Configuracion

    private lateinit var fRecycler: RecyclerView
    private lateinit var fAdapter: PrecEspRvAdapter


    private var fFtoDecPrBase: String = ""
    private var fFtoDecPrII: String = ""
    private var fDecPrII = 0
    private lateinit var fDocumento: Documento

    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.lista_precios_esp)

        val i = intent
        fCliente = i.getIntExtra("cliente", 0)
        fConfiguracion = Comunicador.fConfiguracion
        fDocumento = Comunicador.fDocumento
        inicializarControles()
    }


    private fun inicializarControles() {
        fFtoDecPrBase = fConfiguracion.formatoDecPrecioBase()
        fFtoDecPrII = fConfiguracion.formatoDecPrecioIva()
        fDecPrII = fConfiguracion.decimalesPrecioIva()

        fRecycler = findViewById(R.id.rvListaPreciosEsp)
        fRecycler.layoutManager = LinearLayoutManager(this)
        prepararRecycler()

        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.pr_especiales)
    }


    private fun prepararRecycler() {
        fAdapter = PrecEspRvAdapter(getPrecios(), fConfiguracion.ivaIncluido(fDocumento.fEmpresa), this,
                                        object: PrecEspRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: ListaPreciosEsp) {
            }
        })
    }


    private fun getPrecios(): List<ListaPreciosEsp> {
        val ratingArtDao: RatingArtDao? = MyDatabase.getInstance(this)?.ratingArtDao()
        return if (fDocumento.fClientes.fRamo > 0)
            ratingArtDao?.getPreciosEspRamo(fDocumento.fTarifaDoc, fCliente) ?: emptyList<ListaPreciosEsp>().toMutableList()
        else
            ratingArtDao?.getPreciosEspeciales(fDocumento.fTarifaDoc, fCliente) ?: emptyList<ListaPreciosEsp>().toMutableList()
    }

}