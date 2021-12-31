package es.albainformatica.albamobileandroid.cobros

import android.content.Context
import android.database.Cursor
import es.albainformatica.albamobileandroid.FLAGPENDIENTE_EN_CARTERA
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.actualizarSaldo
import es.albainformatica.albamobileandroid.dao.CabDiferidasDao
import es.albainformatica.albamobileandroid.dao.PendienteDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.PendienteEnt


class PendienteClase(queContexto: Context) {
    private val pendienteDao: PendienteDao? = MyDatabase.getInstance(queContexto)?.pendienteDao()
    private val bd = BaseDatos(queContexto)
    private val dbAlba = bd.writableDatabase
    private val fContexto = queContexto

    var pendienteId: Int = 0
    var clienteId: Int = 0
    var empresa: Short = 0
    var importe: String = ""
    var cobrado: String = ""
    var estado: String = ""
    var tipoDoc: String = ""
    var almacen: String = ""
    var cAlmacen: String = ""
    var cPuesto: String = ""
    var cApunte: String = ""
    var serie: String = ""
    var numero: String = ""
    var ejercicio: String = ""
    var fechaDoc: String = ""
    var fechaVto: String = ""
    var flag: Int = 0
    var enviar: String = ""
    var fPago: String = ""
    var descrFPago: String = ""

    var cursor: Cursor? = null


    fun borrarPdteDoc(queTipoDoc: Short, queEmpresa: Short, queAlmacen: Short, queSerie: String,
                      queNumero: Int, queEjercicio: Short, queCliente: Int, totalDoc: Double) {
        pendienteDao?.borrarPdteDoc(queTipoDoc, queEmpresa, queAlmacen, queSerie, queNumero, queEjercicio)

        // Actualizamos el saldo del cliente
        actualizarSaldo(fContexto, queCliente, queEmpresa, -totalDoc)
    }


    fun abrirPendienteId(quePendienteId: Int) {
        val pendienteEnt = pendienteDao?.abrirPendienteId(quePendienteId) ?: PendienteEnt()
        pendienteId = quePendienteId
        clienteId = pendienteEnt.clienteId
        importe = pendienteEnt.importe.replace(',', '.')
        cobrado = pendienteEnt.cobrado.replace(',', '.')
        estado = pendienteEnt.estado
        tipoDoc = pendienteEnt.tipoDoc.toString()
        empresa = pendienteEnt.empresa
        almacen = pendienteEnt.almacen.toString()
        cAlmacen = pendienteEnt.cAlmacen
        cPuesto = pendienteEnt.cPuesto
        cApunte = pendienteEnt.cApunte
        serie = pendienteEnt.serie
        numero = pendienteEnt.numero.toString()
        ejercicio = pendienteEnt.ejercicio.toString()
        fechaDoc = pendienteEnt.fechaDoc
        flag = pendienteEnt.flag
    }

    fun abrirTodosDocClte(queCliente: Int): Boolean {
        cursor = pendienteDao?.abrirTodosDocClte(queCliente)

        return (cursor?.moveToFirst() ?: false)
    }


    fun dimeNumDocsClte(queCliente: Int, queEmpresa: Int): Int {
        return pendienteDao?.dimeNumDocsClte(queCliente, queEmpresa) ?: 0
    }


    fun getFPagoDoc(queAlmacen: Short, queSerie: String, queNumero: Int, queEjercicio: Short): String {
        return pendienteDao?.abrirFPagoDoc(queAlmacen, queSerie, queNumero, queEjercicio) ?: ""
    }

    fun abrirPorFDoc(queCliente: Int, queEmpresa: Int): Boolean {
        cursor = pendienteDao?.abrirPorFDoc(queCliente, queEmpresa)

        return (cursor?.moveToFirst() ?: false)
    }


    fun abrir(queCliente: Int): Boolean {
        cursor = pendienteDao?.abrir(queCliente)

        if (cursor?.moveToFirst() == true) {
            clienteId = queCliente
            pendienteId = cursor?.getInt(cursor?.getColumnIndex("pendienteId") ?: 0) ?: 0
            fechaDoc = cursor?.getString(cursor?.getColumnIndex("fechaDoc") ?: 7) ?: ""
            fechaVto = cursor?.getString(cursor?.getColumnIndex("fechaVto") ?: 12) ?: ""
            flag = cursor?.getInt(cursor?.getColumnIndex("flag") ?: 20) ?: 0
            enviar = cursor?.getString(cursor?.getColumnIndex("enviar") ?: 14) ?: "F"
            descrFPago = cursor?.getString(cursor?.getColumnIndex("descripcion") ?: 24) ?: ""
            importe = (cursor?.getString(cursor?.getColumnIndex("importe") ?: 10) ?: "0").replace(',', '.')
            cobrado = (cursor?.getString(cursor?.getColumnIndex("cobrado") ?: 11) ?: "0").replace(',', '.')
            tipoDoc = cursor?.getShort(cursor?.getColumnIndex("tipoDoc") ?: 5).toString()
            empresa = cursor?.getShort(cursor?.getColumnIndex("empresa") ?: 3) ?: 0.toShort()
            almacen = cursor?.getShort(cursor?.getColumnIndex("almacen") ?: 4).toString()
            cAlmacen = cursor?.getString(cursor?.getColumnIndex("cAlmacen") ?: 15) ?: ""
            cPuesto = cursor?.getString(cursor?.getColumnIndex("cPuesto") ?: 16) ?: ""
            cApunte = cursor?.getString(cursor?.getColumnIndex("cApunte") ?: 17) ?: ""
            serie = cursor?.getString(cursor?.getColumnIndex("serie") ?: 8) ?: ""
            numero = cursor?.getString(cursor?.getColumnIndex("numero") ?: 9) ?: ""
            ejercicio = cursor?.getShort(cursor?.getColumnIndex("ejercicio") ?: 2).toString()

            return true
        }
        else return false
    }


