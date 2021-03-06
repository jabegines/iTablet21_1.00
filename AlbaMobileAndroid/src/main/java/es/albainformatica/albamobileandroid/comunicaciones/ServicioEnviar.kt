package es.albainformatica.albamobileandroid.comunicaciones

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.preference.PreferenceManager
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.NumExportDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.database.MyDatabase.Companion.queBDRoom
import es.albainformatica.albamobileandroid.entity.NumExportEnt
import es.albainformatica.albamobileandroid.registroEventos.RegistroEventosClase
import kotlinx.android.synthetic.main.com_servicio_enviar.*
import kotlinx.android.synthetic.main.com_servicio_recibir.progressBar
import kotlinx.android.synthetic.main.com_servicio_recibir.tvNumArchivos
import kotlinx.android.synthetic.main.com_servicio_recibir.tvPorcentaje
import okhttp3.*
import org.apache.commons.net.util.Base64
import org.jetbrains.anko.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class ServicioEnviar: AppCompatActivity() {
    private val numExpDao: NumExportDao? = MyDatabase.getInstance(this)?.numExportDao()
    private lateinit var fRegEventos: RegistroEventosClase

    private lateinit var prefs: SharedPreferences
    private lateinit var fContext: Context
    private var fEmail: String = ""
    private var fPassword: String = ""
    private var urlServicio: String = ""
    private var fHuella: String = ""
    private lateinit var miscCom: MiscComunicaciones
    private lateinit var handler: Handler
    private var fNumArchivo: Int = 1
    private var fSistemaId: String = "00"
    private var fNumPaquete = 0
    private var fEnviarAutom: Boolean = false

    private lateinit var fRecyclerView: RecyclerView
    private lateinit var fAdapter: RecAdapServEnviar
    private val lPaquetes = arrayListOf<ListaPaquetes>()

    private val fRequestServRecibir = 1

    // Definimos un Handler y un Runnable para que se ejecuten cada x tiempo y comprueben el estado de los paquetes
    private var fDelay: Long = 5000
    private lateinit var mainHandler: Handler
    private val updateRecycler = object: Runnable {
        override fun run() {
            verEstadoPaquetes()
            fAdapter.paquetes = lPaquetes
            fAdapter.notifyDataSetChanged()

            mainHandler.postDelayed(this, fDelay)
            fDelay = 30000
        }
    }


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.com_servicio_enviar)
        fContext = this

        fRegEventos = Comunicador.fRegEventos
        fRegEventos.registrarEvento(codEv_ComServEnv_Entrar, descrEv_ComServEnv_Entrar)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        fHuella = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

        val usarMultisistema = prefs.getBoolean("usar_multisistema", false)
        if (usarMultisistema) {
            val queBD = queBDRoom
            fSistemaId = queBD.substring(queBD.length-5, queBD.length-3)
            fEmail = prefs.getString(fSistemaId + "_usuario_servicio", "") ?: ""
            fPassword = prefs.getString(fSistemaId + "_password_servicio", "") ?: ""
            urlServicio = prefs.getString(fSistemaId + "_url_servicio", "") ?: ""
        }
        else
        {
            fSistemaId = prefs.getString("sistemaId_servicio", "00") ?: "00"
            fEmail = prefs.getString("usuario_servicio", "") ?: ""
            fPassword = prefs.getString("password_servicio", "") ?: ""
            urlServicio = prefs.getString("url_servicio", "") ?: ""
        }
        fSistemaId = Base64.encodeBase64String(fSistemaId.toByteArray()).replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_").replace("=", "*")

        fEnviarAutom = intent.getBooleanExtra("enviarAutom", false)

        inicializarControles()
        // Creamos el Handler
        mainHandler = Handler(Looper.getMainLooper())

        // Siempre recibiremos antes de enviar, igual que hacemos en ibsTablet Central
        val i = Intent(this, ServicioRecibir::class.java)
        i.putExtra("recibirPaquetes", true)
        i.putExtra("recibirAutom", true)
        startActivityForResult(i, fRequestServRecibir)
    }


    override fun onPause() {
        super.onPause()
        // Paramos el Handler con el Runnable para que no siga ejecut??ndose
        mainHandler.removeCallbacks(updateRecycler)
    }

    override fun onResume() {
        super.onResume()
        if (!fEnviarAutom) {
            // Arrancamos el Handler con el Runnable
            mainHandler.post(updateRecycler)
        }
    }

    override fun onDestroy() {
        fRegEventos.registrarEvento(codEv_ComServEnv_Salir, descrEv_ComServEnv_Salir)

        super.onDestroy()
    }

    private fun inicializarControles() {

        tvEnviando.text = ""
        tvPorcentaje.text = "0%"
        tvNumArchivos.text = "0/0"

        handler = @SuppressLint("HandlerLeak")
        object: Handler() {

            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                tvEnviando.text = msg.obj.toString()

                if (msg.arg1 > 0) {
                    if (progressBar.max == 0) {
                        progressBar.max = msg.arg1
                        progressBar.visibility = View.VISIBLE
                    }
                    progressBar.incrementProgressBy(1)
                    val sPorcentaje = msg.arg2.toString() + "%"
                    tvPorcentaje.text = sPorcentaje
                    val sNumArchivos = fNumArchivo.toString() + "/" + msg.arg1.toString()
                    tvNumArchivos.text = sNumArchivos
                    fNumArchivo++
                }
            }
        }

        miscCom = MiscComunicaciones(fContext, true)
        miscCom.puente = this.handler

        if (fEnviarAutom) {
            lyEstadoPaquetes.visibility = View.GONE
        } else {
            setupRecyclerView()
        }
    }



    private fun setupRecyclerView() {
        fRecyclerView = rvEstadoEnv
        fRecyclerView.layoutManager = LinearLayoutManager(this)

        fAdapter = RecAdapServEnviar(getPaquetes(), fContext, object: RecAdapServEnviar.OnItemClickListener {
            override fun onClick(view: View, data: ListaPaquetes) {
                //
            }
        })

        fRecyclerView.adapter = fAdapter
    }


    private fun verEstadoPaquetes() {
        doAsync {

            for (dPaquete in lPaquetes) {
                dPaquete.fechaHoraRecogida = dimeEstadoPaquete(dPaquete.numPaquete)

                // Si el paquete ya fue recogido por ibsTablet Central lo podemos borrar de la tabla NumExport
                if (dPaquete.fechaHoraRecogida != "") {
                    numExpDao?.borrarExp(dPaquete.numPaquete)
                }
            }
        }
    }

    private fun getPaquetes(): MutableList<ListaPaquetes> {
        val lExportaciones = numExpDao?.getAllExport() ?: emptyList<NumExportEnt>().toMutableList()

        if (lExportaciones.isNotEmpty()) {
            for (numExp in lExportaciones) {
                val dPaquete = ListaPaquetes()
                dPaquete.numPaquete = numExp.numExport
                dPaquete.fechaHoraEnvio = numExp.fecha + " " + numExp.hora
                lPaquetes.add(dPaquete)
            }
        }

        return lPaquetes
    }



    fun salir(view: View) {
        view.isEnabled = false      // Para que el compilador no d?? warning
        finish()
    }



    fun enviar(view: View) {

        fContext.alert("??Comenzar con el envio?") {
            title = "Enviar"
            yesButton {
                val msg = Message()
                msg.obj = "Conectando con el servicio ..."
                handler.sendMessage(msg)

                comenzarEnvio(view)
            }
            noButton { finish() }
        }.show()
    }

    private fun comenzarEnvio(view: View) {

        var resultado: Boolean

        doAsync {

            uiThread {
                view.isEnabled = false
            }

            if (miscCom.baseDatosAXML(0)) {

                resultado = enviarZip()

                // Si el envio al servicio ha sido correcto actualizamos los ficheros con el n??mero de paquete
                // que ??ste nos ha devuelto.
                if (resultado) miscCom.actualizarNumPaquete(fNumPaquete)
                else miscCom.revertirEstado()

                uiThread {
                    if (resultado) {
                        fContext.alert("Fin del envio") {
                            title = "Informaci??n"
                            yesButton {
                                val returnIntent = Intent()
                                setResult(Activity.RESULT_OK, returnIntent)
                                finish()
                            }
                        }.show()
                    } else {
                        fContext.alert("Hubo problemas al enviar, por favor int??ntelo de nuevo") {
                            title = "Error"
                            yesButton {
                                // Por ahora hemos desabilitado la comprobaci??n del riesgo, ya que la hacemos de otra manera en grabarPieDoc()
                                val returnIntent = Intent()
                                setResult(Activity.RESULT_CANCELED, returnIntent)
                                finish() }
                        }.show()
                    }
                }
            }
        }
    }


    @SuppressLint("SimpleDateFormat", "HardwareIds")
    private fun dimeEstadoPaquete(fNumPaquete: Int): String {
        //val fEmail = prefs.getString("usuario_servicio", "") ?: ""
        //val fHuella = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        //val fPassword = prefs.getString("password_servicio", "")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        val fFechaHora = sdf.format(Date()).replace("/", "").replace(":", "").replace(" ", "")
        val fAccion = "13"
        val fAppId = "1"

        val quePaquete = Base64.encodeBase64String(fNumPaquete.toString().toByteArray()).replace("\r\n", "")
        var fFirma = fEmail + ";;;" + fHuella + ";;;" + (fEmail + fHuella).length + ";;;" + fPassword +
                ";;;" + fAccion + ";;;" + fAppId + ";;;" + fFechaHora
        fFirma = fFirma + ";;;" + sha1(fFirma)
        fFirma = Base64.encodeBase64String(fFirma.toByteArray())
        fFirma = fFirma.replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_").replace("=", "*")

        val client = OkHttpClient.Builder().readTimeout(5, TimeUnit.MINUTES).build()
        val queUrl = "$urlServicio/Service/Action/ActualStatePackage"
        val call = client.newCall(Request.Builder()
                .url("$queUrl?Sign=$fFirma&SystemId=$fSistemaId&Package=$quePaquete")
                .get()
                .build())

        val response = call.execute()
        if (response.isSuccessful) {
            val queRespuesta = response.body()?.string() ?: ""
            if (respuestaCorrecta(queRespuesta)) {
                return descomponerEstadoPaquete(queRespuesta)
            }
        }

        return ""
    }


    private fun descomponerEstadoPaquete(response: String): String {
        val posicion = response.indexOf("FechaRecepcion") + 17
        var fechaRecepcion = response.substring(posicion, posicion+1)

        fechaRecepcion = if (fechaRecepcion != "\"") {
            response.substring(posicion, posicion + 16)
        }
        else ""

        return fechaRecepcion
    }


    @SuppressLint("HardwareIds", "SimpleDateFormat")
    private fun enviarZip(): Boolean {
        //val fEmail = prefs.getString("usuario_servicio", "") ?: ""
        //val fHuella = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        //val fPassword = prefs.getString("password_servicio", "")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        val fFechaHora = sdf.format(Date()).replace("/", "").replace(":", "").replace(" ", "")
        val fAccion = "1"
        val fAppId = "1"

        var fFirma = fEmail + ";;;" + fHuella + ";;;" + (fEmail + fHuella).length + ";;;" + fPassword +
                ";;;" + fAccion + ";;;" + fAppId + ";;;" + fFechaHora
        fFirma = fFirma + ";;;" + sha1(fFirma)
        fFirma = Base64.encodeBase64String(fFirma.toByteArray())
        fFirma = fFirma.replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_").replace("=", "*")

        val file = File(miscCom.rutaLocalEnvio + "/envio.zip")

        val msg = Message()
        msg.obj = "Enviando fichero .zip"
        handler.sendMessage(msg)

        try {
            val client = OkHttpClient()
            val urlBuilder = HttpUrl.parse("$urlServicio/Service/Action/UploadPackage")?.newBuilder()
            val url = urlBuilder?.build().toString()

            val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("Sign", fFirma)
                    .addFormDataPart("SystemId", fSistemaId)
                    .addFormDataPart("Summary", miscCom.cadenaResumen)
                    .addFormDataPart("UploadedZip", "envio.zip", RequestBody.create(MediaType.parse("application/octect-stream"), file))
                    .build()

            val request = Request.Builder().url(url).post(requestBody).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val queRespuesta = response.body()!!.string()

                // Si hemos recibido Success:true en la respuesta seguimos trabajando
                return if (respuestaCorrecta(queRespuesta)) {
                    // Vemos el n??mero de paquete que nos ha devuelto el servicio. Este n??mero es el que estableceremos
                    // como n??mero de exportaci??n, de esta forma podremos hacer consultas al servicio acerca de este paquete.
                    obtenerNumPaquete(queRespuesta)

                    // Una vez hemos enviado, borramos los ficheros de la carpeta local.
                    borrarFichCarpLocal()

                    true

                } else {
                    // Si la respuesta no ha sido correcta comprobaremos si el paquete est?? subido al servicio
                    // para, en este caso, dar por bueno el resultado del env??o.
                    obtenerNumPaquete(queRespuesta)
                    if (fNumPaquete > 0) {
                        val quePaquete = paqueteEnServicio(fNumPaquete)
                        if (quePaquete > 0) {
                            // Una vez hemos enviado, borramos los ficheros de la carpeta local.
                            borrarFichCarpLocal()
                            return true
                        }
                    }

                    false
                }
            } else return false

        } catch (e: Exception) {
            return false
        }
    }


    private fun borrarFichCarpLocal() {
        val rutaLocEnv = File(miscCom.rutaLocalEnvio)
        val xmlFiles = rutaLocEnv.listFiles() ?: emptyArray()
        for (File in xmlFiles) {
            File.delete()
        }
    }


    @SuppressLint("HardwareIds")
    private fun paqueteEnServicio(fNumPaquete: Int): Int {
        //val fEmail = prefs.getString("usuario_servicio", "") ?: ""
        //val fHuella = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        //val fPassword = prefs.getString("password_servicio", "")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val fFechaHora = sdf.format(Date()).replace("/", "").replace(":", "").replace(" ", "")
        val fAccion = "13"
        val fAppId = "1"

        val quePaquete = Base64.encodeBase64String(fNumPaquete.toString().toByteArray()).replace("\r\n", "")
        var fFirma = fEmail + ";;;" + fHuella + ";;;" + (fEmail + fHuella).length + ";;;" + fPassword +
                ";;;" + fAccion + ";;;" + fAppId + ";;;" + fFechaHora
        fFirma = fFirma + ";;;" + sha1(fFirma)
        fFirma = Base64.encodeBase64String(fFirma.toByteArray())
        fFirma = fFirma.replace("\r", "").replace("\n", "").replace("+", "-").replace("\\", "_").replace("=", "*")

        val client = OkHttpClient.Builder().readTimeout(5, TimeUnit.MINUTES).build()
        val queUrl = "$urlServicio/Service/Action/ActualStatePackage"
        val call = client.newCall(Request.Builder()
            .url("$queUrl?Sign=$fFirma&SystemId=$fSistemaId&Package=$quePaquete")
            .get()
            .build())

        val response = call.execute()
        if (response.isSuccessful) {
            val queRespuesta = response.body()?.string() ?: ""
            if (respuestaCorrecta(queRespuesta)) {
                return fNumPaquete
            }
        }

        return 0
    }


    private fun respuestaCorrecta(queRespuesta: String): Boolean {
        var posicion = queRespuesta.indexOf("Success") + 9
        var queCadena = queRespuesta.substring(posicion, queRespuesta.length)
        posicion = queCadena.indexOf(",")
        queCadena = queCadena.substring(0, posicion)

        return queCadena == "true"
    }

    private fun obtenerNumPaquete(queRespuesta: String) {
        var posicion = queRespuesta.indexOf("Entero1") + 9
        var queCadena = queRespuesta.substring(posicion, queRespuesta.length)
        posicion = queCadena.indexOf(",")
        queCadena = queCadena.substring(0, posicion)

        fNumPaquete = Integer.parseInt(queCadena)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fRequestServRecibir) {
            if (resultCode == RESULT_OK) {
                // Si queremos enviar autom??ticamente llamamos desde aqu?? a enviar()
                if (fEnviarAutom)
                    comenzarEnvio(View(fContext))
            }
            else {
                finish()
            }
        }
    }



}