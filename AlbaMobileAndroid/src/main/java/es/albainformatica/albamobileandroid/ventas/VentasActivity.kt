package es.albainformatica.albamobileandroid.ventas

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.cobros.CobrosActivity
import es.albainformatica.albamobileandroid.comunicaciones.ServicioEnviar
import es.albainformatica.albamobileandroid.dao.CabecerasDao
import es.albainformatica.albamobileandroid.dao.EmpresasDao
import es.albainformatica.albamobileandroid.dao.SeriesDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.maestros.*
import es.albainformatica.albamobileandroid.registroEventos.RegistroEventosClase
import kotlinx.android.synthetic.main.ventas_rutero.*


class VentasActivity: AppCompatActivity() {
    private val cabecerasDao: CabecerasDao? = MyDatabase.getInstance(this)?.cabecerasDao()
    private val seriesDao: SeriesDao? = MyDatabase.getInstance(this)?.seriesDao()
    private lateinit var fConfiguracion: Configuracion
    private lateinit var prefs: SharedPreferences
    private lateinit var fRutero: Rutero
    private lateinit var fRutas: Rutas
    private lateinit var fClientes: ClientesClase
    private lateinit var fRegEventos: RegistroEventosClase

    private lateinit var fRecRutero: RecyclerView
    private lateinit var fAdpRutero: RuteroRvAdapter

    private var fUsarRutero: Boolean = false
    private var fUsarCP: Boolean = false
    private var fUsarServicio: Boolean = false
    private var fEnvDocAutom: Boolean = false
    private var fClasDocEnvG: Boolean = false
    private var fClteDoc: Int = 0
    private var fCodPostal: String = ""
    private var fRutaActiva: Short = 0
    private var fTipoDoc: Short = 0
    private var fTipoPedido: Int = 0
    private var fAntClteDoc: Int = 0
    private var fClteFueraDeRuta: Boolean = false
    private var fProblemasEnvio: Boolean = false
    private var fAplOftEnPed: Boolean = true

