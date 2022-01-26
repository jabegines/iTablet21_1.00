package es.albainformatica.albamobileandroid.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import es.albainformatica.albamobileandroid.DatosArtDesctOftVol
import es.albainformatica.albamobileandroid.DatosArticulo
import es.albainformatica.albamobileandroid.DatosGridView
import es.albainformatica.albamobileandroid.ListaArticulos
import es.albainformatica.albamobileandroid.entity.ArticulosEnt
import es.albainformatica.albamobileandroid.entity.BusquedasEnt


@Dao
interface ArticulosDao {

    @Query("SELECT A.articuloId FROM Articulos A " +
            " LEFT JOIN Busquedas B ON B.articuloId = A.articuloId AND B.tipo = 6 " +
            " WHERE A.descripcion LIKE(:queBuscar) OR A.codigo LIKE(:queBuscar) OR B.clave LIKE(:queBuscar)")
    fun bioBuscar(queBuscar: String): List<Int>


    @Query("SELECT A.articuloId FROM ArticClasif A " +
            " LEFT JOIN Articulos B ON B.articuloId =  A.articuloId " +
            " WHERE A.clasificadorId = :queCatalogo " +
            " ORDER BY CASE " +
            " WHEN :queOrdenacion = 1 THEN A.orden " +
            " WHEN :queOrdenacion = 2 THEN B.codigo " +
            " ELSE B.descripcion " +
            " END")
    fun abrirBioCatalogo(queCatalogo: Int, queOrdenacion: Int): List<Int>


    @Query("SELECT articuloId FROM Articulos " +
            " WHERE grupoId = :queGrupo AND departamentoId = :queDepartamento " +
            " ORDER BY CASE " +
            " WHEN :queOrdenacion = 2 THEN codigo " +
            " WHEN :queOrdenacion = 3 THEN descripcion " +
            " END")
    fun abrirBioDepartamento(queGrupo: Short, queDepartamento: Short, queOrdenacion: Int): List<Int>


    @Query("SELECT A.articuloId FROM Historico A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId " +
            " WHERE A.clienteId = :queCliente " +
            " ORDER BY CASE" +
            " WHEN :queOrdenacion = 2 THEN B.codigo " +
            " WHEN :queOrdenacion = 3 THEN B.descripcion " +
            " END")
    fun abrirBioHistorico(queCliente: Int, queOrdenacion: Int): List<Int>


    @Query("SELECT A.codigo, A.descripcion, I.codigo codigoIva, I.porcIva " +
            " FROM Articulos A " +
            " LEFT JOIN Ivas I ON I.tipo = A.tipoIva" +
            " WHERE A.articuloId = :queArticulo")
    fun datosArtDesctOftVol(queArticulo: Int): DatosArtDesctOftVol


    @Query("SELECT A.*, B.clave, C.clave codAlternativo, I.codigo codigoIva, I.porcIva, " +
            " S.ent, S.sal, S.entc, S.salc " +
            " FROM Articulos A " +
            " LEFT JOIN Busquedas B ON B.articuloId = A.articuloId AND B.tipo = 2" +
            " LEFT JOIN Busquedas C ON C.articuloId = A.articuloId AND C.tipo = 6" +
            " LEFT JOIN Ivas I ON I.tipo = A.tipoIva " +
            " LEFT JOIN Stock S ON S.articuloId = A.articuloId AND S.empresa = :queEmpresa" +
            " WHERE A.articuloId = :queArticulo")
    fun abrirUnArticulo(queArticulo: Int, queEmpresa: Short): DatosArticulo


    @Query("SELECT A.*, B.clave, C.clave codAlternativo, I.codigo codigoIva, I.porcIva, " +
            " SUM(S.ent) ent, SUM(S.sal) sal, S.entc, S.salc " +
            " FROM Articulos A " +
            " LEFT JOIN Busquedas B ON B.articuloId = A.articuloId AND B.tipo = 2" +
            " LEFT JOIN Busquedas C ON C.articuloId = A.articuloId AND C.tipo = 6" +
            " LEFT JOIN Ivas I ON I.tipo = A.tipoIva " +
            " LEFT JOIN Stock S ON S.articuloId = A.articuloId " +
            " WHERE A.articuloId = :queArticulo")
    fun abrirUnArtSum(queArticulo: Int): DatosArticulo



