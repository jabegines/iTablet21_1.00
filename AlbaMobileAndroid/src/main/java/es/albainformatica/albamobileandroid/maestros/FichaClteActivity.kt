package es.albainformatica.albamobileandroid.maestros

import android.Manifest.permission
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.cobros.FormasPagoClase
import es.albainformatica.albamobileandroid.dao.ContactosCltesDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.ContactosCltesEnt
import kotlinx.android.synthetic.main.tab_telefonos_clientes.*
import java.io.IOException
import java.util.ArrayList


class FichaClteActivity: AppCompatActivity() {
    private val telefonosDao: ContactosCltesDao? = MyDatabase.getInstance(this)?.contactosCltesDao()

    private var fCliente = 0
    private var fIdTelf = 0
    private  var fIdDir: Int = 0
    private var fSoloVer = false
    private lateinit var fClientes: ClientesClase
    private lateinit var fTarifas: Tarifas
    private lateinit var fFPago: FormasPagoClase
    private lateinit var fRutas: Rutas
    private var queFPago: String = ""
    private var queRuta: String = ""
    private lateinit var fConfiguracion: Configuracion
    private var fEstTelef: Byte = est_Telef_Browse
    private var fEstDirecc: Byte = est_Direcc_Browse
    private lateinit var adapterDir: SimpleCursorAdapter

    private lateinit var fRecyclerTlfs: RecyclerView
    private lateinit var fAdapterTlfs: TlfsClteRvAdapter
    private lateinit var fDataActual: ContactosCltesEnt

    private lateinit var edtCodigo: EditText
    private lateinit var edtNFiscal: EditText
    private lateinit var edtNComercial: EditText
    private lateinit var edtCIF: EditText
    private lateinit var edtDirecc: EditText
    private lateinit var edtPoblac: EditText
    private lateinit var edtCodP: EditText
    private lateinit var edtProvincia: EditText
    private lateinit var edtRiesgo: EditText
    private lateinit var edtSaldo: EditText
    private lateinit var chkAplIva: CheckBox
    private lateinit var chkAplRe: CheckBox

    private lateinit var imgNuevoTlf: ImageView
    private lateinit var imgEditTlf: ImageView
    private lateinit var imgBorrarTlf: ImageView
    private lateinit var imgNuevaDir: ImageView
    private lateinit var imgEditDir: ImageView
    private lateinit var imgBorrarDir: ImageView
    private lateinit var imgMapaDir: ImageView

    private var fShowBtFlotTlf = false
    private var fShowBtFlotDir = false

