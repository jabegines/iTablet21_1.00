package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.database.Cursor
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.Comunicador.Companion.fConfiguracion
import es.albainformatica.albamobileandroid.dao.*
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.FormatosEnt


class ArticulosClase(val contexto: Context) {
    private val articulosDao: ArticulosDao? = MyDatabase.getInstance(contexto)?.articulosDao()
    private val ofertasDao: OfertasDao? = MyDatabase.getInstance(contexto)?.ofertasDao()
    private val articDatAdicDao: ArticDatAdicDao? = MyDatabase.getInstance(contexto)?.articDatAdicDao()
    private var formatosDao: FormatosDao? = MyDatabase.getInstance(contexto)?.formatosDao()
    private var historicoDao: HistoricoDao? = MyDatabase.getInstance(contexto)?.historicoDao()
    private var ivasDao: IvasDao? = MyDatabase.getInstance(contexto)?.ivasDao()
    private var ftosLineasDao: FtosLineasDao? = MyDatabase.getInstance(contexto)?.ftosLineasDao()
    private var fLotes: LotesClase = LotesClase(contexto)

    lateinit var lArticulos: List<Int>
    lateinit var lArtGridView: List<DatosGridView>

    var cDatAdicionales: Cursor? = null

    var fArticulo = 0
    var fCodigo: String = ""
    var fCodBarras: String = ""
    var fDescripcion: String = ""
    var fEmpresa: Short = 0
    var fCodIva: Short = 0
    var fPorcIva: Double = 0.0
    var fGrupo: Short = 0
    var fDepartamento: Short = 0
    var fUCaja: Double = 0.0
    var fCodBCajas: Boolean = false
    var fTarifaCajas: Boolean = false
    var fCodProv: Int = 0
    var fTasa1: Double = 0.0
    var fTasa2: Double = 0.0
    private var fFlag1: Int = 0
    private var fFlag2: Int = 0
    var fEnlace: Int = 0
    var fCodAlternativo: String = ""
    var fPeso: Double = 0.0
    private var entradas: Double = 0.0
    private var salidas: Double = 0.0



    fun close() {
        cDatAdicionales?.close()
    }


    // Por ahora, la diferencia entre abrirUnArticulo y existeArticulo es la empresa, que la tomamos
    // en cuenta en abrirUnArticulo para mostrar el stock del artículo en dicha empresa
    fun abrirUnArticulo(queArticulo: Int, queEmpresa: Short): Boolean {

        val datosArticulo: DatosArticulo
        if (fConfiguracion.sumarStockEmpresas()) {
            datosArticulo = articulosDao?.abrirUnArtSum(queArticulo) ?: DatosArticulo()
        }
        else {
            datosArticulo = articulosDao?.abrirUnArticulo(queArticulo, queEmpresa) ?: DatosArticulo()
        }

        return if (datosArticulo.articuloId > 0) {
            fArticulo = queArticulo
            fEmpresa = queEmpresa
            fCodigo = datosArticulo.codigo
            fCodBarras = datosArticulo.clave
            fDescripcion = datosArticulo.descripcion

            fCodIva = datosArticulo.codigoIva
            fPorcIva = if(datosArticulo.porcIva != "") datosArticulo.porcIva.replace(',', '.').toDouble()
                        else 0.0

            fTasa1 = if (datosArticulo.tasa1 != "") datosArticulo.tasa1.replace(',', '.').toDouble()
                        else 0.0
            fTasa2 = if (datosArticulo.tasa2 != "") datosArticulo.tasa2.replace(',', '.').toDouble()
                        else 0.0

            fGrupo = datosArticulo.grupoId
            fDepartamento = datosArticulo.departamentoId
            fCodProv = datosArticulo.proveedorId
            fFlag1 = datosArticulo.flag1
            fFlag2 = datosArticulo.flag2
            fEnlace = datosArticulo.enlace
            fCodAlternativo = datosArticulo.codAlternativo ?: ""
            fCodProv = datosArticulo.proveedorId
            fPeso = if (datosArticulo.peso != "") datosArticulo.peso.replace(',', '.').toDouble() else 0.0
            fUCaja = if (datosArticulo.uCaja != "") datosArticulo.uCaja.replace(',', '.').toDouble() else 0.0
            entradas = if (datosArticulo.ent != null) datosArticulo.ent?.replace(',', '.')?.toDouble() ?: 0.0 else 0.0
            salidas = if (datosArticulo.sal != null) datosArticulo.sal?.replace(',', '.')?.toDouble() ?: 0.0 else 0.0

            return true
        }
        else false
    }


