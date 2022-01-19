package es.albainformatica.albamobileandroid.impresion_informes

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import es.albainformatica.albamobileandroid.Configuracion
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.historicos.HistoricoMes


class GrafHcoClte: AppCompatActivity() {
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fHcoMes: HistoricoMes

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
        // TODO: hacer recyclerView
        //prepararListView()
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

        fHcoMes.cCursorHco.close()
    }


    /*
    private fun prepararListView() {
        val columnas = arrayOf("codigo", "descr", "sumCantAnt", "sumCant", "sumImpteAnt", "sumImpte")
        val to = intArrayOf(R.id.lygrafhcoCodigo, R.id.lygrafhcoDescr, R.id.lygrafhcoCantAnt, R.id.lygrafhcoCant, R.id.lygrafhcoImpteAnt, R.id.lygrafhcoImpte)

        adapterLineas = SimpleCursorAdapter(this, R.layout.ly_graf_hco_clte, fHcoMes.cCursorHco, columnas, to, 0)
        // Formateamos las columnas.
        formatearColumnas()

        lvLineas.adapter = adapterLineas
        formatearColumnas()
    }


    private fun formatearColumnas() {
        adapterLineas.viewBinder = SimpleCursorAdapter.ViewBinder { view, cursor, column ->
            val tv = view as TextView

            // El orden de las columnas será el que tengan en el cursor que estemos utilizando
            // (en este caso fHcoMes.cCursorHco), comenzando por la cero.
            // Formateamos las cantidades.
            if (column == 5 || column == 6) {
                val sCantidad: String = if (column == 5)
                    cursor.getString(cursor.getColumnIndex("sumCant")).replace(',', '.')
                else
                    cursor.getString(cursor.getColumnIndex("sumCantAnt")).replace(',', '.')

                val dCantidad = java.lang.Double.parseDouble(sCantidad)
                tv.text = String.format(fConfiguracion.formatoDecCantidad(), dCantidad)

                return@ViewBinder true
            }

            // Formateamos los importes.
            if (column == 7 || column == 8) {
                val sImporte: String = if (column == 7)
                    cursor.getString(cursor.getColumnIndex("sumImpte")).replace(',', '.')
                else
                    cursor.getString(cursor.getColumnIndex("sumImpteAnt")).replace(',', '.')

                val dImporte = java.lang.Double.parseDouble(sImporte)
                tv.text = String.format(fConfiguracion.formatoDecImptesBase(), dImporte)

                return@ViewBinder true
            }

            false
        }
    }
    */

}