package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosRutero
import es.albainformatica.albamobileandroid.entity.RuterosEnt


@Dao
interface RuterosDao {

    @Query("SELECT 0 AS orden, clienteId, codigo, nombre, nombreComercial, tieneIncid FROM Clientes " +
            " WHERE cPostal = :queCodPostal " +
            " ORDER BY CASE " +
            " WHEN :queOrden = 1 THEN nombre " +
            " ELSE nombreComercial " +
            " END")
    fun abrirCodPostal(queCodPostal: String, queOrden: Short): List<DatosRutero>


    @Query("SELECT A.orden, A.clienteId, B.codigo, B.nombre, B.nombreComercial, B.tieneIncid" +
            " FROM Rutero A" +
            " JOIN Clientes B ON B.clienteId = A.clienteId" +
            " WHERE A.rutaId = :queRuta" +
            " ORDER BY A.orden ")
    fun abrirRuta(queRuta: Short): List<DatosRutero>


    @Query("DELETE FROM Rutero")
    fun vaciar()

    @Insert
    fun insertar(rutero: RuterosEnt)

}