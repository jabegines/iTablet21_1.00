package es.albainformatica.albamobileandroid

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceManager
import es.albainformatica.albamobileandroid.dao.CabecerasDao
import es.albainformatica.albamobileandroid.dao.CargasDao
import es.albainformatica.albamobileandroid.dao.CobrosDao
import es.albainformatica.albamobileandroid.dao.SaldosDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.database.MyDatabase.Companion.queBDRoom
import java.io.File
import java.math.RoundingMode
import java.security.MessageDigest
import java.text.NumberFormat
import java.util.*

import kotlin.math.pow


fun dimeRutaImagenes(activity: Activity): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val usarMultisistema = prefs.getBoolean("usar_multisistema", false)
        val rutaImagenes = prefs.getString("rutacomunicacion", "") ?: ""
        val carpetaImagenes: String = if (rutaImagenes == "") {
            if (usarMultisistema) "/storage/sdcard0/alba/imagenes/$queBDRoom/"
            else "/storage/sdcard0/alba/imagenes/"
        } else {
            if (usarMultisistema) "$rutaImagenes/imagenes/$queBDRoom/" else "$rutaImagenes/imagenes/"
        }
        return carpetaImagenes
    }


    fun dimeRutaDocAsoc(activity: Activity): String {
        val pref = PreferenceManager.getDefaultSharedPreferences(activity)
        val usarMultisistema = pref.getBoolean("usar_multisistema", false)
        val rutaDocAsoc = pref.getString("rutacomunicacion", "") ?: ""
        val carpetaDocAsoc: String = if (rutaDocAsoc == "") {
            if (usarMultisistema) "/storage/sdcard0/alba/docasociados/$queBDRoom/" else "/storage/sdcard0/alba/docasociados/"
        } else {
            if (usarMultisistema) "$rutaDocAsoc/docasociados/$queBDRoom/" else "$rutaDocAsoc/docasociados/"
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


fun redondear(dNumero: Double, iDecimales: Int): Double {
    val dDecimales = iDecimales * 1.0 + 1
    val numberFormat = NumberFormat.getInstance()
    numberFormat.maximumFractionDigits = 0
    numberFormat.roundingMode = RoundingMode.DOWN
    val sPrecision =
        numberFormat.format(10.0.pow(dDecimales)).replace(".", "").replace(",", "")
    val precision = sPrecision.toInt()
    val sRedondeo = numberFormat.format(dNumero * precision).replace(".", "").replace(",", "")
    var redondeo = sRedondeo.toInt()
    val aux = redondeo % 10
    redondeo = if (aux >= 0) {
        if (aux >= 5) redondeo + 10 - aux else redondeo - aux
    } else {
        if (aux <= -5) redondeo - 10 - aux else redondeo - aux
    }
    return redondeo.toDouble() / precision
}


fun redondear(dNumero: Float, iDecimales: Int): Float {
    val dDecimales = iDecimales * 1.0 + 1
    val numberFormat = NumberFormat.getInstance()
    numberFormat.maximumFractionDigits = 0
    numberFormat.roundingMode = RoundingMode.DOWN
    val sPrecision = numberFormat.format(10.0.pow(dDecimales)).replace(".", "")
    val precision = sPrecision.toInt()
    val sRedondeo = numberFormat.format((dNumero * precision).toDouble()).replace(".", "")
    var redondeo = sRedondeo.toInt()
    val aux = redondeo % 10
    redondeo = if (aux >= 0) {
        if (aux >= 5) redondeo + 10 - aux else redondeo - aux
    } else {
        if (aux <= -5) redondeo - 10 - aux else redondeo - aux
    }
    return redondeo.toFloat() / precision
}


    fun stringOfChar(cadena: String, numRepet: Int): String {
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
            "2" -> "Hist??rico"
            "3" -> "Cat??logo visual"
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
            3.toShort() -> "Ped"
            6.toShort() -> "Prsp"
            else -> ""
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

    // Compruebo que el c??digo del terminal sea una cadena con 3 cifras num??ricas.
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
        // Esto s?? oculta el teclado de verdad.
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


    fun sha512(clearString: String): String {
        return try {
            val messageDigest = MessageDigest.getInstance("SHA-512")
            messageDigest.update(clearString.toByteArray(charset("UTF-8")))
            byteArrayToString(messageDigest.digest())
        } catch (ignored: Exception) {
            ignored.printStackTrace()
            ""
        }
    }

/*
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
*/

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

    fun dimeDiaSemana(queDia: Int): String {
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

    fun dimeNombreMes(queMes: Int): String {
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


    fun dimeNombreMesAbrev(queMes: Int): String {
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


    fun dimeNombreMesResum(queMes: Int): String {
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


    fun nuevoAlertBuilder(activity: Activity, queTitulo: String, queMensaje: String, conBotonNo: Boolean): AlertDialog.Builder {
        val aldDialog = AlertDialog.Builder(activity)
        aldDialog.setTitle(HtmlCompat.fromHtml("<font color='" +
                ContextCompat.getColor(activity, R.color.texto_botones) + "'>" + queTitulo + "</font>",
                HtmlCompat.FROM_HTML_MODE_LEGACY))
        aldDialog.setMessage(queMensaje)
        aldDialog.setIcon(R.drawable.mensaje)
        aldDialog.setCancelable(false)
        if (conBotonNo) {
            aldDialog.setNegativeButton(activity.resources.getString(R.string.dlg_no)) { dialog: DialogInterface, _: Int -> dialog.cancel() }
        }
        return aldDialog
    }


    // No podremos recibir siempre que comuniquemos v??a wifi o ftp y:
    // - tengamos documentos o cobros pendientes de enviar
    // - tengamos cargas pendientes de enviar
    // - la fecha del ??ltimo env??o desde el terminal sea mayor que la de la ??ltima preparaci??n desde el PC. Esto se comprueba
    // en MiscComunicaciones.xmlABaseDatos()
    fun puedoRecibir(activity: Activity): Boolean {

        // Puede ocurrir que no tengamos a??n la base de datos creada, por eso tenemos que controlar la excepci??n.
        try {
            val cabecerasDao: CabecerasDao? = MyDatabase.getInstance(activity)?.cabecerasDao()
            val lCabeceras = cabecerasDao?.getPdtesEnviar() ?: emptyList<Int>().toMutableList()

                return if (lCabeceras.isNotEmpty()) {
                    false
                } else {
                    // Comprobamos que no tenemos cobros realizados
                    val cobrosDao: CobrosDao? = MyDatabase.getInstance(activity)?.cobrosDao()
                    val queId = cobrosDao?.hayCobros() ?: 0
                    val hayCobros = (queId > 0)

                    if (hayCobros) {
                        false
                    } else {
                        val cargasDao: CargasDao? = MyDatabase.getInstance(activity)?.cargasDao()
                        val lCargas = cargasDao?.getPdtesEnviar() ?: emptyList<Int>().toMutableList()

                        val hayCargas = lCargas.isNotEmpty()
                        !hayCargas
                    }
                }

        } catch (e: Exception) {
            return true
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
// C??digos para la impresora Star DP8340
    private const val caracteresEspeciales = "????????????????????????????"
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


    fun stringABytes(s: String): ByteArray {
        var l: Int
        var iEspecial: Int
        var b: Byte
        var sSub: String

        if (s.length.also { l = it } < 1) return ByteArray(0)

        // Convertimos a byte car??cter por car??cter
        val bArr = ByteArray(l)
        var i = 0
        while (i < l) {
            sSub = s.substring(i, i + 1)
            iEspecial = caracteresEspeciales.indexOf(sSub)
            b = if (iEspecial < 0) sSub.toByteArray()[0] else codigoCaracteresEspeciales[iEspecial]
            bArr[i] = b
            i++
        }
        return bArr
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



