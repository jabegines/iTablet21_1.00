package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import es.albainformatica.albamobileandroid.entity.NotasCltesEnt

@Dao
interface NotasCltesDao {

    @Query("UPDATE NotasCltes SET numExport = :fNumPaquete WHERE numExport = -1")
    fun marcarNumExport(fNumPaquete: Int)

    @Query("SELECT * FROM NotasCltes WHERE clienteId = :queCliente" +
            " ORDER BY fecha DESC")
    fun abrirUnCliente(queCliente: Int): MutableList<NotasCltesEnt>


    @Query("UPDATE NotasCltes SET estado = 'XN', numExport = :iSigExportacion" +
            " WHERE estado = 'N'")
    fun marcarNuevasComoExport(iSigExportacion: Int)

    @Query("UPDATE NotasCltes SET estado = 'XM', numExport = :iSigExportacion"  +
            " WHERE estado = 'M'")
    fun marcarModifComoExport(iSigExportacion: Int)


    @Query("SELECT * FROM NotasCltes WHERE estado = 'N' OR estado = 'M'")
    fun abrirParaEnviar(): MutableList<NotasCltesEnt>


    @Query("SELECT * FROM NotasCltes WHERE numExport = :queNumExportacion")
    fun abrirNumExp(queNumExportacion: Int): MutableList<NotasCltesEnt>


    @Query("DELETE FROM NotasCltes WHERE (estado <> 'N' and estado <> 'M') OR estado IS NULL")
    fun borrarViejas()

    @Query("DELETE FROM NotasCltes")
    fun vaciar()


    @Update
    fun actualizar(notaClte: NotasCltesEnt)

    @Insert
    fun insertar(notaClte: NotasCltesEnt)

}