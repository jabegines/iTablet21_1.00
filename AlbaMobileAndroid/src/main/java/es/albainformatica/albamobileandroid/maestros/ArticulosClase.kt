package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.database.Cursor
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.*
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.ArticulosEnt
import es.albainformatica.albamobileandroid.entity.FormatosEnt
import es.albainformatica.albamobileandroid.entity.IvasEnt


class ArticulosClase(val contexto: Context) {
    private val articulosDao: ArticulosDao? = MyDatabase.getInstance(contexto)?.articulosDao()
    private val ofertasDao: OfertasDao? = MyDatabase.getInstance(contexto)?.ofertasDao()
    private val articDatAdicDao: ArticDatAdicDao? = MyDatabase.getInstance(contexto)?.articDatAdicDao()
    private var formatosDao: FormatosDao? = MyDatabase.getInstance(contexto)?.formatosDao()
    private var ivasDao: IvasDao? = MyDatabase.getInstance(contexto)?.ivasDao()
    private var fLotes: LotesClase = LotesClase(contexto)

    lateinit var cursor: Cursor
    lateinit var cTarifas: Cursor
    var cDatAdicionales: Cursor? = null

    var fArticulo = 0
    var fCodigo: String = ""
    var fDescripcion: String = ""
    var fEmpresa = 0
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


    fun close() {
        cDatAdicionales?.close()
        if (this::cTarifas.isInitialized)
            cTarifas.close()
        if (this::cursor.isInitialized)
            cursor.close()
    }


    fun abrirUnArticulo(queArticulo: Int, queEmpresa: Int): Boolean {
        // TODO
        /*
        cursor = dbAlba.rawQuery(
            "SELECT A.*, B.clave, D.iva porciva, E.ent, E.sal, E.entc, E.salc"
                    + " FROM articulos A"
                    + " LEFT JOIN busquedas B ON B.articulo = A.articulo AND B.tipo = 2"
                    + " LEFT JOIN ivas D ON D.tipo = A.tipoiva"
                    + " LEFT JOIN stock E ON E.articulo = A.articulo AND E.empresa = " + queEmpresa
                    //+ " LEFT JOIN ofertas F ON F.articulo = A.articulo"
                    + " WHERE A.articulo = " + queArticulo, null
        )

        // Tenemos que hacer moveToFirst, ya que la posición inicial de los cursores es -1.
        if (cursor.moveToFirst()) {
            fEmpresa = queEmpresa
            abrirTarifas(queArticulo)
            cTarifas.moveToFirst()

            fCodProv = cursor.getString(cursor.getColumnIndex("prov"))

            return true
        } else return false
        */
        return true
    }


    private fun abrirTarifas(queArticulo: Int) {
        // Si el artículo tiene formatos el cursor cTarifas saldrá de la tabla "trfformatos".
        // TODO
        /*
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
        */
    }


    fun abrir(): Boolean {
        // TODO
        //cursor = dbAlba.rawQuery("SELECT * FROM articulos", null)
        //return cursor.moveToFirst()
        return true
    }


    // Catálogos Bionat
    fun abrirBioCatalogo(queCatalogo: Int, fOrdenacion: Int): Boolean {
        // TODO
        /*
        var consulta = "SELECT A.articulo FROM articclasif A" +
                " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                " WHERE A.clasificador = " + queCatalogo
        if (fOrdenacion == 1) consulta += " ORDER BY A.orden" else if (fOrdenacion == 2) consulta += " ORDER BY B.codigo" else if (fOrdenacion == 3) consulta += " ORDER BY B.descr"
        cursor = dbAlba.rawQuery(consulta, null)
        return cursor.moveToFirst()
        */
        return true
    }


    fun abrirBioDepartamento(queGrupo: Int, queDepartamento: Int, fOrdenacion: Int): Boolean {
        var consulta = "SELECT articulo FROM articulos WHERE grupo = $queGrupo AND dpto = $queDepartamento"
        if (fOrdenacion == 2) consulta += " ORDER BY codigo" else if (fOrdenacion == 3) consulta += " ORDER BY descr"
        // TODO
        //cursor = dbAlba.rawQuery(consulta, null)
        //return cursor.moveToFirst()
        return true
    }


    fun abrirBioHistorico(queCliente: Int, fOrdenacion: Int): Boolean {
        var consulta = "SELECT A.articulo FROM historico A" +
                " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                " WHERE A.cliente = " + queCliente
        if (fOrdenacion == 2) consulta += " ORDER BY B.codigo" else if (fOrdenacion == 3) consulta += " ORDER BY B.descr"
        // TODO
        //cursor = dbAlba.rawQuery(consulta, null)
        //return cursor.moveToFirst()
        return true
    }


