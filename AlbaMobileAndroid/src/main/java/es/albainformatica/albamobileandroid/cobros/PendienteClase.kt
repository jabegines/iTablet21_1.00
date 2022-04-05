package es.albainformatica.albamobileandroid.cobros

import android.content.Context
import es.albainformatica.albamobileandroid.FLAGPENDIENTE_EN_CARTERA
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.actualizarSaldo
import es.albainformatica.albamobileandroid.dao.CabDiferidasDao
import es.albainformatica.albamobileandroid.dao.CabecerasDao
import es.albainformatica.albamobileandroid.dao.FormasPagoDao
import es.albainformatica.albamobileandroid.dao.PendienteDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.PendienteEnt


class PendienteClase(queContexto: Context) {
    private val pendienteDao: PendienteDao? = MyDatabase.getInstance(queContexto)?.pendienteDao()
    private val formasPagoDao: FormasPagoDao? = MyDatabase.getInstance(queContexto)?.formasPagoDao()
    private val cabecerasDao: CabecerasDao? = MyDatabase.getInstance(queContexto)?.cabecerasDao()
    private val fContexto = queContexto

    var pendienteId: Int = 0
    var clienteId: Int = 0
    var empresa: Short = 0
    var importe: String = ""
    var cobrado: String = ""
    var estado: String = ""
    var tipoDoc: Short = 0
    var almacen: Short = 0
    var cAlmacen: String = ""
    var cPuesto: String = ""
    var cApunte: String = ""
    var serie: String = ""
    var numero: Int = 0
    var ejercicio: Short = 0
    var fechaDoc: String = ""
    var fechaVto: String = ""
    var flag: Int = 0
    var enviar: String = ""
    var fPago: String = ""
    var descrFPago: String = ""

    lateinit var lPendiente: List<PendienteEnt>
    lateinit var lTodosDocClte: List<String>


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
        tipoDoc = pendienteEnt.tipoDoc
        empresa = pendienteEnt.empresa
        almacen = pendienteEnt.almacen
        cAlmacen = pendienteEnt.cAlmacen
        cPuesto = pendienteEnt.cPuesto
        cApunte = pendienteEnt.cApunte
        serie = pendienteEnt.serie
        numero = pendienteEnt.numero
        ejercicio = pendienteEnt.ejercicio
        fechaDoc = pendienteEnt.fechaDoc
        flag = pendienteEnt.flag
    }

    fun abrirTodosDocClte(queCliente: Int): Boolean {
        lTodosDocClte = pendienteDao?.abrirTodosDocClte(queCliente) ?:  emptyList<String>().toMutableList()
        return (lTodosDocClte.count() > 0)
    }


    fun dimeNumDocsClte(queCliente: Int, queEmpresa: Int): Int {
        return pendienteDao?.dimeNumDocsClte(queCliente, queEmpresa) ?: 0
    }


    fun getFPagoDoc(queAlmacen: Short, queSerie: String, queNumero: Int, queEjercicio: Short): String {
        return pendienteDao?.abrirFPagoDoc(queAlmacen, queSerie, queNumero, queEjercicio) ?: ""
    }

    fun abrirPorFDoc(queCliente: Int, queEmpresa: Int): Boolean {
        lTodosDocClte = pendienteDao?.abrirPorFDoc(queCliente, queEmpresa) ?: emptyList<String>().toMutableList()
        return (lTodosDocClte.count() > 0)
    }


    fun abrir(queCliente: Int): Boolean {
        lPendiente = pendienteDao?.abrir(queCliente) ?: emptyList<PendienteEnt>().toMutableList()

        if (lPendiente.count() > 0) {
            val pdteEnt = lPendiente[0]
            clienteId = queCliente
            pendienteId = pdteEnt.pendienteId
            fechaDoc = pdteEnt.fechaDoc
            fechaVto = pdteEnt.fechaVto
            flag = pdteEnt.flag
            enviar = pdteEnt.enviar
            descrFPago = formasPagoDao?.getDescrFPago(pdteEnt.fPago) ?: ""
            importe = pdteEnt.importe.replace(',', '.')
            cobrado =  pdteEnt.cobrado.replace(',', '.')
            tipoDoc = pdteEnt.tipoDoc
            empresa = pdteEnt.empresa
            almacen = pdteEnt.almacen
            cAlmacen = pdteEnt.cAlmacen
            cPuesto =  pdteEnt.cPuesto
            cApunte = pdteEnt.cApunte
            serie = pdteEnt.serie
            numero = pdteEnt.numero
            ejercicio =  pdteEnt.ejercicio

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
    }


    fun getCabeceraId(): Int {
        return cabecerasDao?.getCabeceraId(tipoDoc, empresa, almacen, serie, numero, ejercicio) ?: 0
    }


    fun dimeIdDocDiferido(): Int {
        val cabDifDao: CabDiferidasDao? = MyDatabase.getInstance(fContexto)?.cabDiferidasDao()

        return cabDifDao?.getIdDocumento(serie, numero, ejercicio) ?: 0
    }


    fun actualizarCobrado(queImporte: String, queIdPendiente: Int) {
        pendienteDao?.actualizarCobrado(queImporte, queIdPendiente)

        // Comprobamos si hemos liquidado el pendiente.
        if (cobrado.toDouble() + queImporte.toDouble() >= importe.toDouble())
            pendienteDao?.marcarComoLiquidado(queIdPendiente)
        else
            pendienteDao?.marcarComoCobrParcial(queIdPendiente)
    }


}