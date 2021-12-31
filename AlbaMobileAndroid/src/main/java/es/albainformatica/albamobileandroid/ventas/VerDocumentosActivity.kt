package es.albainformatica.albamobileandroid.ventas

import android.app.*
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.impresion_informes.*
import es.albainformatica.albamobileandroid.reparto.FirmarDoc
import java.io.File
import java.util.ArrayList
import android.net.Uri
import androidx.core.content.FileProvider
import android.content.pm.PackageManager
import es.albainformatica.albamobileandroid.dao.ContactosCltesDao
import es.albainformatica.albamobileandroid.dao.TiposIncDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.ContactosCltesEnt
import es.albainformatica.albamobileandroid.entity.TiposIncEnt


class VerDocumentosActivity: Activity() {
    private lateinit var fDocumento: Documento
    private lateinit var adapterLineas: SimpleCursorAdapter
    private lateinit var fConfiguracion: Configuracion
    private var fIdDocumento = 0
    private var fEstado: String = ""
    private var fFirmado: String = ""
    private var fTipoDoc: Byte = 0
    private lateinit var tvEstado: TextView
    private lateinit var tvNombreClte: TextView
    private lateinit var tvNombreComClte: TextView
    private lateinit var tvIncidencia: TextView
    private lateinit var btnEditar: Button
    private lateinit var imvFirma: ImageView
    private var fFtoDecImpIva: String = ""
    private lateinit var Dialogo: ProgressDialog
    private var fCliente = 0
    private var fClteDoc = 0
    private lateinit var lvDocumentos: ListView
    private lateinit var chsIncidencias: Array<CharSequence>
    private var fTipoIncidencia = 0
    private var fRutaFirmas: String = ""
    private var fTipoFiltrar = 0
    private var fEmpresaActual = 0

