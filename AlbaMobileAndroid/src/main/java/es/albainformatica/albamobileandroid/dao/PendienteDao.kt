package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.PendienteEnt


@Dao
interface PendienteDao {

    @Query("SELECT B.generaCobro FROM pendiente A" +
            " LEFT JOIN formaspago B ON B.codigo = A.fPago" +
            " WHERE A.Almacen = :queAlmacen AND A.serie = :queSerie AND A.Numero = :queNumero" +
            " AND A.ejercicio = :queEjercicio AND Empresa = :queEmpresa")
    fun esContado(queEmpresa: Short, queAlmacen: Short, queSerie: String, queNumero: Int, queEjercicio: Short): String


    @Query("UPDATE Pendiente SET Cobrado = Cobrado + :queImporte WHERE PendienteId = :quePendienteId")
    fun actualizarCobrado(queImporte: String, quePendienteId: Int)


    @Query("UPDATE Pendiente SET numExport = :fNumPaquete WHERE numExport = -1")
    fun actualizarNumExport(fNumPaquete: Int)

    @Query("UPDATE Pendiente SET enviar = 'T' WHERE numExport = -1")
    fun revertirEstado()


    @Query("UPDATE Pendiente SET Estado = 'L' WHERE PendienteId = :quePendienteId")
    fun marcarComoLiquidado(quePendienteId: Int)

    @Query("UPDATE Pendiente SET Estado = 'CP' WHERE PendienteId = :quePendienteId")
    fun marcarComoCobrParcial(quePendienteId: Int)

    @Query("SELECT * FROM Pendiente WHERE PendienteId = :quePendienteId")
    fun abrirPendienteId(quePendienteId: Int): PendienteEnt


    @Query("SELECT FechaDoc FROM Pendiente WHERE clienteId = :queCliente AND importe <> cobrado")
    fun abrirTodosDocClte(queCliente: Int): List<String>


    @Query("DELETE FROM Pendiente WHERE tipoDoc = :queTipoDoc AND Empresa = :queEmpresa" +
            " AND Almacen = :queAlmacen AND Serie = :queSerie AND Numero = :queNumero AND ejercicio = :queEjercicio")
    fun borrarPdteDoc(queTipoDoc: Short, queEmpresa: Short, queAlmacen: Short, queSerie: String, queNumero: Int, queEjercicio: Short)


    @Query("UPDATE Pendiente SET enviar = 'T' WHERE tipoDoc = :queTipoDoc AND empresa = :queEmpresa" +
            " AND Almacen = :queAlmacen AND Serie = :queSerie AND Numero = :queNumero AND ejercicio = :queEjercicio")
    fun reenviar(queTipoDoc: Short, queEmpresa: Short, queAlmacen: Short, queSerie: String, queNumero: Int, queEjercicio: Short)


    @Query("SELECT FechaDoc FROM Pendiente WHERE clienteId = :queCliente AND empresa = :queEmpresa" +
            " AND CAST(importe AS REAL) <> CAST(cobrado AS REAL)" +
            " ORDER BY FechaDoc" +
            " LIMIT 1")
    fun abrirPorFDoc(queCliente: Int, queEmpresa: Int): List<String>


    @Query("SELECT COUNT(*) FROM Pendiente WHERE clienteId = :queCliente AND empresa = :queEmpresa" +
            " AND CAST (importe AS REAL) <> CAST(cobrado AS REAL)")
    fun dimeNumDocsClte(queCliente: Int, queEmpresa: Int): Int


    @Query("SELECT A.*, B.descripcion FROM Pendiente A " +
            " LEFT JOIN FormasPago B ON B.Codigo = A.FPago" +
            " WHERE A.clienteId = :queCliente AND CAST(A.importe AS REAL) <> CAST(A.cobrado AS REAL)" +
            " ORDER BY A.fechaVto")
    fun abrir(queCliente: Int): List<PendienteEnt>



    @Query("SELECT * FROM Pendiente WHERE Almacen = :queAlmacen" +
            " AND Serie = :queSerie AND Numero = :queNumero AND Ejercicio = :queEjercicio")
    fun abrirDocumento(queAlmacen: Short, queSerie: String, queNumero: Int, queEjercicio: Short): PendienteEnt


    @Query("SELECT * FROM Pendiente WHERE Serie = :queSerie AND Numero = :queNumero AND Ejercicio = :queEjercicio")
    fun abrirDocDiferido(queSerie: String, queNumero: Int, queEjercicio: Short): PendienteEnt


    @Query("UPDATE Pendiente SET FechaCartera = :queFechaPagare, Anotacion = :queAnotacion," +
            " Enviar = 'T', Flag = :queFlag WHERE PendienteId = :quePendienteId")
    fun actualizarFechaPagare(quePendienteId: Int, queFechaPagare: String, queAnotacion: String, queFlag: Int)


    @Query("SELECT pendienteId FROM Pendiente WHERE tipoDoc = :queTipoDoc" +
            " AND cAlmacen = :queAlmacen" +
            " AND cPuesto = :quePuesto" +
            " AND cApunte = :queApunte" +
            " AND cEjercicio = :queEjercicio")
    fun existeVencimiento(queTipoDoc: Short, queAlmacen: String, quePuesto: String, queApunte: String,
                          queEjercicio: String): Int


    @Query("SELECT fPago FROM pendiente WHERE Almacen = :queAlmacen" +
        " AND Serie= :queSerie AND Numero = :queNumero AND Ejercicio = :queEjercicio")
    fun abrirFPagoDoc(queAlmacen: Short, queSerie: String, queNumero: Int, queEjercicio: Short): String


    // Hemos quitado la comprobación de estado <> 'CP' por el bts de Maquinex nº: 53348,
    // donde teníamos un albarán parcialmente cobrado y luego recibíamos la factura diferida; al no borrarse
    // el albarán (estado='CP'), se daba el caso de tener el albarán y la factura en el pendiente del cliente

    //@Query("DELETE FROM pendiente WHERE (enviar <> 'T' AND estado <> 'L' AND estado <> 'CP') " +
    //        " OR (estado = 'L' AND numexport > 0)")
    @Query("DELETE FROM pendiente WHERE (enviar <> 'T' AND estado <> 'L') OR (estado = 'L' AND numexport > 0)")
    fun borrarEnviados()


    @Query("UPDATE pendiente SET numExport = :iSigExportacion WHERE estado = 'L' AND numExport IS NULL")
    fun numExp2VtosLiquidados(iSigExportacion: Int)

    @Query("SELECT * FROM Pendiente WHERE enviar = 'T'")
    fun abrirParaEnviar(): List<PendienteEnt>

    @Query("SELECT * FROM Pendiente WHERE numExport = :queNumExportacion")
    fun abrirPorNumExport(queNumExportacion: Int): List<PendienteEnt>

    @Query("UPDATE Pendiente SET enviar = 'F', numExport = :iSigExportacion WHERE enviar = 'T'")
    fun marcarNoEnviar(iSigExportacion: Int)


    @Query("SELECT * FROM Pendiente WHERE enviar = 'T'")
    fun getPendienteEnviar(): List<PendienteEnt>


    @Query("DELETE FROM Pendiente")
    fun vaciar()

    @Insert
    fun insertar(pdteEnt: PendienteEnt): Long

}