package es.albainformatica.albamobileandroid

import es.albainformatica.albamobileandroid.dao.*
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.ConfiguracionEnt
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by jabegines on 10/10/13.
 */
class Configuracion(queContexto: Context) {
    private val configuracionDao: ConfiguracionDao? = MyDatabase.getInstance(queContexto)?.configuracionDao()
    private val seriesDao: SeriesDao? = MyDatabase.getInstance(queContexto)?.seriesDao()
    private val ejerciciosDao: EjerciciosDao? = MyDatabase.getInstance(queContexto)?.ejerciciosDao()
    private val docsCabPiesDao: DocsCabPiesDao? = MyDatabase.getInstance(queContexto)?.docsCabPiesDao()
    private val empresasDao: EmpresasDao? = MyDatabase.getInstance(queContexto)?.empresasDao()
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(queContexto)
    private val fContext: Context = queContexto
    var fTamanyoPantLargo = true


    fun rutaLocalComunicacion(): String {

        val fCodTablet = prefs.getString("tablet", "") ?: ""
        return if (fCodTablet != "") {
            val fSistema = prefs.getString("sistema", "00") ?: "00"
            val optionalPath = fContext.getExternalFilesDir(null)?.absolutePath ?: ""
            // Extraemos de optionalPath la parte que no nos interesa
            val extraPortion = ("/Android/data/es.albainformatica.albamobileandroid/files")
            var queResultado = optionalPath.replace(extraPortion, "")
            queResultado = "$queResultado/alba/$fSistema/$fCodTablet"

            queResultado

        } else ""
    }


    fun codTerminal(): String {
        return configuracionDao?.getValor(100) ?: ""
    }

    fun nombreTerminal(): String {
        return configuracionDao?.getValor(101) ?: ""
    }

    fun vendedor(): String {
        return configuracionDao?.getValor(102) ?: ""
    }

    fun nombreVendedor(): String {
        return configuracionDao?.getValor(103) ?: ""
    }

    fun almacen(): Short {
        val queValor = configuracionDao?.getValor(104) ?: "0"
        return queValor.toShort()
    }


    fun nombreAlmacen(): String {
        return configuracionDao?.getValor(105) ?: ""
    }


    fun decimalesCantidad(): Int {
        val sDecimales = configuracionDao?.getValor(107) ?: "0"
        return (sDecimales.toInt())
    }

    fun formatoDecCantidad(): String {
        val sDecimales = configuracionDao?.getValor(107) ?: ""

        return if (sDecimales != "") "%." + sDecimales + "f"
        else "%.2f"
    }

    fun formatoDecPrecioBase(): String {
        val sDecimales = configuracionDao?.getValor(108) ?: ""

        return if (sDecimales != "") "%." + sDecimales + "f"
        else "%.2f"
    }

    fun formatoDecPrecioIva(): String {
        val sDecimales = configuracionDao?.getValor(109) ?: ""

        return if (sDecimales != "") "%." + sDecimales + "f"
        else "%.2f"
    }

    fun formatoDecImptesBase(): String {
        val sDecimales = configuracionDao?.getValor(110) ?: ""

        return if (sDecimales != "") "%." + sDecimales + "f"
        else "%.2f"
    }

    fun formatoDecImptesIva(): String {
        val sDecimales = configuracionDao?.getValor(111) ?: ""

        return if (sDecimales != "") "%." + sDecimales + "f"
        else "%.2f"
    }

    fun decimalesPrecioBase(): Int {
        val sDecimales = configuracionDao?.getValor(108) ?: "2"
        return sDecimales.toInt()
    }

    fun decimalesPrecioIva(): Int {
        val sDecimales = configuracionDao?.getValor(109) ?: "2"
        return sDecimales.toInt()
    }

    fun decimalesImpBase(): Int {
        val sDecimales = configuracionDao?.getValor(110) ?: "2"
        return sDecimales.toInt()
    }

    fun decimalesImpII(): Int {
        val sDecimales = configuracionDao?.getValor(111) ?: "2"
        return sDecimales.toInt()
    }

    fun tarifaVentas(): Short {
        val queTarifa = configuracionDao?.getValor(112) ?: "1"
        return queTarifa.toShort()
    }

    fun tarifaDto(): Short {
        val queTarifa = configuracionDao?.getValor(113) ?: "0"
        return queTarifa.toShort()
    }