    private val fRequestVenta = 1
    private val fRequestFirmarDoc = 2
    private val fRequestModifDocReparto = 3
    private val fRequestIncidencia = 4


    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.ver_documentos)
        val intent = intent
        fCliente = intent.getIntExtra("cliente", 0)
        fDocumento = Documento(this)
        fConfiguracion = Comunicador.fConfiguracion
        inicializarControles()
    }


    override fun onDestroy() {
        if (fDocumento != null) fDocumento.close()
        super.onDestroy()
    }


    private fun inicializarControles() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val usarMultisistema = prefs.getBoolean("usar_multisistema", false)
        fEmpresaActual = prefs.getInt("ultima_empresa", 0)
        fRutaFirmas = prefs.getString("rutacomunicacion", "") ?: ""
        fRutaFirmas = if (fRutaFirmas == "") {
            if (usarMultisistema) "/storage/sdcard0/alba/firmas/" + BaseDatos.queBaseDatos else "/storage/sdcard0/alba/firmas/"
        } else {
            if (usarMultisistema) fRutaFirmas + "/firmas/" + BaseDatos.queBaseDatos else "$fRutaFirmas/firmas/"
        }

        btnEditar = findViewById(R.id.btnVDEditar)
        // Idem con la impresión.
        val btnImpr = findViewById<Button>(R.id.btnVDImpr)
        if (!fConfiguracion.imprimir()) btnImpr.visibility = View.GONE

        val btnFirmar = findViewById<Button>(R.id.btnVDFirmar)
        val btnIncidencia = findViewById<Button>(R.id.btnVDIncidencia)
        val btnReenviar = findViewById<Button>(R.id.btnVDReenviar)

        // Si usamos el servicio pero no clasificamos los pedidos como Enviar/Guardar no mostraremos
        // el botón de Reenviar.
        val fUsarServicio = prefs.getBoolean("usar_servicio", false)
        if (fUsarServicio) {
            if (!prefs.getBoolean("ventas_enviar_guardar", false)) btnReenviar.visibility =
                View.GONE
        }
        tvIncidencia = findViewById(R.id.tvVD_incidencia)
        tvIncidencia.text = ""
        imvFirma = findViewById(R.id.imvVD_firma)
        imvFirma.setImageBitmap(null)
        if (fConfiguracion.hayReparto()) {
            btnReenviar.visibility = View.GONE
        } else {
            if (!fConfiguracion.activarFirmaDigital()) btnFirmar.visibility = View.GONE
            btnIncidencia.visibility = View.GONE
        }
        fFtoDecImpIva = fConfiguracion.formatoDecImptesIva()
        tvEstado = findViewById(R.id.tvVD_Estado)
        tvNombreClte = findViewById(R.id.tvVD_Clte)
        tvNombreComClte = findViewById(R.id.tvVD_NComClte)
        fIdDocumento = 0
        fEstado = ""
        fTipoDoc = 0
        fTipoFiltrar = 0
        fDocumento.abrirTodos(fCliente, fEmpresaActual, fTipoFiltrar)
        prepararListView()
        //  Si tenemos algún documento presentamos los datos del primero
        if (!fDocumento.cDocumentos.isBeforeFirst) nuevoClick(0)

        // Preparamos el array de incidencias por si las usamos para el documento.
        prepararIncidencias()
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.btn_documentos_full)
    }


    private fun prepararListView() {
        val columnas: Array<String> =
            arrayOf("fecha", "tipodoc", "serie", "numero", "total", "firmado", "tipoincidencia")
        val to = intArrayOf(
            R.id.lyvdFecha,
            R.id.lyvdTipoDoc,
            R.id.lyvdSerie,
            R.id.lyvdNumero,
            R.id.lyvdTotal,
            R.id.lyvdDocFdo,
            R.id.lyvdDocIncid
        )
        adapterLineas = SimpleCursorAdapter(
            this,
            R.layout.ly_ver_documentos,
            fDocumento.cDocumentos,
            columnas,
            to
        )
        formatearColumnas()
        lvDocumentos = findViewById(R.id.lvVerDoc)
        lvDocumentos.choiceMode = ListView.CHOICE_MODE_SINGLE
        lvDocumentos.adapter = adapterLineas

        // Establecemos el evento on click del ListView.
        lvDocumentos.onItemClickListener =
            AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                nuevoClick(position)
            }
    }


    private fun nuevoClick(position: Int) {
        // Tomamos el campo _id de la fila en la que hemos pulsado.
        val cursor = lvDocumentos.getItemAtPosition(position) as Cursor
        fIdDocumento = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
        fEstado = cursor.getString(cursor.getColumnIndexOrThrow("estado"))
        fFirmado = if (cursor.getString(cursor.getColumnIndexOrThrow("firmado")) != null)
            cursor.getString(cursor.getColumnIndexOrThrow("firmado"))
        else "F"
        val fImprimido = if (cursor.getString(cursor.getColumnIndexOrThrow("imprimido")) != null)
            cursor.getString(cursor.getColumnIndexOrThrow("imprimido"))
            else "F"

        fTipoDoc = cursor.getString(cursor.getColumnIndexOrThrow("tipodoc")).toByte()
        fClteDoc = cursor.getInt(cursor.getColumnIndexOrThrow("cliente"))

        // Si el documento es factura o albarán no permitiremos la modificación en ningún caso.
        // Si es pedido o presupuesto la permitiremos si no está firmado, ni imprimido ni comunicado.
        if (fTipoDoc.toShort() == TIPODOC_FACTURA || fTipoDoc.toShort() == TIPODOC_ALBARAN) {
            btnEditar.isEnabled = false
        } else {
            if (fFirmado == "T" || fImprimido == "T") btnEditar.isEnabled = false
            else btnEditar.isEnabled = fEstado == "N" || fEstado == "P"
        }

        if (btnEditar.isEnabled)
            btnEditar.setTextColor(Color.parseColor("#415BB6"))
        else
            btnEditar.setTextColor(Color.LTGRAY)

        val bFacturado = cadenaALogico(cursor.getString(cursor.getColumnIndex("facturado")))
        if (bFacturado) {
            val queTexto =
                nombreEstado(cursor.getString(cursor.getColumnIndex("estado"))) + ". Facturado."
            tvEstado.text = queTexto
        } else tvEstado.text = nombreEstado(cursor.getString(cursor.getColumnIndex("estado")))
        tvNombreClte.text = cursor.getString(cursor.getColumnIndex("nomfi"))
        tvNombreComClte.text = cursor.getString(cursor.getColumnIndex("nomco"))
        if (cursor.getInt(cursor.getColumnIndexOrThrow("tipoincidencia")) > 0) {
            val queTexto =
                """
            ${fDocumento.getTipoIncidencia(cursor.getInt(cursor.getColumnIndexOrThrow("tipoincidencia")))}
            ${fDocumento.getTextoIncidencia(fIdDocumento)}
            """.trimIndent()
            tvIncidencia.text = queTexto
        } else tvIncidencia.text = ""
        val imgFile = File("$fRutaFirmas$fIdDocumento.jpg")
        if (imgFile.exists()) {
            val myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            imvFirma.setImageBitmap(myBitmap)
        } else imvFirma.setImageBitmap(null)
    }


    fun reenviarDoc(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fIdDocumento > 0) {
            when (fEstado) {
                "X" -> {
                    // Vemos la posición del item del listView seleccionado, para poder seleccionarlo después.
                    val quePosicion = lvDocumentos.checkedItemPosition
                    fDocumento.reenviar(fIdDocumento, fCliente, fEmpresaActual)
                    adapterLineas.changeCursor(fDocumento.cDocumentos)

                    // Volvemos a señalar la fila en la que habíamos pulsado, ya que el listView, al perder el foco, no deja ninguna fila señalada.
                    lvDocumentos.requestFocusFromTouch()
                    lvDocumentos.setSelection(quePosicion)
                    // Llamamos a nuevoClick para refrescar los textView inferiores.
                    nuevoClick(quePosicion)
                    fEstado = ""
                }
                "P" -> {
                    marcarParaEnviar()
                }
                else -> MsjAlerta(this).alerta(resources.getString(R.string.msj_Reenviar))
            }
        } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
    }


    private fun marcarParaEnviar() {
        val aldDialog = NuevoAlertBuilder(
            this, getString(R.string.tit_marcarenv),
            getString(R.string.msj_MarcarEnviar), false
        )
        aldDialog.setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
            fDocumento.marcarParaEnviar(fIdDocumento)
            refrescarLineas()
            if (fDocumento.cDocumentos.count > 0) nuevoClick(0)
        }
        val alert = aldDialog.create()
        alert.show()
    }

    private fun formatearColumnas() {
        adapterLineas.viewBinder =
            SimpleCursorAdapter.ViewBinder { view: View, cursor: Cursor, column: Int ->

                // Formateamos el tipo de documento.
                if (column == 1) {
                    val tv = view as TextView
                    tv.text = tipoDocAsString(cursor.getString(cursor.getColumnIndex("tipodoc")).toShort())
                    return@ViewBinder true
                }
                // Formateamos el total.
                if (column == 8) {
                    val tv = view as TextView
                    if (cursor.getString(cursor.getColumnIndex("total")) != null) {
                        val sTotal =
                            cursor.getString(cursor.getColumnIndex("total")).replace(',', '.')
                        val dTotal = sTotal.toDouble()
                        tv.text = String.format(fFtoDecImpIva, dTotal)
                        return@ViewBinder true
                    }
                }

                // Firmado
                if (column == 13) {
                    val iv = view as ImageView
                    if (cursor.getString(cursor.getColumnIndex("firmado")) != null &&
                        cursor.getString(cursor.getColumnIndex("firmado"))
                            .equals("T", ignoreCase = true)
                    ) {
                        iv.visibility = View.VISIBLE
                    } else {
                        iv.visibility = View.INVISIBLE
                    }
                    return@ViewBinder true
                }

                // Incidencia
                if (column == 14) {
                    val iv = view as ImageView
                    if (cursor.getInt(cursor.getColumnIndex("tipoincidencia")) > 0) iv.visibility =
                        View.VISIBLE else iv.visibility = View.INVISIBLE
                    return@ViewBinder true
                }
                false
            }
    }


    fun modificarDoc(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fIdDocumento > 0) {
            if (fEstado == "N" || fEstado == "P") {
                val i = Intent(this, VentasLineas::class.java)
                i.putExtra("nuevo", false)
                i.putExtra("iddoc", fIdDocumento)
                startActivityForResult(i, fRequestVenta)
            } else MsjAlerta(this).alerta(resources.getString(R.string.msj_Modificar))
        } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == fRequestVenta) {
            if (resultCode == RESULT_OK) {
                refrescarLineas()
                nuevoClick(0)
            }
        } else if (requestCode == fRequestFirmarDoc) {
            if (resultCode == RESULT_OK) {
                if (fConfiguracion.hayReparto()) {
                    fDocumento.marcarComoEntregado(fIdDocumento, fClteDoc, fEmpresaActual, true)
                    adapterLineas.changeCursor(fDocumento.cDocumentos)
                } else {
                    // Vemos la posición del item del listView seleccionado, para poder seleccionarlo después.
                    val quePosicion = lvDocumentos.checkedItemPosition
                    // Volvemos a señalar la fila en la que habíamos pulsado, ya que el listView, al perder el foco, no deja ninguna fila señalada.
                    lvDocumentos.requestFocusFromTouch()
                    lvDocumentos.setSelection(quePosicion)
                    fDocumento.marcarComoEntregado(fIdDocumento, fClteDoc, fEmpresaActual, true)
                    adapterLineas.changeCursor(fDocumento.cDocumentos)
                    // Llamamos a nuevoClick para refrescar los textView inferiores.
                    nuevoClick(quePosicion)
                }
            }
        } else if (requestCode == fRequestModifDocReparto) {
            //if (resultCode == RESULT_OK) {
            fDocumento.cDocumentos.close()
            fDocumento.abrirTodos(fCliente, fEmpresaActual, fTipoFiltrar)
            adapterLineas.changeCursor(fDocumento.cDocumentos)
            //}
        } else if (requestCode == fRequestIncidencia) {
            if (resultCode == RESULT_OK) {
                val textoIncidencia = data.getStringExtra("textoincid")
                fDocumento.setTextoIncidencia(
                    fIdDocumento,
                    textoIncidencia,
                    fCliente,
                    fEmpresaActual,
                    fTipoIncidencia
                )
                adapterLineas.changeCursor(fDocumento.cDocumentos)
            }
        }
    }


    private fun refrescarLineas() {
        fDocumento.abrirTodos(fCliente, fEmpresaActual, fTipoFiltrar)
        adapterLineas.changeCursor(fDocumento.cDocumentos)
    }


    fun verDoc(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fIdDocumento > 0) {
            val i = Intent(this, VentasLineas::class.java)
            i.putExtra("nuevo", false)
            i.putExtra("solover", true)
            i.putExtra("iddoc", fIdDocumento)
            startActivity(i)
        } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
    }


    fun crearPDF(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fIdDocumento > 0) {

            Comunicador.fDocumento = fDocumento
            fDocumento.cargarDocumento(fIdDocumento, false)

            fDocumento.marcarComoImprimido(fIdDocumento)
            refrescarLineas()
            nuevoClick(0)

            Dialogo = ProgressDialog.show(this, "Exportar a PDF", "Creando PDF ...", true, true)

            val hiloExport: Thread = object : Thread() {
                override fun run() {
                    // Necesitamos hacer Looper.prepare() y Looper.loop() para mostrar el
                    // mensaje de alerta dentro del hilo.
                    Looper.prepare()
                    // Creo el objeto de tipo DocPDF y le digo que genere el fichero.
                    val documPDF = DocPDF(this@VerDocumentosActivity)
                    documPDF.crearPDF()

                    // Cerramos el diálogo y mostramos mensaje.
                    Dialogo.dismiss()

                    // Comprobamos si el Whatsapp está instalado
                    if (whatsappInstalado(this@VerDocumentosActivity)) {

                        val aldDialog = NuevoAlertBuilder(
                            this@VerDocumentosActivity,
                            "Escoja",
                            "Enviar documento PDF",
                            true
                        )

                        aldDialog.setPositiveButton("Por email") { _, _ ->
                            // Enviamos el documento por email.
                            documPDF.enviarPorEmail()
                            MsjAlerta(this@VerDocumentosActivity).alerta("Se terminó de exportar")
                        }
                        aldDialog.setNegativeButton("Por whatsapp") { _, _ ->
                            val telfDao: ContactosCltesDao? = MyDatabase.getInstance(this@VerDocumentosActivity)?.contactosCltesDao()
                            val lTelfs = telfDao?.getTlfsCliente(fDocumento.fCliente) ?: emptyList<ContactosCltesEnt>().toMutableList()
                            var numeroTelefono = lTelfs[0].telefono1
                            if (numeroTelefono == "") numeroTelefono = lTelfs[0].telefono2
                            // Si no añadimos el prefijo no funciona
                            if (!numeroTelefono.startsWith("34")) numeroTelefono = "34$numeroTelefono"

                            enviarPorWhatsapPdf(this@VerDocumentosActivity, documPDF.nombrePDF, numeroTelefono)
                            MsjAlerta(this@VerDocumentosActivity).alerta("Se terminó de exportar")
                        }
                        aldDialog.setCancelable(true)
                        val alert = aldDialog.create()
                        alert.show()
                    }
                    else {
                        documPDF.enviarPorEmail()
                        MsjAlerta(this@VerDocumentosActivity).alerta("Se terminó de exportar")
                    }

                    Looper.loop()
                }
            }
            hiloExport.start()

        } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
    }