    fun abrirDocumento(): Boolean {
        val fDocumento = Comunicador.fDocumento
        val pendienteEnt = pendienteDao?.abrirDocumento(fDocumento.fAlmacen, fDocumento.serie,
                                    fDocumento.numero, fDocumento.fEjercicio)

        val quePendienteId = pendienteEnt?.pendienteId ?: 0
        return if (quePendienteId > 0) {
            importe = pendienteEnt?.importe ?: "0.0"
            cobrado = pendienteEnt?.cobrado ?: "0.0"

            true
        } else false
    }

    fun abrirFraDiferida(queSerie: String, queNumero: Int, queEjercicio: Short): Boolean {
        val pendienteEnt = pendienteDao?.abrirDocDiferido(queSerie, queNumero, queEjercicio)

        val quePendienteId = pendienteEnt?.pendienteId ?: 0
        return if (quePendienteId > 0) {
            importe = pendienteEnt?.importe ?: "0.0"
            cobrado = pendienteEnt?.cobrado ?: "0.0"
            fPago = pendienteEnt?.fPago ?: ""

            true
        } else false
    }

    fun nuevoDoc(): Long {
        val pendienteEnt = PendienteEnt()
        val fDocumento = Comunicador.fDocumento

        pendienteEnt.clienteId = fDocumento.fCliente
        pendienteEnt.ejercicio = fDocumento.fEjercicio
        pendienteEnt.empresa = fDocumento.fEmpresa
        pendienteEnt.almacen = fDocumento.fAlmacen
        pendienteEnt.tipoDoc = fDocumento.fTipoDoc
        pendienteEnt.fPago = fDocumento.fPago
        pendienteEnt.fechaDoc = fDocumento.fFecha
        pendienteEnt.serie = fDocumento.serie
        pendienteEnt.numero = fDocumento.numero
        pendienteEnt.importe = fDocumento.fBases.totalConImptos.toString().replace(',', '.')
        pendienteEnt.cobrado = "0.0"
        pendienteEnt.fechaVto = ""
        pendienteEnt.estado = "P"
        pendienteEnt.enviar = "T"
        pendienteEnt.cAlmacen = ""
        pendienteEnt.cPuesto = ""
        pendienteEnt.cApunte = ""
        pendienteEnt.cEjercicio = ""
        pendienteEnt.numExport = 0
        pendienteEnt.flag = 0

        val fIdPdte = pendienteDao?.insertar(pendienteEnt) ?: 0

        // Actualizamos el saldo del cliente
        actualizarSaldo(fContexto, fDocumento.fCliente, fDocumento.fEmpresa, fDocumento.fBases.totalConImptos)

        return fIdPdte
    }


    fun actualizarFechaVto(queFechaVto: String, queAnotacion: String) {

        val queFlag = flag or FLAGPENDIENTE_EN_CARTERA
        pendienteDao?.actualizarFechaVto(pendienteId, queFechaVto, queAnotacion, queFlag)

        cursor?.close()
        abrir(clienteId)
    }


    fun getCabeceraId(): Int {
        try {
            dbAlba.rawQuery("SELECT _id FROM cabeceras WHERE tipodoc = " + tipoDoc +
                    " AND empresa = " + empresa + " AND alm = " + almacen +
                    " AND serie = '" + serie + "'" + " AND numero = " + numero +
                    " AND ejer = " + ejercicio, null
            ).use {
                    cDoc -> return if (cDoc.moveToFirst()) { cDoc.getInt(0) } else 0
            }
        } catch (e: Exception) {
            return 0
        }
    }


    fun dimeIdDocDiferido(): Int {
        val cabDifDao: CabDiferidasDao? = MyDatabase.getInstance(fContexto)?.cabDiferidasDao()

        return cabDifDao?.getIdDocumento(serie, numero.toInt(), ejercicio.toShort()) ?: 0
    }


    fun actualizarCobrado(queImporte: String, queIdPendiente: Int) {
        pendienteDao?.actualizarCobrado(queImporte, queIdPendiente)

        // Comprobamos si hemos liquidado el pendiente.
        if (cobrado.toDouble() + queImporte.toDouble() >= importe.toDouble())
            pendienteDao?.marcarComoLiquidado(queIdPendiente)
    }


}