    fun enterosCajas(): Int {
        val queValor = configuracionDao?.getValor(116) ?: "0"
        return queValor.toInt()
    }


    fun ejercicio(): Short {
        // A partir de ahora (compilación 50) obtendremos el ejercicio de forma automática, a través de la fecha de la tablet
        // Obtenemos la fecha actual
        val tim = System.currentTimeMillis()
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fFecha = df.format(tim)

        if ((ejerciciosDao?.hayDatosEjercicioActual(fFecha) ?: 0) > 0) {
            return ejerciciosDao?.getEjercicioActual(fFecha) ?: 0
        }
        else return -1
    }

    fun hayReparto(): Boolean {
        val queReparto = configuracionDao?.getValor(125) ?: "F"
        return (queReparto == "T")
    }


    fun idReparto(): String {
        return configuracionDao?.getValor(126) ?: ""
    }


    fun nombreReparto(): String {
        return configuracionDao?.getValor(127) ?: ""
    }


    fun hayAutoventa(): Boolean {
        val queAutoventa = configuracionDao?.getValor(128) ?: "F"
        val quePreventa = configuracionDao?.getValor(129) ?: "F"
        val bAutoventa = (queAutoventa == "T")
        val bPreventa = (quePreventa == "T")

        // Si hayReparto, hayAutoventa y hayPreventa son los tres 'F' entonces entenderemos que hayAutoventa y hayPreventa son 'T'
        if (!bAutoventa && !bPreventa && !hayReparto())
            return true

        return bAutoventa
    }

    fun hayPreventa(): Boolean {
        val queAutoventa = configuracionDao?.getValor(128) ?: "F"
        val quePreventa = configuracionDao?.getValor(129) ?: "F"
        val bAutoventa = (queAutoventa == "T")
        val bPreventa = (quePreventa == "T")

        // Si hayReparto, hayAutoventa y hayPreventa son los tres 'F' entonces entenderemos que hayAutoventa y hayPreventa son 'T'
        if (!bAutoventa && !bPreventa && !hayReparto())
            return true

        return bPreventa
    }

    fun carpetaImportFTP(): String {
        var queValor = configuracionDao?.getValor(154) ?: ""
        if (queValor == "") queValor = "import"

        return queValor
    }

    fun carpetaExportFTP(): String {
        var queValor = configuracionDao?.getValor(155) ?: ""
        if (queValor == "") queValor = "export"

        return queValor
    }

    fun emailResumPedidos(): String {
        return configuracionDao?.getValor(156) ?: ""
    }


    fun usarRutero(): Boolean {
        val queValor = configuracionDao?.getValor(205) ?: "F"
        return (queValor == "T")
    }

    fun verProveedores(): Boolean {
        val queValor = configuracionDao?.getValor(206) ?: "F"
        return (queValor == "T")
    }


    fun hayElaboracionLacteos(): Boolean {
        val queValor = configuracionDao?.getValor(210) ?: "F"
        return (queValor == "T")
    }

    fun activarRuta(fRuta: Short) {
        val values = ContentValues()
        values.put("valor", fRuta)
        // Este valor no lo recibimos de gestión, por eso comprobamos si existe para, si no, dar de alta.
        val queValor = configuracionDao?.getValor(300) ?: ""
        if (queValor == "") {
            val configEnt = ConfiguracionEnt()
            configEnt.grupo = 300
            configEnt.descripcion = "Ruta activa"
            configEnt.valor = fRuta.toString()
            configuracionDao?.insertar(configEnt)
        } else if (queValor != fRuta.toString()) {
            configuracionDao?.actualizar(fRuta.toString(), 300)
        }
    }

    fun rutaActiva(): Short {
        return configuracionDao?.getValor(300)?.toShort() ?: 0
    }

    fun pedirTarifa(): Boolean {
        val queValor = configuracionDao?.getValor(400) ?: "F"
        return (queValor ==  "T")
    }

    fun pedirCajas(): Boolean {
        val queValor = configuracionDao?.getValor(401) ?: "F"
        return (queValor == "T")
    }

    fun pedirDtos(): Boolean {
        val queValor = configuracionDao?.getValor(402) ?: "F"
        return (queValor == "T")
    }

    fun dtosPie(): Boolean {
        val queValor = configuracionDao?.getValor(403) ?: "F"
        return (queValor == "T")
    }

