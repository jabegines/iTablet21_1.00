package es.albainformatica.albamobileandroid.reparto


import es.albainformatica.albamobileandroid.NuevoAlertBuilder
import android.app.Activity
import es.albainformatica.albamobileandroid.Configuracion
import es.albainformatica.albamobileandroid.historicos.Historico
import android.os.Bundle
import android.content.Intent
import es.albainformatica.albamobileandroid.Comunicador
import android.widget.AdapterView
import android.widget.TextView
import android.database.Cursor
import android.view.KeyEvent
import android.view.View
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import es.albainformatica.albamobileandroid.R


class RepartoDevoluciones: Activity() {
    private lateinit var fConfiguracion: Configuracion
    private lateinit var adapterLineas: SimpleCursorAdapter
    private lateinit var lvLineas: ListView
    private lateinit var fHistorico: Historico
    private var fCliente = 0
    private var fLinea = 0

    private val REQUEST_DATOS_DEVOLUCION = 1


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.reparto_devoluciones)

        val i = intent
        fCliente = i.getIntExtra("cliente", 0)
        fConfiguracion = Comunicador.fConfiguracion
        fHistorico = Historico(this)
        Comunicador.fHistorico = fHistorico
        inicializarControles()
    }


    private fun inicializarControles() {
        lvLineas = findViewById(R.id.lvHcoDev)
        prepararListView()
    }

    private fun prepararListView() {
        val columnas = arrayOf("codigo", "descr", "cantpedida", "cantidad", "cajas", "fecha")
        val to = intArrayOf(
            R.id.lyDevRepCodigo, R.id.lyDevRepDescr, R.id.lyDevRepCantPedida, R.id.lyDevRepCant,
            R.id.lyDevRepCajas, R.id.lyhcoFecha
        )
        fHistorico.abrir(fCliente)
        adapterLineas =
            SimpleCursorAdapter(this, R.layout.ly_dev_reparto, fHistorico.cHco, columnas, to, 0)
        // Formateamos las columnas.
        formatearColumnas()
        lvLineas.adapter = adapterLineas

        // Establecemos el evento on click del ListView.
        lvLineas.onItemClickListener =
            AdapterView.OnItemClickListener { listView, _, position, _ ->
                val cursor = listView.getItemAtPosition(position) as Cursor
                fLinea = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                val i = Intent(this@RepartoDevoluciones, DatosDevolucion::class.java)
                i.putExtra("linea", fLinea)
                i.putExtra("articulo", cursor.getInt(cursor.getColumnIndex("articulo")))
                i.putExtra("codigo", cursor.getString(cursor.getColumnIndex("codigo")))
                i.putExtra("descripcion", cursor.getString(cursor.getColumnIndex("descr")))
                startActivityForResult(i, REQUEST_DATOS_DEVOLUCION)
            }
    }

    private fun formatearColumnas() {
        adapterLineas.viewBinder = SimpleCursorAdapter.ViewBinder { view, cursor, column ->
            val tv = view as TextView

            // El orden de las columnas será el que tengan en el cursor que estemos utilizando
            // (en este caso fHistorico.cHco), comenzando por la cero.
            // Formateamos las cajas y la cantidad.
            if (column == 3 || column == 4) {
                val sCajas: String = if (column == 3) cursor.getString(cursor.getColumnIndex("cajas"))
                    .replace(',', '.') else cursor.getString(cursor.getColumnIndex("cantidad"))
                    .replace(',', '.')
                val dCajas = sCajas.toDouble()
                tv.text = String.format(fConfiguracion.formatoDecCantidad(), dCajas)
                return@ViewBinder true
            }
            // Formateamos la cantidad pedida.
            if (column == 12) {
                if (cursor.getString(column) != null) {
                    val sCant = cursor.getString(column).replace(',', '.')
                    val dCant = sCant.toDouble()
                    tv.text = String.format(fConfiguracion.formatoDecCantidad(), dCant)
                    return@ViewBinder true
                }
            }
            // Formateamos el formato. Si la línea no tiene formato no lo presentaremos.
            if (column == 15) {
                if (cursor.getString(15) != null) {
                    tv.visibility = View.VISIBLE
                } else {
                    tv.visibility = View.GONE
                    return@ViewBinder true
                }
            }
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // Actividad editar linea
        if (requestCode == REQUEST_DATOS_DEVOLUCION) {
            if (resultCode == RESULT_OK) {
                fHistorico.cHco.close()
                fHistorico.abrir(fCliente)
                adapterLineas.changeCursor(fHistorico.cHco)
            }
        }
    }


    fun cancelarRepDev(view: View?) {
        view?.getTag(0)              // Para que no dé warning el compilador

        val aldDialog = NuevoAlertBuilder(this, "Salir", "¿Anular las devoluciones?", true)
        aldDialog.setPositiveButton("Sí") { _, _ ->
            fHistorico.borrar()
            val returnIntent = Intent()
            setResult(RESULT_CANCELED, returnIntent)
            finish()
        }
        val alert = aldDialog.create()
        alert.show()
    }

    fun salvarRepDev(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            cancelarRepDev(null)
            // Si el listener devuelve true, significa que el evento está procesado, y nadie debe hacer nada más.
            return true
        }
        // Para las demás cosas, se reenvía el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }




}