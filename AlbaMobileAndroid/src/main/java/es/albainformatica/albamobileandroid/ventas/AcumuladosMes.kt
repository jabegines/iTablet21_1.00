package es.albainformatica.albamobileandroid.ventas


import android.app.Activity
import android.content.Intent
import es.albainformatica.albamobileandroid.historicos.HistoricoMes
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import java.util.*

/**
 * Created by jabegines on 30/01/2017.
 */
class AcumuladosMes: Activity() {
    private lateinit var fHistorico: HistoricoMes
    private lateinit var fConfiguracion: Configuracion

    private lateinit var fRecycler: RecyclerView
    private lateinit var fAdapter: AcumMesRvAdapter

    private var fCliente = 0
    private var fArticulo = 0
    private var fFtoDecCantidad: String = ""


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.acumulados_mes)

        val i = intent
        fCliente = i.getIntExtra("cliente", 0)
        fHistorico = HistoricoMes(this)
        fConfiguracion = Comunicador.fConfiguracion
        inicializarControles()
    }


    private fun inicializarControles() {
        fFtoDecCantidad = fConfiguracion.formatoDecCantidad()

        val fecha = Calendar.getInstance()
        val anyo = fecha[Calendar.YEAR]

        val nombreMes = dimeNombreMesResum(fHistorico.lDatosHMDif[0].mes - 1)

        // Etiquetamos los nombres de los meses
        val tvCantAnt = findViewById<TextView>(R.id.tvacummesCantAnt)
        val tvCantAct = findViewById<TextView>(R.id.tvacummesCantAct)
        var queTexto = nombreMes + " " + (anyo - 1)
        tvCantAnt.text = queTexto
        queTexto = "$nombreMes $anyo"
        tvCantAct.text = queTexto

        fRecycler = findViewById(R.id.rvAcumuladosMes)
        fRecycler.layoutManager = LinearLayoutManager(this)
        prepararRecycler()

        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.acum_mes)
    }

    private fun prepararRecycler() {
        fAdapter = AcumMesRvAdapter(getAcum(), this, object: AcumMesRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosHistMesDif) {
                // Tomamos el campo _id de la fila en la que hemos pulsado.
                fArticulo = data.articuloId
                val i = Intent(this@AcumuladosMes, AcumuladosAnyo::class.java)
                i.putExtra("cliente", fCliente)
                i.putExtra("articulo", fArticulo)
                i.putExtra("codart", data.codigo)
                i.putExtra("descrart", data.descripcion)
                startActivity(i)
            }
        })

        fRecycler.adapter = fAdapter
    }

    private fun getAcum(): List<DatosHistMesDif> {
        fHistorico.abrir(fCliente)
        return fHistorico.lDatosHMDif
    }


}