    fun existeArticulo(queArticulo: Int): Boolean {

        val datosArticulo = articulosDao?.existeArticulo(queArticulo) ?: DatosArticulo()

        return if (datosArticulo.articuloId > 0) {
            fArticulo = queArticulo
            fCodigo = datosArticulo.codigo
            fCodBarras = datosArticulo.clave
            fDescripcion = datosArticulo.descripcion

            fCodIva = datosArticulo.codigoIva
            fPorcIva = if(datosArticulo.porcIva != "") datosArticulo.porcIva.replace(',', '.').toDouble() else 0.0
            fTasa1 = if (datosArticulo.tasa1 != "") datosArticulo.tasa1.replace(',', '.').toDouble() else 0.0
            fTasa2 = if (datosArticulo.tasa2 != "") datosArticulo.tasa2.replace(',', '.').toDouble() else 0.0
            fGrupo = datosArticulo.grupoId
            fDepartamento = datosArticulo.departamentoId
            fCodProv = datosArticulo.proveedorId
            fFlag1 = datosArticulo.flag1
            fFlag2 = datosArticulo.flag2
            fEnlace = datosArticulo.enlace
            fCodAlternativo = datosArticulo.codAlternativo ?: ""
            fCodProv = datosArticulo.proveedorId
            fPeso = if (datosArticulo.peso != "") datosArticulo.peso.replace(',', '.').toDouble() else 0.0
            fUCaja = if (datosArticulo.uCaja != "") datosArticulo.uCaja.replace(',', '.').toDouble() else 0.0
            entradas = if (datosArticulo.ent != null) datosArticulo.ent?.replace(',', '.')?.toDouble() ?: 0.0 else 0.0
            salidas = if (datosArticulo.sal != null) datosArticulo.sal?.replace(',', '.')?.toDouble() ?: 0.0 else 0.0

            true
        }
        else false

        // TODO
        /*
        cursor = dbAlba.rawQuery(
            "SELECT A.*, B.Clave, C.Clave codalternativo, I.codigo codiva, I.iva porciva,"
                    + " S.ent, S.sal, S.entc, S.salc"
                    + " FROM articulos A"
                    + " LEFT JOIN busquedas B ON B.articulo = A.articulo AND B.tipo = 2"
                    + " LEFT JOIN busquedas C ON C.articulo = A.articulo AND C.tipo = 6"
                    + " LEFT JOIN ivas I ON I.tipo = A.tipoiva"
                    + " LEFT JOIN stock S ON S.articulo = A.articulo"
                    + " WHERE A.articulo = " + QueArticulo, null)

        if (cursor.moveToFirst()) {
            fArticulo = getArticulo()
            if (!fCodBCajas) fUCaja = getUCaja()

            fCodProv = cursor.getString(cursor.getColumnIndex("prov"))

            return true
        } else return false
        */
    }


