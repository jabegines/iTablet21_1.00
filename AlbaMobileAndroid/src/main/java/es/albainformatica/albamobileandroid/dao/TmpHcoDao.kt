package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.TmpHcoEnt


@Dao
interface TmpHcoDao {


    @Query("SELECT * FROM TmpHco")
    fun getAllLineas(): List<TmpHcoEnt>


    @Query("UPDATE TmpHco SET cajas = :queCajas, cantidad = :queCantidad, piezas =  :quePiezas, " +
            " precio = :quePrecio, precioII = :quePrecioII, dto = :queDto, dtoImpte = :queDtoImpte, " +
            " dtoImpteII = :queDtoImpII, codigoIva = :queCodIva, tasa1 = :queTasa1, tasa2 = :queTasa2, " +
            " lote = :queLote, almacenPedido = :queAlmPedido, textoLinea = :queTextoLinea, flag = :queFlag, " +
            " flag3 = :queFlag3, flag5 = :queFlag5 " +
            " WHERE tmpHcoId = :queTmpHcoId")
    fun actualizar(queTmpHcoId: Int, queCajas: String, queCantidad: String, quePiezas: String, quePrecio: String,
                    quePrecioII: String, queDto: String, queDtoImpte: String, queDtoImpII: String,
                    queCodIva: Short, queTasa1: String, queTasa2: String, queLote: String, queAlmPedido: Short,
                    queTextoLinea: String, queFlag: Int, queFlag3: Int, queFlag5: Int)


    @Query("DELETE FROM TmpHco")
    fun vaciar()

    @Insert
    fun insertar(tempHco: TmpHcoEnt)
}