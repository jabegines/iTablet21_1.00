package es.albainformatica.albamobileandroid.ventas

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.KeyEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.CnfTarifasDao
import es.albainformatica.albamobileandroid.dao.OftCantRangosDao
import es.albainformatica.albamobileandroid.dao.SeriesDao
import es.albainformatica.albamobileandroid.dao.TiposIncDao
import es.albainformatica.albamobileandroid.database.MyDatabase.Companion.getInstance
import es.albainformatica.albamobileandroid.entity.CnfTarifasEnt
import es.albainformatica.albamobileandroid.entity.DtosLinFrasEnt
import es.albainformatica.albamobileandroid.entity.DtosLineasEnt
import es.albainformatica.albamobileandroid.entity.TiposIncEnt
import es.albainformatica.albamobileandroid.historicos.VerHcoArtCliente
import es.albainformatica.albamobileandroid.maestros.AlmacenesClase
import es.albainformatica.albamobileandroid.maestros.ArticulosActivity
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.maestros.FichaArticuloActivity
import es.albainformatica.albamobileandroid.oldcatalogo.CatalogoArticulos
import es.albainformatica.albamobileandroid.oldcatalogo.CatalogoCatalogos
import es.albainformatica.albamobileandroid.oldcatalogo.CatalogoGruposDep
import java.io.File
import java.util.*

/**
 * Created by jabegines on 06/06/2014.
 */
class VentasDatosLinea: Activity() {
    private var ofCantRangosDao: OftCantRangosDao? = getInstance(this)?.oftCantRangosDao()
    private val cnfTarifasDao: CnfTarifasDao? = getInstance(this)?.cnfTarifasDao()

    private lateinit var fDocumento: Documento
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fArticulos: ArticulosClase
    private lateinit var fAlmacenes: AlmacenesClase
    private lateinit var prefs: SharedPreferences
    private lateinit var fDtosCascada: DtosCascada

    private var fEditAlmEnabled: Boolean = false
    private var fPedirCajas: Boolean = false
    private var fPedirPiezas: Boolean = false
    private var fPedirTarifa: Boolean = false
    private var fHeElegidoTarifa: Boolean = false
    private var fTarifaAnt: Short = 0
    private var fVendTrfPiezas = false
    private var fEstado: Byte = 0
    private var fIvaIncluido: Boolean = false
    private var fAplicarIva: Boolean = true
    private var fUsarTasa1: Boolean = false
    private var fUsarTasa2: Boolean = false
    private var fLinea = 0
    private var fArtConEnlace: Boolean = false
    private var fCodArtEnlazado: String = ""
    private var fVendiendoEnlace: Boolean = false
    private var fCantParaEnlace: Double = 0.0

    private var fDecPrBase = 0
    private var fFtoDecPrBase: String = ""
    private var fFtoDecPrII: String = ""
    private var fInicializando = true
    private var fMantenerVistaLotes = false
    private var fPedirDosis = false

    // EditText's de la actividad.
    private lateinit var edtCodArt: EditText
    private lateinit var tvDescr: TextView
    private lateinit var imvBuscar: ImageView
    private lateinit var imgTrfPiezas: ImageView
    private lateinit var tvTarifa: TextView
    private lateinit var tvFormato: TextView
    private lateinit var edtAlmacen: EditText
    private lateinit var edtCajas: EditText
    private lateinit var edtCantidad: EditText
    private lateinit var edtPrecio: EditText
    private lateinit var edtDto: EditText
    private lateinit var tvPrNeto: TextView
    private lateinit var edtLote: EditText
    private lateinit var tvLote: TextView
    private lateinit var imgArticulo: ImageView
    private lateinit var imgLote: ImageView
    private lateinit var tvPiezas: TextView
    private lateinit var edtPiezas: EditText
    private lateinit var btnTextoLinea: Button
    private lateinit var imgBuscaArt: ImageView
    private lateinit var chsTarifas: Array<CharSequence>
    private lateinit var chsFormatos: Array<CharSequence>
    private lateinit var chsIncidencias: Array<CharSequence>


    // Ruta de la carpeta im??genes.
    private var carpetaImagenes: String = ""

