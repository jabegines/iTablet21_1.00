package es.albainformatica.albamobileandroid.ventas


import es.albainformatica.albamobileandroid.cobros.PendienteClase
import es.albainformatica.albamobileandroid.cobros.CobrosClase
import es.albainformatica.albamobileandroid.historicos.Historico
import android.content.SharedPreferences
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.content.DialogInterface
import android.annotation.SuppressLint
import android.app.*
import android.view.*
import es.albainformatica.albamobileandroid.biocatalogo.BioCatalogo
import es.albainformatica.albamobileandroid.historicos.CargarHcoPorDoc
import es.albainformatica.albamobileandroid.historicos.CargarHco
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.impresion_informes.ImprimirDocumento
import es.albainformatica.albamobileandroid.impresion_informes.ImprDocDatamaxApex2
import es.albainformatica.albamobileandroid.impresion_informes.ImprIntermecPB51
import es.albainformatica.albamobileandroid.impresion_informes.ImprGenerica
import es.albainformatica.albamobileandroid.impresion_informes.ImprZebra
import es.albainformatica.albamobileandroid.impresion_informes.DocPDF
import es.albainformatica.albamobileandroid.reparto.FirmarDoc
import es.albainformatica.albamobileandroid.cobros.Cobrar
import es.albainformatica.albamobileandroid.cobros.CobrosActivity
import es.albainformatica.albamobileandroid.dao.ContactosCltesDao
import es.albainformatica.albamobileandroid.dao.SeriesDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.ContactosCltesEnt
import es.albainformatica.albamobileandroid.impresion_informes.ImprDocFormato2
import kotlinx.android.synthetic.main.ventas_lineas.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParseException
import java.util.*

/**
 * Created by jabegines on 14/10/13.
 */
class VentasLineas: AppCompatActivity() {
    private lateinit var fDocumento: Documento
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fPendiente: PendienteClase
    private lateinit var fCobros: CobrosClase
    private lateinit var fHistorico: Historico

    private var fEstado: Byte = 0
    private var fIvaIncluido: Boolean = false

    private lateinit var fRecyclerView: RecyclerView
    private lateinit var fAdapter: LineasVtasRvAdapter
    private lateinit var fRecBases: RecyclerView
    private lateinit var fAdpBases: BasesRvAdapter


    private lateinit var tvTotal: TextView
    private var fLinea = 0
    private var fDocNuevo: Boolean = true
    private var fSoloVer: Boolean = false
    private var fAplicarIva: Boolean = true
    private var fUsarPiezas: Boolean = false
    private var fUsarTasa1: Boolean = false
    private var fUsarTasa2: Boolean = false
    private var fNombreTasa1: String = ""
    private var fNombreTasa2: String = ""
    private var fSerie: String = ""
    private var fIdDoc = 0
    private var fTerminar: Boolean = false
    private var fDocImprimido = false

    // Layouts de bases imponibles que mostraremos/ocultaremos
    private lateinit var lyTotales: LinearLayout
    private lateinit var prefs: SharedPreferences
    private var fPrimeraVez: Boolean = false // Nos sirve para saber si es la primera vez que entramos a nueva l??nea o no.
    private var modoVenta = 0
    private var fDocEnviarGuardar = 0
    private lateinit var fQueData: Intent
    private lateinit var btnTerminar: Button

    // Formatos
    private var fFtoDecImpIva: String = ""
    private var fFtoDecImpBase: String = ""
    private var fFtoDecPrIva: String = ""
    private var fFtoDecPrBase: String = ""
    private var fFtoDecCant: String = ""

