package es.albainformatica.albamobileandroid.oldcatalogo

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import es.albainformatica.albamobileandroid.actividades.Dlg2Listener
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.historicos.Historico
import es.albainformatica.albamobileandroid.ventas.Documento
import android.content.SharedPreferences
import android.os.Bundle
import android.content.Intent
import androidx.preference.PreferenceManager
import androidx.core.content.ContextCompat
import es.albainformatica.albamobileandroid.maestros.FichaArticuloActivity
import android.view.LayoutInflater
import android.content.DialogInterface
import android.view.View
import android.widget.*
import es.albainformatica.albamobileandroid.*
import java.util.*

class CatalogoArticulos: Activity(), Dlg2Listener {
    private lateinit var gridView: GridView
    private lateinit var fArticulosGrv: ArticulosClase
    private lateinit var fHistorico: Historico
    private var fDocumento: Documento? = null
    private lateinit var fConfiguracion: Configuracion
    private lateinit var prefs: SharedPreferences

    private var fGrupo: Short = 0
    private var fDepart: Short = 0
    private var queBuscar: String = ""
    private var dondeBuscar: Short = 0 // 0-> buscar en el catálogo, 1-> buscar en todos los artículos
    private var queOrdenacion: Short = 0 // 0-> por descripción, 1-> por código
    private var fPosicion = 0
    private var fSoloOftas: Boolean = false
    private var fVendiendo: Boolean = false
    private var fUsarTasa1: Boolean = false
    private var fUsarTasa2: Boolean = false

