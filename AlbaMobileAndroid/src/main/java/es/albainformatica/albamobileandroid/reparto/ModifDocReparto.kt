package es.albainformatica.albamobileandroid.reparto

import android.app.Activity
import android.app.AlertDialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.cobros.CobrosClase
import es.albainformatica.albamobileandroid.dao.ContactosCltesDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.CobrosEnt
import es.albainformatica.albamobileandroid.entity.ContactosCltesEnt
import es.albainformatica.albamobileandroid.impresion_informes.*
import es.albainformatica.albamobileandroid.ventas.*
import kotlinx.android.synthetic.main.modif_doc_reparto.*
import org.jetbrains.anko.alert


class ModifDocReparto: Activity() {
    private lateinit var fDocumento: Documento
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fCobros: CobrosClase

    private lateinit var fRecyclerView: RecyclerView
    private lateinit var fAdapter: LinRepVtasRvAdapter

    private var fIdDocOriginal = 0
    private var fTotalDocOriginal: Double = 0.0
    private var fIdAlbNuevo = 0
    private var fHayDosFirmas: Boolean = false
    private var fEstado: Byte = 0
    private var fIvaIncluido: Boolean = false

    private var fLinea = 0
    private var fAplicarIva: Boolean = true
    private var fUsarPiezas: Boolean = false
    private var fUsarTasa1: Boolean = false
    private var fUsarTasa2: Boolean = false
    private var fNombreTasa1: String = ""
    private var fNombreTasa2: String = ""
    private var fPrimeraVez: Boolean = false // Nos sirve para saber si es la primera vez que entramos a nueva línea o no.
    private var fPedirCobro = false
    private var fPedirFirma = false

    // Formatos
    private var fFtoDecImpIva: String = ""
    private var fFtoDecImpBase: String = ""
    private var fFtoDecPrIva: String = ""
    private var fFtoDecPrBase: String = ""
    private var fFtoDecCant: String = ""

    // Request de las actividades a las que llamamos.
    private val fRequestNuevaLinea = 1
    private val fRequestEditarLinea = 2
    private val fRequestPieDoc = 3
    private val fRequestFirmarDoc = 4
    private val fRequestDevoluciones = 5

