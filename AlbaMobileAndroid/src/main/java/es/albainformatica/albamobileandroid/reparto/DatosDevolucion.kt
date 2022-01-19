package es.albainformatica.albamobileandroid.reparto

import android.app.Activity
import android.app.AlertDialog
import es.albainformatica.albamobileandroid.historicos.Historico
import es.albainformatica.albamobileandroid.ventas.Documento
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import android.content.SharedPreferences
import android.widget.TextView
import android.widget.EditText
import android.os.Bundle
import android.content.Intent
import android.preference.PreferenceManager
import android.content.DialogInterface
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.TiposIncDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.TiposIncEnt
import java.util.ArrayList

class DatosDevolucion: Activity() {
    private lateinit var fHistorico: Historico
    private lateinit var fDocumento: Documento
    private lateinit var fArticulos: ArticulosClase
    private lateinit var fConfiguracion: Configuracion
    private lateinit var prefs: SharedPreferences

    private lateinit var tvArticulo: TextView
    private lateinit var edtCantidad: EditText
    private lateinit var edtCajas: EditText
    private lateinit var fCodigo: String
    private lateinit var fDescr: String
    private var fArticulo = 0
    private var fLinea = 0
    private lateinit var chsIncidencias: Array<CharSequence>
    private var fTipoIncidencia = 0
    private var fIncidPorDef = 0
    private var fPedirIncid = false
    private lateinit var tvDescrIncidencia: TextView


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.reparto_datos_devol)

        val i = intent
        fCodigo = i.getStringExtra("codigo") ?: ""
        fDescr = i.getStringExtra("descripcion") ?: ""
        fArticulo = i.getIntExtra("articulo", 0)
        fLinea = i.getIntExtra("linea", 0)
        fConfiguracion = Comunicador.fConfiguracion
        fHistorico = Comunicador.fHistorico
        fDocumento = Comunicador.fDocumento
        fArticulos = ArticulosClase(this)
        // Nos posicionamos en el articulo de la linea del historico (nos servira para calcular las unidades por caja, etc.).
        fArticulos.existeArticulo(fArticulo)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        inicializarControles()
        fHistorico.inicializarLinea()
        setArticulo()
    }

    override fun onDestroy() {
        fArticulos.close()
        super.onDestroy()
    }

    private fun inicializarControles() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        tvArticulo = findViewById(R.id.tvDatosDev_Articulo)
        edtCantidad = findViewById(R.id.edtDatosDev_Cant)
        edtCajas = findViewById(R.id.edtDatosDev_Cajas)
        tvDescrIncidencia = findViewById(R.id.tvDescrIncidencia)
        fPedirIncid = prefs.getBoolean("reparto_pedir_incid", false)
        if (fPedirIncid) {
            // Preparamos el array de incidencias por si las usamos para el documento.
            prepararIncidencias()
            fIncidPorDef = (prefs.getString("incid_por_defecto", "0") ?: "0").toInt()
            fTipoIncidencia = fIncidPorDef
            val queItem = localizarIncid(fIncidPorDef)
            if (queItem > -1) tvDescrIncidencia.text = chsIncidencias[queItem].toString()
        } else {
            val btnIncidencia = findViewById<Button>(R.id.btnDatosDev_Incidencia)
            btnIncidencia.visibility = View.GONE
            val tvCapIncidencia = findViewById<TextView>(R.id.tvCapIncidencia)
            tvCapIncidencia.visibility = View.GONE
            tvDescrIncidencia.visibility = View.GONE
        }
        val fPedirCajas = fConfiguracion.pedirCajas()

        // Si el articulo tiene unidades por caja, pedimos la cajas. En caso
        // contrario, no. Siempre y cuando tengamos configurado pedir cajas.
        if (fPedirCajas) {
            prepararCajas()
            if (fArticulos.fUCaja != 0.0) {
                edtCajas.requestFocus()
                imm.showSoftInput(edtCajas, 0)
            } else {
                edtCajas.isEnabled = false
                edtCantidad.requestFocus()
                imm.showSoftInput(edtCantidad, 0)
            }
        } else {
            edtCajas.isEnabled = false
            edtCantidad.requestFocus()
            imm.showSoftInput(edtCantidad, 0)
        }
    }

    private fun prepararCajas() {
        // Pediremos cajas siempre que esté configurado. Establecemos los eventos OnKeyListener y OnFocusChangeListener.
        edtCajas.setOnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
            if (event.action == KeyEvent.ACTION_DOWN
                && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
            ) {
                if (edtCajas.text.toString() == "." || edtCajas.text.toString() == "-") {
                    edtCajas.setText("")
                }
                calcularCantidad()
                return@setOnKeyListener true
            }
            false
        }
    }

    private fun calcularCantidad() {
        var sCajas = edtCajas.text.toString().replace(',', '.')
        if (sCajas == "" || sCajas == "." || sCajas == "-") {
            sCajas = "0.0"
            fDocumento.fCajas = 0.0
        }
        val numCajas = sCajas.toDouble()
        fDocumento.fCantidad = fArticulos.fUCaja * numCajas
        edtCantidad.setText(fDocumento.fCantidad.toString())
    }

    private fun setArticulo() {
        fDocumento.fArticulo = fArticulos.fArticulo
        fDocumento.fCodArt = fArticulos.fCodigo
        fDocumento.fDescr = fArticulos.fDescripcion
        fDocumento.fCodigoIva = fArticulos.fCodIva
        fDocumento.fPorcIva = fArticulos.fPorcIva

        // TODO
        /*
        // Si la línea del histórico tiene formato, mantenemos el mismo.
        if (fHistorico.cHco.getString(fHistorico.cHco.getColumnIndex("formato")) != null)
            fHistorico.fFormatoLin = fHistorico.cHco.getString(fHistorico.cHco.getColumnIndex("formato")).toShort()
        fDocumento.fFormatoLin = fHistorico.fFormatoLin
        var queTexto = "$fCodigo - $fDescr"
        tvArticulo.text = queTexto
        // Si la línea tiene formato, adjuntaremos la descripción del mismo a la del artículo.
        if (fHistorico.cHco.getString(fHistorico.cHco.getColumnIndex("descrfto")) != null) {
            val fDescrFto = fHistorico.cHco.getString(fHistorico.cHco.getColumnIndex("descrfto"))
            queTexto = tvArticulo.text.toString() + " " + fDescrFto
            tvArticulo.text = queTexto
        }
        fDocumento.fPrecio = fHistorico.cHco.getString(fHistorico.cHco.getColumnIndex("precio"))
                .replace(',', '.').toDouble()
        fDocumento.calculaPrecioII()
        fHistorico.fPrecio = fDocumento.fPrecio
        fHistorico.fPrecioII = fDocumento.fPrecioII
        fHistorico.fTasa1 = 0.0
        fHistorico.fTasa2 = 0.0

        // Si la línea del histórico tiene formato, mantenemos el mismo.
        if (fHistorico.cHco.getString(fHistorico.cHco.getColumnIndex("formato")) != null)
            fHistorico.fFormatoLin = fHistorico.cHco.getString(fHistorico.cHco.getColumnIndex("formato")).toShort()
        fHistorico.fDtoLin = fHistorico.cHco.getDouble(fHistorico.cHco.getColumnIndex("dto"))
        fHistorico.fDtoImp = 0.0
        fHistorico.fDtoImpII = 0.0
        fHistorico.fCodigoIva = fDocumento.fCodigoIva
        fHistorico.fCodigo = fDocumento.fCodArt
        fHistorico.fDescr = fDocumento.fDescr
        */
    }

    fun incidenciaDatosDev(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val altBld = AlertDialog.Builder(this)
        altBld.setTitle("Escoger incidencia")
        val queItem = localizarIncid(fIncidPorDef)
        altBld.setSingleChoiceItems(chsIncidencias, queItem) { dialog: DialogInterface, item: Int ->
            val sIncidencia = chsIncidencias[item].toString()
            tvDescrIncidencia.text = sIncidencia
            fTipoIncidencia = sIncidencia.substring(0, 2).toByte().toInt()
            dialog.dismiss()
        }
        val alert = altBld.create()
        alert.show()
    }

    private fun localizarIncid(queTipoInc: Int): Int {
        for (i in chsIncidencias.indices) {
            if (chsIncidencias[i].toString().substring(0, 2).toByte()
                    .toInt() == queTipoInc
            ) return i
        }
        return -1
    }

    fun salvarDatosDev(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        if (puedoSalvar()) {
            fHistorico.fArticulo = fArticulo
            if (edtCajas.isEnabled) if (edtCajas.text.toString() != "") fHistorico.fCajas =
                edtCajas.text.toString().toDouble() * -1 else fHistorico.fCajas =
                0.0 else fHistorico.fCajas = 0.0
            fHistorico.fCantidad = edtCantidad.text.toString().toDouble() * -1
            // Por ahora no pedimos piezas
            fHistorico.fPiezas = 0.0
            if (fPedirIncid) fHistorico.fIncidencia = fTipoIncidencia
            fHistorico.aceptarDatosDevolucion(fLinea)
            val returnIntent = Intent()
            setResult(RESULT_OK, returnIntent)
            finish()
        }
    }

    private fun puedoSalvar(): Boolean {
        var resultado = true
        if (edtCantidad.text.toString() == "") {
            MsjAlerta(this).alerta("No ha indicado cantidad")
            resultado = false
        }
        return resultado
    }

    fun cancelarDatosDev(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }

    private fun prepararIncidencias() {
        val tiposIncDao: TiposIncDao? = MyDatabase.getInstance(this)?.tiposIncDao()
        val lIncidencias = tiposIncDao?.getAllIncidencias() ?: emptyList<TiposIncEnt>().toMutableList()

        val listItems: MutableList<String> = ArrayList()
        for (incidencia in lIncidencias) {
            listItems.add(ponerCeros(incidencia.tipoIncId.toString(), ancho_cod_incidencia) + "  " + incidencia.descripcion)
        }

        chsIncidencias = listItems.toTypedArray()
    }


    // Manejo de los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            val returnIntent = Intent()
            setResult(RESULT_CANCELED, returnIntent)
            finish()
            return true
        }
        // Para las demás cosas, se reenvía el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }


}