/*
    private fun whatsappInstalado(): Boolean {
        val pm: PackageManager = packageManager
        return try {
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            // Para Whatsapp business
            //pm.getPackageInfo("com.whatsapp.w4b", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
*/

/*
    private fun enviarPorWhatsapPdf(nombreFichero: String, numeroTelefono: String) {

        val queFichero = File(nombreFichero)
        if (queFichero.exists()) {
            try {
                val uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", queFichero)

                val sendIntent = Intent()
                sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
                sendIntent.type = "application/pdf"
                sendIntent.putExtra("jid", "$numeroTelefono@s.whatsapp.net")
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.setPackage("com.whatsapp")
                sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(sendIntent)

            } catch (e: Exception) {
                Toast.makeText(this@VerDocumentosActivity, "El dispositivo no tiene instalado WhatsApp", Toast.LENGTH_LONG).show()
            }
        }
    }
*/

    private fun enviarPorWhatsapSoloTexto() {
        val sendIntent = Intent(Intent.ACTION_VIEW)
        sendIntent.type = "text/plain"

        val numeroTelefono = "+34603714650"

        val uri = "whatsapp://send?phone=$numeroTelefono&text=prueba de texto"
        sendIntent.data = Uri.parse(uri)

        try {
            startActivity(sendIntent)

        } catch (e: Exception) {
            Toast.makeText(this@VerDocumentosActivity, "El dispositivo no tiene instalado WhatsApp", Toast.LENGTH_LONG).show()
        }

    }



    fun imprimirDoc(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fIdDocumento > 0) {
            if (fEstado == "N") {
                Comunicador.fDocumento = fDocumento
                fDocumento.cargarDocumento(fIdDocumento, false)
                // Tenemos que llamar a fDocumento.calcularDtosPie() porque el documento no trae los descuentos a pie calculados,
                // ya que fDocumento.cargarDocumento() no lo hace.
                fDocumento.calcularDtosPie()

                // Lo primero que debemos hacer es rescatar el layout creado para el prompt.
                val li = LayoutInflater.from(this)
                val prompt = li.inflate(R.layout.imprimir_doc, null)
                // Luego, creamos un constructor de Alert Dialog que nos ayudará a utilizar nuestro layout.
                val alertDialogBuilder = AlertDialog.Builder(this)
                alertDialogBuilder.setView(prompt)
                // Por ultimo, creamos el cuadro de dialogo y las acciones requeridas al aceptar o cancelar el prompt.
                // Mostramos el mensaje del cuadro de diálogo
                alertDialogBuilder.setCancelable(false).setNegativeButton("Cancelar") { dialog: DialogInterface, _: Int ->
                    // Cancelamos el cuadro de dialogo
                    dialog.cancel()
                }

                // Creamos un AlertDialog y lo mostramos
                val alertDialog = alertDialogBuilder.create()
                alertDialog.show()

                // Establecemos los eventos para los distintos botones del layout del diálogo.
                val btnImpr = alertDialog.findViewById<Button>(R.id.btnImprDoc)
                btnImpr.setOnClickListener {
                    fDocumento.marcarComoImprimido(fIdDocumento)

                    // Vemos si tenemos que pedir el formato con el que queremos imprimir o no.
                    if (fConfiguracion.pedirFormato()) {
                        val newFragment: DialogFragment = VentasLineas.DlgSeleccFormato.newInstance(R.string.app_name)
                        newFragment.show(fragmentManager, "dialog")
                    } else {
                        if (fConfiguracion.impresora() == IMPRESORA_DATAMAX_APEX_2) {
                            val imprDoc = ImprDocDatamaxApex2(this@VerDocumentosActivity)
                            imprDoc.imprimir()
                        } else if (fConfiguracion.impresora() == IMPRESORA_INTERMEC_PB51) {
                            val imprDoc = ImprIntermecPB51(this@VerDocumentosActivity)
                            imprDoc.imprimir()
                        } else if (fConfiguracion.impresora() == IMPRESORA_BIXOLON_SPP_R410 || fConfiguracion.impresora() == IMPRESORA_GENERICA_110 || fConfiguracion.impresora() == IMPRESORA_GENERICA_80
                        ) {
                            val imprDoc = ImprGenerica(this@VerDocumentosActivity)
                            imprDoc.imprimir()
                        } else if (fConfiguracion.impresora() == IMPRESORA_ZEBRA_80) {
                            val imprDoc = ImprZebra(this@VerDocumentosActivity)
                            imprDoc.imprimir()
                        } else {
                            val chkSinValorar = alertDialog.findViewById<CheckBox>(R.id.chkSinValorar)
                            val imprDoc = ImprimirDocumento(this@VerDocumentosActivity)
                            imprDoc.imprimir(chkSinValorar.isChecked)
                            // Hacemos una pausa de 8 segundos para evitar que salgamos de la aplicación antes de que la impresora termine de imprimir.
                            // Si salimos puede que no se imprima bien el pie del documento, ya que no tendría acceso a la clase de base imponibles e
                            // imprimiría el total a cero.
                            SystemClock.sleep(8000)
                        }
                    }
                    alertDialog.dismiss()
                    refrescarLineas()
                    nuevoClick(0)
                }

                // Establecemos la visibilidad del check 'Sin Valorar'
                val chkSinValorar = alertDialog.findViewById<CheckBox>(R.id.chkSinValorar)
                if (fConfiguracion.imprimir()) {
                    // Si el documento no es albarán ni pedido no presentaremos el botón para imprimir sin valorar
                    if (fDocumento.fTipoDoc.toShort() != TIPODOC_ALBARAN && fDocumento.fTipoDoc.toShort() != TIPODOC_PEDIDO) {
                        chkSinValorar.visibility = View.GONE
                    }
                } else chkSinValorar.visibility = View.GONE
                val btnExpPDF = alertDialog.findViewById<Button>(R.id.btnExpPDFDoc)
                btnExpPDF.visibility = View.GONE
            } else MsjAlerta(this).alerta(resources.getString(R.string.msj_ImprNuevo))
        } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
    }


    fun firmarDoc(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fIdDocumento > 0) {
            if (fEstado == "X") {
                MsjAlerta(this).alerta(resources.getString(R.string.msj_FirmarRegExp))
            } else {
                val i = Intent(this, FirmarDoc::class.java)
                i.putExtra("id_doc", fIdDocumento)
                startActivityForResult(i, fRequestFirmarDoc)
            }
        } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
    }


    fun incidenciaDoc(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fIdDocumento > 0) {
            if (fEstado == "X") {
                MsjAlerta(this).alerta(resources.getString(R.string.msj_IncidRegExp))
            } else {
                val altbld = AlertDialog.Builder(this)
                altbld.setTitle("Escoger incidencia")
                val queTipoInc =
                    fDocumento.cDocumentos.getInt(fDocumento.cDocumentos.getColumnIndexOrThrow("tipoincidencia"))
                val queItem: Int = localizarIncid(queTipoInc)
                altbld.setSingleChoiceItems(
                    chsIncidencias,
                    queItem
                ) { dialog: DialogInterface, item: Int ->
                    val sIncidencia = chsIncidencias[item].toString()
                    fTipoIncidencia = sIncidencia.substring(0, 2).toByte().toInt()
                    dialog.dismiss()
                    val i = Intent(this@VerDocumentosActivity, VentasIncidencia::class.java)
                    i.putExtra("cliente", fClteDoc)
                    i.putExtra("incidencia", sIncidencia)
                    i.putExtra("texto", fDocumento.getTextoIncidencia(fIdDocumento))
                    startActivityForResult(i, fRequestIncidencia)
                }
                val alert = altbld.create()
                alert.show()
            }
        } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
    }


    private fun localizarIncid(queTipoInc: Int): Int {
        for (i in chsIncidencias.indices) {
            if (chsIncidencias[i].toString().substring(0, 2).toByte()
                    .toInt() == queTipoInc
            ) return i
        }
        return -1
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


    fun filtrarDocs(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        // Creamos un objeto de la clase Bio_TipoPedido y lo mostramos para elegir el tipo de pedido
        val newFragment: DialogFragment = FiltrarPorEstado.newInstance(R.string.app_name)
        newFragment.show(fragmentManager, "dialog")
    }


    class FiltrarPorEstado : DialogFragment() {
        private var queOpcion = 0
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            queOpcion = 0
            val items = arrayOfNulls<String>(4)
            items[0] = "Todos"
            items[1] = "Aparcados"
            items[2] = "Para enviar"
            items[3] = "Enviados"
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Escoja el tipo de documento").setSingleChoiceItems(
                items, 0
            ) { _: DialogInterface?, item: Int ->
                queOpcion = item
            }
            builder.setPositiveButton(
                "Aceptar"
            ) { _: DialogInterface?, _: Int ->
                (activity as VerDocumentosActivity).filtrar(queOpcion)
            }
            return builder.create()
        }

        companion object {
            fun newInstance(title: Int): FiltrarPorEstado {
                val frag = FiltrarPorEstado()
                val args = Bundle()
                args.putInt("title", title)
                frag.arguments = args
                return frag
            }
        }
    }


    fun filtrar(opcion: Int) {
        var item = opcion
        if (item < 0) item = 0
        fTipoFiltrar = item
        refrescarLineas()
        if (fDocumento.cDocumentos.count > 0) nuevoClick(0)
    }


}