    private var fDesde = 0
    private var fClasificador = 0
    private var fCatalogo = 0
    private var fEnBusqueda = false
    private var fEnOfertas = false
    private lateinit var btnBuscar: Button
    private lateinit var btnOrdenar: Button
    private var fFtoDecCant: String = ""
    private var fFtoPrecio: String = ""
    private var fDecPrBase = 0
    private var fDecPrII = 0
    private lateinit var contexto: Context
    private lateinit var itemArticulo: ItemArticulo
    private var fEditandoCajas: Boolean = false
    private var fEmpresaActual: Short = 0

    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.catalogo_articulos)

        val i = intent
        fDesde = i.getIntExtra("modoVisArtic", GRUPOS_Y_DEP)
        when (fDesde) {
            GRUPOS_Y_DEP -> {
                fGrupo = i.getShortExtra("grupo", 0)
                fDepart = i.getShortExtra("departamento", 0)
            }
            CLASIFICADORES -> {
                fClasificador = i.getIntExtra("clasificador", 0)
            }
            CATALOGOS -> {
                fCatalogo = i.getIntExtra("catalogo", 0)
            }
        }
        fVendiendo = i.getBooleanExtra("vendiendo", false)
        fSoloOftas = i.getBooleanExtra("soloofertas", false)
        if (fVendiendo) {
            fDocumento = Comunicador.fDocumento
            fHistorico = Comunicador.fHistorico
        }

        fConfiguracion = Comunicador.fConfiguracion
        // Si vamos a visualizar el histórico tendremos que crear fArticulosGrv ya que, al no tener una activity anterior
        // que lo haga, tenemos que hacerlo aquí.
        if (fDesde != HISTORICO) fArticulosGrv = Comunicador.fArticulosGrv else {
            fArticulosGrv = ArticulosClase(this)
            // Pasamos fArticulosGrv al comunicador para hacer uso del objeto en CatalogoFichaArtic.
            Comunicador.fArticulosGrv = fArticulosGrv
        }

        inicializarControles(i)
    }

    override fun onDestroy() {
        prefs.edit().putInt("ordenarpor", queOrdenacion.toInt()).apply()
        prefs.edit().putInt("dondebuscar", dondeBuscar.toInt()).apply()
        if (fDesde == HISTORICO) fDesde = LISTA_ARTICULOS
        prefs.edit().putInt("modoVisArtic", fDesde).apply()
        super.onDestroy()
    }

    private fun leerPreferencias() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        queOrdenacion = prefs.getInt("ordenarpor", 0).toShort()
        if (queOrdenacion.toInt() == 0) btnOrdenar.setCompoundDrawablesWithIntrinsicBounds(
            null,
            ContextCompat.getDrawable(this, R.drawable.ordenacion_alf),
            null,
            null
        ) else btnOrdenar.setCompoundDrawablesWithIntrinsicBounds(
            null,
            ContextCompat.getDrawable(this, R.drawable.ordenacion_cod),
            null,
            null
        )
        dondeBuscar = prefs.getInt("dondebuscar", 0).toShort()
        fEmpresaActual = prefs.getInt("ultima_empresa", 0).toShort()
    }

    private fun inicializarControles(i: Intent) {
        btnBuscar = findViewById(R.id.btnBuscar)
        btnOrdenar = findViewById(R.id.btnOrdenar)
        val btnAceptar = findViewById<Button>(R.id.btnAceptarCat)
        if (!fVendiendo) btnAceptar.visibility = View.GONE
        ocultarTeclado(this)

        // Leemos las preferencias de la actividad;
        leerPreferencias()
        fUsarTasa1 = fConfiguracion.usarTasa1()
        fUsarTasa2 = fConfiguracion.usarTasa2()
        queBuscar = ""
        gridView = findViewById(R.id.gridView)
        fFtoDecCant = fConfiguracion.formatoDecCantidad()
        fFtoPrecio =
            if (fConfiguracion.ivaIncluido(fEmpresaActual)) fConfiguracion.formatoDecPrecioIva() else fConfiguracion.formatoDecPrecioBase()
        fDecPrBase = fConfiguracion.decimalesPrecioBase()
        fDecPrII = fConfiguracion.decimalesPrecioIva()
        verArticulos()

        // Establecemos el título de la actividad, que irá en la ToolBar
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        if (fDesde == HISTORICO)
            tvTitulo.setText(R.string.mni_verhistorico)
        else tvTitulo.text = i.getStringExtra("descr_titulo")
    }

    private fun verArticulos() {
        // Con setAdapter se llena el gridview con datos. En este caso un nuevo objeto de la clase GrvImageArticulosAdapter,
        // que está definida en otro archivo. Para que detecte la pulsación se le añade un listener de itemClick
        // que recibe un onItemClickListener creado con new.
        val fTarifa: Short = if (fVendiendo) {
            val queTarifaDoc = fDocumento?.fTarifaDoc ?: 0
            if (queTarifaDoc > 0)
                fDocumento?.fTarifaDoc ?: 0
            else
                fConfiguracion.tarifaVentas().toString().toShort()
        }
        else fConfiguracion.tarifaVentas().toString().toShort()

        // Tendremos más de un constructor para la clase GrvImageArticulosAdapter.
        when (fDesde) {
            GRUPOS_Y_DEP -> gridView.adapter = GrvImageArticulosAdapter(this, fGrupo, fDepart, queBuscar,
                dondeBuscar, queOrdenacion, fSoloOftas, fTarifa, fVendiendo)

            CLASIFICADORES -> gridView.adapter = GrvImageArticulosAdapter(
                this,
                fClasificador,
                queBuscar,
                dondeBuscar,
                queOrdenacion,
                fSoloOftas,
                fTarifa,
                fVendiendo
            )
            CATALOGOS -> gridView.adapter = GrvImageArticulosAdapter(
                this,
                fCatalogo,
                queBuscar,
                dondeBuscar,
                queOrdenacion,
                fSoloOftas,
                fTarifa,
                fVendiendo
            )
            HISTORICO -> gridView.adapter = GrvImageArticulosAdapter(this, queBuscar,
                dondeBuscar, queOrdenacion, fSoloOftas, fTarifa, fVendiendo)
        }

        // Si no hemos encontrado ningún artículo, avisamos.
        if (gridView.adapter.count == 0) {
            MsjAlerta(this).alerta(getString(R.string.msj_SinDatos))
        }
    }

    fun verFichaArt(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        fPosicion = view.tag.toString().toInt()
        val queItem = gridView.getItemAtPosition(fPosicion) as ItemArticulo
        val fArticulo = queItem.articulo
        val i = Intent(this, FichaArticuloActivity::class.java)
        i.putExtra("articulo", fArticulo)
        startActivity(i)
    }

    fun ordenarArt(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        if (queOrdenacion.toInt() == 0) {
            queOrdenacion = 1
            btnOrdenar.setCompoundDrawablesWithIntrinsicBounds(
                null,
                ContextCompat.getDrawable(this, R.drawable.ordenacion_cod),
                null,
                null
            )
        } else {
            queOrdenacion = 0
            btnOrdenar.setCompoundDrawablesWithIntrinsicBounds(
                null,
                ContextCompat.getDrawable(this, R.drawable.ordenacion_alf),
                null,
                null
            )
        }
        verArticulos()
    }

    fun buscarArt(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        if (fEnBusqueda) {
            fEnBusqueda = false
            queBuscar = ""
            btnBuscar.setText(R.string.buscar)
            verArticulos()
        } else {
            // Lo primero que debemos hacer es rescatar el layout creado para el prompt.
            val li = LayoutInflater.from(this)
            val prompt = li.inflate(R.layout.catalog_alert_dialog_with_edittext, null)
            // Luego, creamos un constructor de Alert Dialog que nos ayudará a utilizar nuestro layout.
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setView(prompt)
            val editText = prompt.findViewById<EditText>(R.id.editText)
            val chkBsEnCat = prompt.findViewById<CheckBox>(R.id.chkEnCatalogo)
            val chkBsEnArt = prompt.findViewById<CheckBox>(R.id.chkEnArticulos)
            if (dondeBuscar.toInt() == 0) chkBsEnCat.isChecked = true
            else chkBsEnArt.isChecked = true
            chkBsEnCat.setOnCheckedChangeListener { _: CompoundButton?, _: Boolean ->
                chkBsEnArt.isChecked = !chkBsEnCat.isChecked
            }
            chkBsEnArt.setOnCheckedChangeListener { _: CompoundButton?, _: Boolean ->
                chkBsEnCat.isChecked = !chkBsEnArt.isChecked
            }

            // Por ultimo, creamos el cuadro de dialogo y las acciones requeridas al aceptar o cancelar el prompt.
            // Mostramos el mensaje del cuadro de diálogo
            alertDialogBuilder.setPositiveButton("OK") { _: DialogInterface?, _: Int ->
                if (editText.text.toString() != "") {
                    fEnBusqueda = true
                    btnBuscar.setText(R.string.anular_busq)
                    queBuscar = editText.text.toString()
                    dondeBuscar = if (chkBsEnArt.isChecked) 1 else 0
                    // Volvemos a cargar el gridView con los artículos que busquemos.
                    verArticulos()
                }
            }
            alertDialogBuilder.setCancelable(false)
                .setNegativeButton("Cancelar") { dialog: DialogInterface, _: Int ->
                    // Cancelamos el cuadro de dialogo
                    dialog.cancel()
                }

            // Creamos un AlertDialog y lo mostramos
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }
    }

    fun verPromociones(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        if (!fEnBusqueda) {
            if (fEnOfertas) {
                queBuscar = ""
                fEnOfertas = false
                fSoloOftas = false
            } else {
                // Volvemos a cargar el gridView sólo con las ofertas.
                queBuscar = ""
                fEnOfertas = true
                fSoloOftas = true
            }
            verArticulos()
        }
    }


    fun aceptarCatalogo(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        // Ya que las cantidades las hemos ido guardando en el histórico temporal, llamamos a fDocumento.grabarHistorico()
        // para pasar dichas cantidades al documento.
        fDocumento?.grabarHistorico()
        if (fDesde == HISTORICO) {
            val returnIntent = Intent()
            returnIntent.putExtra("vengoDe", HISTORICO)
            setResult(RESULT_OK, returnIntent)
        } else {
            setResult(RESULT_OK)
        }
        finish()
    }

    fun sumarCantidad(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        fPosicion = view.tag.toString().toInt()
        val queItem = gridView.getItemAtPosition(fPosicion) as ItemArticulo
        queItem.cantidad = queItem.cantidad + 1
        gridView.invalidateViews()
        // Actualizamos el temporal del histórico con los cambios en la cantidad.
        actualizarTempHco(queItem)
    }

    private fun actualizarTempHco(queItem: ItemArticulo) {
        // Buscamos el articulo en el que hemos pulsado (nos servira para calcular las unidades por caja, etc.).
        fArticulosGrv.existeArticulo(queItem.articulo)
        // Inicializamos y actualizamos el objeto fHistorico para luego añadir o editar la tabla tmpHco.
        fHistorico.inicializarLinea()
        // Si hemos indicado cajas calcularemos la cantidad. Además añadiremos las unidades sueltas que hayamos podido indicar.
        if (queItem.cajas != 0.0) fHistorico.fCantidad =
            fArticulosGrv.fUCaja * queItem.cajas + queItem.cantidad else fHistorico.fCantidad =
            queItem.cantidad
        fHistorico.fCajas = queItem.cajas
        var sPrecio: String
        val sDto: String
        if (queItem.tieneOferta()) {
            sPrecio = queItem.prOfta.replace(',', '.')
            sDto = queItem.dtoOfta.replace(',', '.')

            // Si la oferta es de descuento entonces el precio de oferta vendrá a cero y, en este caso, tendremos
            // que tomar como precio el precio normal.
            val dDtoOfta = sDto.toDouble()
            if (dDtoOfta != 0.0) sPrecio = queItem.prClte?.replace(',', '.') ?: "'0.0"
        } else {
            // Si hemos vendido alguna caja aplicaremos la tarifa de cajas, siempre que el documento nos permita aplicar
            // tarifa de cajas. Si vendemos alguna unidad además de las cajas se aplicará la tarifa de cajas.
            if (fHistorico.fCajas != 0.0 && (fDocumento?.fPrecioRating == true)) {
                fDocumento?.fTarifaLin = fConfiguracion.tarifaCajas().toString().toShort()
            }
            fDocumento?.fArticulo = fArticulosGrv.fArticulo
            fDocumento?.fAlmacen = fConfiguracion.almacen()
            fDocumento?.calculaPrecioYDto(fArticulosGrv.fGrupo, fArticulosGrv.fDepartamento, fArticulosGrv.fCodProv, fArticulosGrv.fPorcIva)
            sPrecio = if (fDocumento?.fPrecio != 0.0)
                fDocumento?.fPrecio.toString().replace(',', '.')
                else "0"
            sDto = if (fDocumento?.fDtoLin != 0.0) fDocumento?.fDtoLin.toString().replace(',', '.')
                else "0"
        }
        var dPrecio = redondear(sPrecio.toDouble(), fDecPrBase)
        fHistorico.fPrecio = dPrecio
        val dImpIva = dPrecio * queItem.porcIva / 100
        dPrecio += dImpIva
        fHistorico.fPrecioII = redondear(dPrecio, fDecPrII)
        fHistorico.fDtoLin = sDto.toDouble()
        fHistorico.fTasa1 = 0.0
        fHistorico.fTasa2 = 0.0
        if (fDocumento?.fAplicarIva == true) {
            if (fUsarTasa1) fHistorico.fTasa1 = fArticulosGrv.fTasa1
            if (fUsarTasa2) fHistorico.fTasa2 = fArticulosGrv.fTasa2
        }
        fHistorico.fArticulo = queItem.articulo
        fHistorico.fCodigo = queItem.codigo
        fHistorico.fDescr = queItem.descr
        fHistorico.fCodigoIva = fArticulosGrv.fCodIva
        fHistorico.aceptarCambiosArt(fHistorico.fArticulo)
    }

    fun restarCantidad(view: View) {
        fPosicion = view.tag.toString().toInt()
        val queItem = gridView.getItemAtPosition(fPosicion) as ItemArticulo
        queItem.cantidad = queItem.cantidad - 1
        gridView.invalidateViews()

        // Actualizamos el temporal del histórico con los cambios en la cantidad.
        actualizarTempHco(queItem)
    }

    fun sumarCajas(view: View) {
        fPosicion = view.tag.toString().toInt()
        val queItem = gridView.getItemAtPosition(fPosicion) as ItemArticulo
        queItem.cajas = queItem.cajas + 1
        gridView.invalidateViews()

        // Actualizamos el temporal del histórico con los cambios en la cantidad.
        actualizarTempHco(queItem)
    }

    fun restarCajas(view: View) {
        fPosicion = view.tag.toString().toInt()
        val queItem = gridView.getItemAtPosition(fPosicion) as ItemArticulo
        queItem.cajas = queItem.cajas - 1
        gridView.invalidateViews()

        // Actualizamos el temporal del histórico con los cambios en la cantidad.
        actualizarTempHco(queItem)
    }

    fun verHcoArticulo(view: View) {
        if (fVendiendo) {
            fPosicion = view.tag.toString().toInt()
            val queItem = gridView.getItemAtPosition(fPosicion) as ItemArticulo
            val queArticulo = queItem.articulo

            // Cargamos el histórico del artículo para el cliente.
            val listItems: MutableList<String> = ArrayList()
            fArticulosGrv.cargarHcoArtClte(queArticulo, (fDocumento?.fCliente ?: 0), listItems)
            if (listItems.isNotEmpty()) {
                val fragmentManager = fragmentManager
                val dialogo = DlgoPersHcoArtclte()
                dialogo.lista = listItems
                dialogo.fFtoDecCant = fFtoDecCant
                dialogo.fFtoPrecio = fFtoPrecio
                dialogo.fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresaActual)
                dialogo.fPorcIva = queItem.porcIva
                dialogo.show(fragmentManager, "tagAlerta")
            } else {
                MsjAlerta(this).informacion("El artículo no tiene histórico para este cliente")
            }
        }
    }

    // Mediante esta clase podemos construir un AlertDialog personalizado, teniendo en su contenido el
    // layout que queramos. Para este AlertDialog cargamos el layout dialog_hco_art_clte.
    class DlgoPersHcoArtclte : DialogFragment() {
        lateinit var lista: List<String>
        var fFtoDecCant: String = ""
        var fFtoPrecio: String = ""
        var fIvaIncluido = false
        var fPorcIva = 0.0

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(activity)
            val inflater = activity.layoutInflater
            val v = inflater.inflate(R.layout.dialog_hco_art_clte, null)
            val tvFecha = v.findViewById<TextView>(R.id.tvHAC_Fecha)
            val tvCajas = v.findViewById<TextView>(R.id.tvHAC_Cajas)
            val tvCant = v.findViewById<TextView>(R.id.tvHAC_Cant)
            val tvPrecio = v.findViewById<TextView>(R.id.tvHAC_Precio)
            val tvDto = v.findViewById<TextView>(R.id.tvHAC_Dto)
            val dCajas = lista[0].replace(',', '.').toDouble()
            val dCant = lista[1].replace(',', '.').toDouble()
            var dPrecio = lista[2].replace(',', '.').toDouble()
            val dDto = lista[3].replace(',', '.').toDouble()
            if (fIvaIncluido) {
                val dImpIva = dPrecio * fPorcIva / 100
                dPrecio += dImpIva
            }
            tvFecha.text = lista[4]
            tvCajas.text = String.format(fFtoDecCant, dCajas)
            tvCant.text = String.format(fFtoDecCant, dCant)
            tvPrecio.text = String.format(fFtoPrecio, dPrecio)
            tvDto.text = String.format(Locale.getDefault(), "%.2f", dDto)
            builder.setView(v)
                .setPositiveButton("Aceptar") { dialog: DialogInterface, _: Int -> dialog.cancel() }
            return builder.create()
        }
    }

    // La técnica que utilizo para obtener los datos de un cuadro de diálogo consiste en definir un Listener que “se dé cuenta”
    // cuando nuestro Custom Dialog esté por cerrarse y le comunique a la actividad principal (CatalogoArticulos) los datos
    // requeridos (el user input). Para ello la actividad principal deberá implementar dicho Listener como interface
    // y se lo deberá pasar como parámetro al Dialog, para que el mismo pueda llamarlo cuando el usuario oprima el botón OK,
    // antes de llamar al método dismiss() que cierra el diálogo.
    // El Diálogo se crea extendiendo de la clase Dialog y poniendo como uno de los parámetros de su constructor el Listener antes mencionado.
    fun editarCajas(view: View) {
        fPosicion = view.tag.toString().toInt()
        itemArticulo = gridView.getItemAtPosition(fPosicion) as ItemArticulo
        val act = this
        contexto = this
        fEditandoCajas = true
        val dlgCjUn = DialogoCajasUnid(contexto, act)
        dlgCjUn.show()
    }

    fun editarUnidades(view: View) {
        fPosicion = view.tag.toString().toInt()
        itemArticulo = gridView.getItemAtPosition(fPosicion) as ItemArticulo
        val act = this
        contexto = this
        fEditandoCajas = false
        val dlgCjUn = DialogoCajasUnid(contexto, act)
        dlgCjUn.show()
    }

    override fun onOkClick(name: String) {
        if (name != "") {
            if (fEditandoCajas) itemArticulo.cajas =
                name.replace(',', '.').toDouble() else itemArticulo.cantidad =
                name.replace(',', '.').toDouble()
            gridView.invalidateViews()
            // Actualizamos el temporal del histórico con los cambios en la cantidad.
            actualizarTempHco(itemArticulo)
        }
    }
}