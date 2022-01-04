package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosLinDocDif
import es.albainformatica.albamobileandroid.entity.LineasDifEnt


@Dao
interface LineasDifDao {

    @Query("SELECT A.codigo, A.descripcion, A.cajas, A.cantidad, A.precio, A.dto, A.importe, A.codigoIva, B.porcIva " +
            " FROM LineasDiferidas A" +
            " LEFT JOIN Ivas B ON B.codigo = A.codigoIva" +
            " WHERE A.cabeceraId = :queIdDocumento")
    fun getLineasDoc(queIdDocumento: Int): List<DatosLinDocDif>


    @Query("DELETE FROM LineasDiferidas")
    fun vaciar()

    @Insert
    fun insertar(lineaDif: LineasDifEnt)
}