    @Query("SELECT A.*, B.clave, C.clave codAlternativo, I.codigo codigoIva, I.porcIva, " +
            " S.ent, S.sal, S.entc, S.salc " +
            " FROM Articulos A " +
            " LEFT JOIN Busquedas B ON B.articuloId = A.articuloId AND B.tipo = 2" +
            " LEFT JOIN Busquedas C ON C.articuloId = A.articuloId AND C.tipo = 6" +
            " LEFT JOIN Ivas I ON I.tipo = A.tipoIva " +
            " LEFT JOIN Stock S ON S.articuloId = A.articuloId " +
            " WHERE A.articuloId = :queArticulo")
    fun existeArticulo(queArticulo: Int): DatosArticulo


    @Query("SELECT DISTINCT A.articuloId, A.codigo, A.descripcion, A.uCaja, B.articuloId artOfert, " +
            " C.precio, C.dto, D.precio prCajas, D.dto dtoCajas, E.porcIva, F.ent, F.sal, F.entc, F.salc, " +
            " H.historicoId, 0 cantHco, 0 cajasHco, 0 precioHco, 0 dtoHco, '' fecha  " +
            " FROM Articulos A " +
            " LEFT JOIN Ofertas B ON B.articuloId = A.articuloId " +
            " LEFT JOIN Tarifas C ON C.articuloId = A.articuloId AND C.tarifaId = :queTarifa " +
            " LEFT JOIN Tarifas D ON D.articuloId = A.articuloId AND C.tarifaId = :queTrfCajas " +
            " LEFT JOIN Ivas E ON E.tipo = A.tipoIva " +
            " LEFT JOIN Stock F ON F.articuloId = A.articuloId " +
            " LEFT JOIN Historico H ON H.articuloId = A.articuloId AND H.clienteId = :queCliente " +
            " WHERE A.grupoId = :queGrupo AND A.departamentoId = :queDepartamento " +
            " ORDER BY CASE" +
            " WHEN :queOrdenacion = 0 THEN A.descripcion " +
            " ELSE A.codigo " +
            " END")
    fun abrirPGVConHco(queTarifa: Short, queTrfCajas: Short, queCliente: Int,
                                 queGrupo: Short, queDepartamento: Short, queOrdenacion: Short): List<DatosGridView>


    @Query("SELECT DISTINCT A.articuloId, A.codigo, A.descripcion, A.uCaja, B.articuloId artOfert, " +
            " C.precio, C.dto, D.precio prCajas, D.dto dtoCajas, E.porcIva, F.ent, F.sal, F.entc, F.salc, " +
            " 0 historicoId, 0 cantHco, 0 cajasHco, 0 precioHco, 0 dtoHco, '' fecha  " +
            " FROM Articulos A " +
            " LEFT JOIN Ofertas B ON B.articuloId = A.articuloId " +
            " LEFT JOIN Tarifas C ON C.articuloId = A.articuloId AND C.tarifaId = :queTarifa " +
            " LEFT JOIN Tarifas D ON D.articuloId = A.articuloId AND C.tarifaId = :queTrfCajas " +
            " LEFT JOIN Ivas E ON E.tipo = A.tipoIva " +
            " LEFT JOIN Stock F ON F.articuloId = A.articuloId " +
            " WHERE A.grupoId = :queGrupo AND A.departamentoId = :queDepartamento " +
            " ORDER BY CASE" +
            " WHEN :queOrdenacion = 0 THEN A.descripcion " +
            " ELSE A.codigo " +
            " END")
    fun abrirPGV(queTarifa: Short, queTrfCajas: Short, queGrupo: Short, queDepartamento: Short,
                                queOrdenacion: Short): List<DatosGridView>

