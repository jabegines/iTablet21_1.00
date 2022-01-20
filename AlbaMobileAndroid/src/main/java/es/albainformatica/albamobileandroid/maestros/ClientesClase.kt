package es.albainformatica.albamobileandroid.maestros

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.preference.PreferenceManager
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.cobros.FormasPagoClase
import es.albainformatica.albamobileandroid.cobros.PendienteClase
import es.albainformatica.albamobileandroid.dao.*
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.ClientesEnt
import es.albainformatica.albamobileandroid.entity.ContactosCltesEnt
import es.albainformatica.albamobileandroid.entity.DireccCltesEnt
import es.albainformatica.albamobileandroid.entity.RuterosEnt
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class ClientesClase(val contexto: Context) {
    private val clientesDao: ClientesDao? = MyDatabase.getInstance(contexto)?.clientesDao()
    private val saldosDao: SaldosDao? = MyDatabase.getInstance(contexto)?.saldosDao()
    private val contactosClteDao: ContactosCltesDao? = MyDatabase.getInstance(contexto)?.contactosCltesDao()
    private val direccCltesDao: DireccCltesDao? = MyDatabase.getInstance(contexto)?.direccCltesDao()
    private val ruterosDao: RuterosDao? = MyDatabase.getInstance(contexto)?.ruterosDao()
    private var fConfiguracion: Configuracion = Comunicador.fConfiguracion
    private var fFPago: FormasPagoClase = FormasPagoClase(contexto)

    lateinit var cursor: Cursor

    private var fCliente = 0
    var fCodigo: String = ""
    var fNombre: String = ""
    var fNomComercial: String = ""
    var fCif: String = ""
    var fAplIva: Boolean = true
    var fAplRec: Boolean = false
    private var fFlag: Int = 0
    var fTarifa: Short = 0
    var fTrfDto: Short = 0
    var fTrfPiezas: Short = 0
    var fPago: String = ""
    var fRuta: Short = 0
    private var fEmpresaActual = 0
    private var fMaxFrasPdtes: Int = 0
    var fRiesgo: Double = 0.0
    var fDireccion: String = ""
    var fCodPostal: String = ""
    var fPoblacion: String = ""
    var fProvincia: String = ""
    var fEstado: String = ""
    var fRamo: Short = 0



    init {
        val prefs = PreferenceManager.getDefaultSharedPreferences(contexto)
        fEmpresaActual = prefs.getInt("ultima_empresa", 0)
    }


    fun close() {
        if (this::cursor.isInitialized)
            cursor.close()
    }


    fun abrirUnCliente(queCliente: Int): Boolean {
        fCliente = queCliente

        val clteEnt = clientesDao?.abrirUnCliente(queCliente) ?: ClientesEnt()
        if (clteEnt.codigo > 0) {
            fCodigo = ponerCeros(clteEnt.codigo.toString(), ancho_codclte)
            fNombre = clteEnt.nombre
            fNomComercial = clteEnt.nombreComercial
            fCif = clteEnt.cif
            fAplIva = (clteEnt.aplIva == "T")
            fAplRec = (clteEnt.aplRec == "T")
            fFlag = clteEnt.flag
            fMaxFrasPdtes = clteEnt.maxFrasPdtes
            fTarifa = clteEnt.tarifaId
            fTrfDto = clteEnt.tarifaDtoId
            fTrfPiezas = clteEnt.tarifaPiezas
            fPago = clteEnt.fPago
            fRuta = clteEnt.rutaId
            fRiesgo = if (clteEnt.riesgo != "") clteEnt.riesgo.replace(',', '.').toDouble()
                        else 0.0
            fDireccion = clteEnt.direccion
            fCodPostal = clteEnt.cPostal
            fPoblacion = clteEnt.localidad
            fProvincia = clteEnt.provincia
            fEstado = clteEnt.estado
            fRamo = clteEnt.ramo
        }

        return (clteEnt.codigo > 0)
    }




    fun abrirParaEnviar(queNumExportacion: Int): List<ClientesEnt> {
        return if (queNumExportacion == 0)
            clientesDao?.abrirParaEnviar() ?: emptyList<ClientesEnt>().toMutableList()
        else
            clientesDao?.abrirParaEnvExp(queNumExportacion) ?: emptyList<ClientesEnt>().toMutableList()
    }


    fun getEmailsClte(queCliente: Int): MutableList<String> {
        return contactosClteDao?.getEmailsClte(queCliente) ?: emptyList<String>().toMutableList()
    }

    fun variasDirecciones(queCliente: Int): Boolean {

        val lDirecciones = direccCltesDao?.getDirNoDocClte(queCliente) ?: emptyList<DireccCltesEnt>().toMutableList()
        return (lDirecciones.count() > 1)
    }


    fun existeCodigo(queCodigo: Int): Int {
        return clientesDao?.existeCodigo(queCodigo) ?: 0
    }

    fun aceptarCambDirec(aDatosDirecc: ArrayList<String>, insertando: Boolean) {
        val dirClteEnt = DireccCltesEnt()
        dirClteEnt.direccion = aDatosDirecc[3]
        dirClteEnt.localidad = aDatosDirecc[4]
        dirClteEnt.provincia = aDatosDirecc[6]
        dirClteEnt.cPostal = aDatosDirecc[5]

        if (insertando) {
            dirClteEnt.clienteId = aDatosDirecc[1].toInt()
            dirClteEnt.almacen = aDatosDirecc[2].toShort()
            dirClteEnt.orden = dimeUltimoOrdenDir(aDatosDirecc[1].toInt()).toShort()
            dirClteEnt.sucursal = 0
            dirClteEnt.estado = "N"

            direccCltesDao?.insertar(dirClteEnt)
        } else {
            dirClteEnt.direccionId = aDatosDirecc[0].toInt()
            dirClteEnt.estado =  "M"

            direccCltesDao?.actualizar(dirClteEnt.direccionId, dirClteEnt.direccion, dirClteEnt.localidad,
                        dirClteEnt.provincia, dirClteEnt.cPostal, dirClteEnt.estado)
        }
    }


    fun aceptarCambTelf(aDatosTelf: ArrayList<String>, insertando: Boolean) {
        val tlfClteEnt = ContactosCltesEnt()
        tlfClteEnt.nombre = aDatosTelf[3]
        tlfClteEnt.telefono1 = aDatosTelf[4]
        tlfClteEnt.telefono2 = aDatosTelf[5]
        tlfClteEnt.obs1 = aDatosTelf[7]
        tlfClteEnt.eMail = aDatosTelf[6]

        if (insertando) {
            tlfClteEnt.clienteId = aDatosTelf[1].toInt()
            tlfClteEnt.almacen = aDatosTelf[2].toShort()
            tlfClteEnt.orden = dimeUltimoOrdenTelf(aDatosTelf[1].toInt()).toShort()
            tlfClteEnt.sucursal = 0
            tlfClteEnt.estado = "N"

            contactosClteDao?.insertar(tlfClteEnt)
        } else {
            tlfClteEnt.contactoClteId = aDatosTelf[0].toInt()
            tlfClteEnt.estado = "M"

            contactosClteDao?.actualizar(tlfClteEnt.contactoClteId, tlfClteEnt.nombre, tlfClteEnt.telefono1,
                        tlfClteEnt.telefono2, tlfClteEnt.obs1, tlfClteEnt.eMail, tlfClteEnt.estado)
        }
    }



    fun marcarComoExportados(iSigExportacion: Int) {
        clientesDao?.marcarComoExportados(iSigExportacion)
        clientesDao?.marcarComoExpModif(iSigExportacion)
        contactosClteDao?.marcarComoExportados(iSigExportacion)
        contactosClteDao?.marcarComoExpModif(iSigExportacion)
        direccCltesDao?.marcarComoExportados(iSigExportacion)
        direccCltesDao?.marcarComoExpModif(iSigExportacion)
    }


    fun marcarNumExport(fNumPaquete: Int) {
        clientesDao?.marcarNumExport(fNumPaquete)
        contactosClteDao?.marcarNumExport(fNumPaquete)
        direccCltesDao?.marcarNumExport(fNumPaquete)
    }


    fun borrarDirecc(queId: Int) {
        direccCltesDao?.borrar(queId)
    }


    fun borrarTelf(queId: Int) {
        contactosClteDao?.borrar(queId)
    }


    fun aceptarCambios(aDatosClte: ArrayList<String>, insertando: Boolean) {
        val clteEnt = ClientesEnt()
        clteEnt.codigo = aDatosClte[1].toInt()
        clteEnt.nombre = aDatosClte[2]
        clteEnt.nombreComercial = aDatosClte[3]
        clteEnt.cif = aDatosClte[4]
        clteEnt.direccion = aDatosClte[5]
        clteEnt.localidad = aDatosClte[6]
        clteEnt.cPostal = aDatosClte[7]
        clteEnt.provincia = aDatosClte[8]
        clteEnt.aplIva = aDatosClte[14]
        clteEnt.aplRec = aDatosClte[15]
        clteEnt.tipoIva = 1
        clteEnt.tarifaId = aDatosClte[9].toShort()
        clteEnt.tarifaDtoId = aDatosClte[10].toShort()
        clteEnt.fPago = aDatosClte[11]
        clteEnt.rutaId = aDatosClte[12].toShort()
        clteEnt.riesgo = aDatosClte[13]
        clteEnt.estado = aDatosClte[16]
        clteEnt.ramo = 0

        if (insertando) {
            clteEnt.clienteId = dimeUltimoClte()
            clteEnt.tarifaPiezas = 0
            /* Por ahora estos campos (flag, flag2) no los tocamos, dejamos los que vengan de la gestion (si estamos modificando) */
            clteEnt.flag = FLAGCLIENTE_APLICAROFERTAS // Aplicar ofertas. Activaremos siempre este flag al dar de alta.
            clteEnt.flag2 = 0
            clteEnt.tieneIncid = "F"

            clientesDao?.insertar(clteEnt)

            // Si hemos indicado alguna ruta, incluimos el cliente al final del rutero
            if (aDatosClte[12].toShort() > 0) anyadirARutero(clteEnt.clienteId, clteEnt.rutaId)
        }
        else {
            clteEnt.clienteId = aDatosClte[0].toInt()
            clientesDao?.actualizar(clteEnt)
        }
    }


    private fun anyadirARutero(queCliente: Int, queRuta: Short) {
        val ruteroEnt = RuterosEnt()
        ruteroEnt.rutaId = queRuta
        ruteroEnt.orden = 9999
        ruteroEnt.clienteId = queCliente

        ruterosDao?.insertar(ruteroEnt)
    }

    private fun dimeUltimoOrdenTelf(queCliente: Int): Int {
        return (contactosClteDao?.getUltimoOrden(queCliente) ?: 0) + 1
    }

    private fun dimeUltimoOrdenDir(queCliente: Int): Int {
        return (direccCltesDao?.getUltimoOrden(queCliente) ?: 0) + 1
    }


    private fun dimeUltimoClte(): Int {
        var bContinuar: Boolean
        val iCodTerm = fConfiguracion.codTerminal().toInt()
        val iRangoInf = iCodTerm * 10000
        val iRangoSuperior = iRangoInf + 9999
        // Vemos el proximo ID de cliente en la tabla de Configuracion. Este numero nos viene de gestion.
        var iProxClte = fConfiguracion.proximoIDClte()
        if (iProxClte == 0) iProxClte = iRangoInf
        bContinuar = existeUltClte(iProxClte)
        while (bContinuar && iProxClte < iRangoSuperior) {
            iProxClte++
            bContinuar = existeUltClte(iProxClte)
        }
        // Guardamos en configuracion el proximo ID.
        fConfiguracion.setProximoIdClte(iProxClte + 1)
        return iProxClte
    }


    private fun existeUltClte(queClte: Int): Boolean {
        val queClteId = clientesDao?.existeClteId(queClte) ?: 0
        return (queClteId > 0)
    }


    fun actualizarPendiente(queCliente: Int, queImporte: Double) {
        var dImporte = queImporte

        var quePendiente = clientesDao?.getPendienteClte(queCliente) ?: "0.0"
        if (quePendiente == "") quePendiente = "0.0"

        dImporte += quePendiente.replace(',', '.').toDouble()
        clientesDao?.actualizarPendiente(queCliente, dImporte.toString())
    }




    fun nombreFPago(queFPago: String): String {
        return fFPago.getDescrFPago(queFPago)
    }



    fun getSaldo(): Double {
        return saldosDao?.getSaldoClte(fCliente, fEmpresaActual)?.replace(",", ".")?.toDouble() ?: 0.0
    }


    private fun getPendiente(queCliente: Int): Double {
        return saldosDao?.getPendienteClte(queCliente, fEmpresaActual)?.replace(",", ".")?.toDouble() ?: 0.0
    }



    fun getAplicarOfertas(): Boolean {
        return (fFlag and FLAGCLIENTE_APLICAROFERTAS) > 0
    }



    private fun controlarRiesgo(): Boolean {
        return (fFlag and FLAGCLIENTE_CONTROLARRIESGO) > 0
    }

    fun maxDiasRiesgo(): Int {
        val columna = cursor.getColumnIndex("maxdias")
        return cursor.getInt(columna)
    }


    fun noVender(): Boolean {
        return (fFlag and FLAGCLIENTE_NOVENDER) > 0
    }


    @SuppressLint("SimpleDateFormat")
    fun clienteEnRiesgo(totalDoc: Double, numDocsPdtes: Int, fEmpresa: Int): Boolean {
        var resultado = false
        // Actualizamos fEmpresaActual aquí en la clase Clientes porque la hemos podido cambiar en la
        // pantalla de ventas y aquí no se ha refrescado
        fEmpresaActual = fEmpresa
        val fControlarRiesgo = controlarRiesgo()
        val fControlarFechas = fConfiguracion.cltesContrFechas()
        val fControlarFrasPdtes = fConfiguracion.cltesContrFactPdtes()
        val fContrFechasSiempre = fConfiguracion.contrFechasSiempre()
        if (fControlarRiesgo || fControlarFechas && !fContrFechasSiempre || fControlarFrasPdtes || fContrFechasSiempre) {
            val fTotalPendiente = getSaldo() + getPendiente(fCliente) + totalDoc
            // Si controlamos el riesgo del cliente y lo ha superado presentaremos la ventana
            if (fControlarRiesgo && (fTotalPendiente > fRiesgo)) resultado = true

            // Vemos si ha superado el riesgo mediante la fecha. Para ello abrimos el vencimiento más antiguo del cliente
            // y comparamos la fecha del documento con los días de riesgo que tenemos configurado.
            if (!resultado) {
                if (!fControlarRiesgo && fControlarFechas || fContrFechasSiempre) {
                    val fDiasClte = maxDiasRiesgo()
                    val fCltesDiasRiesgo = fConfiguracion.cltesMaxDiasRiesgo()
                    if (fDiasClte > 0 || fCltesDiasRiesgo > 0) {
                        val fPendiente = PendienteClase(contexto)
                        if (fPendiente.abrirPorFDoc(fCliente, fEmpresa)) {
                            val fEsDocNuevo = fPendiente.cursor?.getString(0)?.contains("/") ?: true
                            val strFechaDoc = fPendiente.cursor?.getString(0)?.replace('-', '/') ?: ""
                            val fFechaDoc: Date
                            val fFechaAct: Date
                            // Si el registro de la tabla Pendiente lo hemos hecho nuevo en la tablet, el formato de la fecha
                            // será dd/MM/yyyy. En cambio, si el registro viene de la gestión el formato será yyyy/MM/dd
                            @SuppressLint("SimpleDateFormat") var formatoFechaDoc = SimpleDateFormat("yyyy/MM/dd")
                            if (fEsDocNuevo) formatoFechaDoc = SimpleDateFormat("dd/MM/yyyy")
                            val formatoFechaAct = SimpleDateFormat("yyyy/MM/dd")
                            val tim = System.currentTimeMillis()
                            val strFechaAct = formatoFechaAct.format(tim)
                            try {
                                fFechaDoc = formatoFechaDoc.parse(strFechaDoc) ?: Date()
                                fFechaAct = formatoFechaAct.parse(strFechaAct) ?: Date()
                                val dias = ((fFechaAct.time - fFechaDoc.time) / 86400000).toInt()
                                resultado = if (fDiasClte > 0) dias >= fDiasClte else dias >= fCltesDiasRiesgo
                            } catch (ex: ParseException) {
                                ex.printStackTrace()
                            }
                        }
                        if (fPendiente.cursor != null) fPendiente.cursor!!.close()
                    }
                }
            }

            // Vemos si ha superado el riesgo mediante el número de facturas pendientes.
            if (!resultado) {
                if (!fControlarRiesgo && fControlarFrasPdtes) {
                    val fPendiente = PendienteClase(contexto)
                    val fNumDocs = fPendiente.dimeNumDocsClte(fCliente, fEmpresa) + numDocsPdtes
                    if (fNumDocs > 0) {
                        val fCltesDocsRiesgo = fConfiguracion.cltesMaxFrasRiesgo()
                        if (fMaxFrasPdtes > 0) resultado = fNumDocs > fMaxFrasPdtes else {
                            if (fCltesDocsRiesgo > 0) resultado = fNumDocs > fCltesDocsRiesgo
                        }
                    }
                    if (fPendiente.cursor != null) fPendiente.cursor!!.close()
                }
            }
        }
        return resultado
    }


}