package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.OftCantRangosEnt


@Dao
interface  OftCantRangosDao {

    @Query("SELECT * FROM OftCantRangos WHERE articuloId = :queArticulo")
    fun getAllOftArt(queArticulo: Int): List<OftCantRangosEnt>

    @Query("SELECT COUNT(*) contador FROM OftCantRangos")
    fun getCountOftCant(): Int


    @Query("DELETE FROM OftCantRangos")
    fun vaciar()

    @Insert
    fun insertar(oftCantRangos: OftCantRangosEnt)
}