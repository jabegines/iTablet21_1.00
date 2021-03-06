package es.albainformatica.albamobileandroid.actividades

import android.annotation.SuppressLint
import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import androidx.preference.PreferenceManager
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.cargas.VerCargas
import es.albainformatica.albamobileandroid.cobros.CobrosActivity
import es.albainformatica.albamobileandroid.comunicaciones.*
import es.albainformatica.albamobileandroid.dao.EmpresasDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.database.MyDatabase.Companion.queBDRoom
import es.albainformatica.albamobileandroid.impresion_informes.*
import es.albainformatica.albamobileandroid.maestros.ArticulosActivity
import es.albainformatica.albamobileandroid.maestros.ClientesActivity
import es.albainformatica.albamobileandroid.maestros.ElegirEmpresaActivity
import es.albainformatica.albamobileandroid.oldcatalogo.CatalogoCatalogos
import es.albainformatica.albamobileandroid.oldcatalogo.CatalogoGruposDep
import es.albainformatica.albamobileandroid.registroEventos.RegistroEventosClase
import es.albainformatica.albamobileandroid.reparto.DocsReparto
import es.albainformatica.albamobileandroid.ventas.Documento
import es.albainformatica.albamobileandroid.ventas.VentasActivity
import kotlinx.android.synthetic.main.main.*
import org.apache.commons.net.util.Base64
import org.jetbrains.anko.alert
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.*
import java.security.NoSuchAlgorithmException
import java.util.*


class Main: AppCompatActivity() {

    private lateinit var fConfiguracion: Configuracion
    private var prefs: SharedPreferences? = null
    private lateinit var fRegEventos: RegistroEventosClase

    private lateinit var chsBBDD: Array<CharSequence>
    private var fNumClicks: Short = 0
    private var fSistemaId: String = "00"
    private var fUsarMultisistema: Boolean = false
    private var fUsarServicio: Boolean = false
    private var fEmpresaActual: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Cada vez que hacemos visible la aplicaci??n, bien porque la arrancamos por primera vez bien porque
        // la volvemos a traer a primer plano, el c??digo pasa por el onCreate(). O sea, que se comporta siempre
        // como si fuera la primera vez que arrancamos la aplicaci??n.
        setContentView(R.layout.main)

        // Lo primero que hacemos es cargar las preferencias para ver si trabajamos con multisistema.
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        fUsarMultisistema = prefs?.getBoolean("usar_multisistema", false) ?: false
        imgHayPaquetes.visibility = View.GONE
        imgHayImagenes.visibility = View.GONE

        comprobarMultisistema()

        if (fUsarMultisistema) {
            val queBD = queBDRoom
            fSistemaId = queBD.substring(queBD.length - 5, queBD.length-3)
            fUsarServicio = prefs?.getBoolean(fSistemaId + "_usar_servicio", false) ?: false
        }
        else {
            fSistemaId = prefs?.getString("sistemaId_servicio", "00") ?: "00"
            fUsarServicio = prefs?.getBoolean("usar_servicio", false) ?: false
        }
        fSistemaId = Base64.encodeBase64String(fSistemaId.toByteArray()).replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_").replace("=", "*")

        // Vemos si tenemos que descargarnos una actualizaci??n de la apk.
        // Para ello comparamos la versi??n y compilaci??n que tenemos con las que nos da el servicio. Da igual que el cliente use
        // el servicio o no, tendremos a todos los clientes registrados.
        //if (fUsarServicio)
        comprobarActApk()