    fun bioBuscar(queBuscar: String): Boolean {
        val cadenaLike = "LIKE('%$queBuscar%')"
        var consulta = ("SELECT A.articulo FROM articulos A "
                + " LEFT JOIN busquedas B ON B.articulo = A.articulo AND B.tipo = 6")

        // Buscaremos tanto por descripción como por código
        consulta = "$consulta WHERE A.descr $cadenaLike OR A.codigo $cadenaLike OR B.clave $cadenaLike"
        // TODO
        //cursor = dbAlba.rawQuery(consulta, null)
        //return cursor.moveToFirst()
        return true
    }


    fun abrirParaGridView(
        queGrupo: Int,
        queDepartam: Int,
        queTarifa: Short,
        queTrfCajas: Short,
        queCliente: Int,
        queOrdenacion: Short
    ) {
        var consulta =
            //"SELECT DISTINCT A.articulo, A.codigo, A.descr, A.ucaja, B.articulo artofert," +
            "SELECT DISTINCT A.articulo, A.codigo, A.descr, A.ucaja," +
                    " C.precio, C.dto, D.precio prCajas, D.dto dtCajas, E.iva porciva, F.ent, F.sal, F.entc, F.salc, "
        consulta = if (queCliente > 0) consulta + "H._id idHco" else consulta + "0 idHco"
        consulta = consulta + " FROM articulos A" +
                //" LEFT JOIN ofertas B ON B.articulo = A.articulo" +
                " LEFT OUTER JOIN tarifas C ON C.articulo = A.articulo AND C.tarifa = " + queTarifa +
                " LEFT OUTER JOIN tarifas D ON D.articulo = A.articulo AND D.tarifa = " + queTrfCajas +
                " LEFT OUTER JOIN ivas E ON E.tipo = A.tipoiva" +
                " LEFT OUTER JOIN stock F ON F.articulo = A.articulo"
        if (queCliente > 0) consulta =
            "$consulta LEFT OUTER JOIN historico H ON H.articulo = A.articulo AND H.cliente = $queCliente"
        consulta = "$consulta WHERE A.grupo = $queGrupo AND A.dpto = $queDepartam"
        consulta =
            if (queOrdenacion.toInt() == 0) "$consulta ORDER BY A.descr" else "$consulta ORDER BY A.codigo"
        // TODO
        //cursor = dbAlba.rawQuery(consulta, null)
        //cursor.moveToFirst()
    }


    // Buscamos una cadena dentro de un grupo y departamento concretos.
    fun abrirBusqEnGrupoParaGridView(
        queBuscar: String,
        queGrupo: Int,
        queDepartam: Int,
        queTarifa: Short,
        queTrfCajas: Short,
        queCliente: Int,
        queOrdenacion: Short
    ) {
        val cadenaLike = "LIKE('%$queBuscar%')"
        var consulta =
            "SELECT DISTINCT A.articulo, A.codigo, A.descr, A.ucaja," +
                    " C.precio, C.dto, D.precio prCajas, D.dto dtCajas, E.iva porciva, F.ent, F.sal, F.entc, F.salc, "
        consulta = if (queCliente > 0) consulta + "H._id idHco" else consulta + "0 idHco"
        consulta = consulta + " FROM articulos A" +
                //" LEFT JOIN ofertas B ON B.articulo = A.articulo" +
                " LEFT OUTER JOIN tarifas C ON C.articulo = A.articulo AND C.tarifa = " + queTarifa +
                " LEFT OUTER JOIN tarifas D ON D.articulo = A.articulo AND D.tarifa = " + queTrfCajas +
                " LEFT OUTER JOIN ivas E ON E.tipo = A.tipoiva" +
                " LEFT OUTER JOIN stock F ON F.articulo = A.articulo"
        if (queCliente > 0) consulta =
            "$consulta LEFT OUTER JOIN historico H ON H.articulo = A.articulo AND H.cliente = $queCliente"
        consulta =
            "$consulta WHERE (A.descr $cadenaLike OR A.codigo $cadenaLike) AND A.grupo = $queGrupo AND A.dpto = $queDepartam"
        consulta =
            if (queOrdenacion.toInt() == 0) "$consulta ORDER BY A.descr" else "$consulta ORDER BY A.codigo"
        // TODO
        //cursor = dbAlba.rawQuery(consulta, null)
        //cursor.moveToFirst()
    }