    fun pedirCobrosVtos(): Boolean {
        val queValor = configuracionDao?.getValor(404) ?: "F"
        return (queValor == "T")
    }

    fun modificarPrecios(): Boolean {
        val queValor = configuracionDao?.getValor(405) ?: "F"
        return (queValor == "T")
    }

    fun aconsUnidCajaModif(): Boolean {
        val queValor = configuracionDao?.getValor(406) ?: "F"
        return (queValor == "T")
    }

    fun noModifCantidad(): Boolean {
        val queValor = configuracionDao?.getValor(407) ?: "F"
        return (queValor == "T")
    }

    fun pedirAlmPorLinPresup(): Boolean {
        val queValor = configuracionDao?.getValor(408) ?: "F"
        return (queValor == "T")
    }

    fun usarHcoPorArticulo(): Boolean {
        val queValor = configuracionDao?.getValor(409) ?: "F"
        return (queValor == "T")
    }


    fun pedirEntregasAlbaranes(): Boolean {
        val queValor = configuracionDao?.getValor(410) ?: "F"
        return (queValor == "T")
    }

    /*
    fun modificarVentas(): Boolean {
        val queValor = configuracionDao?.getValor(501) ?: "F"
        return (queValor == "T")
    }
    */

    fun hacerEntrCta(): Boolean {
        val queValor = configuracionDao?.getValor(500) ?: "F"
        return (queValor == "T")
    }

    fun hacerFacturas(): Boolean {
        val queValor = configuracionDao?.getValor(502) ?: "F"
        return (queValor == "T")
    }

    fun hacerAlbaranes(): Boolean {
        val queValor = configuracionDao?.getValor(503) ?: "F"
        return (queValor == "T")
    }

    fun hacerPedidos(): Boolean {
        val queValor = configuracionDao?.getValor(504) ?: "F"
        return (queValor == "T")
    }

    fun hacerPresup(): Boolean {
        val queValor = configuracionDao?.getValor(505) ?: "F"
        return (queValor == "T")
    }

    fun noVenderNeg(): Boolean {
        val queValor = configuracionDao?.getValor(506) ?: "F"
        return (queValor == "T")
    }

    fun cobrosPorAlbaran(): Boolean {
        val queValor = configuracionDao?.getValor(507) ?: "F"
        return (queValor == "T")
    }

    fun usarTarifaClte(): Boolean {
        val queValor = configuracionDao?.getValor(601) ?: "F"
        return (queValor == "T")
    }


    //fun ivaIncluido(): Boolean {
    //    val queValor = configuracionDao?.getValor(602) ?: "F"
    //    return (queValor == "T")
    //}

    fun ivaIncluido(queEmpresa: Short): Boolean {
        val queValor = empresasDao?.getIvaIncluido(queEmpresa.toInt()) ?: "F"
        return (queValor == "T")
    }

    fun aconsNomComercial(): Boolean {
        val queValor = configuracionDao?.getValor(604) ?: "F"
        return (queValor == "T")
    }

    fun controlarStock(): Boolean {
        val queValor = configuracionDao?.getValor(608) ?: "F"
        return (queValor == "T")
    }

    fun usarTrazabilidad(): Boolean {
        val queValor = configuracionDao?.getValor(609) ?: "F"
        return (queValor == "T")
    }

    fun usarPiezas(): Boolean {
        val queValor = configuracionDao?.getValor(610) ?: "F"
        return (queValor == "T")
    }

    fun sumarStockEmpresas(): Boolean {
        val queValor = configuracionDao?.getValor(640) ?: "F"
        return (queValor == "T")
    }

    fun pvpHistorico(): Boolean {
        val queValor = configuracionDao?.getValor(611) ?: "F"
        return (queValor == "T")
    }

    fun usarOfertas(): Boolean {
        val queValor = configuracionDao?.getValor(612) ?: "F"
        return (queValor == "T")
    }

    fun usarRating(): Boolean {
        val queValor = configuracionDao?.getValor(614) ?: "F"
        return (queValor == "T")
    }

    fun predominaRating(): Boolean {
        val queValor = configuracionDao?.getValor(615) ?: "F"
        return (queValor == "T")
    }

    fun pedirIncidLineas(): Boolean {
        val queValor = configuracionDao?.getValor(617) ?: "F"
        return (queValor == "T")
    }

