package es.albainformatica.albamobileandroid.dao

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.FormasPagoEnt


@Dao
interface FormasPagoDao {

    @Query("SELECT Descripcion FROM FormasPago WHERE GeneraCobro = 'T' LIMIT 1")
    fun primeraDescrContado(): String


    @Query("SELECT GeneraCobro FROM FormasPago WHERE Codigo = :queCodigo")
    fun fPagoEsContado(queCodigo: String): String


    @Query("SELECT * FROM FormasPago")
    fun getFPago2Selec(): List<FormasPagoEnt>

    @Query("SELECT * FROM FormasPago WHERE GeneraCobro = 'T'")
    fun getFPagoSoloCont2Selec(): List<FormasPagoEnt>


    @Query("SELECT descripcion FROM FormasPago WHERE Codigo = :queCodigo")
    fun getDescripcion(queCodigo: String): String


    @Query("SELECT codigo _id, descripcion FROM FormasPago")
    fun getAllFPago(): Cursor

    @Query("SELECT Codigo _id, Descripcion, PideAnotacion, Anotacion FROM FormasPago" +
            " WHERE GeneraCobro = 'T' AND PideDivisas = 'T' ORDER BY Orden")
    fun abrirSoloContado(): Cursor


    @Query("SELECT codigo FROM FormasPago WHERE descripcion = :queDescripcion")
    fun existeDescr(queDescripcion: String): String


    @Query("SELECT generaCobro FROM FormasPago WHERE Codigo = :queFPago")
    fun getGeneraCobro(queFPago: String): String

    @Query("SELECT Descripcion FROM FormasPago WHERE Codigo = :queFPago")
    fun getDescrFPago(queFPago: String): String


    @Insert
    fun insertar(formaPago: FormasPagoEnt)

    @Query("DELETE FROM FormasPago")
    fun vaciar()
}