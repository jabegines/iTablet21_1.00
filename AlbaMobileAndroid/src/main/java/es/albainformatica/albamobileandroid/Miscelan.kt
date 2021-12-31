package es.albainformatica.albamobileandroid

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.preference.PreferenceManager
import android.text.Html
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import es.albainformatica.albamobileandroid.dao.CobrosDao
import es.albainformatica.albamobileandroid.dao.SaldosDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import java.io.File
import java.lang.Exception
import java.lang.NumberFormatException
import java.lang.StringBuilder
import java.math.RoundingMode
import java.security.MessageDigest
import java.text.NumberFormat
import java.util.*



    fun dimeRutaImagenes(activity: Activity): String {
        val pref = PreferenceManager.getDefaultSharedPreferences(activity)
        val usarMultisistema = pref.getBoolean("usar_multisistema", false)
        val rutaImagenes = pref.getString("rutacomunicacion", "") ?: ""
        val carpetaImagenes: String = if (rutaImagenes == "") {
            if (usarMultisistema) "/storage/sdcard0/alba/imagenes/" + BaseDatos.queBaseDatos + "/"
            else "/storage/sdcard0/alba/imagenes/"
        } else {
            if (usarMultisistema) rutaImagenes + "/imagenes/" + BaseDatos.queBaseDatos + "/" else "$rutaImagenes/imagenes/"
        }
        return carpetaImagenes
    }


    fun dimeRutaDocAsoc(activity: Activity?): String {
        val pref = PreferenceManager.getDefaultSharedPreferences(activity)
        val usarMultisistema = pref.getBoolean("usar_multisistema", false)
        val rutaDocAsoc = pref.getString("rutacomunicacion", "") ?: ""
        val carpetaDocAsoc: String = if (rutaDocAsoc == "") {
            if (usarMultisistema) "/storage/sdcard0/alba/docasociados/" + BaseDatos.queBaseDatos + "/" else "/storage/sdcard0/alba/docasociados/"
        } else {
            if (usarMultisistema) rutaDocAsoc + "/docasociados/" + BaseDatos.queBaseDatos + "/" else "$rutaDocAsoc/docasociados/"
        }
        return carpetaDocAsoc
    }


    fun ponerCeros(cadena: String, longitud: Byte): String {
        var resultado: String
        return run {
            resultado = cadena.trim()
            val fNumCeros = longitud - cadena.length

            if (cadena != "") {
                if (fNumCeros > 0) {
                    resultado = String.format("%0" + fNumCeros + "d", 0) + cadena
                }
                resultado
            } else ""
        }
    }


    fun Redondear(dNumero: Double, iDecimales: Int): Double {
        val dDecimales = iDecimales * 1.0 + 1
        val numberFormat = NumberFormat.getInstance()
        numberFormat.maximumFractionDigits = 0
        numberFormat.roundingMode = RoundingMode.DOWN
        val sPrecision =
            numberFormat.format(Math.pow(10.0, dDecimales)).replace(".", "").replace(",", "")
        val Precision = sPrecision.toInt()
        val sRedondeo = numberFormat.format(dNumero * Precision).replace(".", "").replace(",", "")
        var Redondeo = sRedondeo.toInt()
        val Aux = Redondeo % 10
        Redondeo = if (Aux >= 0) {
            if (Aux >= 5) Redondeo + 10 - Aux else Redondeo - Aux
        } else {
            if (Aux <= -5) Redondeo - 10 - Aux else Redondeo - Aux
        }
        val fResultado: Double = Redondeo.toDouble() / Precision
        return fResultado
    }


    fun Redondear(dNumero: Float, iDecimales: Int): Float {
        val dDecimales = iDecimales * 1.0 + 1
        val numberFormat = NumberFormat.getInstance()
        numberFormat.maximumFractionDigits = 0
        numberFormat.roundingMode = RoundingMode.DOWN
        val sPrecision = numberFormat.format(Math.pow(10.0, dDecimales)).replace(".", "")
        val Precision = sPrecision.toInt()
        val sRedondeo = numberFormat.format((dNumero * Precision).toDouble()).replace(".", "")
        var Redondeo = sRedondeo.toInt()
        val Aux = Redondeo % 10
        Redondeo = if (Aux >= 0) {
            if (Aux >= 5) Redondeo + 10 - Aux else Redondeo - Aux
        } else {
            if (Aux <= -5) Redondeo - 10 - Aux else Redondeo - Aux
        }
        val fResultado: Float = Redondeo.toFloat() / Precision
        return fResultado
    }


    fun StringOfChar(cadena: String, numRepet: Int): String {
        var result = ""
        for (x in 0 until numRepet) {
            result += cadena
        }
        return result
    }

    // Establece el MaxLength de un EditText.
    fun editTextMaxLength(ed: EditText, maxLength: Int) {
        val filterArray = arrayOfNulls<InputFilter>(1)
        filterArray[0] = LengthFilter(maxLength)
        ed.filters = filterArray
    }


    fun logicoACadena(valor: Boolean): String {
        return if (valor) "T" else "F"
    }


    fun cadenaALogico(valor: String): Boolean {
        return valor.equals("T", ignoreCase = true)
    }

    fun modoVtaAsString(modoVta: String?): String {
        val sModoVta: String = when (modoVta) {
            "2" -> "Histórico"
            "3" -> "Catálogo visual"
            else -> "Modo lista"
        }
        return sModoVta
    }

    fun docDefectoAsString(fTipoDoc: Byte): String {
        val sTipoDoc: String = when (fTipoDoc) {
            2.toByte() -> "Albaran"
            3.toByte() -> "Pedido"
            4.toByte() -> "Presupuesto"
            else -> "Factura"
        }
        return sTipoDoc
    }


    fun tipoDocAsString(fTipoDoc: Short): String {
        val sTipoDoc: String = when (fTipoDoc) {
            1.toShort() -> "Factura"
            2.toShort() -> "Albaran"
            6.toShort() -> "Presupuesto"
            else -> "Pedido"
        }
        return sTipoDoc
    }


    fun tipoDocResumAsString(fTipoDoc: Short): String {
        val sTipoDoc: String = when (fTipoDoc) {
            1.toShort() -> "Fra"
            2.toShort() -> "Alb"
            6.toShort() -> "Prsp"
            else -> "Ped"
        }
        return sTipoDoc
    }


    fun nombreEstado(queEstado: String): String {
        return when (queEstado) {
            "0" -> "Importado"
            "N" -> "Nuevo"
            "M" -> "Modificado"
            "X" -> "Exportado"
            "R" -> "Reenviar"
            "P" -> "Guardado"
            else -> ""
        }
    }

    // Compruebo que el código del terminal sea una cadena con 3 cifras numéricas.
    fun codTerminalCorrecto(CodTerminal: String): Boolean {
        return if (CodTerminal.length != 3) false else {
            try {
                CodTerminal.toInt() > 0
            } catch (E: NumberFormatException) {
                false
            }
        }
    }

    fun ocultarTeclado(activity: Activity) {
        // Esto sí oculta el teclado de verdad.
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

/*
    @Throws(NoSuchAlgorithmException::class)
    fun sha1(input: String): String {
        val mDigest = MessageDigest.getInstance("SHA1")
        val result = mDigest.digest(input.toByteArray())
        val sb = StringBuffer()
        //for (i in result.indices) {
        //    sb.append(((result[i] and 0xff.toByte()) + 0x100).toString(16).substring(1))
        //}

        for (i in result.indices) {
            sb.append(((result[i] and 0xff.toByte()) + 0x100.toByte()).toString(16)).substring(1)
        }

        return sb.toString()
    }
*/

    fun byteArrayToString(bytes: ByteArray): String {
        val buffer = StringBuilder()
        for (b in bytes) {
            buffer.append(java.lang.String.format(Locale.getDefault(), "%02x", b))
        }
        return buffer.toString()
    }

    fun sha1(clearString: String): String {
        return try {
            val messageDigest = MessageDigest.getInstance("SHA-1")
            messageDigest.update(clearString.toByteArray(charset("UTF-8")))
            byteArrayToString(messageDigest.digest())

        } catch (ignored: Exception) {
            ignored.printStackTrace()
            ""
        }
    }

    fun hideStr(str: String): String {
        var i: Int
        var pass1 = ""
        var pass2 = ""
        if (str.trim { it <= ' ' } != "") {
            i = 0
            while (i < str.length) {
                pass1 =
                    if (str[i].toInt() >= 79) pass1 + (79 - (str[i].toInt() - 79)).toChar() else pass1 + (79 + (79 - str[i].toInt())).toChar()
                i++
            }
            i = 0
            while (i < pass1.length) {
                pass2 += (pass1[i].toInt() / 10 + 48).toChar()
                pass2 += (pass1[i].toInt() % 10 + 48).toChar()
                i++
            }
        }
        return pass2
    }


    fun dimeMiTipoDeArchivo(nombreDocumento: String): String {
        val tipo = ""
        val index = nombreDocumento.lastIndexOf('.') + 1
        val ext = nombreDocumento.substring(index).lowercase(Locale.getDefault())
        if (ext != "") {
            when (ext) {
                "jpg", "bmp", "png" -> return "image"
                "pdf" -> return "pdf"
                "doc", "docx" -> return "word"
                "mp4" -> return "video"
            }
        }
        return tipo
    }


    fun decodeBitmapDesdeFichero(queFichero: String, reqWidth: Int, reqHeight: Int): Bitmap {
        // Primero hacemos el decode con inJustDecodeBounds=true para chequear las dimensiones de la imagen
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(queFichero, options)

        // Calculamos inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Hacemos el decode sin establecer inSampleSize
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(queFichero, options)
    }


    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Alto y ancho reales de la imagen
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculamos el mayor valor para inSampleSize que sea potencia de 2.
            while (halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun DimeDiaSemana(queDia: Int): String {
        var nombreDia = ""
        when (queDia) {
            1 -> nombreDia = "DOMINGO"
            2 -> nombreDia = "LUNES"
            3 -> nombreDia = "MARTES"
            4 -> nombreDia = "MIERCOLES"
            5 -> nombreDia = "JUEVES"
            6 -> nombreDia = "VIERNES"
            7 -> nombreDia = "SABADO"
        }
        return nombreDia
    }

    fun DimeNombreMes(queMes: Int): String {
        var nombreMes = ""
        when (queMes) {
            0 -> nombreMes = "ENERO"
            1 -> nombreMes = "FEBRERO"
            2 -> nombreMes = "MARZO"
            3 -> nombreMes = "ABRIL"
            4 -> nombreMes = "MAYO"
            5 -> nombreMes = "JUNIO"
            6 -> nombreMes = "JULIO"
            7 -> nombreMes = "AGOSTO"
            8 -> nombreMes = "SEPTIEMBRE"
            9 -> nombreMes = "OCTUBRE"
            10 -> nombreMes = "NOVIEMBRE"
            11 -> nombreMes = "DICIEMBRE"
        }
        return nombreMes
    }


    fun DimeNombreMesAbrev(queMes: Int): String {
        var nombreMes = ""
        when (queMes) {
            1 -> nombreMes = "EN"
            2 -> nombreMes = "FB"
            3 -> nombreMes = "MZ"
            4 -> nombreMes = "AB"
            5 -> nombreMes = "MY"
            6 -> nombreMes = "JN"
            7 -> nombreMes = "JL"
            8 -> nombreMes = "AG"
            9 -> nombreMes = "SP"
            10 -> nombreMes = "OC"
            11 -> nombreMes = "NV"
            12 -> nombreMes = "DC"
        }
        return nombreMes
    }


    fun DimeNombreMesResum(queMes: Int): String {
        var nombreMes = ""
        when (queMes) {
            0 -> nombreMes = "Enero"
            1 -> nombreMes = "Febr."
            2 -> nombreMes = "Marzo"
            3 -> nombreMes = "Abril"
            4 -> nombreMes = "Mayo"
            5 -> nombreMes = "Junio"
            6 -> nombreMes = "Julio"
            7 -> nombreMes = "Agosto"
            8 -> nombreMes = "Sept."
            9 -> nombreMes = "Oct."
            10 -> nombreMes = "Nov."
            11 -> nombreMes = "Dic."
        }
        return nombreMes
    }

    fun NuevoAlertBuilder(
        activity: Activity,
        queTitulo: String,
        queMensaje: String,
        conBotonNo: Boolean
    ): AlertDialog.Builder {
        val aldDialog = AlertDialog.Builder(activity)
        aldDialog.setTitle(Html.fromHtml("<font color='" + activity.resources.getColor(R.color.texto_botones) + "'>" + queTitulo + "</font>"))
        aldDialog.setMessage(queMensaje)
        aldDialog.setIcon(R.drawable.mensaje)
        aldDialog.setCancelable(false)
        if (conBotonNo) {
            aldDialog.setNegativeButton(
                activity.resources.getString(R.string.dlg_no)
            ) { dialog: DialogInterface, which: Int -> dialog.cancel() }
        }
        return aldDialog
    }


    fun ColorDividerAlert(activity: Activity, alert: AlertDialog) {
        // Color para el divider del AlertDialog. Hay que asignarlo después de hacer el alert.show().
        val dividerId = alert.context.resources.getIdentifier("android:id/titleDivider", null, null)
        if (dividerId != 0) {
            val divider = alert.findViewById<View>(dividerId)
            divider?.setBackgroundColor(activity.resources.getColor(R.color.gris_alba))
        }
    }

    // No podremos recibir siempre que comuniquemos vía wifi o ftp y:
// - tengamos documentos o cobros pendientes de enviar
// - tengamos cargas pendientes de enviar
// - la fecha del último envío desde el terminal sea mayor que la de la última preparación desde el PC. Esto se comprueba
// en MiscComunicaciones.xmlABaseDatos()
    fun puedoRecibir(activity: Activity): Boolean {
        val bd = BaseDatos(activity)

        // Puede ocurrir que no tengamos aún la base de datos creada, por eso tenemos que controlar la excepción.
        try {
            bd.writableDatabase.use { dbAlba ->
                var cCursor = dbAlba.rawQuery(
                    "SELECT _id FROM cabeceras WHERE estado = 'N' OR estado = 'R' OR estado = 'P'",
                    null
                )
                return if (cCursor.moveToFirst()) {
                    false
                } else {
                    // Comprobamos que no tenemos cobros realizados
                    cCursor.close()
                    val cobrosDao: CobrosDao? = MyDatabase.getInstance(activity)?.cobrosDao()
                    val queId = cobrosDao?.hayCobros() ?: 0
                    val hayCobros = (queId > 0)

                    if (hayCobros) {
                        false
                    } else {
                        cCursor = dbAlba.rawQuery("SELECT cargaId FROM cargas WHERE estado = 'N' OR estado = 'R'", null)
                        val hayCargas = cCursor.moveToFirst()
                        cCursor.close()
                        !hayCargas
                    }
                }
            }
        } catch (e: Exception) {
            return true
        } finally {
            bd.close()
        }
    }


    fun actualizarSaldo(fContexto: Context, queCliente: Int, queEmpresa: Short, queImporte: Double) {
        val saldosDao: SaldosDao? = MyDatabase.getInstance(fContexto)?.saldosDao()
        val clteSaldo = saldosDao?.existeSaldo(queCliente) ?: 0

        if (clteSaldo > 0)
            saldosDao?.actualizarSaldo(queCliente, queEmpresa, queImporte)
        else
            saldosDao?.insertarSaldo(queCliente, queEmpresa, queImporte)
    }



// ====================
// Utilidades
// ====================
// Códigos para la impresora Star DP8340
    private val caracteresEspeciales = "ÜüÁáÉéÍíÓóÚúÑñ"
    private val codigoCaracteresEspeciales = byteArrayOf(
        0xA2.toByte(),
        0xBE.toByte(),
        0x41.toByte(),
        0xCE.toByte(),
        0x45.toByte(),
        0xB0.toByte(),
        0x49.toByte(),
        0xB5.toByte(),
        0x4F.toByte(),
        0xBA.toByte(), 0x55.toByte(), 0xBF.toByte(), 0x5C.toByte(), 0x7C.toByte()
    )


    fun stringABytes(s: String?): ByteArray? {
        var l: Int
        var i_especial: Int
        var b: Byte
        var s_sub: String
        if (s == null) return null
        if (s.length.also { l = it } < 1) return ByteArray(0)

        // Convertimos a byte carácter por carácter
        val b_arr: ByteArray = ByteArray(l)
        var i: Int = 0
        while (i < l) {
            s_sub = s.substring(i, i + 1)
            i_especial = caracteresEspeciales.indexOf(s_sub)
            b =
                if (i_especial < 0) s_sub.toByteArray()[0] else codigoCaracteresEspeciales[i_especial]
            b_arr[i] = b
            i++
        }
        return b_arr
    }


    fun fechaEnJulian(queFecha: String): String {
        val queAnyo = queFecha.substring(6, 10)
        val queMes = queFecha.substring(3, 5)
        val queDia = queFecha.substring(0, 2)
        return "$queAnyo-$queMes-$queDia"
    }



    fun whatsappInstalado(context: Context): Boolean {
        val pm: PackageManager = context.packageManager
        return try {
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            // Para Whatsapp business
            //pm.getPackageInfo("com.whatsapp.w4b", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }



    fun enviarPorWhatsapPdf(context: Context, nombreFichero: String, numeroTelefono: String) {

        val queFichero = File(nombreFichero)
        if (queFichero.exists()) {
            try {
                val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", queFichero)

                val sendIntent = Intent()
                sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
                sendIntent.type = "application/pdf"
                sendIntent.putExtra("jid", "$numeroTelefono@s.whatsapp.net")
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.setPackage("com.whatsapp")
                sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(context, sendIntent, null)

            } catch (e: Exception) {
                Toast.makeText(context, "El dispositivo no tiene instalado WhatsApp", Toast.LENGTH_LONG).show()
            }
        }
    }