    fun avisoLotes(): Boolean {
        val queValor = configuracionDao?.getValor(618) ?: "F"
        return (queValor == "T")
    }

    fun aplicarPvpMasVent(): Boolean {
        val queValor = configuracionDao?.getValor(619) ?: "F"
        return (queValor == "T")
    }

    fun usarFormatos(): Boolean {
        val queValor = configuracionDao?.getValor(620) ?: "F"
        return (queValor == "T")
    }

    fun cltesContrFechas(): Boolean {
        val queValor = configuracionDao?.getValor(622) ?: "F"
        return (queValor == "T")
    }

    fun cltesContrFactPdtes(): Boolean {
        val queValor = configuracionDao?.getValor(623) ?: "F"
        return (queValor == "T")
    }

    fun cltesMaxDiasRiesgo(): Int {
        val queValor = configuracionDao?.getValor(624) ?: "0"
        return queValor.toInt()
    }

    fun cltesMaxFrasRiesgo(): Int {
        val queValor = configuracionDao?.getValor(625) ?: "0"
        return queValor.toInt()
    }

    fun contrFechasSiempre(): Boolean {
        val queValor = configuracionDao?.getValor(626) ?: "F"
        return (queValor == "T")
    }

    fun venderRiesgoSuperado(): Boolean {
        val queValor = configuracionDao?.getValor(627) ?: "F"
        return (queValor == "T")
    }

    fun noCambiarFPago(): Boolean {
        val queValor = configuracionDao?.getValor(628) ?: "F"
        return (queValor == "T")
    }


    fun altaDeClientes(): Boolean {
        val queValor = configuracionDao?.getValor(629) ?: "F"
        return (queValor == "T")
    }

    fun diasAlertaArtNoVend(): Int {
        val queValor = configuracionDao?.getValor(630) ?: "0"
        return queValor.toInt()
    }

    fun igualarCantArtEnlace(): Boolean {
        val queValor = configuracionDao?.getValor(631) ?: "F"
        return (queValor == "T")
    }

    fun diasMantCobros(): Int {
        val queValor = configuracionDao?.getValor(632) ?: "0"
        return queValor.toInt()
    }

    fun pedirFormato(): Boolean {
        val queValor = configuracionDao?.getValor(707) ?: "F"
        return (queValor == "T")
    }


    fun proximoIDClte(): Int {
        val queValor = configuracionDao?.getValor(916) ?: "0"
        return queValor.toInt()
    }


    fun nombreTasa1(): String {
        return configuracionDao?.getValor(923) ?: ""
    }

    fun nombreTasa2(): String {
        return configuracionDao?.getValor(924) ?: ""
    }


    fun lineasCabDocZebra(queEmpresa: Short, lineasDobles: String): String {
        val fCR = 13.toChar().toString()
        val fLF = 10.toChar().toString()
        val resultado = StringBuilder()
        var posicion = 80

        val lLineas = docsCabPiesDao?.lineasCabDoc(queEmpresa) ?: emptyList<String>().toMutableList()

        for (x in lLineas) {
            resultado.append("^FT0,").append(posicion).append("^AKN,15").append(fCR).append(fLF)
            resultado.append("^FD").append(x).append("^FS").append(fCR).append(fLF)
            posicion += 20
        }
        resultado.append("^FT0,").append(posicion).append("^AKN,12").append(fCR).append(fLF)
        resultado.append("^FD").append(lineasDobles).append("^FS").append(fCR).append(fLF)
        return resultado.toString()
    }

    fun lineasCabDocConMargIzq(queEmpresa: Short, queMargenIzq: String): String {
        val resultado = java.lang.StringBuilder()
        val lLineas = docsCabPiesDao?.lineasCabDoc(queEmpresa) ?: emptyList<String>().toMutableList()

        for (x in lLineas) {
            if (resultado.toString() != "") resultado.append("\n")
            resultado.append(queMargenIzq).append(x)
        }

        return resultado.toString()
    }

    fun lineasCabDoc(queEmpresa: Short): String {
        val lLineas = docsCabPiesDao?.lineasCabDoc(queEmpresa) ?: emptyList<String>().toMutableList()

        val resultado = java.lang.StringBuilder()
        for (x in lLineas) {
            if (resultado.toString() != "") resultado.append("\n")
            resultado.append(x)
        }

        return resultado.toString()
    }



