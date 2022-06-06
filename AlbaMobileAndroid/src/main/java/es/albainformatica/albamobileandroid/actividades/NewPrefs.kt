package es.albainformatica.albamobileandroid.actividades

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.comunicaciones.MiscServicio
import es.albainformatica.albamobileandroid.database.MyDatabase
import kotlinx.android.synthetic.main.new_prefs.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread


class NewPrefs: AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private var numClicks: Int = 0
    private var fUsarMultisistema: Boolean = false


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.new_prefs)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        inicializarControles()
    }


    private fun inicializarControles() {
        fUsarMultisistema = prefs.getBoolean("usar_multisistema", false)

        val queTerminal = "Terminal " + prefs.getString("terminal", "")
        tvTerminal.text = queTerminal
        //val queTexto = "Ruta local de comunicación: " + prefs.getString("rutacomunicacion", "")
        //tvRutaLocal.text = queTexto

        tvRutaComWifi.text = prefs.getString("ruta_wifi", "")
        tvDominioWifi.text = prefs.getString("dominio_wifi", "")
        tvUsuarioWifi.text = prefs.getString("usuario_wifi", "")
        tvPasswWifi.text = prefs.getString("password_wifi", "")

        if (fUsarMultisistema) {
            val queBD = MyDatabase.queBDRoom
            val fSistemaId = queBD.substring(queBD.length - 5, queBD.length-3)

            tvUsarServ.text = if (prefs.getBoolean(fSistemaId + "_usar_servicio", false)) "Usar servicio: SI"
            else "Usar servicio: NO"
            tvRutaComServ.text = prefs.getString(fSistemaId + "_url_servicio", "")
            tvUsuarioServ.text = prefs.getString(fSistemaId + "_usuario_servicio", "")
            tvPassServ.text = prefs.getString(fSistemaId + "_password_servicio", "")
            val envDocs: String = if (prefs.getBoolean(fSistemaId + "_enviar_docs_autom", false)) "Enviar documentos automáticamente: SI"
            else "Enviar documentos automáticamente: NO"
            tvEnvDoc.text = envDocs
        } else {
            tvUsarServ.text = if (prefs.getBoolean("usar_servicio", false)) "Usar servicio: SI"
            else "Usar servicio: NO"
            tvRutaComServ.text = prefs.getString("url_servicio", "")
            tvUsuarioServ.text = prefs.getString("usuario_servicio", "")
            tvPassServ.text = prefs.getString("password_servicio", "")
            val envDocs: String = if (prefs.getBoolean("enviar_docs_autom", false)) "Enviar documentos automáticamente: SI"
            else "Enviar documentos automáticamente: NO"
            tvEnvDoc.text = envDocs
        }

        var queTipoDoc = prefs.getString("doc_defecto", "1") ?: "1"
        queTipoDoc = "Documento por defecto: " + docDefectoAsString(queTipoDoc.toByte())
        tvDocPorDft.text = queTipoDoc
        var queModoVta = prefs.getString("modo_venta", "1")
        queModoVta = "Modo de venta por defecto: " + modoVtaAsString(queModoVta)
        tvModoVenta.text = queModoVta
        val pedirDetalle: String = if (prefs.getBoolean("ventas_detall_cat_vis", false)) "Pedir detalle de cada artículo: SI"
        else "Pedir detalle de cada artículo: NO"
        tvPedirDetalle.text = pedirDetalle
        val pedirCodPrim: String = if (prefs.getBoolean("ventas_pedir_codigo", false)) "Pedir código primero: SI"
        else "Pedir código primero: NO"
        tvPedirCodArt.text = pedirCodPrim
        val cltesCodPostal: String = if (prefs.getBoolean("ventas_rutero_cp", false)) "Presentar cltes. por código postal: SI"
        else "Presentar cltes. por código postal: NO"
        tvCltCodPostal.text = cltesCodPostal
        val clasDoc: String = if (prefs.getBoolean("ventas_enviar_guardar", false)) "Clasificar doc. como Env./Guardar: SI"
        else "Clasificar doc. como Env./Guardar: NO"
        tvEnvGuardar.text = clasDoc
        val expPedidos: String = if (prefs.getBoolean("ventas_exportar_pdf", false)) "Exp. pedidos como pdf al terminar: SI"
        else "Exp. pedidos como pdf al terminar: NO"
        tvExpPedidos.text = expPedidos

        val pedirCobro: String = if (prefs.getBoolean("reparto_pedir_cobro", false)) resources.getString(R.string.pref_pedir_cobro_reparto) + " SI"
        else resources.getString(R.string.pref_pedir_cobro_reparto) + " NO"
        tvPedirCobro.text = pedirCobro
        val pedirFirma: String = if (prefs.getBoolean("reparto_pedir_firma", false)) resources.getString(R.string.pref_pedir_firma_reparto) + " SI"
        else resources.getString(R.string.pref_pedir_firma_reparto) + " NO"
        tvPedirFirma.text = pedirFirma
        val pedirIncidencia: String = if (prefs.getBoolean("reparto_pedir_incid", false)) resources.getString(R.string.pref_pedir_incid_reparto) + " SI"
        else resources.getString(R.string.pref_pedir_incid_reparto) + " NO"
        tvPedirIncid.text = pedirIncidencia
        val incidDef = resources.getString(R.string.pref_incid_por_def) + prefs.getString("incid_por_defecto", "0")
        tvIncDefecto.text = incidDef

        val cargarTodos: String = if (prefs.getBoolean("cargar_todos_art", true)) "Cargar todos los articulos: " + "SI"
        else "Cargar todos los articulos: " + "NO"
        tvCargarTodosArt.text = cargarTodos
        val mantBusq: String = if (prefs.getBoolean("mantener_ult_busq", false)) "Mantener ultima busqueda: " + "SI"
        else "Mantener ultima busqueda: " +"NO"
        tvMantUltBusq.text = mantBusq

        val lineasDoc = "Lineas del documento: " + prefs.getString("lineas_doc", "48")
        tvLineasDoc.text = lineasDoc
        val posCorte = "Posición del corte: " + prefs.getString("posicion_corte", "8")
        tvPosCorte.text = posCorte
        val primLinea = "Primera linea: " + prefs.getString("primera_linea", "8")
        tvPrimeraLinea.text = primLinea
        val prLinArticulos = "Primera linea para los articulos: " + prefs.getString("prim_linea_articulos", "24")
        tvPrLinArticulos.text = prLinArticulos
        val posPie = "Posición del pie: " + prefs.getString("posicion_pie", "37")
        tvPosicionPie.text = posPie

        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.mni_configuracion)
    }



    @SuppressLint("InflateParams")
    fun pedirTerminal(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        builder.setTitle("Configurar terminal")
        val dialogLayout = inflater.inflate(R.layout.prefs_conf_terminal, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.edtNumTerminal)
        //val edtRutaLocal = dialogLayout.findViewById<EditText>(R.id.edtRutaLocal)
        editText.setText(prefs.getString("terminal", ""))
        //edtRutaLocal.setText(prefs.getString("rutacomunicacion", this.getExternalFilesDir(null)?.path))

        builder.setView(dialogLayout)
        builder.setPositiveButton("OK") { _, _ ->
            prefs.edit().putString("terminal", editText.text.toString()).apply()
            //prefs.edit().putString("rutacomunicacion", edtRutaLocal.text.toString()).apply()
            // Por ahora asignamos la ruta de comunicación sin preguntar, apuntamos a la carpeta
            // que Android (a partir de la versión 10) asigna a la apk:
            // 'Android/data/es.albainformatica.albamobileandroid/files'
            prefs.edit().putString("rutacomunicacion", this.getExternalFilesDir(null)?.path).apply()

            inicializarControles()
        }
        builder.setNegativeButton("Cancelar") { _, _ -> }
        builder.show()
    }

    fun confDefectoWifi(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        numClicks ++
        if (numClicks >= 3) {
            numClicks = 0

            prefs.edit().putString("ruta_wifi", "//192.168.10.41/android").apply()
            prefs.edit().putString("dominio_wifi", "central").apply()
            prefs.edit().putString("usuario_wifi", "jabegines").apply()
            prefs.edit().putString("password_wifi", "ja@123456").apply()

            inicializarControles()
        }
    }


    @SuppressLint("InflateParams")
    fun pedirWifi(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        builder.setTitle("Comunicación WIFI")
        val dialogLayout = inflater.inflate(R.layout.prefs_conf_wifi, null)
        val edtRuta = dialogLayout.findViewById<EditText>(R.id.edtRutaWifi)
        val edtDominio = dialogLayout.findViewById<EditText>(R.id.edtDominio)
        val edtUsuario = dialogLayout.findViewById<EditText>(R.id.edtUsuarioWifi)
        val edtPassword = dialogLayout.findViewById<EditText>(R.id.edtPasswWifi)
        edtRuta.setText(prefs.getString("ruta_wifi", ""))
        edtDominio.setText(prefs.getString("dominio_wifi", ""))
        edtUsuario.setText(prefs.getString("usuario_wifi", ""))
        edtPassword.setText(prefs.getString("password_wifi", ""))
        builder.setView(dialogLayout)
        builder.setPositiveButton("OK") { _, _ ->
            prefs.edit().putString("ruta_wifi", edtRuta.text.toString()).apply()
            prefs.edit().putString("dominio_wifi", edtDominio.text.toString()).apply()
            prefs.edit().putString("usuario_wifi", edtUsuario.text.toString()).apply()
            prefs.edit().putString("password_wifi", edtPassword.text.toString()).apply()

            inicializarControles()
        }
        builder.setNegativeButton("Cancelar") { _, _ -> }
        builder.show()
    }

    @SuppressLint("InflateParams")
    fun pedirServicio(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        builder.setTitle("Comunicación con Servicio")
        val dialogLayout = inflater.inflate(R.layout.prefs_conf_servicio, null)
        val chkUsarServ = dialogLayout.findViewById<CheckBox>(R.id.chkUsarServicio)
        val edtUrl = dialogLayout.findViewById<EditText>(R.id.edtUrlServicio)
        val edtCuenta = dialogLayout.findViewById<EditText>(R.id.edtCuentaServicio)
        val edtPassword = dialogLayout.findViewById<EditText>(R.id.edtPasswServicio)
        val chkEnvDoc = dialogLayout.findViewById<CheckBox>(R.id.chkEnvDocAut)

        if (fUsarMultisistema) {
            val queBD = MyDatabase.queBDRoom
            val fSistemaId = queBD.substring(queBD.length - 5, queBD.length-3)

            chkUsarServ.isChecked = prefs.getBoolean(fSistemaId + "_usar_servicio", false)
            edtUrl.setText(prefs.getString(fSistemaId + "_url_servicio", ""))
            edtCuenta.setText(prefs.getString(fSistemaId + "_usuario_servicio", ""))
            edtPassword.setText(prefs.getString(fSistemaId + "_password_servicio", ""))
            chkEnvDoc.isChecked = prefs.getBoolean(fSistemaId + "_enviar_docs_autom", false)

        } else {
            chkUsarServ.isChecked = prefs.getBoolean("usar_servicio", false)
            edtUrl.setText(prefs.getString("url_servicio", ""))
            edtCuenta.setText(prefs.getString("usuario_servicio", ""))
            edtPassword.setText(prefs.getString("password_servicio", ""))
            chkEnvDoc.isChecked = prefs.getBoolean("enviar_docs_autom", false)
        }

        builder.setView(dialogLayout)
        builder.setPositiveButton("OK") { _, _ ->
            if (fUsarMultisistema) {
                val queBD = MyDatabase.queBDRoom
                val fSistemaId = queBD.substring(queBD.length - 5, queBD.length-3)

                prefs.edit().putBoolean(fSistemaId + "_usar_servicio", chkUsarServ.isChecked).apply()
                prefs.edit().putString(fSistemaId + "_url_servicio", edtUrl.text.toString()).apply()
                prefs.edit().putString(fSistemaId + "_usuario_servicio", edtCuenta.text.toString()).apply()
                prefs.edit().putString(fSistemaId + "_password_servicio", edtPassword.text.toString()).apply()
                prefs.edit().putBoolean(fSistemaId + "_enviar_docs_autom", chkEnvDoc.isChecked).apply()

            } else {
                prefs.edit().putBoolean("usar_servicio", chkUsarServ.isChecked).apply()
                prefs.edit().putString("url_servicio", edtUrl.text.toString()).apply()
                prefs.edit().putString("usuario_servicio", edtCuenta.text.toString()).apply()
                prefs.edit().putString("password_servicio", edtPassword.text.toString()).apply()
                prefs.edit().putBoolean("enviar_docs_autom", chkEnvDoc.isChecked).apply()
            }

            inicializarControles()
        }
        builder.setNegativeButton("Cancelar") { _, _ -> }
        builder.show()
    }


    fun actualizarApk(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        // Antes de actualizar comprobaremos que no tengamos nada pendiente de enviar
        if (puedoRecibir(this)) {
            val aldDialog = nuevoAlertBuilder(this, "Actualizar", "¿Actualizar la aplicación?", true)

            aldDialog.setPositiveButton("Sí") { _, _ ->
                val i = Intent(this, ActualizarApk::class.java)
                startActivity(i)
            }
            val alert = aldDialog.create()
            alert.show()
        } else {
            MsjAlerta(this).alerta("Tiene documentos o cobros pendientes de enviar. No podrá actualizar.")
        }
    }


    @SuppressLint("InflateParams")
    fun probarComServicio(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val miscServicio = MiscServicio(this@NewPrefs)
        doAsync {
            val hayComunicacion = miscServicio.hayComunicacion()

            uiThread {
                val queMensaje: String = if (hayComunicacion) "La comunicación con el servicio es buena"
                else "No se pudo comunicar con el servicio"

                alert(queMensaje, "Probar comunicación con el servicio") {
                    positiveButton("Ok") { }
                }.show()
            }
        }
    }


    @SuppressLint("InflateParams")
    fun pedirVentas(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        builder.setTitle("Ventas")
        val dialogLayout = inflater.inflate(R.layout.prefs_conf_ventas, null)
        val rgrpDocDef = dialogLayout.findViewById<RadioGroup>(R.id.rdgDocDefecto)
        val sDoc = prefs.getString("doc_defecto", "1") ?: "1"
        val rdbqueDoc = rgrpDocDef.getChildAt(sDoc.toInt()-1) as RadioButton
        rdbqueDoc.isChecked = true

        val rdgModoVta = dialogLayout.findViewById<RadioGroup>(R.id.rdgModoVta)
        val sModo = prefs.getString("modo_venta", "1") ?: "1"
        val rdbqueModo = rdgModoVta.getChildAt(sModo.toInt()-1) as RadioButton
        rdbqueModo.isChecked = true

        val tvPedirDet = dialogLayout.findViewById<TextView>(R.id.tvPedirDetalles)
        val chkPedirDet = dialogLayout.findViewById<CheckBox>(R.id.chkPedirDetalles)
        rdgModoVta.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rdbModoLista -> {
                    tvPedirDet.isEnabled = false
                    chkPedirDet.isEnabled = false
                }
                R.id.rdbModoHco -> {
                    tvPedirDet.isEnabled = false
                    chkPedirDet.isEnabled = false
                }
                R.id.rdbModoCatVisual -> {
                    tvPedirDet.isEnabled = true
                    chkPedirDet.isEnabled = true
                }
            }
        }

        chkPedirDet.isChecked = prefs.getBoolean("ventas_detall_cat_vis", false)
        val chkPedirCod = dialogLayout.findViewById<CheckBox>(R.id.chkPedirCodigo)
        val chkClteCodP = dialogLayout.findViewById<CheckBox>(R.id.chkCltCodPostal)
        val chkClasDoc = dialogLayout.findViewById<CheckBox>(R.id.chkDocEnvGd)
        val chkExpPed = dialogLayout.findViewById<CheckBox>(R.id.chkDocExpPdf)
        chkPedirCod.isChecked = prefs.getBoolean("ventas_pedir_codigo", false)
        chkClteCodP.isChecked = prefs.getBoolean("ventas_rutero_cp", false)
        chkClasDoc.isChecked = prefs.getBoolean("ventas_enviar_guardar", false)
        chkExpPed.isChecked = prefs.getBoolean("ventas_exportar_pdf", false)

        builder.setView(dialogLayout)
        builder.setPositiveButton("OK") { _, _ ->
            when (rgrpDocDef.checkedRadioButtonId) {
                R.id.rdbFactura -> prefs.edit().putString("doc_defecto", "1").apply()
                R.id.rdbAlbaran -> prefs.edit().putString("doc_defecto", "2").apply()
                R.id.rdbPedido -> prefs.edit().putString("doc_defecto", "3").apply()
                else -> prefs.edit().putString("doc_defecto", "4").apply()
            }
            when (rdgModoVta.checkedRadioButtonId) {
                R.id.rdbModoLista -> prefs.edit().putString("modo_venta", "1").apply()
                R.id.rdbModoHco -> prefs.edit().putString("modo_venta", "2").apply()
                R.id.rdbModoCatVisual -> prefs.edit().putString("modo_venta", "3").apply()
            }

            prefs.edit().putBoolean("ventas_detall_cat_vis", chkPedirDet.isChecked).apply()
            prefs.edit().putBoolean("ventas_pedir_codigo", chkPedirCod.isChecked).apply()
            prefs.edit().putBoolean("ventas_rutero_cp", chkClteCodP.isChecked).apply()
            prefs.edit().putBoolean("ventas_enviar_guardar", chkClasDoc.isChecked).apply()
            prefs.edit().putBoolean("ventas_exportar_pdf", chkExpPed.isChecked).apply()

            inicializarControles()
        }
        builder.setNegativeButton("Cancelar") { _, _ -> }
        builder.show()
    }


    @SuppressLint("InflateParams")
    fun pedirReparto(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        builder.setTitle("Reparto")
        val dialogLayout = inflater.inflate(R.layout.prefs_conf_reparto, null)

        val chkPedirCobro = dialogLayout.findViewById<CheckBox>(R.id.chkPedirCobro)
        val chkPedirFirma = dialogLayout.findViewById<CheckBox>(R.id.chkPedirFirma)
        val chkPedirIncid = dialogLayout.findViewById<CheckBox>(R.id.chkPedirIncidencia)
        val editText = dialogLayout.findViewById<EditText>(R.id.edtIncidDef)
        chkPedirCobro.isChecked = prefs.getBoolean("reparto_pedir_cobro", false)
        chkPedirFirma.isChecked = prefs.getBoolean("reparto_pedir_firma", false)
        chkPedirIncid.isChecked = prefs.getBoolean("reparto_pedir_incid", false)
        editText.setText(prefs.getString("incid_por_defecto", "0"))

        builder.setView(dialogLayout)
        builder.setPositiveButton("OK") { _, _ ->
            prefs.edit().putBoolean("reparto_pedir_cobro", chkPedirCobro.isChecked).apply()
            prefs.edit().putBoolean("reparto_pedir_firma", chkPedirFirma.isChecked).apply()
            prefs.edit().putBoolean("reparto_pedir_incid", chkPedirIncid.isChecked).apply()
            prefs.edit().putString("incid_por_defecto", editText.text.toString()).apply()

            inicializarControles()
        }
        builder.setNegativeButton("Cancelar") { _, _ -> }
        builder.show()
    }


    @SuppressLint("InflateParams")
    fun pedirArticulos(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        builder.setTitle("Artículos")
        val dialogLayout = inflater.inflate(R.layout.prefs_conf_articulos, null)

        val chkCargarTodos = dialogLayout.findViewById<CheckBox>(R.id.chkCargarTodos)
        val chkMantUltBusq = dialogLayout.findViewById<CheckBox>(R.id.chkMantUltBusq)
        chkCargarTodos.isChecked = prefs.getBoolean("cargar_todos_art", true)
        chkMantUltBusq.isChecked = prefs.getBoolean("mantener_ult_busq", false)

        builder.setView(dialogLayout)
        builder.setPositiveButton("OK") { _, _ ->
            prefs.edit().putBoolean("cargar_todos_art", chkCargarTodos.isChecked).apply()
            prefs.edit().putBoolean("mantener_ult_busq", chkMantUltBusq.isChecked).apply()

            inicializarControles()
        }
        builder.setNegativeButton("Cancelar") { _, _ -> }
        builder.show()
    }


    @SuppressLint("InflateParams")
    fun pedirImpresion(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        builder.setTitle("Impresión")
        val dialogLayout = inflater.inflate(R.layout.prefs_conf_impresion, null)

        val edtLineasDoc = dialogLayout.findViewById<EditText>(R.id.edtLineasDoc)
        val edtPosCorte = dialogLayout.findViewById<EditText>(R.id.edtPosicionCorte)
        val edtPrimLinea = dialogLayout.findViewById<EditText>(R.id.edtPrimeraLinea)
        val edtPrLinArt = dialogLayout.findViewById<EditText>(R.id.edtPrimLinArt)
        val edtPosPie = dialogLayout.findViewById<EditText>(R.id.edtPosicionPie)
        edtLineasDoc.setText(prefs.getString("lineas_doc", "48"))
        edtPosCorte.setText(prefs.getString("posicion_corte", "8"))
        edtPrimLinea.setText(prefs.getString("primera_linea", "8"))
        edtPrLinArt.setText(prefs.getString("prim_linea_articulos", "24"))
        edtPosPie.setText(prefs.getString("posicion_pie", "37"))

        builder.setView(dialogLayout)
        builder.setPositiveButton("OK") { _, _ ->
            prefs.edit().putString("lineas_doc", edtLineasDoc.text.toString()).apply()
            prefs.edit().putString("posicion_corte", edtPosCorte.text.toString()).apply()
            prefs.edit().putString("primera_linea", edtPrimLinea.text.toString()).apply()
            prefs.edit().putString("prim_linea_articulos", edtPrLinArt.text.toString()).apply()
            prefs.edit().putString("posicion_pie", edtPosPie.text.toString()).apply()

            inicializarControles()
        }
        builder.setNegativeButton("Cancelar") { _, _ -> }
        builder.show()
    }


}