package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosInfCobros
import es.albainformatica.albamobileandroid.DatosResCobros
import es.albainformatica.albamobileandroid.entity.CobrosEnt


@Dao
interface CobrosDao {

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


    @Query("SELECT A.* FROM cobros A" +
            " WHERE A.estado = 'N'")
            //" LEFT JOIN cabeceras B ON B.tipodoc = A.tipodoc AND B.alm = A.alm AND B.serie = A.serie AND B.numero = A.numero AND B.ejer = A.ejer" +
            //" WHERE A.estado = 'N' AND (B.estado <> 'P' OR B.estado IS NULL")
    fun abrirParaExportar(): MutableList<CobrosEnt>


    @Query("SELECT A.clienteId, A.tipoDoc, A.fechaCobro, A.cobro, A.serie, A.numero FROM Cobros A" +
            " WHERE (julianday(substr(A.fechaCobro, 7, 4) || '-' || substr(A.fechaCobro, 4, 2) || '-' ||" +
            " substr(A.fechacobro, 1, 2)) >= julianday(:queDesdeFecha))" +
            " AND (julianday(substr(A.fechacobro, 7, 4) || '-' || substr(A.fechacobro, 4, 2) || '-' || " +
            " substr(A.fechacobro, 1, 2)) <= julianday(:queHastaFecha))")
    fun abrirEntreFechas(queDesdeFecha: String, queHastaFecha: String): MutableList<DatosInfCobros>


    @Query("SELECT B.descripcion, TOTAL(REPLACE(A.cobro, ',', '.')) cobro FROM Cobros A" +
            " LEFT JOIN divisas B ON B.codigo = A.divisa" +
            " WHERE (julianday(substr(A.fechacobro, 7, 4) || '-' || substr(A.fechacobro, 4, 2) || '-' ||" +
            " substr(A.fechacobro, 1, 2)) >= julianday(:queDesdeFecha))" +
            " AND (julianday(substr(A.fechacobro, 7, 4) || '-' || substr(A.fechacobro, 4, 2) || '-' ||" +
            " substr(A.fechacobro, 1, 2)) <= julianday(:queHastaFecha))" +
            " GROUP BY A.divisa")
    fun abrirResDivisas(queDesdeFecha: String, queHastaFecha: String): MutableList<DatosResCobros>


    @Query("SELECT * FROM cobros WHERE numExport = :queNumExportacion")
    fun abrirExportacion(queNumExportacion: Int): MutableList<CobrosEnt>


    @Query("SELECT COUNT(*) numCobros FROM cobros" +
            " WHERE estado = 'N'")
            //" LEFT JOIN cabeceras B ON B.tipodoc = A.tipodoc AND B.alm = A.alm AND B.serie = A.serie AND B.numero = A.numero AND B.ejer = A.ejer" +
            //" WHERE A.estado = 'N' AND (B.estado <> 'P' OR B.estado IS NULL")
    fun hayCobrosParaEnviar(): Int


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