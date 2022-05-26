package es.albainformatica.albamobileandroid.impresion_informes

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.Configuracion
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.DatosHistMesClte
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.historicos.HistoricoMes


class GrafHcoClte: AppCompatActivity() {
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fHcoMes: HistoricoMes

    private lateinit var fRecycler: RecyclerView
    private lateinit var fAdapter: GrafHcoClteRvAdapter

    private var fCliente: Int = 0




    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.graf_hco_clte)

        val i = intent
        fCliente = i.getIntExtra("cliente", 0)
        fHcoMes = HistoricoMes(this)
        fConfiguracion = Comunicador.fConfiguracion

        inicializarControles()
    }



    private fun inicializarControles() {
        mostrarTotales()

        fHcoMes.abrirHcoClte(fCliente)

        fRecycler = findViewById(R.id.rvGrafHcoClte)
        fRecycler.layoutManager = LinearLayoutManager(this)
        prepararRecycler()
    }


    private fun mostrarTotales() {
        val tvCantAnt = findViewById<TextView>(R.id.tvgrhcoTotCAnt)
        val tvCant = findViewById<TextView>(R.id.tvgrhcoTotCant)
        val tvImpteAnt = findViewById<TextView>(R.id.tvgrhcoTotIAnt)
        val tvImpte = findViewById<TextView>(R.id.tvgrhcoTotImpte)

        if (fHcoMes.totalesHcoClte(fCliente)) {
            var sCantidad = fHcoMes.totalesHistMes.sumCantAnt
            var dCantidad = sCantidad.toDouble()
            tvCantAnt.text = String.format(fConfiguracion.formatoDecCantidad(), dCantidad)

            sCantidad = fHcoMes.totalesHistMes.sumCant
            dCantidad = sCantidad.toDouble()
            tvCant.text = String.format(fConfiguracion.formatoDecCantidad(), dCantidad)

            var sImporte = fHcoMes.totalesHistMes.sumImpteAnt
            var dImporte = sImporte.toDouble()
            tvImpteAnt.text = String.format(fConfiguracion.formatoDecImptesBase(), dImporte)

            sImporte = fHcoMes.totalesHistMes.sumImpte
            dImporte = sImporte.toDouble()
            tvImpte.text = String.format(fConfiguracion.formatoDecImptesBase(), dImporte)
        } else {
            tvCantAnt.text = ""
            tvCant.text = ""
            tvImpteAnt.text = ""
            tvImpte.text = ""
        }
    }


    private fun prepararRecycler() {
        fAdapter  = GrafHcoClteRvAdapter(getHco(), this, object: GrafHcoClteRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosHistMesClte) {
            }
        })

        fRecycler.adapter = fAdapter
    }


    private fun getHco(): List<DatosHistMesClte> {
        return fHcoMes.lDatosHcoMesClte
    }


}