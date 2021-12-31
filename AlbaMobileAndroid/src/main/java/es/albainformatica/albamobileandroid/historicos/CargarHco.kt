package es.albainformatica.albamobileandroid.historicos

import android.app.Activity
import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.content.Intent
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.content.DialogInterface
import android.database.Cursor
import android.graphics.Color
import android.view.KeyEvent
import android.view.View
import android.widget.*
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.ventas.ListaPreciosEspeciales
import es.albainformatica.albamobileandroid.ventas.AcumuladosMes
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by jabegines on 14/10/13.
 */
class CargarHco: Activity() {
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fHistorico: Historico
    private lateinit var adapterLineas: SimpleCursorAdapter

    private var fLinea = 0
    private lateinit var lvLineas: ListView
    private var fIvaIncluido: Boolean = false
    private var fAplicarIva: Boolean = true
    private var fCliente = 0
    private var fDesdeCltes: Boolean = true
    private var fFtoDecPrIva: String = ""
    private var fFtoDecPrBase: String = ""
    private var queBuscar: String = ""
    private lateinit var prefs: SharedPreferences
    private var fAlertarArtNoVend = false
    private var fDiasAlerta = 0
    private var fEmpresaActual = 0


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.ventas_cargarhco)

        val i = intent
        fCliente = i.getIntExtra("cliente", 0)
        fAplicarIva = i.getBooleanExtra("aplicariva", true)
        fDesdeCltes = i.getBooleanExtra("desdecltes", false)
        fEmpresaActual = i.getIntExtra("empresa", 0)
        fConfiguracion = Comunicador.fConfiguracion
        fHistorico = if (fDesdeCltes) Historico(this) else Comunicador.fHistorico
        inicializarControles()
    }

    override fun onDestroy() {
        if (fDesdeCltes) {
            if (this::fHistorico.isInitialized)
                fHistorico.close()
        }
        super.onDestroy()
    }

    private fun inicializarControles() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        lvLineas = findViewById(R.id.lvHcoArticulos)
        queBuscar = ""
        fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresaActual)
        fFtoDecPrIva = fConfiguracion.formatoDecPrecioIva()
        fFtoDecPrBase = fConfiguracion.formatoDecPrecioBase()
        fDiasAlerta = fConfiguracion.diasAlertaArtNoVend()
        fAlertarArtNoVend = fDiasAlerta > 0
        val lyHco_Botones = findViewById<LinearLayout>(R.id.llyHco_Botones)
        if (fDesdeCltes) lyHco_Botones.visibility = View.GONE
        val btnAcumulados = findViewById<Button>(R.id.btnHco_Acumulados)
        if (btnAcumulados != null) {
            if (prefs.getBoolean(
                    "usar_acummes",
                    false
                ) || usarHcoCompSemMeses()
            ) btnAcumulados.visibility = View.VISIBLE else btnAcumulados.visibility = View.GONE
        }
        prepararListView("")
        ocultarTeclado(this)
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.btn_hco)
    }

    fun buscarEnHco(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Buscar artículos")
        val inflater = LayoutInflater.from(this)
        val dialogLayout = inflater.inflate(R.layout.bio_alert_dialog_with_edittext, null)
        builder.setView(dialogLayout)
        val editText = dialogLayout.findViewById<EditText>(R.id.editText)
        editText.requestFocus()
        builder.setPositiveButton("OK") { dialog: DialogInterface?, id: Int ->
            prepararListView(
                editText.text.toString()
            )
        }
        builder.setNegativeButton("Cancelar") { dialog: DialogInterface, id: Int -> dialog.cancel() }
        val alert = builder.create()
        alert.show()
    }

    fun verPrecEspeciales(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val i = Intent(this, ListaPreciosEspeciales::class.java)
        i.putExtra("cliente", fCliente)
        startActivity(i)
    }

    private fun prepararListView(artBuscar: String) {
        val columnas = arrayOf(
            "codigo",
            "descr",
            "piezpedida",
            "cantpedida",
            "cantidad",
            "precio",
            "dto",
            "prneto",
            "cajas",
            "fecha",
            "stock",
            "descrfto",
            "texto"
        )
        val to = intArrayOf(
            R.id.lyhcoCodigo,
            R.id.lyhcoDescr,
            R.id.lyhcoPiezPedida,
            R.id.lyhcoCantPedida,
            R.id.lyhcoCant,
            R.id.lyhcoPrecio,
            R.id.lyhcoDto,
            R.id.lyhcoPrNeto,
            R.id.lyhcoCajas,
            R.id.lyhcoFecha,
            R.id.lyhcoStock,
            R.id.lyhcoFormato,
            R.id.lyhcoTxtArtHab
        )

        // Si queremos buscar una cadena dentro del histórico reabrimos el cursor con la cadena de búsqueda.
        fHistorico.abrirConBusqueda(fCliente, artBuscar)
        adapterLineas =
            SimpleCursorAdapter(this, R.layout.ly_hco_cliente, fHistorico.cHco, columnas, to, 0)
        // Formateamos las columnas.
        formatearColumnas()
        lvLineas.adapter = adapterLineas

        // Establecemos el evento on click del ListView.
        if (!fDesdeCltes) {
            lvLineas.onItemClickListener =
                AdapterView.OnItemClickListener { listView: AdapterView<*>, _: View?, position: Int, _: Long ->
                    // Tomamos el campo _id de la fila en la que hemos pulsado.
                    val cursor = listView.getItemAtPosition(position) as Cursor
                    fLinea = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                    val i = Intent(this@CargarHco, EditarHcoActivity::class.java)
                    i.putExtra("linea", fLinea)
                    i.putExtra("empresa", fEmpresaActual)
                    startActivityForResult(i, REQUEST_EDITARHCO)
                }
        }
    }

    private fun formatearColumnas() {
        adapterLineas.viewBinder =
            SimpleCursorAdapter.ViewBinder { view: View, cursor: Cursor, column: Int ->
                val tv = view as TextView

                // El orden de las columnas será el que tengan en el cursor que estemos utilizando
                // (en este caso fHistorico.cHco), comenzando por la cero.
                // Formateamos el precio.
                if (column == 4 || column == 6) {
                    formatearPrecio(tv, cursor, column)
                    return@ViewBinder true
                }
                // Formateamos las cajas y la cantidad.
                if (column == 3 || column == 7) {
                    val sCajas: String = if (column == 7) cursor.getString(cursor.getColumnIndex("cajas"))
                        .replace(',', '.') else cursor.getString(cursor.getColumnIndex("cantidad"))
                        .replace(',', '.')
                    val dCajas = sCajas.toDouble()
                    tv.text = String.format(fConfiguracion.formatoDecCantidad(), dCajas)
                    return@ViewBinder true
                }
                // Formateamos la cantidad pedida o piezas pedidas.
                if (column == 11 || column == 12) {
                    if (cursor.getString(column) != null) {
                        val sCant = cursor.getString(column).replace(',', '.')
                        val dCant = sCant.toDouble()
                        tv.text = String.format(fConfiguracion.formatoDecCantidad(), dCant)
                        return@ViewBinder true
                    }
                }
                // Formateamos el % de descuento.
                if (column == 5) {
                    val sDto = cursor.getString(cursor.getColumnIndex("dto")).replace(',', '.')
                    val dDto = sDto.toDouble()
                    tv.text = String.format(Locale.getDefault(), "%.2f", dDto)
                    return@ViewBinder true
                }

                // Formateamos el stock.
                if (column == 14) {
                    if (cursor.getString(14) != null) {
                        val dStock = cursor.getString(14).toDouble()
                        tv.text = String.format(fConfiguracion.formatoDecCantidad(), dStock)
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
                // Descripción. Si tenemos configurado alertar de artículos no vendidos y hemos sobrepasado los días la visualizamos en rojo
                if (column == 10) {
                    if (cursor.getString(column) != null) {
                        if (fAlertarArtNoVend) {
                            try {
                                val queFecha = cursor.getString(8)
                                val fechaMenor = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(queFecha) ?: Date()
                                val diferenciaEnMs = System.currentTimeMillis() - fechaMenor.time
                                val dias = diferenciaEnMs / (1000 * 60 * 60 * 24)
                                tv.text = cursor.getString(column)
                                if (dias > fDiasAlerta) tv.setTextColor(Color.RED) else tv.setTextColor(
                                    Color.BLACK
                                )
                                return@ViewBinder true
                            } catch (ex: Exception) {
                                //
                            }
                        }
                    }
                }
                false
            }
    }

    private fun formatearPrecio(tv: TextView, cursor: Cursor, column: Int) {
        val sPrecio: String
        val sPorcIva: String
        var dPrecio: Double
        val dPorcIva: Double
        val sDto = cursor.getString(cursor.getColumnIndex("dto")).replace(',', '.')
        val dDto = sDto.toDouble()
        if (fIvaIncluido && fAplicarIva) {
            // No tenemos un campo para el precio iva incluído, por eso lo calculamos.
            sPrecio = cursor.getString(cursor.getColumnIndex("precio")).replace(',', '.')
            sPorcIva = cursor.getString(cursor.getColumnIndex("porciva")).replace(',', '.')
            dPrecio = sPrecio.toDouble()
            dPorcIva = sPorcIva.toDouble()
            dPrecio += dPrecio * dPorcIva / 100
            if (column == 4) tv.text = String.format(fFtoDecPrIva, dPrecio) else {
                val dPrNeto = dPrecio - dPrecio * dDto / 100
                tv.text = String.format(fFtoDecPrIva, dPrNeto)
            }
        } else {
            sPrecio = cursor.getString(cursor.getColumnIndex("precio")).replace(',', '.')
            dPrecio = sPrecio.toDouble()
            if (column == 4) tv.text = String.format(fFtoDecPrBase, dPrecio) else {
                val dPrNeto = dPrecio - dPrecio * dDto / 100
                tv.text = String.format(fFtoDecPrBase, dPrNeto)
            }
        }
    }

    fun editarHco(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fLinea > 0) {
            val i = Intent(this, EditarHcoActivity::class.java)
            i.putExtra("linea", fLinea)
            i.putExtra("empresa", fEmpresaActual)
            startActivityForResult(i, REQUEST_EDITARHCO)
        } else MsjAlerta(this).alerta(getString(R.string.msj_NoRegSelecc))
    }

    fun verAcumulados(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (prefs.getBoolean("usar_acummes", false)) {
            val i = Intent(this, AcumuladosMes::class.java)
            i.putExtra("cliente", fCliente)
            startActivity(i)
        } else {
            val i = Intent(this, AcumComSemMes::class.java)
            i.putExtra("cliente", fCliente)
            startActivity(i)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // Actividad editar historico.
        if (requestCode == REQUEST_EDITARHCO) {
            if (resultCode == RESULT_OK) fHistorico.abrirConBusqueda(fCliente, queBuscar)
            refrescarLineas()
        }
    }

    fun cancelarHco(view: View?) {
        view?.getTag(0)          // Para que no dé warning el compilador

        if (!fDesdeCltes) {
            val aldDialog = NuevoAlertBuilder(
                this, getString(R.string.tit_cancelar_hco),
                getString(R.string.msj_Abandonar), true
            )
            aldDialog.setPositiveButton(getString(R.string.dlg_si)) { _: DialogInterface?, _: Int ->
                fHistorico.borrar()
                val returnIntent = Intent()
                setResult(RESULT_CANCELED, returnIntent)
                finish()
            }
            val alert = aldDialog.create()
            alert.show()
            ColorDividerAlert(this, alert)
        }
    }

    fun salvarHco(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    private fun refrescarLineas() {
        adapterLineas.changeCursor(fHistorico.cHco)
    }

    fun limpiarHco(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        queBuscar = ""
        fHistorico.abrirConBusqueda(fCliente, "")
        refrescarLineas()
    }

    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            cancelarHco(null)
        }
        // Para las demas cosas, se reenvia el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }

    private fun usarHcoCompSemMeses(): Boolean {
        val bd = BaseDatos(this)
        try {
            bd.writableDatabase.use { dbAlba ->
                dbAlba.rawQuery(
                    "SELECT _id FROM hcoCompSemMes",
                    null
                ).use { cursor -> return cursor.moveToFirst() }
            }
        } finally {
            bd.close()
        }
    }

    companion object {
        // Request de las actividades a las que llamamos.
        private const val REQUEST_EDITARHCO = 1
    }
}