    fun abrirBusqParaGridView(
        queBuscar: String,
        queTarifa: Short,
        queTrfCajas: Short,
        queCliente: Int,
        queOrdenacion: Short
    ) {
        val cadenaLike = "LIKE('%$queBuscar%')"
        var consulta =
            "SELECT DISTINCT A.articulo, A.codigo, A.descr, A.ucaja," +
                    " C.precio, C.dto, D.precio prCajas, D.dto dtCajas, E.iva porciva, F.ent, F.sal, F.entc, F.salc, "
        consulta = if (queCliente > 0) consulta + "H._id idHco" else consulta + "0 idHco"
        consulta = consulta + " FROM articulos A" +
                //" LEFT JOIN ofertas B ON B.articulo = A.articulo" +
                " LEFT OUTER JOIN tarifas C ON C.articulo = A.articulo AND C.tarifa = " + queTarifa +
                " LEFT OUTER JOIN tarifas D ON D.articulo = A.articulo AND D.tarifa = " + queTrfCajas +
                " LEFT OUTER JOIN ivas E ON E.tipo = A.tipoiva" +
                " LEFT OUTER JOIN stock F ON F.articulo = A.articulo"
        if (queCliente > 0) consulta =
            "$consulta LEFT OUTER JOIN historico H ON H.articulo = A.articulo AND H.cliente = $queCliente"

        // Buscaremos tanto por descripción como por código
        consulta = "$consulta WHERE A.descr $cadenaLike OR A.codigo $cadenaLike"
        consulta =
            if (queOrdenacion.toInt() == 0) "$consulta ORDER BY A.descr" else "$consulta ORDER BY A.codigo"
        // TODO
        //cursor = dbAlba.rawQuery(consulta, null)
        //cursor.moveToFirst()
    }

    // Buscamos una cadena dentro de un catálogo concreto.
    fun abrirBusqEnClasifParaGridView(
        queBuscar: String,
        queClasificador: Int,
        queTarifa: Short,
        queTrfCajas: Short,
        queCliente: Int,
        queOrdenacion: Short
    ) {
        val cadenaLike = "LIKE('%$queBuscar%')"
        var consulta =
            "SELECT DISTINCT A.articulo, B.codigo, B.descr, B.ucaja," +
                    " D.precio, D.dto, E.precio prCajas, E.dto dtCajas, G.iva porciva, F.ent, F.sal, F.entc, F.salc, "
        consulta = if (queCliente > 0) consulta + "H._id idHco" else consulta + "0 idHco"
        consulta = consulta + " FROM articclasif A" +
                " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                //" LEFT JOIN ofertas C ON C.articulo = A.articulo" +
                " LEFT OUTER JOIN tarifas D ON D.articulo = A.articulo AND D.tarifa = " + queTarifa +
                " LEFT OUTER JOIN tarifas E ON E.articulo = A.articulo AND E.tarifa = " + queTrfCajas +
                " LEFT OUTER JOIN stock F ON F.articulo = A.articulo" +
                " LEFT OUTER JOIN ivas G ON G.tipo = B.tipoiva"
        if (queCliente > 0) consulta =
            "$consulta LEFT OUTER JOIN historico H ON H.articulo = A.articulo AND H.cliente = $queCliente"
        consulta =
            "$consulta WHERE A.clasificador = $queClasificador AND (B.descr $cadenaLike OR B.codigo $cadenaLike)"
        consulta =
            if (queOrdenacion.toInt() == 0) "$consulta ORDER BY B.descr" else "$consulta ORDER BY B.codigo"
        // TODO
        //cursor = dbAlba.rawQuery(consulta, null)
        //cursor.moveToFirst()
    }


