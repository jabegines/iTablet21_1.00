package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.IvasEnt


@Dao
interface IvasDao {

    @Query("SELECT * FROM Ivas WHERE codigo = :queCodigo")
    fun getCodigoIva(queCodigo: Short): IvasEnt


    @Query("SELECT * FROM Ivas WHERE tipo = :queTipoIva")
    fun getDatosIva(queTipoIva: Short): IvasEnt


    @Query("DELETE FROM Ivas")
    fun vaciar()

    @Insert
    fun insertar(iva: IvasEnt)
}