    private var carpetaImagenes: String = ""


    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.modif_doc_reparto)

        fDocumento = Documento(this)
        fConfiguracion = Comunicador.fConfiguracion
        fCobros = CobrosClase(this)
        val intent = intent

        inicializarDocumento(intent)
    }

    override fun onDestroy() {
        fDocumento.close()
        super.onDestroy()
    }


    private fun inicializarDocumento(intent: Intent) {
        fIdDocOriginal = intent.getIntExtra("iddoc", 0)
        // Comprobamos si podemos aplicar la tarifa de cajas en el documento, por si nos hiciera falta para algún artículo.
        // Hacemos esta comprobación porque hay quien tiene artículos con tarifa de cajas pero no envían la tarifa de cajas
        // a la tablet, y quieren que en este caso se aplique la tarifa normal.
        fDocumento.poderAplTrfCajas()
        fDocumento.fHayArtHabituales = fDocumento.hayArtHabituales()

        // Lo que haré será copiar el documento original en un nuevo albarán y trabajaré en éste. De esta forma
        // el documento original nunca se modifica. Entiendo también que es la forma más rápida de trabajar para el
        // usuario que tiene que realizar modificaciones sobre un documento con el resultado de crear otro nuevo.
        fTotalDocOriginal = fDocumento.copiarAAlbaran(fIdDocOriginal)

        if (fTotalDocOriginal == -0.00001) {
            alert("No se encontró una serie válida para el nuevo documento") {
                title = "Anomalías"
                positiveButton("Ok") {
                    finish()
                }
            }.show()

        } else {

            fLinea = 0
            fAplicarIva = fDocumento.fClientes.fAplIva

            // Uso Comunicador para tener una referencia al objeto
            // fDocumento también desde CargarHco.java y desde Pendiente.java.
            Comunicador.fDocumento = fDocumento

            inicializarControles()
        }
    }


    private fun inicializarControles() {
        // Leemos las preferencias de la aplicación;
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        fPedirCobro = prefs.getBoolean("reparto_pedir_cobro", false)
        fPedirFirma = prefs.getBoolean("reparto_pedir_firma", false)
        val tvNombreClte = findViewById<TextView>(R.id.tvVL_Clte)
        val tvNComClte = findViewById<TextView>(R.id.tvVL_NComClte)
        // Por ahora dejamos invisible el botón de borrar, porque parece que no sirve para nada
        val btnBorrar = findViewById<Button>(R.id.btnVL_Borrar)
        btnBorrar.visibility = View.GONE
        fFtoDecImpIva = fConfiguracion.formatoDecImptesIva()
        fFtoDecImpBase = fConfiguracion.formatoDecImptesBase()
        fFtoDecPrIva = fConfiguracion.formatoDecPrecioIva()
        fFtoDecPrBase = fConfiguracion.formatoDecPrecioBase()
        fFtoDecCant = fConfiguracion.formatoDecCantidad()
        carpetaImagenes = dimeRutaImagenes(this)
        fUsarPiezas = fConfiguracion.usarPiezas()
        fUsarTasa1 = fConfiguracion.usarTasa1()
        fUsarTasa2 = fConfiguracion.usarTasa2()
        fNombreTasa1 = fConfiguracion.nombreTasa1()
        fNombreTasa2 = fConfiguracion.nombreTasa2()
        indicarTipoDoc()
        val queTexto = fDocumento.fClientes.fCodigo + " - " + fDocumento.nombreCliente()
        tvNombreClte.text = queTexto
        tvNComClte.text = fDocumento.nombreComClte()
        fEstado = est_Vl_Browse
        fIvaIncluido = fConfiguracion.ivaIncluido(fDocumento.fEmpresa)

        fRecyclerView = rvRep_LineasDoc
        fRecyclerView.layoutManager = LinearLayoutManager(this)
        prepararRecyclerView()

        fPrimeraVez = true
        fHayDosFirmas = false
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.reparto)
    }


    private fun indicarTipoDoc() {
        val tvTipoDoc = findViewById<TextView>(R.id.tvVL_TipoDoc)
        val tvSerieNum = findViewById<TextView>(R.id.tvVL_SerieNum)
        tvTipoDoc.text = tipoDocAsString(fDocumento.fTipoDoc)
        val queTexto = fDocumento.serie + '/' + fDocumento.numero
        tvSerieNum.text = queTexto
    }


    private fun prepararRecyclerView() {
        fAdapter = LinRepVtasRvAdapter(getLineasDoc(), fIvaIncluido, fAplicarIva, this,
                            object: LinRepVtasRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosLinVtas) {
                fLinea = data.lineaId
            }
        })

        fRecyclerView.adapter = fAdapter
    }


    private fun getLineasDoc(): List<DatosLinVtas> {
        return fDocumento.lLineas
    }



    private fun refrescarLineas() {
        prepararRecyclerView()
    }


    fun borrarLinea(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

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

    fun devoluciones(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fEstado == est_Vl_Browse) {
            val i = Intent(this, RepartoDevoluciones::class.java)
            i.putExtra("cliente", fDocumento.fCliente)
            startActivityForResult(i, fRequestDevoluciones)
        }
    }


    fun editarLinea(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fEstado == est_Vl_Browse && fDocumento.lLineas.count() > 0) {
            if (fLinea > 0) {

                // Desactivo el adapter del listView porque he detectado que al movernos
                // al layout de edición de la línea, el cursor (fDocumento.cLineas) se
                // mueve al último registro, por lo que realmente no estamos modificando
                // la línea que queremos, sino siempre la última.
                fEstado = est_Vl_Editar
                val i = Intent(this, VentasDatosLinea::class.java)
                i.putExtra("estado", fEstado)
                i.putExtra("numlinea", fLinea)
                startActivityForResult(i, fRequestEditarLinea)
            } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
        }
    }


    fun nuevaLinea(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fEstado == est_Vl_Browse) {
            fEstado = est_Vl_Nueva
            val i = Intent(this, VentasDatosLinea::class.java)
            i.putExtra("estado", fEstado)
            i.putExtra("primera_vez", fPrimeraVez)
            fPrimeraVez = false
            startActivityForResult(i, fRequestNuevaLinea)
        }
    }

    fun terminarDoc(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        // Limpiamos el documento y dejamos sólo las líneas que hemos modificado o añadido. Volvemos a abrir
        // para que el cursor tenga sólo las lineas del nuevo albarán. De esta forma controlamos si hemos realizado
        // alguna modificación o no, para no crear el nuevo albarán.
        fDocumento.borrarLineasNoModif()
        fDocumento.abrirLineas()

        // Comprobamos que el documento tenga alguna línea.
        if (fDocumento.lLineas.count() > 0) {
            val i = Intent(this, VentasFinDoc::class.java)
            i.putExtra("iddoc", fIdDocOriginal)
            i.putExtra("separarlineas", false)
            i.putExtra("terminar", true)
            startActivityForResult(i, fRequestPieDoc)
        } else {
            val aldDialog = nuevoAlertBuilder(this, "Terminar", "No ha realizado ninguna modificación", false)
            aldDialog.setPositiveButton("Ok") { _: DialogInterface?, _: Int ->
                    // Borramos la cabecera del albarán
                    fDocumento.borrarDocumento(fDocumento.fIdDoc)
                    val returnIntent = Intent()
                    setResult(RESULT_CANCELED, returnIntent)
                    finish()
                }
            val alert = aldDialog.create()
            alert.show()
        }
    }


    private fun grabarPieDoc(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            fDocumento.fObs1 = data?.getStringExtra("obs1") ?: ""
            fDocumento.fObs2 = data?.getStringExtra("obs2") ?: ""
            // Si estamos haciendo un pedido, recogemos la fecha de entrega.
            if (fDocumento.fTipoDoc == TIPODOC_PEDIDO)
                fDocumento.fFEntrega = data?.getStringExtra("fentrega") ?: ""
            fDocumento.fDtoPie1 = data?.getDoubleExtra("dto1", 0.0) ?: 0.0
            fDocumento.fDtoPie2 = data?.getDoubleExtra("dto2", 0.0) ?: 0.0
            fDocumento.fDtoPie3 = data?.getDoubleExtra("dto3", 0.0) ?: 0.0
            fDocumento.fDtoPie4 = data?.getDoubleExtra("dto4", 0.0) ?: 0.0
        }
        fIdAlbNuevo = fDocumento.fIdDoc

        // Si estamos haciendo una factura establecemos la forma de pago, que nos
        // servirá luego para insertar en la tabla Pendiente. Si estamos haciendo un pedido también
        // tomamos la forma de pago para grabarla en la cabecera.
        if (fDocumento.fTipoDoc == TIPODOC_FACTURA || fDocumento.fTipoDoc == TIPODOC_PEDIDO)
            fDocumento.fPago = data?.getStringExtra("fpago") ?: ""
        fDocumento.calcularDtosPie()
        // Tenemos que recalcular las bases del documento antes de terminarlo.
        fDocumento.recalcularBases()
        fDocumento.terminarDoc(false, "")
        fConfiguracion.actualizarNumero(fDocumento.serie, fDocumento.fEjercicio, TIPODOC_ALBARAN.toString().toByte(), fDocumento.numero)
        val dTotalDoc: Double = if (fDocumento.fAplicarIva) fDocumento.fBases.totalConImptos else fDocumento.fBases.totalBases
        val dSumaDocs = dTotalDoc + fTotalDocOriginal
        val sSumaDocs = String.format(fFtoDecImpIva, dSumaDocs)
        if (fPedirCobro) {
            val aldDialog = nuevoAlertBuilder(this, "Cobrar", "¿Cobrar $sSumaDocs por los dos documentos?", true)
            aldDialog
                .setPositiveButton("Sí") { _: DialogInterface?, _: Int -> cobrarModifDocReparto() }
                .setNegativeButton("No") { _: DialogInterface?, _: Int ->
                    pedirFirma(fIdAlbNuevo, 0)
                }
            val alert = aldDialog.create()
            alert.show()
        } else pedirFirma(fIdAlbNuevo, 0)
    }


    private fun cobrarModifDocReparto() {
        val cobroEnt = CobrosEnt()

        cobroEnt.clienteId = fDocumento.fCliente
        cobroEnt.tipoDoc = fDocumento.fTipoDoc
        cobroEnt.almacen = fDocumento.fAlmacen
        cobroEnt.serie = fDocumento.serie
        cobroEnt.numero = fDocumento.numero
        cobroEnt.ejercicio = fDocumento.fEjercicio
        cobroEnt.empresa = fDocumento.fEmpresa
        cobroEnt.fechaCobro = fDocumento.fFecha

        if (fDocumento.fAplicarIva) cobroEnt.cobro = fDocumento.fBases.totalConImptos.toString()
        else cobroEnt.cobro = fDocumento.fBases.totalBases.toString()
        cobroEnt.fPago = "1"
        cobroEnt.divisa = "1"
        cobroEnt.anotacion = ""
        cobroEnt.codigo = "CO"
        cobroEnt.estado = "N"
        cobroEnt.vAlmacen = ""
        cobroEnt.vPuesto = ""
        cobroEnt.vAlmacen = ""
        cobroEnt.vEjercicio = ""

        fCobros.nuevoCobro(cobroEnt)

        // Cobramos ahora el albarán original. Para ello lo cargamos.
        fDocumento.cargarDocumento(fIdDocOriginal, false)

        cobroEnt.clienteId = fDocumento.fCliente
        cobroEnt.tipoDoc = fDocumento.fTipoDoc
        cobroEnt.almacen = fDocumento.fAlmacen
        cobroEnt.serie = fDocumento.serie
        cobroEnt.numero = fDocumento.numero
        cobroEnt.ejercicio = fDocumento.fEjercicio
        cobroEnt.empresa = fDocumento.fEmpresa
        cobroEnt.fechaCobro = fDocumento.fFecha

        if (fDocumento.fAplicarIva) cobroEnt.cobro = fDocumento.fBases.totalConImptos.toString()
        else cobroEnt.cobro = fDocumento.fBases.totalBases.toString()
        cobroEnt.fPago = "1"
        cobroEnt.divisa = "1"
        cobroEnt.anotacion = ""
        cobroEnt.codigo = "CO"
        cobroEnt.estado = "N"
        cobroEnt.vAlmacen = ""
        cobroEnt.vPuesto = ""
        cobroEnt.vAlmacen = ""
        cobroEnt.vEjercicio = ""

        fCobros.nuevoCobro(cobroEnt)

        pedirFirma(fIdAlbNuevo, fIdDocOriginal)
    }


    private fun pedirFirma(queIdDocNuevo: Int, otroDoc: Int) {
        if (fPedirFirma) {
            val i = Intent(this, FirmarDoc::class.java)
            i.putExtra("id_doc", queIdDocNuevo)
            if (otroDoc > 0) {
                i.putExtra("otro_doc", otroDoc)
                fHayDosFirmas = true
            }
            startActivityForResult(i, fRequestFirmarDoc)
        } else {
            fDocumento.marcarComoEntregado(
                fIdAlbNuevo,
                fDocumento.fCliente,
                fDocumento.fEmpresa.toInt(),
                true
            )
            if (otroDoc > 0) fDocumento.marcarComoEntregado(
                fIdDocOriginal,
                fDocumento.fCliente,
                fDocumento.fEmpresa.toInt(),
                true
            )
            refrescarLineas()
            imprimirDoc()
        }
    }


    private fun imprimirDoc() {
        if (fConfiguracion.imprimir()) {
            // Lo primero que debemos hacer es rescatar el layout creado para el prompt.
            val li = LayoutInflater.from(this)
            val prompt = li.inflate(R.layout.imprimir_doc, null)
            // Luego, creamos un constructor de Alert Dialog que nos ayudará a utilizar nuestro layout.
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setView(prompt)
            // Por ultimo, creamos el cuadro de dialogo y las acciones requeridas al aceptar o cancelar el prompt.
            // Mostramos el mensaje del cuadro de diálogo
            alertDialogBuilder.setCancelable(false).setNegativeButton(
                "Cancelar"
            ) { dialog: DialogInterface, _: Int ->
                // Cancelamos el cuadro de dialogo
                dialog.cancel()
                finalizarVenta()
            }

            // Creamos un AlertDialog y lo mostramos
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()

            // Establecemos los eventos para los distintos botones del layout del diálogo.
            val btnImpr = alertDialog.findViewById<Button>(R.id.btnImprDoc)
            btnImpr.setOnClickListener { v: View ->
                // Vemos si tenemos que pedir el formato con el que queremos imprimir o no.
                if (fConfiguracion.pedirFormato()) {
                    val newFragment: DialogFragment = VentasLineas.DlgSeleccFormato.newInstance(R.string.app_name)
                    newFragment.show(fragmentManager, "dialog")
                } else {

                    // Vemos el tipo de impresora por el que vamos a imprimir.
                    if (fConfiguracion.impresora() == IMPRESORA_DATAMAX_APEX_2) {
                        val imprDoc = ImprDocDatamaxApex2(this@ModifDocReparto)
                        imprDoc.imprimir()
                    } else if (fConfiguracion.impresora() == IMPRESORA_BIXOLON_SPP_R410 || fConfiguracion.impresora() == IMPRESORA_GENERICA_110 || fConfiguracion.impresora() == IMPRESORA_GENERICA_80
                    ) {
                        val imprDoc = ImprGenerica(this@ModifDocReparto)
                        imprDoc.imprimir()
                    } else if (fConfiguracion.impresora() == IMPRESORA_ZEBRA_80) {
                        val imprDoc = ImprZebra(this@ModifDocReparto)
                        imprDoc.imprimir()
                    } else {
                        val imprDoc = ImprimirDocumento(this@ModifDocReparto)
                        imprDoc.imprimir(false)
                    }
                }
                v.isEnabled = false
            }

            // Comprobamos si tenemos algun email para enviar. Si no, desactivamos la exportacion a PDF.
            val documPDF = DocPDF(this@ModifDocReparto)
            val btnExpPDF = alertDialog.findViewById<Button>(R.id.btnExpPDFDoc)
            if (documPDF.dimeNumEmailsClte() > 0) {
                btnExpPDF.setOnClickListener {
                    documPDF.crearPDF()

                    // Comprobamos si el Whatsapp está instalado
                    if (whatsappInstalado(this)) {

                        val aldDialog = nuevoAlertBuilder(this, "Escoja", "Enviar documento PDF", true)

                        aldDialog.setPositiveButton("Por email") { _: DialogInterface?, _: Int ->
                            documPDF.enviarPorEmail()
                            it.isEnabled = false
                        }
                        aldDialog.setNegativeButton("Por whatsapp") { _: DialogInterface?, _: Int ->
                            val telfDao: ContactosCltesDao? = MyDatabase.getInstance(this@ModifDocReparto)?.contactosCltesDao()
                            val lTelfs = telfDao?.getTlfsCliente(fDocumento.fCliente) ?: emptyList<ContactosCltesEnt>().toMutableList()
                            var numeroTelefono = lTelfs[0].telefono1
                            if (numeroTelefono == "") numeroTelefono = lTelfs[0].telefono2
                            // Si no añadimos el prefijo no funciona
                            if (!numeroTelefono.startsWith("34")) numeroTelefono =
                                "34$numeroTelefono"
                            enviarPorWhatsapPdf(this, documPDF.nombrePDF, numeroTelefono)
                            it.isEnabled = false
                        }
                        aldDialog.setCancelable(true)
                        val alert = aldDialog.create()
                        alert.show()
                    } else {
                        documPDF.enviarPorEmail()
                    }

                }
            } else {
                btnExpPDF.visibility = View.GONE
            }
        } else {
            finalizarVenta()
        }
    }

    private fun finalizarVenta() {
        val returnIntent = Intent()
        setResult(RESULT_OK, returnIntent)
        finish()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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
            if (resultCode == RESULT_OK) grabarPieDoc(resultCode, data)
            // Actividad firmar documento
        } else if (requestCode == fRequestFirmarDoc) {
            if (resultCode == RESULT_OK) {
                fDocumento.marcarComoEntregado(
                    fIdAlbNuevo,
                    fDocumento.fCliente,
                    fDocumento.fEmpresa.toInt(),
                    true
                )
                if (fHayDosFirmas) fDocumento.marcarComoEntregado(
                    fIdDocOriginal,
                    fDocumento.fCliente,
                    fDocumento.fEmpresa.toInt(),
                    true
                )
                refrescarLineas()
            }
            imprimirDoc()
        } else if (requestCode == fRequestDevoluciones) {
            if (resultCode == RESULT_OK) {
                fDocumento.grabarHistorico()
                refrescarLineas()
            }
        }
    }

    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (fEstado == est_Vl_Browse) {
                val aldDialog = nuevoAlertBuilder(this, "Salir", "¿Anular el documento?", true)
                aldDialog.setPositiveButton("Sí") { _: DialogInterface?, _: Int ->
                    fDocumento.borrarModifDocReparto(fDocumento.fIdDoc)
                    val returnIntent = Intent()
                    setResult(RESULT_CANCELED, returnIntent)
                    this@ModifDocReparto.finish()
                }
                val alert = aldDialog.create()
                alert.show()

                // Si el listener devuelve true, significa que el evento está procesado, y nadie debe hacer nada más.
                return true
            }
        }
        // Para las demás cosas, se reenvía el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }


}