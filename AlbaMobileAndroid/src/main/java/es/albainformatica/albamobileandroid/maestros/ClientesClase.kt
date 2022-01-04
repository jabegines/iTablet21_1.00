package es.albainformatica.albamobileandroid.maestros

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.preference.PreferenceManager
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.cobros.FormasPagoClase
import es.albainformatica.albamobileandroid.cobros.PendienteClase
import es.albainformatica.albamobileandroid.dao.ContactosCltesDao
import es.albainformatica.albamobileandroid.dao.SaldosDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.ContactosCltesEnt
import java.lang.Exception
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class ClientesClase(val contexto: Context) {
    private val saldosDao: SaldosDao? = MyDatabase.getInstance(contexto)?.saldosDao()
    private val contactosClteDao: ContactosCltesDao? = MyDatabase.getInstance(contexto)?.contactosCltesDao()

    lateinit var cursor: Cursor
    lateinit var cDirecciones: Cursor
    private var fFPago: FormasPagoClase = FormasPagoClase(contexto)

    private var fCliente = 0
    private var fEmpresaActual = 0
    private var fConfiguracion: Configuracion = Comunicador.fConfiguracion


    init {
        val prefs = PreferenceManager.getDefaultSharedPreferences(contexto)
        fEmpresaActual = prefs.getInt("ultima_empresa", 0)
    }


    fun close() {
        //cTelefonos?.close()
        if (this::cDirecciones.isInitialized)
            cDirecciones.close()
        if (this::cursor.isInitialized)
            cursor.close()
    }


    fun abrirUnCliente(queCliente: Int): Boolean {
        fCliente = queCliente
        // TODO
        /*
        cursor = dbAlba.rawQuery("SELECT * FROM clientes WHERE cliente = $queCliente", null)
        //cTelefonos = dbAlba.rawQuery("SELECT * FROM conclientes WHERE cliente = $queCliente ORDER BY orden", null)
        cDirecciones = dbAlba.rawQuery("SELECT * FROM dirclientes WHERE cliente = $queCliente ORDER BY orden", null)

        // Tenemos que hacer moveToFirst, ya que la posicion inicial de los cursores es -1.
        if (cursor.moveToFirst()) {
            //cTelefonos?.moveToFirst()
            cDirecciones.moveToFirst()
            return true
        } else return false
        */
        return true
    }


    fun Abrir() {
        // TODO
        //dbAlba = writableDatabase
    }

    fun abrirParaEnviar(QueNumExportacion: Int) {
        // TODO
        val sCondicion: String = if (QueNumExportacion == 0) " WHERE estado = 'N' OR estado = 'M'" else " WHERE numexport = $QueNumExportacion"
        //cursor = dbAlba.rawQuery("SELECT * FROM clientes $sCondicion", null)
    }


    fun getEmailsClte(queCliente: Int): MutableList<String> {
        return contactosClteDao?.getEmailsClte(queCliente) ?: emptyList<String>().toMutableList()
    }

    fun variasDirecciones(queCliente: Int): Boolean {
        // TODO
        /*
        return try {
            cDirecciones = dbAlba.rawQuery("SELECT * FROM dirClientes WHERE cliente = $queCliente" +
                    " AND (dirdoc = 'F' OR dirdoc IS NULL)", null)
            if (cDirecciones.moveToFirst()) {
                cDirecciones.count > 1
            } else false
        } finally {
            cDirecciones.close()
        }
        */
        return true
    }

    fun abrirDirParaEnviar(QueNumExportacion: Int) {
        val sCondicion: String = if (QueNumExportacion == 0) "WHERE estado = 'N' OR estado = 'M'" else " WHERE numexport = $QueNumExportacion"
        // TODO
        //cDirecciones = dbAlba.rawQuery("SELECT * FROM dirclientes $sCondicion", null)
    }



    fun existeCodigo(QueCodigo: String): Int {
        // TODO
        /*
        cursor = dbAlba.rawQuery("SELECT cliente FROM clientes WHERE codigo = '$QueCodigo'", null)
        return try {
            if (cursor.moveToFirst()) {
                getCliente()
            } else 0
        } finally {
            cursor.close()
        }
       */
        return 0
    }

    fun aceptarCambDirec(aDatosDirecc: ArrayList<String>, insertando: Boolean) {
        val values = ContentValues()
        values.put("direcc", aDatosDirecc[3])
        values.put("poblac", aDatosDirecc[4])
        values.put("cpostal", aDatosDirecc[5])
        values.put("provin", aDatosDirecc[6])
        if (insertando) {
            values.put("cliente", aDatosDirecc[1])
            values.put("alm", aDatosDirecc[2])
            values.put("orden", dimeUltimoOrdenDir(aDatosDirecc[1].toInt()))
            values.put("sucursal", "0")
            values.put("estado", "N")
            // TODO
            //dbAlba.insert("dirclientes", null, values)
        } else {
            values.put("estado", "M")
            // TODO
            //dbAlba.update("dirclientes", values, "_id=" + aDatosDirecc[0], null)
        }
        refrescarDirecc()
    }

    fun marcarComoExportados(iSigExportacion: Int) {
        val values = ContentValues()
        values.put("estado", "XN")
        values.put("numexport", iSigExportacion)
        // TODO
        //dbAlba.update("clientes", values, "estado='N'", null)
        //dbAlba.update("conclientes", values, "estado='N'", null)
        //dbAlba.update("dirclientes", values, "estado='N'", null)
        values.put("estado", "XM")
        values.put("numexport", iSigExportacion)
        //dbAlba.update("clientes", values, "estado='M'", null)
        //dbAlba.update("conclientes", values, "estado='M'", null)
        //dbAlba.update("dirclientes", values, "estado='M'", null)
    }


    fun marcarNumExport(fNumPaquete: Int) {
        val values = ContentValues()
        values.put("numexport", fNumPaquete)
        // TODO
        //dbAlba.update("clientes", values, "numexport=-1", null)
        //dbAlba.update("conclientes", values, "numexport=-1", null)
        //dbAlba.update("dirclientes", values, "numexport=-1", null)
    }

    fun borrarDirecc(QueId: String) {
        // TODO
        //dbAlba.delete("dirclientes", "_id=$QueId", null)
        refrescarDirecc()
    }

    private fun refrescarDirecc() {
        cDirecciones.close()
        // TODO
        //cDirecciones = dbAlba.rawQuery("SELECT * FROM dirclientes WHERE cliente = " + fCliente + " ORDER BY orden", null)
        cDirecciones.moveToFirst()
    }

    private fun refrescarTelef() {
    }

    fun aceptarCambTelf(aDatosTelf: ArrayList<String>, insertando: Boolean) {
        val values = ContentValues()
        values.put("contacto", aDatosTelf[3])
        values.put("tel1", aDatosTelf[4])
        values.put("tel2", aDatosTelf[5])
        values.put("email", aDatosTelf[6])
        values.put("obs1", aDatosTelf[7])
        if (insertando) {
            values.put("cliente", aDatosTelf[1])
            values.put("alm", aDatosTelf[2])
            values.put("sucursal", "0")
            values.put("orden", dimeUltimoOrdenTelf())
            values.put("estado", "N")
            // TODO
            //dbAlba.insert("conclientes", null, values)
        } else {
            values.put("estado", "M")
            // TODO
            //dbAlba.update("conclientes", values, "_id=" + aDatosTelf[0], null)
        }
        refrescarTelef()
    }

    fun borrarTelf(QueId: String) {
        // TODO
        //dbAlba.delete("conclientes", "_id=$QueId", null)
        refrescarTelef()
    }


    fun aceptarCambios(aDatosClte: ArrayList<String>, insertando: Boolean) {
        val values = ContentValues()
        values.put("codigo", aDatosClte[1])
        values.put("nomfi", aDatosClte[2])
        values.put("nomco", aDatosClte[3])
        values.put("cif", aDatosClte[4])
        values.put("direcc", aDatosClte[5])
        values.put("locali", aDatosClte[6])
        values.put("cpostal", aDatosClte[7])
        values.put("provin", aDatosClte[8])
        values.put("tarifa", aDatosClte[9])
        values.put("tardto", aDatosClte[10])
        values.put("fpago", aDatosClte[11])
        values.put("ruta", aDatosClte[12])
        values.put("riesgo", aDatosClte[13])
        values.put("apliva", aDatosClte[14])
        values.put("aplrec", aDatosClte[15])
        values.put("tipoiva", "1")
        /* Por ahora estos campos (flag, flag2) no los tocamos, dejamos los que vengan de la gestion (si estamos modificando) */if (insertando) {
            values.put("flag", FLAGCLIENTE_APLICAROFERTAS) // Aplicar ofertas. Activaremos siempre este flag al dar de alta.
            values.put("flag2", "0")
        }
        values.put("tipo", "")
        values.put("ramo", "")
        values.put("estado", aDatosClte[16])
        if (insertando) {
            values.put("cliente", dimeUltimoClte())
            values.put("tieneincid", "F")
            // TODO
            //dbAlba.insert("clientes", null, values)

            // Si hemos indicado alguna ruta, incluimos el cliente al final del rutero
            if (aDatosClte[12] != "") anyadirARutero(values.getAsString("cliente"), values.getAsString("ruta"))
        }
        // TODO
        //else dbAlba.update("clientes", values, "cliente=" + aDatosClte[0], null)
    }

    private fun anyadirARutero(queCliente: String, queRuta: String) {
        val values = ContentValues()
        values.put("ruta", queRuta)
        values.put("orden", 9999)
        values.put("cliente", queCliente)
        // TODO
        //dbAlba.insert("rutero", null, values)
    }

    private fun dimeUltimoOrdenTelf(): Int {
        // TODO
        /*
        dbAlba.rawQuery("SELECT MAX(orden) orden FROM conclientes", null).use { cUltOrden ->
            return if (cUltOrden.moveToFirst()) {
                val columna = cUltOrden.getColumnIndex("orden")
                cUltOrden.getInt(columna) + 1
            } else 1
        }
        */
        return 0
    }

    private fun dimeUltimoOrdenDir(QueCliente: Int): Int {
        // TODO
        /*
        dbAlba.rawQuery("SELECT MAX(orden) orden FROM dirclientes WHERE cliente = $QueCliente", null
        ).use { cUltOrden ->
            return if (cUltOrden.moveToFirst()) {
                val columna = cUltOrden.getColumnIndex("orden")
                cUltOrden.getInt(columna) + 1
            } else 1
        }
        */
        return 0
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

    private fun existeUltClte(QueClte: Int): Boolean {
        return try {
            // TODO
            //cursor = dbAlba.rawQuery("SELECT cliente FROM clientes WHERE cliente = $QueClte", null)
            cursor.moveToFirst()
        } finally {
            cursor.close()
        }
    }


    fun actualizarPendiente(queCliente: Int, queImporte: Double) {
        var dImporte = queImporte
        val values = ContentValues()
        // TODO
        /*
        dbAlba.rawQuery("SELECT pendiente FROM clientes WHERE cliente = $queCliente", null)
            .use { cPendiente ->
                if (cPendiente.moveToFirst()) {
                    if (cPendiente.getString(cPendiente.getColumnIndex("pendiente")) != null)
                        dImporte = cPendiente.getString(cPendiente.getColumnIndex("pendiente")).replace(',', '.').toDouble() + dImporte
                    values.put("pendiente", dImporte)
                    dbAlba.update("clientes", values, "cliente=$queCliente", null)
                }
            }
        */
    }


    fun getCliente(): Int {
        val columna = cursor.getColumnIndex("cliente")
        return cursor.getInt(columna)
    }


    fun getCodigo(): String {
        val columna = cursor.getColumnIndex("codigo")
        return cursor.getString(columna)
    }

    fun getNFiscal(): String {
        val columna = cursor.getColumnIndex("nomfi")
        return cursor.getString(columna)
    }

    fun getNComercial(): String {
        val columna = cursor.getColumnIndex("nomco")
        return cursor.getString(columna)
    }

    fun getCIF(): String {
        val columna = cursor.getColumnIndex("cif")
        return cursor.getString(columna)
    }

    fun getDireccion(): String {
        val columna = cursor.getColumnIndex("direcc")
        return cursor.getString(columna)
    }

    fun getPoblacion(): String {
        val columna = cursor.getColumnIndex("locali")
        return cursor.getString(columna)
    }

    fun getCodPostal(): String {
        val columna = cursor.getColumnIndex("cpostal")
        return cursor.getString(columna)
    }

    fun getProvincia(): String {
        val columna = cursor.getColumnIndex("provin")
        return cursor.getString(columna)
    }

    fun getMatricula(): String {
        return cursor.getString(cursor.getColumnIndex("matricula"))
    }

    fun getFlag(): Int {
        val columna = cursor.getColumnIndex("flag")
        return cursor.getInt(columna)
    }

    fun getTarifa(): String {
        // Hacemos el trim a getString() porque en el caso de haber escogido
        // "Sin tarifa" hemos guardado un espacio en blanco en el campo "tarifa" de
        // la tabla.
        val columna = cursor.getColumnIndex("tarifa")
        return if (cursor.getString(columna) != null) cursor.getString(columna)
            .trim { it <= ' ' } else ""
    }

    fun getTarifaDto(): String {
        val columna = cursor.getColumnIndex("tardto")
        return if (cursor.getString(columna) != null) cursor.getString(columna)
            .trim { it <= ' ' } else ""
    }

    fun getTarifaPiezas(): String {
        var queTarifa = "0"
        if (cursor.getString(cursor.getColumnIndex("tarifaPiezas")) != null) queTarifa =
            cursor.getString(cursor.getColumnIndex("tarifaPiezas"))
        return queTarifa
    }

    fun getFPago(): String {
        val columna = cursor.getColumnIndex("fpago")
        return cursor.getString(columna)
    }

    fun nombreFPago(queFPago: String?): String {
        return fFPago.getDescrFPago(queFPago!!)
    }


    fun getRuta(): String {
        val columna = cursor.getColumnIndex("ruta")
        return cursor.getString(columna)
    }

    fun getRamo(): String {
        return cursor.getString(cursor.getColumnIndex("ramo"))
    }

    fun getEstado(): String {
        return try {
            val sEstado = cursor.getString(cursor.getColumnIndex("estado"))
            sEstado ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun getRiesgo(): Double {
        val columna = cursor.getColumnIndex("riesgo")
        var sRiesgo = cursor.getString(columna)
        sRiesgo = sRiesgo.replace(',', '.')
        if (sRiesgo == "") sRiesgo = "0"
        return try {
            java.lang.Double.valueOf(sRiesgo)
        } catch (e: Exception) {
            0.0
        }
    }


    fun getSaldo(): Double {
        return saldosDao?.getSaldoClte(getCliente(), fEmpresaActual)?.replace(",", ".")?.toDouble() ?: 0.0
    }


    private fun getPendiente(queCliente: Int): Double {
        return saldosDao?.getPendienteClte(queCliente, fEmpresaActual)?.replace(",", ".")?.toDouble() ?: 0.0
    }



    fun getAplicarIva(): Boolean {
        val columna = cursor.getColumnIndex("apliva")
        val sAplIva = cursor.getString(columna)
        return sAplIva == "T"
    }

    fun getAplicarRe(): Boolean {
        val columna = cursor.getColumnIndex("aplrec")
        val sAplRec = cursor.getString(columna)
        return sAplRec == "T"
    }



    fun getAplicarOfertas(): Boolean {
        val sFlag = cursor.getString(cursor.getColumnIndex("flag"))
        return if (sFlag != null) {
            val iFlag = sFlag.toInt()
            val Resultado = iFlag and FLAGCLIENTE_APLICAROFERTAS
            Resultado > 0
        } else false
    }



    fun getDir_Cliente(): Int {
        val columna = cDirecciones.getColumnIndex("cliente")
        return if (cDirecciones.count > 0) cDirecciones.getInt(columna) else 0
    }

    fun getDir_Orden(): String {
        val columna = cDirecciones.getColumnIndex("orden")
        return if (cDirecciones.count > 0) cDirecciones.getString(columna) else ""
    }

    fun getDir_Direccion(): String {
        val columna = cDirecciones.getColumnIndex("direcc")
        return if (cDirecciones.count > 0) cDirecciones.getString(columna) else ""
    }

    fun getDir_Poblac(): String {
        val columna = cDirecciones.getColumnIndex("poblac")
        return if (cDirecciones.count > 0) cDirecciones.getString(columna) else ""
    }

    fun getDir_CP(): String {
        val columna = cDirecciones.getColumnIndex("cpostal")
        return if (cDirecciones.count > 0) cDirecciones.getString(columna) else ""
    }

    fun getDir_Provincia(): String {
        val columna = cDirecciones.getColumnIndex("provin")
        return if (cDirecciones.count > 0) cDirecciones.getString(columna) else ""
    }

    fun getDir_Pais(): String {
        val columna = cDirecciones.getColumnIndex("pais")
        if (cDirecciones.count > 0)
            if (cDirecciones.getString(columna) != null)
                return cDirecciones.getString(columna)
            else
                return ""
        else return ""
    }


    fun controlarRiesgo(): Boolean {
        val sFlag = cursor.getString(cursor.getColumnIndex("flag"))
        return if (sFlag != null) {
            val iFlag = sFlag.toInt()
            val Resultado = iFlag and FLAGCLIENTE_CONTROLARRIESGO
            Resultado > 0
        } else false
    }

    fun maxDiasRiesgo(): Int {
        val columna = cursor.getColumnIndex("maxdias")
        return cursor.getInt(columna)
    }

    fun maxFrasRiesgo(): Int {
        val columna = cursor.getColumnIndex("maxfraspdtes")
        return cursor.getInt(columna)
    }

    fun noVender(): Boolean {
        val sFlag = cursor.getString(cursor.getColumnIndex("flag"))
        return if (sFlag != null) {
            val iFlag = sFlag.toInt()
            val Resultado = iFlag and FLAGCLIENTE_NOVENDER
            Resultado > 0
        } else false
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
            if (fControlarRiesgo && (fTotalPendiente > getRiesgo())) resultado = true

            // Vemos si ha superado el riesgo mediante la fecha. Para ello abrimos el vencimiento más antiguo del cliente
            // y comparamos la fecha del documento con los días de riesgo que tenemos configurado.
            if (!resultado) {
                if (!fControlarRiesgo && fControlarFechas || fContrFechasSiempre) {
                    val fDiasClte = maxDiasRiesgo()
                    val fCltesDiasRiesgo = fConfiguracion.cltesMaxDiasRiesgo()
                    if (fDiasClte > 0 || fCltesDiasRiesgo > 0) {
                        val fPendiente = PendienteClase(contexto)
                        if (fPendiente.abrirPorFDoc(fCliente, fEmpresa)) {
                            val fEsDocNuevo = fPendiente.cursor!!.getString(0).contains("/")
                            val strFechaDoc = fPendiente.cursor!!.getString(0).replace('-', '/')
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
                                fFechaDoc = formatoFechaDoc.parse(strFechaDoc)
                                fFechaAct = formatoFechaAct.parse(strFechaAct)
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
                        val fDocsClte = maxFrasRiesgo()
                        val fCltesDocsRiesgo = fConfiguracion.cltesMaxFrasRiesgo()
                        if (fDocsClte > 0) resultado = fNumDocs > fDocsClte else {
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