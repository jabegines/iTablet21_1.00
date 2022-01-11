package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.ListaArticulos
import es.albainformatica.albamobileandroid.entity.ArticulosEnt


@Dao
interface ArticulosDao {

    //@Query("SELECT A.*, B.clave, C.clave codAlternativo, I.codigo codigoIva, I.porcIva, " +
            //" S.ent, S.sal, S.entc, S.salc " +
            //" FROM Articulos A " +
            //" LEFT JOIN Busquedas B ON B.articuloId = A.articuloId AND B.tipo = 2" +
            //" LEFT JOIN Busquedas C ON C.articuloId = A.articuloId AND C.tipo = 6" +
            //" LEFT JOIN Ivas I ON I.tipo = A.tipoIva " +
            //" LEFT JOIN Stock S ON S.articuloId = A.articuloId " +
            //" WHERE A.articuloId = :queArticulo")
    @Query("SELECT * FROM Articulos WHERE articuloId = :queArticulo")
    fun existeArticulo(queArticulo: Int): ArticulosEnt


    @Query("SELECT DISTINCT A.articuloId, A.codigo, A.descripcion, A.ucaja, C.precio, C.dto, D.precio prCaja, E.porcIva," +
            " (F.ent - F.sal) stock, '' descrfto, G.idOferta" +
            " FROM Articulos A"  +
            " LEFT JOIN Busquedas B ON B.articuloId = A.articuloId AND B.tipo = 6" +
            " LEFT JOIN Tarifas C ON C.articuloId = A.articuloId AND C.tarifaId = :pTarifa" +
            " LEFT JOIN Tarifas D ON D.articuloId = A.articuloId AND D.tarifaId = :pTarifaCajas" +
            " LEFT JOIN Ivas E ON E.tipo = A.tipoiva" +
            " LEFT JOIN Stock F ON F.articuloId = A.articuloId" +
            " LEFT JOIN Ofertas G ON G.articuloId = A.articuloId AND G.empresa = :pEmpresa AND G.tarifa = :pTarifa" +
            " WHERE A.descripcion LIKE(:artBuscar) OR A.codigo LIKE(:artBuscar) OR B.clave LIKE(:artBuscar)" +
            " ORDER BY CASE WHEN :queOrdenacion = 0 THEN A.descripcion ELSE A.codigo END")
    fun getArticPorDescrCod(queOrdenacion: Short, artBuscar: String, pEmpresa: Int, pTarifa: Short,
                            pTarifaCajas: Short): List<ListaArticulos>


    @Query("SELECT DISTINCT A.articuloId, A.codigo, A.descripcion, A.ucaja, B.precio, B.dto, 0 prCaja, E.porcIva," +
            " (F.ent - F.sal) stock, '' descrfto, B.idOferta" +
            " FROM Articulos A, Ofertas B" +
            " LEFT JOIN Ivas E ON E.tipo = A.tipoiva" +
            " LEFT JOIN Stock F ON F.articuloId = A.articuloId" +
            " WHERE B.articuloId = A.articuloId")
    fun getArticPorPromoc(): List<ListaArticulos>


    @Query("SELECT codigo FROM Articulos WHERE articuloId = :queArticulo")
    fun getCodigo(queArticulo: Int): String


    @Query("DELETE FROM Articulos")
    fun vaciar()

    @Insert
    fun insertar(articulo: ArticulosEnt)
}