        comprobarLineasHuerfanas()
    }


    @SuppressLint("HardwareIds", "SimpleDateFormat")
    override fun onResume() {
        super.onResume()
        // Puede que hayamos cambiado de empresa en otra actividad
        mostrarEmpresaActual()

        // Si usamos el servicio comprobamos si tenemos algo pendiente
        if (fUsarServicio) {
            val miscServicio = MiscServicio(this)

            imgHayPaquetes.visibility = View.GONE
            imgHayImagenes.visibility = View.GONE

            doAsync {
                // Comprobamos si hay paquetes pendientes de descargar
                if (miscServicio.hayPaquetesParaTerminal()) {
                    uiThread {
                        imgHayPaquetes.visibility = View.VISIBLE
                    }
                }
            }

            // Comprobamos si hay im??genes pendientes de descargar.
            doAsync {
                if (miscServicio.hayImagenesParaTerminal()) {
                    uiThread {
                        imgHayImagenes.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        // No debo cerrar fConfiguraci??n (ya se encargar?? de ello el recolector de basura), porque he detectado que si salimos
        // muy r??pido de la aplicaci??n hay instrucciones que graban en fConfiguracion que a??n no se han ejecutado y, al hacer
        // el fConfiguracion.close(), estamos cerrando la base de datos y, por lo tanto, al intentar ejecutar dichas instrucciones el
        // programa nos dar?? un error. Un ejemplo de esto lo tenemos en la pantalla de Ventas.kt, que al cerrarla grabamos en fConfiguracion
        // la ??ltima ruta activa. Si salimos de la pantalla de Ventas y r??pidamente de la aplicaci??n, nos dar??a error. Para comprobarlo
        // s??lo hay que descomentar las dos l??neas que est??n m??s abajo.

        //if (fConfiguracion != null)
        //  fConfiguracion.close();


        //fRegEventos.registrarEvento(codEv_Salida_Sistema, descrEv_Salida_Sistema)
        super.onDestroy()
    }

    private fun comprobarActApk() {
        val miscServicio = MiscServicio(this)
        doAsync {
            val queVersion = miscServicio.getVersionApk()

            if (queVersion != "" && queVersion != VERSION_PROGRAMA + COMPILACION_PROGRAMA) {
                uiThread {
                    alert("Se ha detectado una nueva compilaci??n, ??actualizar?", "Nueva compilaci??n") {
                        positiveButton("S??") {
                            actualizarApkServicio(queVersion)
                        }
                        negativeButton("No") {}
                    }.show()
                }
            }
        }
    }

    fun recibirImagenes(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        val i = Intent(this, ServicioRecibir::class.java)
        i.putExtra("recibirImagenes", true)
        startActivity(i)
    }


    fun recibirPaquetes(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        val i = Intent(this, ServicioRecibir::class.java)
        i.putExtra("recibirPaquetes", true)
        startActivity(i)
    }



    /*
    private fun comprobarBD() {
        val queVersionBd = prefs?.getInt("version_bd", 1) ?: 1

        if (queVersionBd != Constantes.VERSION_BD) {

            if (Miscelan.puedoRecibir(this)) {
                alert("Se ha detectado una nueva versi??n de la base de datos.\n" +
                        "Tendr?? que realizar una nueva recogida porque se borrar??n todos los datos.\n" +
                        "Si no actualiza es probable que no pueda realizar algunas acciones. ??Actualizar?",
                        "Nueva versi??n de base de datos") {
                    positiveButton("S??") {
                        // Antes de crear la base de datos leemos la configuraci??n del ftp para restablecerla luego
                        leerConfFtp()
                        CrearBD(this@Main)
                        if (fServidorFtp != "")
                            guardarConfFtp()
                        prefs?.edit()?.putInt("version_bd", Constantes.VERSION_BD)?.apply()
                        lanzarAplicacion()
                    }
                    negativeButton("No") {}
                }.show()
            }
            else {
                alert("Se ha detectado una nueva versi??n de la base de datos,\n" +
                        "pero tiene datos pendientes de enviar.\n\n" +
                        "Por favor, envielos antes de actualizar.",
                        "Nueva versi??n de base de datos") {
                    positiveButton("Ok") { enviar() }
                }.show()
            }
        }
    }


    private fun leerConfFtp() {
        try {
            fServidorFtp = fConfiguracion.servidorFTP()
            fUsuarioFtp = fConfiguracion.usuarioFTP()
            fPasswordFtp = fConfiguracion.passwordFTP()
            fCarpImpFtp = fConfiguracion.carpetaImportFTP()
            fCarpExpFtp = fConfiguracion.carpetaExportFTP()

        } catch (e: java.lang.Exception) {
            fServidorFtp = ""
        }
    }

    private fun guardarConfFtp() {
        val bd = BaseDatos(this)
        val dbAlba = bd.writableDatabase

        val values = ContentValues()
        values.put("grupo", "151")
        values.put("descr", "Servidor")
        values.put("valor", fServidorFtp)
        dbAlba.insertWithOnConflict("configuracion", null, values, SQLiteDatabase.CONFLICT_IGNORE)

        values.put("grupo", "152")
        values.put("descr", "Usuario")
        values.put("valor", fUsuarioFtp)
        dbAlba.insertWithOnConflict("configuracion", null, values, SQLiteDatabase.CONFLICT_IGNORE)

        values.put("grupo", "153")
        values.put("descr", "Password")
        values.put("valor", fPasswordFtp)
        dbAlba.insertWithOnConflict("configuracion", null, values, SQLiteDatabase.CONFLICT_IGNORE)

        values.put("grupo", "154")
        values.put("descr", "Carpeta Imp.")
        values.put("valor", fCarpImpFtp)
        dbAlba.insertWithOnConflict("configuracion", null, values, SQLiteDatabase.CONFLICT_IGNORE)

        values.put("grupo", "155")
        values.put("descr", "Carpeta Exp.")
        values.put("valor", fCarpExpFtp)
        dbAlba.insertWithOnConflict("configuracion", null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }


    private fun lanzarAplicacion() {
        val intent = Intent(this, Main::class.java)
        startActivity(intent)
        finish()
    }
    */

    private fun comprobarLineasHuerfanas() {
        // Creamos un objeto Documento para realizar el rec??lculo de stock de posibles l??neas sin cabecera
        try {
            val fDocumento = Documento(this)
            fDocumento.comprobarLineasHuerfanas()
            fDocumento.close()

        } catch (ex: Exception) {
            // Puede ser que a??n no tengamos la base de datos creada
            ex.printStackTrace()
        }
    }


    private fun comprobarMultisistema() {
        if (fUsarMultisistema)
            elegirBaseDatos()
        else {
            iniciarAplicacion()
        }
    }


    private fun iniciarAplicacion() {
        try {
            if (fUsarServicio) {
                // Comprobamos si existe la base de datos
                val existeBaseDatos = (MyDatabase.getInstance(this) != null)

                if (!existeBaseDatos)
                    MyDatabase.getInstance(this)
            }

            fConfiguracion = Configuracion(this)
            // Tendremos fConfiguracion visible durante toda la aplicaci??n
            Comunicador.fConfiguracion = fConfiguracion

            inicializarControles()
            // Si el usuario tiene clave, la pedimos.
            if (fConfiguracion.claveUsuario() != "")
                pedirClaveUsuario()

        } catch (e: Exception) {
            // De todas formas, activamos la toolbar para poder mostrar el men??
            val toolbar = findViewById<Toolbar>(R.id.tlbAlba)
            setSupportActionBar(toolbar)

            // Si llegamos hasta aqu?? es que no existe la base de datos, por eso la creamos (si usamos el servicio)
            if (fUsarServicio)
                MyDatabase.getInstance(this)

            Toast.makeText(this, resources.getString(R.string.msj_AlgunProblema), Toast.LENGTH_LONG).show()
        }

        fRegEventos = RegistroEventosClase(this)
        // Tendremos fRegEventos visible durante toda la aplicaci??n
        Comunicador.fRegEventos = fRegEventos
        fRegEventos.registrarEvento(codEv_Entrada_Sistema, descrEv_Entrada_Sistema)
    }


    private fun elegirBaseDatos() {
        // Nos aseguramos de que tendremos un nombre de base de datos v??lido, por si cancelamos el di??logo.
        queBDRoom = "ibsTablet00.db"

        val listItems = ArrayList<String>()
        bdALista(listItems)
        chsBBDD = listItems.toTypedArray()

        val altBld = AlertDialog.Builder(this)
        altBld.setTitle("Escoger base de datos")

        // Si s??lo tenemos una base de datos seleccionada no hace falta que preguntemos.
        if (listItems.size == 1) {
            queBDRoom = dimeNombreBDRoom(chsBBDD[0].toString())
            iniciarAplicacion()

        } else {
            altBld.setSingleChoiceItems(chsBBDD, 0) { dialog, item ->
                MyDatabase.INSTANCE = null
                queBDRoom = dimeNombreBDRoom(chsBBDD[item].toString())
                dialog.dismiss()
                iniciarAplicacion()
            }
            // Nos aseguramos de que la aplicacion pasara por iniciarAplicacion() aunque cancelemos el dialogo.
            altBld.setOnCancelListener { iniciarAplicacion() }
            val alert = altBld.create()
            alert.show()
        }
    }

    private fun dimeNombreBDRoom(queItem: String): String {
        return when (queItem) {
            getString(R.string.TituloBD_0) -> "ibsTablet00.db"
            getString(R.string.TituloBD_1) -> "ibsTablet10.db"
            getString(R.string.TituloBD_2) -> "ibsTablet20.db"
            getString(R.string.TituloBD_3) -> "ibsTablet30.db"
            getString(R.string.TituloBD_4) -> "ibsTablet40.db"
            getString(R.string.TituloBD_5) -> "ibsTablet50.db"
            getString(R.string.TituloBD_6) -> "ibsTablet60.db"
            getString(R.string.TituloBD_7) -> "ibsTablet70.db"
            getString(R.string.TituloBD_8) -> "ibsTablet80.db"
            getString(R.string.TituloBD_9) -> "ibsTablet90.db"
            else -> "ibsTablet00.db"
        }
    }


    private fun bdALista(listItems: MutableList<String>) {
        // Vemos en preferencias los sistemas que queremos usar.
        try {
            if (prefs?.getBoolean("usarBD00", false) == true) listItems.add(getString(R.string.TituloBD_0))
            if (prefs?.getBoolean("usarBD10", false) == true) listItems.add(getString(R.string.TituloBD_1))
            if (prefs?.getBoolean("usarBD20", false) == true) listItems.add(getString(R.string.TituloBD_2))
            if (prefs?.getBoolean("usarBD30", false) == true) listItems.add(getString(R.string.TituloBD_3))
            if (prefs?.getBoolean("usarBD40", false) == true) listItems.add(getString(R.string.TituloBD_4))
            if (prefs?.getBoolean("usarBD50", false) == true) listItems.add(getString(R.string.TituloBD_5))
            if (prefs?.getBoolean("usarBD60", false) == true) listItems.add(getString(R.string.TituloBD_6))
            if (prefs?.getBoolean("usarBD70", false) == true) listItems.add(getString(R.string.TituloBD_7))
            if (prefs?.getBoolean("usarBD80", false) == true) listItems.add(getString(R.string.TituloBD_8))
            if (prefs?.getBoolean("usarBD90", false) == true) listItems.add(getString(R.string.TituloBD_9))

        } catch (E: ClassCastException) {
            //
        }
    }


    private fun inicializarControles() {
        val tvAlmacen = findViewById<View>(R.id.tvMainAlmacen) as TextView
        val tvVendedor = findViewById<View>(R.id.tvMainVendedor) as TextView
        val tvTerminal = findViewById<View>(R.id.tvMainNombreTerm) as TextView
        val tvVersion = findViewById<View>(R.id.tvMainVersion) as TextView
        val sVersion = resources.getString(R.string.version) + " " + VERSION_PROGRAMA + COMPILACION_PROGRAMA + "  VC: " + VERSION_COMUNICACION
        tvVersion.text = sVersion
        val tvVentas = findViewById<View>(R.id.tvVentas) as TextView

        if (fConfiguracion.hayReparto()) {
            tvVentas.text = resources.getString(R.string.reparto)
        } else {
            tvVentas.text = resources.getString(R.string.ventas)
        }

        val cal = Calendar.getInstance()
        val queMes = cal.get(Calendar.MONTH)
        val queDia = cal.get(Calendar.DAY_OF_MONTH).toString()
        val queDiaSemana = dimeDiaSemana(cal.get(Calendar.DAY_OF_WEEK))
        val queNombreMes = dimeNombreMes(queMes)

        val tvDiaNombre = findViewById<View>(R.id.tvMainDiaNombre) as TextView
        val tvDiaNum = findViewById<View>(R.id.tvMainDiaNum) as TextView
        val tvMesNombre = findViewById<View>(R.id.tvMainMesNombre) as TextView
        tvDiaNombre.text = queDiaSemana
        tvDiaNum.text = queDia
        tvMesNombre.text = queNombreMes

        // Comprobamos el tama??o de la pantalla, para no presentar el cat??logo en terminales con menos de 7".
        fConfiguracion.fTamanyoPantLargo = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE

        tvAlmacen.text = fConfiguracion.nombreAlmacen()
        val sVendedor = fConfiguracion.vendedor() + " " + fConfiguracion.nombreVendedor()
        tvVendedor.text = sVendedor
        val sTerminal = fConfiguracion.codTerminal() + " " + fConfiguracion.nombreTerminal()
        tvTerminal.text = sTerminal
        // Obtenemos una instancia de la clase Miscelan y le asignamos el c??digo del terminal, para poder acceder a ??l
        // desde cualquier parte de la aplicaci??n.
        //val m = Miscelan.getInstancia()
        //m.codTerminal = fConfiguracion.codTerminal()

        fNumClicks = 0

        val toolbar = findViewById<Toolbar>(R.id.tlbAlba)
        setSupportActionBar(toolbar)
        mostrarEmpresaActual()
    }

    private fun mostrarEmpresaActual() {
        val empresasDao: EmpresasDao? = MyDatabase.getInstance(this)?.empresasDao()
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)

        fEmpresaActual = prefs?.getInt("ultima_empresa", 0) ?: -1
        if (fEmpresaActual > -1)
            tvTitulo.text = empresasDao?.getNombreEmpresa(fEmpresaActual) ?: "Sin empresa actual"
        else {
            fEmpresaActual = empresasDao?.getCodigoEmpresa() ?: 0
            prefs?.edit()?.putInt("ultima_empresa", fEmpresaActual)?.apply()
            tvTitulo.text = empresasDao?.getNombreEmpresa(fEmpresaActual) ?: "Sin empresa actual"
        }


        tvTitulo.setOnClickListener {
            val intent = Intent(this, ElegirEmpresaActivity::class.java)
            resultElegirEmpresa.launch(intent)
        }
    }


    /*
    private fun comprobarFechas() {
        try {
            // Comparo la fecha actual con las de inicio y fin del ejercicio.
            val fechaActual = Date()
            val fechaInicio = fConfiguracion.fechaInicio()
            val fechaFin = fConfiguracion.fechaFin()

            if (fechaActual < fechaInicio || fechaActual > fechaFin) {
                fFechaCorrecta = false
                MsjAlerta(this).alerta(resources.getString(R.string.msj_FechaRango))
            } else
                fFechaCorrecta = true
        } catch (e: Exception) {
            MsjAlerta(this).alerta(resources.getString(R.string.msj_SinFechaIF))
            fFechaCorrecta = false
        }
    }
    */


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_configuracion, menu)

        // Configuramos cada uno de los items del men??, d??ndole un color personalizado (azul de la aplicaci??n).
        val text1 = SpannableStringBuilder()
        text1.append(resources.getString(R.string.mni_cargas))
        text1.setSpan(ForegroundColorSpan(Color.parseColor(COLOR_MENUS)), 0, text1.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        var item = menu.findItem(R.id.mni_cargas)
        item.title = text1

        try {
            item.isVisible = fConfiguracion.usarCargas()
        } catch (ex: Exception) {
            // Puede ser que a??n no tengamos la base de datos creada
            ex.printStackTrace()
        }

        val text2 = SpannableStringBuilder()
        text2.append(resources.getString(R.string.mni_informes))
        text2.setSpan(ForegroundColorSpan(Color.parseColor(COLOR_MENUS)), 0, text2.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        item = menu.findItem(R.id.mni_informes)
        item.title = text2

        val text3 = SpannableStringBuilder()
        text3.append(resources.getString(R.string.mni_configuracion))
        text3.setSpan(ForegroundColorSpan(Color.parseColor(COLOR_MENUS)), 0, text3.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        item = menu.findItem(R.id.mni_configuracion)
        item.title = text3

        val text4 = SpannableStringBuilder()
        text4.append(resources.getString(R.string.mni_confimpr))
        text4.setSpan(ForegroundColorSpan(Color.parseColor(COLOR_MENUS)), 0, text4.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        item = menu.findItem(R.id.mni_confimpresora)
        item.title = text4

        val text6 = SpannableStringBuilder()
        text6.append(resources.getString(R.string.mni_verID))
        text6.setSpan(ForegroundColorSpan(Color.parseColor(COLOR_MENUS)), 0, text6.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        item = menu.findItem(R.id.mni_verID)
        item.title = text6

        val text7 = SpannableStringBuilder()
        text7.append(resources.getString(R.string.mni_hacerBackup))
        text7.setSpan(ForegroundColorSpan(Color.parseColor(COLOR_MENUS)), 0, text7.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        item = menu.findItem(R.id.mni_hacerBackup)
        item.title = text7

        return true
    }



    @SuppressLint("HardwareIds")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val aldDialog: AlertDialog.Builder
        val alert: AlertDialog

        val i: Intent
        when (item.itemId) {
            R.id.mni_cargas -> {
                i = Intent(this, VerCargas::class.java)
                startActivity(i)
                return true
            }

            R.id.mni_configuracion -> {
                i = Intent(this, NewPrefs::class.java)
                startActivity(i)
                return true
            }

            R.id.mni_confimpresora -> {
                i = Intent(this, BuscarBluetooth::class.java)
                startActivity(i)
                return true
            }

            R.id.mni_verID -> {
                val fDispositivoId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
                MsjAlerta(this).informacion(fDispositivoId)
                return true
            }

            R.id.mni_infStock -> {
                aldDialog = nuevoAlertBuilder(this, "Informe", "??Emitir informe de stock?", true)

                aldDialog.setPositiveButton("S??") { _, _ ->
                    val infStock = InfStock(this@Main)
                    infStock.imprimir()
                }
                alert = aldDialog.create()
                alert.show()

                return true
            }

            R.id.mni_infDoc -> {

                val builder = AlertDialog.Builder(this)
                builder.setTitle(HtmlCompat.fromHtml("<font color='#000000'>Introducir fechas</font>", HtmlCompat.FROM_HTML_MODE_LEGACY))
                val inflater = layoutInflater
                val dialogLayout = inflater.inflate(R.layout.alert_dialog_pedir_fechas, null)
                builder.setView(dialogLayout)
                val edtDesdeFecha = dialogLayout.findViewById<EditText>(R.id.edtDesdeFecha)
                val edtHastaFecha = dialogLayout.findViewById<EditText>(R.id.edtHastaFecha)
                edtDesdeFecha.setOnClickListener { showDatePickerDialog(edtDesdeFecha) }
                edtHastaFecha.setOnClickListener { showDatePickerDialog(edtHastaFecha) }

                builder.setPositiveButton("Aceptar") { _, _ ->
                    val infDocumentos = InfDocumentos(this@Main)
                    val desdeFecha = edtDesdeFecha.text.toString()
                    val hastaFecha = edtHastaFecha.text.toString()

                    infDocumentos.imprimir(desdeFecha, hastaFecha)
                }
                builder.setNegativeButton("Cancelar") { _, _ -> }

                builder.show()
                return true
            }

            R.id.mni_resPedidos -> {
                aldDialog = nuevoAlertBuilder(this, "Informe", "??Emitir resumen de pedidos?", true)

                aldDialog.setPositiveButton("S??") { _, _ ->
                    val resPedidos = ResumenPedidos(this@Main)
                    resPedidos.crearResumen()
                    resPedidos.enviarPorEmail()
                    Toast.makeText(this@Main, getString(R.string.tst_envinf), Toast.LENGTH_LONG).show()
                }
                alert = aldDialog.create()
                alert.show()

                return true
            }

            R.id.mni_vtasRepre -> {
                i = Intent(this, GrafVtasRepre::class.java)
                startActivity(i)
                return true
            }

            R.id.mni_hacerBackup -> {
                bdBackup()
                return true
            }

            else -> return true
        }
    }

    private fun showDatePickerDialog(editText: EditText) {
        val newFragment = DatePickerFragment.newInstance { _, year, month, day ->
            val selectedDate = ponerCeros(day.toString(), 2) +
                    "/" + ponerCeros((month + 1).toString(), 2) + "/" + year
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


    private fun actualizarApkServicio(queVersion: String) {
        val i = Intent(this, ActApkServicio::class.java)
        i.putExtra("versionApk", queVersion)
        startActivity(i)
    }

    /*
    private fun lanzarActualizar() {
        val i = Intent(this, ActualizarApk::class.java)
        startActivity(i)
    }
    */


    fun lanzarArticulos(view: View) {
        view.getTag(0)          // Para que no d?? warning el compilador

        fNumClicks = 0

        // Si el tama??o de la pantalla es menor de 7" no presentaremos el cat??logo.
        // Tampoco si tenemos en la configuraci??n 'Usar piezas' y 'Usar formatos' (ambas configuraciones a 'T')
        if (fConfiguracion.fTamanyoPantLargo && (!fConfiguracion.usarPiezas() || !fConfiguracion.usarFormatos())) {
            // Vemos como entramos la ??ltima vez en art??culos.
            when (prefs?.getInt("modoVisArtic", LISTA_ARTICULOS)) {
                LISTA_ARTICULOS -> lanzarListaArticulos()

                GRUPOS_Y_DEP -> lanzarCatalGrupDep()

                CATALOGOS -> lanzarCatalogos()

                // Ya que el modo hist??rico s??lo tiene sentido desde ventas, si el ??ltimo modo de visualizaci??n de art??culos
                // fue el hist??rico, cuando intentemos entrar en art??culos desde Main, lo haremos en forma de LISTA_ARTICULOS.
                HISTORICO -> lanzarListaArticulos()
            }
        } else {
            lanzarListaArticulos()
        }
    }


    private fun lanzarListaArticulos() {
        val intent = Intent(this, ArticulosActivity::class.java)
        resultListaArticulos.launch(intent)
    }

    private fun lanzarCatalGrupDep() {
        val intent = Intent(this, CatalogoGruposDep::class.java)
        resultCatalGrupDep.launch(intent)
    }


    private fun lanzarCatalogos() {
        val intent = Intent(this, CatalogoCatalogos::class.java)
        resultCatalogos.launch(intent)
    }


    fun lanzarClientes(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        fNumClicks = 0
        val i = Intent(this, ClientesActivity::class.java)
        startActivity(i)
    }


    fun lanzarVentas(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador
        fNumClicks = 0

        if (fConfiguracion.hayReparto()) {
            val i = Intent(this, DocsReparto::class.java)
            startActivity(i)
        } else {
            val i = Intent(this, VentasActivity::class.java)
            startActivity(i)
        }
    }


    fun lanzarEnviar(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador
        fNumClicks = 0
        enviar()
    }

    private fun enviar() {
        if (fUsarServicio) {
            val i = Intent(this, ServicioEnviar::class.java)
            startActivity(i)
        } else {
            val i = Intent(this, Enviar::class.java)
            startActivity(i)
        }
    }


    fun lanzarRecibir(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        fNumClicks = 0
        if (fUsarServicio) {
            val i = Intent(this, ServicioRecibir::class.java)
            startActivity(i)
        } else {
            val i = Intent(this, Recibir::class.java)
            startActivity(i)
        }
    }


    fun lanzarCobros(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        fNumClicks = 0
        val i = Intent(this, CobrosActivity::class.java)
        startActivity(i)
    }


    private fun pedirClaveUsuario() {
        val intent = Intent(this, PedirPassword::class.java)
        resultClaveUsuario.launch(intent)
    }


    private var resultElegirEmpresa = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fEmpresaActual = result.data?.getIntExtra("codEmpresa", 0) ?: 0
            // Guardamos la empresa actual en las preferencias para poder tener acceso al dato
            // desde otras actividades
            prefs?.edit()?.putInt("ultima_empresa", fEmpresaActual)?.apply()
            mostrarEmpresaActual()
        }
    }

    private var resultListaArticulos = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

            when (result.data?.getIntExtra("voyA", 1)) {
                LISTA_ARTICULOS -> lanzarListaArticulos()
                GRUPOS_Y_DEP -> lanzarCatalGrupDep()
                CATALOGOS -> lanzarCatalogos()
            }
        }
    }

    private var resultCatalGrupDep = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

            when (result.data?.getIntExtra("voyA", 1)) {
                LISTA_ARTICULOS -> lanzarListaArticulos()
                GRUPOS_Y_DEP -> lanzarCatalGrupDep()
                CATALOGOS -> lanzarCatalogos()
            }
        }
    }

    private var resultCatalogos = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

            when (result.data?.getIntExtra("voyA", 1)) {
                LISTA_ARTICULOS -> lanzarListaArticulos()
                GRUPOS_Y_DEP -> lanzarCatalGrupDep()
                CATALOGOS -> lanzarCatalogos()
            }
        }
    }

    private var resultClaveUsuario = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

            //val sQuePassword = result.data?.getStringExtra("password")?.uppercase(Locale.ROOT) ?: ""
            val sQuePassword = result.data?.getStringExtra("password") ?: ""
            val bSupervisor = result.data?.getBooleanExtra("supervisor", false) ?: false
            val sChorizo: String
            try {
                // Si no tenemos password, no le pasamos el algoritmo sha1.
                sChorizo = if (sQuePassword == "") { sQuePassword } else sha1(sQuePassword)

                if (bSupervisor) {
                    if (!sChorizo.equals(fConfiguracion.claveSupervisor(), ignoreCase = true)) {
                        val t = Toast.makeText(this, "Contrase??a incorrecta", Toast.LENGTH_LONG)
                        t.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
                        t.show()
                        pedirClaveUsuario()
                    }
                } else {
                    // Si hemos introducido la clave 20032610 entraremos del tir??n
                    if (sQuePassword != "20032610") {
                        if (!sChorizo.equals(fConfiguracion.claveUsuario(), ignoreCase = true)) {
                            val t = Toast.makeText(this, "Contrase??a incorrecta", Toast.LENGTH_LONG)
                            t.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
                            t.show()
                            pedirClaveUsuario()
                        }
                    }
                }

            } catch (e: NoSuchAlgorithmException) {
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            }
        }
        else finish()
    }

    fun confMultisistema(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        fNumClicks++
        if (fNumClicks >= 10) {
            val i = Intent(this, ConfMultisistema::class.java)
            startActivity(i)
            fNumClicks = 0
        }
    }


    fun confAcumMes(view: View) {
        view.getTag(0)              // Para que no d?? warning el compilador

        fNumClicks++
        if (fNumClicks >= 10) {
            prefs?.edit()?.putBoolean("usar_acummes", true)?.apply()

            MsjAlerta(this).informacion(resources.getString(R.string.msj_ConfigGuardada))
        }
    }


    @Throws(IOException::class)
    fun bdBackup() {
        val aldDialog = nuevoAlertBuilder(this, "Backup", "??Hacer backup?", true)

        aldDialog.setPositiveButton("S??") { _, _ ->
            hacerBackup()
        }
        val alert = aldDialog.create()
        alert.show()
    }


    //private fun hacerBackup(queBD: String, queNombre: String) {
    private fun hacerBackup() {
        try {
            val dbFile = this.getDatabasePath("ibsTablet00.db")
            val fis: FileInputStream?
            fis = FileInputStream(dbFile)

            val directorio = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.path
            val d = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

            if (d?.exists() == false) {
                d.mkdir()
            }

            val outFileName = "$directorio/ibsTablet.db"

            val output = FileOutputStream(outFileName)
            val buffer = ByteArray(1024)
            var length: Int

            var continuar = true
            while (continuar) {
                length = fis.read(buffer)
                if (length > 0) output.write(buffer, 0, length)
                else continuar = false
            }

            output.flush()
            output.close()
            fis.close()

            MsjAlerta(this).informacion("Se realiz?? la copia")
        } catch (e: IOException) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    /*
    @Throws(IOException::class)
    private fun importDatabase(inputFileName: String) {
        val mInput: InputStream = FileInputStream(inputFileName)
        val outFileName: String = YOUR_DB_PATH_HERE
        val mOutput: OutputStream = FileOutputStream(outFileName)
        val mBuffer = ByteArray(1024)
        var mLength: Int
        while (mInput.read(mBuffer).also { mLength = it } > 0) {
            mOutput.write(mBuffer, 0, mLength)
        }
        mOutput.flush()
        mOutput.close()
        mInput.close()
    }
    */

}