    @Query("SELECT DISTINCT A.articuloId, A.codigo, A.descripcion, A.uCaja, B.articuloId artOfert, " +
            " C.precio, C.dto, D.precio prCajas, D.dto dtoCajas, E.porcIva, F.ent, F.sal, F.entc, F.salc, " +
            " H.historicoId, 0 cantHco, 0 cajasHco, 0 precioHco, 0 dtoHco, '' fecha  " +
            " FROM Articulos A " +
            " LEFT JOIN Ofertas B ON B.articuloId = A.articuloId " +
            " LEFT JOIN Tarifas C ON C.articuloId = A.articuloId AND C.tarifaId = :queTarifa " +
            " LEFT JOIN Tarifas D ON D.articuloId = A.articuloId AND C.tarifaId = :queTrfCajas " +
            " LEFT JOIN Ivas E ON E.tipo = A.tipoIva " +
            " LEFT JOIN Stock F ON F.articuloId = A.articuloId " +
            " LEFT JOIN Historico H ON H.articuloId = A.articuloId AND H.clienteId = :queCliente " +
            " WHERE (A.descripcion LIKE(:artBuscar) OR A.codigo LIKE(:artBuscar)) " +
            " AND A.grupoId = :queGrupo AND A.departamentoId = :queDepartamento " +
            " ORDER BY CASE" +
            " WHEN :queOrdenacion = 0 THEN A.descripcion " +
            " ELSE A.codigo " +
            " END")
    fun abrirBusqEnGrupoPGVConHco(artBuscar: String, queTarifa: Short, queTrfCajas: Short, queCliente: Int,
                                  queGrupo: Short, queDepartamento: Short, queOrdenacion: Short): List<DatosGridView>


    @Query("SELECT DISTINCT A.articuloId, A.codigo, A.descripcion, A.uCaja, B.articuloId artOfert, " +
            " C.precio, C.dto, D.precio prCajas, D.dto dtoCajas, E.porcIva, F.ent, F.sal, F.entc, F.salc, " +
            " 0 historicoId, 0 cantHco, 0 cajasHco, 0 precioHco, 0 dtoHco, '' fecha " +
            " FROM Articulos A " +
            " LEFT JOIN Ofertas B ON B.articuloId = A.articuloId " +
            " LEFT JOIN Tarifas C ON C.articuloId = A.articuloId AND C.tarifaId = :queTarifa " +
            " LEFT JOIN Tarifas D ON D.articuloId = A.articuloId AND C.tarifaId = :queTrfCajas " +
            " LEFT JOIN Ivas E ON E.tipo = A.tipoIva " +
            " LEFT JOIN Stock F ON F.articuloId = A.articuloId " +
            " WHERE (A.descripcion LIKE(:artBuscar) OR A.codigo LIKE(:artBuscar)) " +
            " AND A.grupoId = :queGrupo AND A.departamentoId = :queDepartamento " +
            " ORDER BY CASE" +
            " WHEN :queOrdenacion = 0 THEN A.descripcion " +
            " ELSE A.codigo " +
            " END")
    fun abrirBusqEnGrupoPGV(artBuscar: String, queTarifa: Short, queTrfCajas: Short,
                                  queGrupo: Short, queDepartamento: Short, queOrdenacion: Short): List<DatosGridView>

    @Query("SELECT DISTINCT A.articuloId, A.codigo, A.descripcion, A.uCaja, B.articuloId artOfert, " +
            " C.precio, C.dto, D.precio prCajas, D.dto dtoCajas, E.porcIva, F.ent, F.sal, F.entc, F.salc, " +
            " H.historicoId, 0 cantHco, 0 cajasHco, 0 precioHco, 0 dtoHco, '' fecha  " +
            " FROM Articulos A " +
            " LEFT JOIN Ofertas B ON B.articuloId = A.articuloId " +
            " LEFT JOIN Tarifas C ON C.articuloId = A.articuloId AND C.tarifaId = :queTarifa " +
            " LEFT JOIN Tarifas D ON D.articuloId = A.articuloId AND C.tarifaId = :queTrfCajas " +
            " LEFT JOIN Ivas E ON E.tipo = A.tipoIva " +
            " LEFT JOIN Stock F ON F.articuloId = A.articuloId " +
            " LEFT JOIN Historico H ON H.articuloId = A.articuloId AND H.clienteId = :queCliente " +
            " WHERE A.descripcion LIKE(:artBuscar) OR A.codigo LIKE(:artBuscar) " +
            " ORDER BY CASE" +
            " WHEN :queOrdenacion = 0 THEN A.descripcion " +
            " ELSE A.codigo " +
            " END")
    fun abrirBusqPGVConHco(artBuscar: String, queTarifa: Short, queTrfCajas: Short, queCliente: Int,
                                  queOrdenacion: Short): List<DatosGridView>


