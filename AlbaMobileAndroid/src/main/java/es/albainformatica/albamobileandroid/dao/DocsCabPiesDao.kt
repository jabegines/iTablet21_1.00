package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.entity.DocsCabPiesEnt


@Dao
interface DocsCabPiesDao {

    @Query("SELECT Cadena FROM DocsCabPies WHERE Empresa = :queEmpresa AND Valor = 'TCABDOC01'")
    fun cabeceraDoc(queEmpresa: Short): String


    @Query("SELECT Cadena FROM DocsCabPies WHERE Empresa = :queEmpresa AND Valor LIKE 'TCABDOC%'" +
            " AND Entero > 1 AND Cadena <> ''")
    fun lineasCabDoc(queEmpresa: Short): MutableList<String>


    @Query("SELECT Cadena FROM DocsCabPies WHERE Empresa = :queEmpresa AND Valor LIKE 'TPIEDOC%'" +
            " AND Cadena <> ''")
    fun lineasPieDoc(queEmpresa: Short): MutableList<String>


    @Query("SELECT Cadena FROM DocsCabPies WHERE Empresa = :queEmpresa AND Valor LIKE 'TPIEDOC%'" +
            " AND Entero = :queLinea")
    fun lineaPieDoc(queEmpresa: Short, queLinea: Short): String


    @Query("SELECT Cadena FROM DocsCabPies WHERE Empresa = :queEmpresa AND Valor LIKE 'TCABDOC%'" +
            " AND Entero = :queLinea")
    fun lineaCabeceraDoc(queEmpresa: Short, queLinea: Short): String


    @Query("DELETE FROM DocsCabPies")
    fun vaciar()

    @Insert
    fun insertar(docCabPie: DocsCabPiesEnt)
}