    /*
    private fun abrirTarifas(queArticulo: Int) {
        // Si el artículo tiene formatos el cursor cTarifas saldrá de la tabla "trfformatos".
        if (usarFormatos()) {
            // En la consulta repetimos el campo precio porque nos hará falta en FichaArticuloActivity.setViewBinder.
            cTarifas = dbAlba.rawQuery(
                "SELECT A.articulo _id, A.tarifa, A.precio, A.precio priva,"
                        + " A.dto, B.tarifa nombretrf, C.descr nombrefto FROM trfformatos A"
                        + " LEFT OUTER JOIN cnftarifas B ON B.codigo = A.tarifa"
                        + " LEFT OUTER JOIN formatos C ON C.codigo = A.formato"
                        + " WHERE A.articulo =" + queArticulo, null
            )
        } else {
            // En la consulta repetimos el campo precio porque nos hará falta en FichaArticuloActivity.setViewBinder.
            cTarifas = dbAlba.rawQuery(
                ("SELECT A.articulo _id, A.tarifa, A.precio, A.precio priva,"
                        + " A.dto, B.tarifa nombretrf FROM tarifas A"
                        + " LEFT OUTER JOIN cnftarifas B ON B.codigo = A.tarifa"
                        + " WHERE A.articulo =" + queArticulo), null
            )
        }
    }
    */


    // Catálogos Bionat
    // =============================================================================
    fun abrirBioCatalogo(queCatalogo: Int, fOrdenacion: Int): Boolean {

        lArticulos = articulosDao?.abrirBioCatalogo(queCatalogo, fOrdenacion) ?: emptyList<Int>().toMutableList()
        return (lArticulos.count() > 0)
    }

    fun abrirBioDepartamento(queGrupo: Int, queDepartamento: Int, fOrdenacion: Int): Boolean {

        lArticulos = articulosDao?.abrirBioDepartamento(queGrupo.toShort(), queDepartamento.toShort(), fOrdenacion) ?: emptyList<Int>().toMutableList()
        return (lArticulos.count() > 0)
    }

    fun abrirBioHistorico(queCliente: Int, fOrdenacion: Int): Boolean {

        lArticulos = articulosDao?.abrirBioHistorico(queCliente, fOrdenacion) ?: emptyList<Int>().toMutableList()
        return (lArticulos.count() > 0)
    }


    fun bioBuscar(queBuscar: String): Boolean {

        lArticulos = articulosDao?.bioBuscar("%$queBuscar%") ?: emptyList<Int>().toMutableList()
        return (lArticulos.count() > 0)
    }

    // =============================================================================


    fun abrirParaGridView(queGrupo: Short, queDepartam: Short, queTarifa: Short, queTrfCajas: Short,
                          queCliente: Int, queOrdenacion: Short) {

        lArtGridView = if (queCliente > 0)
            articulosDao?.abrirPGVConHco(queTarifa, queTrfCajas, queCliente,
                queGrupo, queDepartam, queOrdenacion) ?: emptyList<DatosGridView>().toMutableList()
        else
            articulosDao?.abrirPGV(queTarifa, queTrfCajas,
                queGrupo, queDepartam, queOrdenacion) ?: emptyList<DatosGridView>().toMutableList()
    }


    // Buscamos una cadena dentro de un grupo y departamento concretos.
    fun abrirBusqEnGrupoParaGridView(queBuscar: String, queGrupo: Short, queDepartam: Short, queTarifa: Short,
                                     queTrfCajas: Short, queCliente: Int, queOrdenacion: Short) {

        val cadenaLike = "'%$queBuscar%'"

        lArtGridView = if (queCliente > 0)
            articulosDao?.abrirBusqEnGrupoPGVConHco(cadenaLike, queTarifa, queTrfCajas, queCliente,
                queGrupo, queDepartam, queOrdenacion) ?: emptyList<DatosGridView>().toMutableList()
        else
            articulosDao?.abrirBusqEnGrupoPGV(cadenaLike, queTarifa, queTrfCajas,
                queGrupo, queDepartam, queOrdenacion) ?: emptyList<DatosGridView>().toMutableList()
    }


    fun abrirBusqParaGridView(queBuscar: String, queTarifa: Short, queTrfCajas: Short, queCliente: Int,
                              queOrdenacion: Short) {

        val cadenaLike = "'%$queBuscar%'"

        lArtGridView = if (queCliente > 0)
            articulosDao?.abrirBusqPGVConHco(cadenaLike, queTarifa, queTrfCajas, queCliente,
                queOrdenacion) ?: emptyList<DatosGridView>().toMutableList()
        else
            articulosDao?.abrirBusqPGV(cadenaLike, queTarifa, queTrfCajas,
                queOrdenacion) ?: emptyList<DatosGridView>().toMutableList()
    }

