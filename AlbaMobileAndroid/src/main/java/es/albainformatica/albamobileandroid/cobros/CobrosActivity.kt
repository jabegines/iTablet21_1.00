package es.albainformatica.albamobileandroid.cobros

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Looper
import androidx.preference.PreferenceManager
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.ContactosCltesDao
import es.albainformatica.albamobileandroid.dao.EmpresasDao
import es.albainformatica.albamobileandroid.dao.FormasPagoDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.CobrosEnt
import es.albainformatica.albamobileandroid.entity.ContactosCltesEnt
import es.albainformatica.albamobileandroid.entity.PendienteEnt
import es.albainformatica.albamobileandroid.impresion_informes.DocDiferidaPDF
import es.albainformatica.albamobileandroid.maestros.ClientesActivity
import es.albainformatica.albamobileandroid.maestros.ClientesClase
import es.albainformatica.albamobileandroid.maestros.ElegirEmpresaActivity
import es.albainformatica.albamobileandroid.registroEventos.RegistroEventosClase
import es.albainformatica.albamobileandroid.ventas.VentasLineas
import java.util.*

class CobrosActivity: AppCompatActivity() {
    private val formasPagoDao: FormasPagoDao? = MyDatabase.getInstance(this)?.formasPagoDao()
    private lateinit var fClientes: ClientesClase
    private lateinit var fCobros: CobrosClase
    private lateinit var fPendiente: PendienteClase
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fRegEventos: RegistroEventosClase

    private var fCliente = 0
    private var idPendiente = 0
    private lateinit var fContexto: Context
    private lateinit var prefs: SharedPreferences

    private lateinit var fRecycler: RecyclerView
    private lateinit var fRecPdtes: RecyclerView
    private lateinit var fAdapter: CobrosRvAdapter
    private lateinit var fAdpPdtes: PdtesRvAdapter

    private lateinit var edtCodClte: EditText
    private lateinit var tvNombre: TextView
    private lateinit var tvNomCom: TextView
    private lateinit var tvFPagoClte: TextView
    private lateinit var tvRiesgo: TextView
    private lateinit var tvSaldo: TextView
    private lateinit var tvTotal: TextView
    private lateinit var fFtoDecImpIva: String
    private lateinit var tvFPago: TextView
    private lateinit var tvDivisa: TextView
    private lateinit var tvAnotacion: TextView
    private lateinit var tvPendiente: TextView
    private lateinit var tvFecha: TextView
    private lateinit var tvFVto: TextView
    private lateinit var tvPdFPago: TextView
    private lateinit var vsCobros: ViewSwitcher
    private lateinit var lyCobrosPend: View
    private lateinit var btnCambVista: Button
    private var fEmpresaActual: Int = 0

    private val fRequestBuscarClte = 1
    private val fRequestCobrar = 2
    private val fRequestCobrarPdte = 3
    private val fRequestInfCobros = 4
    private val fRequestPedirAnotacion = 5


    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        fContexto = this
        setContentView(R.layout.cobros)

        fRegEventos = Comunicador.fRegEventos
        fRegEventos.registrarEvento(codEv_Cobros_Entrar, descrEv_Cobros_Entrar)

        fCobros = CobrosClase(this)
        fPendiente = PendienteClase(this)
        fClientes = ClientesClase(this)
        fConfiguracion = Comunicador.fConfiguracion

        // Uso Comunicador para tener una referencia a los objetos fPendiente y fCobros también desde Cobrar.java y desde InfCobros.
        Comunicador.fCobros = fCobros
        Comunicador.fPendiente = fPendiente
        val intent = intent
        fCliente = intent.getIntExtra("cliente", 0)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        fEmpresaActual = prefs.getInt("ultima_empresa", 0)

