package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.StockEnt


@Dao
interface StockDao {

    @Query("SELECT * FROM stock ORDER BY empresa")
    fun abrirParaFinDeDia(): List<StockEnt>


    @Query("DELETE FROM Stock")
    fun vaciar()

    @Insert
    fun insertar(stock: StockEnt)
}