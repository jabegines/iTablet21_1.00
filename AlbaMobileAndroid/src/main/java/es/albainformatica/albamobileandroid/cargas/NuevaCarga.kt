package es.albainformatica.albamobileandroid.cargas

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.EditText
import android.widget.SimpleCursorAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.maestros.ArticulosActivity
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.maestros.LotesClase
import es.albainformatica.albamobileandroid.ventas.BuscarLotes
import kotlinx.android.synthetic.main.nueva_carga.*
import org.jetbrains.anko.alert
import java.text.SimpleDateFormat
import java.util.*


class NuevaCarga: AppCompatActivity() {
    private lateinit var fLotes: LotesClase
    private lateinit var fArticulos: ArticulosClase
    private lateinit var fConfiguracion: Configuracion

    private lateinit var adapterLineas: SimpleCursorAdapter
    private lateinit var prefs: SharedPreferences

    private var fCargaId = 0
    private var fNumLinea = 0
    private var fUsarTrazabilidad = false
    private var fAlmacen: Short = 0
    private var fPedirCodVal = false
    private var fTerminando = false

    private var fEmpresaActual: Int = 0

    private val fRequestBuscarArt = 1
    private val fRequestBuscarLote = 2



    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.nueva_carga)

        fLotes = LotesClase(this)
        fConfiguracion = Comunicador.fConfiguracion
        fArticulos = ArticulosClase(this)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        inicializarControles()
    }


    override fun onDestroy() {
        fArticulos.close()

        super.onDestroy()
    }


    private fun inicializarControles() {
        activarControlesLinea(false)
        // Preparamos el evento para controlar la pulsación de Enter en el código de artículo
        prepararCodigo()
        fAlmacen = fConfiguracion.almacen()
        fUsarTrazabilidad = fConfiguracion.usarTrazabilidad()
        fPedirCodVal = prefs.getBoolean("cargas_pedir_cod_valid", false)
        fEmpresaActual = prefs.getInt("ultima_empresa", 0)

        // Preparamos el edit de cajas y el del lote
        prepararCajas()
        prepararLote()

        // TODO: cambiar esto
        //prepararListView()
    }


    private fun prepararCodigo() {
        // Preparamos el evento para controlar la pulsación de Enter en el código de artículo
        edtCodArtNCarga.setOnKeyListener(object: View.OnKeyListener {
                override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
                    if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {

                        val edtCodArtNCarga = v as EditText
                        if (fArticulos.existeCodigo(edtCodArtNCarga.text.toString()
                                .uppercase(Locale.getDefault()))) {

                            aceptarArticulo()

                        } else {
                            MsjAlerta(this@NuevaCarga).alerta(resources.getString(R.string.msj_CodNoExiste))
                            edtCodArtNCarga.setText("")
                            edtCodArtNCarga.requestFocus()
                            return false
                        }
                        return true
                    }
                    return false
                }
            }
        )
    }


    private fun prepararCajas() {
        // Establecemos el evento OnKeyListener.
        edtCajasNCarga.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
                // Calcularemos la cantidad a partir de las cajas
                calcularCantidadCj()

                return@OnKeyListener true
            }
            false
        })
    }

    private fun prepararLote() {
        // Establecemos el evento OnKeyListener.
        edtLoteNCarga.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
                // Si el artículo no tiene unidades por caja le daremos el foco a la cantidad al salir del lote.
                if (fArticulos.fUCaja > 0)
                    edtCajasNCarga.requestFocus()
                else {
                    edtCantidadNCarga.requestFocus()
                }

                return@OnKeyListener true
            }
            false
        })
    }

    private fun calcularCantidadCj() {
        var sCajas = edtCajasNCarga.text.toString().replace(',', '.')
        if (sCajas == "") {
            sCajas = "0.0"
        }
        val numCajas = java.lang.Double.parseDouble(sCajas)
        val dCantidad = fArticulos.fUCaja * numCajas
        edtCantidadNCarga.setText(dCantidad.toString())
    }

    // TODO: cambiar el listView por un RecyclerView
    /*
    fun prepararListView() {
        val columnas: Array<String> = arrayOf("codigo", "descr", "lote", "cajas", "cantidad")
        val to: IntArray = intArrayOf(R.id.tvLNCCodigo, R.id.tvLNCDescr, R.id.tvLNCLote, R.id.tvLNCCajas, R.id.tvLNCCantidad)

        cargarCursor()

        adapterLineas = SimpleCursorAdapter(this, R.layout.ly_nueva_carga, fCursor, columnas, to, 0)

        lvNCarga.adapter = adapterLineas

        // Establecemos el evento on click del ListView.
        lvNCarga.onItemClickListener = AdapterView.OnItemClickListener { listView, _, position, _ ->
            val cursor = listView.getItemAtPosition(position) as Cursor
            // Tomamos el id de la fila en la que hemos pulsado.
            fNumLinea = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
        }
    }


    private fun cargarCursor() {

        fCursor = dbAlba.rawQuery("SELECT A.*, B.codigo, B.descr FROM cargasLineas A " +
                " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                " WHERE A.cargaId = $fCargaId", null)

        fCursor.moveToFirst()
    }
    */

    fun buscarArticulo(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val i = Intent(this, ArticulosActivity::class.java)
        i.putExtra("buscando", true)
        // Si tenemos algo en el TextView edtNCarga_CodigoArt lo pasaremos como argumento de la búsqueda
        if (edtCodArtNCarga.text.toString() != "")
            i.putExtra("buscar", edtCodArtNCarga.text.toString())

        startActivityForResult(i, fRequestBuscarArt)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Actividad buscar articulos.
        if (requestCode == fRequestBuscarArt) {
            if (resultCode == Activity.RESULT_OK) {

                val queArticulo = data?.getIntExtra("articulo", -1) ?: -1
                if (fArticulos.existeArticulo(queArticulo)) {

                    aceptarArticulo()
                }
                // Mostramos el teclado.
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            }
        } else if (requestCode == fRequestBuscarLote) {
            if (resultCode == Activity.RESULT_OK) {
                val sQueLote = data?.getStringExtra("lote")
                edtLoteNCarga.setText(sQueLote)
                // Si el artículo no tiene lotes o no hemos escogido ninguno, limpiamos edtLoteNCarga
            } else {
                edtLoteNCarga.setText("")
            }
            edtCajasNCarga.requestFocus()
        }
    }


    private fun aceptarArticulo() {

        edtCodArtNCarga.setText(fArticulos.getCodigo())
        tvDescrNCarga.text = fArticulos.getDescripcion()

        // Una vez tenemos un código de artículo válido, activamos el resto de controles.
        activarControlesLinea(true)

        // Presentaremos automáticamente la ventana de lotes siempre que tengamos en configuración:
        // - Usar cargas
        // - Aviso lotes

        if (fUsarTrazabilidad) {
            if (fArticulos.controlaTrazabilidad()) {
                edtLoteNCarga.isEnabled = true
                edtLoteNCarga.requestFocus()
            } else {
                edtLoteNCarga.isEnabled = false
                edtLoteNCarga.setText("")
                if (fArticulos.fUCaja > 0)
                    edtCajasNCarga.requestFocus()
                else
                    edtCantidadNCarga.requestFocus()
            }
        }
        else {
            if (fArticulos.fUCaja > 0)
                edtCajasNCarga.requestFocus()
            else
                edtCantidadNCarga.requestFocus()
        }
    }


    private fun activarControlesLinea(activar: Boolean) {

        edtLoteNCarga.isEnabled = activar && fUsarTrazabilidad
        edtCajasNCarga.isEnabled = activar
        edtCantidadNCarga.isEnabled = activar

        if (!activar) {
            edtLoteNCarga.setText("")
            edtCajasNCarga.setText("")
            edtCantidadNCarga.setText("")
        }
    }


    fun buscarLote(view: View?) {
        view?.getTag(0)          // Para que no dé warning el compilador

        val i = Intent(this, BuscarLotes::class.java)
        i.putExtra("articulo", fArticulos.getArticulo())
        i.putExtra("formatocant", fConfiguracion.formatoDecCantidad())
        startActivityForResult(i, fRequestBuscarLote)
    }

    // TODO: terminar esto
    /*
    @SuppressLint("SimpleDateFormat")
    fun aceptarNCarga(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (edtCajasNCarga.hasFocus()) {
            calcularCantidadCj()
        }

        // Si no hemos indicado nada no grabaremos. Comprobamos las cajas y la cantidad.
        if ((edtCajasNCarga.text.toString() != "") || (edtCantidadNCarga.text.toString() != "")) {

            if (fCargaId == 0) {
                // Obtenemos la fecha y hora actuales
                val tim = System.currentTimeMillis()
                val df = SimpleDateFormat("dd/MM/yyyy")
                val fFecha = df.format(tim)
                val dfHora = SimpleDateFormat("HH:mm")
                val fHora = dfHora.format(tim)

                val valuesCarga = ContentValues()
                valuesCarga.put("empresa", fEmpresaActual)
                valuesCarga.put("fecha", fFecha)
                valuesCarga.put("hora", fHora)
                valuesCarga.put("esFinDeDia", "F")
                valuesCarga.put("estado", "N")

                fCargaId = dbAlba.insert("cargas", null, valuesCarga).toInt()
            }

            val values = ContentValues()
            values.put("cargaId", fCargaId)
            values.put("articulo", fArticulos.getArticulo())
            values.put("lote", edtLoteNCarga.text.toString())
            values.put("cajas", edtCajasNCarga.text.toString())
            values.put("cantidad", edtCantidadNCarga.text.toString())

            dbAlba.insert("cargasLineas", null, values)
            // Actualizamos stocks en este momento, por si al usuario le da por cortar el programa desde el sistema
            if (fAlmacen.toInt() != 0) {
                actualizarStockCarga(fArticulos.getArticulo(), edtCajasNCarga.text.toString(), edtCantidadNCarga.text.toString(), edtLoteNCarga.text.toString())
            }

            activarControlesLinea(false)
            edtCodArtNCarga.setText("")
            tvDescrNCarga.text = ""
            edtCodArtNCarga.requestFocus()
            // Volvemos a cargar el cursor para refrescar el listView
            cargarCursor()
            adapterLineas.changeCursor(fCursor)
        }
    }
    */

    private fun actualizarStockCarga(queArticulo: Int, queCajas: String, queCantidad: String, queLote: String) {
        val dCantidad = queCantidad.toDouble()
        val dCajas: Double = if (queCajas != "")
            queCajas.toDouble()
        else
            0.0
        fArticulos.actualizarStock(queArticulo, fEmpresaActual.toShort(), dCantidad, dCajas, true)

        // Actualizamos el stock del lote.
        if (queLote != "") {
            fLotes.actStockLote(queArticulo, (queCantidad.toDouble()) * -1, queLote, fEmpresaActual.toShort())
        }
    }

    // TODO: terminar esto
    /*
    fun borrarNCarga(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fNumLinea > 0) {

            if (fAlmacen.toInt() != 0) {
                val queArticulo = fCursor.getInt(fCursor.getColumnIndex("articulo"))
                val dCajas = fCursor.getString(fCursor.getColumnIndex("cajas")).toDouble() * -1
                val dCantidad = fCursor.getString(fCursor.getColumnIndex("cantidad")).toDouble() * -1
                val queLote = fCursor.getString(fCursor.getColumnIndex("lote"))
                val queCajas = dCajas.toString()
                val queCantidad = dCantidad.toString()
                actualizarStockCarga(queArticulo, queCajas, queCantidad, queLote)
            }

            dbAlba.delete("cargasLineas", "_id=$fNumLinea", null)

            fNumLinea = 0
            // Volvemos a cargar el cursor para refrescar el listView
            cargarCursor()
            adapterLineas.changeCursor(fCursor)
        }
        else Toast.makeText(this, "No ha seleccionado ninguna línea", Toast.LENGTH_SHORT).show()
    }
    */

    @SuppressLint("InflateParams")
    fun aceptarTodos(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador
        if (!fTerminando) {
            fTerminando = true

            // Si tenemos configurado pedir código de validación para terminar las cargas lo pedimos.
            // Continuaremos si el código es válido.
            if (fPedirCodVal) {
                val builder = AlertDialog.Builder(this)
                val inflater = layoutInflater
                builder.setTitle("Validar carga")
                val dialogLayout = inflater.inflate(R.layout.cargas_clave_validacion, null)
                val editText = dialogLayout.findViewById<EditText>(R.id.editText)
                builder.setView(dialogLayout)
                builder.setPositiveButton("OK") { _, _ -> validarCodCarga(editText.text.toString()) }
                builder.setNegativeButton("Cancelar") { _, _ -> }
                builder.show()
            } else {
                terminarCarga()
            }
        }
    }


    private fun validarCodCarga(queCodigo: String) {

        // Comprobamos que el código introducido sea el mismo que el del supervisor
        val fCodigo = sha1(queCodigo)
        var fClaveSupervisor = fConfiguracion.claveSupervisor()
        if (fClaveSupervisor == "") fClaveSupervisor = sha1("")

        if (fCodigo.equals(fClaveSupervisor, true)) {
            terminarCarga()
        } else {
            alert("Código inválido, no se ha podido validar la carga") {
                title = "Validar carga"
                positiveButton("OK") { }
            }.show()
        }
    }

    private fun terminarCarga() {
        val returnIntent = Intent()
        returnIntent.putExtra("cargaId", fCargaId)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }



    // Manejo de los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            alert("¿Abandonar la carga?" + "\nPerderá los datos que ha introducido") {
                title = "Abandonar"
                positiveButton("SI") {
                    // TODO: terminar esto
                    //abandonarCarga()
                    }
                negativeButton("NO") { }
            }.show()

            // Si el listener devuelve true, significa que el evento está procesado, y nadie debe hacer nada más.
            return true
        }
        // Para las demás cosas, se reenvía el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }

    // TODO: terminar esto
    /*
    private fun abandonarCarga() {

        if (fCursor.moveToFirst()) {
            do {
                val queArticulo = fCursor.getInt(fCursor.getColumnIndex("articulo"))
                val dCajas = fCursor.getString(fCursor.getColumnIndex("cajas")).toDouble() * -1
                val dCantidad = fCursor.getString(fCursor.getColumnIndex("cantidad")).toDouble() * -1
                val queLote = fCursor.getString(fCursor.getColumnIndex("lote"))
                val queCajas = dCajas.toString()
                val queCantidad = dCantidad.toString()
                actualizarStockCarga(queArticulo, queCajas, queCantidad, queLote)

            } while (fCursor.moveToNext())
        }


        dbAlba.delete("cargas", "cargaId=$fCargaId", null)
        dbAlba.delete("cargasLineas", "cargaId=$fCargaId", null)

        val returnIntent = Intent()
        setResult(Activity.RESULT_CANCELED, returnIntent)
        finish()
    }
    */

}