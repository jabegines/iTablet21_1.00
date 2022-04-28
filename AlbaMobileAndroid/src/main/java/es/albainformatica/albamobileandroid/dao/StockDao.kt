package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import es.albainformatica.albamobileandroid.DatosInfStock
import es.albainformatica.albamobileandroid.DatosStock
import es.albainformatica.albamobileandroid.entity.StockEnt


@Dao
interface StockDao {

    @Query("SELECT articuloId FROM Stock " +
            " WHERE articuloId = :queArticulo AND empresa = :queEmpresa")
    fun existeArtYEmpresa(queArticulo: Int, queEmpresa: Short): Int


    @Query("SELECT ent unidades, entc cajas FROM Stock " +
            " WHERE articuloId = :queArticulo AND empresa = :queEmpresa")
    fun getEntrArtEmpr(queArticulo: Int, queEmpresa: Short): DatosStock


    @Query("SELECT sal unidades, salc cajas FROM Stock " +
            " WHERE articuloId = :queArticulo AND empresa = :queEmpresa")
    fun getSalArtEmpr(queArticulo: Int, queEmpresa: Short): DatosStock


    @Query("SELECT A.empresa, A.ent, A.sal, B.codigo, B.descripcion FROM Stock A" +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId" +
            " WHERE CAST(A.ent AS REAL) <> 0 OR CAST(A.sal AS REAL) <> 0 " +
            " ORDER BY B.codigo")
    fun getInfStock(): List<DatosInfStock>


    @Query("SELECT * FROM stock ORDER BY empresa")
    fun abrirParaFinDeDia(): List<StockEnt>


    @Query("DELETE FROM Stock")
    fun vaciar()

    @Update
    fun actualizar(stock: StockEnt)

    @Insert
    fun insertar(stock: StockEnt)
}