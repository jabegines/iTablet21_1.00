package es.albainformatica.albamobileandroid.ventas

import android.app.Activity
import android.view.View.OnFocusChangeListener
import android.os.Bundle
import android.content.ContentValues
import android.database.Cursor
import android.view.KeyEvent
import android.view.View
import android.widget.*
import es.albainformatica.albamobileandroid.*
import java.util.*

/**
 * Created by jabegines on 29/05/2014.
 */
class EditarDtosCascada : Activity(), OnFocusChangeListener {
    private lateinit var fDocumento: Documento
    private lateinit var fDtosCascada: DtosCascada
    private lateinit var fConfiguracion: Configuracion
    private var fNumLinea = 0
    private var fIdDto = 0
    private var fOrden = 0
    lateinit var adapterLineas: SimpleCursorAdapter
    private var fFtoCantidad: String = ""
    private var fFtoImpBase: String = ""
    private var fDecPrBase = 0
    private var fEstado: Byte = 0
    private var fPuedoEditar: Boolean = false

    private lateinit var fPorcDto: EditText
    private lateinit var fImpDto: EditText
    private lateinit var fCant1: EditText
    private lateinit var fCant2: EditText
    private lateinit var lvDtosC: ListView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dtos_cascada)

        fDocumento = Comunicador.fDocumento
        fConfiguracion = Comunicador.fConfiguracion
        fDtosCascada = DtosCascada(this)
        val intent = intent
        fNumLinea = intent.getIntExtra("numerolinea", -1)
        fDtosCascada.abrir(fNumLinea)
        inicializarControles()
    }


    override fun onFocusChange(v: View, hasFocus: Boolean) {
        // En este evento se entra varias veces: cuando un control pierde el foco y cuando un control lo toma.
        // hasFocus nos indica si el control ha perdido el foco o lo ha tomado. A mí me interesa controlar cuando
        // algún control toma el foco. En este caso compruebo si algún control anterior tiene datos y, si es así,
        // llamo a aceptarDtCasc() para grabar el descuento. De esta forma consigo que si el usuario introduce algún descuento
        // y pulsa el botón Siguiente en el teclado, se grabe el descuento que ha introducido y vuelva a pedir otro nuevo.
        if (hasFocus) {
            if (v === fImpDto && fPorcDto.text.toString() != "") aceptarDtCasc(null) else if (v === fCant1 && fPorcDto.text.toString() != "")
                aceptarDtCasc(null)
            else if (v === fCant1 && fImpDto.text.toString() != "") aceptarDtCasc(null) else if (v === fCant2 && fPorcDto.text.toString() != "")
                aceptarDtCasc(null)
            else if (v === fCant2 && fImpDto.text.toString() != "") aceptarDtCasc(null)
        }
    }

    private fun inicializarControles() {
        fEstado = est_Vl_Nueva
        fOrden = 1
        fPorcDto = findViewById(R.id.edtVDtc_Porc)
        fPorcDto.onFocusChangeListener = this
        fImpDto = findViewById(R.id.edtVDtc_Imp)
        fImpDto.onFocusChangeListener = this
        fCant1 = findViewById(R.id.edtVDtc_Cant1)
        fCant1.onFocusChangeListener = this
        fCant2 = findViewById(R.id.edtVDtc_Cant2)
        fCant2.onFocusChangeListener = this
        fFtoCantidad = fConfiguracion.formatoDecCantidad()
        fDecPrBase = fConfiguracion.decimalesPrecioBase()
        fFtoImpBase = fConfiguracion.formatoDecImptesBase()

        // Configuramos el objeto de los dtos. en cascada
        fDtosCascada.fIvaIncluido =
            fConfiguracion.ivaIncluido(fDocumento.fEmpresa.toString().toInt())
        fDtosCascada.fAplicarIva = fDocumento.fClientes.fAplIva
        fDtosCascada.fPorcIva = fDocumento.fPorcIva
        fDtosCascada.fDecPrBase = fConfiguracion.decimalesPrecioBase()
        fDtosCascada.fExentoIva = fDocumento.fClientes.fAplIva

        // Si el dto. en cascada viene a través de un rating no permitiremos editar ni borrar ni crear, sólo ver.
        fPuedoEditar = !fDtosCascada.desdeRating
        if (!fPuedoEditar) {
            val btnModificar = findViewById<Button>(R.id.btnModificar)
            val btnBorrar = findViewById<Button>(R.id.btnBorrar)
            val btnAceptar = findViewById<Button>(R.id.btnAceptar)
            btnModificar.isEnabled = false
            btnBorrar.isEnabled = false
            btnAceptar.isEnabled = false
        }
        lvDtosC = findViewById(R.id.lvDtosC)
        prepararListView()
    }

    private fun prepararListView() {
        val columnas = arrayOf("descuento", "importe", "cantidad1", "cantidad2")
        val to =
            intArrayOf(R.id.ly_dc_porcentaje, R.id.ly_dc_impte, R.id.ly_dc_cant1, R.id.ly_dc_cant2)
        adapterLineas = SimpleCursorAdapter(
            this,
            R.layout.ly_dtos_cascada,
            fDtosCascada.cursor,
            columnas,
            to,
            0
        )
        lvDtosC.adapter = adapterLineas

        // Formateamos las columnas
        formatearColumnas()
        lvDtosC.onItemClickListener =
            AdapterView.OnItemClickListener { listView: AdapterView<*>, _: View?, position: Int, _: Long ->
                // Tomamos el campo _id de la fila en la que hemos pulsado.
                val cursor = listView.getItemAtPosition(position) as Cursor
                fIdDto = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
            }
    }

    private fun formatearColumnas() {
        adapterLineas.viewBinder =
            SimpleCursorAdapter.ViewBinder { view: View, cursor: Cursor, column: Int ->
                val tv = view as TextView

                // Formateamos el % de descuento
                if (column == 3) {
                    val sDto =
                        cursor.getString(cursor.getColumnIndex("descuento")).replace(',', '.')
                    val dDto = sDto.toDouble()
                    tv.text = String.format(Locale.getDefault(), "%.2f", dDto)
                    return@ViewBinder true
                }
                // Formateamos el importe de descuento
                if (column == 4) {
                    val sDto = cursor.getString(cursor.getColumnIndex("importe")).replace(',', '.')
                    val dDto = sDto.toDouble()
                    tv.text = String.format(fFtoImpBase, dDto)
                    return@ViewBinder true
                }
                // Cantidad1
                if (column == 5) {
                    val sCajas =
                        cursor.getString(cursor.getColumnIndex("cantidad1")).replace(',', '.')
                    val dCantidad = sCajas.toDouble()
                    tv.text = String.format(fFtoCantidad, dCantidad)
                    return@ViewBinder true
                }
                // Cantidad2
                if (column == 6) {
                    val sCajas =
                        cursor.getString(cursor.getColumnIndex("cantidad2")).replace(',', '.')
                    val dCantidad = sCajas.toDouble()
                    tv.text = String.format(fFtoCantidad, dCantidad)
                    return@ViewBinder true
                }
                false
            }
    }

    fun cancelarDtCasc(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        // Calculamos el descuento equivalente
        if (fDtosCascada.cursor.count > 0) {
            fDocumento.fDtoLin = fDtosCascada.calcularDtoEquiv(fDocumento.fPrecio, fDecPrBase)
                .toDouble()
            // Le decimos al documento que la línea tiene descuentos en cascada.
            fDocumento.fLineaConDtCasc = true
        } else fDocumento.fDtoLin = 0.0
        finish()
    }

    fun aceptarDtCasc(view: View?) {
        view?.getTag(0)          // Para que no dé warning el compilador

        if (fPuedoEditar) {
            if (datosCorrectos()) {
                val values = ContentValues()
                val sPorcDto = fPorcDto.text.toString().replace(',', '.')
                if (sPorcDto == "") values.put("descuento", 0.0) else values.put(
                    "descuento",
                    sPorcDto.toDouble()
                )
                val sImpDto = fImpDto.text.toString().replace(',', '.')
                if (sImpDto == "") values.put("importe", 0.0) else values.put(
                    "importe",
                    sImpDto.toDouble()
                )
                val sCant1 = fCant1.text.toString().replace(',', '.')
                if (sCant1 == "") values.put("cantidad1", 0.0) else values.put(
                    "cantidad1",
                    sCant1.toDouble()
                )
                val sCant2 = fCant2.text.toString().replace(',', '.')
                if (sCant2 == "") values.put("cantidad2", 0.0) else values.put(
                    "cantidad2",
                    sCant2.toDouble()
                )
                if (fEstado == est_Vl_Nueva) {
                    values.put("linea", fNumLinea)
                    values.put("orden", fOrden)
                    fDocumento.insertarDtoCasc(values)
                    fOrden++
                } else if (fEstado == est_Vl_Editar) {
                    fDocumento.editarDtoCasc(values, fIdDto)
                    fEstado = est_Vl_Nueva
                }
                limpiarControles()
                refrescarListView()
                fImpDto.clearFocus()
                fCant1.clearFocus()
                fCant2.clearFocus()
                fPorcDto.requestFocus()
            }
        }
    }

    private fun datosCorrectos(): Boolean {
        var continuar = true
        if (fPorcDto.text.toString() == "") {
            if (fImpDto.text.toString() == "") {
                if (fCant1.text.toString() == "" && fCant2.text.toString() == "") {
                    MsjAlerta(this).alerta(getString(R.string.msj_SinDto))
                    continuar = false
                }
            }
        }
        return continuar
    }

    private fun refrescarListView() {
        fDtosCascada.cursor.close()
        fDtosCascada.abrir(fNumLinea)
        adapterLineas.changeCursor(fDtosCascada.cursor)
    }

    private fun limpiarControles() {
        fPorcDto.setText("")
        fImpDto.setText("")
        fCant1.setText("")
        fCant2.setText("")
    }


    private fun llenarControles() {
        when {
            fDtosCascada.porcDto != 0.0 -> {
                fPorcDto.setText(String.format(Locale.getDefault(), "%.2f", fDtosCascada.porcDto))
                fPorcDto.requestFocus()
            }
            fDtosCascada.impDto != 0.0 -> {
                fImpDto.setText(String.format(Locale.getDefault(), "%.2f", fDtosCascada.impDto))
                fImpDto.requestFocus()
            }
            fDtosCascada.cant1 != 0.0 -> {
                fCant1.setText(String.format(fFtoCantidad, fDtosCascada.cant1))
                fCant2.setText(String.format(fFtoCantidad, fDtosCascada.cant2))
                fCant1.requestFocus()
            }
        }
    }

    fun borrarDtCasc(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        fDtosCascada.borrar(fIdDto)
        refrescarListView()
    }

    fun modificarDtCasc(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        fEstado = est_Vl_Editar
        llenarControles()
    }

    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (fEstado == est_Vl_Browse) {
                // Si el usuario está modificando no dejaré que anule la modificación,
                // ya que trabajo directamente sobre la tabla de líneas. La PDA
                // funciona igual, para salir de una modificación de un documento habrá que grabarlo.
                // Si el listener devuelve true, significa que el evento está procesado, y nadie debe hacer nada más.
                return true
            }
        }
        // Para las demás cosas, se reenvía el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }
}