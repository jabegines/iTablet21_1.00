package es.albainformatica.albamobileandroid.ventas

import es.albainformatica.albamobileandroid.DimeNombreMesResum
import android.app.Activity
import es.albainformatica.albamobileandroid.historicos.HistoricoMes
import es.albainformatica.albamobileandroid.Configuracion
import android.os.Bundle
import android.content.Intent
import android.database.Cursor
import android.view.View
import es.albainformatica.albamobileandroid.Comunicador
import android.widget.TextView
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import es.albainformatica.albamobileandroid.R
import java.util.*

/**
 * Created by jabegines on 30/01/2017.
 */
class AcumuladosMes: Activity() {
    private lateinit var lvLineas: ListView
    private var fCliente = 0
    private var fArticulo = 0
    private lateinit var fHistorico: HistoricoMes
    private lateinit var adapterLineas: SimpleCursorAdapter
    private lateinit var fConfiguracion: Configuracion
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

    override fun onDestroy() {
        fHistorico.close()
        super.onDestroy()
    }

    private fun inicializarControles() {
        lvLineas = findViewById(R.id.lvAcumuladosMes)
        fFtoDecCantidad = fConfiguracion.formatoDecCantidad()
        fHistorico.Abrir(fCliente)
        val fecha = Calendar.getInstance()
        val anyo = fecha[Calendar.YEAR]
        val nombreMes = DimeNombreMesResum(fHistorico.getMes() - 1)

        // Etiquetamos los nombres de los meses
        val tvCantAnt = findViewById<TextView>(R.id.tvacummesCantAnt)
        val tvCantAct = findViewById<TextView>(R.id.tvacummesCantAct)
        var queTexto = nombreMes + " " + (anyo - 1)
        tvCantAnt.text = queTexto
        queTexto = "$nombreMes $anyo"
        tvCantAct.text = queTexto
        prepararListView()
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.acum_mes)
    }

    private fun prepararListView() {
        val columnas = arrayOf("codigo", "descr", "cantidadant", "cantidad", "diferencia")
        val to = intArrayOf(
            R.id.lyhcomesCodigo, R.id.lyhcomesDescr,
            R.id.lyhcomesCantAnt, R.id.lyhcomesCant, R.id.lyhcomesDiferencia
        )
        adapterLineas = SimpleCursorAdapter(
            this,
            R.layout.ly_acum_mes,
            fHistorico.cCursorHco,
            columnas,
            to,
            0
        )
        // Formateamos las columnas.
        formatearColumnas()
        lvLineas.adapter = adapterLineas

        // Establecemos el evento on click del ListView.
        lvLineas.onItemClickListener =
            AdapterView.OnItemClickListener { listView: AdapterView<*>, _: View?, position: Int, _: Long ->
                // Tomamos el campo _id de la fila en la que hemos pulsado.
                val cursor = listView.getItemAtPosition(position) as Cursor
                fArticulo = cursor.getInt(cursor.getColumnIndexOrThrow("articulo"))
                val i = Intent(this@AcumuladosMes, AcumuladosAnyo::class.java)
                i.putExtra("cliente", fCliente)
                i.putExtra("articulo", fArticulo)
                i.putExtra("codart", cursor.getString(cursor.getColumnIndex("codigo")))
                i.putExtra("descrart", cursor.getString(cursor.getColumnIndex("descr")))
                startActivity(i)
            }
    }

    private fun formatearColumnas() {
        adapterLineas.viewBinder =
            SimpleCursorAdapter.ViewBinder { view: View, cursor: Cursor, column: Int ->
                val tv = view as TextView

                // El orden de las columnas será el que tengan en el cursor que estemos utilizando
                // (en este caso fHistorico.cHcoMes), comenzando por la cero.
                // Formateamos las cantidades.
                if (column == 2 || column == 3 || column == 4) {
                    val sCantidad: String = when (column) {
                        2 -> cursor.getString(cursor.getColumnIndex("cantidadant"))
                            .replace(
                                ',',
                                '.'
                            )
                        3 -> cursor.getString(cursor.getColumnIndex("cantidad"))
                            .replace(
                                ',',
                                '.'
                            )
                        else -> cursor.getString(cursor.getColumnIndex("diferencia"))
                            .replace(',', '.')
                    }
                    val dCantidad = sCantidad.toDouble()
                    tv.text = String.format(fFtoDecCantidad, dCantidad)
                }
                false
            }
    }
}