    fun abrirClasifParaGrView(
        queClasificador: Int,
        queTarifa: Short,
        queTrfCajas: Short,
        queCliente: Int,
        queOrdenacion: Short
    ) {
        var consulta = "SELECT DISTINCT A.articulo, B.codigo, B.descr, B.ucaja," +
                    " D.precio, D.dto, E.precio prCajas, E.dto dtCajas, G.iva porciva, F.ent, F.sal, F.entc, F.salc, "
        consulta = if (queCliente > 0) consulta + "H._id idHco" else consulta + "0 idHco"
        consulta = consulta + " FROM articclasif A" +
                " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                //" LEFT JOIN ofertas C ON C.articulo = A.articulo" +
                " LEFT OUTER JOIN tarifas D ON D.articulo = A.articulo AND D.tarifa = " + queTarifa +
                " LEFT OUTER JOIN tarifas E ON E.articulo = A.articulo AND E.tarifa = " + queTrfCajas +
                " LEFT OUTER JOIN stock F ON F.articulo = A.articulo" +
                " LEFT OUTER JOIN ivas G ON G.tipo = B.tipoiva"
        if (queCliente > 0) consulta =
            "$consulta LEFT OUTER JOIN historico H ON H.articulo = A.articulo AND H.cliente = $queCliente"

        // La última condición (AND B.articulo IS NOT NULL) la añado porque he comprobado que desde gestión pueden venir
        // registros en la tabla articclasif apuntando a artículos que no existen.
        consulta = "$consulta WHERE A.clasificador = $queClasificador AND B.articulo IS NOT NULL"
        consulta =
            if (queOrdenacion.toInt() == 0) "$consulta ORDER BY B.descr" else "$consulta ORDER BY B.codigo"
        // TODO
        //cursor = dbAlba.rawQuery(consulta, null)
        //cursor.moveToFirst()
    }

    fun abrirHistorico(queCliente: Int, queOrdenacion: Short, queTarifa: Short) {
        // Pondremos a cero el campo idHco porque cuando estemos vendiendo en modo histórico no queremos que aparezca el icono
        // indicando que el artículo tiene histórico. Si ya estamos vendiendo desde el histórico no nos hace falta ver
        // dicho icono, sería muy redundante, porque aparecería en todos los artículos.
        var consulta =
            "SELECT DISTINCT A.articulo, B.codigo, B.descr, B.ucaja," +
                    " A.cantidad cantHco, A.cajas cajasHco, A.precio precioHco, A.dto dtoHco, F.precio, F.dto, A.fecha, 0 prCajas, 0 dtCajas, D.iva porciva, " +
                    " E.ent, E.sal, E.entc, E.salc, 0 idHco" +
                    " FROM historico A" +
                    " LEFT OUTER JOIN articulos B ON B.articulo = A.articulo" +
                    //" LEFT JOIN ofertas C ON C.articulo = A.articulo" +
                    " LEFT OUTER JOIN ivas D ON D.tipo = B.tipoiva" +
                    " LEFT OUTER JOIN stock E ON E.articulo = A.articulo" +
                    " LEFT OUTER JOIN tarifas F ON F.articulo = A.articulo AND F.tarifa = " + queTarifa +
                    " WHERE A.cliente = " + queCliente
        consulta =
            if (queOrdenacion.toInt() == 0) "$consulta ORDER BY B.descr" else "$consulta ORDER BY B.codigo"
        // TODO
        //cursor = dbAlba.rawQuery(consulta, null)
        //cursor.moveToFirst()
    }


    // Buscamos una cadena dentro del histórico de un cliente concreto.
    fun abrirBusqEnHcoParaGridView(
        queBuscar: String,
        queCliente: Int,
        queOrdenacion: Short,
        queTarifa: Short
    ) {
        val cadenaLike = "LIKE('%$queBuscar%')"
        var consulta =
            "SELECT DISTINCT A.articulo, B.codigo, B.descr, B.ucaja," +
                    " A.cantidad cantHco, A.cajas cajasHco, A.precio precioHco, A.dto dtoHco, F.precio, F.dto, A.fecha, 0 prCajas, 0 dtCajas, D.iva porciva, " +
                    " E.ent, E.sal, E.entc, E.salc, 0 idHco" +
                    " FROM historico A" +
                    " LEFT OUTER JOIN articulos B ON B.articulo = A.articulo" +
                    //" LEFT JOIN ofertas C ON C.articulo = A.articulo" +
                    " LEFT OUTER JOIN ivas D ON D.tipo = B.tipoiva" +
                    " LEFT OUTER JOIN stock E ON E.articulo = A.articulo" +
                    " LEFT OUTER JOIN tarifas F ON F.articulo = A.articulo AND F.tarifa = " + queTarifa +
                    " WHERE A.cliente = " + queCliente + " AND (B.descr " + cadenaLike + " OR B.codigo " + cadenaLike + ")"
        consulta =
            if (queOrdenacion.toInt() == 0) "$consulta ORDER BY B.descr" else "$consulta ORDER BY B.codigo"
        // TODO
        //cursor = dbAlba.rawQuery(consulta, null)
        //cursor.moveToFirst()
    }