    @Query("SELECT DISTINCT A.articuloId, A.codigo, A.descripcion, A.uCaja, B.articuloId artOfert, " +
            " C.precio, C.dto, D.precio prCajas, D.dto dtoCajas, E.porcIva, F.ent, F.sal, F.entc, F.salc, " +
            " 0 historicoId, 0 cantHco, 0 cajasHco, 0 precioHco, 0 dtoHco, '' fecha  " +
            " FROM Articulos A " +
            " LEFT JOIN Ofertas B ON B.articuloId = A.articuloId " +
            " LEFT JOIN Tarifas C ON C.articuloId = A.articuloId AND C.tarifaId = :queTarifa " +
            " LEFT JOIN Tarifas D ON D.articuloId = A.articuloId AND C.tarifaId = :queTrfCajas " +
            " LEFT JOIN Ivas E ON E.tipo = A.tipoIva " +
            " LEFT JOIN Stock F ON F.articuloId = A.articuloId " +
            " WHERE A.descripcion LIKE(:artBuscar) OR A.codigo LIKE(:artBuscar) " +
            " ORDER BY CASE" +
            " WHEN :queOrdenacion = 0 THEN A.descripcion " +
            " ELSE A.codigo " +
            " END")
    fun abrirBusqPGV(artBuscar: String, queTarifa: Short, queTrfCajas: Short,
                           queOrdenacion: Short): List<DatosGridView>


    @Query("SELECT DISTINCT A.articuloId, B.codigo, B.descripcion, B.uCaja, C.articuloId artOfert, " +
            " D.precio, D.dto, E.precio prCajas, E.dto dtoCajas, G.porcIva, F.ent, F.sal, F.entc, F.salc, " +
            " H.historicoId, 0 cantHco, 0 cajasHco, 0 precioHco, 0 dtoHco, '' fecha  " +
            " FROM ArticClasif A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId" +
            " LEFT JOIN Ofertas C ON C.articuloId = A.articuloId " +
            " LEFT JOIN Tarifas D ON D.articuloId = A.articuloId AND D.tarifaId = :queTarifa " +
            " LEFT JOIN Tarifas E ON E.articuloId = A.articuloId AND E.tarifaId = :queTrfCajas " +
            " LEFT JOIN Ivas G ON G.tipo = B.tipoIva " +
            " LEFT JOIN Stock F ON F.articuloId = A.articuloId " +
            " LEFT JOIN Historico H ON H.articuloId = A.articuloId AND H.clienteId = :queCliente " +
            " WHERE A.clasificadorId = :queClasificador AND (B.descripcion LIKE(:artBuscar) OR B.codigo LIKE(:artBuscar))" +
            " ORDER BY CASE" +
            " WHEN :queOrdenacion = 0 THEN B.descripcion " +
            " ELSE B.codigo " +
            " END")
    fun abrirBusqEnClasifPGVConHco(artBuscar: String, queTarifa: Short, queTrfCajas: Short, queCliente: Int,
                           queClasificador: Int, queOrdenacion: Short): List<DatosGridView>


    @Query("SELECT DISTINCT A.articuloId, B.codigo, B.descripcion, B.uCaja, C.articuloId artOfert, " +
            " D.precio, D.dto, E.precio prCajas, E.dto dtoCajas, G.porcIva, F.ent, F.sal, F.entc, F.salc, " +
            " 0 historicoId, 0 cantHco, 0 cajasHco, 0 precioHco, 0 dtoHco, '' fecha  " +
            " FROM ArticClasif A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId" +
            " LEFT JOIN Ofertas C ON C.articuloId = A.articuloId " +
            " LEFT JOIN Tarifas D ON D.articuloId = A.articuloId AND D.tarifaId = :queTarifa " +
            " LEFT JOIN Tarifas E ON E.articuloId = A.articuloId AND E.tarifaId = :queTrfCajas " +
            " LEFT JOIN Ivas G ON G.tipo = B.tipoIva " +
            " LEFT JOIN Stock F ON F.articuloId = A.articuloId " +
            " WHERE A.clasificadorId = :queClasificador AND (B.descripcion LIKE(:artBuscar) OR B.codigo LIKE(:artBuscar))" +
            " ORDER BY CASE" +
            " WHEN :queOrdenacion = 0 THEN B.descripcion " +
            " ELSE B.codigo " +
            " END")
    fun abrirBusqEnClasifPGV(artBuscar: String, queTarifa: Short, queTrfCajas: Short,
                                   queClasificador: Int, queOrdenacion: Short): List<DatosGridView>


