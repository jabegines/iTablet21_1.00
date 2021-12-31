package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.ListOftVol
import es.albainformatica.albamobileandroid.entity.OfertasEnt


@Dao
interface OfertasDao {

    @Query("SELECT articuloId FROM Ofertas WHERE idOferta = :queIdOfta")
    fun getAllArtOftaId(queIdOfta: Int): MutableList<Int>


    @Query("SELECT A.idOferta, B.articuloDesct, B.tarifa, 0 importe FROM Ofertas A" +
            " LEFT JOIN OftVolumen B ON B.oftVolumenId = A.idOferta" +
            " WHERE A.articuloId = :queArticulo AND A.empresa = :queEmpresa AND A.tipoOferta = 7" +
            " AND julianday(A.fFinal) >= julianday(:queFecha)" +
            " AND B.tarifa = :queTarifaLin")
    fun getOftVolArt(queArticulo: Int, queEmpresa: Short, queTarifaLin: Short, queFecha: String): MutableList<ListOftVol>


    @Query("SELECT * FROM Ofertas WHERE articuloId = :queArticulo AND empresa = :queEmpresa AND tarifa = :queTarifa" +
            " AND formato = :queFormato AND julianday(fFinal) >= julianday(:queFecha)")
    fun getOftaVtaFto(queArticulo: Int, queEmpresa: Int, queTarifa: Short, queFormato: Short, queFecha: String): OfertasEnt


    @Query("SELECT * FROM Ofertas WHERE articuloId = :queArticulo AND empresa = :queEmpresa AND tarifa = :queTarifa" +
            " AND julianday(fFinal) >= julianday(:queFecha)")
    fun getOftaVtaArt(queArticulo: Int, queEmpresa: Int, queTarifa: Short, queFecha: String): OfertasEnt


    @Query("SELECT articuloId FROM Ofertas WHERE empresa = :queEmpresa AND tarifa = :queTarifa")
    fun getAllOftas(queEmpresa: Int, queTarifa: Short): MutableList<Int>

    @Query("SELECT * FROM Ofertas WHERE articuloId = :queArticulo AND empresa = :queEmpresa AND tarifa = :queTarifa")
    fun getOftaArt(queArticulo: Int, queEmpresa: Int, queTarifa: Short): OfertasEnt


    @Query("SELECT precio FROM Ofertas WHERE articuloId = :queArticulo AND empresa = :queEmpresa LIMIT 1")
    fun getPrOferta(queArticulo: Int, queEmpresa: Int): String


    @Query("SELECT dto FROM Ofertas WHERE articuloId = :queArticulo AND empresa = :queEmpresa LIMIT 1")
    fun getDtoOferta(queArticulo: Int, queEmpresa: Int): String


    @Query("SELECT articuloId FROM Ofertas WHERE articuloId = :queArticulo AND empresa = :queEmpresa LIMIT 1")
    fun articuloEnOfta(queArticulo: Int, queEmpresa: Int): Int


    @Query("DELETE FROM Ofertas")
    fun vaciar()

    @Insert
    fun insertar(oferta: OfertasEnt)
}