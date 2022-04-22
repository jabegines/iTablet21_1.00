package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosCobrResPedidos
import es.albainformatica.albamobileandroid.DatosInfCobros
import es.albainformatica.albamobileandroid.DatosResCobros
import es.albainformatica.albamobileandroid.entity.CobrosEnt


@Dao
interface CobrosDao {


    @Query("SELECT A.tipoDoc, A.serie, A.numero, A.fechaCobro, A.cobro, A.anotacion, B.codigo, B.nombre, " +
            " C.descripcion descrDivisa, D.descripcion descrFPago, E.fecha fechaDoc FROM Cobros A" +
            " LEFT JOIN Clientes B ON B.clienteId = A.clienteId" +
            " LEFT JOIN Divisas C ON C.codigo = A.divisa" +
            " LEFT JOIN FormasPago D ON D.codigo = A.fPago" +
            " LEFT JOIN Cabeceras E ON E.tipoDoc = A.tipoDoc AND E.almacen = A.almacen AND E.serie = A.serie" +
            " AND E.numero = A.numero AND E.ejercicio = A.ejercicio")
    fun getResumenPedidos(): List<DatosCobrResPedidos>



    // Enviaremos aquellos cobros que sean de documentos que vienen de la gestión (vapunte > 0) o de documentos realizados en la tablet
    // que ya han sido exportados (estado del cobro = 'N' y estado del documento = 'X')
    @Query("SELECT A.tipoDoc, A.serie, A.numero, A.cobro, B.nombre, B.nombreComercial, 0 clienteId, '' fechaCobro " +
            " FROM Cobros A " +
            " LEFT JOIN Clientes B ON B.clienteId = A.clienteId " +
            " LEFT JOIN Cabeceras C ON C.tipoDoc = A.tipoDoc AND C.almacen = A.almacen " +
            " AND C.serie = A.serie AND C.numero = A.numero AND C.ejercicio = A.ejercicio " +
            " WHERE (A.vApunte > 0 OR (A.estado = 'N' AND C.estado = 'X')) " +
            " AND (julianday(substr(A.fechacobro, 7, 4) || '-' || substr(A.fechacobro, 4, 2) " +
            " || '-' || substr(A.fechacobro, 1, 2)) >= julianday(:desdeFecha)) " +
            " AND (julianday(substr(A.fechacobro, 7, 4) || '-' || substr(A.fechacobro, 4, 2) " +
            " || '-' || substr(A.fechacobro, 1, 2)) <= julianday(:hastaFecha))")
    fun getCobrosPorFechas(desdeFecha: String, hastaFecha: String): List<DatosInfCobros>


    @Query("SELECT cobroId FROM Cobros WHERE estado = 'N'")
    fun hayCobros(): Int


    @Query("SELECT * FROM Cobros WHERE clienteId = :queClienteId AND tipoDoc = :queTipoDoc " +
            " AND ejercicio = :queEjercicio AND empresa = :queEmpresa AND fechaCobro = :queFechaCobro " +
            " AND cobro = :queCobro AND vAlmacen = :queVAlmacen AND vPuesto = :queVPuesto " +
            " AND vApunte = :queVApunte AND vEjercicio = :queVEjercicio")
    fun existeCobro(queClienteId: Int, queTipoDoc: Short, queEjercicio: Short, queEmpresa: Short,
                    queFechaCobro: String, queCobro: String, queVAlmacen: String, queVPuesto: String,
                    queVApunte: String, queVEjercicio: String): CobrosEnt


    @Query("SELECT cobro FROM Cobros WHERE clienteId = :queCliente")
    fun dimeCobrosClte(queCliente: Int): MutableList<String>


    @Query("SELECT cobro FROM Cobros WHERE tipoDoc = :fTipoDoc" +
            " AND almacen = :fAlmacen AND serie = :fSerie AND numero = :fNumero" +
            " AND ejercicio = :fEjercicio AND empresa = :fEmpresa")
    fun dimeCobrosDoc(fTipoDoc: String, fAlmacen: String, fSerie: String,
                  fNumero: String, fEjercicio: String, fEmpresa: String): String


    @Query("DELETE FROM Cobros WHERE tipoDoc = :fTipoDoc" +
            " AND almacen = :fAlmacen AND serie = :fSerie AND numero = :fNumero" +
            " AND ejercicio = :fEjercicio")
    fun borrarCobroDoc(fTipoDoc: Short, fAlmacen: Short, fSerie: String, fNumero: Int, fEjercicio: Short)