    // Buscamos una cadena dentro de un catálogo concreto.
    fun abrirBusqEnClasifParaGridView(queBuscar: String, queClasificador: Int, queTarifa: Short, queTrfCajas: Short,
                        queCliente: Int,queOrdenacion: Short) {
        val cadenaLike = "'%$queBuscar%'"

        lArtGridView = if (queCliente > 0)
            articulosDao?.abrirBusqEnClasifPGVConHco(cadenaLike, queTarifa, queTrfCajas, queCliente,
                queClasificador, queOrdenacion) ?: emptyList<DatosGridView>().toMutableList()
        else
            articulosDao?.abrirBusqEnClasifPGV(cadenaLike, queTarifa, queTrfCajas,
                queClasificador, queOrdenacion) ?: emptyList<DatosGridView>().toMutableList()
    }


    fun abrirClasifParaGrView(queClasificador: Int, queTarifa: Short, queTrfCajas: Short, queCliente: Int,
                              queOrdenacion: Short) {

        lArtGridView = if (queCliente > 0)
            articulosDao?.abrirClasifPGVConHco(queTarifa, queTrfCajas, queCliente,
                queClasificador, queOrdenacion) ?: emptyList<DatosGridView>().toMutableList()
        else
            articulosDao?.abrirClasifPGV(queTarifa, queTrfCajas,
                queClasificador, queOrdenacion) ?: emptyList<DatosGridView>().toMutableList()
    }


    fun abrirHistoricoParaGrView(queCliente: Int, queOrdenacion: Short, queTarifa: Short) {
        // Pondremos a cero el campo idHco porque cuando estemos vendiendo en modo histórico no queremos que aparezca el icono
        // indicando que el artículo tiene histórico. Si ya estamos vendiendo desde el histórico no nos hace falta ver
        // dicho icono, sería muy redundante, porque aparecería en todos los artículos.

        lArtGridView = articulosDao?.abrirHistoricoPGV(queTarifa, queCliente, queOrdenacion)
                            ?: emptyList<DatosGridView>().toMutableList()
    }


    // Buscamos una cadena dentro del histórico de un cliente concreto.
    fun abrirBusqEnHcoParaGridView(queBuscar: String, queCliente: Int, queOrdenacion: Short, queTarifa: Short) {
        val cadenaLike = "'%$queBuscar%'"

        lArtGridView = articulosDao?.abrirBusqEnHcoPGV(cadenaLike, queTarifa, queCliente,
                queOrdenacion) ?: emptyList<DatosGridView>().toMutableList()
    }


    fun abrirSoloOftasParaGrView(queTarifa: Short, queCliente: Int, queOrdenacion: Short) {
        // Aunque mostremos los precios en oferta, tomamos también los precios normales para mostrarlos tachados.

        lArtGridView = if (queCliente > 0)
            articulosDao?.abrirSoloOftasPGVConHco(queTarifa, queCliente, queOrdenacion)
                            ?: emptyList<DatosGridView>().toMutableList()
        else
            articulosDao?.abrirSoloOftasPGV(queTarifa, queOrdenacion) ?: emptyList<DatosGridView>().toMutableList()
    }



    fun datosAdicionales(queArticulo: Int): Boolean {
        val lDatAdic = articDatAdicDao?.getDatosArticulo(queArticulo) ?: emptyList<String>().toMutableList()
        return lDatAdic.isNotEmpty()
    }


    fun docAsociado(): String {
        return cDatAdicionales?.getString(cDatAdicionales?.getColumnIndex("cadena") ?: 0) ?: ""
    }