    @Query("SELECT DISTINCT A.articuloId, B.codigo, B.descripcion, B.uCaja, C.articuloId artOfert, " +
            " D.precio, D.dto, E.precio prCajas, E.dto dtoCajas, G.porcIva, F.ent, F.sal, F.entc, F.salc, " +
            " H.historicoId, 0 cantHco, 0 cajasHco, 0 precioHco, 0 dtoHco, '' fecha  " +
            " FROM ArticClasif A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId" +
            " LEFT JOIN Ofertas C ON C.articuloId = A.articuloId " +
            " LEFT JOIN Tarifas D ON D.articuloId = A.articuloId AND D.tarifaId = :queTarifa " +
            " LEFT JOIN Tarifas E ON E.articuloId = A.articuloId AND E.tarifaId = :queTrfCajas " +
            " LEFT JOIN Ivas G ON G.tipo = B.tipoIva " +
            " LEFT JOIN Stock F ON F.articuloId = A.articuloId " +
            " LEFT JOIN Historico H ON H.articuloId = A.articuloId AND H.clienteId = :queCliente " +
            " WHERE A.clasificadorId = :queClasificador AND B.articuloId > 0" +
            " ORDER BY CASE" +
            " WHEN :queOrdenacion = 0 THEN B.descripcion " +
            " ELSE B.codigo " +
            " END")
    fun abrirClasifPGVConHco(queTarifa: Short, queTrfCajas: Short, queCliente: Int,
                                   queClasificador: Int, queOrdenacion: Short): List<DatosGridView>


    @Query("SELECT DISTINCT A.articuloId, B.codigo, B.descripcion, B.uCaja, C.articuloId artOfert, " +
            " D.precio, D.dto, E.precio prCajas, E.dto dtoCajas, G.porcIva, F.ent, F.sal, F.entc, F.salc, " +
            " 0 historicoId, 0 cantHco, 0 cajasHco, 0 precioHco, 0 dtoHco, '' fecha " +
            " FROM ArticClasif A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId" +
            " LEFT JOIN Ofertas C ON C.articuloId = A.articuloId " +
            " LEFT JOIN Tarifas D ON D.articuloId = A.articuloId AND D.tarifaId = :queTarifa " +
            " LEFT JOIN Tarifas E ON E.articuloId = A.articuloId AND E.tarifaId = :queTrfCajas " +
            " LEFT JOIN Ivas G ON G.tipo = B.tipoIva " +
            " LEFT JOIN Stock F ON F.articuloId = A.articuloId " +
            " WHERE A.clasificadorId = :queClasificador AND B.articuloId > 0" +
            " ORDER BY CASE" +
            " WHEN :queOrdenacion = 0 THEN B.descripcion " +
            " ELSE B.codigo " +
            " END")
    fun abrirClasifPGV(queTarifa: Short, queTrfCajas: Short,
                             queClasificador: Int, queOrdenacion: Short): List<DatosGridView>


    @Query("SELECT DISTINCT A.articuloId, B.codigo, B.descripcion, B.uCaja, C.articuloId artOfert, " +
            " A.cantidad cantHco, A.cajas cajasHco, A.precio precioHco, A.dto dtoHco," +
            " F.precio, F.dto, A.fecha, 0 prCajas, 0 dtoCajas, D.porcIva, E.ent, E.sal, E.entc, E.salc, " +
            " 0 historicoId " +
            " FROM Historico A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId" +
            " LEFT JOIN Ofertas C ON C.articuloId = A.articuloId " +
            " LEFT JOIN Tarifas F ON F.articuloId = A.articuloId AND F.tarifaId = :queTarifa " +
            " LEFT JOIN Ivas D ON D.tipo = B.tipoIva " +
            " LEFT JOIN Stock E ON E.articuloId = A.articuloId " +
            " WHERE A.clienteId = :queCliente" +
            " ORDER BY CASE" +
            " WHEN :queOrdenacion = 0 THEN B.descripcion " +
            " ELSE B.codigo " +
            " END")
    fun abrirHistoricoPGV(queTarifa: Short, queCliente: Int, queOrdenacion: Short): List<DatosGridView>