    // Request de las actividades a las que llamamos.
    private val fRequestPieDoc = 1
    private val fRequestHistorico = 2
    private val fRequestCobros = 3
    private val fRequestImprimirExportar = 4
    private val fRequestNuevaLinea = 5
    private val fRequestEditarLinea = 6
    private val fRequestFirmarDoc = 7
    private val fRequestVerRiesgo = 8



    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.ventas_lineas)

        fDocumento = Documento(this)
        fConfiguracion = Comunicador.fConfiguracion
        fPendiente = PendienteClase(this)
        fCobros = CobrosClase(this)
        fHistorico = Historico(this)

        // Leemos las preferencias de la aplicaci??n;
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val intent = intent
        if (inicializarDocumento(intent)) {
            inicializarControles()
        }
    }

    override fun onDestroy() {
        fDocumento.close()
        super.onDestroy()
    }

    private fun inicializarDocumento(intent: Intent): Boolean {
        var continuar = true
        fDocNuevo = intent.getBooleanExtra("nuevo", true)
        fSoloVer = intent.getBooleanExtra("solover", false)
        fIdDoc = 0
        // Comprobamos si podemos aplicar la tarifa de cajas en el documento, por si nos hiciera falta para alg??n art??culo.
        // Hacemos esta comprobaci??n porque hay quien tiene art??culos con tarifa de cajas pero no env??an la tarifa de cajas
        // a la tablet, y quieren que en este caso se aplique la tarifa normal.
        fDocumento.poderAplTrfCajas()
        fDocumento.fHayArtHabituales = fDocumento.hayArtHabituales()
        // Vemos si queremos aplicar ofertas en los pedidos
        fDocumento.fAplOftEnPed = intent.getBooleanExtra("aplOftEnPed", true)
        if (fDocNuevo) {
            fDocumento.setCliente(intent.getIntExtra("cliente", 0))
            fDocumento.fTipoDoc = intent.getShortExtra("tipodoc", 0.toShort())
            fDocumento.fTipoPedido = intent.getShortExtra("tipopedido", 0)
            fSerie = intent.getStringExtra("serie") ?: ""
            if (!fDocumento.setSerieNumero(fSerie)) {
                val returnIntent = Intent()
                setResult(RESULT_CANCELED, returnIntent)
                // Este finish() aqu??, en este punto, me ha dado todos los dolores de cabeza habidos y por haber.
                // Resulta que a Android, aunque le indiquemos que queremos finalizar, le da igual: har?? el finish() cuando
                // le venga bien, despu??s de ejecutar todas las instrucciones que tenga pendientes de ejecutar. ??Que ocurr??a?.
                // Pues que despu??s de inicializarDocumento() llam??bamos a inicializarControles() y ??ste, a su vez, a nuevaLinea(),
                // por lo que se mostraba la actividad de VentasDatosLinea mientras, por debajo, se destru??a ??sta (VentasLineas).
                // Al destruirse VentasLineas se destruyen tambi??n los objetos creados en ella, entre ellos fDocumento, por eso
                // nos daba el error: 'sqlite database already closed'. Ahora, al hacer el 'return false' s?? salimos de esta
                // funci??n y no ejecutamos inicializarControles().
                finish()
                return false
            }
            // Vemos si el documento es exento o no
            fDocumento.setExento()

        } else {
            fIdDoc = intent.getIntExtra("iddoc", 0)
            val queTipoDoc = intent.getShortExtra("tipodoc", 0.toShort())
            if (queTipoDoc == TIPODOC_FACTURA) fDocumento.cargarFactura(fIdDoc, !fSoloVer)
            else fDocumento.cargarDocumento(fIdDoc, !fSoloVer)

            // Tenemos que llamar a fDocumento.calcularDtosPie() porque el documento no trae los descuentos a pie calculados,
            // ya que fDocumento.cargarDocumento() no lo hace. Esto lo hacemos si estamos visualizando, en caso de modificar no lo hacemos.
            if (fSoloVer) fDocumento.calcularDtosPie()

            // Si el documento es de contado no permitiremos la modificaci??n.
            if (fDocumento.fTipoDoc == TIPODOC_FACTURA && !fSoloVer) {
                if (fDocumento.esContado()) {
                    continuar = false
                    val aldDialog = nuevoAlertBuilder(this, "Salir", resources.getString(R.string.msj_DocContado), false)
                    aldDialog.setPositiveButton("OK") { _: DialogInterface?, _: Int -> finish() }
                    val alert = aldDialog.create()
                    alert.show()
                }
            }
        }
        if (continuar) {
            fLinea = 0
            fAplicarIva = fDocumento.fAplicarIva

            // Uso Comunicador para tener una referencia al objeto
            // fDocumento tambi??n desde CargarHco.java y desde Pendiente.java.
            Comunicador.fDocumento = fDocumento

            // Compartimos el objeto fHistorico.
            Comunicador.fHistorico = fHistorico
        }
        return continuar
    }

    @SuppressLint("SetTextI18n")
    private fun inicializarControles() {
        val tvNombreClte = findViewById<TextView>(R.id.tvVL_Clte)
        val tvNComClte = findViewById<TextView>(R.id.tvVL_NComClte)
        val lyNuevaLin = findViewById<LinearLayout>(R.id.llyVl_Nueva)
        val btnEditarLin = findViewById<Button>(R.id.btnVL_Editar)
        val btnBorrarLin = findViewById<Button>(R.id.btnVL_Borrar)
        val btnHco = findViewById<Button>(R.id.btnVL_Hco)
        btnTerminar = findViewById(R.id.btnVL_Terminar)
        lyTotales = findViewById(R.id.llyVL_TotalesDoc)

        // Establecemos la visibilidad de los layouts de totales para dejarla igual
        // que la ??ltima vez que estuvimos vendiendo.
        if (prefs.getBoolean("verTotales", true)) {
            lyTotales.visibility = View.VISIBLE
        } else {
            lyTotales.visibility = View.GONE
        }

        // Tomo en cuenta si s??lo estoy viendo.
        lyNuevaLin.isEnabled = !fSoloVer
        btnEditarLin.isEnabled = !fSoloVer
        btnBorrarLin.isEnabled = !fSoloVer
        btnHco.isEnabled = !fSoloVer
        btnTerminar.isEnabled = !fSoloVer

        // Si s??lo estamos viendo no aparecer??n los botones de Modificar ni Borrar
        if (fSoloVer) {
            btnEditarLin.visibility = View.GONE
            btnBorrarLin.visibility = View.GONE
        }
        fFtoDecImpIva = fConfiguracion.formatoDecImptesIva()
        fFtoDecImpBase = fConfiguracion.formatoDecImptesBase()
        fFtoDecPrIva = fConfiguracion.formatoDecPrecioIva()
        fFtoDecPrBase = fConfiguracion.formatoDecPrecioBase()
        fFtoDecCant = fConfiguracion.formatoDecCantidad()

        fUsarPiezas = fConfiguracion.usarPiezas()
        fUsarTasa1 = fConfiguracion.usarTasa1()
        fUsarTasa2 = fConfiguracion.usarTasa2()
        fNombreTasa1 = fConfiguracion.nombreTasa1()
        fNombreTasa2 = fConfiguracion.nombreTasa2()
        indicarTipoDoc()
        tvNombreClte.text = fDocumento.fClientes.fCodigo + " - " + fDocumento.nombreCliente()
        tvNComClte.text = fDocumento.nombreComClte()
        fEstado = est_Vl_Browse

        val fSeriesDao: SeriesDao? = MyDatabase.getInstance(this)?.seriesDao()
        val queFlagSerie = fSeriesDao?.getFlag(fDocumento.serie, fDocumento.fEjercicio) ?: 0
        val fForzarPrIvaIncl = queFlagSerie and FLAGSERIE_FORZAR_PR_IVA_INCL > 0
        fIvaIncluido = fForzarPrIvaIncl || fConfiguracion.ivaIncluido(fDocumento.fEmpresa)

        fRecyclerView = rvVL_LineasDoc
        fRecyclerView.layoutManager = LinearLayoutManager(this)
        prepararRecyclerView()
        prepararBases()

        // Vemos como tenemos configurado el modo de venta
        var sModoVenta = prefs.getString("modo_venta", "1")
        if (sModoVenta == null) sModoVenta = "1"
        modoVenta = sModoVenta.toInt()
        fPrimeraVez = true
        // Si el modo de venta que tenemos configurado es el de Hist??rico activaremos ??ste
        // la primera vez que entremos en el documento. Si no tenemos este modo configurado
        // entramos en nueva l??nea del tir??n.
        if (fDocNuevo) {
            if (modoVenta == mVta_Historico) {
                fPrimeraVez = false
                cargarHco(null)
            } else nuevaLinea(null)
        }
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.ventas)
    }


    private fun prepararRecyclerView() {
        fAdapter = LineasVtasRvAdapter(getLineasDoc(), fIvaIncluido, fAplicarIva, this, object: LineasVtasRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosLinVtas) {
                fLinea = data.lineaId
            }
        })

        fRecyclerView.adapter = fAdapter
    }


    private fun getLineasDoc(): List<DatosLinVtas> {
        fDocumento.abrirLineas()
        return fDocumento.lLineas
    }


    private fun prepararBases() {
        tvTotal = findViewById(R.id.tvVL_Total)
        tvTotal.text = String.format(fFtoDecImpIva, fDocumento.fBases.totalConImptos)

        fAdpBases = BasesRvAdapter(fDocumento.fBases.fLista, this, object: BasesRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: ListaBasesDoc.TBaseDocumento) {}
        })

        fRecBases = findViewById(R.id.rvVl_BasesDoc)
        fRecBases.layoutManager = LinearLayoutManager(this)
        fRecBases.adapter = fAdpBases
    }


    private fun refrescarLineas() {
        prepararRecyclerView()
        fAdpBases.notifyDataSetChanged()
        tvTotal.text = String.format(fFtoDecImpIva, fDocumento.fBases.totalConImptos)
    }


    fun nuevaLinea(view: View?) {
        view?.getTag(0)              // Para que no d?? warning el compilador

        if (fEstado == est_Vl_Browse) {
            if (modoVenta == mVta_Catalogo) {
                val i = Intent(this, BioCatalogo::class.java)
                startActivityForResult(i, fRequestNuevaLinea)
            } else {
                fEstado = est_Vl_Nueva
                val i = Intent(this, VentasDatosLinea::class.java)
                i.putExtra("estado", fEstado)
                i.putExtra("primera_vez", fPrimeraVez)
                fPrimeraVez = false
                startActivityForResult(i, fRequestNuevaLinea)
            }
        }
    }

    fun editarLinea(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        if (fEstado == est_Vl_Browse && fDocumento.lLineas.isNotEmpty()) {
            if (fLinea > 0) {

                // Desactivo el adapter del listView porque he detectado que al movernos
                // al layout de edici??n de la l??nea, el cursor (fDocumento.cLineas) se
                // mueve al ??ltimo registro, por lo que realmente no estamos modificando
                // la l??nea que queremos, sino siempre la ??ltima.
                fEstado = est_Vl_Editar
                val i = Intent(this, VentasDatosLinea::class.java)
                i.putExtra("estado", fEstado)
                i.putExtra("numlinea", fLinea)
                startActivityForResult(i, fRequestEditarLinea)
            } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
        }
    }

    fun borrarLinea(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        if (fEstado == est_Vl_Browse && fDocumento.lLineas.isNotEmpty()) {
            if (fLinea > 0) {
                for (linea in fDocumento.lLineas) {
                    if (linea.lineaId == fLinea)
                        fDocumento.borrarLinea(linea, true)
                }
                refrescarLineas()
                fLinea = 0
            } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
        }
    }

    fun cargarHco(view: View?) {
        view?.getTag(0)              // Para que no d?? warning el compilador

        if (fEstado == est_Vl_Browse) {
            if (fConfiguracion.usarHcoPorArticulo()) {
                val i = Intent(this, CargarHcoPorDoc::class.java)
                i.putExtra("cliente", fDocumento.fCliente)
                startActivityForResult(i, fRequestHistorico)
            } else {
                val i = Intent(this, CargarHco::class.java)
                i.putExtra("cliente", fDocumento.fCliente)
                i.putExtra("aplicariva", fDocumento.fClientes.fAplIva)
                i.putExtra("desdecltes", false)
                i.putExtra("empresa", fDocumento.fEmpresa.toString().toInt())
                startActivityForResult(i, fRequestHistorico)
            }
        }
    }

    fun terminarDoc(view: View) {
        // Comprobamos que el documento tenga alguna l??nea.
        if (fDocumento.lLineas.isNotEmpty()) {
            // Deshabilitamos el bot??n para que no lo pulsemos varias veces (p.ej. cuando est?? calculando las ofertas por volumen)
            view.isEnabled = false

            // Si estamos haciendo un pedido comprobaremos que queremos aplicar ofertas en el mismo
            // Comprobamos si tenemos ofertas por volumen para a??adir las l??neas de descuento.
            if (fDocumento.fTipoDoc != TIPODOC_PEDIDO || fDocumento.fAplOftEnPed) {
                if (fDocumento.fClientes.getAplicarOfertas()) fDocumento.verOftVolumen()
            }

            // Si tenemos alguna oferta por volumen la mostramos antes de terminar el documento
            if (fDocumento.hayOftVolumen()) mostrarOftVolumenDoc() else llamarPieDoc()
        }
    }

    private fun mostrarOftVolumenDoc() {
        // Lo primero que debemos hacer es rescatar el layout creado para el prompt.
        val li = LayoutInflater.from(this)
        val prompt = li.inflate(R.layout.mostrar_oft_vol_doc, null)
        prepararRecVOftVolDoc(prompt)

        // Luego, creamos un constructor de Alert Dialog que nos ayudar?? a utilizar nuestro layout.
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(prompt)
        // Por ultimo, creamos el cuadro de dialogo y las acciones requeridas al aceptar o cancelar el prompt.
        // Mostramos el mensaje del cuadro de di??logo
        alertDialogBuilder.setPositiveButton("Aceptar") { _: DialogInterface?, _: Int -> llamarPieDoc() }
        alertDialogBuilder.setCancelable(false)

        // Creamos un AlertDialog y lo mostramos
        val alertDialog = alertDialogBuilder.create()
        // El c??digo que viene a continuaci??n lo usamos para presentar el di??logo en la parte de la pantalla que queramos.
        val wmlp = Objects.requireNonNull(alertDialog.window)?.attributes
        wmlp?.gravity = Gravity.TOP
        wmlp?.y = 200
        alertDialog.show()
    }

    private fun prepararRecVOftVolDoc(prompt: View) {
        val rvOftVolDoc = prompt.findViewById<RecyclerView>(R.id.rvOftVolDoc)
        rvOftVolDoc.layoutManager = LinearLayoutManager(this)

        val fAdpOftVol = OftVolDocRvAdapter(getOftVolDoc(), this, object: OftVolDocRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosOftVol) {
            }
        })

        rvOftVolDoc.adapter = fAdpOftVol
    }

    private fun getOftVolDoc(): List<DatosOftVol> {
        return fDocumento.cargarListaOftVol()
    }



    private fun indicarTipoDoc() {
        val tvTipoDoc = findViewById<TextView>(R.id.tvVL_TipoDoc)
        val tvSerieNum = findViewById<TextView>(R.id.tvVL_SerieNum)
        tvTipoDoc.text = tipoDocAsString(fDocumento.fTipoDoc)
        tvSerieNum.text = fDocumento.serie + '/' + fDocumento.numero
    }

    private fun llamarPieDoc() {
        val i = Intent(this, VentasFinDoc::class.java)
        i.putExtra("iddoc", fIdDoc)
        i.putExtra("terminar", true)
        startActivityForResult(i, fRequestPieDoc)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Actividad editarlinea
        if (requestCode == fRequestEditarLinea) {
            // Volvemos a activar el adaptador del listView, porque en editarLinea lo desactivamos.
            if (resultCode == RESULT_OK) refrescarLineas()
            fEstado = est_Vl_Browse

        // Nueva linea.
        } else if (requestCode == fRequestNuevaLinea) {
            refrescarLineas()
            fEstado = est_Vl_Browse

        // Actividad pie de documento.
        } else if (requestCode == fRequestPieDoc) {
            if (resultCode == RESULT_OK) {
                // Guardamos data en fQueData porque nos har?? falta en grabarPieDoc().
                fQueData = data ?: Intent()

                // Si tenemos configurado 'Clasificar documentos como Enviar/Guardar' pediremos qu?? hacer con ??l.
                if (prefs.getBoolean("ventas_enviar_guardar", false)) {
                    // Creamos un objeto de la clase EnviarOGuardar y lo mostramos para elegir si guardamos o est?? listo para el env??o.
                    val newFragment: DialogFragment = EnviarOGuardar.newInstance(R.string.app_name)
                    newFragment.show(fragmentManager, "dialog")

                } else grabarPieDoc()
            } else {
                // Si hemos a??adido ofertas por volumen las borramos porque seguiremos editando el documento
                fDocumento.borrarOftVolumen(true)
                // Y activamos el bot??n que previamente habiamos desactivado
                btnTerminar.isEnabled = !fSoloVer
            }

        // Actividad hist??rico.
        } else if (requestCode == fRequestHistorico) {
            if (resultCode == RESULT_OK) fDocumento.grabarHistorico()
            refrescarLineas()

        // Actividad cobros de factura.
        } else if (requestCode == fRequestCobros) {
            if (fTerminar) {
                pedirFirma()
            } else continuarFinDoc()

        // Actividad firmar documento
        } else if (requestCode == fRequestFirmarDoc) {
            if (resultCode == RESULT_OK) fDocumento.marcarComoEntregado(
                fDocumento.fIdDoc,
                fDocumento.fCliente,
                fDocumento.fEmpresa.toInt(),
                false
            )
            imprimirDoc()

        // Actividad imprimir.
        } else if (requestCode == fRequestImprimirExportar) {
            finalizarVenta()
        } else if (requestCode == fRequestVerRiesgo) {
            if (resultCode == RESULT_OK) {
                terminaGrabarPieDoc()
            } else {
                // Activamos el bot??n que previamente habiamos desactivado
                //btnTerminar.setEnabled(!fSoloVer);
                llamarPieDoc()
            }
        }
    }

    private fun finalizarVenta() {
        // Marcamos el documento como imprimido para no permitir modificarlo (si es que lo hemos imprimido)
        if (fDocImprimido) fDocumento.marcarComoImprimido(fDocumento.fIdDoc, fDocumento.fTipoDoc)

        // Por ahora hemos desabilitado la comprobaci??n del riesgo, ya que la hacemos de otra manera en grabarPieDoc()
        val returnIntent = Intent()
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    private fun imprimirDoc() {
        var fExportar = prefs.getBoolean("ventas_exportar_pdf", false)
        if (fExportar && fDocumento.fTipoDoc == TIPODOC_PEDIDO) {
            if (prefs.getBoolean("ventas_enviar_guardar", false))
                fExportar = fDocEnviarGuardar == 1
        } else fExportar = false

        if (fConfiguracion.imprimir() || fExportar) {

            // Comprobamos si tenemos alguna impresora configurada para imprimir
            val mDeviceAddress: String = prefs.getString("impresoraBT", "") ?: ""
            if (fExportar || mDeviceAddress != "") {

                // Volvemos a recalcular el documento, para que no pase lo que le ha pasado a Artesantequera,
                // que le ha imprimido tres l??neas y las bases corresponden s??lo a dos de ellas.
                fDocumento.recalcularBases()
                val imprDoc = ImprimirDocumento(this@VentasLineas)
                val imprDocDtmApex2 = ImprDocDatamaxApex2(this@VentasLineas)
                val imprDocIntermec = ImprIntermecPB51(this)
                val imprDocBixolon = ImprGenerica(this)
                val imprZebra = ImprZebra(this)

                // Lo primero que debemos hacer es rescatar el layout creado para el prompt.
                val li = LayoutInflater.from(this)
                val prompt = li.inflate(R.layout.imprimir_doc, null)
                // Luego, creamos un constructor de Alert Dialog que nos ayudar?? a utilizar nuestro layout.
                val alertDialogBuilder = AlertDialog.Builder(this)
                alertDialogBuilder.setView(prompt)

                // Por ultimo, creamos el cuadro de dialogo y las acciones requeridas al aceptar o cancelar el prompt.
                // Creamos un AlertDialog y lo mostramos
                val alertDialog = alertDialogBuilder.create()
                alertDialog.setCancelable(false)
                alertDialog.show()

                // Establecemos los eventos para los distintos botones del layout del di??logo.
                val btnImpr = alertDialog.findViewById<Button>(R.id.btnImprDoc)
                if (fConfiguracion.imprimir()) {
                    btnImpr.setOnClickListener {
                        fDocImprimido = true
                        val chkSinValorar = alertDialog.findViewById<CheckBox>(es.albainformatica.albamobileandroid.R.id.chkSinValorar)
                        val btnNoImpr = alertDialog.findViewById<Button>(es.albainformatica.albamobileandroid.R.id.btnNoImpr)
                        btnNoImpr.visibility = View.GONE
                        chkSinValorar.visibility = View.GONE
                        comenzarImprDoc(chkSinValorar.isChecked, imprDocDtmApex2, imprDocIntermec,
                            imprDocBixolon, imprZebra, imprDoc, alertDialog, btnImpr)
                    }
                } else btnImpr.visibility = View.GONE
                val btnNoImpr = alertDialog.findViewById<Button>(es.albainformatica.albamobileandroid.R.id.btnNoImpr)
                btnNoImpr.setOnClickListener {
                    alertDialog.cancel()
                    finalizarVenta()
                }

                // Establecemos la visibilidad del check 'Sin Valorar'
                val chkSinValorar =
                    alertDialog.findViewById<CheckBox>(es.albainformatica.albamobileandroid.R.id.chkSinValorar)
                if (fConfiguracion.imprimir()) {
                    // Si el documento no es albar??n ni pedido no presentaremos el bot??n para imprimir sin valorar
                    if (fDocumento.fTipoDoc != TIPODOC_ALBARAN && fDocumento.fTipoDoc != TIPODOC_PEDIDO) {
                        chkSinValorar.visibility = View.GONE
                    }
                } else chkSinValorar.visibility = View.GONE

                // Comprobamos si tenemos algun email para enviar. Si no, desactivamos la exportacion a PDF.
                val documPDF = DocPDF(this@VentasLineas)
                val btnExpPDF = alertDialog.findViewById<Button>(R.id.btnExpPDFDoc)

                if (documPDF.dimeNumEmailsClte() > 0) {
                    btnExpPDF.setOnClickListener { v: View ->
                        fDocImprimido = true
                        documPDF.crearPDF()

                        // Comprobamos si el Whatsapp est?? instalado
                        if (whatsappInstalado(this)) {
                            val aldDialog =
                                nuevoAlertBuilder(this, "Escoja", "Enviar documento PDF", true)
                            aldDialog.setPositiveButton("Por email") { _: DialogInterface?, _: Int ->
                                //Toast.makeText(VentasLineas.this, getString(R.string.tst_docpdf), Toast.LENGTH_LONG).show();
                                documPDF.enviarPorEmail()
                                v.isEnabled = false
                            }
                            aldDialog.setNegativeButton("Por whatsapp") { _: DialogInterface?, _: Int ->
                                val telfDao: ContactosCltesDao? =
                                    MyDatabase.getInstance(this@VentasLineas)?.contactosCltesDao()
                                val lTelfs = telfDao?.getTlfsCliente(fDocumento.fCliente)
                                    ?: emptyList<ContactosCltesEnt>().toMutableList()
                                var numeroTelefono = lTelfs[0].telefono1
                                if (numeroTelefono == "") numeroTelefono = lTelfs[0].telefono2
                                // Si no a??adimos el prefijo no funciona
                                if (!numeroTelefono.startsWith("34"))
                                    numeroTelefono = "34$numeroTelefono"
                                enviarPorWhatsapPdf(this, documPDF.nombrePDF, numeroTelefono)
                                v.isEnabled = false
                            }
                            aldDialog.setCancelable(true)
                            val alert = aldDialog.create()
                            alert.show()
                        } else {
                            documPDF.enviarPorEmail()
                        }
                    }
                } else btnExpPDF.visibility = View.GONE
            }
            else finalizarVenta()
        }
        else {
            finalizarVenta()
        }
    }

    private fun comenzarImprDoc(
        fImprSinValorar: Boolean,
        imprDocDtmApex2: ImprDocDatamaxApex2,
        imprDocIntermec: ImprIntermecPB51,
        imprDocBixolon: ImprGenerica,
        imprZebra: ImprZebra,
        imprDoc: ImprimirDocumento,
        alertDialog: AlertDialog,
        queBoton: Button
    ) {
        // Vemos si tenemos que pedir el formato con el que queremos imprimir o no.
        if (fConfiguracion.pedirFormato()) {
            val newFragment: DialogFragment =
                DlgSeleccFormato.newInstance(R.string.app_name)
            newFragment.show(fragmentManager, "dialog")
        } else {
            // Una vez que hemos pulsado en el bot??n Imprimir cambiamos el texto de ??ste y
            // comprobaremos si hemos terminado de imprimir para, en este caso, terminar el di??logo.
            // Por ello, el bot??n seguir?? estando activo y no hacemos v.setEnabled(false).
            // Lo hacemos as?? porque cuando ten??amos el di??logo como cancelable, si el usuario pulsaba
            // en el bot??n Cancelar antes de que la impresora terminara de imprimir se imprim??a el total
            // del documento a cero, porque el objeto imprDoc ya no tiene acceso al documento que est?? imprimiendo,
            // ya que habr??amos ejecutado finalizarVenta() antes de terminar de imprimir.

            // Vemos el tipo de impresora por el que vamos a imprimir.
            if (fConfiguracion.impresora() == IMPRESORA_DATAMAX_APEX_2) {
                if (imprDocDtmApex2.fTerminado) {
                    alertDialog.cancel()
                    finalizarVenta()
                } else {
                    if (imprDocDtmApex2.fImprimiendo) Toast.makeText(
                        this@VentasLineas,
                        "Espere a que termine la impresi??n",
                        Toast.LENGTH_SHORT
                    ).show() else imprDocDtmApex2.imprimir()
                }
            } else if (fConfiguracion.impresora() == IMPRESORA_INTERMEC_PB51) {
                if (imprDocIntermec.fTerminado) {
                    alertDialog.cancel()
                    finalizarVenta()
                } else {
                    if (imprDocIntermec.fImprimiendo) Toast.makeText(
                        this@VentasLineas,
                        "Espere a que termine la impresi??n",
                        Toast.LENGTH_SHORT
                    ).show() else imprDocIntermec.imprimir()
                }
            } else if (fConfiguracion.impresora() == IMPRESORA_BIXOLON_SPP_R410 || fConfiguracion.impresora() == IMPRESORA_GENERICA_110 || fConfiguracion.impresora() == IMPRESORA_GENERICA_80) {
                if (imprDocBixolon.fTerminado) {
                    alertDialog.cancel()
                    finalizarVenta()
                } else {
                    if (imprDocBixolon.fImprimiendo) Toast.makeText(
                        this@VentasLineas,
                        "Espere a que termine la impresi??n",
                        Toast.LENGTH_SHORT
                    ).show() else imprDocBixolon.imprimir()
                }
            } else if (fConfiguracion.impresora() == IMPRESORA_ZEBRA_80) {
                if (imprZebra.fTerminado) {
                    alertDialog.cancel()
                    finalizarVenta()
                } else {
                    if (imprZebra.fImprimiendo) Toast.makeText(
                        this@VentasLineas,
                        "Espere a que termine la impresi??n",
                        Toast.LENGTH_SHORT
                    ).show() else imprZebra.imprimir()
                }
            } else {
                if (imprDoc.fTerminado) {
                    alertDialog.cancel()
                    finalizarVenta()
                } else {
                    if (imprDoc.fImprimiendo) Toast.makeText(
                        this@VentasLineas,
                        "Espere a que termine la impresi??n",
                        Toast.LENGTH_SHORT
                    ).show() else imprDoc.imprimir(fImprSinValorar)
                }
            }
            queBoton.text = resources.getString(R.string.salir)
        }
    }

    private fun pedirFirma() {
        if (fConfiguracion.activarFirmaDigital()) {
            // Si estamos modificando no volveremos a pedir la firma.
            if (fDocNuevo) {
                val i = Intent(this, FirmarDoc::class.java)
                i.putExtra("id_doc", fDocumento.fIdDoc)
                i.putExtra("tipo_doc", fDocumento.fTipoDoc)
                startActivityForResult(i, fRequestFirmarDoc)
            } else imprimirDoc()
        } else imprimirDoc()
    }

    private fun grabarPieDoc() {
        fDocumento.fObs1 = fQueData.getStringExtra("obs1") ?: ""
        fDocumento.fObs2 = fQueData.getStringExtra("obs2") ?: ""
        // Si estamos haciendo un pedido, recogemos la fecha de entrega.
        if (fDocumento.fTipoDoc == TIPODOC_PEDIDO)
            fDocumento.fFEntrega = fQueData.getStringExtra("fentrega") ?: ""
        fDocumento.fDtoPie1 = fQueData.getDoubleExtra("dto1", 0.0)
        fDocumento.fDtoPie2 = fQueData.getDoubleExtra("dto2", 0.0)
        fDocumento.fDtoPie3 = fQueData.getDoubleExtra("dto3", 0.0)
        fDocumento.fDtoPie4 = fQueData.getDoubleExtra("dto4", 0.0)
        fTerminar = fQueData.getBooleanExtra("terminar", false)
        // Si estamos haciendo una factura establecemos la forma de pago, que nos
        // servir?? luego para insertar en la tabla Pendiente. Si estamos haciendo un pedido tambi??n
        // tomamos la forma de pago para grabarla en la cabecera.
        if (fDocumento.fTipoDoc == TIPODOC_FACTURA || fDocumento.fTipoDoc == TIPODOC_PEDIDO)
            fDocumento.fPago = fQueData.getStringExtra("fpago") ?: ""
        fDocumento.calcularDtosPie()

        // Si estamos haciendo un documento nuevo controlamos el riesgo del cliente.
        // No permitiremos terminar el documento si superamos el riesgo y el usuario no lo acepta.
        val continuar: Boolean
        var queTotal = 0.0
        var queNumDocs = 0
        if (fDocNuevo) {
            queTotal = fDocumento.fBases.totalConImptos
            if (fDocumento.fTipoDoc == TIPODOC_FACTURA) queNumDocs = 1 else {
                if (fDocumento.fTipoDoc == TIPODOC_ALBARAN && fConfiguracion.cobrosPorAlbaran()) queNumDocs =
                    1
            }
            // Continuaremos si el documento es de contado o si es un pedido o un albar??n y no pedimos cobros por albar??n.
            // En cambio, si es un documento a cr??dito o un albar??n y pedimos cobros por albar??n, continuaremos si el riesgo nos deja.
            continuar =
                (fDocumento.fTipoDoc == TIPODOC_FACTURA && fDocumento.docNuevoEsContado()
                        || queNumDocs == 0 || !fDocumento.fClientes.clienteEnRiesgo(
                    queTotal,
                    queNumDocs,
                    fDocumento.fEmpresa.toInt()
                ))
        } else continuar = true

        if (continuar) {
            terminaGrabarPieDoc()
        } else {
            val i = Intent(this, VerRiesgo::class.java)
            i.putExtra("cliente", fDocumento.fCliente)
            i.putExtra("empresa", fDocumento.fEmpresa)
            i.putExtra("totalDoc", queTotal)
            i.putExtra("numDocs", queNumDocs)
            i.putExtra("soloVer", false)
            startActivityForResult(i, fRequestVerRiesgo)
        }
    }

    private fun terminaGrabarPieDoc() {
        var queEstado = "N"
        if (fDocEnviarGuardar == 2) queEstado = "P"
        fDocumento.terminarDoc(fDocNuevo, queEstado)

        // Si estamos haciendo una factura pediremos el cobro y, si no tenemos que
        // hacer m??s documentos (fTerminar = true), desde onActivityResult haremos
        // el finish() despu??s de haber lanzado la actividad Cobrar. Si no,
        // comprobaremos si tenemos m??s documentos por hacer o si terminamos.
        // Tambi??n pediremos cobro si estamos haciendo un albar??n y tenemos configurado pedir cobro por albar??n
        // o estamos en reparto.
        if (fDocumento.fTipoDoc == TIPODOC_FACTURA ||
            fDocumento.fTipoDoc == TIPODOC_ALBARAN && fConfiguracion.pedirEntregasAlbaranes()
        ) //fConfiguracion.cobrosPorAlbaran()))
            cobrosFra() else {
            if (fTerminar) {
                pedirFirma()
            } else continuarFinDoc()
        }
    }

    private fun continuarFinDoc() {
        if (fDocNuevo) fDocumento.inicializarPie()
        val i = Intent(this, VentasFinDoc::class.java)
        i.putExtra("iddoc", fIdDoc)
        i.putExtra("terminar", false)
        startActivityForResult(i, fRequestPieDoc)

        // Mirar esto, a ver qu?? pasa con pedirFirma().
        //pedirFirma();
    }

    private fun cobrosFra() {
        // Si estamos modificando un documento borraremos el posible cobro y el
        // apunte en la tabla Pendiente porque, a continuacion, los volveremos a pedir.
        if (!fDocNuevo) {
            fPendiente.borrarPdteDoc(
                fDocumento.fTipoDoc,
                fDocumento.fEmpresa,
                fDocumento.fAlmacen,
                fDocumento.serie,
                fDocumento.numero,
                fDocumento.fEjercicio,
                fDocumento.fCliente,
                fDocumento.fBases.totalConImptos
            )
            // Antes de borrar los cobros del documento, vemos lo cobrado y actualizamos el saldo del cliente.
            val fCobrado = fCobros.impCobradoDoc()
            actualizarSaldo(this, fDocumento.fCliente, fDocumento.fEmpresa, fCobrado)

            // Ahora borramos los cobros del documento.
            fCobros.borrarCobroDoc()
        }

        // Si el documento tiene un total de 0 euros no haremos nada, porque de otra manera crear??a un vencimiento
        // con importe 0 que en gesti??n no se podr??a borrar ni modificar.
        val fImporte = fDocumento.fBases.totalConImptos

        // Inserto en la tabla Pendiente. Me quedo con el ID reci??n insertado para,
        // a continuaci??n, abrir el cursor s??lo con dicho registro.
        val fIdPendiente = fPendiente.nuevoDoc()
        fPendiente.abrirPendienteId(fIdPendiente.toInt())
        if (fImporte != 0.0 && (fDocumento.fTipoDoc == TIPODOC_FACTURA || fDocumento.fTipoDoc == TIPODOC_ALBARAN && fConfiguracion.pedirEntregasAlbaranes())) {
            // Hacemos fCobros.abrir() porque luego, al hacer fCobros.nuevoCobro,
            // necesita el cursor abierto (para hacer refresh). Esto se podria cambiar
            // si quisieramos, mandando una se??al a fCobros.nuevoCobro para que refresque o no.
            //fCobros.abrir(fDocumento.fCliente);
            // Pedimos el cobro.
            Comunicador.fCobros = fCobros
            Comunicador.fPendiente = fPendiente
            val i = Intent(this, Cobrar::class.java)
            i.putExtra("espendiente", true)
            i.putExtra("fpagoDoc", fDocumento.fPago)
            i.putExtra("desdeventas", true)
            i.putExtra("tipoDoc", fDocumento.fTipoDoc)
            startActivityForResult(i, fRequestCobros)
        } else {
            if (fTerminar) {
                pedirFirma()
            } else continuarFinDoc()
        }
    }

    fun pendienteClte(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        val i = Intent(this, CobrosActivity::class.java)
        i.putExtra("cliente", fDocumento.fCliente)
        startActivity(i)
    }

    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (fEstado == est_Vl_Browse) {
                // Si el usuario est?? modificando no dejar?? que anule la modificaci??n,
                // ya que trabajo directamente sobre la tabla de l??neas. La PDA
                // funciona igual, para salir de una modificaci??n de un documento habr?? que grabarlo.
                if (fDocNuevo) {
                    val aldDialog = nuevoAlertBuilder(this, "Salir", "??Anular el documento?", true)
                    aldDialog.setPositiveButton("S??") { _: DialogInterface?, _: Int ->
                        fDocumento.anularDocumento()
                        val returnIntent = Intent()
                        setResult(RESULT_OK, returnIntent)
                        finish()
                    }
                    val alert = aldDialog.create()
                    alert.show()

                    // Si s??lo estamos viendo, saldremos.
                } else if (fSoloVer) {
                    val returnIntent = Intent()
                    setResult(RESULT_OK, returnIntent)
                    finish()
                }
                // Si el listener devuelve true, significa que el evento est?? procesado, y nadie debe hacer nada m??s.
                return true
            }
        }
        // Para las dem??s cosas, se reenv??a el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }

    fun dialogoCambiarTipoDoc(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        // Por ahora s??lo permitiremos cambiar el tipo de documento cuando lo estamos haciendo nuevo.
        // Si estuvi??ramos modificando tendr??amos que cambiar tambi??n el registro de la tabla cabecera.
        if (fDocNuevo) {
            val dialog = SeleccDocFragment()
            dialog.show(supportFragmentManager, "customDialog")
        }
    }

    fun cambiarTipoDoc(item: Int) {
        val nuevoTipoDoc = tipoDocElegido(item)
        if (fDocumento.fTipoDoc != nuevoTipoDoc) {
            fDocumento.fTipoDoc = nuevoTipoDoc

            // Establecemos la serie y el n??mero del documento.
            if (!fDocumento.setSerieNumero(fSerie)) {
                val returnIntent = Intent()
                setResult(RESULT_CANCELED, returnIntent)
                finish()
            }
            indicarTipoDoc()
        }
    }

    private fun tipoDocElegido(item: Int): Short {
        var nuevoTipoDoc = fDocumento.fTipoDoc
        var numOpciones = -1
        var opFra = -1
        var opAlb = -1
        var opPed = -1
        var opPres = -1
        if (fConfiguracion.hacerFacturas()) {
            numOpciones++
            opFra = numOpciones
        }
        if (fConfiguracion.hacerAlbaranes()) {
            numOpciones++
            opAlb = numOpciones
        }
        if (fConfiguracion.hacerPedidos()) {
            numOpciones++
            opPed = numOpciones
        }
        if (fConfiguracion.hacerPresup()) {
            numOpciones++
            opPres = numOpciones
        }
        if (item == opFra) nuevoTipoDoc = TIPODOC_FACTURA
        if (item == opAlb) nuevoTipoDoc = TIPODOC_ALBARAN
        if (item == opPed) nuevoTipoDoc = TIPODOC_PEDIDO
        if (item == opPres) nuevoTipoDoc = TIPODOC_PRESUPUESTO
        return nuevoTipoDoc
    }

    class DlgSeleccFormato : DialogFragment() {
        private var queOpcion = 0

        override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
            val items = arrayOfNulls<String>(2)
            items[0] = "Formato est??ndar"
            items[1] = "Formato 2"
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Selecci??n").setSingleChoiceItems(items, 0 ) { _: DialogInterface?, item: Int -> queOpcion = item + 1 }
            builder.setPositiveButton("Aceptar") { _: DialogInterface?, _: Int ->
                if (queOpcion > -1) {
                    if (queOpcion == 0) queOpcion = 1

                    // Seg??n el formato que hayamos elegido imprimiremos un documento u otro. El 1 es el formato est??ndar.
                    // Por ahora s??lo para la impresora Star. Si fuese necesario habr??a que hacerlo para la Datamax Apex 2.
                    if (queOpcion == 2) {
                        val imprDocFto2 = ImprDocFormato2(activity)
                        imprDocFto2.imprimir()
                    } else {
                        val imprDoc = ImprimirDocumento(activity)
                        imprDoc.imprimir(false)
                    }
                }
            }
            return builder.create()
        }

        companion object {
            fun newInstance(title: Int): DlgSeleccFormato {
                val frag = DlgSeleccFormato()
                val args = Bundle()
                args.putInt("title", title)
                frag.arguments = args
                return frag
            }
        }
    }


    fun verDesctos(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        val pattern = "00.00"
        val formatter = DecimalFormat(pattern, DecimalFormatSymbols())
        try {
            MsjAlerta(this).informacion(
                """
                    Dto. pie 1:   ${formatter.format(formatter.parse(fDocumento.fDtoPie1.toString()))}
                    Dto. pie 2:   ${formatter.format(formatter.parse(fDocumento.fDtoPie2.toString()))}
                    Dto. pie 3:   ${formatter.format(formatter.parse(fDocumento.fDtoPie3.toString()))}
                    Dto. pie 4:   ${formatter.format(formatter.parse(fDocumento.fDtoPie4.toString()))}
                    """.trimIndent()
            )
        } catch (e: ParseException) {
            //
        }
    }

    fun verTotales(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        if (lyTotales.visibility == View.GONE) {
            lyTotales.visibility = View.VISIBLE
        } else {
            lyTotales.visibility = View.GONE
        }

        // Dejaremos guardado en preferencias el estado de los layouts, de forma que la siguiente vez
        // que entremos a vender ser??n visibles o no en funci??n de c??mo los hayamos dejado.
        val editor = prefs.edit()
        editor.putBoolean("verTotales", lyTotales.visibility == View.VISIBLE)
        editor.apply()
    }

    class EnviarOGuardar: DialogFragment() {
        private var queOpcion = 0

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            queOpcion = 0
            val items = arrayOfNulls<String>(2)
            items[0] = "Para env??o"
            items[1] = "Aparcarlo"
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("??Qu?? hacemos con el documento?").setSingleChoiceItems(items, 0) { _: DialogInterface?, item: Int -> queOpcion = item }
            builder.setPositiveButton("Aceptar") { _: DialogInterface?, _: Int ->
                (activity as VentasLineas).elegirEnviarGuardar(
                    queOpcion
                )
            }
            builder.setNegativeButton("Cancelar") { _: DialogInterface?, _: Int ->
                // Si hemos a??adido ofertas por volumen las borramos porque seguiremos editando el documento
                (activity as VentasLineas).fDocumento.borrarOftVolumen(true)

                // Activamos el bot??n que previamente habiamos desactivado
                (activity as VentasLineas).btnTerminar.isEnabled = true
            }
            return builder.create()
        }

        companion object {
            fun newInstance(title: Int): EnviarOGuardar {
                val frag = EnviarOGuardar()
                val args = Bundle()
                args.putInt("title", title)
                frag.arguments = args
                // Hacemos el DialogFragmen como no cancelable para poder controlar el caso de que volvamos atr??s
                frag.isCancelable = false
                return frag
            }
        }
    }

    fun elegirEnviarGuardar(item: Int) {
        var queItem = item
        if (item < 0) queItem = 0
        fDocEnviarGuardar = queItem + 1
        grabarPieDoc()
    }


}