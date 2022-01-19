package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosInfStock
import es.albainformatica.albamobileandroid.entity.StockEnt


@Dao
interface StockDao {

    @Query("SELECT A.empresa, A.ent, A.sal, B.codigo, B.descripcion FROM Stock A" +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId" +
            " WHERE CAST(A.ent AS REAL) <> 0 OR CAST(A.sal AS REAL) <> 0 " +
            " ORDER BY B.codigo")
    fun getInfStock(): List<DatosInfStock>

    @Query("SELECT * FROM stock ORDER BY empresa")
    fun abrirParaFinDeDia(): List<StockEnt>


    @Query("DELETE FROM Stock")
    fun vaciar()

    @Insert
    fun insertar(stock: StockEnt)
}