        inicializarControles()
    }


    override fun onDestroy() {
        fRegEventos.registrarEvento(codEv_Cobros_Salir, descrEv_Cobros_Salir)

        super.onDestroy()
    }


    private fun inicializarControles() {
        edtCodClte = findViewById(R.id.edtCobr_Clte)
        tvNombre = findViewById(R.id.tvCobr_Clte)
        tvNomCom = findViewById(R.id.tvCobr_NComClte)
        tvFPagoClte = findViewById(R.id.tvCobr_FPagoClte)
        tvRiesgo = findViewById(R.id.tvCobr_Riesgo)
        tvSaldo = findViewById(R.id.tvCobr_Saldo)
        tvTotal = findViewById(R.id.tvCobr_Total)
        tvFPago = findViewById(R.id.tvCobr_FPago)
        tvDivisa = findViewById(R.id.tvCobr_Divisa)
        tvAnotacion = findViewById(R.id.tvCobr_Anotac)
        vsCobros = findViewById(R.id.vsCobros)
        lyCobrosPend = findViewById(R.id.lyCobrosPend)
        tvPendiente = findViewById(R.id.tvPd_Pdte)
        tvFecha = findViewById(R.id.tvPd_Fecha)
        tvFVto = findViewById(R.id.tvPd_FVto)
        tvPdFPago = findViewById(R.id.tvPd_FPago)
        btnCambVista = findViewById(R.id.btnCobr_Switch)

        // Establecemos el ancho del código del cliente.
        editTextMaxLength(edtCodClte, ancho_codclte.toInt())
        fFtoDecImpIva = fConfiguracion.formatoDecImptesIva()
        idPendiente = 0
        tvFPagoClte.text = ""
        tvRiesgo.text = ""
        tvSaldo.text = ""
        prepararEdit()

        fRecycler = findViewById(R.id.rvCobros)
        fRecycler.layoutManager = LinearLayoutManager(this)
        prepararRecycler()

        fRecPdtes = findViewById(R.id.rvPdtes)
        fRecPdtes.layoutManager = LinearLayoutManager(this)

        if (fCliente > 0) {
            edtCodClte.isEnabled = false
            mostrarCliente(fCliente)
            // Quitamos el teclado de otra forma distinta a como lo hacemos en
            // ocultarTeclado(). De esta forma funciona cuando llamamos a la actividad
            // desde otra (p.ej. desde ventas) y con ocultarTeclado() funciona cuando
            // entramos a la actividad directamente.
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        } else  // Si entramos directamente en cobros, mostramos el teclado sin desplazar ningún layout.
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        // Llamamos a cambiarVista() porque queremos tener como primera pantalla la de documentos pendientes.
        cambiarVista(edtCodClte)
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.cobros)

        mostrarEmpresaActual()
    }


    private fun mostrarEmpresaActual() {
        val empresasDao: EmpresasDao? = MyDatabase.getInstance(this)?.empresasDao()
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)

        tvTitulo.text = empresasDao?.getNombreEmpresa(fEmpresaActual) ?: "Sin empresa actual"

        tvTitulo.setOnClickListener {
            val intent = Intent(this, ElegirEmpresaActivity::class.java)
            resultElegirEmpresa.launch(intent)
        }
    }


    private var resultElegirEmpresa = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fEmpresaActual = result.data?.getIntExtra("codEmpresa", 0) ?: 0
            // Guardamos la empresa actual en las preferencias para poder tener acceso al dato
            // desde otras actividades
            prefs.edit().putInt("ultima_empresa", fEmpresaActual).apply()
            mostrarEmpresaActual()
        }
    }


    private fun prepararRecPdtes() {
        fAdpPdtes  = PdtesRvAdapter(getPendiente(), this, object: PdtesRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: PendienteEnt) {
                idPendiente = data.pendienteId
                fPendiente.abrirPendienteId(idPendiente)
                mostrarPdte()
            }
        })

        fRecPdtes.adapter = fAdpPdtes
    }

    private fun getPendiente(): List<PendienteEnt> {
        fPendiente.abrir(fCliente)
        return fPendiente.lPendiente
    }


    @SuppressLint("SetTextI18n")
    private fun mostrarPdte() {
        val dImporte = fPendiente.importe.toDouble()
        val dCobrado = fPendiente.cobrado.toDouble()
        val dPendiente = dImporte - dCobrado
        tvPendiente.text = String.format(fFtoDecImpIva, dPendiente)
        var sFecha = fPendiente.fechaDoc
        if (fPendiente.estado == "E" || fPendiente.estado == "L")
            tvFecha.text = sFecha.substring(8, 10) + '/' + sFecha.substring(5, 7) + '/' + sFecha.substring(0, 4)
        else tvFecha.text = sFecha
        sFecha = fPendiente.fechaVto

        if (fPendiente.flag and FLAGPENDIENTE_EN_CARTERA > 0) {
            if (fPendiente.enviar.equals("T", ignoreCase = true))
                tvFVto.text = sFecha
            else
                if (sFecha != "") tvFVto.text = sFecha.substring(8, 10) + '/' + sFecha.substring(5, 7) + '/' + sFecha.substring(0, 4)
        } else {
            if (sFecha != "") tvFVto.text =
                sFecha.substring(8, 10) + '/' + sFecha.substring(5, 7) + '/' + sFecha.substring(0, 4)
        }
        tvPdFPago.text = fPendiente.descrFPago
    }


    private fun prepararRecycler() {
        fAdapter  = CobrosRvAdapter(getCobros(), this, object: CobrosRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: CobrosEnt) {
                val queFpago = data.fPago
                val queDescrFPago = formasPagoDao?.getDescrFPago(queFpago) ?: ""
                tvFPago.text = queDescrFPago
                tvDivisa.text = data.divisa
                tvAnotacion.text = data.anotacion
            }
        })

        fRecycler.adapter = fAdapter
    }


    private fun getCobros(): MutableList<CobrosEnt> {
        return fCobros.abrir(fCliente)
    }



    private fun prepararEdit() {
        edtCodClte.setOnKeyListener { v: View, keyCode: Int, event: KeyEvent ->
            if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
                val edtCodigo = v as EditText
                val queCodigo: String = edtCodigo.text.toString()
                //queCodigo = ponerCeros(queCodigo, ancho_codclte.toString().toByte())
                val queCliente = fClientes.existeCodigo(queCodigo.toInt())
                if (queCliente > 0) mostrarCliente(queCliente)
                else {
                    MsjAlerta(this@CobrosActivity).alerta(resources.getString(R.string.msj_CodNoExiste))
                    edtCodigo.setText("")
                }
                return@setOnKeyListener true
            }
            false
        }
    }

    fun infCobros(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val builder = AlertDialog.Builder(this)
        builder.setTitle(HtmlCompat.fromHtml("<font color='#000000'>Introducir fechas</font>", HtmlCompat.FROM_HTML_MODE_LEGACY))
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.alert_dialog_pedir_fechas, null)
        builder.setView(dialogLayout)
        val edtDesdeFecha = dialogLayout.findViewById<EditText>(R.id.edtDesdeFecha)
        val edtHastaFecha = dialogLayout.findViewById<EditText>(R.id.edtHastaFecha)
        edtDesdeFecha.setOnClickListener { showDatePickerDialog(edtDesdeFecha) }
        edtHastaFecha.setOnClickListener { showDatePickerDialog(edtHastaFecha) }

        builder.setPositiveButton("Aceptar") { _: DialogInterface?, _: Int ->
            val desdeFecha = edtDesdeFecha.text.toString()
            val hastaFecha = edtHastaFecha.text.toString()

            if (desdeFecha != "" && hastaFecha != "") {
                val i = Intent(fContexto, InfCobrosActivity::class.java)
                i.putExtra("desdeFecha", desdeFecha)
                i.putExtra("hastaFecha", hastaFecha)
                startActivityForResult(i, fRequestInfCobros)
            }
            else {
                MsjAlerta(fContexto).alerta("Tiene que indicar las dos fechas")
            }
        }
        builder.setNegativeButton("Cancelar") { dialog: DialogInterface, _: Int -> dialog.cancel() }
        val alert = builder.create()
        alert.show()
    }


    private fun showDatePickerDialog(editText: EditText) {
        val newFragment = DatePickerFragment.newInstance { _, year, month, day ->
            val selectedDate = ponerCeros(day.toString(), 2) + "/" + ponerCeros((month + 1).toString(), 2) + "/" + year
            editText.setText(selectedDate)
        }
        newFragment.show(supportFragmentManager, "datePicker")
    }


    class DatePickerFragment: DialogFragment() {

        private var listener: DatePickerDialog.OnDateSetListener? = null

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Usamos la fecha actual por defecto en el picker
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            // Creamos una nueva instancia de DatePickerDialog y la devolvemos
            return DatePickerDialog(requireContext(), listener, year, month, day)
        }

        companion object {
            fun newInstance(listener: DatePickerDialog.OnDateSetListener): DatePickerFragment {
                val fragment = DatePickerFragment()
                fragment.listener = listener
                return fragment
            }
        }
    }


    fun buscarCliente(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val i = Intent(this, ClientesActivity::class.java)
        i.putExtra("buscar", true)
        startActivityForResult(i, fRequestBuscarClte)
    }


    private fun continuarPagare(queAnotacion: String, queFechaPagare: String) {
        val aldDialog = nuevoAlertBuilder(this, "Marcar", resources.getString(R.string.msj_MarcarPagare), true)
        aldDialog.setPositiveButton("OK") { _: DialogInterface?, _: Int ->
            fPendiente.actualizarFechaPagare(queFechaPagare, queAnotacion)

            idPendiente = 0
            prepararRecPdtes()
            // Ocultamos el teclado
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }
        val alert = aldDialog.create()
        alert.show()
    }


    fun marcarPagare(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        if (idPendiente == 0) {
            MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
        } else {

            fPendiente.abrirPendienteId(idPendiente)

            if (fPendiente.flag and FLAGPENDIENTE_EN_CARTERA == 0) {
                if (fPendiente.cobrado.toDouble() < fPendiente.importe.toDouble()) {
                    if (fPendiente.tipoDoc == TIPODOC_FACTURA && fPendiente.cApunte != "") {
                        val i = Intent(this, PedirAnotacion::class.java)
                        startActivityForResult(i, fRequestPedirAnotacion)
                    } else MsjAlerta(this).alerta(resources.getString(R.string.msj_VtoDeFra))
                } else MsjAlerta(this).alerta(resources.getString(R.string.msj_VtoCobrado))
            } else MsjAlerta(this).alerta(resources.getString(R.string.msj_VtoYaEsPagare))
        }
    }

    fun cobrar(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        if (vsCobros.currentView === lyCobrosPend) {
            if (idPendiente == 0) MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc)) else {
                if (fPendiente.flag and FLAGPENDIENTE_EN_CARTERA == 0) {
                    val i = Intent(this, Cobrar::class.java)
                    i.putExtra("espendiente", true)
                    i.putExtra("fpagoDoc", "")
                    i.putExtra("idPendiente", idPendiente)
                    i.putExtra("cliente", fCliente)
                    startActivityForResult(i, fRequestCobrarPdte)
                } else MsjAlerta(this).alerta(resources.getString(R.string.msj_VtoYaEsPagare))
            }
        } else {
            if (fCliente > 0 && fConfiguracion.hacerEntrCta()) {
                val i = Intent(this, Cobrar::class.java)
                i.putExtra("espendiente", false)
                i.putExtra("fpagoDoc", "")
                i.putExtra("cliente", fCliente)
                startActivityForResult(i, fRequestCobrar)
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Buscar cliente.
        if (requestCode == fRequestBuscarClte) {
            if (resultCode == RESULT_OK) {
                val queCliente = data?.getIntExtra("cliente", -1) ?: 0
                mostrarCliente(queCliente)
            }
            // Cobrar. Refrescaremos el listView si hemos hecho un nuevo cobro.
        } else if (requestCode == fRequestCobrar) {
            if (resultCode == RESULT_OK) {
                refrescarCobros()
                // Volvemos a mostrar los datos del cliente por si ha cambiado el saldo.
                mostrarCliente(fCliente)
            }
        } else if (requestCode == fRequestCobrarPdte) {
            if (resultCode == RESULT_OK) {
                prepararRecPdtes()
                refrescarCobros()
                // Volvemos a mostrar los datos del cliente por si ha cambiado el saldo.
                mostrarCliente(fCliente)
            }
        } else if (requestCode == fRequestPedirAnotacion) {
            var queAnotacion = ""
            var queFechaPagare = ""
            if (resultCode == RESULT_OK) {
                queAnotacion = data?.getStringExtra("anotacion") ?: ""
                queFechaPagare = data?.getStringExtra("fechaPagare") ?: ""
            }
            continuarPagare(queAnotacion, queFechaPagare)
        }
    }


    private fun mostrarCliente(queCliente: Int) {
        if (fClientes.abrirUnCliente(queCliente)) {
            fCliente = queCliente
            val fCodClte = fClientes.fCodigo
            val fNombreClte = fClientes.fNombre
            val fNomComClte = fClientes.fNomComercial
            edtCodClte.setText(ponerCeros(fCodClte, ancho_codclte))
            tvNombre.text = fNombreClte
            tvNomCom.text = fNomComClte
            tvFPagoClte.text = fClientes.nombreFPago(fClientes.fPago)
            tvRiesgo.text = String.format(fFtoDecImpIva, fClientes.fRiesgo)
            tvSaldo.text = String.format(fFtoDecImpIva, fClientes.getSaldo())

            refrescarCobros()
            prepararRecPdtes()
        }
    }

    private fun refrescarCobros() {
        prepararRecycler()
        val fTotal = fCobros.dimeTotalCobros(fCliente)
        tvTotal.text = String.format(fFtoDecImpIva, fTotal)

        ocultarTeclado()
    }


    private fun ocultarTeclado() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(edtCodClte.windowToken, 0)
    }


    fun crearPDFCobros(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        var esDiferida = false
        var fIdDocumento = fPendiente.getCabeceraId()
        // Si no encontramos la factura puede que sea diferida
        if (fIdDocumento == 0) {
            fIdDocumento = fPendiente.dimeIdDocDiferido()
            esDiferida = true
        }
        if (fIdDocumento > 0) {
            if (esDiferida) {
                val dialogo = ProgressDialog.show(this, "Exportar a PDF", "Creando PDF ...", true, true)
                val finalFIdDocumento = fIdDocumento
                val hiloExport: Thread = object : Thread() {
                    override fun run() {
                        // Necesitamos hacer Looper.prepare() y Looper.loop() para mostrar el
                        // mensaje de alerta dentro del hilo.
                        Looper.prepare()
                        // Creo el objeto de tipo DocPDF y le digo que genere el fichero.
                        val documPDF = DocDiferidaPDF(this@CobrosActivity, finalFIdDocumento)
                        documPDF.crearPDF()

                        // Cerramos el diálogo y mostramos mensaje.
                        dialogo.dismiss()

                        // Comprobamos si el Whatsapp está instalado
                        if (whatsappInstalado(this@CobrosActivity)) {
                            val aldDialog = nuevoAlertBuilder(
                                this@CobrosActivity,
                                "Escoja",
                                "Enviar documento PDF",
                                true
                            )

                            aldDialog.setPositiveButton("Por email") { _, _ ->
                                // Enviamos el documento por email.
                                documPDF.enviarPorEmail()
                                MsjAlerta(this@CobrosActivity).alerta("Se terminó de exportar")
                            }
                            aldDialog.setNegativeButton("Por whatsapp") { _, _ ->
                                val telfDao: ContactosCltesDao? = MyDatabase.getInstance(fContexto)?.contactosCltesDao()
                                val lTelefonos = telfDao?.getTlfsCliente(fCliente) ?: emptyList<ContactosCltesEnt>().toMutableList()
                                if (lTelefonos.isNotEmpty()) {
                                    var numeroTelefono = lTelefonos[0].telefono1
                                    if (numeroTelefono == "") numeroTelefono = lTelefonos[0].telefono2
                                    // Si no añadimos el prefijo no funciona
                                    if (!numeroTelefono.startsWith("34"))
                                        numeroTelefono = "34$numeroTelefono"

                                    enviarPorWhatsapPdf(this@CobrosActivity, documPDF.nombrePDF, numeroTelefono)
                                    MsjAlerta(this@CobrosActivity).alerta("Se terminó de exportar")
                                }
                            }
                            aldDialog.setCancelable(true)
                            val alert = aldDialog.create()
                            alert.show()
                        }

                        Looper.loop()
                    }
                }
                hiloExport.start()
            } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoDiferida))
        } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
    }


    @SuppressLint("SetTextI18n")
    fun cambiarVista(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        vsCobros.showNext()
        if (vsCobros.currentView == lyCobrosPend) btnCambVista.setText(R.string.btn_cobros)
        else btnCambVista.text = "Pendiente"
    }


    fun ver(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        if (vsCobros.currentView === lyCobrosPend) {
            if (idPendiente > 0) {
                // Averiguo el id del documento para poder visualizarlo.
                val fIdDocumento = fPendiente.getCabeceraId()
                if (fIdDocumento > 0) {
                    val i = Intent(this, VentasLineas::class.java)
                    i.putExtra("nuevo", false)
                    i.putExtra("solover", true)
                    i.putExtra("iddoc", fIdDocumento)
                    i.putExtra("tipodoc", fPendiente.tipoDoc)
                    startActivity(i)
                } else MsjAlerta(this).alerta(resources.getString(R.string.msj_DocNoCargado))
            } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
        }
    }


}