    fun articuloEnTablet(queArticulo: Int): Boolean {
        // TODO
        /*
        cursor = dbAlba.rawQuery("SELECT articulo FROM articulos WHERE articulo = $queArticulo", null)
        return try {
            cursor.moveToFirst()
        } finally {
            cursor.close()
        }
        */
        return true
    }


    fun existeCodigo(QueCodigo: String): Boolean {
        // TODO
        /*
        cursor = dbAlba.rawQuery("SELECT articulo, tipo, tcaja, ucaja FROM busquedas WHERE clave = '$QueCodigo'", null)
        return if (cursor.moveToFirst()) {
            val QueArticulo = cursor.getInt(0)
            val tipoCodigo = cursor.getInt(1)
            // Si el código es de cajas, asignamos las unidades por caja. Si no, las
            // asignaremos en existeArticulo().
            if (tipoCodigo == tipoBusq_Caja.toInt()) {
                fCodBCajas = true
                fUCaja = cursor.getDouble(3)
                fTarifaCajas = cursor.getString(2)[0] == 'T'
            } else {
                fCodBCajas = false
                fTarifaCajas = false
            }
            cursor.close()
            existeArticulo(QueArticulo)
        } else {
            cursor.close()
            false
        }
        */
        return true
    }


    /*
    fun getCodAlternativo(): String {
        if (cursor.getString(cursor.getColumnIndex("codalternativo")) != null)
            return cursor.getString(cursor.getColumnIndex("codalternativo"))
        else
            return ""
    }
    */



    fun getCosto(): Double {
        val costosDao: CostosArticulosDao? = MyDatabase.getInstance(contexto)?.costosArticulosDao()
        return costosDao?.getCostoArticulo(fArticulo, fEmpresa) ?: 0.0
    }


    /*
    fun getPrOfta(): String {
        return ofertasDao?.getPrOferta(fArticulo, fEmpresa) ?: "0.0"
    }

    fun getDtoOfta(): String {
        return ofertasDao?.getDtoOferta(fArticulo, fEmpresa) ?: "0.0"
    }
    */

    fun tieneOferta(queArticulo: Int): Boolean {
        val queArticuloId = ofertasDao?.articuloEnOfta(queArticulo, fEmpresa) ?: 0
        return (queArticuloId > 0)
    }

    /*
    fun getPeso(): Double {
        val columna = cursor.getColumnIndex("peso")
        var sPeso = cursor.getString(columna)
        return if (sPeso != null) {
            sPeso = sPeso.replace(',', '.')
            java.lang.Double.valueOf(sPeso)
        } else 0.0
    }
    */

    /*
    fun getTieneHco(): Boolean {
        val idHco = cursor.getInt(cursor.getColumnIndex("idHco"))
        return idHco > 0
    }
    */

    /*
    fun getCantHco(): Double {
        val sCant = cursor.getString(cursor.getColumnIndex("cantHco"))
        return if (sCant != null) {
            java.lang.Double.valueOf(sCant.replace(',', '.'))
        } else 0.0
    }
    */

    /*
    fun getCajasHco(): Double {
        val sCajas = cursor.getString(cursor.getColumnIndex("cajasHco"))
        return if (sCajas != null) {
            java.lang.Double.valueOf(sCajas.replace(',', '.'))
        } else 0.0
    }
    */

    /*
    fun getFechaHco(): String {
        val sFecha = cursor.getString(cursor.getColumnIndex("fecha"))
        return sFecha ?: ""
    }
    */


    fun getExistencias(): Double {
        return entradas - salidas
    }


    fun getCajas(): Double {
        // Devolveremos el stock de cajas calculado
        val dStock = getExistencias()
        return if (fUCaja != 0.0) dStock / fUCaja else 0.0
    }


    fun getImagen(): String {
        return "ART_$fArticulo.jpg"
    }

    fun usarPiezas(): Boolean {
        return (fFlag2 and FLAGARTICULO_USARPIEZAS) > 0
    }

    fun venderPorDosis(): Boolean {
        return (fFlag1 and FLAGARTICULO_VENDER_POR_DOSIS) > 0
    }

