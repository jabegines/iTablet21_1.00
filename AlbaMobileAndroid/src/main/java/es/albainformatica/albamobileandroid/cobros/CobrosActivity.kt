package es.albainformatica.albamobileandroid.cobros

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.text.Html
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.ContactosCltesDao
import es.albainformatica.albamobileandroid.dao.EmpresasDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.CobrosEnt
import es.albainformatica.albamobileandroid.entity.ContactosCltesEnt
import es.albainformatica.albamobileandroid.impresion_informes.DocDiferidaPDF
import es.albainformatica.albamobileandroid.maestros.ClientesActivity
import es.albainformatica.albamobileandroid.maestros.ClientesClase
import es.albainformatica.albamobileandroid.maestros.ElegirEmpresaActivity
import es.albainformatica.albamobileandroid.ventas.VentasLineas
import kotlinx.android.synthetic.main.ventas_verhcoartclte.*
import java.util.*

class CobrosActivity: AppCompatActivity() {
    private lateinit var fClientes: ClientesClase
    private lateinit var fCobros: CobrosClase
    private lateinit var fPendiente: PendienteClase
    private lateinit var fConfiguracion: Configuracion

    private var fCliente = 0
    private var idPendiente = 0
    private lateinit var fContexto: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var fRecycler: RecyclerView
    private lateinit var fAdapter: CobrosRvAdapter

    private lateinit var edtCodClte: EditText
    private lateinit var tvNombre: TextView
    private lateinit var tvNomCom: TextView
    private lateinit var tvFPagoClte: TextView
    private lateinit var tvRiesgo: TextView
    private lateinit var tvSaldo: TextView
    private lateinit var tvTotal: TextView
    private lateinit var adapterPdtes: SimpleCursorAdapter
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

    private val REQUEST_BUSCAR_CLTE = 1
    private val REQUEST_COBRAR = 2
    private val REQUEST_COBRAR_PDTE = 3
    private val REQUEST_INF_COBROS = 4
    private val REQUEST_PEDIR_ANOTACION = 5


    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        fContexto = this
        setContentView(R.layout.cobros)
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
        fClientes.close()
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
        prepararRecycler()

        prepararListViewPdtes()
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




    private fun prepararListViewPdtes() {
        val columnas: Array<String> = arrayOf("tipoDoc", "serie", "numero", "importe", "cobrado", "flag")
        val to = intArrayOf(R.id.lyvpTipoDoc, R.id.lyvpSerie, R.id.lyvpNumero, R.id.lyvpImporte, R.id.lyvpCobrado, R.id.lyvpFlag)
        adapterPdtes = SimpleCursorAdapter(this, R.layout.ly_ver_pendientes, fPendiente.cursor, columnas, to, 0)
        formatearColumnasPdtes()
        val lvPdtes = findViewById<ListView>(R.id.lvPdtes)
        lvPdtes.adapter = adapterPdtes

        // Establecemos el evento on click del ListView.
        lvPdtes.onItemClickListener =
            AdapterView.OnItemClickListener { listView: AdapterView<*>, _: View?, position: Int, _: Long ->
                val cursor = listView.getItemAtPosition(position) as Cursor
                idPendiente = cursor.getInt(0)

                fPendiente.abrirPendienteId(idPendiente)
                mostrarPdte()
            }
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


    private fun formatearColumnasPdtes() {
        adapterPdtes.viewBinder = SimpleCursorAdapter.ViewBinder { view: View, cursor: Cursor, column: Int ->
                val tv = view as TextView

                // Formateamos el tipo de documento.
                if (column == 5) {
                    if (cursor.getString(cursor.getColumnIndex("tipoDoc")) != "")
                        if (cursor.getString(cursor.getColumnIndex("tipoDoc")).toInt() == 33)
                            tv.setText(R.string.vtos_agrup)
                        else
                            tv.text = tipoDocAsString(cursor.getString(cursor.getColumnIndex("tipoDoc")).toShort()
                    ) else tv.text = ""
                    return@ViewBinder true
                }
                // Serie
                if (column == 8) {
                    if (cursor.getString(cursor.getColumnIndex("tipoDoc")).toByte().toInt() != 0
                    ) tv.text = cursor.getString(cursor.getColumnIndex("serie")) else tv.text = ""
                    return@ViewBinder true
                }
                // Número
                if (column == 9) {
                    if (cursor.getString(cursor.getColumnIndex("tipoDoc")).toByte().toInt() != 33)
                        tv.text = cursor.getString(cursor.getColumnIndex("numero")) else tv.text = ""
                    return@ViewBinder true
                }
                // Formateamos el total.
                if (column == 10 || column == 11) {
                    val sTotal = cursor.getString(column).replace(',', '.')
                    val dTotal = sTotal.toDouble()
                    tv.text = String.format(fFtoDecImpIva, dTotal)
                    return@ViewBinder true
                }
                // Flag
                if (column == 20) {
                    if (cursor.getInt(cursor.getColumnIndex("flag")) and FLAGPENDIENTE_EN_CARTERA > 0) {
                        tv.text = "*"
                    }
                    return@ViewBinder true
                }
                false
            }
    }


    private fun prepararRecycler() {
        fAdapter  = CobrosRvAdapter(getCobros(), this, object: CobrosRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: CobrosEnt) {
            }
        })