    @Query("SELECT DISTINCT A.articuloId, B.codigo, B.descripcion, B.uCaja, C.articuloId artOfert, " +
            " A.cantidad cantHco, A.cajas cajasHco, A.precio precioHco, A.dto dtoHco," +
            " F.precio, F.dto, A.fecha, 0 prCajas, 0 dtoCajas, D.porcIva, E.ent, E.sal, E.entc, E.salc, " +
            " 0 historicoId " +
            " FROM Historico A " +
            " LEFT JOIN Articulos B ON B.articuloId = A.articuloId" +
            " LEFT JOIN Ofertas C ON C.articuloId = A.articuloId " +
            " LEFT JOIN Tarifas F ON F.articuloId = A.articuloId AND F.tarifaId = :queTarifa " +
            " LEFT JOIN Ivas D ON D.tipo = B.tipoIva " +
            " LEFT JOIN Stock E ON E.articuloId = A.articuloId " +
            " WHERE A.clienteId = :queCliente AND (B.descripcion LIKE(:artBuscar) OR B.codigo LIKE(:artBuscar))" +
            " ORDER BY CASE" +
            " WHEN :queOrdenacion = 0 THEN B.descripcion " +
            " ELSE B.codigo " +
            " END")
    fun abrirBusqEnHcoPGV(artBuscar: String, queTarifa: Short, queCliente: Int, queOrdenacion: Short): List<DatosGridView>


    @Query("SELECT DISTINCT A.articuloId, A.codigo, A.descripcion, A.uCaja, B.articuloId artOfert, " +
            " 0 cantHco, 0 cajasHco, 0 precioHco, 0 dtoHco," +
            " C.precio, C.dto, '' fecha, 0 prCajas, 0 dtoCajas, D.porcIva, E.ent, E.sal, E.entc, E.salc, " +
            " H.historicoId " +
            " FROM Articulos A, Ofertas B " +
            " LEFT JOIN Tarifas C ON C.articuloId = A.articuloId AND C.tarifaId = :queTarifa " +
            " LEFT JOIN Ivas D ON D.tipo = A.tipoIva " +
            " LEFT JOIN Stock E ON E.articuloId = A.articuloId " +
            " LEFT JOIN Historico H ON H.articuloId = A.articuloId AND H.clienteId = :queCliente " +
            " WHERE B.articuloId = A.articuloId" +
            " ORDER BY CASE" +
            " WHEN :queOrdenacion = 0 THEN A.descripcion " +
            " ELSE A.codigo " +
            " END")
    fun abrirSoloOftasPGVConHco(queTarifa: Short, queCliente: Int, queOrdenacion: Short): List<DatosGridView>


    @Query("SELECT DISTINCT A.articuloId, A.codigo, A.descripcion, A.uCaja, B.articuloId artOfert, " +
            " 0 cantHco, 0 cajasHco, 0 precioHco, 0 dtoHco," +
            " C.precio, C.dto, '' fecha, 0 prCajas, 0 dtoCajas, D.porcIva, E.ent, E.sal, E.entc, E.salc, " +
            " 0 historicoId " +
            " FROM Articulos A, Ofertas B " +
            " LEFT JOIN Tarifas C ON C.articuloId = A.articuloId AND C.tarifaId = :queTarifa " +
            " LEFT JOIN Ivas D ON D.tipo = A.tipoIva " +
            " LEFT JOIN Stock E ON E.articuloId = A.articuloId " +
            " WHERE B.articuloId = A.articuloId" +
            " ORDER BY CASE" +
            " WHEN :queOrdenacion = 0 THEN A.descripcion " +
            " ELSE A.codigo " +
            " END")
    fun abrirSoloOftasPGV(queTarifa: Short, queOrdenacion: Short): List<DatosGridView>