    fun lineasPieDoc(queEmpresa: Short): String {
        val lLineas = docsCabPiesDao?.lineasPieDoc(queEmpresa) ?: emptyList<String>().toMutableList()

        val resultado = java.lang.StringBuilder()
        for (x in lLineas) {
            if (resultado.toString() != "") resultado.append("\n")
            resultado.append(x)
        }

        return resultado.toString()
    }


    fun lineasPieDocZebra(queEmpresa: Short): String {
        val fCR = 13.toChar().toString()
        val fLF = 10.toChar().toString()
        val resultado = java.lang.StringBuilder()
        var posicion = 0
        var primeraLinea = true

        val lLineas = docsCabPiesDao?.lineasPieDoc(queEmpresa) ?: emptyList<String>().toMutableList()

        for (x in lLineas) {
            resultado.append("^FT0,").append(posicion).append("^AKN,20").append(fCR).append(fLF)
            resultado.append("^FD").append(x).append("^FS").append(fCR).append(fLF)
            if (primeraLinea) {
                posicion += 35
                primeraLinea = false
            } else posicion += 20
        }
        return resultado.toString()
    }



    fun lineaCabDoc(queEmpresa: Short, queLinea: Short): String {
        return docsCabPiesDao?.lineaCabeceraDoc(queEmpresa, queLinea) ?: ""
    }


    fun lineaPieDoc(queEmpresa: Short, queLinea: Short): String {
        return docsCabPiesDao?.lineaPieDoc(queEmpresa, queLinea) ?: ""
    }




    fun imprimir(): Boolean {
        val queValor = configuracionDao?.getValor(701) ?: "F"
        return (queValor == "T")
    }

    fun impresora(): Int {
        val queImpresora = configuracionDao?.getValor(702) ?: ""
        return when {
            queImpresora.uppercase(Locale.getDefault()) == "STARTDP8340S" -> IMPRESORA_STARTDP8340S
            queImpresora.uppercase(Locale.getDefault()) == "STARDP8340" -> IMPRESORA_STARTDP8340S
            queImpresora.uppercase(Locale.getDefault()) == "EPSONTMU295" -> IMPRESORA_EPSONTMU295
            queImpresora.uppercase(Locale.getDefault()) == "DATAMAX APEX 2" -> IMPRESORA_DATAMAX_APEX_2
            queImpresora.uppercase(Locale.getDefault()) == "INTERMEC PB51" -> IMPRESORA_INTERMEC_PB51
            queImpresora.uppercase(Locale.getDefault()) == "BIXOLON SPP-R410" -> IMPRESORA_BIXOLON_SPP_R410
            queImpresora.uppercase(Locale.getDefault()) == "GENERICA_110" -> IMPRESORA_GENERICA_110
            queImpresora.uppercase(Locale.getDefault()) == "GENERICA_80" -> IMPRESORA_GENERICA_80
            queImpresora.uppercase(Locale.getDefault()) == "ZEBRA_80" -> IMPRESORA_ZEBRA_80
            else -> 0
        }
    }


    fun dtoPie1(): Boolean {
        val queValor = configuracionDao?.getValor(902) ?: "F"
        return (queValor == "T")
    }

    fun dtoPie2(): Boolean {
        val queValor = configuracionDao?.getValor(903) ?: "F"
        return (queValor == "T")
    }

    fun dtoPie3(): Boolean {
        val queValor = configuracionDao?.getValor(904) ?: "F"
        return (queValor == "T")
    }

    fun dtoPie4(): Boolean {
        val queValor = configuracionDao?.getValor(905) ?: "F"
        return (queValor == "T")
    }

    fun claveUsuario(): String {
        return configuracionDao?.getDescripcion(910) ?: ""
    }

    fun dtosCascada(): Boolean {
        val queValor = configuracionDao?.getValor(911) ?: "F"
        return (queValor == "T")
    }

    fun usarCargas(): Boolean {
        val queValor = configuracionDao?.getValor(912) ?: "F"
        return (queValor == "T")
    }

    fun dtosCltesSoloFact(): Boolean {
        val queValor = configuracionDao?.getValor(913) ?: "F"
        return (queValor == "T")
    }

    fun claveSupervisor(): String {
        return configuracionDao?.getDescripcion(914) ?: ""
    }