    @Query("SELECT cobro FROM Cobros WHERE tipoDoc = :fTipoDoc" +
            " AND almacen = :fAlmacen AND serie = :fSerie AND numero = :fNumero" +
            " AND ejercicio = :fEjercicio")
    fun dimeCobradoDoc(fTipoDoc: Short, fAlmacen: Short, fSerie: String, fNumero: Int, fEjercicio: Short): String



    //@Query("SELECT A.*, B.formadepago, C.divisa descrdivisa FROM Cobros A" +
    @Query("SELECT A.* FROM Cobros A" +
            //" LEFT JOIN formaspago B ON B.codigo = A.fPago" +
            //" LEFT JOIN divisas C ON C.codigo = A.divisa" +
            " WHERE A.clienteId = :queCliente")
    fun abrir(queCliente: Int): MutableList<CobrosEnt>


    @Query("UPDATE Cobros SET estado = 'X', numExport = :queExportacion WHERE estado = 'N'")
    fun marcarComoExportados(queExportacion: Int)


    @Query("SELECT A.* FROM Cobros A" +
            " LEFT JOIN Cabeceras B ON B.tipoDoc = A.tipoDoc AND B.almacen = A.almacen AND B.serie = A.serie AND B.numero = A.numero AND B.ejercicio = A.ejercicio" +
            " WHERE A.estado = 'N' AND (B.estado <> 'P' OR B.estado IS NULL)")
    fun abrirParaExportar(): MutableList<CobrosEnt>


    @Query("SELECT A.clienteId, A.tipoDoc, A.fechaCobro, A.cobro, A.serie, A.numero, B.nombre, B.nombreComercial " +
            " FROM Cobros A" +
            " LEFT JOIN Clientes B ON B.clienteId = A.clienteId " +
            " WHERE (julianday(substr(A.fechaCobro, 7, 4) || '-' || SUBSTR(A.fechaCobro, 4, 2) || '-' ||" +
            " SUBSTR(A.fechacobro, 1, 2)) >= julianday(:queDesdeFecha))" +
            " AND (julianday(substr(A.fechacobro, 7, 4) || '-' || SUBSTR(A.fechacobro, 4, 2) || '-' || " +
            " SUBSTR(A.fechacobro, 1, 2)) <= julianday(:queHastaFecha))")
    fun abrirEntreFechas(queDesdeFecha: String, queHastaFecha: String): MutableList<DatosInfCobros>


    @Query("SELECT B.descripcion, TOTAL(A.cobro) cobro FROM Cobros A" +
            " LEFT JOIN divisas B ON B.codigo = A.divisa" +
            " WHERE (julianday(substr(A.fechacobro, 7, 4) || '-' || substr(A.fechacobro, 4, 2) || '-' ||" +
            " substr(A.fechacobro, 1, 2)) >= julianday(:queDesdeFecha))" +
            " AND (julianday(substr(A.fechacobro, 7, 4) || '-' || substr(A.fechacobro, 4, 2) || '-' ||" +
            " substr(A.fechacobro, 1, 2)) <= julianday(:queHastaFecha))" +
            " GROUP BY A.divisa")
    fun abrirResDivisas(queDesdeFecha: String, queHastaFecha: String): MutableList<DatosResCobros>


    @Query("SELECT * FROM Cobros WHERE numExport = :queNumExportacion")
    fun abrirExportacion(queNumExportacion: Int): MutableList<CobrosEnt>



    @Query("SELECT COUNT(*) numCobros FROM cobros WHERE numExport = :queNumExportacion")
    fun hayCobrosEnExport(queNumExportacion: Int): Int


    @Query("UPDATE Cobros SET estado = 'N' WHERE numExport = -1")
    fun revertirEstado()


    @Query("DELETE FROM cobros WHERE (estado = 'X'" +
            " AND (julianday(substr(fechacobro, 7, 4) || '-' || substr(fechacobro, 4, 2) || '-' || " +
            "substr(fechacobro, 1, 2)) <= julianday(:hastaFecha)))")
    fun borrarCobrosEnviados(hastaFecha: String)


    @Query("UPDATE cobros SET numExport = :queExportacion WHERE numExport = -1")
    fun actualizarNumPaquete(queExportacion: Int)


    @Insert
    fun insertar(cobro: CobrosEnt)
}