        fRecycler.layoutManager = LinearLayoutManager(this)
        fRecycler.adapter = fAdapter
    }

    private fun getCobros(): MutableList<CobrosEnt> {
        return fCobros.abrir(fCliente)
    }

/*
    private fun prepararListViewCobros() {
        val columnas: Array<String> = arrayOf("fechacobro", "tipodoc", "serie", "numero", "cobro")
        val to = intArrayOf(R.id.lyvdFecha, R.id.lyvdTipoDoc, R.id.lyvdSerie, R.id.lyvdNumero, R.id.lyvdTotal)
        adapterCobros = SimpleCursorAdapter(this, R.layout.ly_ver_documentos, fCobros.cursor, columnas, to, 0)
        formatearColumnasCobros()
        val lvCobros = findViewById<ListView>(R.id.lvCobros)
        lvCobros.adapter = adapterCobros

        // Establecemos el evento on click del ListView.
        lvCobros.onItemClickListener =
            AdapterView.OnItemClickListener { listView: AdapterView<*>, _: View?, position: Int, _: Long ->
                val cursor = listView.getItemAtPosition(position) as Cursor

                val queFpago = cursor.getString(cursor.getColumnIndex("fpago"))
                val queDescrFPago = formasPagoDao?.getDescrFPago(queFpago) ?: ""
                tvFPago.text = queDescrFPago
                tvDivisa.text = cursor.getString(cursor.getColumnIndex("descrdivisa"))
                tvAnotacion.text = cursor.getString(cursor.getColumnIndex("anotacion"))
            }
    }
*/

    /*
    private fun formatearColumnasCobros() {
        adapterCobros.viewBinder = SimpleCursorAdapter.ViewBinder { view: View, cursor: Cursor, column: Int ->
                val tv = view as TextView

                // Formateamos el tipo de documento.
                if (column == 2) {
                    if (cursor.getString(cursor.getColumnIndex("tipodoc")) != "") tv.text =
                        tipoDocAsString(cursor.getString(cursor.getColumnIndex("tipodoc")).toShort()) else tv.text = ""
                    return@ViewBinder true
                }
                // Formateamos el total.
                if (column == 9) {
                    val sTotal =
                        cursor.getString(cursor.getColumnIndex("cobro")).replace(',', '.')
                    val dTotal = sTotal.toDouble()
                    tv.text = String.format(fFtoDecImpIva, dTotal)
                    return@ViewBinder true
                }
                false
            }
    }
    */

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
        builder.setTitle(Html.fromHtml("<font color='#000000'>Introducir fechas</font>"))
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
                startActivityForResult(i, REQUEST_INF_COBROS)
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
        startActivityForResult(i, REQUEST_BUSCAR_CLTE)
    }


    private fun continuarPagare(queAnotacion: String, queFechaVto: String) {
        val aldDialog = NuevoAlertBuilder(this, "Marcar", resources.getString(R.string.msj_MarcarPagare), true)
        aldDialog.setPositiveButton("OK") { _: DialogInterface?, _: Int ->
            fPendiente.actualizarFechaVto(queFechaVto, queAnotacion)

            // Tengo que cerrar y abrir fPendiente porque si no, no refresca el listView de pendientes.
            idPendiente = 0
            if (fPendiente.cursor != null) fPendiente.cursor!!.close()
            fPendiente.abrir(fCliente)
            refrescarPdtes()
            // Ocultamos el teclado
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }
        val alert = aldDialog.create()
        alert.show()
        ColorDividerAlert(this, alert)
    }


    fun marcarPagare(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        if (idPendiente == 0) {
            MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
        } else {

            fPendiente.abrirPendienteId(idPendiente)

            if (fPendiente.flag and FLAGPENDIENTE_EN_CARTERA == 0) {
                if (fPendiente.cobrado.toDouble() < fPendiente.importe.toDouble()) {
                    if (fPendiente.tipoDoc == TIPODOC_FACTURA.toString() && fPendiente.cApunte != "") {
                        val i = Intent(this, PedirAnotacion::class.java)
                        startActivityForResult(i, REQUEST_PEDIR_ANOTACION)
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
                    startActivityForResult(i, REQUEST_COBRAR_PDTE)
                } else MsjAlerta(this).alerta(resources.getString(R.string.msj_VtoYaEsPagare))
            }
        } else {
            if (fCliente > 0) {
                val i = Intent(this, Cobrar::class.java)
                i.putExtra("espendiente", false)
                i.putExtra("fpagoDoc", "")
                i.putExtra("cliente", fCliente)
                startActivityForResult(i, REQUEST_COBRAR)
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Buscar cliente.
        if (requestCode == REQUEST_BUSCAR_CLTE) {
            if (resultCode == RESULT_OK) {
                val queCliente = data?.getIntExtra("cliente", -1) ?: 0
                mostrarCliente(queCliente)
            }
            // Cobrar. Refrescaremos el listView si hemos hecho un nuevo cobro.
        } else if (requestCode == REQUEST_COBRAR) {
            if (resultCode == RESULT_OK) {
                refrescarCobros()
                // Volvemos a mostrar los datos del cliente por si ha cambiado el saldo.
                mostrarCliente(fCliente)
            }
        } else if (requestCode == REQUEST_COBRAR_PDTE) {
            if (resultCode == RESULT_OK) {
                // Tengo que cerrar y abrir fPendiente porque si no, no refresca el listView de pendientes.
                if (fPendiente.cursor != null) fPendiente.cursor!!.close()
                fPendiente.abrir(fCliente)
                refrescarPdtes()
                refrescarCobros()
                // Volvemos a mostrar los datos del cliente por si ha cambiado el saldo.
                mostrarCliente(fCliente)
            }
        } else if (requestCode == REQUEST_PEDIR_ANOTACION) {
            var queAnotacion: String? = ""
            var queFechaVto: String? = ""
            if (resultCode == RESULT_OK) {
                queAnotacion = data?.getStringExtra("anotacion")
                queFechaVto = data?.getStringExtra("fechavto")
            }
            continuarPagare(queAnotacion!!, queFechaVto!!)
        }
    }


    private fun mostrarCliente(queCliente: Int) {
        if (fClientes.abrirUnCliente(queCliente)) {
            fCliente = queCliente
            val fCodClte = fClientes.fCodigo
            val fNombreClte = fClientes.fNombre
            val fNomComClte = fClientes.fNomComercial
            edtCodClte.setText(ponerCeros(fCodClte.toString(), ancho_codclte))
            tvNombre.text = fNombreClte
            tvNomCom.text = fNomComClte
            tvFPagoClte.text = fClientes.nombreFPago(fClientes.getFPago())
            tvRiesgo.text = String.format(fFtoDecImpIva, fClientes.getRiesgo())
            tvSaldo.text = String.format(fFtoDecImpIva, fClientes.getSaldo())
            fClientes.close()
            fPendiente.abrir(fCliente)
            refrescarCobros()
            refrescarPdtes()
        }
    }

    private fun refrescarCobros() {
        prepararRecycler()
        val fTotal = fCobros.dimeTotalCobros(fCliente)
        tvTotal.text = String.format(fFtoDecImpIva, fTotal)
        refrescarLineas()
        ocultarTeclado()
    }


    private fun refrescarPdtes() {
        adapterPdtes.changeCursor(fPendiente.cursor)
    }

    private fun ocultarTeclado() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(edtCodClte.windowToken, 0)
    }

    private fun refrescarLineas() {
        //adapterCobros.changeCursor(fCobros.cursor)
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
                            val aldDialog = NuevoAlertBuilder(
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

                                    fClientes.close()
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
                    startActivity(i)
                } else MsjAlerta(this).alerta(resources.getString(R.string.msj_DocNoCargado))
            } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
        }
    }


}