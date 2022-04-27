package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosEmpresas
import es.albainformatica.albamobileandroid.entity.EmpresasEnt


@Dao
interface EmpresasDao {

    @Query("SELECT venderIvaIncl FROM Empresas WHERE codigo = :queEmpresa")
    fun getIvaIncluido(queEmpresa: Int): String


    @Query("SELECT codigo, nombreFiscal FROM Empresas WHERE nombreFiscal <> ''")
    fun getAllEmpresas(): MutableList<DatosEmpresas>


    @Query("SELECT nombreFiscal FROM Empresas WHERE codigo = :queEmpresa")
    fun getNombreEmpresa(queEmpresa: Int): String

    @Query("SELECT codigo FROM Empresas")
    fun getCodigoEmpresa(): Int


    @Query("DELETE FROM Empresas")
    fun vaciar()

    @Insert
    fun insertar(almacen: EmpresasEnt)
}