    fun abrirSoloOftas(queTarifa: Short, queCliente: Int, queOrdenacion: Short) {
        // Aunque mostremos los precios en oferta, tomamos también los precios normales para mostrarlos tachados.
        var consulta =
            "SELECT DISTINCT A.articulo, A.codigo, A.descr, A.ucaja," +
                    " C.precio, C.dto, 0 prCajas, 0 dtCajas, D.iva porciva, E.ent, E.sal, E.entc, E.salc, "
        consulta = if (queCliente > 0) consulta + "H._id idHco" else consulta + "0 idHco"
        //consulta = consulta + " FROM articulos A, Ofertas B" +
        consulta = consulta + " FROM articulos A" +
                " LEFT OUTER JOIN tarifas C ON C.articulo = A.articulo AND C.tarifa = " + queTarifa +
                " LEFT OUTER JOIN ivas D ON D.tipo = A.tipoiva" +
                " LEFT OUTER JOIN stock E ON E.articulo = A.articulo"
        if (queCliente > 0) consulta =
            "$consulta LEFT OUTER JOIN historico H ON H.articulo = A.articulo AND H.cliente = $queCliente"
        //consulta = "$consulta WHERE B.articulo = A.articulo"
        consulta =
            if (queOrdenacion.toInt() == 0) "$consulta ORDER BY A.descr" else "$consulta ORDER BY A.codigo"
        // TODO
        //cursor = dbAlba.rawQuery(consulta, null)
        //cursor.moveToFirst()
    }

    fun abrirParaFinDeDia(): Boolean {
        // TODO
        //cursor = dbAlba.rawQuery("SELECT * FROM stock ORDER BY empresa", null)
        // Tenemos que hacer moveToFirst, ya que la posición inicial de los cursores es -1.
        //return cursor.moveToFirst()
        return true
    }


    fun abrirLotesFinDia(): Boolean {
        cursor = fLotes.getAllLotes()!!
        return if (cursor != null) cursor.moveToFirst() else false
    }


    fun datosAdicionales(queArticulo: Int): Boolean {
        val lDatAdic = articDatAdicDao?.getDatosArticulo(queArticulo) ?: emptyList<String>().toMutableList()
        return lDatAdic.isNotEmpty()
    }


    fun docAsociado(): String {
        return cDatAdicionales?.getString(cDatAdicionales?.getColumnIndex("cadena") ?: 0) ?: ""
    }

