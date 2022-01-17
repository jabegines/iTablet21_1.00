package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.ClasifParaCat
import es.albainformatica.albamobileandroid.entity.ClasificadoresEnt


@Dao
interface ClasificadoresDao {


    // En la tabla de clasificadores el flag 0 nos indicará que el registro pertenece a la clasificación avanzada
    // y el flag 1 nos indicará que el registro es un catálogo.
    @Query("SELECT * FROM Clasificadores WHERE padre = :quePadre AND nivel = :queNivel " +
            " AND flag = 0 ORDER BY orden")
    fun getClasifPadre(quePadre: Int, queNivel: Int): List<ClasificadoresEnt>


    @Query("SELECT A.clasificadorId, A.descripcion, " +
            " (SELECT COUNT(*) FROM ArticClasif WHERE clasificadorId = A.clasificadorId) numArticulos " +
            " FROM Clasificadores A " +
            " WHERE A.flag = 1 " +
            " ORDER BY A.orden")
    fun abrirBioCatalogo(): List<ClasifParaCat>



    @Query("SELECT * FROM Clasificadores WHERE flag = 1 ORDER BY orden")
    fun abrirCatalogos(): List<ClasificadoresEnt>


    @Query("DELETE FROM Clasificadores")
    fun vaciar()

    @Insert
    fun insertar(clasificador: ClasificadoresEnt)
}