    private lateinit var edtCodClte: EditText
    private lateinit var tvNombreClte: TextView
    private lateinit var tvNombreCom: TextView
    private lateinit var rdbFra: RadioButton
    private lateinit var rdbAlb: RadioButton
    private lateinit var rdbPed: RadioButton
    private lateinit var rdbPresp: RadioButton
    private lateinit var spRuta: Spinner
    private lateinit var imvNoVender: ImageView
    private lateinit var tvSerie: TextView
    private var fEmpresaActual: Int = 0



    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)

        fRegEventos = Comunicador.fRegEventos
        fRegEventos.registrarEvento(codEv_Vtas_Entrar, descrEv_Vtas_Entrar)

        fConfiguracion = Comunicador.fConfiguracion
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        fEmpresaActual = prefs.getInt("ultima_empresa", 0)
        fUsarCP = prefs.getBoolean("ventas_rutero_cp", false)
        fUsarRutero = fConfiguracion.usarRutero() && !fUsarCP
        fUsarServicio = prefs.getBoolean("usar_servicio", false)
        fEnvDocAutom = prefs.getBoolean("enviar_docs_autom", false)
        fClasDocEnvG = prefs.getBoolean("ventas_enviar_guardar", false)

        val intent = intent
        fClteDoc = intent.getIntExtra("cliente", 0)

        // Según usemos el rutero o no, cargaremos un layout u otro.
        when {
            fUsarCP -> setContentView(R.layout.ventas_cp)
            fUsarRutero -> setContentView(R.layout.ventas_rutero)
            else -> setContentView(R.layout.ventas)
        }

        fRutero = Rutero(this)
        fRutas = Rutas(this)
        fClientes = ClientesClase(this)

        inicializarControles()
    }


    override fun onResume() {
        super.onResume()

        if (fUsarServicio) {
            if (fEnvDocAutom) {
                // Si hemos tenido problemas al enviar el último documento no reintentaremos el envío inmediatamente, sino
                // en el siguiente onResume()
                if (!fProblemasEnvio) {
                    if (hayDocParaEnviar()) {
                        val intent = Intent(this, ServicioEnviar::class.java)
                        intent.putExtra("enviarAutom", true)
                        resultEnvServicio.launch(intent)
                    }
                }
                else fProblemasEnvio = false
            }
        }
    }

    override fun onDestroy() {
        // Si estamos usando rutero, guardamos el último cliente al que le hemos vendido,
        // para continuar por el siguiente cuando volvamos a entrar en ventas.
        if (fUsarRutero || fUsarCP) guardarPreferencias()

        // Guardamos la ruta activa para volver a presentarla la siguiente vez que entremos en ventas.
        if (fUsarRutero) fConfiguracion.activarRuta(fRutaActiva)

        fRegEventos.registrarEvento(codEv_Vtas_Salir, descrEv_Vtas_Salir)

        super.onDestroy()
    }



    private fun hayDocParaEnviar(): Boolean {
        // Si tenemos rutero_reparto mandaremos también las cabeceras de los documentos
        // que estén firmados o tengan alguna incidencia (sólo las cabeceras).
        val queCabId: Int = if (fConfiguracion.hayReparto())
            cabecerasDao?.hayDocsParaEnvRep() ?: 0
        else
            cabecerasDao?.hayDocsParaEnviar() ?: 0

        return (queCabId > 0)
    }



    private fun guardarPreferencias() {
        prefs.edit().putInt("vtas_ult_clte", fClteDoc).apply()
        prefs.edit().putString("vtas_ult_cpostal", fCodPostal).apply()
    }


    private fun inicializarControles() {
        rdbFra = findViewById(R.id.rdbVt_Fra)
        rdbAlb = findViewById(R.id.rdbVt_Alb)
        rdbPed = findViewById(R.id.rdbVt_Ped)
        rdbPresp = findViewById(R.id.rdbVt_Presp)

        if (fUsarRutero)
            spRuta = findViewById(R.id.spnVt_Ruta)

        if (!fUsarCP && !fUsarRutero) {
            imvNoVender = findViewById(R.id.imgNoVender)
            imvNoVender.visibility = View.GONE
        }

        // Configuramos si tenemos autoventa y/o preventa
        if (!fConfiguracion.hayAutoventa()) {
            rdbFra.visibility = View.GONE
            rdbAlb.visibility = View.GONE
        }

        if (!fConfiguracion.hayPreventa()) {
            rdbPed.visibility = View.GONE
            rdbPresp.visibility = View.GONE
            rdbPresp.visibility = View.GONE
        }

        ocultarTeclado(this)

        when {
            fUsarCP -> inicContrCodPostal()
            fUsarRutero -> inicContrRutero()
            else -> inicContrSinRutero()
        }

        prepararSpinners()
        prepararRadioGr()

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

        // Vemos si tenemos alguna serie por defecto para el ejercicio actual
        val queSerie = seriesDao?.getSeriePorDefEj(fEmpresaActual.toShort(), fConfiguracion.ejercicio()) ?: ""
        if (queSerie != "") {
            tvSerie.text = queSerie
        } else {
            MsjAlerta(this).alerta(resources.getString(R.string.msj_SerieNoEjerc))
        }

        /*
        val queSerie = empresasDao?.getSerieEmpresa(fEmpresaActual) ?: ""

        if (queSerie != "") {
            // Vemos si la serie pertenece al ejercicio actual
            val lEjSerie = seriesDao?.ejercicioSerie(queSerie) ?: emptyList<Short>().toMutableList()
            var haySerie = false
            for (queEjercicio in lEjSerie) {
                if (queEjercicio == fConfiguracion.ejercicio()) {
                    haySerie = true
                }
            }

            if (haySerie) {
                tvSerie.text = queSerie
            } else {
                MsjAlerta(this).alerta(resources.getString(R.string.msj_SerieNoEjerc))
            }
        }
        else tvSerie.text = ""
        */
    }


    private fun inicContrRutero() {
        fRutaActiva = fConfiguracion.rutaActiva()
        if (fRutaActiva > 0) {
            mostrarRutaAct()
        }

        fRecRutero = rvRutero
        fRecRutero.layoutManager = LinearLayoutManager(this)
        prepararRvRutero()

        if (fRutaActiva > 0) {
            if (fClteDoc == 0) {
                // Vemos el último cliente al que le vendimos y lo ponemos en negrita
                fClteDoc = prefs.getInt("vtas_ult_clte", 0)
                fAdpRutero.localizarClte(fClteDoc)
            }
        } else
            fClteDoc = 0

        tvSerie = findViewById(R.id.edtVtSerie)
    }


    private fun inicContrCodPostal() {
        fCodPostal = prefs.getString("vtas_ult_cpostal", "") ?: ""
        mostrarCodPostalActivo(fCodPostal)

        fRecRutero = rvRutero
        fRecRutero.layoutManager = LinearLayoutManager(this)
        prepararRvRutero()

        if (fCodPostal != "") {
            if (fClteDoc == 0) {
                // Vemos el último cliente al que le vendimos y lo ponemos en negrita
                fClteDoc = prefs.getInt("vtas_ult_clte", 0)
                fAdpRutero.localizarClte(fClteDoc)
            }
        } else
            fClteDoc = 0

        tvSerie = findViewById(R.id.edtVtSerie)
    }


    private fun inicContrSinRutero() {
        edtCodClte = findViewById(R.id.edtVt_CodClte)
        tvNombreClte = findViewById(R.id.edtVt_NFiscal)
        tvNombreCom = findViewById(R.id.edtVt_NComercial)

        // Establecemos el ancho del código del cliente.
        editTextMaxLength(edtCodClte, ancho_codclte.toInt())
        edtCodClte.requestFocus()
        if (fClteDoc > 0)
            mostrarCliente(fClteDoc)

        prepararEdits()
        tvSerie = findViewById(R.id.edtVtSerie)
        //tvSerie.text = fConfiguracion.getSerie()
    }


    fun lanzarLineas(view: View?) {
        view?.getTag(0)              // Para que no dé warning el compilador

        if (datosCorrectos()) {
            when {
                rdbFra.isChecked -> fTipoDoc = TIPODOC_FACTURA
                rdbAlb.isChecked -> fTipoDoc = TIPODOC_ALBARAN
                rdbPed.isChecked -> fTipoDoc = TIPODOC_PEDIDO
                rdbPresp.isChecked -> fTipoDoc = TIPODOC_PRESUPUESTO
            }

            // Si vamos a hacer un pedido y el código de producto es el de Bionat, pediremos el tipo de pedido.
            // Hacemos esto así porque a Bionat se le vendió que el programa iba a pedir el tipo de pedido. El problema es
            // que es algo que sólo usan ellos, por eso controlo que el código de producto sea el de ellos para hacer la llamada
            // a BioTipoPedido()
            // Creamos un objeto de la clase BioTipoPedido y lo mostramos para elegir el tipo de pedido
            if (fTipoDoc == TIPODOC_PEDIDO &&
                fConfiguracion.codigoProducto().equals("UY6JK-6KAYw-PO0Py-6OX9B-OJOPY", ignoreCase = true)) {

                // Creamos un objeto de la clase BioTipoPedido y lo mostramos para elegir el tipo de pedido
                val newFragment = BioTipoPedido.newInstance()
                newFragment.show(supportFragmentManager, "dialog")

            } else {
                verRiesgoClte()
            }
        }
    }


    private fun verRiesgoClte() {
        if (fClientes.abrirUnCliente(fClteDoc)) {
            val continuar = !fClientes.clienteEnRiesgo(0.0, 1, fEmpresaActual)

            // Si hemos superado algún límite de riesgo presentaremos la ventana, aunque seguiremos vendiendo ya
            // que permitiremos la venta de contado.
            if (!continuar) {
                val intent = Intent(this, VerRiesgo::class.java)
                intent.putExtra("cliente", fClteDoc)
                intent.putExtra("empresa", fEmpresaActual)
                intent.putExtra("totalDoc", 0.0)
                intent.putExtra("numDocs", 0)
                if (fTipoDoc == TIPODOC_FACTURA) intent.putExtra("soloVer", true)
                else intent.putExtra("soloVer", false)

                resultVerRiesgo.launch(intent)
            } else
                lanzarVentasLineas()
        }
    }

    fun cambiarSerie(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val intent = Intent(this, SeleccSerieActivity::class.java)
        resultCambSerie.launch(intent)
    }


    fun buscarCliente(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val intent = Intent(this, ClientesActivity::class.java)
        intent.putExtra("buscar", true)
        resultBuscarCltes.launch(intent)
    }


    private fun lanzarVentasLineas() {
        val intent = Intent(this, VentasLineas::class.java)
        intent.putExtra("serie", tvSerie.text.toString())
        intent.putExtra("cliente", fClteDoc)
        intent.putExtra("tipodoc", fTipoDoc)
        intent.putExtra("tipopedido", fTipoPedido)
        intent.putExtra("aplOftEnPed", fAplOftEnPed)
        intent.putExtra("nuevo", true)
        resultVentasLineas.launch(intent)
    }


    private var resultVentasLineas = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Si estamos usando rutero, nos situamos en el siguiente cliente.
            if (fUsarRutero || fUsarCP) {
                if (fClteFueraDeRuta)
                    fClteDoc = fAntClteDoc
                else
                    siguienteClte()
            }
            // Volvemos a aconsejar la serie por defecto para el ejercicio y empresa
            val queSerie = seriesDao?.getSeriePorDefEj(fEmpresaActual.toShort(), fConfiguracion.ejercicio()) ?: ""
            if (queSerie != "") tvSerie.text = queSerie

        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            MsjAlerta(this).alerta(resources.getString(R.string.msj_SinSerie))
        }
        fClteFueraDeRuta = false
    }

    private var resultBuscarCltes = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (fUsarRutero || fUsarCP) {
                // fClteFueraDeRuta nos servirá para saber que estamos haciendo un documento a un cliente que no es del rutero,
                // de forma que, cuando terminemos el documento, volveremos a situarnos en el cliente en que estábamos (para eso
                // nos sirve fAntClteDoc).
                fAntClteDoc = fClteDoc
                fClteFueraDeRuta = true
                fClteDoc = result.data?.getIntExtra("cliente", -1) ?: 0

                lanzarLineas(null)
            } else
                mostrarCliente(result.data?.getIntExtra("cliente", -1) ?: 0)
        }
    }

    private var resultEnvServicio = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        fProblemasEnvio = result.resultCode != Activity.RESULT_OK
    }

    private var resultCambSerie = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            tvSerie.text = result.data?.getStringExtra("serie") ?: ""
        }
    }

    private var resultVerRiesgo = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            lanzarVentasLineas()
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

    fun finalizar(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        finish()
    }


    private fun prepararRadioGr() {
        if (!fConfiguracion.hacerFacturas()) rdbFra.visibility = View.GONE
        if (!fConfiguracion.hacerAlbaranes()) rdbAlb.visibility = View.GONE
        if (!fConfiguracion.hacerPedidos()) rdbPed.visibility = View.GONE
        if (!fConfiguracion.hacerPresup()) rdbPresp.visibility = View.GONE

        val docDefecto = prefs.getString("doc_defecto", "1")
        if (docDefecto == "1" && fConfiguracion.hacerFacturas()) rdbFra.isChecked = true
        if (docDefecto == "2" && fConfiguracion.hacerAlbaranes()) rdbAlb.isChecked = true
        if (docDefecto == "3" && fConfiguracion.hacerPedidos()) rdbPed.isChecked = true
        if (docDefecto == "4" && fConfiguracion.hacerPresup()) rdbPresp.isChecked = true
    }


    private fun prepararEdits() {
        editTextMaxLength(edtCodClte, ancho_codclte.toInt())

        edtCodClte.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {

                val edtCodigo = v as EditText
                val queCodigo = edtCodigo.text.toString()
                //queCodigo = ponerCeros(queCodigo, ancho_codclte)
                val queCliente = fClientes.existeCodigo(queCodigo.toInt())
                if (queCliente > 0)
                    mostrarCliente(queCliente)
                else {
                    MsjAlerta(this@VentasActivity).alerta(resources.getString(R.string.msj_CodNoExiste))
                    edtCodigo.setText("")
                }
                return@OnKeyListener true
            }
            false
        })
    }



    private fun prepararSpinners() {
        if (fUsarRutero) {
            // Spinner de rutas
            val aRutas: Array<String> = fRutas.abrir()
            val adaptador = ArrayAdapter(this, android.R.layout.simple_spinner_item, aRutas)
            adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spRuta.adapter = adaptador

            // Seleccionamos la ruta activa.
            spRuta.setSelection(getIndexRuta(spRuta, fRutaActiva))

            spRuta.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    val fOldRuta = fRutaActiva
                    val quePosicion = spRuta.getItemAtPosition(position).toString().indexOf(" ")
                    fRutaActiva = if (quePosicion > -1)
                        (spRuta.getItemAtPosition(position).toString().substring(0, quePosicion).trim { it <= ' ' }).toShort()
                    else
                        0

                    if (fRutaActiva != fOldRuta) {
                        if (fRutaActiva > 0) {
                            if (fRutero.abrirRuta(fRutaActiva)) {
                                mostrarRutaAct()
                                prepararRvRutero()
                                fClteDoc = fRutero.lRutero[0].clienteId

                            } else {
                                fRutaActiva = fOldRuta

                                // Volvemos a dejar el spinner con la ruta que teníamos
                                spRuta.setSelection(getIndexRuta(spRuta, fRutaActiva))
                                MsjAlerta(this@VentasActivity).alerta("La ruta no tiene clientes")
                            }
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun getIndexRuta(s1: Spinner, nombre: Short): Int {
        var index = 0
        for (i in 0 until s1.count) {
            val queCodigo = s1.getItemAtPosition(i).toString().substring(0, 2).trim { it <= ' ' }
            if (queCodigo == nombre.toString()) {
                index = i
            }
        }
        return index
    }


    private fun mostrarRutaAct() {
        val tvRutaAct = findViewById<TextView>(R.id.tvVtRutaAct)
        tvRutaAct?.text = fRutas.dimeNombre(fRutaActiva)
    }

    private fun mostrarCodPostalActivo(queCodPostal: String) {
        val tvVtCodPostal = findViewById<TextView>(R.id.tvVtCodPostal)
        val sCodPostal = getString(R.string.cod_postal) + queCodPostal
        tvVtCodPostal.text = sCodPostal
    }


    fun pendienteClte(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val queCliente: Int
        if (fUsarRutero || fUsarCP) {
            if (fClteDoc > 0)
                queCliente = fClteDoc
            else {
                queCliente = 0
                MsjAlerta(this).alerta(getString(R.string.msj_SinClte))
            }
        } else {
            if (edtCodClte.text.toString() != "") {
                val queCodClte = edtCodClte.text.toString()
                queCliente = fClientes.existeCodigo(queCodClte.toInt())
            }
            else {
                queCliente = 0
                MsjAlerta(this).alerta(getString(R.string.msj_SinClte))
            }
        }

        if (queCliente > 0) {
            val i = Intent(this, CobrosActivity::class.java)
            i.putExtra("cliente", queCliente)
            startActivity(i)
        }
    }


    fun verDocumentos(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val queCliente: Int = if (fUsarRutero || fUsarCP) {
            if (fClteDoc > 0)
                fClteDoc
            else {
                0
            }
        } else {
            val queCodClte = edtCodClte.text.toString()
            if (queCodClte != "")
                fClientes.existeCodigo(queCodClte.toInt())
            else
                0
        }

        //if (queCliente > 0) {
            val i = Intent(this, VerDocumentosActivity::class.java)
            i.putExtra("cliente", queCliente)
            startActivity(i)
        //}
    }


    fun verTodosDoc(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val i = Intent(this, VerDocumentosActivity::class.java)
        i.putExtra("cliente", 0)
        startActivity(i)
    }


    fun notasClte(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val queCliente: Int
        if (fUsarRutero || fUsarCP) {
            if (fClteDoc > 0)
                queCliente = fClteDoc
            else {
                queCliente = 0
                MsjAlerta(this).alerta(getString(R.string.msj_SinClte))
            }
        } else {
            val queCodClte = edtCodClte.text.toString()
            queCliente = if (queCodClte != "")
                fClientes.existeCodigo(queCodClte.toInt())
            else
                0
        }

        if (queCliente > 0) {
            val i = Intent(this, VerNotasCliente::class.java)
            i.putExtra("cliente", queCliente)
            startActivity(i)
        }
    }



    private fun mostrarCliente(queCliente: Int) {
        if (fClientes.abrirUnCliente(queCliente)) {
            edtCodClte.setText(ponerCeros(fClientes.fCodigo, ancho_codclte))
            tvNombreClte.text = fClientes.fNombre
            tvNombreCom.text = fClientes.fNomComercial

            if (fClientes.noVender()) imvNoVender.visibility = View.VISIBLE
            else imvNoVender.visibility = View.GONE
        }
    }



    private fun datosCorrectos(): Boolean {
        val continuar: Boolean
        if (fUsarRutero || fUsarCP) {
            continuar = fClteDoc > 0

        } else {
            val queCodClte = edtCodClte.text.toString()
            if (edtCodClte.text.toString() == "") return false

            //queCodClte = ponerCeros(queCodClte, ancho_codclte)
            val queCliente = fClientes.existeCodigo(queCodClte.toInt())
            if (queCliente > 0) {
                fClteDoc = queCliente
                continuar = true
            } else {
                fClteDoc = 0
                return false
            }
        }

        if (continuar) {

            if (fClientes.abrirUnCliente(fClteDoc)) {
                if (fClientes.noVender()) {
                    MsjAlerta(this).alerta(resources.getString(R.string.msj_NoVender))
                    return false
                }
            }

            return if (!rdbFra.isChecked && !rdbAlb.isChecked && !rdbPed.isChecked && !rdbPresp.isChecked) {
                MsjAlerta(this).alerta(resources.getString(R.string.msj_SinDocumento))
                //fClteDoc = 0;
                false
            } else
                true
        }

        return false
    }


    private fun prepararRvRutero() {
        fAdpRutero = RuteroRvAdapter(getCltesRuta(), fUsarRutero, this, object: RuteroRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosRutero) {
                // Tomamos el campo cliente de la fila en la que hemos pulsado
                fClteDoc = data.clienteId
            }
        })

        fRecRutero.adapter = fAdpRutero
    }

    private fun getCltesRuta(): List<DatosRutero> {
        if (fUsarRutero) {
            if (fRutaActiva > 0)
                fRutero.abrirRuta(fRutaActiva)
            else
                fRutero.abrirRuta(0)
        }
        else {
            fRutero.abrirCodPostal(fCodPostal)
        }

        return fRutero.lRutero
    }



    private fun siguienteClte() {
        fAdpRutero.selectedPos++
        fClteDoc = fAdpRutero.datosRutero[fAdpRutero.selectedPos].clienteId
        fAdpRutero.notifyDataSetChanged()
    }


    class BioTipoPedido: DialogFragment() {
        private var queOpcion: Int = 0

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

            queOpcion = 0

            val items = arrayOfNulls<String>(3)
            items[0] = "Ordinario"
            items[1] = "De campaña"
            items[2] = "De abono"

            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Escoja el tipo de pedido").setSingleChoiceItems(items, 0
            ) { _, item -> queOpcion = item }

            builder.setPositiveButton("Aceptar") { _, _ -> (activity as VentasActivity).elegirTipoPedido(queOpcion) }

            return builder.create()
        }

        companion object {

            //fun newInstance(title: Int): BioTipoPedido {
            fun newInstance(): BioTipoPedido {
                val frag = BioTipoPedido()
                val args = Bundle()
                //args.putInt("title", title)
                args.putInt("title", 1)
                frag.arguments = args
                return frag
            }
        }
    }


    fun elegirTipoPedido(item: Int) {

        val queItem = if (item < 0) { 0 } else item
        fTipoPedido = queItem + 1

        // Si vamos a realizar un pedido y al cliente se le pueden aplicar ofertas preguntaremos antes
        // si queremos aplicarlas o no
        var fClteAplOftas = true
        if (fClientes.abrirUnCliente(fClteDoc)) {
           fClteAplOftas = fClientes.getAplicarOfertas()
        }

        if (fClteAplOftas) {
            val aldDialog = nuevoAlertBuilder(this, resources.getString(R.string.tit_apl_oft_ped), resources.getString(R.string.dlg_apl_oft_ped), true)

            aldDialog.setPositiveButton(resources.getString(R.string.dlg_si)) { _, _ ->
                fAplOftEnPed = true
                verRiesgoClte()
            }
            aldDialog.setNegativeButton(resources.getString(R.string.dlg_no)) { _, _ ->
                fAplOftEnPed = false
                verRiesgoClte()
            }
            val alert = aldDialog.create()
            alert.show()
        }
        else verRiesgoClte()
    }


    @SuppressLint("InflateParams")
    fun seleccCodPostal(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        // Lo primero que debemos hacer es rescatar el layout creado para el prompt.
        val li = LayoutInflater.from(this)
        val prompt = li.inflate(R.layout.selecc_codpostal, null)
        val edtCodPostal = prompt.findViewById<EditText>(R.id.edtCodPostal)

        // Luego, creamos un constructor de Alert Dialog que nos ayudará a utilizar nuestro layout.
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(prompt)
        // Por ultimo, creamos el cuadro de dialogo y las acciones requeridas al aceptar o cancelar el prompt.
        // Mostramos el mensaje del cuadro de diálogo
        alertDialogBuilder.setNegativeButton("Cancelar") { dialog, _ ->
            // Cancelamos el cuadro de dialogo
            dialog.cancel()
        }

        alertDialogBuilder.setPositiveButton("Aceptar") { _, _ -> cargarCodPostal(edtCodPostal.text.toString()) }

        // Creamos un AlertDialog y lo mostramos
        val alertDialog = alertDialogBuilder.create()
        // El código que viene a continuación lo usamos para presentar el diálogo en la parte de la pantalla que queramos.
        val wmlp = alertDialog.window?.attributes
        wmlp?.gravity = Gravity.TOP
        wmlp?.y = 200   //y position
        alertDialog.show()
    }


    private fun cargarCodPostal(queCodPostal: String) {
        if (fRutero.abrirCodPostal(queCodPostal)) {
            fCodPostal = queCodPostal
            mostrarCodPostalActivo(fCodPostal)
            prepararRvRutero()
            fClteDoc = fRutero.lRutero[0].clienteId

        } else
            MsjAlerta(this@VentasActivity).alerta("El código postal no tiene clientes")
    }


}