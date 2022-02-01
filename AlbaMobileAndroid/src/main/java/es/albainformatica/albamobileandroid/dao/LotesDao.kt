package es.albainformatica.albamobileandroid.dao

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.LotesEnt


@Dao
interface LotesDao {

    @Query("UPDATE Lotes SET stock = :queStock WHERE articuloId = :queArticulo AND lote = :queLote AND empresa = :queEmpresa")
    fun actualizarStock(queArticulo: Int, queStock: String, queLote: String, queEmpresa: Short)


    @Query("SELECT * FROM Lotes WHERE articuloId = :queArticulo AND lote = :queLote AND empresa = :queEmpresa")
    fun existeArtYLote(queArticulo: Int, queLote: String, queEmpresa: Short): LotesEnt


    @Query("SELECT stock FROM Lotes WHERE articuloId = :queArticulo AND lote = :queLote")
    fun getStockLote(queArticulo:  Int, queLote: String): String


    @Query("SELECT * FROM Lotes WHERE articuloId = :queArticulo" +
            " AND CAST(stock AS REAL) > 0")
    fun getAllLotesArticulo(queArticulo: Int): List<LotesEnt>

    @Query("SELECT loteId, empresa, articuloId, lote, SUM(stock) stock, stockPiezas, flag FROM Lotes " +
            " WHERE articuloId = :queArticulo " +
            " AND CAST(stock AS REAL) > 0 " +
            " GROUP BY lote")
    fun getAllLotesArtSum(queArticulo: Int): List<LotesEnt>


    @Query("SELECT * FROM Lotes ORDER BY Empresa")
    fun getAllLotes(): List<LotesEnt>

    @Query("DELETE FROM Lotes")
    fun vaciar()

    @Insert
    fun insertar(lote: LotesEnt)
}