    // Request de las actividades a las que llamamos.
    private val fRequestBuscarArt = 1
    private val fRequestBuscarLote = 2
    private val fRequestDtosCascada = 3
    private val fRequestPedirDosis = 4



    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.ventas_datoslinea)

        fDocumento = Comunicador.fDocumento
        fConfiguracion = Comunicador.fConfiguracion
        fArticulos = ArticulosClase(this)
        fDtosCascada = DtosCascada(this)
        fAlmacenes = AlmacenesClase(this)
        val i = intent
        fEstado = i.getByteExtra("estado", est_Vl_Nueva)

        fVendiendoEnlace = false
        fCantParaEnlace = 0.0
        fHeElegidoTarifa = false
        fVendTrfPiezas = false
        fPedirDosis = fConfiguracion.hayElaboracionLacteos() && fConfiguracion.usarPiezas()

        // fLinea nos servir?? para cuando estemos modificando una l??nea.
        fLinea = i.getIntExtra("numlinea", 0)
        inicializarControles()
        fInicializando = false
    }


    private fun inicializarControles() {
        val tvNombreClte = findViewById<TextView>(R.id.tvVL_Clte)
        val tvNComClte = findViewById<TextView>(R.id.tvVL_NComClte)
        val tvTipoDoc = findViewById<TextView>(R.id.tvVL_TipoDoc)
        val tvSerieNum = findViewById<TextView>(R.id.tvVL_SerieNum)
        val queNombreClte = fDocumento.fClientes.fCodigo + " - " + fDocumento.nombreCliente()
        tvNombreClte.text = queNombreClte
        tvNComClte.text = fDocumento.nombreComClte()
        tvTipoDoc.text = tipoDocAsString(fDocumento.fTipoDoc)
        val queSerieNum = fDocumento.serie + '/' + fDocumento.numero
        tvSerieNum.text = queSerieNum
        edtCodArt = findViewById(R.id.edtVDL_CodigoArt)
        tvDescr = findViewById(R.id.tvVDL_Descr)
        imvBuscar = findViewById(R.id.imvVDL_Buscar)
        imgTrfPiezas = findViewById(R.id.imgTarifaPiezas)
        tvTarifa = findViewById(R.id.tvVDL_Tarifa)
        tvFormato = findViewById(R.id.tvVDL_Formato)
        edtAlmacen = findViewById(R.id.edtVDL_Almacen)
        edtCajas = findViewById(R.id.edtVDL_Cajas)
        edtCantidad = findViewById(R.id.edtVDL_Cantidad)
        edtPrecio = findViewById(R.id.edtVDL_Precio)
        edtDto = findViewById(R.id.edtVDL_Dto)
        tvPrNeto = findViewById(R.id.tvVDL_PrNeto)
        tvPrNeto.text = ""
        edtLote = findViewById(R.id.edtVDL_Lote)
        tvLote = findViewById(R.id.tvVDL_Lote)
        if (fConfiguracion.fTamanyoPantLargo) imgArticulo = findViewById(R.id.imgVLArtic)
        imgLote = findViewById(R.id.imgVDL_Lote)
        tvPiezas = findViewById(R.id.tvVDL_Piezas)
        edtPiezas = findViewById(R.id.edtVDL_Piezas)
        btnTextoLinea = findViewById(R.id.btnVDL_TextoLin)
        imgBuscaArt = findViewById(R.id.imgBuscaArticulo)
        carpetaImagenes = dimeRutaImagenes(this)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val fSeriesDao: SeriesDao? = getInstance(this)?.seriesDao()
        val queFlagSerie = fSeriesDao?.getFlag(fDocumento.serie, fDocumento.fEjercicio) ?: 0
        val fForzarPrIvaIncl = queFlagSerie and FLAGSERIE_FORZAR_PR_IVA_INCL > 0
        fIvaIncluido = fForzarPrIvaIncl || fConfiguracion.ivaIncluido(fDocumento.fEmpresa)

        fAplicarIva = fDocumento.fClientes.fAplIva
        fUsarTasa1 = fConfiguracion.usarTasa1()
        fUsarTasa2 = fConfiguracion.usarTasa2()
        fPedirTarifa = fConfiguracion.pedirTarifa()
        if (fPedirTarifa) prepararTarifas()

        // fEditAlmEnabled nos servir?? para saber si tenemos el control edtAlmacen enabled o no
        fEditAlmEnabled = fConfiguracion.pedirAlmPorLinPresup() && fDocumento.fTipoDoc == TIPODOC_PRESUPUESTO
        fPedirCajas = fConfiguracion.pedirCajas()
        fPedirPiezas = fConfiguracion.usarPiezas()
        fDecPrBase = fConfiguracion.decimalesPrecioBase()
        fFtoDecPrBase = fConfiguracion.formatoDecPrecioBase()
        fFtoDecPrII = fConfiguracion.formatoDecPrecioIva()
        prepararEdits()
        // Preparamos el array de incidencias por si usamos l??neas sin cargo o vendemos en negativo.
        prepararIncidencias()
        activarDesactivarControles()

        // Llamamos directamente a buscarArticulo si la l??nea es nueva (si as?? lo tenemos configurado).
        if (fEstado == est_Vl_Nueva) {
            if (!prefs.getBoolean("ventas_pedir_codigo", false)) buscarArticulo(null)
        }
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.datoslinea)
    }

    private fun prepararEdits() {
        // Preparo el tipo y las cajas antes que el c??digo porque me viene bien saber si ??stos est??n enabled o no.
        prepararCajas()
        prepararCantidad()
        if (fEditAlmEnabled) prepararAlmacen()
        //prepararCodigo();
        prepararDtos()
        prepararLote()
        prepararPiezas()

        // Establecemos los eventos que controlan los formatos de la cantidad y el precio.
        edtCantidad.keyListener = DigitsKeyListener.getInstance(true, true)
        edtPrecio.keyListener = DigitsKeyListener.getInstance(true, true)

        // Permiso para modificar precios.
        if (!fConfiguracion.modificarPrecios()) {
            edtPrecio.isEnabled = false
            edtPrecio.isFocusable = false
            // Si no pedimos precio y s?? dtos., hacemos el nextfocusdown a edtCantidad.
            if (fConfiguracion.pedirDtos()) edtCantidad.nextFocusDownId = edtDto.id
        }
    }

    private fun prepararCantidad() {
        // Preparamos el evento onKey de la cantidad para poder calcular ofertas cuando la cambiamos.
        if (hayOftasPorCantidad()) {
            // He tenido que comentar el evento setOnKeyListener y crear el addTextChangedListener porque
            // el setOnKeyListener no funciona en la tablet con Android 8 (con Android 4.4.2 s?? funciona)
            edtCantidad.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (!fInicializando) {
                        val sCantidad = edtCantidad.text.toString().replace(',', '.')
                        if (sCantidad != "" && sCantidad != "." && sCantidad != "-") {
                            fDocumento.fCantidad = sCantidad.toDouble()
                            calcularPrecioYDto(true)
                        }
                    }
                }
            })
        }
        edtCantidad.setOnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
            if (event.action == KeyEvent.ACTION_DOWN
                && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
            ) {
                if (edtCantidad.text.toString() == "." || edtCantidad.text.toString() == "-") {
                    edtCantidad.setText("")
                }
                return@setOnKeyListener true
            }
            false
        }
    }

    private fun prepararCajas() {
        // Pediremos cajas siempre que est?? configurado. Establecemos los eventos OnKeyListener y OnFocusChangeListener.
        if (fPedirCajas) {
            edtCajas.setOnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
                if (event.action == KeyEvent.ACTION_DOWN
                    && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
                ) {
                    if (edtCajas.text.toString() == "." || edtCajas.text.toString() == "-") {
                        edtCajas.setText("")
                    }
                    // Calcularemos la cantidad a partir de las cajas siempre que estemos en una l??nea nueva
                    // o lo tengamos configurado as??.
                    if (fEstado == est_Vl_Nueva || fConfiguracion.aconsUnidCajaModif()) calcularCantidadYTrfCj()

                    // Si tenemos configurado no pedir la cantidad al indicar cajas (Configuracion.noModifCantidad()),
                    // buscamos el siguiente control disponible y, si no lo hay, terminamos la l??nea.
                    if (fConfiguracion.noModifCantidad()) {
                        if (edtCajas.text.toString() != "" &&
                            edtCajas.text.toString().replace(',', '.').toDouble() != 0.0
                        ) {
                            edtCantidad.isEnabled = false
                            when {
                                edtPiezas.isEnabled -> edtPiezas.requestFocus()
                                edtPrecio.isFocusable -> edtPrecio.requestFocus()
                                edtDto.isEnabled -> edtDto.requestFocus()
                                edtLote.visibility == View.VISIBLE -> edtLote.requestFocus()
                                else -> aceptarLinea(null)
                            }
                        }
                    }
                    return@setOnKeyListener true
                }
                false
            }
            edtCajas.onFocusChangeListener =
                OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                    if (!hasFocus) {
                        if (edtCajas.text.toString() == "." || edtCajas.text.toString() == "-") {
                            edtCajas.setText("")
                        }
                        if (fConfiguracion.noModifCantidad()) {
                            edtCantidad.isEnabled = edtCajas.text.toString() == "" ||
                                    edtCajas.text.toString().replace(',', '.').toDouble() == 0.0
                        }
                    }
                }
        } else edtCajas.isEnabled = false
    }

    fun enterEnArticulo(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        if (fArticulos.existeCodigo(edtCodArt.text.toString().uppercase(Locale.getDefault()))) {
            // No s?? por qu??, pero si quitamos esta l??nea la aplicaci??n da error
            edtCodArt.setText(fArticulos.fCodigo)

            // Si el art??culo usa formatos no permitiremos continuar hasta haber elegido uno.
            if (fArticulos.usarFormatos()) {
                elegirFormato()
            } else {
                aceptarArticulo()
            }
        } else {
            MsjAlerta(this@VentasDatosLinea).alerta(resources.getString(R.string.msj_CodNoExiste))
            edtCodArt.setText("")
            edtCodArt.requestFocus()
        }
    }

    private fun prepararAlmacen() {
        edtAlmacen.setOnKeyListener { v: View, keyCode: Int, event: KeyEvent ->
            if (event.action == KeyEvent.ACTION_DOWN
                && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
            ) {
                val edtCodAlmacen = v as EditText
                if (fAlmacenes.existe(edtCodAlmacen.text.toString())) {
                    edtCodAlmacen.setText(
                        ponerCeros(
                            edtCodAlmacen.text.toString(),
                            ancho_cod_almacen
                        )
                    )

                    // Buscamos el siguiente control al que tenemos que darle el foco.
                    val queId = edtCodAlmacen.nextFocusDownId
                    val queView = findViewById<View>(queId)
                    queView?.requestFocus()
                    return@setOnKeyListener true
                } else {
                    MsjAlerta(this@VentasDatosLinea).alerta(resources.getString(R.string.msj_CodNoExiste))
                    edtCodAlmacen.setText("")
                    edtCodAlmacen.requestFocus()
                    return@setOnKeyListener false
                }
            }
            false
        }
    }

    private fun prepararDtos() {
        val pedirDtos = fConfiguracion.pedirDtos()
        val dtosCascada = fConfiguracion.dtosCascada()
        if (!pedirDtos) {
            edtDto.isEnabled = false
            edtDto.isFocusable = false
        }
        edtDto.addTextChangedListener(NumberTextWatcher(edtDto, 2, 2))

        // Establecemos el evento para cuando el edit toma el foco.
        if (pedirDtos && dtosCascada) {
            edtDto.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (hasFocus) {
                    pedirDtosCascada()
                }
            }
        }
    }

    private fun prepararLote() {
        if (fDocumento.fTipoDoc == TIPODOC_FACTURA || fDocumento.fTipoDoc == TIPODOC_ALBARAN) {
            if (!fConfiguracion.usarTrazabilidad()) {
                tvLote.visibility = View.GONE
                edtLote.visibility = View.GONE
                imgLote.visibility = View.GONE
            }
        } else {
            tvLote.visibility = View.GONE
            edtLote.visibility = View.GONE
            imgLote.visibility = View.GONE
        }
    }

    private fun prepararPiezas() {
        if (fConfiguracion.usarPiezas()) {
            // Establecemos el eventa para cuando toma el foco, por si tenemos que pedir las dosis
            edtPiezas.onFocusChangeListener =
                OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                    if (hasFocus) {
                        if (fPedirDosis) {
                            if (fArticulos.venderPorDosis() && fArticulos.usarPiezas()) {
                                val i = Intent(this@VentasDatosLinea, PedirDosis::class.java)
                                i.putExtra("articulo", fArticulos.fArticulo)
                                startActivityForResult(i, fRequestPedirDosis)
                            }
                        }
                    }
                }
        } else {
            tvPiezas.visibility = View.GONE
            edtPiezas.visibility = View.GONE
        }
    }

    private fun pedirDtosCascada() {
        val i = Intent(this, EditarDtosCascada::class.java)
        if (fEstado == est_Vl_Nueva) {
            i.putExtra("numerolinea", -1)
        } else {
            i.putExtra("numerolinea", fLinea)
        }

        // Establecemos ahora el precio de la l??nea porque nos har?? falta para calcular los dtos. en casdada
        setPrecioLineaDoc()
        startActivityForResult(i, fRequestDtosCascada)
    }

    private fun setPrecioLineaDoc() {
        // Si la l??nea es sin cargo el precio ser?? cero.
        if (fDocumento.fArtSinCargo) {
            fDocumento.fPrecio = 0.0
            fDocumento.fPrecioII = 0.0
        } else {
            val sPrecio = edtPrecio.text.toString().replace(',', '.')
            if (sPrecio == "") {
                fDocumento.fPrecio = 0.0
                fDocumento.fPrecioII = 0.0
            } else {
                val queLinea: Long = if (fEstado == est_Vl_Nueva) -1 else fLinea.toLong()
                if (fIvaIncluido && fAplicarIva) {
                    // Si tenemos el flag de precio de rating (fPrecioRating) o el de oferta y hemos modificado el precio, quitaremos dicho flag
                    // y borramos el descuento en cascada que gener?? dicho rating.
                    if (fDocumento.fPrecioRating || fDocumento.fArtEnOferta) {
                        if (fDocumento.fPrecioII != sPrecio.toDouble()) {
                            fDocumento.fPrecioRating = false
                            fDocumento.fArtEnOferta = false
                            fDocumento.borrarDtosCasc(queLinea)
                        }
                    }
                    fDocumento.fPrecioII = sPrecio.toDouble()
                    fDocumento.calculaPrBase()
                } else {
                    // Si tenemos el flag de precio de rating (fPrecioRating) y hemos modificado el precio, quitaremos dicho flag
                    // y borramos el descuento en cascada que gener?? dicho rating.
                    if (fDocumento.fPrecioRating || fDocumento.fArtEnOferta) {
                        if (fDocumento.fPrecio != sPrecio.toDouble()) {
                            fDocumento.fPrecioRating = false
                            fDocumento.fArtEnOferta = false
                            fDocumento.borrarDtosCasc(queLinea)
                        }
                    }
                    fDocumento.fPrecio = sPrecio.toDouble()
                    fDocumento.calculaPrecioII()
                }
            }
        }

        // Vemos si ha habido un cambio del precio o del dto. Para ello no podemos hacer simplemente una comparaci??n
        // porque los valores son del tipo Double y esto en java falla, aunque fueran iguales dice que son distintos.
        // Por eso, hacemos primero una conversi??n con un formato determinado y comparamos las cadenas.
        val sPrecDoc = String.format(fFtoDecPrBase, fDocumento.fPrecio)
        val sDtoDoc = String.format(Locale.getDefault(), "%.2f", fDocumento.fDtoLin)
        val sPrecTrfa = String.format(fFtoDecPrBase, fDocumento.fPrecioTarifa)
        val sDtoTrfa = String.format(Locale.getDefault(), "%.2f", fDocumento.fDtoLinTarifa)
        fDocumento.fHayCambPrecio = sPrecDoc != sPrecTrfa || sDtoDoc != sDtoTrfa
    }

    private fun aceptarArticulo() {
        // Si hemos le??do un c??digo de cajas y el art??culo tiene la marca 'aplicar tarifa cajas', establecemos la
        // tarifa de cajas, siempre y cuando el documento nos permita aplicar tarifa de cajas. Antes de comenzar cualquier documento
        // comprobamos si hemos recibido en la tablet la tarifa de cajas, para permitir su uso o no.
        if (fArticulos.fTarifaCajas && fDocumento.fPuedoAplTrfCajas)
            fDocumento.fTarifaLin = fConfiguracion.tarifaCajas()
        fHeElegidoTarifa = false
        // Si tenemos configurado pedir tarifa por linea la pedimos ahora, a no ser que
        // el art??culo tenga la marca de 'aplicar tarifa cajas'.
        if (fPedirTarifa && !fArticulos.fTarifaCajas) {
            elegirTarifa()
        } else {
            // Esto lo hacemos as?? porque elegirTarifa() llama a un AlertDialog, el cual no se ejecuta hasta el final
            // de este procedimiento. O sea, primero se ejecuta activarPiezas(), activarCajas(), setArticulo(), etc., y
            // luego elegirTarifa(), aunque la llamada a ??sta sea la primera. De esta forma, en elegirTarifa() tambi??n
            // llamamos a terminaAceptarArticulo() una vez que hemos escogido la tarifa.
            terminaAceptarArticulo()
        }
    }


    private fun elegirFormato() {
        //var listItems: MutableList<String> = ArrayList()
        val listItems = fArticulos.formatosALista()
        chsFormatos = listItems.toTypedArray()
        // Mostramos la descripci??n del art??culo para saber de qu?? art??culo estamos escogiendo los formatos,
        // aunque luego la volvemos a mostrar en terminaAceptarArticulo().
        tvDescr.text = fArticulos.fDescripcion
        val altBld = AlertDialog.Builder(this)
        altBld.setTitle("Escoger formato")
        altBld.setSingleChoiceItems(chsFormatos, -1) { dialog: DialogInterface, item: Int ->
            val sFormato = chsFormatos[item].toString()
            fDocumento.fFormatoLin = sFormato.substring(0, 2).toShort()
            dialog.dismiss()
            aceptarArticulo()
        }
        val alert = altBld.create()
        alert.show()
    }

    private fun elegirTarifa() {
        val altBld = AlertDialog.Builder(this)
        altBld.setTitle("Escoger tarifa")
        altBld.setSingleChoiceItems(chsTarifas, -1) { dialog: DialogInterface, item: Int ->
            val sTarifa = chsTarifas[item].toString()
            fDocumento.fTarifaLin = sTarifa.substring(0, 2).toShort()
            // Si la tarifa que acabamos de elegir es de piezas activaremos la venta por piezas
            if (esTarifaPiezas(fDocumento.fTarifaLin)) {
                fTarifaAnt = fDocumento.fTarifaLin
                calcularPrecioYDto(false)
                mostrarTarifa()
                fDocumento.fLineaPorPiezas = true
                imgTrfPiezas.visibility = View.VISIBLE
                fVendTrfPiezas = true
            }
            dialog.dismiss()
            mostrarTarifa()
            fHeElegidoTarifa = true
            terminaAceptarArticulo()
        }
        val alert = altBld.create()
        alert.show()
    }

    private fun prepararTarifas() {
        val listItems: MutableList<String> = ArrayList()
        tarifasALista(listItems)
        chsTarifas = listItems.toTypedArray()
    }

    private fun mostrarTarifa() {
        val queTarifa = "Tarifa: " + fDocumento.fTarifaLin
        tvTarifa.text = queTarifa
    }

    private fun mostrarFormato() {
        tvFormato.text = fDocumento.getDescrFormato(fDocumento.fFormatoLin.toInt())
    }

    private fun calcularCantidadYTrfCj() {
        var sCajas = edtCajas.text.toString().replace(',', '.')
        if (sCajas == "" || sCajas == "." || sCajas == "-") {
            sCajas = "0.0"
            fDocumento.fCajas = 0.0
        }
        val numCajas = sCajas.toDouble()
        fDocumento.fCantidad = fArticulos.fUCaja * numCajas
        edtCantidad.setText(fDocumento.fCantidad.toString())

        // Si el art??culo tiene el flag de Aplicar tarifa de cajas e indicamos alguna caja, aplicaremos dicha tarifa, siempre y
        // cuando el documento lo permita. Si previamente he elegido la tarifa mantenemos la tarifa que hemos elegido.
        if (!fHeElegidoTarifa) {
            if (numCajas != 0.0) {
                if (fArticulos.aplicarTrfCajas() && fDocumento.fPuedoAplTrfCajas) {
                    fDocumento.fTarifaLin = fConfiguracion.tarifaCajas()
                    calcularPrecioYDto(true)
                }
            }
        }
    }

    private fun terminaAceptarArticulo() {
        activarPiezas()
        activarCajas()
        activarAlmacen()
        setArticulo()
        // Una vez tenemos un c??digo de art??culo v??lido, activamos el resto de controles.
        activarControlesLinea(true)

        // Buscamos el siguiente control al que tenemos que darle el foco.
        val queId = imgBuscaArt.nextFocusDownId
        val queView = findViewById<View>(queId)
        queView?.requestFocus()

        // Presentaremos autom??ticamente la ventana de lotes siempre que tengamos en configuraci??n:
        // - Usar cargas
        // - Aviso lotes
        if (fDocumento.fTipoDoc == TIPODOC_FACTURA || fDocumento.fTipoDoc == TIPODOC_ALBARAN) {
            if (fConfiguracion.usarCargas() && fConfiguracion.avisoLotes()) {
                fMantenerVistaLotes = false
                abrirVistaLotes()
            }
        }
    }

    fun buscarLote(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        fMantenerVistaLotes = true
        abrirVistaLotes()
    }

    private fun abrirVistaLotes() {
        val i = Intent(this, BuscarLotes::class.java)
        i.putExtra("articulo", fDocumento.fArticulo)
        i.putExtra("formatocant", fConfiguracion.formatoDecCantidad())
        i.putExtra("mantenerVista", fMantenerVistaLotes)
        startActivityForResult(i, fRequestBuscarLote)
    }

    private fun activarAlmacen() {
        // Pediremos el almac??n por l??nea siempre que lo tengamos configurado y el documento sea un presupuesto
        edtAlmacen.isEnabled = fEditAlmEnabled

        // Vemos a qu?? control le daremos el foco al salir de edtAlmacen
        if (fEditAlmEnabled) {
            if (edtCajas.isEnabled) edtAlmacen.nextFocusDownId = edtCajas.id else {
                if (edtPiezas.isEnabled) edtAlmacen.nextFocusDownId =
                    edtPiezas.id else edtAlmacen.nextFocusDownId = edtCantidad.id
            }
        }
    }

    private fun activarPiezas() {
        // Si el art??culo tiene el flag 'Usar piezas', pediremos piezas.
        // Obviamente, si en configuraci??n tenemos 'Usar piezas'.
        if (fPedirPiezas) {
            edtPiezas.isEnabled = fArticulos.usarPiezas()
        } else edtPiezas.isEnabled = false
        if (edtPiezas.isEnabled) edtPiezas.keyListener = DigitsKeyListener.getInstance(true, true)
    }


    private fun activarCajas() {
        // Si el art??culo tiene unidades por caja, pedimos las cajas. En caso contrario, no.
        // Siempre y cuando tengamos configurado pedir cajas.
        if (fPedirCajas) {
            if (fArticulos.fUCaja != 0.0) {
                edtCajas.isEnabled = true
                if (fEditAlmEnabled) imgBuscaArt.nextFocusDownId = edtAlmacen.id
                else imgBuscaArt.nextFocusDownId = edtCajas.id
                if (edtPiezas.isEnabled) edtCajas.nextFocusDownId = edtPiezas.id
                else edtCajas.nextFocusDownId = edtCantidad.id
            } else {
                edtCajas.isEnabled = false
                if (fEditAlmEnabled) imgBuscaArt.nextFocusDownId = edtAlmacen.id
                else {
                    if (edtPiezas.isEnabled) imgBuscaArt.nextFocusDownId = edtPiezas.id
                    else imgBuscaArt.nextFocusDownId = edtCantidad.id
                }
            }
        } else {
            edtCajas.isEnabled = false
            if (fEditAlmEnabled) {
                imgBuscaArt.nextFocusDownId = edtAlmacen.id
            } else {
                if (edtPiezas.isEnabled) imgBuscaArt.nextFocusDownId = edtPiezas.id
                else imgBuscaArt.nextFocusDownId = edtCantidad.id
            }
        }
        if (edtCajas.isEnabled) edtCajas.keyListener = DigitsKeyListener.getInstance(true, true)
    }

    private fun setArticulo() {
        fDocumento.fArticulo = fArticulos.fArticulo
        fDocumento.fCodArt = fArticulos.fCodigo
        fDocumento.fDescr = fArticulos.fDescripcion
        fDocumento.fCodigoIva = fArticulos.fCodIva

        // Vemos las posibles tasas de la l??nea.
        if (fDocumento.fAplicarIva) {
            if (fUsarTasa1) fDocumento.fTasa1 = fArticulos.fTasa1
            if (fUsarTasa2) fDocumento.fTasa2 = fArticulos.fTasa2
        }

        // Si trabajamos con art??culos habituales grabamos el texto del art??culo en el de la l??nea.
        // Si el art??culo tiene texto resaltamos en rojo el t??tulo del bot??n para que el vendedor se percate.
        if (fDocumento.fHayArtHabituales) {
            fDocumento.fTextoLinea = fDocumento.textoArtHabitual()
            if (fDocumento.fTextoLinea != "") {
                if (fDocumento.fTextoLinea == "") btnTextoLinea.setTextColor(ContextCompat.getColor(this, R.color.texto_botones)
                ) else btnTextoLinea.setTextColor(Color.RED)
            }
        }
        edtCodArt.setText(fArticulos.fCodigo)
        tvDescr.text = fArticulos.fDescripcion
        mostrarTarifa()
        if (fDocumento.fFormatoLin.toInt() != 0) mostrarFormato()

        // Mostramos la imagen del art??culo.
        if (fConfiguracion.fTamanyoPantLargo) mostrarImagen()

        // Si el c??d. barras que hemos le??do es de cajas, aconsejamos 1 caja y calculamos la cantidad.
        if (fArticulos.fCodBCajas) {
            fDocumento.fCajas = 1.0
            fDocumento.fCantidad = fArticulos.fUCaja
            edtCajas.setText(fDocumento.fCajas.toString())
            edtCantidad.setText(fDocumento.fCantidad.toString())
        }

        // Calculamos el precio y el descuento.
        calcularPrecioYDto(true)
        if (fVendiendoEnlace) fArtConEnlace = false else {
            fArtConEnlace = fArticulos.tieneEnlace()
            if (fArtConEnlace) fCodArtEnlazado = fArticulos.codArtEnlazado()
        }
    }

    private fun calcularPrecioYDto(calcDtoImpte: Boolean) {
        tvPrNeto.text = ""
        // Calculamos precio y dto. una vez que ya hemos escogido la tarifa.
        fDocumento.calculaPrecioYDto(fArticulos.fGrupo, fArticulos.fDepartamento, fArticulos.fCodProv, fArticulos.fPorcIva)
        // Mostramos precio y dto.
        if (fIvaIncluido && fAplicarIva) {
            edtPrecio.setText(String.format(fFtoDecPrII, fDocumento.fPrecioII))
        } else {
            edtPrecio.setText(String.format(fFtoDecPrBase, fDocumento.fPrecio))
        }

        // Si tenemos un descuento por importe (desde el rating), lo a??adiremos como un descuento en cascada. Por ahora en estado linea nueva.
        if (fEstado == est_Vl_Nueva) {
            if (fDocumento.fDtoRatingImp != 0.0 && calcDtoImpte) {
                // Borramos los posibles dtos. en cascada anteriores
                if (fDocumento.fLineaConDtCasc) fDocumento.borrarDtosCasc(-1)
                anyadirDtoCascada()

                // Mostramos el precio neto.
                if (fIvaIncluido && fAplicarIva) {
                    var fDtoRatingImpII = fDocumento.fDtoRatingImp + fDocumento.fDtoRatingImp * fDocumento.fPorcIva / 100
                    fDtoRatingImpII = redondear(fDtoRatingImpII, 2)
                    tvPrNeto.text = String.format(fFtoDecPrII, fDocumento.fPrecioII - fDtoRatingImpII)
                } else {
                    tvPrNeto.text = String.format(fFtoDecPrBase, fDocumento.fPrecio - fDocumento.fDtoRatingImp)
                }
            }
        }
        edtDto.setText(String.format(Locale.getDefault(), "%.2f", fDocumento.fDtoLin))
        if (fEstado == est_Vl_Nueva) {
            // Guardamos el precio y dto. de la tarifa
            fDocumento.fPrecioTarifa = fDocumento.fPrecio
            fDocumento.fDtoLinTarifa = fDocumento.fDtoLin
        }
    }

    private fun anyadirDtoCascada() {
        if (fDocumento.fTipoDoc == TIPODOC_FACTURA) {
            val dtoLineaEnt = DtosLinFrasEnt()
            dtoLineaEnt.lineaId = -1
            dtoLineaEnt.orden = 1
            dtoLineaEnt.descuento = "0.0"
            dtoLineaEnt.importe = fDocumento.fDtoRatingImp.toString()
            dtoLineaEnt.cantidad1 = "0.0"
            dtoLineaEnt.cantidad2 = "0.0"
            dtoLineaEnt.desdeRating = "T"

            fDocumento.insertarDtoCascFras(dtoLineaEnt)

        } else {

            val dtoLineaEnt = DtosLineasEnt()
            dtoLineaEnt.lineaId = -1
            dtoLineaEnt.orden = 1
            dtoLineaEnt.descuento = "0.0"
            dtoLineaEnt.importe = fDocumento.fDtoRatingImp.toString()
            dtoLineaEnt.cantidad1 = "0.0"
            dtoLineaEnt.cantidad2 = "0.0"
            dtoLineaEnt.desdeRating = "T"

            fDocumento.insertarDtoCasc(dtoLineaEnt)
        }

        fDtosCascada.fTipoDoc = fDocumento.fTipoDoc
        fDtosCascada.abrir(-1)
        // Configuramos el objeto de los dtos. en cascada
        fDtosCascada.fIvaIncluido = fIvaIncluido //fConfiguracion.ivaIncluido(fDocumento.fEmpresa)
        fDtosCascada.fAplicarIva = fDocumento.fClientes.fAplIva
        fDtosCascada.fPorcIva = fDocumento.fPorcIva
        fDtosCascada.fDecPrBase = fConfiguracion.decimalesPrecioBase()
        fDocumento.fDtoLin = fDtosCascada.calcularDtoEquiv(fDocumento.fPrecio, fDecPrBase).toDouble()
        fDocumento.fLineaConDtCasc = true
    }

    private fun mostrarImagen() {
        val path = carpetaImagenes + fArticulos.getImagen()
        val file = File(path)
        if (file.exists()) {
            imgArticulo.visibility = View.VISIBLE
            imgArticulo.setImageURI(Uri.parse(path))
        } else {
            imgArticulo.setImageURI(null)
        }
    }

    private fun activarControlesLinea(activar: Boolean) {
        // Si activar==true, en aceptarArticulo() ya hemos configurado el control
        // edtCajas para activarlo o no seg??n el art??culo tenga unidades por caja.
        // Idem con piezas.
        if (!activar) {
            edtCajas.isEnabled = false
            edtPiezas.isEnabled = false
        }
        edtCantidad.isEnabled = activar
        edtPrecio.isEnabled = activar
        edtLote.isEnabled = activar
        imgLote.isEnabled = activar

        // Los descuentos los activaremos si as?? lo tenemos configurado.
        if (activar) {
            if (fConfiguracion.pedirDtos()) edtDto.isEnabled = true

            // Si el art??culo no controla la trazabilidad desactivaremos el lote
            if (!fArticulos.controlaTrazabilidad()) {
                edtLote.isEnabled = false
                imgLote.isEnabled = false
            }
        } else {
            edtDto.isEnabled = false
        }
    }

    private fun limpiarControles() {
        edtCodArt.setText("")
        tvDescr.text = ""
        tvTarifa.text = ""
        tvFormato.text = ""
        edtCajas.setText("")
        edtPiezas.setText("")
        edtCantidad.setText("")
        edtPrecio.setText("")
        edtDto.setText("")
        tvPrNeto.text = ""
        edtLote.setText("")
        if (fConfiguracion.fTamanyoPantLargo) imgArticulo.setImageURI(null)
    }

    private fun llenarControles() {
        edtCodArt.setText(fDocumento.fCodArt)
        tvDescr.text = fDocumento.fDescr
        val queTarifa = "Tarifa: " + fDocumento.fTarifaLin
        tvTarifa.text = queTarifa
        tvFormato.text = fDocumento.getDescrFormato(fDocumento.fFormatoLin.toInt())
        edtCajas.setText(String.format(fConfiguracion.formatoDecCantidad(), fDocumento.fCajas))
        edtPiezas.setText(String.format(fConfiguracion.formatoDecCantidad(), fDocumento.fPiezas))
        edtCantidad.setText(String.format(fConfiguracion.formatoDecCantidad(), fDocumento.fCantidad))
        if (fIvaIncluido && fAplicarIva) {
            edtPrecio.setText(String.format(fConfiguracion.formatoDecPrecioIva(), fDocumento.fPrecioII))
        } else {
            edtPrecio.setText(String.format(fConfiguracion.formatoDecPrecioBase(), fDocumento.fPrecio))
        }
        edtDto.setText(String.format(Locale.getDefault(), "%.2f", fDocumento.fDtoLin))
        edtLote.setText(fDocumento.fLote)
        if (fEditAlmEnabled) {
            edtAlmacen.setText(fDocumento.fAlmacPedido)
        }
    }

    private fun activarDesactivarControles() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        if (fEstado == est_Vl_Nueva) {
            // Inicializamos las propiedades de la linea.
            fDocumento.inicializarLinea()
            limpiarControles()
            edtCodArt.isEnabled = true
            activarControlesLinea(false)
            edtCodArt.requestFocus()
            imgTrfPiezas.visibility = View.GONE
            // Activo el teclado.
            imm.showSoftInput(edtCodArt, 0)

            // Si estamos vendiendo el art??culo enlazado lo que hacemos es poner en el control edtCodArt
            // el c??digo de ??ste, para que el evento addTextChangedListener lo procese y,
            // a partir de ah??, continuamos como si hubi??ramos introducido el c??digo a mano.
            if (fVendiendoEnlace) {
                val queCodEnlazado = fCodArtEnlazado
                edtCodArt.setText(queCodEnlazado)
                edtCantidad.setText(String.format(fConfiguracion.formatoDecCantidad(), fCantParaEnlace))
            }
        } else if (fEstado == est_Vl_Editar) {
            fDocumento.cargarLinea(fLinea)
            if (!fDocumento.fLineaPorPiezas) {
                imgTrfPiezas.visibility = View.GONE
            }
            llenarControles()
            // Nos posicionamos en el art??culo de la l??nea y mostramos su imagen.
            fArticulos.existeCodigo(fDocumento.fCodArt)
            if (fConfiguracion.fTamanyoPantLargo) mostrarImagen()
            edtCodArt.isEnabled = false
            imvBuscar.isEnabled = false
            activarPiezas()
            activarCajas()
            activarAlmacen()
            activarControlesLinea(true)
            // Si estamos pidiendo el tipo por l??nea permitiremos modificar el tipo de la l??nea antes de cerrar el documento.
            //edtTipo.setEnabled(false);
            if (edtCajas.isEnabled) {
                if (edtCajas.requestFocus()) {
                    imm.showSoftInput(edtCajas, 0)
                }
            } else {
                edtCantidad.requestFocus()
                imm.showSoftInput(edtCantidad, 0)
            }
        }
    }

    fun aceptarLinea(view: View?) {
        view?.getTag(0)              // Para que no d?? warning el compilador

        if (puedoSalvar()) {
            if (edtCajas.isEnabled) {
                val sCajas = edtCajas.text.toString().replace(',', '.')
                if (sCajas != "" && sCajas != "." && sCajas != "-") fDocumento.fCajas = sCajas.toDouble()
                else fDocumento.fCajas = 0.0
            }
            if (edtPiezas.isEnabled) {
                val sPiezas = edtPiezas.text.toString().replace(',', '.')
                if (sPiezas != "") fDocumento.fPiezas = sPiezas.toDouble()
            }
            val sCantidad = edtCantidad.text.toString().replace(',', '.')
            if (sCantidad == "" || sCantidad == "." || sCantidad == "-") fDocumento.fCantidad = 0.0
            else fDocumento.fCantidad = sCantidad.toDouble()
            // Tomamos aqu?? la cantidad por si el art??culo tuviera otro enlazado.
            if (!fVendiendoEnlace) {
                fCantParaEnlace = if (fConfiguracion.igualarCantArtEnlace()) fDocumento.fCantidad else 1.0
            }

            // Establecemos el precio de la l??nea.
            setPrecioLineaDoc()

            // Si la l??nea tiene descuentos en cascada, puede ser que el descuento equivalente
            // tenga varios decimales, mientras que en el control s??lo visualizamos dos.
            // Por eso mantenemos el porcentaje de descuento que hemos calculado en los dtos. en cascada.
            if (!fDocumento.fLineaConDtCasc) {
                val sDto = edtDto.text.toString().replace(',', '.')
                if (sDto == "") fDocumento.fDtoLin = 0.0
                else fDocumento.fDtoLin = sDto.toDouble()
            } else {
                // Puede ser que estemos modificando la l??nea y cambiemos el precio. Si la l??nea ten??a descuentos en cascada
                // hay que recalcular el descuento equivalente, puesto que al cambiar el precio el dto. equivalente ya no es el mismo.
                // Lo mismo nos puede pasar en nueva l??nea, porque podr??amos cambiar el precio despu??s de tener calculados los descuentos
                // en cascada, p.ej. cuando tenemos un descuento por importe desde rating.
                fDtosCascada.fTipoDoc = fDocumento.fTipoDoc
                if (fEstado == est_Vl_Editar) fDtosCascada.abrir(fLinea)
                else fDtosCascada.abrir(-1)
                // Configuramos el objeto de los dtos. en cascada
                fDtosCascada.fIvaIncluido = fIvaIncluido //fConfiguracion.ivaIncluido(fDocumento.fEmpresa)
                fDtosCascada.fAplicarIva = fDocumento.fClientes.fAplIva
                fDtosCascada.fPorcIva = fDocumento.fPorcIva
                fDtosCascada.fDecPrBase = fConfiguracion.decimalesPrecioBase()
                fDocumento.fDtoLin = fDtosCascada.calcularDtoEquiv(fDocumento.fPrecio, fDecPrBase).toDouble()
            }
            var continuar: Boolean
            if (fIvaIncluido && fAplicarIva) {
                continuar = fDocumento.calcularImpteII(false)
                if (continuar) continuar = fDocumento.calcularImpte(true)
            } else {
                continuar = fDocumento.calcularImpte(false)
                if (continuar) continuar = fDocumento.calcularImpteII(true)
            }
            if (continuar) {
                fDocumento.fLote = edtLote.text.toString()
                fDocumento.fAlmacPedido = edtAlmacen.text.toString()

                // Si la cantidad es negativa y tenemos configurado pedir incidencias, pues las pedimos.
                // Lo hacemos aqu?? al final de la funci??n porque Android es asincr??nico, no detiene la ejecuci??n al
                // llamar a otra funci??n o a un cuadro de di??logo, sino que contin??a y, cuando puede, llama al cuadro de di??logo.
                if (fDocumento.fCantidad < 0 && fConfiguracion.pedirIncidLineas()) {
                    incidLineaNegativo()
                } else {
                    grabarLinea()
                }
            }
        }
    }

    private fun grabarLinea() {
        if (fEstado == est_Vl_Nueva) {
            // Comprobamos si sobrepasamos el stock del lote al vender.
            if (fDocumento.fLote != "") avisoStockLote()
            fDocumento.insertarLinea()
            fVendiendoEnlace = false
            if (fArtConEnlace) {
                venderEnlace()
            } else {
                activarDesactivarControles()
                // Esta l??nea es la que vuelve a llamar a la ventana de lista de art??culos cuando terminamos de vender
                // un art??culo. Comentarla o descomentarla seg??n queramos que funcione o no.
                //buscarArticulo(null);
            }
        } else if (fEstado == est_Vl_Editar) {
            fDocumento.editarLinea(fLinea)
            fLinea = 0
            val returnIntent = Intent()
            setResult(RESULT_OK, returnIntent)
            finish()
        }
    }

    private fun venderEnlace() {
        fVendiendoEnlace = true
        activarDesactivarControles()
    }

    private fun puedoSalvar(): Boolean {
        if (fEstado == est_Vl_Nueva) {
            val sCodArt = edtCodArt.text.toString()
            if (sCodArt == "") {
                MsjAlerta(this).alerta(resources.getString(R.string.msj_SinCodigo))
                return false
            } else {
                if (fDocumento.fArticulo == 0) return false
            }
        }

        // Si tenemos configurado pedir almac??n por l??nea comprobamos que haya alguno
        if (fEditAlmEnabled) {
            val sCodAlm = edtAlmacen.text.toString()
            if (sCodAlm == "") {
                MsjAlerta(this@VentasDatosLinea).alerta(resources.getString(R.string.msj_SinAlmacen))
                edtAlmacen.requestFocus()
                return false
            } else {
                if (!fAlmacenes.existe(sCodAlm)) {
                    MsjAlerta(this@VentasDatosLinea).alerta(resources.getString(R.string.msj_CodNoExiste))
                    edtAlmacen.requestFocus()
                    return false
                }
            }
        }
        val queView = currentFocus
        // Si pulsamos Aceptar estando en edtCajas, calculamos la cantidad antes de salir, siempre que estemos
        // en una l??nea nueva o lo tengamos configurado.
        if (queView != null) {
            if (queView.id == edtCajas.id) {
                if (fEstado == est_Vl_Nueva || fConfiguracion.aconsUnidCajaModif()) calcularCantidadYTrfCj()
            }
        }
        var sCantidad = edtCantidad.text.toString().replace(',', '.')
        if (sCantidad == "" || sCantidad == "." || sCantidad == "-") sCantidad = "0.0"
        val dCantidad = sCantidad.toDouble()
        if (dCantidad < 0 && fConfiguracion.noVenderNeg()) {
            MsjAlerta(this).alerta(resources.getString(R.string.msj_NoVenderNeg))
            return false
        }
        return true
    }

    private fun avisoStockLote() {
        if (!fDocumento.hayStockLote()) MsjAlerta(this).alerta("No tiene suficiente stock del lote")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        // Actividad buscar articulos.
        if (requestCode == fRequestBuscarArt) {
            if (resultCode == RESULT_OK) {
                val voyA = data?.getIntExtra("voyA", 0) ?: 0
                if (voyA > 0) {
                    when (voyA) {
                        LISTA_ARTICULOS -> lanzarListaArticulos()
                        GRUPOS_Y_DEP -> lanzarCatalGrupDep()
                        HISTORICO -> lanzarHistorico()
                        CATALOGOS -> lanzarCatalogos()
                    }
                } else {
                    val vengoDe = data?.getIntExtra("vengoDe", 0) ?: 0

                    if (vengoDe > LISTA_ARTICULOS) {
                        // Volvemos a la pantalla del documento.
                        val returnIntent = Intent()
                        setResult(RESULT_OK, returnIntent)
                        finish()

                    } else if (vengoDe == LISTA_ARTICULOS) {
                        val queArticulo = data?.getIntExtra("articulo", -1) ?: 0
                        if (fArticulos.existeArticulo(queArticulo)) {
                            edtCodArt.setText(fArticulos.fCodigo)
                            tvDescr.text = fArticulos.fDescripcion
                            if (fArticulos.usarFormatos()) {
                                elegirFormato()
                            } else {
                                aceptarArticulo()
                            }
                        } // Mostramos el teclado.
                        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                    }
                }
            } else {
                finish()
            }

        // Actividad buscar lote.
        } else if (requestCode == fRequestBuscarLote) {
            if (resultCode == RESULT_OK) {
                val sQueLote = data?.getStringExtra("lote")
                edtLote.setText(sQueLote)
            }
            // Tenemos que hacer esto al volver de buscar lote, porque si no el programa tiene un comportamiento extra??o
            // con respecto al teclado y los controles edit, ya que se comporta como si el control que ten??a el foco antes de la
            // llamada a buscar lote hubiera perdido el foco y no lo reconociera (por ejemplo si el foco lo toma edtCajas nos
            // presenta el teclado alfanum??rico en vez del num??rico).
            activarPiezas()
            activarCajas()
            activarAlmacen()
            // Una vez tenemos un c??digo de art??culo v??lido, activamos el resto de controles.
            activarControlesLinea(true)

        } else if (requestCode == fRequestDtosCascada) {
            edtDto.setText(String.format(Locale.getDefault(), "%.2f", fDocumento.fDtoLin))
            aceptarLinea(null)

        } else if (requestCode == fRequestPedirDosis) {
            if (resultCode == RESULT_OK) {
                val queFormatoId = data?.getShortExtra("formatoId", 0) ?: 0
                val queNumPiezas = data?.getStringExtra("piezas")
                fDocumento.fFormatoLin = queFormatoId
                mostrarFormato()
                edtPiezas.setText(queNumPiezas)
            }
        }
    }

    fun buscarArticulo(view: View?) {
        view?.getTag(0)              // Para que no d?? warning el compilador

        if (fEstado == est_Vl_Nueva) {
            // Si el tama??o de la pantalla es menor de 7" no presentaremos el cat??logo.
            if (fConfiguracion.fTamanyoPantLargo && (!fConfiguracion.usarPiezas() || !fConfiguracion.usarFormatos())) {
                // Si estamos entrando a nueva linea por primera vez visualizaremos los art??culos seg??n como
                // tengamos configurado en preferencias. Las dem??s veces los veremos como la ??ltima vez.
                when (prefs.getInt("modoVisArtic", LISTA_ARTICULOS)) {
                    LISTA_ARTICULOS -> lanzarListaArticulos()
                    GRUPOS_Y_DEP -> lanzarCatalGrupDep()
                    HISTORICO -> lanzarHistorico()
                    CATALOGOS -> lanzarCatalogos()
                }
            } else lanzarListaArticulos()
        }
    }

    private fun lanzarListaArticulos() {
        val i = Intent(this, ArticulosActivity::class.java)
        i.putExtra("vendiendo", true)
        i.putExtra("ivaIncluido", fIvaIncluido)
        // Si tenemos algo en el TextView edtCodArt lo pasaremos como argumento de la b??squeda
        if (edtCodArt.text.toString() != "") i.putExtra("buscar", edtCodArt.text.toString())
        startActivityForResult(i, fRequestBuscarArt)
    }

    private fun lanzarCatalGrupDep() {
        val i = Intent(this, CatalogoGruposDep::class.java)
        i.putExtra("vendiendo", true)
        startActivityForResult(i, fRequestBuscarArt)
    }

    private fun lanzarCatalogos() {
        val i = Intent(this, CatalogoCatalogos::class.java)
        i.putExtra("vendiendo", true)
        startActivityForResult(i, fRequestBuscarArt)
    }

    private fun lanzarHistorico() {
        val i = Intent(this, CatalogoArticulos::class.java)
        i.putExtra("vendiendo", true)
        i.putExtra("modoVisArtic", HISTORICO)
        startActivityForResult(i, fRequestBuscarArt)
    }

    fun fichaArt(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        if (fEstado == est_Vl_Nueva || fEstado == est_Vl_Editar) {
            if (fDocumento.fArticulo > 0) {
                val i = Intent(this, FichaArticuloActivity::class.java)
                i.putExtra("articulo", fDocumento.fArticulo)
                startActivity(i)
            }
        }
    }

    fun textoLinea(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        if (fDocumento.fArticulo > 0) {
            val i = Intent(this, TextoLinea::class.java)
            i.putExtra("textolinea", fDocumento.fTextoLinea)
            startActivity(i)
        }
    }

    fun anularLinea(view: View?) {
        view?.getTag(0)              // Para que no d?? warning el compilador

        // Si hab??amos introducido descuentos en cascada en la l??nea los borramos, siempre que estuvi??ramos
        // a??adiendo una l??nea nueva.
        if (fEstado == est_Vl_Nueva) {
            if (fDocumento.fLineaConDtCasc) fDocumento.borrarDtosCasc(-1)
        }

        // Con Miscelan.ocultarTeclado() no lo ocultaba. De esta forma s??.
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(edtDto.windowToken, 0)
        finish()
    }

    fun hcoArtClte(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        if (fEstado == est_Vl_Nueva || fEstado == est_Vl_Editar) {
            if (fDocumento.fArticulo > 0) {
                val i = Intent(this, VerHcoArtCliente::class.java)
                i.putExtra("cliente", fDocumento.fCliente)
                i.putExtra("articulo", fDocumento.fArticulo)
                startActivity(i)
            }
        }
    }

    fun dialogoCambiarTipoDoc(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        // Tenemos esto para que no nos d?? error. Desde esta actividad no haremos nada,
        // funciona en VentasLineas.
    }

    private fun prepararIncidencias() {
        val tiposIncDao: TiposIncDao? = getInstance(this)?.tiposIncDao()
        val lIncidencias = tiposIncDao?.getAllIncidencias() ?: emptyList<TiposIncEnt>().toMutableList()

        val listItems: MutableList<String> = ArrayList()
        for (incidencia in lIncidencias) {
            listItems.add(ponerCeros(incidencia.tipoIncId.toString(), ancho_cod_incidencia) + "  " + incidencia.descripcion)
        }

        chsIncidencias = listItems.toTypedArray()
    }

    fun lineaSinCargo(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        // Mostramos la descripci??n del art??culo para saber de qu?? art??culo estamos escogiendo la incidencia.
        tvDescr.text = fArticulos.fDescripcion
        val altBld = AlertDialog.Builder(this)
        altBld.setTitle("Escoger incidencia")
        altBld.setSingleChoiceItems(chsIncidencias, -1) { dialog: DialogInterface, item: Int ->
            val sIncidencia = chsIncidencias[item].toString()
            fDocumento.fCodIncidencia = sIncidencia.substring(0, 2).toByte().toInt()
            dialog.dismiss()

            // Por ahora no permitiremos revertir una l??nea sin cargo, o sea, convertir una l??nea sin cargo
            // en otra con cargo.
            fDocumento.fArtSinCargo = true

            // Borramos los descuentos en cascada.
            val queLinea: Long = if (fEstado == est_Vl_Nueva) -1 else fLinea.toLong()
            if (fDocumento.fLineaConDtCasc) fDocumento.borrarDtosCasc(queLinea)
            fDocumento.fLineaConDtCasc = false

            // Aceptamos la l??nea.
            aceptarLinea(null)
        }
        val alert = altBld.create()
        alert.show()
    }

    private fun incidLineaNegativo() {
        val altBld = AlertDialog.Builder(this)
        altBld.setTitle("Escoger incidencia")
        altBld.setSingleChoiceItems(chsIncidencias, -1) { dialog: DialogInterface, item: Int ->
            val sIncidencia = chsIncidencias[item].toString()
            fDocumento.fCodIncidencia = sIncidencia.substring(0, 2).toByte().toInt()
            dialog.dismiss()

            // Tenemos que llamar desde aqu?? a grabarLinea() porque Android es asincr??nico en las llamadas a funciones o
            // cuadros de di??logo, es decir, no detiene la ejecuci??n.
            grabarLinea()
        }
        val alert = altBld.create()
        alert.show()
    }

    fun setTarifaPiezas(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        if (fPedirPiezas) {
            if (fVendTrfPiezas) {
                fDocumento.fTarifaLin = fTarifaAnt
                calcularPrecioYDto(false)
                mostrarTarifa()
                fDocumento.fLineaPorPiezas = false
                imgTrfPiezas.visibility = View.GONE
                fVendTrfPiezas = false
            } else {
                fTarifaAnt = fDocumento.fTarifaLin
                if (fDocumento.fClientes.fTrfPiezas > 0)
                    fDocumento.fTarifaLin = fDocumento.fClientes.fTrfPiezas

                calcularPrecioYDto(false)
                mostrarTarifa()
                fDocumento.fLineaPorPiezas = true
                imgTrfPiezas.visibility = View.VISIBLE
                fVendTrfPiezas = true
            }
        }
    }

    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            anularLinea(null)
            // Si el listener devuelve true, significa que el evento est?? procesado, y nadie debe hacer nada m??s.
            return true
        }
        // Para las dem??s cosas, se reenv??a el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }

    private fun hayOftasPorCantidad(): Boolean {
        return (ofCantRangosDao?.getCountOftCant() ?: 0) > 0
    }


    private fun tarifasALista(listItems: MutableList<String>) {

        val lTarifas = cnfTarifasDao?.getAllCnfTarifas() ?: emptyList<CnfTarifasEnt>().toMutableList()

        for (cnfTrfEnt in lTarifas) {
            listItems.add(ponerCeros(cnfTrfEnt.codigo.toString(), ancho_tarifa) + " " + cnfTrfEnt.descrTarifa)
        }
    }


    private fun esTarifaPiezas(queTarifa: Short): Boolean {

        val queFlag = cnfTarifasDao?.getFlag(queTarifa) ?: 0
        return (queFlag and FLAGCNFTARIFAS_PARA_PIEZAS > 0)
    }


}