    fun activarFirmaDigital(): Boolean {
        val queValor = configuracionDao?.getValor(919) ?: "F"
        return (queValor == "T")
    }

    fun usarTasa1(): Boolean {
        val queValor = configuracionDao?.getValor(920) ?: "F"
        return (queValor == "T")
    }

    fun usarTasa2(): Boolean {
        val queValor = configuracionDao?.getValor(921) ?: "F"
        return (queValor == "T")
    }

    fun tarifaCajas(): Short {
        val queTarifa = configuracionDao?.getValor(925) ?: "1"
        return queTarifa.toShort()
    }

    fun getSiguCodClte(): String {
        return configuracionDao?.getValor(605) ?: ""
    }

    fun setSiguCodClte(queCodClte: Int) {
        val iSigCodigo = queCodClte + 1

        if (getSiguCodClte() == "") {
            val fConfEnt = ConfiguracionEnt()
            fConfEnt.grupo = 605
            fConfEnt.descripcion = "Siguiente código cliente"
            fConfEnt.valor = iSigCodigo.toString()
            configuracionDao?.insertar(fConfEnt)
        }
        else configuracionDao?.actualizar(iSigCodigo.toString(), 605)
    }

    /*
    fun proximoIdClte(): Int {
        val queId = configuracionDao?.getValor(916) ?: "0"
        return queId.toInt()
    }
    */

    fun setProximoIdClte(queIdClte: Int) {
        configuracionDao?.actualizar(queIdClte.toString(), 916)
    }

    fun codigoProducto(): String {
        return configuracionDao?.getValor(917) ?: ""
    }

    fun servidorFTP(): String {
        return configuracionDao?.getValor(151) ?: ""
    }
    fun usuarioFTP(): String {
        return configuracionDao?.getValor(152) ?: ""
    }
    fun passwordFTP(): String {
        return configuracionDao?.getValor(153) ?: ""
    }

    fun fechaInicio(): Date? {
        val queFecha = configuracionDao?.getValor(121) ?: ""
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        return try { formato.parse(queFecha)
        } catch (ex: ParseException) {
            ex.printStackTrace()
            null
        }
    }

    fun fechaFin(): Date? {
        val queFecha = configuracionDao?.getValor(122) ?: ""
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        return try { formato.parse(queFecha)
        } catch (ex: ParseException) {
            ex.printStackTrace()
            null
        }
    }


    fun getNumero(queSerie: String, queEjercicio: Short, queTipoDoc: Short): Int {
        return when (queTipoDoc) {
            2.toShort() -> seriesDao?.getNumAlbaran(queSerie, queEjercicio.toInt()) ?: 0
            3.toShort() -> seriesDao?.getNumPedido(queSerie, queEjercicio.toInt()) ?: 0
            6.toShort() -> seriesDao?.getNumPresupuesto(queSerie, queEjercicio.toInt()) ?: 0
            else -> seriesDao?.getNumFactura(queSerie, queEjercicio.toInt()) ?: 0
        }
    }


    fun actualizarNumero(queSerie: String, queEjercicio: Short, queTipoDoc: Byte, fNumero: Int) {
        when (queTipoDoc) {
            2.toByte() -> seriesDao?.setNumAlbaran(queSerie, queEjercicio, fNumero + 1)
            3.toByte() -> seriesDao?.setNumPedido(queSerie, queEjercicio, fNumero + 1)
            6.toByte() -> seriesDao?.setNumPresupuesto(queSerie, queEjercicio, fNumero + 1)
            else -> seriesDao?.setNumFactura(queSerie, queEjercicio, fNumero + 1)
        }
    }


/*
    fun hayOftasPorCantidad(): Boolean {
        val oftCantRangosDao: OftCantRangosDao? = MyDatabase.getInstance(fContext)?.oftCantRangosDao()

        val queContador = oftCantRangosDao?.hayOftasPorCantidad() ?: 0
        return (queContador > 0)
    }
*/
/*
    fun esTarifaPiezas(queTarifa: Byte): Boolean {
        val cnfTarifasDao: CnfTarifasDao? = MyDatabase.getInstance(fContext)?.cnfTarifasDao()

        val queFlag = cnfTarifasDao?.getFlagTarifa(queTarifa) ?: 0
        return (queFlag and Constantes.FLAGCNFTARIFAS_PARA_PIEZAS > 0)
    }
*/

}