    fun controlaTrazabilidad(): Boolean {
        return (fFlag2 and FLAGARTICULO_CONTROLA_TRAZABILIDAD) > 0
    }

    fun usarFormatos(): Boolean {
        return (fFlag1 and FLAGARTICULO_USARFORMATOS) > 0
    }

    fun aplicarTrfCajas(): Boolean {
        return (fFlag1 and FLAGARTICULO_APLICARTRFCAJAS) > 0
    }

    /*
    fun getPrecio(): String {
        if (cursor.getString(cursor.getColumnIndex("precio")) != null)
            return cursor.getString(cursor.getColumnIndex("precio"))
        else
            return "0.0"
    }
    */

    /*
    fun getPrecioHco(): String {
        return cursor.getString(cursor.getColumnIndex("precioHco"))
    }
    */

    /*
    fun getDto(): String {
        if (cursor.getString(cursor.getColumnIndex("dto")) != null)
            return cursor.getString(cursor.getColumnIndex("dto"))
        else
            return "0.0"
    }
    */

    /*
    fun getDtoHco(): String {
        return cursor.getString(cursor.getColumnIndex("dtoHco"))
    }
    */

    /*
    fun getPrCajas(): String {
        if (cursor.getString(cursor.getColumnIndex("prCajas")) != null)
            return cursor.getString(cursor.getColumnIndex("prCajas"))
        else
            return "0.0"
    }
    */

    /*
    fun getDtoCajas(): String {
        if (cursor.getString(cursor.getColumnIndex("dtCajas")) != null)
            return cursor.getString(cursor.getColumnIndex("dtCajas"))
        else
            return "0.0"
    }
    */

    fun tieneEnlace(): Boolean {
        return fEnlace > 0
    }


    fun codArtEnlazado(): String {
        return articulosDao?.getCodigo(fEnlace) ?: ""
    }


    fun formatosALista(): MutableList<String> {

        val lFormatos = formatosDao?.formatosALista(fArticulo) ?: emptyList<FormatosEnt>().toMutableList()
        val listItems: MutableList<String> = emptyList<String>().toMutableList()

        for (formatoEnt in lFormatos) {
            listItems.add(ponerCeros(formatoEnt.formatoId.toString(), ancho_formato) + " " + formatoEnt.descripcion)
        }
        return listItems

        // TODO
        /*
        dbAlba.rawQuery("SELECT DISTINCT A.codigo, A.descr FROM formatos A" +
                    " JOIN trfformatos B ON B.formato = A.codigo AND B.articulo = " + getArticulo() +
                    " ORDER BY A.codigo", null
        ).use { cFormatos ->
            cFormatos.moveToFirst()
            while (!cFormatos.isAfterLast) {
                listItems.add(
                    ponerCeros(cFormatos.getString(0), ancho_formato) + "  " + cFormatos.getString(1)
                )
                cFormatos.moveToNext()
            }
        }
        */
    }


    fun cargarHcoArtClte(queArticulo: Int, queCliente: Int, listItems: MutableList<String>) {
        // TODO
        /*
        dbAlba.rawQuery(
            "SELECT cajas, cantidad, precio, dto, fecha FROM historico" +
                    " WHERE articulo = " + queArticulo + " AND cliente = " + queCliente, null
        ).use { cHco ->
            if (cHco.moveToFirst()) {
                listItems.add(cHco.getString(0))
                listItems.add(cHco.getString(1))
                listItems.add(cHco.getString(2))
                listItems.add(cHco.getString(3))
                listItems.add(cHco.getString(4))
            }
        }
        */
    }


    fun artEnHistorico(queCliente: Int, queArticulo: Int): Boolean {
        val artEnHco = historicoDao?.artEnHistorico(queCliente, queArticulo) ?: 0
        return (artEnHco > 0)
    }



    fun artEnFtosLineas(queArticulo: Int): Boolean {
        val ftoLineaId = ftosLineasDao?.artEnFtosLineas(queArticulo) ?: 0
        return (ftoLineaId > 0)
    }