    fun existeArticulo(queArticulo: Int): Boolean {

        val articuloEnt = articulosDao?.existeArticulo(queArticulo) ?: ArticulosEnt()

        return if (articuloEnt.articuloId > 0) {
            fArticulo = queArticulo
            fCodigo = articuloEnt.codigo
            fDescripcion = articuloEnt.descripcion

            val ivasEnt = ivasDao?.getDatosIva(articuloEnt.tipoIva) ?: IvasEnt()
            fCodIva = ivasEnt.codigo
            if (ivasEnt.porcIva != "") fPorcIva = ivasEnt.porcIva.replace(',', '.').toDouble()
            else fPorcIva = 0.0

            fTasa1 = if (articuloEnt.tasa1 != "") articuloEnt.tasa1.replace(',', '.').toDouble()
            else 0.0
            fTasa2 = if (articuloEnt.tasa2 != "") articuloEnt.tasa2.replace(',', '.').toDouble()
            else 0.0

            fGrupo = articuloEnt.grupoId
            fDepartamento = articuloEnt.departamentoId
            fCodProv = articuloEnt.proveedorId
            fFlag1 = articuloEnt.flag1
            fFlag2 = articuloEnt.flag2
            fEnlace = articuloEnt.enlace

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



    fun getCodAlternativo(): String {
        if (cursor.getString(cursor.getColumnIndex("codalternativo")) != null)
            return cursor.getString(cursor.getColumnIndex("codalternativo"))
        else
            return ""
    }


    fun getCBarras(): String {
        val colCBarras = cursor.getColumnIndex("clave")
        return if (cursor.getString(colCBarras) != null)
            cursor.getString(colCBarras)
        else
            ""
    }



    fun getUCaja(): Double {
        var sUCaja = cursor.getString(cursor.getColumnIndex("ucaja"))
        return if (sUCaja != null) {
            sUCaja = sUCaja.replace(',', '.')
            java.lang.Double.valueOf(sUCaja)
        } else 0.0
    }


    fun getCosto(): Double {
        val costosDao: CostosArticulosDao? = MyDatabase.getInstance(contexto)?.costosArticulosDao()
        return costosDao?.getCostoArticulo(fArticulo, fEmpresa) ?: 0.0
    }


    fun getPrOfta(): String {
        return ofertasDao?.getPrOferta(fArticulo, fEmpresa) ?: "0.0"
    }

    fun getDtoOfta(): String {
        return ofertasDao?.getDtoOferta(fArticulo, fEmpresa) ?: "0.0"
    }


    fun tieneOferta(queArticulo: Int): Boolean {
        val queArticuloId = ofertasDao?.articuloEnOfta(queArticulo, fEmpresa) ?: 0
        return (queArticuloId > 0)
    }


    fun getPeso(): Double {
        val columna = cursor.getColumnIndex("peso")
        var sPeso = cursor.getString(columna)
        return if (sPeso != null) {
            sPeso = sPeso.replace(',', '.')
            java.lang.Double.valueOf(sPeso)
        } else 0.0
    }

    fun getTieneHco(): Boolean {
        val idHco = cursor.getInt(cursor.getColumnIndex("idHco"))
        return idHco > 0
    }

    fun getCantHco(): Double {
        val sCant = cursor.getString(cursor.getColumnIndex("cantHco"))
        return if (sCant != null) {
            java.lang.Double.valueOf(sCant.replace(',', '.'))
        } else 0.0
    }

    fun getCajasHco(): Double {
        val sCajas = cursor.getString(cursor.getColumnIndex("cajasHco"))
        return if (sCajas != null) {
            java.lang.Double.valueOf(sCajas.replace(',', '.'))
        } else 0.0
    }

    fun getFechaHco(): String {
        val sFecha = cursor.getString(cursor.getColumnIndex("fecha"))
        return sFecha ?: ""
    }

    fun getExistencias(): Double {
        val dEnt: Double
        val dSal: Double
        val colEnt = cursor.getColumnIndex("ent")
        val sEnt = cursor.getString(colEnt)
        dEnt = sEnt?.replace(',', '.')?.toDouble() ?: 0.0
        val colSal = cursor.getColumnIndex("sal")
        val sSal = cursor.getString(colSal)
        dSal = sSal?.replace(',', '.')?.toDouble() ?: 0.0
        return dEnt - dSal
    }

    fun getCajas(): Double {
        // Devolveremos el stock de cajas calculado
        val dStock = getExistencias()
        val dUnCaja = getUCaja()
        return if (dUnCaja != 0.0) dStock / dUnCaja else 0.0
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

    fun getPrecio(): String {
        if (cursor.getString(cursor.getColumnIndex("precio")) != null)
            return cursor.getString(cursor.getColumnIndex("precio"))
        else
            return "0.0"
    }

    fun getPrecioHco(): String {
        return cursor.getString(cursor.getColumnIndex("precioHco"))
    }

    fun getDto(): String {
        if (cursor.getString(cursor.getColumnIndex("dto")) != null)
            return cursor.getString(cursor.getColumnIndex("dto"))
        else
            return "0.0"
    }

    fun getDtoHco(): String {
        return cursor.getString(cursor.getColumnIndex("dtoHco"))
    }

    fun getPrCajas(): String {
        if (cursor.getString(cursor.getColumnIndex("prCajas")) != null)
            return cursor.getString(cursor.getColumnIndex("prCajas"))
        else
            return "0.0"
    }

    fun getDtoCajas(): String {
        if (cursor.getString(cursor.getColumnIndex("dtCajas")) != null)
            return cursor.getString(cursor.getColumnIndex("dtCajas"))
        else
            return "0.0"
    }


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
        // TODO
        /*
        dbAlba.rawQuery(
            "SELECT articulo FROM historico WHERE articulo = $queArticulo AND cliente = $queCliente",
            null
        ).use { cHco -> return cHco.moveToFirst() }
        */
        return true
    }

    fun artEnFtosLineas(queArticulo: Int): Boolean {
        // TODO
        //dbAlba.rawQuery("SELECT * FROM ftosLineas WHERE articulo = $queArticulo AND borrar <> 'T'", null
        //).use { cFtosLinea -> return cFtosLinea.moveToFirst() }
        return true
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


    fun getUCajaAsString(): String {
        val sUCaja = cursor.getString(cursor.getColumnIndex("ucaja") )
        return sUCaja ?: ""
    }


}