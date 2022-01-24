package es.albainformatica.albamobileandroid.ventas

import android.app.Activity
import es.albainformatica.albamobileandroid.maestros.LotesClase
import android.os.Bundle
import android.content.Intent
import android.database.Cursor
import android.view.View
import android.widget.TextView
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.Configuracion
import es.albainformatica.albamobileandroid.R

/**
 * Created by jabegines on 14/10/13.
 */
class BuscarLotes: Activity() {
    private lateinit var fLotes: LotesClase
    private lateinit var fConfiguracion: Configuracion

    private lateinit var adapterLineas: SimpleCursorAdapter
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
        inicializarListView()
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.buscar_lote)
    }

    private fun inicializarListView() {
        val cursor = fLotes.getAllLotesArticulo(fArticulo, fConfiguracion.sumarStockEmpresas())
        val listView = findViewById<ListView>(R.id.lvLotes)

        // Si no tenemos datos cerramos la actividad y si sólo tenemos un registro lo devolvemos (si así lo tenemos configurado)
        if (cursor != null && !cursor.moveToFirst()) finish() else if (cursor != null && cursor.count == 1) {
            if (!fMantenerVista) {
                val returnIntent = Intent()
                returnIntent.putExtra(
                    "lote",
                    cursor.getString(cursor.getColumnIndexOrThrow("lote"))
                )
                setResult(RESULT_OK, returnIntent)
                finish()
            }
        }
        val columns = arrayOf("lote", "stock")
        val to = intArrayOf(R.id.lylot_lote, R.id.lylot_stock)
        adapterLineas = SimpleCursorAdapter(this, R.layout.layout_lotes, cursor, columns, to, 0)
        formatearColumnas()
        listView.adapter = adapterLineas

        // Establecemos el evento on click del ListView.
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { listView1: AdapterView<*>, _: View?, position: Int, _: Long ->
                val cursor1 = listView1.getItemAtPosition(position) as Cursor
                // Tomamos el campo lote de la fila en la que hemos pulsado.
                val sLote = cursor1.getString(cursor1.getColumnIndexOrThrow("lote"))
                val returnIntent = Intent()
                returnIntent.putExtra("lote", sLote)
                setResult(RESULT_OK, returnIntent)
                finish()
            }
    }

    private fun formatearColumnas() {
        adapterLineas.viewBinder =
            SimpleCursorAdapter.ViewBinder { view: View, cursor: Cursor, column: Int ->
                val tv = view as TextView

                // Formateamos el stock.
                if (column == 2) {
                    val sStock = cursor.getString(cursor.getColumnIndex("stock")).replace(',', '.')
                    val dStock = sStock.toDouble()
                    tv.text = String.format(fFtoCantidad, dStock)
                    return@ViewBinder true
                }
                false
            }
    }
}