    fun actualizarStock(queArticulo: Int, queEmpresa: Short, dCantidad: Double, dCajas: Double, deEntradas: Boolean) {
        var bInsertar = false

        // Vemos si el artículo está en la tabla Stock
        if (deEntradas) {
            var sEntradas: String
            var sEntCajas: String
            var dEntradas: Double
            var dEntCajas: Double

            val sQuery = "SELECT ent entradas, entc entcajas FROM stock" +
                    " WHERE articulo = $queArticulo AND empresa = $queEmpresa"

            // TODO
            /*
            dbAlba.rawQuery(sQuery, null).use { cStock ->
                if (cStock.moveToFirst()) {
                    sEntradas = if (cStock.getString(cStock.getColumnIndex("entradas")) != null) {
                        cStock.getString(cStock.getColumnIndex("entradas")).replace(',', '.')
                    } else {
                        "0.0"
                    }
                    sEntCajas = if (cStock.getString(cStock.getColumnIndex("entcajas")) != null) {
                        cStock.getString(cStock.getColumnIndex("entcajas")).replace(',', '.')
                    } else {
                        "0.0"
                    }

                    dEntradas = sEntradas.toDouble() + dCantidad
                    dEntCajas = sEntCajas.toDouble() + dCajas
                } else {
                    bInsertar = true
                    dEntradas = dCantidad
                    dEntCajas = dCajas
                }
                val values = ContentValues()
                if (bInsertar) {
                    values.put("articulo", queArticulo)
                    values.put("empresa", queEmpresa)
                    values.put("sal", 0.0)
                    values.put("salc", 0.0)
                    values.put("salp", 0.0)
                }
                values.put("ent", dEntradas)
                values.put("entc", dEntCajas)
                values.put("entp", 0.0)

                if (bInsertar) dbAlba.insert("stock", null, values)
                else dbAlba.update("stock", values, "articulo=$queArticulo and empresa=$queEmpresa", null)
            }
            */
        } else {
            var sSalidas: String
            var sSalCajas: String
            var dSalidas: Double
            var dSalCajas: Double

            val sQuery = "SELECT sal salidas, salc salcajas FROM stock" +
                    " WHERE articulo = $queArticulo AND empresa = $queEmpresa"

            // TODO
            /*
            dbAlba.rawQuery(sQuery, null).use { cStock ->
                if (cStock.moveToFirst()) {
                    sSalidas = if (cStock.getString(cStock.getColumnIndex("salidas")) != null) {
                        cStock.getString(cStock.getColumnIndex("salidas")).replace(',', '.')
                    } else {
                        "0.0"
                    }
                    sSalCajas = if (cStock.getString(cStock.getColumnIndex("salcajas")) != null) {
                        cStock.getString(cStock.getColumnIndex("salcajas")).replace(',', '.')
                    } else {
                        "0.0"
                    }

                    dSalidas = sSalidas.toDouble() + dCantidad
                    dSalCajas = sSalCajas.toDouble() + dCajas
                } else {
                    bInsertar = true
                    dSalidas = dCantidad
                    dSalCajas = dCajas
                }
                val values = ContentValues()
                if (bInsertar) {
                    values.put("articulo", queArticulo)
                    values.put("empresa", queEmpresa)
                    values.put("ent", 0.0)
                    values.put("entc", 0.0)
                    values.put("entp", 0.0)
                }
                values.put("sal", dSalidas)
                values.put("salc", dSalCajas)
                values.put("salp", 0.0)

                if (bInsertar) dbAlba.insert("stock", null, values)
                else dbAlba.update("stock", values, "articulo=$queArticulo and empresa=$queEmpresa", null)
            }
            */
        }
    }

    /*
    fun getUCajaAsString(): String {
        val sUCaja = cursor.getString(cursor.getColumnIndex("ucaja") )
        return sUCaja ?: ""
    }
    */

}