    // Request de las actividades a las que llamamos.
    private val fRequestEditarTlf = 1
    private val fRequestEditarDir = 2
    private val fRequestPermisoLlamar = 3


    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.ficha_clientes)
        // Recogemos el valor de fCliente, que nos ha sido enviado por la activity ClientesActivity
        val i = intent
        fCliente = i.getIntExtra("cliente", 0)
        fSoloVer = i.getBooleanExtra("solover", false)
        fClientes = ClientesClase(this)
        fTarifas = Tarifas(this)
        fFPago = FormasPagoClase(this)
        fRutas = Rutas(this)
        fConfiguracion = Comunicador.fConfiguracion

        inicializarControles()
    }


    override fun onDestroy() {
        fTarifas.close()
        fClientes.close()
        super.onDestroy()
    }


    private fun inicializarControles() {
        edtCodigo = findViewById<View>(R.id.edtCodClte) as EditText
        edtNFiscal = findViewById<View>(R.id.edtNFiscal) as EditText
        edtNComercial = findViewById<View>(R.id.edtNComercial) as EditText
        edtCIF = findViewById(R.id.edtCIF)
        edtDirecc = findViewById<View>(R.id.edtDireccion) as EditText
        edtPoblac = findViewById<View>(R.id.edtPoblacion) as EditText
        edtCodP = findViewById<View>(R.id.edtCodPostal) as EditText
        edtProvincia = findViewById<View>(R.id.edtProvincia) as EditText
        edtRiesgo = findViewById<View>(R.id.edtRiesgo) as EditText
        edtSaldo = findViewById<View>(R.id.edtSaldo) as EditText
        chkAplIva = findViewById<View>(R.id.chkAplIva) as CheckBox
        chkAplRe = findViewById<View>(R.id.chkAplRecargo) as CheckBox

        // Ponemos como no visibles los botones flotantes para edición de teléfonos.
        imgNuevoTlf = findViewById<View>(R.id.imvNuevoTlf) as ImageView
        imgEditTlf = findViewById<View>(R.id.imvEditarTlf) as ImageView
        imgBorrarTlf = findViewById<View>(R.id.imvBorrarTlf) as ImageView
        fShowBtFlotTlf = false
        botonesFlotantesTlf()

        // Idem con direcciones.
        imgNuevaDir = findViewById<View>(R.id.imvNuevaDir) as ImageView
        imgEditDir = findViewById<View>(R.id.imvEditarDir) as ImageView
        imgBorrarDir = findViewById<View>(R.id.imvBorrarDir) as ImageView
        imgMapaDir = findViewById<View>(R.id.imvMapaDir) as ImageView
        fShowBtFlotDir = false
        botonesFlotantesDir()
        prepararTabs()
        // Establecemos el ancho del código del cliente.
        editTextMaxLength(edtCodigo, ancho_codclte.toInt())
        if (fCliente > 0) mostrarFicha() else {
            fClientes.Abrir()
            edtCodigo.isEnabled = true
            edtCodigo.setText(fConfiguracion.getSiguCodClte())
            if (edtCodigo.text.toString() == "") edtCodigo.requestFocus() else edtNFiscal.requestFocus()
            // Establecemos edtNFiscal como siguiente control focusable, si no lo
            // hacemos el que tomará el foco será edtNComercial.
            edtCodigo.nextFocusDownId = R.id.edtNFiscal
        }
        // Los spinners los preparamos después de haber abierto fClientes, ya que usamos datos de este objeto.
        prepararSpinners()
    }

    fun cancelarClte(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        finish()
    }

    fun mapaDirPrincipal(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fClientes.getDireccion() != "") {
            buscarDireccion(
                fClientes.getDireccion() + ',' + fClientes.getCodPostal()
                        + ',' + fClientes.getPoblacion() + ',' + fClientes.getProvincia()
            )
        }
    }

    private fun buscarDireccion(queDireccion: String) {
        val geocoder = Geocoder(baseContext)
        var addresses: List<Address>? = null
        try {
            addresses = geocoder.getFromLocationName(queDireccion, 1)
        } catch (e: IOException) {
            Toast.makeText(baseContext, e.message, Toast.LENGTH_LONG).show()
        }
        if (addresses == null || addresses.isEmpty()) {
            Toast.makeText(baseContext, "No se encontró la dirección", Toast.LENGTH_SHORT).show()
        } else {
            val address = addresses[0]

            // Creamos una instancia del punto geográfico para mostrarlo en Google Maps.
            val latLng = LatLng(address.latitude, address.longitude)
            val cadenaIntent = ("geo:" + latLng.latitude + "," + latLng.longitude
                    + "?q=" + latLng.latitude + "," + latLng.longitude + "("
                    + fClientes.getDireccion() + ")")
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(cadenaIntent))
            startActivity(i)
        }
    }

    fun mapaDirContacto(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fIdDir > 0) {
            if (fClientes.getDir_Direccion() != "") {
                buscarDireccion(
                    fClientes.getDir_Direccion() + ','
                            + fClientes.getDir_CP() + ',' + fClientes.getDir_Poblac() + ','
                            + fClientes.getDir_Provincia()
                )
            }
        } else MsjAlerta(this).alerta("Tiene que seleccionar una dirección")
    }


    private fun prepararTabs() {
        val tabs = findViewById<View>(android.R.id.tabhost) as TabHost
        tabs.setup()
        tabs.tabWidget.isStripEnabled = false
        var spec = tabs.newTabSpec("1")
        spec.setContent(R.id.tab1Cltes)
        spec.setIndicator("General", null)
        tabs.addTab(spec)
        spec = tabs.newTabSpec("2")
        spec.setContent(R.id.tab2Cltes)
        spec.setIndicator("Varios", null)
        tabs.addTab(spec)
        spec = tabs.newTabSpec("3")
        spec.setContent(R.id.tab3Cltes)
        spec.setIndicator("Contactos", null)
        tabs.addTab(spec)
        spec = tabs.newTabSpec("4")
        spec.setContent(R.id.tab4Cltes)
        spec.setIndicator("Direcciones", null)
        tabs.addTab(spec)

        //Establecemos las propiedades de las pestañas
        for (i in 0 until tabs.tabWidget.childCount) {
            val tv = tabs.tabWidget.getChildAt(i).findViewById<View>(android.R.id.title) as TextView
            tv.textSize = 14f
            if (fConfiguracion.fTamanyoPantLargo) tabs.tabWidget.getChildAt(i).layoutParams.height =
                50 else tabs.tabWidget.getChildAt(i).layoutParams.height = 70
        }

        // Si estamos dando de alta no veremos las pestañas de teléfonos y direcciones.
        if (fCliente == 0) {
            tabs.tabWidget.getChildTabViewAt(2).visibility = View.GONE
            tabs.tabWidget.getChildTabViewAt(3).visibility = View.GONE
        }
        tabs.currentTab = 0

        // Cambiamos el background de cada pestaña a tab_background_selector.
        for (i in 0 until tabs.tabWidget.childCount) tabs.tabWidget.getChildAt(i)
            .setBackgroundResource(R.drawable.tab_background_selector)
    }

    private fun prepararSpinners() {
        prepararSpTarifas(false)
        prepararSpTarifas(true)
        prepararSpFPago()
        prepararSpRutas()
    }


    private fun prepararSpFPago() {
        val spnFPago = findViewById<View>(R.id.spnFPago) as Spinner
        fFPago.abrir()
        val c = fFPago.cursor
        val from = arrayOf("descripcion")
        val to = intArrayOf(android.R.id.text1)
        val sca = SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, c, from, to)
        sca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnFPago.adapter = sca

        // Hacemos un bucle para dejar seleccionada la forma de pago del cliente.
        if (fCliente > 0) {
            val fPagoClte = fClientes.getFPago()
            queFPago = fPagoClte
            for (i in 0 until spnFPago.count) {
                spnFPago.getItemAtPosition(i)
                val queCodigo: String = if (c != null) {
                    c.getString(c.getColumnIndexOrThrow("_id"))
                } else ""
                if (queCodigo == fPagoClte) {
                    spnFPago.setSelection(i)
                    break
                }
            }
            if (fSoloVer) spnFPago.isEnabled = false
        }
        spnFPago.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long
            ) {
                val queCursor = parent.getItemAtPosition(position) as Cursor
                queFPago = queCursor.getString(queCursor.getColumnIndexOrThrow("_id"))
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun prepararSpTarifas(deDescuento: Boolean) {

        // Usaremos un arrayAdapter para el spinner. El array lo llenamos en llenarTarifas().
        // fTarifas lo abrimos la primera vez que entramos en la función.
        if (!deDescuento) fTarifas.abrir()
        val aTarifas: Array<String> = llenarTarifas()
        val spnTarifa: Spinner = if (deDescuento) findViewById<View>(R.id.spnTarifaDto) as Spinner
                    else findViewById<View>(R.id.spnTarifaCltes) as Spinner

        val adaptador = ArrayAdapter(this, android.R.layout.simple_spinner_item, aTarifas)
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnTarifa.adapter = adaptador

        // Seleccionamos la tarifa que tenga el cliente, siempre que estemos editando.
        if (fCliente > 0) {
            if (deDescuento)
                spnTarifa.setSelection(getIndexTrf(spnTarifa, fClientes.getTarifaDto())
            ) else spnTarifa.setSelection(getIndexTrf(spnTarifa, fClientes.getTarifa()))
        }

        // Evento onClick.
        spnTarifa.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, v: View, position: Int, id: Long
            ) {
                // lblMensaje.setText("Seleccionado: " + datos[position]);
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        if (fSoloVer) spnTarifa.isEnabled = false
    }

    private fun prepararSpRutas() {
        val spnRutas = findViewById<View>(R.id.spnRutas) as Spinner
        val aRutas: Array<String> = fRutas.abrir()

        val adaptador = ArrayAdapter(this, android.R.layout.simple_spinner_item, aRutas)
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnRutas.adapter = adaptador

        // Seleccionamos la ruta del cliente
        if (fCliente > 0) {
            val fRutaClte = fClientes.getRuta()
            queRuta = fRutaClte
            spnRutas.setSelection(getIndexRuta(spnRutas, fRutaClte))
            if (fSoloVer) spnRutas.isEnabled = false
        }

        // Evento selección de un item
        spnRutas.onItemSelectedListener = object: OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long
            ) {
                queRuta = spnRutas.getItemAtPosition(position).toString().substring(0, 2).trim { it <= ' ' }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun getIndexRuta(s1: Spinner, nombre: String): Int {
        var index = 0
        for (i in 0 until s1.count) {
            val queCodigo = s1.getItemAtPosition(i).toString().substring(0, 2).trim { it <= ' ' }
            if (queCodigo == nombre) {
                index = i
            }
        }
        return index
    }

    private fun getIndexTrf(s1: Spinner, nombre: String): Int {
        var index = 0
        for (i in 0 until s1.count) {
            val queTarifa = s1.getItemAtPosition(i).toString().substring(0, 2).trim { it <= ' ' }
            if (queTarifa == nombre) {
                index = i
            }
        }
        return index
    }

    private fun llenarTarifas(): Array<String> {
        // Usaremos un ArrayList porque es dinámico. Luego lo convertimos en un String[].
        val sArrayList = ArrayList<String>()
        fTarifas.llenarArray(sArrayList)
        return sArrayList.toTypedArray()
    }

    private fun mostrarFicha() {
        if (fClientes.abrirUnCliente(fCliente)) {
            val tvNFiscal = findViewById<View>(R.id.tvNombreClte) as TextView
            val tvNComercial = findViewById<View>(R.id.tvNComClte) as TextView
            val tvVariosNFiscal = findViewById<View>(R.id.tvVariosNombreClte) as TextView
            val tvVariosNComercial = findViewById<View>(R.id.tvVariosNComClte) as TextView
            val tvTlfNFiscal = findViewById<View>(R.id.tvTlfNombreClte) as TextView
            val tvTlfNComercial = findViewById<View>(R.id.tvTlfNComClte) as TextView
            val tvDirNFiscal = findViewById<View>(R.id.tvDirNombreClte) as TextView
            val tvDirNComercial = findViewById<View>(R.id.tvDirNComClte) as TextView
            var queTexto = fClientes.getCodigo() + " - " + fClientes.getNFiscal()
            tvNFiscal.text = queTexto
            tvNComercial.text = fClientes.getNComercial()
            queTexto = fClientes.getCodigo() + " - " + fClientes.getNFiscal()
            tvVariosNFiscal.text = queTexto
            tvVariosNComercial.text = fClientes.getNComercial()
            queTexto = fClientes.getCodigo() + " - " + fClientes.getNFiscal()
            tvTlfNFiscal.text = queTexto
            tvTlfNComercial.text = fClientes.getNComercial()
            queTexto = fClientes.getCodigo() + " - " + fClientes.getNFiscal()
            tvDirNFiscal.text = queTexto
            tvDirNComercial.text = fClientes.getNComercial()
            edtCodigo.setText(fClientes.getCodigo())
            edtNFiscal.setText(fClientes.getNFiscal())
            edtNComercial.setText(fClientes.getNComercial())
            edtCIF.setText(fClientes.getCIF())
            edtDirecc.setText(fClientes.getDireccion())
            edtPoblac.setText(fClientes.getPoblacion())
            edtCodP.setText(fClientes.getCodPostal())
            edtProvincia.setText(fClientes.getProvincia())
            edtRiesgo.setText(String.format(fConfiguracion.formatoDecImptesIva(), fClientes.getRiesgo()))
            edtSaldo.setText(String.format(fConfiguracion.formatoDecImptesIva(), fClientes.getSaldo()))
            chkAplIva.isChecked = fClientes.getAplicarIva()
            chkAplRe.isChecked = fClientes.getAplicarRe()
            mostrarTelefonos()
            mostrarDirecciones()
            edtCodigo.isFocusable = false
            if (fSoloVer) {
                ocultarTeclado(this)
                edtNFiscal.isFocusable = false
                edtNComercial.isFocusable = false
                edtCIF.isFocusable = false
                edtDirecc.isFocusable = false
                edtPoblac.isFocusable = false
                edtCodP.isFocusable = false
                edtProvincia.isFocusable = false
                edtRiesgo.isFocusable = false
                edtSaldo.isFocusable = false
                chkAplIva.isFocusable = false
                chkAplIva.isEnabled = false
                chkAplRe.isFocusable = false
                chkAplRe.isEnabled = false
            } else  // Le damos el foco al nombre fiscal.
                edtNFiscal.requestFocus()
            if (fCliente > 0) {
                // Si estamos modificando un cliente nuevo permitiremos modificar el CIF.
                if (fClientes.getEstado() == "N") edtCIF.isFocusable = true else {
                    edtCIF.isEnabled = false
                }
            }
        }
    }

    private fun mostrarDirecciones() {
        val columns = arrayOf("direcc", "poblac")
        val to = intArrayOf(R.id.ly_direcc, R.id.ly_poblac)
        adapterDir = SimpleCursorAdapter(this, R.layout.layout_direcc_cltes, fClientes.cDirecciones, columns, to, 0)
        val listViewDir = findViewById<View>(R.id.lvDirCltes) as ListView
        listViewDir.adapter = adapterDir
        mostrarDatosDirecc()
        // Ver comentarios en mostrarTelefonos().
        fIdDir = 0
        listViewDir.onItemClickListener =
            AdapterView.OnItemClickListener { adapter: AdapterView<*>, _: View?, position: Int, _: Long ->
                val cursor = adapter.getItemAtPosition(position) as Cursor
                fIdDir = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                mostrarDatosDirecc()
            }
    }


    private fun mostrarTelefonos() {

        fAdapterTlfs = TlfsClteRvAdapter(getContactos(), this, object: TlfsClteRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: ContactosCltesEnt) {
                fIdTelf = data.contactoClteId
                fDataActual = data
                mostrarDatosTlf()
            }
        })

        fRecyclerTlfs = rvTlfCltes
        fRecyclerTlfs.layoutManager = LinearLayoutManager(this)
        fRecyclerTlfs.adapter = fAdapterTlfs
        fIdTelf = 0

        /*
        val columnas = arrayOf("contacto", "tel1", "tel2")
        val to = intArrayOf(R.id.ly_contacto, R.id.ly_telf1, R.id.ly_telf2)
        adapterTlf = SimpleCursorAdapter(this, R.layout.layout_telf_cltes, fClientes.cTelefonos, columnas, to, 0)
        val lvTelefonos = findViewById<View>(R.id.lvTlfCltes) as ListView
        lvTelefonos.adapter = adapterTlf
        // Mostramos los datos del primer contacto.
        mostrarDatosTlf()

        // Pongo fIdTelf a cero para estar seguro de que hemos pulsado sobre algún contacto antes de editarlo o borrarlo.
        // Si no hago esto y hago fIdTelf = fClientes.getIdTelf() he comprobado que, cuando intento editar, fClientes.getIdTelf() está
        // apuntando al último registro del cursor y, por lo tanto, no vale lo mismo que fIdTelf.
        fIdTelf = 0

        // Establecemos el evento on click del ListView.
        lvTelefonos.onItemClickListener =
            AdapterView.OnItemClickListener { adapter: AdapterView<*>, _: View?, position: Int, _: Long ->
                val cursor = adapter.getItemAtPosition(position) as Cursor
                // Tomamos el campo _id de la fila en la que hemos pulsado
                fIdTelf = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                mostrarDatosTlf()
            }
        */
    }

    private fun getContactos(): MutableList<ContactosCltesEnt> {
        return telefonosDao?.getTlfsCliente(fCliente) ?: emptyList<ContactosCltesEnt>().toMutableList()
    }

    private fun refrescarDirecc() {
        adapterDir.changeCursor(fClientes.cDirecciones)
        mostrarDatosDirecc()
        // Ver comentarios en mostrarTelefonos()
        fIdDir = 0
    }

    private fun refrescarTelef() {
        //adapterTlf.changeCursor(fClientes.cTelefonos)
        mostrarTelefonos()
        mostrarDatosTlf()
        // Ver comentarios en mostrarTelefonos()
        fIdTelf = 0
    }

    private fun mostrarDatosDirecc() {
        val edtDirDirecc = findViewById<View>(R.id.edtDir_Direcc) as EditText
        val edtDirPoblac = findViewById<View>(R.id.edtDir_Poblacion) as EditText
        val edtDirCP = findViewById<View>(R.id.edtDir_CPostal) as EditText
        val edtDirProv = findViewById<View>(R.id.edtDir_Provincia) as EditText
        edtDirDirecc.setText(fClientes.getDir_Direccion())
        edtDirPoblac.setText(fClientes.getDir_Poblac())
        edtDirCP.setText(fClientes.getDir_CP())
        edtDirProv.setText(fClientes.getDir_Provincia())
    }

    private fun mostrarDatosTlf() {
        val edtContacto = findViewById<View>(R.id.edtContacto) as EditText
        val edtTlf1 = findViewById<View>(R.id.edtTelf1) as EditText
        val edtTlf2 = findViewById<View>(R.id.edtTelf2) as EditText
        val edtEmail = findViewById<View>(R.id.edtTlfEmail) as EditText
        val edtObs = findViewById<View>(R.id.edtObsTlf) as EditText

        edtContacto.setText(fDataActual.nombre)
        edtTlf1.setText(fDataActual.telefono1)
        edtTlf2.setText(fDataActual.telefono2)
        edtEmail.setText(fDataActual.eMail)
        edtObs.setText(fDataActual.obs1)
    }

    fun salvarDatos(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (puedoSalvar()) {
            val spnTarifa = findViewById<View>(R.id.spnTarifaCltes) as Spinner
            val spnTrfDto = findViewById<View>(R.id.spnTarifaDto) as Spinner
            val aDatosClte = ArrayList<String>()
            aDatosClte.add(fCliente.toString())
            aDatosClte.add(ponerCeros(edtCodigo.text.toString(), ancho_codclte))
            aDatosClte.add(edtNFiscal.text.toString())
            aDatosClte.add(edtNComercial.text.toString())
            aDatosClte.add(edtCIF.text.toString())
            aDatosClte.add(edtDirecc.text.toString())
            aDatosClte.add(edtPoblac.text.toString())
            aDatosClte.add(edtCodP.text.toString())
            aDatosClte.add(edtProvincia.text.toString())
            if (spnTarifa.selectedItemId == 0L) aDatosClte.add("")
            else aDatosClte.add(spnTarifa.selectedItem.toString().substring(0, 2).trim { it <= ' ' })
            if (spnTrfDto.selectedItemId == 0L) aDatosClte.add("")
            else aDatosClte.add(spnTrfDto.selectedItem.toString().substring(0, 2).trim { it <= ' ' })
            aDatosClte.add(queFPago)
            aDatosClte.add(queRuta)
            aDatosClte.add(edtRiesgo.text.toString())
            if (chkAplIva.isChecked) aDatosClte.add("T") else aDatosClte.add("F")
            if (chkAplRe.isChecked) aDatosClte.add("T") else aDatosClte.add("F")
            if (fCliente == 0) aDatosClte.add("N")
            else {
                // Si estamos modificando un cliente nuevo, mantenemos el estado a 'N' para que la gestión sepa que es un nuevo cliente.
                if (fClientes.getEstado() == "N") aDatosClte.add("N") else aDatosClte.add("M")
            }
            fClientes.aceptarCambios(aDatosClte, fCliente == 0)

            // Si estamos insertando actualizamos el contador.
            if (fCliente == 0) fConfiguracion.setSiguCodClte(aDatosClte[1].toInt())
            val returnIntent = Intent()
            setResult(RESULT_OK, returnIntent)
            finish()
        }
    }


    private fun puedoSalvar(): Boolean {
        if (edtCodigo.text.toString() == "") {
            MsjAlerta(this).alerta("Tiene que indicar un código")
            return false
        } else {
            // Si fCliente = 0 es que estamos dando de alta.
            if (fCliente <= 0) {
                val queCodigo = ponerCeros(edtCodigo.text.toString(), ancho_codclte)
                if (fClientes.existeCodigo(queCodigo) > 0) {
                    MsjAlerta(this).alerta(resources.getString(R.string.msj_CodYaExiste))
                    return false
                }
            }
        }
        if (edtNFiscal.text.toString() == "") {
            MsjAlerta(this).alerta("Tiene que indicar un nombre")
            return false
        }
        return true
    }

    fun editarTelf(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (!fSoloVer) {
            if (fEstTelef == est_Telef_Browse) {
                if (fIdTelf > 0) {
                    fEstTelef = est_Telef_Editar
                    fShowBtFlotTlf = false
                    botonesFlotantesTlf()
                    val i = Intent(this, EditarTlfClte::class.java)
                    i.putExtra("nuevo", false)
                    i.putExtra("contacto", fDataActual.nombre)
                    i.putExtra("telefono1", fDataActual.telefono1)
                    i.putExtra("telefono2", fDataActual.telefono2)
                    i.putExtra("email", fDataActual.eMail)
                    i.putExtra("observ", fDataActual.obs1)
                    startActivityForResult(i, fRequestEditarTlf)
                } else MsjAlerta(this).alerta("Tiene que seleccionar un contacto")
            }
        }
    }

    fun editarDir(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (!fSoloVer) {
            if (fEstDirecc == est_Direcc_Browse && fClientes.cDirecciones.count > 0) {
                if (fIdDir > 0) {
                    fEstDirecc = est_Direc_Editar
                    fShowBtFlotDir = false
                    botonesFlotantesDir()
                    val i = Intent(this, EditarDirClte::class.java)
                    i.putExtra("nuevo", false)
                    i.putExtra("direccion", fClientes.getDir_Direccion())
                    i.putExtra("poblacion", fClientes.getDir_Poblac())
                    i.putExtra("codpostal", fClientes.getDir_CP())
                    i.putExtra("provincia", fClientes.getDir_Provincia())
                    startActivityForResult(i, fRequestEditarDir)
                } else MsjAlerta(this).alerta("Tiene que seleccionar una dirección")
            }
        }
    }

    fun nuevoTelf(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (!fSoloVer) {
            if (fEstTelef == est_Telef_Browse) {
                fEstTelef = est_Telef_Nuevo
                fShowBtFlotTlf = false
                botonesFlotantesTlf()
                val i = Intent(this, EditarTlfClte::class.java)
                i.putExtra("nuevo", true)
                startActivityForResult(i, fRequestEditarTlf)
            }
        }
    }

    fun nuevaDir(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (!fSoloVer) {
            if (fEstDirecc == est_Direcc_Browse) {
                fEstDirecc = est_Direc_Nueva
                fShowBtFlotDir = false
                botonesFlotantesDir()
                val i = Intent(this, EditarDirClte::class.java)
                i.putExtra("nuevo", true)
                startActivityForResult(i, fRequestEditarDir)
            }
        }
    }

    fun borrarTelf(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (!fSoloVer) {
            if (fEstTelef == est_Telef_Browse) {
                if (fIdTelf > 0) {
                    fShowBtFlotTlf = false
                    botonesFlotantesTlf()
                    val aldDialog = NuevoAlertBuilder(this, "Borrar teléfono", "¿Está seguro de borrar?", true)
                    aldDialog.setPositiveButton("Si") { _: DialogInterface?, _: Int ->
                        fClientes.borrarTelf(fIdTelf.toString())
                        refrescarTelef()
                    }
                    val alert = aldDialog.create()
                    alert.show()
                    ColorDividerAlert(this, alert)
                } else MsjAlerta(this).alerta("Tiene que seleccionar un contacto")
            }
        }
    }


    fun borrarDir(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (!fSoloVer) {
            if (fEstTelef == est_Direcc_Browse && fClientes.cDirecciones.count > 0) {
                if (fIdDir > 0) {
                    fShowBtFlotDir = false
                    botonesFlotantesDir()
                    val aldDialog = NuevoAlertBuilder(this, "Borrar dirección", "¿Está seguro de borrar?", true)
                    aldDialog.setPositiveButton("Si") { _: DialogInterface?, _: Int ->
                        fClientes.borrarDirecc(fIdDir.toString())
                        refrescarDirecc()
                    }
                    val alert = aldDialog.create()
                    alert.show()
                    ColorDividerAlert(this, alert)
                } else MsjAlerta(this).alerta("Tiene que seleccionar una dirección")
            }
        }
    }

    private fun cancelarTelf() {
        fEstTelef = est_Telef_Browse
        mostrarDatosTlf()
        // Ver comentarios en mostrarTelefonos()
        fIdTelf = 0
    }

    private fun cancelarDir() {
        fEstDirecc = est_Direcc_Browse
        mostrarDatosDirecc()
        // Ver comentarios en mostrarTelefonos()
        fIdDir = 0
    }

    fun llamarTelf1(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        try {
            val permissionCheck = ContextCompat.checkSelfPermission(applicationContext, permission.CALL_PHONE)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                val callIntent = Intent(Intent.ACTION_CALL)
                callIntent.data = Uri.parse("tel:" + fDataActual.telefono1)
                startActivity(callIntent)
            } else {
                // Aunque tengamos la constante REQUEST_PERMISO_LLAMAR, por ahora no la usamos
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission.CALL_PHONE), fRequestPermisoLlamar
                )
            }
        } catch (activityException: ActivityNotFoundException) {
            MsjAlerta(this).alerta(activityException.toString())
        }
    }

    fun llamarTelf2(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        try {
            val permissionCheck =
                ContextCompat.checkSelfPermission(applicationContext, permission.CALL_PHONE)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                val callIntent = Intent(Intent.ACTION_CALL)
                callIntent.data = Uri.parse("tel:" + fDataActual.telefono2)
                startActivity(callIntent)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission.CALL_PHONE), fRequestPermisoLlamar
                )
            }
        } catch (activityException: ActivityNotFoundException) {
            MsjAlerta(this).alerta(activityException.toString())
        }
    }

    private fun aceptarTelf(aDatosTelf: ArrayList<String>) {
        if (fEstTelef == est_Telef_Nuevo || fEstTelef == est_Telef_Editar) {
            if (aDatosTelf[3] != "") {
                fClientes.aceptarCambTelf(aDatosTelf, fEstTelef == est_Telef_Nuevo)
                refrescarTelef()
                fEstTelef = est_Telef_Browse
            } else MsjAlerta(this).alerta("Tiene que indicar algún contacto")
        }
    }

    private fun aceptarDir(aDatosDirecc: ArrayList<String>) {
        if (fEstDirecc == est_Direc_Nueva || fEstDirecc == est_Direc_Editar) {
            if (aDatosDirecc[3] != "") {
                fClientes.aceptarCambDirec(aDatosDirecc, fEstDirecc == est_Direc_Nueva)
                refrescarDirecc()
                fEstDirecc = est_Direcc_Browse
            } else MsjAlerta(this).alerta("Tiene que indicar alguna dirección")
        }
    }

    fun menuTelefonos(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (!fSoloVer) {
            fShowBtFlotTlf = !fShowBtFlotTlf
            botonesFlotantesTlf()
        }
    }

    fun menuDirecciones(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (!fSoloVer) {
            fShowBtFlotDir = !fShowBtFlotDir
            botonesFlotantesDir()
        }
    }

    private fun botonesFlotantesTlf() {
        if (fShowBtFlotTlf) {
            imgNuevoTlf.visibility = View.VISIBLE
            imgEditTlf.visibility = View.VISIBLE
            imgBorrarTlf.visibility = View.VISIBLE
        } else {
            imgNuevoTlf.visibility = View.GONE
            imgEditTlf.visibility = View.GONE
            imgBorrarTlf.visibility = View.GONE
        }
    }

    private fun botonesFlotantesDir() {
        if (fShowBtFlotDir) {
            imgNuevaDir.visibility = View.VISIBLE
            imgEditDir.visibility = View.VISIBLE
            imgBorrarDir.visibility = View.VISIBLE
            imgMapaDir.visibility = View.VISIBLE
        } else {
            imgNuevaDir.visibility = View.GONE
            imgEditDir.visibility = View.GONE
            imgBorrarDir.visibility = View.GONE
            imgMapaDir.visibility = View.GONE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Actividad editar historico.
        if (requestCode == fRequestEditarTlf) {
            if (resultCode == RESULT_OK) {
                val aDatosTelf = ArrayList<String>(8)
                aDatosTelf.add(fIdTelf.toString())
                aDatosTelf.add(fCliente.toString())
                aDatosTelf.add(fConfiguracion.almacen().toString())
                aDatosTelf.add(data?.getStringExtra("contacto") ?: "")
                aDatosTelf.add(data?.getStringExtra("telefono1") ?: "")
                aDatosTelf.add(data?.getStringExtra("telefono2") ?: "")
                aDatosTelf.add(data?.getStringExtra("email") ?: "")
                aDatosTelf.add(data?.getStringExtra("observ") ?: "")
                aceptarTelf(aDatosTelf)
                refrescarTelef()
            } else cancelarTelf()
        } else if (requestCode == fRequestEditarDir) {
            if (resultCode == RESULT_OK) {
                val aDatosDirecc = ArrayList<String>(7)
                aDatosDirecc.add(fIdDir.toString())
                aDatosDirecc.add(fCliente.toString())
                aDatosDirecc.add(fConfiguracion.almacen().toString())
                aDatosDirecc.add(data?.getStringExtra("direccion") ?: "")
                aDatosDirecc.add(data?.getStringExtra("poblacion") ?: "")
                aDatosDirecc.add(data?.getStringExtra("codpostal") ?: "")
                aDatosDirecc.add(data?.getStringExtra("provincia") ?: "")
                aceptarDir(aDatosDirecc)
                refrescarDirecc()
            } else cancelarDir()
        }
    }


}