    @Query("SELECT DISTINCT A.articuloId, A.codigo, A.descripcion, A.ucaja, C.precio, C.dto, D.precio prCaja, E.porcIva," +
            " (F.ent - F.sal) stock, '' descrfto, G.idOferta" +
            " FROM Articulos A"  +
            " LEFT JOIN Busquedas B ON B.articuloId = A.articuloId AND B.tipo = 6" +
            " LEFT JOIN Tarifas C ON C.articuloId = A.articuloId AND C.tarifaId = :pTarifa" +
            " LEFT JOIN Tarifas D ON D.articuloId = A.articuloId AND D.tarifaId = :pTarifaCajas" +
            " LEFT JOIN Ivas E ON E.tipo = A.tipoiva" +
            " LEFT JOIN Stock F ON F.articuloId = A.articuloId AND F.empresa = :queEmpresa" +
            " LEFT JOIN Ofertas G ON G.articuloId = A.articuloId AND G.empresa = :pEmpresa AND G.tarifa = :pTarifa" +
            " WHERE A.descripcion LIKE(:artBuscar) OR A.codigo LIKE(:artBuscar) OR B.clave LIKE(:artBuscar)" +
            " ORDER BY CASE WHEN :queOrdenacion = 0 THEN A.descripcion ELSE A.codigo END")
    fun getArticPorDescrCod(queOrdenacion: Short, artBuscar: String, pEmpresa: Int, pTarifa: Short,
                            pTarifaCajas: Short, queEmpresa: Short): List<ListaArticulos>



    @Query("SELECT DISTINCT A.articuloId, A.codigo, A.descripcion, A.ucaja, C.precio, C.dto, D.precio prCaja, E.porcIva," +
            " SUM(F.ent - F.sal) stock, '' descrfto, G.idOferta" +
            " FROM Articulos A"  +
            " LEFT JOIN Busquedas B ON B.articuloId = A.articuloId AND B.tipo = 6" +
            " LEFT JOIN Tarifas C ON C.articuloId = A.articuloId AND C.tarifaId = :pTarifa" +
            " LEFT JOIN Tarifas D ON D.articuloId = A.articuloId AND D.tarifaId = :pTarifaCajas" +
            " LEFT JOIN Ivas E ON E.tipo = A.tipoiva" +
            " LEFT JOIN Stock F ON F.articuloId = A.articuloId" +
            " LEFT JOIN Ofertas G ON G.articuloId = A.articuloId AND G.empresa = :pEmpresa AND G.tarifa = :pTarifa" +
            " WHERE A.descripcion LIKE(:artBuscar) OR A.codigo LIKE(:artBuscar) OR B.clave LIKE(:artBuscar)" +
            " GROUP BY A.articuloId" +
            " ORDER BY CASE WHEN :queOrdenacion = 0 THEN A.descripcion ELSE A.codigo END")
    fun getArticPorDCSuma(queOrdenacion: Short, artBuscar: String, pEmpresa: Int, pTarifa: Short,
                            pTarifaCajas: Short): List<ListaArticulos>


    @Query("SELECT DISTINCT A.articuloId, A.codigo, A.descripcion, A.ucaja, B.precio, B.dto, 0 prCaja, E.porcIva," +
            " (F.ent - F.sal) stock, '' descrfto, B.idOferta" +
            " FROM Articulos A, Ofertas B" +
            " LEFT JOIN Ivas E ON E.tipo = A.tipoiva" +
            " LEFT JOIN Stock F ON F.articuloId = A.articuloId AND F.empresa = :queEmpresa" +
            " WHERE B.articuloId = A.articuloId")
    fun getArticPorPromoc(queEmpresa: Short): List<ListaArticulos>


    @Query("SELECT DISTINCT A.articuloId, A.codigo, A.descripcion, A.ucaja, B.precio, B.dto, 0 prCaja, E.porcIva," +
            " SUM(F.ent - F.sal) stock, '' descrfto, B.idOferta" +
            " FROM Articulos A, Ofertas B" +
            " LEFT JOIN Ivas E ON E.tipo = A.tipoiva" +
            " LEFT JOIN Stock F ON F.articuloId = A.articuloId" +
            " WHERE B.articuloId = A.articuloId" +
            " GROUP BY A.articuloId")
    fun getArticPorPromSuma(): List<ListaArticulos>


    @Query("SELECT codigo FROM Articulos WHERE articuloId = :queArticulo")
    fun getCodigo(queArticulo: Int): String

    @Query("SELECT articuloId FROM Articulos WHERE articuloId = :queArticulo")
    fun getArticulo(queArticulo: Int): Int

    @Query("SELECT * FROM Busquedas WHERE clave = :queCodigo")
    fun existeCodigo(queCodigo: String): BusquedasEnt


    @Query("DELETE FROM Articulos")
    fun vaciar()

    @Insert
    fun insertar(articulo: ArticulosEnt)
}