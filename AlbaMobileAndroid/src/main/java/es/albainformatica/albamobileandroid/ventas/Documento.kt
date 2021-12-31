package es.albainformatica.albamobileandroid.ventas

import android.annotation.SuppressLint
import es.albainformatica.albamobileandroid.database.MyDatabase.Companion.getInstance
import android.database.sqlite.SQLiteDatabase
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.maestros.ClientesClase
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.cobros.FormasPagoClase
import es.albainformatica.albamobileandroid.maestros.LotesClase
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.widget.Toast
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.*
import es.albainformatica.albamobileandroid.entity.CatalogoLineasEnt
import es.albainformatica.albamobileandroid.entity.OfertasEnt
import es.albainformatica.albamobileandroid.entity.TiposIncEnt
import java.lang.NumberFormatException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by jabegines on 14/10/13.
 */
class Documento(private val fContexto: Context) : BaseDatos(fContexto) {
    private val ofertasDao: OfertasDao? = getInstance(fContexto)?.ofertasDao()
    private val ofVolRangosDao: OftVolRangosDao? = getInstance(fContexto)?.oftVolRangosDao()
    private val ratingProvDao: RatingProvDao? = getInstance(fContexto)?.ratingProvDao()
    private val dbAlba: SQLiteDatabase = writableDatabase
    private val myBD: MyDatabase? = getInstance(fContexto)
    lateinit var cLineas: Cursor
    lateinit var cDocumentos: Cursor
    private val fDtosCascada: DtosCascada = DtosCascada(fContexto)
    var fClientes: ClientesClase = ClientesClase(fContexto)
    private val fArticulos: ArticulosClase = ArticulosClase(fContexto)
    private val fConfiguracion: Configuracion = Comunicador.fConfiguracion
    private val fPagoClase: FormasPagoClase = FormasPagoClase(fContexto)
    private val fLotes: LotesClase = LotesClase(fContexto)

    var fAlmacen: Short = 0
    var fEjercicio: Short = 0
    var fEmpresa: Short = 0
    var serie: String = ""
    var fSerieExenta = false
    var numero = 0
    private var fDecPrBase = 0
    private var fDecPrII = 0
    private var fDecImpBase = 0
    private var fDecImpII = 0
    var fBases: ListaBasesDoc = ListaBasesDoc(fContexto)
    private var fTotalAnterior: Double = 0.0
    var fIdDoc = 0
    var fPago: String = ""
    var fHayArtHabituales: Boolean = false
    var fTipoDoc: Short = 0
    var fTipoPedido = 0
    var fAplOftEnPed: Boolean = false
    var fCliente = 0
    var fFecha: String = ""
    var fFEntrega: String = ""
    var fHora: String = ""
    var fTarifaDoc: Byte = 0
    private var fTarifaDto: Byte = 0
    var fTarifaLin: Byte = 0
    var fPuedoAplTrfCajas = false
    var fFormatoLin: Byte = 0
    var fPorcIva: Double = 0.0
    var fAplicarIva: Boolean = true
    private var fAplicarRe: Boolean = false
    var fArtEnOferta: Boolean = false
    var fArtSinCargo: Boolean = false
    var fPrecioRating: Boolean = false
    var fHayCambPrecio: Boolean = false
    var fCodIncidencia = 0
    private var fControlarStock = false
    private var fUsarTrazabilidad = false
    var fArticulo = 0
    var fCodArt: String = ""
    var fDescr: String = ""
    var fLineaPorPiezas = false
    var fPrecio: Double = 0.0
    var fPrecioII: Double = 0.0
    var fDtoLin: Double = 0.0
    var fDtoImp: Double = 0.0
    var fDtoImpII: Double = 0.0
    var fDtoRatingImp: Double = 0.0
    var fLineaConDtCasc: Boolean = false
    var fLineaEsEnlace: Boolean = false
    var fPrecioTarifa: Double = 0.0 // Nos servirán para saber si modificamos el precio de tarifa y, en este caso,
    var fDtoLinTarifa: Double = 0.0 // no aplicar ofertas por volumen.

    var fCajas: Double = 0.0
    var fPiezas: Double = 0.0
    private var fOldCajas: Double = 0.0
    var fCantidad: Double = 0.0
    private var fOldCantidad: Double = 0.0
    private var fOldLote: String = ""
    var fCodigoIva: Short = 0
    var fImporte: Double = 0.0
    var fImpteII: Double = 0.0
    var fLote: String = ""
    var fTasa1: Double = 0.0
    var fTasa2: Double = 0.0
    var fTextoLinea: String = ""
    var fFlag5 = 0
    var fAlmacPedido: String = ""
    var fObs1: String = ""
    var fObs2: String = ""
    var fIncidenciaDoc = 0
    var fTextoIncidencia: String = ""
    var fDtoPie1: Double = 0.0
    var fDtoPie2: Double = 0.0
    var fDtoPie3: Double = 0.0
    var fDtoPie4: Double = 0.0
    var fAlmDireccion: String = ""
    var fOrdenDireccion: String = ""


    init {
        inicializarDocumento()
    }


    override fun close() {
        fBases.close()
        fClientes.close()
        fArticulos.close()
        fDtosCascada.close()
        if (dbAlba.isOpen) dbAlba.close()
    }

    fun abrirLineas() {
        cLineas = dbAlba.rawQuery(
            "SELECT A.*, B.iva porciva, C.descr descrfto FROM lineas A"
                    + " LEFT OUTER JOIN ivas B ON B.codigo = A.codigoiva"
                    + " LEFT OUTER JOIN formatos C ON C.codigo = A.formato"
                    + " WHERE A.cabeceraId = " + fIdDoc, null
        )
        cLineas.moveToFirst()
    }

    fun borrarOftVolumen(recBases: Boolean) {
        dbAlba.delete("lineas", "cabeceraId = $fIdDoc AND flag3 = 128", null)
        if (recBases) recalcularBases()
    }

    @SuppressLint("Range")
    fun comprobarLineasHuerfanas() {
        // Buscaremos aquellas líneas que estén sin cabecera
        val cLineasSinCabecera = dbAlba.rawQuery(
            "SELECT A.* FROM lineas A" +
                    " LEFT JOIN cabeceras B ON B._id = A.cabeceraId" +
                    " WHERE B.numero IS NULL", null
        )
        if (cLineasSinCabecera.moveToFirst()) {
            cLineasSinCabecera.moveToFirst()
            while (!cLineasSinCabecera.isAfterLast) {
                val fLinea = cLineasSinCabecera.getInt(cLineasSinCabecera.getColumnIndex("_id"))
                val queArticulo =
                    cLineasSinCabecera.getInt(cLineasSinCabecera.getColumnIndex("articulo"))
                val queCajas = cLineasSinCabecera.getString(cLineasSinCabecera.getColumnIndex("cajas")).toDouble()
                val queCantidad = cLineasSinCabecera.getString(cLineasSinCabecera.getColumnIndex("cantidad")).toDouble()
                var queLote = ""
                if (cLineasSinCabecera.getString(cLineasSinCabecera.getColumnIndex("lote")) != null)
                    queLote = cLineasSinCabecera.getString(cLineasSinCabecera.getColumnIndex("lote"))

                dbAlba.delete("lineas", "_id=$fLinea", null)

                // Borramos las posibles líneas de descuentos en cascada
                dbAlba.delete("desctoslineas", "linea=$fLinea", null)
                // Actualizamos el stock del artículo
                fControlarStock = fConfiguracion.controlarStock()
                fUsarTrazabilidad = fConfiguracion.usarTrazabilidad()
                fTipoDoc =
                    cLineasSinCabecera.getString(cLineasSinCabecera.getColumnIndex("tipodoc"))
                        .toShort()
                if (fControlarStock && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN)) fArticulos.actualizarStock(
                    queArticulo,
                    fEmpresa,
                    -queCantidad,
                    -queCajas,
                    false
                )

                // Actualizamos el stock del lote.
                if (fUsarTrazabilidad && queLote != null && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN)) fLotes.actStockLote(
                    queArticulo,
                    -queCantidad,
                    queLote,
                    fEmpresa
                )
                cLineasSinCabecera.moveToNext()
            }

            // Borro el histórico, por si he estado indicando cantidades en modo catálogo.
            dbAlba.delete("tmphco", "1=1", null)
        }
        cLineasSinCabecera.close()
    }

    private fun actualizarIdCabecera() {
        val values = ContentValues()
        cLineas.moveToFirst()
        while (!cLineas.isAfterLast) {
            val fLinea = cLineas.getInt(cLineas.getColumnIndexOrThrow("_id"))
            values.put("cabeceraId", fIdDoc)
            dbAlba.update("lineas", values, "_id=$fLinea", null)
            cLineas.moveToNext()
        }
    }

    @SuppressLint("Range")
    fun anularDocumento() {
        cLineas.moveToFirst()
        while (!cLineas.isAfterLast) {
            borrarLinea(cLineas.getInt(cLineas.getColumnIndex("_id")), false)
            cLineas.moveToNext()
        }

        // Borro el histórico, por si he estado indicando cantidades en modo catálogo.
        dbAlba.delete("tmphco", "1=1", null)
    }

    fun borrarDocumento(queIdDoc: Int) {
        // Cargamos el documento para poder tener acceso al cursor de las líneas.
        cargarDocumento(queIdDoc, false)
        // Borramos las líneas.
        anularDocumento()
        // Borramos la cabecera.
        dbAlba.delete("cabeceras", "_id=$queIdDoc", null)
    }

    fun borrarModifDocReparto(queIdDoc: Int) {
        // Borramos las líneas.
        anularDocumento()
        // Borramos la cabecera.
        dbAlba.delete("cabeceras", "_id=$queIdDoc", null)
    }

    fun marcarParaEnviar(queIdDoc: Int) {
        val values = ContentValues()
        values.put("estado", "N")
        dbAlba.update("cabeceras", values, "_id=$queIdDoc", null)
    }

    fun abrirTodos(queCliente: Int, queEmpresa: Int, queFiltro: Int) {
        var sCadena: String
        sCadena = ("SELECT A._id, A.tipodoc, A.alm, A.serie, A.numero, A.ejer, A.fecha,"
                + " A.cliente, A.total, A.estado, A.facturado, B.nomfi, B.nomco, A.firmado, A.imprimido," +
                " A.tipoincidencia FROM cabeceras A"
                + " LEFT OUTER JOIN clientes B ON B.cliente = A.cliente")

        // Vemos si los documentos son de un cliente.
        sCadena = if (queCliente > 0) {
            "$sCadena WHERE A.cliente = $queCliente AND A.empresa = $queEmpresa"
        } else {
            "$sCadena WHERE A.empresa = $queEmpresa"
        }

        // Vemos si queremos filtrar por estado
        if (queFiltro > 0) {
            sCadena = "$sCadena AND "
            when (queFiltro) {
                1 -> sCadena = "$sCadena A.estado = 'P'"
                2 -> sCadena = "$sCadena A.estado = 'N' OR A.estado = 'R'"
                3 -> sCadena = "$sCadena A.estado = 'X'"
            }
        }
        sCadena =
            "$sCadena ORDER BY substr(A.fecha, 7)||substr(A.fecha, 4, 2)||substr(A.fecha, 1, 2) DESC"
        cDocumentos = dbAlba.rawQuery(sCadena, null)
        cDocumentos.moveToFirst()
    }

    @SuppressLint("Range")
    fun copiarAAlbaran(queIdDoc: Int): Double {
        val empresasDao = myBD?.empresasDao()
        var queTotal = 0.0
        var haySerieNumero = false
        cDocumentos = dbAlba.rawQuery("SELECT * FROM cabeceras WHERE _id= $queIdDoc", null)
        if (cDocumentos.moveToFirst()) {
            fTipoDoc = TIPODOC_ALBARAN
            // Tomamos la serie configurada para la empresa
            serie = empresasDao?.getSerieEmpresa(cDocumentos.getInt(cDocumentos.getColumnIndex("empresa"))) ?: ""
            haySerieNumero = setSerieNumero(serie)
            if (haySerieNumero) {
                // Tomamos el total del documento para devolverlo.
                queTotal =
                    cDocumentos.getString(cDocumentos.getColumnIndex("total")).replace(',', '.')
                        .toDouble()
                val values = ContentValues()
                values.put("tipodoc", TIPODOC_ALBARAN)
                val queViejoId = cDocumentos.getInt(cDocumentos.getColumnIndex("_id"))
                val queAlmacen = cDocumentos.getString(cDocumentos.getColumnIndex("alm"))
                val queEjercicio = cDocumentos.getString(cDocumentos.getColumnIndex("ejer"))
                val queEmpresa = cDocumentos.getInt(cDocumentos.getColumnIndex("empresa"))
                values.put("alm", queAlmacen)
                values.put("serie", serie)
                values.put("numero", numero)
                values.put("ejer", queEjercicio)
                values.put("empresa", queEmpresa)
                // Mantendremos el nuevo documento en la misma hoja de reparto que el original. También le daremos el mismo orden.
                values.put("hoja", cDocumentos.getInt(cDocumentos.getColumnIndex("hoja")))
                values.put("orden", cDocumentos.getInt(cDocumentos.getColumnIndex("orden")))

                // Obtenemos la fecha y hora actuales, que son las que grabaremos en el nuevo albarán.
                val tim = System.currentTimeMillis()
                val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                fFecha = df.format(tim)
                fFEntrega = fFecha
                val dfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
                fHora = dfHora.format(tim)
                values.put("fecha", fFecha)
                values.put("hora", fHora)
                values.put("cliente", cDocumentos.getString(cDocumentos.getColumnIndex("cliente")))
                values.put("apliva", cDocumentos.getString(cDocumentos.getColumnIndex("apliva")))
                values.put("aplrec", cDocumentos.getString(cDocumentos.getColumnIndex("aplrec")))
                values.put("bruto", cDocumentos.getString(cDocumentos.getColumnIndex("bruto")))
                if (cDocumentos.getString(cDocumentos.getColumnIndex("dto")) != null) values.put(
                    "dto",
                    cDocumentos.getString(cDocumentos.getColumnIndex("dto")).replace(',', '.')
                )
                if (cDocumentos.getString(cDocumentos.getColumnIndex("dto2")) != null) values.put(
                    "dto2",
                    cDocumentos.getString(cDocumentos.getColumnIndex("dto2")).replace(',', '.')
                )
                if (cDocumentos.getString(cDocumentos.getColumnIndex("dto3")) != null) values.put(
                    "dto3",
                    cDocumentos.getString(cDocumentos.getColumnIndex("dto3")).replace(',', '.')
                )
                if (cDocumentos.getString(cDocumentos.getColumnIndex("dto4")) != null) values.put(
                    "dto4",
                    cDocumentos.getString(cDocumentos.getColumnIndex("dto4")).replace(',', '.')
                )
                if (cDocumentos.getString(cDocumentos.getColumnIndex("base")) != null) values.put(
                    "base",
                    cDocumentos.getString(cDocumentos.getColumnIndex("base")).replace(',', '.')
                )
                if (cDocumentos.getString(cDocumentos.getColumnIndex("iva")) != null) values.put(
                    "iva",
                    cDocumentos.getString(cDocumentos.getColumnIndex("iva")).replace(',', '.')
                )
                if (cDocumentos.getString(cDocumentos.getColumnIndex("recargo")) != null) values.put(
                    "recargo",
                    cDocumentos.getString(cDocumentos.getColumnIndex("recargo")).replace(',', '.')
                )
                if (cDocumentos.getString(cDocumentos.getColumnIndex("total")) != null) values.put(
                    "total",
                    cDocumentos.getString(cDocumentos.getColumnIndex("total")).replace(',', '.')
                )
                values.put("direnv", cDocumentos.getString(cDocumentos.getColumnIndex("direnv")))
                // Cuando modificamos un albarán de reparto, en el campo ruta pondremos lo que tengamos en 'hoja'
                values.put(
                    "ruta",
                    cDocumentos.getInt(cDocumentos.getColumnIndex("hoja")).toString()
                )
                values.put("estado", "N")
                values.put("flag", 0)
                values.put("obs1", cDocumentos.getString(cDocumentos.getColumnIndex("obs1")))
                values.put("obs2", cDocumentos.getString(cDocumentos.getColumnIndex("obs2")))
                values.put("facturado", "F")
                fIdDoc = dbAlba.insert("cabeceras", null, values).toInt()
                cDocumentos.close()
                copiarLineasAAlb(queViejoId)
            }
        }
        return if (haySerieNumero) {
            // Una vez copiado, cargamos el albarán.
            cargarDocumento(fIdDoc, false)
            queTotal
        } else -0.00001
    }

    @SuppressLint("Range")
    private fun copiarLineasAAlb(queViejoId: Int) {
        cLineas = dbAlba.rawQuery(
            "SELECT A.*, B.iva porciva, C.descr descrfto FROM lineas A"
                    + " LEFT OUTER JOIN ivas B ON B.codigo = A.codigoiva"
                    + " LEFT OUTER JOIN formatos C ON C.codigo = A.formato"
                    + " WHERE A.cabeceraId = " + queViejoId, null
        )
        val values = ContentValues()
        cLineas.moveToFirst()
        while (!cLineas.isAfterLast) {
            val viejaLinea = cLineas.getInt(cLineas.getColumnIndex("_id"))
            values.put("cabeceraId", fIdDoc)
            values.put("linea", cLineas.getString(cLineas.getColumnIndex("linea")))
            values.put("articulo", cLineas.getInt(cLineas.getColumnIndex("articulo")))
            values.put("codigo", cLineas.getString(cLineas.getColumnIndex("codigo")))
            values.put("descr", cLineas.getString(cLineas.getColumnIndex("descr")))
            values.put("tarifa", cLineas.getString(cLineas.getColumnIndex("tarifa")))
            if (cLineas.getString(cLineas.getColumnIndex("precio")) != null) values.put(
                "precio",
                cLineas.getString(cLineas.getColumnIndex("precio")).replace(',', '.')
            )
            if (cLineas.getString(cLineas.getColumnIndex("precioii")) != null) values.put(
                "precioii",
                cLineas.getString(cLineas.getColumnIndex("precioii")).replace(',', '.')
            )
            values.put("codigoiva", cLineas.getString(cLineas.getColumnIndex("codigoiva")))
            if (cLineas.getString(cLineas.getColumnIndex("cajas")) != null) values.put(
                "cajas",
                cLineas.getString(cLineas.getColumnIndex("cajas")).replace(',', '.')
            )
            if (cLineas.getString(cLineas.getColumnIndex("cantidad")) != null) values.put(
                "cantidad",
                cLineas.getString(cLineas.getColumnIndex("cantidad")).replace(',', '.')
            )
            if (cLineas.getString(cLineas.getColumnIndex("importe")) != null) values.put(
                "importe",
                cLineas.getString(cLineas.getColumnIndex("importe")).replace(',', '.')
            )
            if (cLineas.getString(cLineas.getColumnIndex("importeii")) != null) values.put(
                "importeii",
                cLineas.getString(cLineas.getColumnIndex("importeii")).replace(',', '.')
            )
            if (cLineas.getString(cLineas.getColumnIndex("dto")) != null) values.put(
                "dto",
                cLineas.getString(cLineas.getColumnIndex("dto")).replace(',', '.')
            )
            if (cLineas.getString(cLineas.getColumnIndex("dtoi")) != null) values.put(
                "dtoi",
                cLineas.getString(cLineas.getColumnIndex("dtoi")).replace(',', '.')
            )
            if (cLineas.getString(cLineas.getColumnIndex("dtoiii")) != null) values.put(
                "dtoiii",
                cLineas.getString(cLineas.getColumnIndex("dtoiii")).replace(',', '.')
            )
            if (cLineas.getString(cLineas.getColumnIndex("lote")) != null)
                values.put("lote", cLineas.getString(cLineas.getColumnIndex("lote")))
            else
                values.put("lote", "")

            if (cLineas.getString(cLineas.getColumnIndex("piezas")) != null)
                values.put("piezas", cLineas.getString(cLineas.getColumnIndex("piezas")).replace(',', '.')
            )
            values.put("flag", cLineas.getString(cLineas.getColumnIndex("flag")))
            values.put("flag3", cLineas.getString(cLineas.getColumnIndex("flag3")))
            values.put("flag5", cLineas.getString(cLineas.getColumnIndex("flag5")))
            if (cLineas.getString(cLineas.getColumnIndex("tasa1")) != null) values.put(
                "tasa1",
                cLineas.getString(cLineas.getColumnIndex("tasa1")).replace(',', '.')
            )
            if (cLineas.getString(cLineas.getColumnIndex("tasa2")) != null) values.put(
                "tasa2",
                cLineas.getString(cLineas.getColumnIndex("tasa2")).replace(',', '.')
            )
            values.put("formato", cLineas.getInt(cLineas.getColumnIndex("formato")))
            values.put("incidencia", cLineas.getInt(cLineas.getColumnIndex("incidencia")))
            values.put("textolinea", cLineas.getString(cLineas.getColumnIndex("textolinea")))
            // Estos valores nos servirán para saber si hemos modificado una línea del albarán original y para calcular
            // la diferencia, que es el dato que al final quedará en el nuevo albarán.
            values.put(
                "cajasorg",
                cLineas.getString(cLineas.getColumnIndex("cajas")).replace(',', '.')
            )
            values.put(
                "cantidadorg",
                cLineas.getString(cLineas.getColumnIndex("cantidad")).replace(',', '.')
            )
            if (cLineas.getString(cLineas.getColumnIndex("piezas")) != null) values.put(
                "piezasorg",
                cLineas.getString(cLineas.getColumnIndex("piezas")).replace(',', '.')
            )
            values.put("modif_nueva", "F")
            dbAlba.insert("lineas", null, values)
            copiarDtosCascAAlbaran(viejaLinea)
            cLineas.moveToNext()
        }
        cLineas.close()
    }

    @SuppressLint("Range")
    private fun copiarDtosCascAAlbaran(viejaLinea: Int) {
        val values = ContentValues()
        val cDtosCas =
            dbAlba.rawQuery("SELECT linea FROM desctoslineas WHERE linea = $viejaLinea", null)
        cDtosCas.moveToFirst()
        while (!cDtosCas.isAfterLast) {
            values.put("linea", cDtosCas.getInt(cDtosCas.getColumnIndex("linea")))
            values.put("orden", cDtosCas.getInt(cDtosCas.getColumnIndex("orden")))
            values.put("descuento", cDtosCas.getString(cDtosCas.getColumnIndex("descuento")))
            values.put("importe", cDtosCas.getString(cDtosCas.getColumnIndex("importe")))
            values.put("cantidad1", cDtosCas.getString(cDtosCas.getColumnIndex("cantidad1")))
            values.put("cantidad2", cDtosCas.getString(cDtosCas.getColumnIndex("cantidad2")))
            values.put("desderating", cDtosCas.getString(cDtosCas.getColumnIndex("desderating")))
            dbAlba.insert("desctoslineas", null, values)
            cDtosCas.moveToNext()
        }
        cDtosCas.close()
    }

    // Borraremos las líneas que no han sido modificadas ni insertadas.
    @SuppressLint("Range")
    fun borrarLineasNoModif() {
        val fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresa.toString().toInt())
        cLineas.moveToFirst()
        while (!cLineas.isAfterLast) {
            val fLinea = cLineas.getInt(cLineas.getColumnIndexOrThrow("_id"))
            if (cLineas.getString(cLineas.getColumnIndex("modif_nueva"))
                    .equals("F", ignoreCase = true)
            ) {
                borrarLinea(fLinea, false)
            } else {
                val fAplicarIva = fClientes.getAplicarIva()
                fPrecio = cLineas.getString(cLineas.getColumnIndex("precio")).toDouble()
                fPrecioII = cLineas.getString(cLineas.getColumnIndex("precioii")).toDouble()
                fCodigoIva = cLineas.getString(cLineas.getColumnIndex("codigoiva")).toShort()
                fDtoLin = cLineas.getString(cLineas.getColumnIndex("dto")).toDouble()
                fDtoImp = cLineas.getString(cLineas.getColumnIndex("dtoi")).toDouble()
                fDtoImpII = cLineas.getString(cLineas.getColumnIndex("dtoiii")).toDouble()
                fTasa1 = cLineas.getString(cLineas.getColumnIndex("tasa1")).toDouble()
                fTasa2 = cLineas.getString(cLineas.getColumnIndex("tasa2")).toDouble()
                fOldCajas = 0.0
                var fOldPiezas = 0.0
                fOldCantidad = 0.0
                if (cLineas.getString(cLineas.getColumnIndex("cajasorg")) != null) fOldCajas =
                    cLineas.getString(
                        cLineas.getColumnIndex("cajasorg")
                    ).toDouble()
                fCajas = cLineas.getString(cLineas.getColumnIndex("cajas")).toDouble()
                if (cLineas.getString(cLineas.getColumnIndex("piezasorg")) != null) fOldPiezas =
                    cLineas.getString(
                        cLineas.getColumnIndex("piezasorg")
                    ).toDouble()
                fPiezas = cLineas.getString(cLineas.getColumnIndex("piezas")).toDouble()
                if (cLineas.getString(cLineas.getColumnIndex("cantidadorg")) != null) fOldCantidad =
                    cLineas.getString(
                        cLineas.getColumnIndex("cantidadorg")
                    ).toDouble()
                fCantidad = cLineas.getString(cLineas.getColumnIndex("cantidad")).toDouble()
                // Recalculamos las cantidades restando las antiguas, de forma que el resultado sea la diferencia.
                fCajas -= fOldCajas
                fCantidad -= fOldCantidad
                fPiezas -= fOldPiezas
                if (fIvaIncluido && fAplicarIva) {
                    calcularImpteII(false)
                    calcularImpte(true)
                } else {
                    calcularImpte(false)
                    calcularImpteII(true)
                }
                val values = ContentValues()
                values.put("cajas", fCajas)
                values.put("piezas", fPiezas)
                values.put("cantidad", fCantidad)
                values.put("importe", fImporte)
                values.put("importeii", fImpteII)
                dbAlba.update("lineas", values, "_id=$fLinea", null)
            }
            cLineas.moveToNext()
        }
    }

    @SuppressLint("Range")
    fun recalcularBases() {
        fBases.fLista.clear()
        configurarBases()
        cLineas.moveToFirst()
        while (!cLineas.isAfterLast) {
            fImporte = cLineas.getString(cLineas.getColumnIndex("importe")).toDouble()
            fImpteII = cLineas.getString(cLineas.getColumnIndex("importeii")).toDouble()
            fCodigoIva = cLineas.getShort(cLineas.getColumnIndex("codigoiva"))
            if (fBases.fIvaIncluido) fBases.calcularBase(
                fCodigoIva,
                fImpteII
            ) else fBases.calcularBase(fCodigoIva, fImporte)
            cLineas.moveToNext()
        }
    }

    @SuppressLint("Range")
    fun cargarDocumento(QueIdDoc: Int, borrarOftVol: Boolean) {
        cDocumentos = dbAlba.rawQuery("SELECT * FROM cabeceras WHERE _id=$QueIdDoc", null)
        if (cDocumentos.moveToFirst()) {
            // Establezco el cliente del documento y las propiedades necesarias para cargar las líneas y el pie del documento.
            fIdDoc = QueIdDoc
            setCliente(cDocumentos.getInt(cDocumentos.getColumnIndex("cliente")))
            fTipoDoc = cDocumentos.getString(cDocumentos.getColumnIndex("tipodoc")).toShort()
            fAlmacen = cDocumentos.getShort(cDocumentos.getColumnIndex("alm"))
            serie = cDocumentos.getString(cDocumentos.getColumnIndex("serie"))
            numero = cDocumentos.getInt(cDocumentos.getColumnIndex("numero"))
            fEjercicio = cDocumentos.getShort(cDocumentos.getColumnIndex("ejer"))
            fEmpresa = cDocumentos.getShort(cDocumentos.getColumnIndex("empresa"))
            fFecha = cDocumentos.getString(cDocumentos.getColumnIndex("fecha"))
            if (cDocumentos.getString(cDocumentos.getColumnIndex("hora")) != null)
                fHora = cDocumentos.getString(cDocumentos.getColumnIndex("hora"))
            else
                fHora = ""
            fObs1 = cDocumentos.getString(cDocumentos.getColumnIndex("obs1"))
            fObs2 = cDocumentos.getString(cDocumentos.getColumnIndex("obs2"))
            fIncidenciaDoc = cDocumentos.getInt(cDocumentos.getColumnIndex("tipoincidencia"))
            if (cDocumentos.getString(cDocumentos.getColumnIndex("tipoincidencia")) != null)
                fTextoIncidencia = cDocumentos.getString(cDocumentos.getColumnIndex("textoincidencia"))
            else
                fTextoIncidencia = ""
            if (cDocumentos.getString(cDocumentos.getColumnIndex("fechaentrega")) != null)
                fFEntrega = cDocumentos.getString(cDocumentos.getColumnIndex("fechaentrega"))
            else
                fFEntrega = ""
            if (cDocumentos.getString(cDocumentos.getColumnIndex("fpago")) != null)
                fPago = cDocumentos.getString(cDocumentos.getColumnIndex("fpago"))
            else
                fPago = ""

            // Vemos si tenemos que aplicar las ofertas si el documento es un pedido de Bionat
            if (fTipoDoc == TIPODOC_PEDIDO && fConfiguracion.codigoProducto() == "UY6JK-6KAYw-PO0Py-6OX9B-OJOPY") {
                fAplOftEnPed = cDocumentos.getInt(cDocumentos.getColumnIndex("hoja")) == 1
            }
            fDtoPie1 = cDocumentos.getString(cDocumentos.getColumnIndex("dto")).replace(',', '.')
                .toDouble()
            fDtoPie2 =
                if (cDocumentos.getString(cDocumentos.getColumnIndex("dto2")) == null) 0.0 else cDocumentos.getString(
                    cDocumentos.getColumnIndex("dto2")
                ).replace(',', '.').toDouble()
            fDtoPie3 =
                if (cDocumentos.getString(cDocumentos.getColumnIndex("dto3")) == null) 0.0 else cDocumentos.getString(
                    cDocumentos.getColumnIndex("dto3")
                ).replace(',', '.').toDouble()
            fDtoPie4 =
                if (cDocumentos.getString(cDocumentos.getColumnIndex("dto4")) == null) 0.0 else cDocumentos.getString(
                    cDocumentos.getColumnIndex("dto4")
                ).replace(',', '.').toDouble()

            // Abrimos el cursor con las líneas del documento.
            abrirLineas()
            // Borramos las líneas de oferta por volumen
            if (borrarOftVol) {
                borrarOftVolumen(false)
                refrescarLineas()
            }

            // Vemos si el documento es exento
            setExento()
            // Le digo al objeto fBases1 que se recalcule.
            // fTotalAnterior nos servirá para las modificaciones (por ahora para recalcular el pendiente del cliente).
            // Limpiamos fBases1 antes de cargar.
            fBases.fLista.clear()
            fBases.cargarDesdeDoc(fIdDoc)
            fTotalAnterior = fBases.totalConImptos
        }
    }

    fun esContado(): Boolean {
        val pendienteDao = myBD?.pendienteDao()
        val generaCobro = pendienteDao?.esContado(fEmpresa, fAlmacen, serie, numero, fEjercicio) ?: "F"
        return generaCobro == "T"
    }

    fun docNuevoEsContado(): Boolean {
        return fPagoClase.fPagoEsContado(fPago)
    }

    private fun inicializarDocumento() {
        fIdDoc = -1
        fAlmacen = fConfiguracion.almacen()
        fEjercicio = fConfiguracion.ejercicio()
        // Nos sirve para saber si queremos llevar el stock en la tablet o no
        fControlarStock = fConfiguracion.controlarStock()
        fUsarTrazabilidad = fConfiguracion.usarTrazabilidad()
        // Obtenemos la fecha actual.
        val tim = System.currentTimeMillis()
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        fFecha = df.format(tim)
        fFEntrega = fFecha
        fTipoPedido = 0
        val dfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        fHora = dfHora.format(tim)
        fDecPrBase = fConfiguracion.decimalesPrecioBase()
        fDecPrII = fConfiguracion.decimalesPrecioIva()
        fDecImpBase = fConfiguracion.decimalesImpBase()
        fDecImpII = fConfiguracion.decimalesImpII()
        fAlmDireccion = ""
        fOrdenDireccion = ""
        inicializarPie()
    }

    fun inicializarPie() {
        fObs1 = ""
        fObs2 = ""
        fIncidenciaDoc = 0
        fTextoIncidencia = ""
        fDtoPie1 = 0.0
        fDtoPie2 = 0.0
        fDtoPie3 = 0.0
        fDtoPie4 = 0.0
        fPago = ""
    }

    fun setSerieNumero(queSerie: String): Boolean {
        val seriesDao = myBD?.seriesDao()
        serie = queSerie
        return if (serie == "") {
            false
        } else {
            if (fTipoDoc > 0) {
                numero = fConfiguracion.getNumero(serie, fEjercicio, fTipoDoc)
                fEmpresa = seriesDao?.getEmpresa(serie, fEjercicio.toInt()) ?: 0
                // Comprobaremos que no exista ya un documento con la serie y el número durante 20 intentos.
                // Si hemos recibido los contadores sin actualizar, al terminar el documento se actualizarán
                // con el último número que hayamos realizado.
                if (numero > 0) {
                    var fNumVeces = 0
                    while (!serieNumeroValidos() && fNumVeces < 20) {
                        numero++
                        fNumVeces++
                    }
                    fNumVeces < 20
                } else false
            } else false
        }
    }

    fun setExento() {
        val seriesDao = myBD?.seriesDao()
        val fClteExento = !fClientes.getAplicarIva()
        val queFlag = seriesDao?.getFlag(serie, fEjercicio.toInt()) ?: 0
        fSerieExenta = queFlag and FLAGSERIE_INV_SUJ_PASIVO > 0
        val fParaFraSimpl = queFlag and FLAGSERIE_PARA_FRA_SIMPL > 0

        // Si la serie es para facturas simplificadas el documento siempre aplica iva y no aplica recargo.
        if (fParaFraSimpl) {
            fAplicarIva = true
            fAplicarRe = false
        } else {
            // Si a un cliente exento se le realiza un documento es siempre exento (excepto si la serie es para fra. simpl.)
            if (fClteExento) {
                fAplicarIva = false
                fAplicarRe = false
            } else {
                if (fSerieExenta) {
                    fAplicarIva = false
                    fAplicarRe = false
                } else {
                    fAplicarIva = true
                    fAplicarRe = fClientes.getAplicarRe()
                }
            }
        }
        configurarBases()
    }

    private fun configurarBases() {
        // Configuramos el objeto de bases. Establecemos sus variables y a continuación
        // le hacemos que aplique la configuración sobre su clase interna TBaseDocumento.
        fBases.fAplicarIva = fAplicarIva
        fBases.fAplicarRecargo = fAplicarRe
        fBases.fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresa.toString().toInt())
        fBases.fDecImpBase = fConfiguracion.decimalesImpBase()
        fBases.fDecImpII = fConfiguracion.decimalesImpII()
    }

    private fun serieNumeroValidos(): Boolean {
        val cDocs = dbAlba.rawQuery(
            "SELECT numero FROM cabeceras" +
                    " WHERE tipodoc = " + fTipoDoc + " AND alm = " + fAlmacen +
                    " AND serie = '" + serie + "' AND numero = " + numero + " AND ejer = " + fEjercicio,
            null
        )
        val resultado = !cDocs.moveToFirst()
        cDocs.close()
        return resultado
    }

    private fun actualizarNumero() {
        val seriesDao = myBD?.seriesDao()
        when (fTipoDoc) {
            2.toShort() -> seriesDao?.setNumAlbaran(serie, fEjercicio.toInt(), numero + 1)
            3.toShort() -> seriesDao?.setNumPedido(serie, fEjercicio.toInt(), numero + 1)
            6.toShort() -> seriesDao?.setNumPresupuesto(serie, fEjercicio.toInt(), numero + 1)
            else -> seriesDao?.setNumFactura(serie, fEjercicio.toInt(), numero + 1)
        }
    }

    fun inicializarLinea() {
        fArticulo = 0
        fCodArt = ""
        fDescr = ""
        fPrecio = 0.0
        fPrecioII = 0.0
        fCodigoIva = 0
        fCajas = 0.0
        fPiezas = 0.0
        fCantidad = 0.0
        fImporte = 0.0
        fImpteII = 0.0
        fDtoLin = 0.0
        fDtoImp = 0.0
        fDtoImpII = 0.0
        fDtoRatingImp = 0.0
        fPorcIva = 0.0
        fPrecioTarifa = 0.0
        fDtoLinTarifa = 0.0
        fAlmacPedido = ""
        fArtEnOferta = false
        fArtSinCargo = false
        fPrecioRating = false
        fHayCambPrecio = false
        fLote = ""
        fLineaConDtCasc = false
        fTasa1 = 0.0
        fTasa2 = 0.0
        fFormatoLin = 0
        fCodIncidencia = 0
        // Volvemos a establecer la tarifa de la línea porque podemos haber vendido con tarifa de cajas anteriormente.
        // Si no lo hiciéramos mantendríamos la tarifa de cajas o la que hubiéramos usado anteriormente.
        fTarifaLin = fTarifaDoc
        fTextoLinea = ""
        fFlag5 = 0
        fLineaPorPiezas = false
        fLineaEsEnlace = false
    }

    @SuppressLint("Range")
    fun cargarLinea(fLinea: Int): Boolean {
        var fEncontrada = false
        val sPorcIva: String
        cLineas.moveToFirst()
        while (!cLineas.isAfterLast) {
            if (fLinea == cLineas.getInt(cLineas.getColumnIndexOrThrow("_id"))) {
                fEncontrada = true
                break
            }
            cLineas.moveToNext()
        }
        return if (fEncontrada) {
            sPorcIva =
                if (cLineas.getString(cLineas.getColumnIndex("porciva")) == null) "0.0" else cLineas.getString(
                    cLineas.getColumnIndex("porciva")
                ).replace(',', '.')
            fArticulo = cLineas.getInt(cLineas.getColumnIndex("articulo"))
            fCodArt = cLineas.getString(cLineas.getColumnIndex("codigo"))
            fDescr = cLineas.getString(cLineas.getColumnIndex("descr"))
            fTarifaLin = cLineas.getString(cLineas.getColumnIndex("tarifa")).toByte()
            //fAlmacen = cLineas.getShort(cLineas.getColumnIndex("alm"));
            fPorcIva = sPorcIva.toDouble()
            fPrecio = cLineas.getString(cLineas.getColumnIndex("precio")).toDouble()
            if (cLineas.getString(cLineas.getColumnIndex("precioii")) != null) fPrecioII =
                cLineas.getString(
                    cLineas.getColumnIndex("precioii")
                ).toDouble() else calculaPrecioII()
            fPrecioTarifa =
                if (cLineas.getString(cLineas.getColumnIndex("precioTarifa")) != null) cLineas.getString(
                    cLineas.getColumnIndex("precioTarifa")
                ).toDouble() else fPrecio
            fCodigoIva = cLineas.getShort(cLineas.getColumnIndex("codigoiva"))
            fCajas = cLineas.getString(cLineas.getColumnIndex("cajas")).toDouble()
            fPiezas = cLineas.getString(cLineas.getColumnIndex("piezas")).toDouble()
            fOldCajas = fCajas
            fCantidad = cLineas.getString(cLineas.getColumnIndex("cantidad")).toDouble()
            fOldCantidad = fCantidad
            fImporte = cLineas.getString(cLineas.getColumnIndex("importe")).toDouble()
            fDtoLin = cLineas.getString(cLineas.getColumnIndex("dto")).toDouble()
            fDtoLinTarifa =
                if (cLineas.getString(cLineas.getColumnIndex("dtoTarifa")) != null) cLineas.getString(
                    cLineas.getColumnIndex("dtoTarifa")
                ).toDouble() else fDtoLin
            fDtoImp =
                if (cLineas.getString(cLineas.getColumnIndex("dtoi")) != null) cLineas.getString(
                    cLineas.getColumnIndex("dtoi")
                ).toDouble() else 0.0
            fDtoImpII =
                0.0 // Por ahora lo dejamos así, tiene que tener algún valor porque lo usamos en algunas funciones.
            // Vemos si la línea es sin cargo.
            val queFlag = cLineas.getInt(cLineas.getColumnIndex("flag"))
            fArtEnOferta = queFlag and FLAGLINEAVENTA_ARTICULO_EN_OFERTA > 0
            fArtSinCargo = queFlag and FLAGLINEAVENTA_SIN_CARGO > 0
            fPrecioRating = queFlag and FLAGLINEAVENTA_PRECIO_RATING > 0
            fHayCambPrecio = queFlag and FLAGLINEAVENTA_CAMBIAR_PRECIO > 0
            val queFlag3 = cLineas.getInt(cLineas.getColumnIndex("flag3"))
            fLineaPorPiezas = queFlag3 and FLAG3LINEAVENTA_PRECIO_POR_PIEZAS > 0
            fCodIncidencia = cLineas.getInt(cLineas.getColumnIndex("incidencia"))
            fFormatoLin = cLineas.getString(cLineas.getColumnIndex("formato")).toByte()
            if (cLineas.getString(cLineas.getColumnIndex("textolinea")) != null)
                fTextoLinea = cLineas.getString(cLineas.getColumnIndex("textolinea"))
            else
                fTextoLinea = ""
            fFlag5 = cLineas.getInt(cLineas.getColumnIndex("flag5"))
            if (cLineas.getString(cLineas.getColumnIndexOrThrow("almacenPedido")) != null)
                fAlmacPedido = cLineas.getString(cLineas.getColumnIndexOrThrow("almacenPedido"))
            else
                fAlmacPedido = ""

            // Vemos si la línea tiene descuentos en cascada.
            val cDtosCas =
                dbAlba.rawQuery("SELECT linea FROM desctoslineas WHERE linea = $fLinea", null)
            fLineaConDtCasc = cDtosCas.moveToFirst()
            cDtosCas.close()
            if (cLineas.getString(cLineas.getColumnIndex("lote")) != null)
                fLote = cLineas.getString(cLineas.getColumnIndex("lote"))
            else
                fLote = ""
            if (fLote == null) fLote = ""
            fOldLote = fLote
            fTasa1 =
                if (cLineas.getString(cLineas.getColumnIndex("tasa1")) != null) cLineas.getString(
                    cLineas.getColumnIndex("tasa1")
                ).toDouble() else 0.0
            fTasa2 =
                if (cLineas.getString(cLineas.getColumnIndex("tasa2")) != null) cLineas.getString(
                    cLineas.getColumnIndex("tasa2")
                ).toDouble() else 0.0
            true
        } else false
    }

    @SuppressLint("Range")
    fun grabarHistorico() {
        val fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresa.toString().toInt())
        dbAlba.rawQuery("SELECT * FROM tmphco", null).use { cTmpHco ->
            cTmpHco.moveToFirst()
            while (!cTmpHco.isAfterLast) {

                // Comprobamos que alguna de las cantidades sea distinta de cero.
                if (cTmpHco.getString(cTmpHco.getColumnIndex("cajas")).toDouble() != 0.0 ||
                    cTmpHco.getString(cTmpHco.getColumnIndex("cantidad")).toDouble() != 0.0 ||
                    cTmpHco.getString(cTmpHco.getColumnIndex("piezas")).toDouble() != 0.0
                ) {
                    inicializarLinea()
                    fArticulo = cTmpHco.getInt(cTmpHco.getColumnIndex("articulo"))
                    fCodArt = cTmpHco.getString(cTmpHco.getColumnIndex("codigo"))
                    fDescr = cTmpHco.getString(cTmpHco.getColumnIndex("descr"))
                    fTarifaLin = fTarifaDoc
                    fPrecio = cTmpHco.getString(cTmpHco.getColumnIndex("precio")).toDouble()
                    fPrecioII = cTmpHco.getString(cTmpHco.getColumnIndex("precioii")).toDouble()
                    fCodigoIva = cTmpHco.getString(cTmpHco.getColumnIndex("codigoiva")).toShort()
                    fCajas = cTmpHco.getString(cTmpHco.getColumnIndex("cajas")).toDouble()
                    fCantidad = cTmpHco.getString(cTmpHco.getColumnIndex("cantidad")).toDouble()
                    fPiezas = cTmpHco.getString(cTmpHco.getColumnIndex("piezas")).toDouble()
                    fDtoLin = cTmpHco.getString(cTmpHco.getColumnIndex("dto")).toDouble()
                    fDtoImp = cTmpHco.getString(cTmpHco.getColumnIndex("dtoi")).toDouble()
                    fDtoImpII = cTmpHco.getString(cTmpHco.getColumnIndex("dtoiii")).toDouble()
                    fTasa1 = cTmpHco.getString(cTmpHco.getColumnIndex("tasa1")).toDouble()
                    fTasa2 = cTmpHco.getString(cTmpHco.getColumnIndex("tasa2")).toDouble()
                    fFormatoLin = cTmpHco.getString(cTmpHco.getColumnIndex("formato")).toByte()
                    if (cTmpHco.getString(cTmpHco.getColumnIndex("textolinea")) != null)
                        fTextoLinea = cTmpHco.getString(cTmpHco.getColumnIndex("textolinea"))
                    else
                        fTextoLinea = ""
                    if (cTmpHco.getString(cTmpHco.getColumnIndex("lote")) != null)
                        fLote = cTmpHco.getString(cTmpHco.getColumnIndex("lote"))
                    else
                        fLote = ""
                    // Por ahora entenderemos que el precio y el dto. que vienen del hco. van a ser el precio y dto. de tarifa,
                    // por si luego modificamos la línea
                    fPrecioTarifa = fPrecio
                    fDtoLinTarifa = fDtoLin
                    if (cTmpHco.getString(cTmpHco.getColumnIndex("almacenPedido")) != null)
                        fAlmacPedido = cTmpHco.getString(cTmpHco.getColumnIndex("almacenPedido"))
                    else
                        fAlmacPedido = ""
                    fCodIncidencia = cTmpHco.getInt(cTmpHco.getColumnIndex("incidencia"))
                    // Tenemos que calcular fLineaPorPiezas antes de llamar a calcularImpte(), ya que fLineaPorPiezas interviene en esta función
                    val queFlag3 = cTmpHco.getInt(cTmpHco.getColumnIndex("flag3"))
                    fLineaPorPiezas = queFlag3 and FLAG3LINEAVENTA_PRECIO_POR_PIEZAS > 0

                    // Calculamos los importes.
                    if (fIvaIncluido && fAplicarIva) {
                        calcularImpteII(false)
                        calcularImpte(true)
                    } else {
                        calcularImpte(false)
                        calcularImpteII(true)
                    }
                    // Si tenemos descuento por importe lo añadimos como descuento en cascada
                    if (fDtoImp != 0.0) {
                        anyadirDtoCascada()
                        fDtoImp = 0.0
                        fDtoImpII = 0.0
                    }
                    // Mantenemos los flags que traigamos del histórico.
                    val queFlag = cTmpHco.getInt(cTmpHco.getColumnIndex("flag"))
                    fArtEnOferta = queFlag and FLAGLINEAVENTA_ARTICULO_EN_OFERTA > 0
                    fArtSinCargo = queFlag and FLAGLINEAVENTA_SIN_CARGO > 0
                    fPrecioRating = queFlag and FLAGLINEAVENTA_PRECIO_RATING > 0
                    fHayCambPrecio = queFlag and FLAGLINEAVENTA_CAMBIAR_PRECIO > 0
                    fFlag5 = cTmpHco.getInt(cTmpHco.getColumnIndex("flag5"))
                    insertarLinea()
                }
                cTmpHco.moveToNext()
            }
            // Borro el histórico.
            dbAlba.delete("tmphco", "1=1", null)
        }
        refrescarLineas()
    }

    private fun anyadirDtoCascada() {
        val values = ContentValues()
        values.put("descuento", 0.0)
        values.put("importe", fDtoImp)
        values.put("cantidad1", 0.0)
        values.put("cantidad2", 0.0)
        values.put("linea", -1)
        values.put("orden", 1)
        values.put("desderating", "T")
        insertarDtoCasc(values)
        fDtosCascada.abrir(-1)
        // Configuramos el objeto de los dtos. en cascada
        fDtosCascada.fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresa.toString().toInt())
        fDtosCascada.fAplicarIva = fClientes.getAplicarIva()
        fDtosCascada.fPorcIva = fPorcIva
        fDtosCascada.fDecPrBase = fConfiguracion.decimalesPrecioBase()
        fDtosCascada.fExentoIva = !fAplicarIva
        fDtoLin = fDtosCascada.calcularDtoEquiv(fPrecio, fDecPrBase).toDouble()
        fLineaConDtCasc = true
    }

    fun insertarLinea() {
        val values = ContentValues()
        values.put("cabeceraId", fIdDoc)
        values.put("linea", siguienteLinea())
        values.put("articulo", fArticulo)
        values.put("codigo", fCodArt)
        values.put("descr", fDescr)
        values.put("tarifa", fTarifaLin)
        values.put("precio", fPrecio)
        values.put("precioii", fPrecioII)
        values.put("codigoiva", fCodigoIva)
        values.put("cajas", fCajas)
        values.put("piezas", fPiezas)
        values.put("cantidad", fCantidad)
        values.put("importe", fImporte)
        values.put("importeii", fImpteII)
        values.put("dto", fDtoLin)
        values.put("dtoi", fDtoImp)
        values.put("dtoiii", fDtoImpII)
        values.put("lote", fLote)
        values.put("flag5", fFlag5)
        values.put("modif_nueva", "T")
        values.put(
            "precioTarifa",
            fPrecioTarifa
        ) // Nos servirán para saber si modificamos el precio de tarifa y, en este caso,
        values.put("dtoTarifa", fDtoLinTarifa) // no aplicar oferta por volumen.
        values.put("almacenPedido", fAlmacPedido)
        var queFlag = 0
        if (fArtSinCargo) queFlag = queFlag or FLAGLINEAVENTA_SIN_CARGO
        if (fPrecioRating) queFlag = queFlag or FLAGLINEAVENTA_PRECIO_RATING
        if (fHayCambPrecio) queFlag = queFlag or FLAGLINEAVENTA_CAMBIAR_PRECIO
        if (fArtEnOferta && !fPrecioRating && !fHayCambPrecio) queFlag =
            queFlag or FLAGLINEAVENTA_ARTICULO_EN_OFERTA
        // Si el artículo está en oferta pero vamos a aplicar el precio por rating lo marcamos como posible oferta,
        // siempre que no hayamos cambiado el precio
        if (fArtEnOferta && fPrecioRating && !fHayCambPrecio) queFlag =
            queFlag or FLAGLINEAVENTA_POSIBLE_OFERTA
        var queFlag3 = 0
        if (fLineaPorPiezas) {
            queFlag3 = queFlag3 or FLAG3LINEAVENTA_PRECIO_POR_PIEZAS
            queFlag3 = queFlag3 or FLAG3LINEAVENTA_ARTICULO_POR_PIEZAS
            queFlag = queFlag or FLAGLINEAVENTA_CAMBIAR_TARIFA_PRECIO
            queFlag = queFlag or FLAGLINEAVENTA_CAMBIAR_DESCRIPCION
        }
        values.put("flag", queFlag)
        values.put("flag3", queFlag3)
        values.put("tasa1", fTasa1)
        values.put("tasa2", fTasa2)
        values.put("formato", fFormatoLin)
        values.put("incidencia", fCodIncidencia)
        // (Si trabajamos con artículos habituales (p.ej. Pare Pere), grabaremos en el texto de la línea
        // el texto que tenga el artículo para el cliente del documento y el formato de la línea.) Por ahora grabamos siempre el texto
        //if (fHayArtHabituales) values.put("textolinea", fTextoLinea);
        values.put("textolinea", fTextoLinea)
        if (fLineaEsEnlace) values.put("esEnlace", "T") else values.put("esEnlace", "F")
        val fIdLinea = dbAlba.insert("lineas", null, values)

        // Si la línea tiene descuentos en cascada lo que hacemos es reemplazar en la tabla "desctoslineas"
        // el campo linea, que estará a -1, por el id de la línea que acabamos de insertar.
        if (fLineaConDtCasc && fIdLinea > -1) {
            asignarLineaADtos(fIdLinea)
        }

        // Actualizamos el stock del artículo, si el almacen <> 0.
        if (fControlarStock && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN)) fArticulos.actualizarStock(
            fArticulo,
            fEmpresa,
            fCantidad,
            fCajas,
            false
        )
        // Actualizamos el stock del lote. Aunque el lote esté en blanco lo llevaremos también a la tabla de lotes, porque me viene
        // bien para el momento de realizar el fin de día de las cargas.
        if (fUsarTrazabilidad && fLote != null && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN)) fLotes.actStockLote(
            fArticulo,
            fCantidad,
            fLote,
            fEmpresa
        )
        if (fBases.fIvaIncluido) fBases.calcularBase(
            fCodigoIva,
            fImpteII
        ) else fBases.calcularBase(fCodigoIva, fImporte)
        refrescarLineas()
    }

    // Vemos si el artículo tiene texto habitual
    val textoArtHabitual: String
        @SuppressLint("Range")
        get() {
            // Vemos si el artículo tiene texto habitual
            val sQuery = ("SELECT texto FROM arthabituales WHERE articulo = " + fArticulo
                    + " AND cliente = " + fCliente + " AND (formato = " + fFormatoLin + " OR formato = 0)")
            dbAlba.rawQuery(sQuery, null).use { cTxtArtHabit ->
                return if (cTxtArtHabit.moveToFirst()) {
                    cTxtArtHabit.getString(cTxtArtHabit.getColumnIndex("texto"))
                } else ""
            }
        }

    @SuppressLint("Range")
    fun getDescrFormato(queCodigo: Int): String {
        if (fFormatoLin.toInt() != 0) {
            dbAlba.rawQuery("SELECT descr FROM formatos WHERE codigo = $queCodigo", null)
                .use { cDescrFto ->
                    return if (cDescrFto.moveToFirst()) cDescrFto.getString(
                        cDescrFto.getColumnIndex("descr")) else ""
                }
        } else return ""
    }

    private fun asignarLineaADtos(fIdLinea: Long) {
        val values = ContentValues()
        values.put("linea", fIdLinea)
        dbAlba.update("desctoslineas", values, "linea=-1", null)
    }

    fun insertarDtoCasc(values: ContentValues?) {
        dbAlba.insert("desctoslineas", null, values)
    }

    fun editarDtoCasc(values: ContentValues?, fLineaDto: Int) {
        dbAlba.update("desctoslineas", values, "_id=$fLineaDto", null)
    }

    fun borrarDtosCasc(fIdLinea: Long) {
        dbAlba.delete("desctoslineas", "linea=$fIdLinea", null)
    }

    @SuppressLint("Range")
    fun editarLinea(fLinea: Int) {
        val fOldImpte = cLineas.getString(cLineas.getColumnIndex("importe")).toDouble()
        var fOldImpteII: Double
        if (cLineas.getString(cLineas.getColumnIndex("importeii")) != null) fOldImpteII =
            cLineas.getString(
                cLineas.getColumnIndex("importeii")
            ).toDouble() else {
            if (!fAplicarIva) fOldImpteII = fOldImpte else {
                fOldImpteII = fOldImpte + fOldImpte * fPorcIva / 100
                fOldImpteII = Redondear(fOldImpteII, fDecImpII)
            }
        }
        val values = ContentValues()
        values.put("precio", fPrecio)
        values.put("precioii", fPrecioII)
        values.put("cajas", fCajas)
        values.put("piezas", fPiezas)
        values.put("cantidad", fCantidad)
        values.put("importe", fImporte)
        values.put("importeii", fImpteII)
        values.put("dto", fDtoLin)
        values.put("dtoi", fDtoImp)
        values.put("dtoiii", fDtoImpII)
        values.put("lote", fLote)
        values.put("tasa1", fTasa1)
        values.put("tasa2", fTasa2)
        values.put("textolinea", fTextoLinea)
        values.put("flag5", fFlag5)
        values.put("almacenPedido", fAlmacPedido)
        values.put("modif_nueva", "T")
        var queFlag = 0
        // Si la línea no tiene cargo guardamos el flag y el código de incidencia.
        if (fArtSinCargo) {
            queFlag = queFlag or FLAGLINEAVENTA_SIN_CARGO
            values.put("incidencia", fCodIncidencia)
        }
        if (fPrecioRating) queFlag = queFlag or FLAGLINEAVENTA_PRECIO_RATING
        if (fHayCambPrecio) queFlag = queFlag or FLAGLINEAVENTA_CAMBIAR_PRECIO
        if (fArtEnOferta && !fPrecioRating && !fHayCambPrecio) queFlag =
            queFlag or FLAGLINEAVENTA_ARTICULO_EN_OFERTA
        // Si el artículo está en oferta pero vamos a aplicar el precio por rating lo marcamos como posible oferta,
        // siempre que no hayamos cambiado el precio
        if (fArtEnOferta && fPrecioRating && !fHayCambPrecio) queFlag =
            queFlag or FLAGLINEAVENTA_POSIBLE_OFERTA
        var queFlag3 = 0
        if (fLineaPorPiezas) {
            queFlag3 = queFlag3 or FLAG3LINEAVENTA_PRECIO_POR_PIEZAS
            queFlag3 = queFlag3 or FLAG3LINEAVENTA_ARTICULO_POR_PIEZAS
            queFlag = queFlag or FLAGLINEAVENTA_CAMBIAR_TARIFA_PRECIO
            queFlag = queFlag or FLAGLINEAVENTA_CAMBIAR_DESCRIPCION
        }
        values.put("flag", queFlag)
        values.put("flag3", queFlag3)
        dbAlba.update("lineas", values, "_id=$fLinea", null)
        // Actualizamos el stock del artículo
        if (fControlarStock && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN)) fArticulos.actualizarStock(
            fArticulo,
            fEmpresa,
            fCantidad - fOldCantidad,
            fCajas - fOldCajas,
            false
        )

        // Actualizamos el stock del lote.
        if (fUsarTrazabilidad && fLote != null && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN)) fLotes.actStockLote(
            fArticulo,
            fCantidad,
            fLote,
            fEmpresa
        )
        if (fUsarTrazabilidad && fOldLote != null && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN)) fLotes.actStockLote(
            fArticulo,
            -fOldCantidad,
            fOldLote,
            fEmpresa
        )
        if (fBases.fIvaIncluido) fBases.calcularBase(
            fCodigoIva,
            -fOldImpteII
        ) else fBases.calcularBase(fCodigoIva, -fOldImpte)
        if (fBases.fIvaIncluido) fBases.calcularBase(
            fCodigoIva,
            fImpteII
        ) else fBases.calcularBase(fCodigoIva, fImporte)
        refrescarLineas()
    }

    fun terminarDoc(DocNuevo: Boolean, queEstado: String) {
        val values = ContentValues()
        if (DocNuevo) {
            values.put("tipodoc", fTipoDoc)
            values.put("tipoPedido", fTipoPedido)
            // Por ahora el campo Facturado irá a falso.
            values.put("facturado", "F")
            values.put("alm", fAlmacen)
            values.put("serie", serie)
            values.put("numero", numero)
            values.put("ejer", fEjercicio)
            values.put("empresa", fEmpresa)
            values.put("fecha", fFecha)
            values.put("hora", fHora)
            values.put("cliente", fCliente)
            values.put("ruta", fClientes.getRuta())
        }
        values.put("fechaentrega", fFEntrega)
        values.put("apliva", logicoACadena(fAplicarIva))
        values.put("aplrec", logicoACadena(fAplicarRe))
        values.put("dto", fDtoPie1)
        values.put("dto2", fDtoPie2)
        values.put("dto3", fDtoPie3)
        values.put("dto4", fDtoPie4)
        values.put("fpago", fPago)
        values.put("bruto", fBases.totalBruto)
        values.put("base", fBases.totalBases)
        values.put("iva", fBases.totalIva)
        values.put("recargo", fBases.totalRe)
        values.put("total", fBases.totalConImptos)
        if (queEstado == "") values.put("estado", "N") else values.put("estado", queEstado)

        // Por ahora el flag "AplicarIvaCliente" no se usa, ya que no viene en la configuración.
        if (fConfiguracion.ivaIncluido(fEmpresa.toString().toInt())) values.put(
            "flag",
            FLAGCABECERAVENTA_PRECIOS_IVA_INCLUIDO
        ) else values.put("flag", 0)
        values.put("obs1", fObs1)
        values.put("obs2", fObs2)
        values.put("tipoincidencia", fIncidenciaDoc)
        values.put("textoincidencia", fTextoIncidencia)
        // Dirección para el pedido
        values.put("almDireccion", fAlmDireccion)
        values.put("ordenDireccion", fOrdenDireccion)

        // Si el documento es un pedido de Bionat grabamos si hemos aplicado las ofertas o no. Para ello aprovechamos
        // el campo 'Hoja', ya que Bionat no lo utiliza
        if (DocNuevo && fTipoDoc == TIPODOC_PEDIDO && fConfiguracion.codigoProducto() == "UY6JK-6KAYw-PO0Py-6OX9B-OJOPY") {
            if (fAplOftEnPed) values.put("hoja", 1) else values.put("hoja", 0)
        }
        if (DocNuevo) {
            fIdDoc = dbAlba.insert("cabeceras", null, values).toInt()

            // En las líneas del nuevo documento hemos ido guardando cabeceraId a -1 y ahora lo actualizamos
            actualizarIdCabecera()

            // Actualizamos el contador.
            actualizarNumero()
            // Si estamos haciendo un pedido actualizaremos el pendiente del cliente.
            if (fTipoDoc == TIPODOC_PEDIDO) actualizarPendiente(true)
        } else {
            dbAlba.update("cabeceras", values, "_id=$fIdDoc", null)
            // Si estamos haciendo un pedido actualizaremos el pendiente del cliente.
            if (fTipoDoc == TIPODOC_PEDIDO) actualizarPendiente(false)
        }
    }

    private fun actualizarPendiente(docNuevo: Boolean) {
        val fTotalDoc = fBases.totalConImptos
        if (docNuevo) fClientes.actualizarPendiente(
            fCliente,
            fTotalDoc
        ) else fClientes.actualizarPendiente(fCliente, fTotalDoc - fTotalAnterior)
    }

    fun calcularDtosPie() {
        fBases.calcularDtosPie(fDtoPie1, fDtoPie2, fDtoPie3, fDtoPie4)
    }

    @SuppressLint("Range")
    fun verOftVolumen() {
        val lOftVol = ArrayList<ListOftVol>()
        var queArticulo: Int
        var queTarifaLin: String
        var oListOftVol: ListOftVol
        var indice: Int
        var linConCambPrecio: Boolean
        var linConRating: Boolean

        cLineas.moveToFirst()
        while (!cLineas.isAfterLast) {
            val queFlag = cLineas.getInt(cLineas.getColumnIndex("flag"))
            linConCambPrecio = queFlag and FLAGLINEAVENTA_CAMBIAR_PRECIO > 0
            linConRating = queFlag and FLAGLINEAVENTA_PRECIO_RATING > 0
            // Las líneas con cambio de precio no contabilizan para las ofertas por volumen. Tampoco las que tengan precio por rating.
            if (!linConCambPrecio && !linConRating) {
                queArticulo = cLineas.getInt(cLineas.getColumnIndex("articulo"))
                queTarifaLin = cLineas.getString(cLineas.getColumnIndex("tarifa"))

                // Buscamos si el artículo tiene oferta por volumen e insertamos en la lista.
                val lista: List<ListOftVol> =
                    ofertasDao?.getOftVolArt(queArticulo, fEmpresa, queTarifaLin.toShort(), fechaEnJulian(fFecha))
                        ?: emptyList()

                for (oferta in lista) {
                    oListOftVol = ListOftVol()
                    oListOftVol.idOferta = oferta.idOferta
                    oListOftVol.articuloDesct = oferta.articuloDesct
                    oListOftVol.tarifa = oferta.tarifa
                    oListOftVol.importe =
                        cLineas.getString(cLineas.getColumnIndex("importe")).toDouble()
                    indice = localizaId(lOftVol, oListOftVol.idOferta)
                    if (indice > -1) lOftVol[indice].importe =
                        lOftVol[indice].importe + oListOftVol.importe else lOftVol.add(oListOftVol)
                }
            }
            cLineas.moveToNext()
        }
        // Añadimos las ofertas
        if (lOftVol.isNotEmpty()) anyadirOftVol(lOftVol)
    }

    private fun anyadirOftVol(lista: ArrayList<ListOftVol>) {
        var cArticuloDto: Cursor
        var dDto: Double
        for (oftVol in lista) {

            // Tendremos que averiguar si el importe acumulado para cada oferta está entre algún rango
            val queDescto: String = ofVolRangosDao?.getDescuento(oftVol.idOferta, oftVol.importe) ?: "0.0"
            if (queDescto != "") {
                dDto = queDescto.replace(',', '.').toDouble()
                cArticuloDto = dbAlba.rawQuery(
                    "SELECT articulo FROM articulos WHERE articulo = " + oftVol.articuloDesct,
                    null
                )
                if (cArticuloDto.moveToFirst()) {
                    insertarLineaOftVol(dDto, oftVol)
                    // Marcamos las líneas del documento que pertenezcan a esta oferta con el flag 4096 (linea con oferta),
                    // siempre que no tengan cambio de precio
                    marcarLinComoOfta(oftVol.idOferta)
                } else MsjAlerta(fContexto).alerta("No se encontró el artículo para los descuentos de la oferta")
                cArticuloDto.close()
            } else  // Si no hemos llegado a completar la oferta marcamos las líneas como posible oferta.
                marcarLinComoPosibleOfta(oftVol.idOferta)
        }
    }

    @SuppressLint("Range")
    private fun marcarLinComoPosibleOfta(queIdOfta: Int) {
        val values = ContentValues()

        // Cargamos una lista con todos los artículos de la oferta
        val lArtOfta: List<Int> = ofertasDao?.getAllArtOftaId(queIdOfta) ?: emptyList()
        for (queArticulo in lArtOfta) {
            // Buscamos el artículo en concreto dentro del documento, excluyendo las lineas con cambio de precio
            val cIdLinea = dbAlba.rawQuery(
                "SELECT _id, flag FROM lineas WHERE articulo = " + queArticulo
                        + " AND flag & " + FLAGLINEAVENTA_CAMBIAR_PRECIO + " = 0"
                        + " AND cabeceraId = " + fIdDoc, null
            )
            // Actualizamos la línea con el flag 16384 (artículo en posible oferta)
            if (cIdLinea.moveToFirst()) {
                val queLinea = cIdLinea.getInt(cIdLinea.getColumnIndex("_id"))
                values.put("flag", FLAGLINEAVENTA_POSIBLE_OFERTA)
                dbAlba.update("lineas", values, "_id=$queLinea", null)
            }
            cIdLinea.close()
        }
    }

    @SuppressLint("Range")
    private fun marcarLinComoOfta(queIdOfta: Int) {
        val values = ContentValues()
        // Cargamos una lista con todos los artículos de la oferta
        val lArtOfta: List<Int> = ofertasDao?.getAllArtOftaId(queIdOfta) ?: emptyList()
        for (queArticulo in lArtOfta) {
            // Buscamos el artículo en concreto dentro del documento, excluyendo las lineas con cambio de precio
            val cIdLinea = dbAlba.rawQuery(
                "SELECT _id, flag FROM lineas WHERE articulo = " + queArticulo
                        + " AND flag & " + FLAGLINEAVENTA_CAMBIAR_PRECIO + " = 0"
                        + " AND cabeceraId = " + fIdDoc, null
            )
            // Actualizamos la línea con el flag 4096 (artículo en oferta)
            if (cIdLinea.moveToFirst()) {
                val queLinea = cIdLinea.getInt(cIdLinea.getColumnIndex("_id"))
                values.put("flag", FLAGLINEAVENTA_POSIBLE_OFERTA)
                dbAlba.update("lineas", values, "_id=$queLinea", null)
            }
            cIdLinea.close()
        }
    }

    @SuppressLint("Range")
    private fun insertarLineaOftVol(dDto: Double, oftVol: ListOftVol) {
        val fFtoDecImpBase = fConfiguracion.formatoDecImptesBase()
        val values = ContentValues()
        values.put("tipodoc", fTipoDoc)
        values.put("alm", fAlmacen)
        values.put("serie", serie)
        values.put("numero", numero)
        values.put("ejer", fEjercicio)
        values.put("linea", siguienteLinea())
        values.put("articulo", oftVol.articuloDesct)
        val cArticulos = dbAlba.rawQuery(
            "SELECT A.codigo, A.descr, I.codigo codiva, I.iva FROM articulos A" +
                    " LEFT JOIN ivas I ON I.tipo = A.tipoiva" +
                    " WHERE A.articulo = " + oftVol.articuloDesct, null
        )
        if (cArticulos.moveToFirst()) {
            values.put("codigo", cArticulos.getString(cArticulos.getColumnIndex("codigo")))
            values.put(
                "descr",
                "[DTO " + String.format(
                    Locale.getDefault(),
                    "%.2f",
                    dDto
                ) + "% sobre " + String.format(fFtoDecImpBase, oftVol.importe) + " €] " +
                        cArticulos.getString(cArticulos.getColumnIndex("descr"))
            )
        }
        values.put("tarifa", oftVol.tarifa)
        fPrecio = oftVol.importe * dDto / 100
        fCodigoIva = cArticulos.getShort(cArticulos.getColumnIndex("codiva"))
        fPorcIva = cArticulos.getDouble(cArticulos.getColumnIndex("iva"))
        fDtoImp = 0.0
        fImporte = fPrecio * -1
        values.put("precio", fPrecio)
        calculaPrecioII()
        calcularDtoImpII()
        calcularImpteII(true)
        values.put("precioii", fPrecioII)
        values.put("codigoiva", fCodigoIva)
        values.put("cajas", 0)
        values.put("piezas", 0)
        fCantidad = -1.0
        values.put("cantidad", fCantidad)
        values.put("importe", fImporte)
        values.put("importeii", fImpteII)
        values.put("dto", 0)
        values.put("dtoi", fDtoImp)
        values.put("dtoiii", 0)
        values.put("lote", "")
        values.put("flag", 16) // Cambiar descripción
        values.put("flag3", 128) // Ajuste por oferta
        values.put("idOferta", oftVol.idOferta)
        values.put("dtoOftVol", dDto)
        cArticulos.close()
        dbAlba.insert("lineas", null, values)

        // Recalculamos las bases
        if (fBases.fIvaIncluido) fBases.calcularBase(
            fCodigoIva,
            fImpteII
        ) else fBases.calcularBase(fCodigoIva, fImporte)
    }

    fun hayOftVolumen(): Boolean {
        val cHayOftVol = dbAlba.rawQuery(
            "SELECT * FROM lineas"
                    + " WHERE cabeceraId = " + fIdDoc + " AND flag3 = 128", null
        )
        val resultado = cHayOftVol.moveToFirst()
        cHayOftVol.close()
        return resultado
    }

    fun cargarCursorOftVol(): Cursor {
        val cLinOftVol = dbAlba.rawQuery(
            "SELECT _id, descr, importe FROM lineas"
                    + " WHERE flag3 = 128 AND cabeceraId = " + fIdDoc, null
        )
        cLinOftVol.moveToFirst()
        return cLinOftVol
    }

    private fun localizaId(lista: ArrayList<ListOftVol>, queId: Int): Int {
        var result = -1
        for (oftVol in lista) {
            if (oftVol.idOferta == queId) result = lista.indexOf(oftVol)
        }
        return result
    }

    @SuppressLint("Range")
    fun borrarArticuloDeDoc(queArticulo: Int) {
        cLineas.moveToFirst()
        while (!cLineas.isAfterLast) {
            if (cLineas.getInt(cLineas.getColumnIndex("articulo")) == queArticulo) {
                // He detectado que si no refresco no borra. No tenemos problemas al hacer
                // el break porque a esta función se llama una vez por cada formato, de forma que
                // si un artículo tiene varios formatos se llamará una vez por cada uno de ellos y,
                // al final, se borran todas las líneas de dicho artículo.
                borrarLinea(cLineas.getInt(cLineas.getColumnIndex("_id")), true)
                break
            }
            cLineas.moveToNext()
        }
    }

    @SuppressLint("Range")
    fun borrarLinea(fLinea: Int, refrescar: Boolean) {
        if (cLineas.count > 0) {
            fArticulo = cLineas.getInt(cLineas.getColumnIndex("articulo"))
            fOldCajas = cLineas.getString(cLineas.getColumnIndex("cajas")).replace(",", ".").toDouble()
            fOldCantidad = cLineas.getString(cLineas.getColumnIndex("cantidad")).replace(",", ".").toDouble()
            if (cLineas.getString(cLineas.getColumnIndex("lote")) != null)
                fOldLote = cLineas.getString(cLineas.getColumnIndex("lote"))
            else
                fOldLote = ""
            val fOldImpte = cLineas.getString(cLineas.getColumnIndex("importe")).replace(",", ".").toDouble()
            val fOldImpteII: Double = if (cLineas.getString(cLineas.getColumnIndex("importeii")) != null) cLineas.getString(
                    cLineas.getColumnIndex("importeii")
                ).replace(",", ".").toDouble() else 0.0

            val queCodIva = cLineas.getShort(cLineas.getColumnIndex("codigoiva"))

            // Recalculamos las bases.
            if (fBases.fIvaIncluido) fBases.calcularBase(queCodIva, -fOldImpteII)
            else fBases.calcularBase(queCodIva, -fOldImpte)
            dbAlba.delete("lineas", "_id=$fLinea", null)

            // Borramos las posibles líneas de descuentos en cascada
            dbAlba.delete("desctoslineas", "linea=$fLinea", null)
            // Actualizamos el stock del artículo
            if (fControlarStock && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN))
                fArticulos.actualizarStock(fArticulo, fEmpresa, -fOldCantidad, -fOldCajas, false)

            // Actualizamos el stock del lote.
            if (fUsarTrazabilidad && fOldLote != null && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN))
                fLotes.actStockLote(fArticulo, -fOldCantidad, fOldLote, fEmpresa)
            if (refrescar) refrescarLineas()
        }
    }

    private fun refrescarLineas() {
        cLineas.close()
        abrirLineas()
    }

    private fun siguienteLinea(): Int {
        dbAlba.rawQuery(
            "SELECT MAX(linea) linea FROM lineas"
                    + " WHERE cabeceraId = " + fIdDoc, null
        ).use { cUltLinea ->
            return if (cUltLinea.moveToFirst()) {
                val columna = cUltLinea.getColumnIndex("linea")
                cUltLinea.getInt(columna) + 1
            } else 1
        }
    }

    fun setCliente(QueCliente: Int) {
        fCliente = QueCliente
        // Abrimos el objeto fClientes para tener acceso a los datos del cliente del documento.
        fClientes.abrirUnCliente(QueCliente)
        // Aplicaremos la tarifa según configuración: si tenemos configurado usar la
        // del cliente usaremos la de éste, si no, la que tengamos para ventas.
        fTarifaDoc = fConfiguracion.tarifaVentas()
        if (fConfiguracion.usarTarifaClte()) {
            if (fClientes.getTarifa() != "" && fClientes.getTarifa() != "0") fTarifaDoc =
                fClientes.getTarifa().toByte()
        }
        // La tarifa de descuento será la que tenga el cliente en su ficha y, si no, la que esté en configuración.
        val queTarifaDto = fClientes.getTarifaDto()
        //if (!fClientes.getTarifaDto().equals("0"))
        fTarifaDto = if (queTarifaDto != "0" && queTarifaDto != "") fClientes.getTarifaDto()
            .toByte() else fConfiguracion.tarifaDto()
        // Por ahora la tarifa de la línea será la del documento, a no ser que cambiemos luego.
        fTarifaLin = fTarifaDoc
    }

    fun nombreCliente(): String {
        return fClientes.getNFiscal()
    }

    fun nombreComClte(): String {
        return fClientes.getNComercial()
    }

    fun marcarComoImprimido(queId: Int) {
        val values = ContentValues()
        values.put("imprimido", "T")
        dbAlba.update("cabeceras", values, "_id=$queId", null)
    }

    fun marcarComoEntregado(queId: Int, queCliente: Int, queEmpresa: Int, refrescar: Boolean) {
        // Obtenemos la fecha y hora actuales, que son las que grabaremos como fecha y hora de la firma.
        val tim = System.currentTimeMillis()
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        val values = ContentValues()
        values.put("firmado", "T")
        values.put("fechafirma", df.format(tim))
        values.put("horafirma", dfHora.format(tim))
        dbAlba.update("cabeceras", values, "_id=$queId", null)
        if (refrescar) {
            // Refresco el cursor cerrándolo y volviéndolo a abrir.
            cDocumentos.close()
            abrirTodos(queCliente, queEmpresa, 0)
        }
    }

    fun setTextoIncidencia(
        queId: Int,
        queTexto: String?,
        queCliente: Int,
        queEmpresa: Int,
        queTipoIncid: Int
    ) {
        val values = ContentValues()
        values.put("tipoincidencia", queTipoIncid)
        values.put("textoincidencia", queTexto)
        dbAlba.update("cabeceras", values, "_id=$queId", null)

        // Refresco el cursor cerrándolo y volviéndolo a abrir.
        cDocumentos.close()
        abrirTodos(queCliente, queEmpresa, 0)
    }

    @SuppressLint("Range")
    fun reenviar(QueID: Int, QueCliente: Int, empresaActual: Int) {
        val queAlmacen: Short
        val queEjercicio: Short
        val queEmpresa: Short
        val queNumero: Int
        val queSerie: String
        val values = ContentValues()
        values.put("estado", "R")
        dbAlba.update("cabeceras", values, "_id=$QueID", null)
        val queTipoDoc: Byte = cDocumentos.getString(cDocumentos.getColumnIndex("tipodoc")).toByte()
        if (queTipoDoc.toShort() == TIPODOC_FACTURA) {
            queAlmacen = cDocumentos.getShort(cDocumentos.getColumnIndex("alm"))
            queSerie = cDocumentos.getString(cDocumentos.getColumnIndex("serie"))
            queNumero = cDocumentos.getInt(cDocumentos.getColumnIndex("numero"))
            queEjercicio = cDocumentos.getShort(cDocumentos.getColumnIndex("ejer"))
            queEmpresa = cDocumentos.getShort(cDocumentos.getColumnIndex("empresa"))

            // Si marcamos para reenviar una factura también tendremos que reenviar el pendiente.
            val pendienteDao = myBD?.pendienteDao()
            pendienteDao?.reenviar(queTipoDoc, queEmpresa, queAlmacen, queSerie, queNumero, queEjercicio)
        }
        // Refresco el cursor cerrándolo y volviéndolo a abrir.
        cDocumentos.close()
        abrirTodos(QueCliente, empresaActual, 0)
    }

    fun calculaPrecioYDto(pGrupo: String, pDpto: String, pProv: String, porcIva: Double) {
        // El orden para obtener el precio y el dto. del artículo será el siguiente:
        // 1º- Del histórico, último precio de venta (si está configurado).
        // 2º- De la oferta o el rating.
        // 3º- De la tarifa.
        var queGrupo = pGrupo
        var queDpto = pDpto
        var queProv = pProv
        fPrecio = 0.0
        fDtoLin = 0.0
        fDtoImp = 0.0
        fDtoRatingImp = 0.0
        fPrecioII = 0.0
        fPorcIva = porcIva

        // Antes de nada vemos el precio y dto. que tenemos en la tarifa y los asignamos a fPrecioRating y fDtoRating
        tomaPrecioTarifa()
        val quePrTarifa = fPrecio
        var fPrRating = 0.0
        var fDtoRating = 0.0
        var fPrecioOfta = 0.0
        var fDtoOfta = 0.0
        val fPrecio1: Double
        val fPrecio2: Double
        fPrecio = 0.0
        fDtoLin = 0.0

        // Comprobamos que tenemos grupo y departamento.
        if (queGrupo == null) queGrupo = ""
        if (queDpto == null) queDpto = ""
        if (queProv == null) queProv = ""

        // Vemos el precio del histórico.
        if (fConfiguracion.pvpHistorico()) {
            tomaPrecioHco()
        }
        // Si tenemos configurado aplicar el precio más ventajoso obviamos la configuración del rating.
        if (fPrecio == 0.0 && fConfiguracion.aplicarPvpMasVent() && fClientes.getAplicarOfertas()) {
            if (fConfiguracion.usarRating()) {
                tomaPrecioRating(queGrupo, queDpto, queProv)
                if (fPrecio != 0.0) fPrRating = fPrecio
                if (fDtoRatingImp != 0.0) {
                    // Puede ser que en el rating sólo tengamos un descuento, por lo que éste se
                    // aplicará sobre el precio de tarifa.
                    if (fPrRating == 0.0) fPrRating = quePrTarifa
                } else if (fDtoLin != 0.0) {
                    fDtoRating = fDtoLin
                    // Puede ser que en el rating sólo tengamos un descuento, por lo que éste se
                    // aplicará sobre el precio de tarifa.
                    if (fPrRating == 0.0) fPrRating = quePrTarifa
                }
            }
            fPrecio = 0.0
            fDtoLin = 0.0
            // Si estamos haciendo un pedido comprobaremos que queremos aplicar ofertas en el mismo
            if (fTipoDoc != TIPODOC_PEDIDO || fAplOftEnPed) {
                tomaPrecioOferta()
                fPrecioOfta = fPrecio
                fDtoOfta = fDtoLin
            }
            fPrecio1 =
                if (fDtoRatingImp != 0.0) fPrRating - fDtoRatingImp else fPrRating - fPrRating * fDtoRating / 100
            fPrecio2 = fPrecioOfta - fPrecioOfta * fDtoOfta / 100
            if (fPrecio1 != 0.0) {
                if (fPrecio2 != 0.0) {
                    // Si tenemos valor en ambos precios calculamos el más ventajoso
                    if (fPrecio1 < fPrecio2) {
                        fPrecioRating = true
                        fPrecio = fPrRating
                        fArtEnOferta =
                            false // Nos aseguramos de quitar el flag si no vamos a aplicar la oferta
                        if (fDtoRatingImp == 0.0) fDtoLin = fDtoRating
                    } else {
                        fPrecio = fPrecioOfta
                        fDtoLin = fDtoOfta
                        fDtoRatingImp = 0.0
                    }
                    // Si sólo tenemos precio del rating aplicamos éste
                } else {
                    fPrecioRating = true
                    fPrecio = fPrRating
                    if (fDtoRatingImp == 0.0) fDtoLin = fDtoRating
                }
            } else {
                // Si sólo tenemos precio de la oferta aplicamos ésta
                if (fPrecio2 != 0.0) {
                    fPrecio = fPrecioOfta
                    fDtoLin = fDtoOfta
                    fDtoRatingImp = 0.0
                }
            }
        } else {
            // Vemos el rating.
            if (fConfiguracion.predominaRating() && fConfiguracion.usarRating() && fPrecio == 0.0) {
                tomaPrecioRating(queGrupo, queDpto, queProv)
                if (fPrecio != 0.0 || fDtoLin != 0.0 || fDtoRatingImp != 0.0) fPrecioRating = true
            }
            // Vemos si el cliente usa ofertas.
            if (fClientes.getAplicarOfertas() && fPrecio == 0.0 && fDtoLin == 0.0) {
                // Si estamos haciendo un pedido comprobaremos que queremos aplicar ofertas en el mismo
                if (fTipoDoc != TIPODOC_PEDIDO || fAplOftEnPed) {
                    fDtoRatingImp = 0.0
                    tomaPrecioOferta()
                }
            }
            // Volvemos a ver el rating, en el caso de que no predomine sobre la oferta.
            if (!fConfiguracion.predominaRating() && fConfiguracion.usarRating() && fPrecio == 0.0) {
                tomaPrecioRating(queGrupo, queDpto, queProv)
                if (fPrecio != 0.0 || fDtoLin != 0.0 || fDtoRatingImp != 0.0) fPrecioRating = true
            }
        }

        // Cargamos el precio de la tarifa del cliente.
        if (fPrecio == 0.0) tomaPrecioTarifa()
        if (fPrecio != 0.0) calculaPrecioII()
    }

    fun calculaPrecioII() {
        if (!fAplicarIva) {
            fPrecioII = fPrecio
        } else {
            fPrecioII = fPrecio + fPrecio * fPorcIva / 100
            fPrecioII = Redondear(fPrecioII, fDecPrII)
        }
    }

    fun calculaPrBase() {
        fPrecio = if (!fAplicarIva) fPrecioII else {
            // Si estamos vendiendo iva incluído grabaremos el precio base con dos
            // decimales más de los que usemos para el precio iva incluído, para que, al
            // recalcular el precio iva incluído desde el precio base, no tengamos
            // problemas. Por ej.: precio iva incluído: 14.250 (iva 10%), precio base:
            // 12.95454545. Si grabáramos como precio base 12.955 (redondeado a 3
            // decimales), al volver a calcular el precio iva incluído nos saldría
            // 14.251.
            val dIvaDiv = (100 + fPorcIva) / 100
            Redondear(fPrecioII / dIvaDiv, fDecPrII + 2)
        }
    }

    fun calcularImpte(desdeIvaIncl: Boolean): Boolean {
        if (desdeIvaIncl) {
            fImporte = if (!fAplicarIva) {
                fImpteII
            } else {
                val dIvaDiv = (100 + fPorcIva) / 100
                try {
                    Redondear(fImpteII / dIvaDiv, fDecImpII + 1)
                } catch (e: NumberFormatException) {
                    val texto = "Error al redondear, número demasiado alto"
                    val textoGrande = SpannableStringBuilder(texto)
                    textoGrande.setSpan(RelativeSizeSpan(1.80f), 0, texto.length, 0)
                    Toast.makeText(fContexto, textoGrande, Toast.LENGTH_LONG).show()
                    return false
                }
            }
        } else {
            fImporte = if (fLineaPorPiezas) fPrecio * fPiezas else fPrecio * fCantidad

            // Calculo el % de dto. y se lo resto al importe, así como también el dto. en euros.
            val dImpteDto = fImporte * fDtoLin / 100
            fImporte -= dImpteDto

            // Tenemos en cuenta las tasas a la hora de calcular el importe (punto verde, mer, etc.). También el posible dto. por importe.
            fImporte = if (fLineaPorPiezas) {
                fImporte + (fTasa1 + fTasa2 - fDtoImp) * fPiezas
            } else {
                fImporte + (fTasa1 + fTasa2 - fDtoImp) * fCantidad
            }
            fImporte = try {
                Redondear(fImporte, fDecImpBase)
            } catch (e: NumberFormatException) {
                val texto = "Error al redondear, número demasiado alto"
                val textoGrande = SpannableStringBuilder(texto)
                textoGrande.setSpan(RelativeSizeSpan(1.80f), 0, texto.length, 0)
                Toast.makeText(fContexto, textoGrande, Toast.LENGTH_LONG).show()
                return false
            }
        }
        return true
    }

    fun calcularImpteII(desdeBase: Boolean): Boolean {
        if (desdeBase) {
            if (!fAplicarIva) fImpteII = fImporte else {
                fImpteII = fImporte + fImporte * fPorcIva / 100
                fImpteII = try {
                    Redondear(fImpteII, fDecImpII)
                } catch (e: NumberFormatException) {
                    val texto = "Error al redondear, número demasiado alto"
                    val textoGrande = SpannableStringBuilder(texto)
                    textoGrande.setSpan(RelativeSizeSpan(1.80f), 0, texto.length, 0)
                    Toast.makeText(fContexto, textoGrande, Toast.LENGTH_LONG).show()
                    return false
                }
            }
        } else {
            fImpteII = if (fLineaPorPiezas) fPrecioII * fPiezas else fPrecioII * fCantidad
            val dImpteDto = fImpteII * fDtoLin / 100
            fImpteII -= dImpteDto

            // Tenemos en cuenta las tasas a la hora de calcular el importe (punto verde, mer, etc.). También el posible dto. por importe.
            var fTasa1II = 0.0
            var fTasa2II = 0.0
            if (fTasa1 != 0.0) fTasa1II = fTasa1 + fTasa1 * fPorcIva / 100
            if (fTasa2 != 0.0) fTasa2II = fTasa2 + fTasa2 * fPorcIva / 100
            calcularDtoImpII()
            fImpteII = if (fLineaPorPiezas) {
                fImpteII + (fTasa1II + fTasa2II - fDtoImpII) * fPiezas
            } else {
                fImpteII + (fTasa1II + fTasa2II - fDtoImpII) * fCantidad
            }
            fImpteII = try {
                Redondear(fImpteII, fDecImpII)
            } catch (e: NumberFormatException) {
                val texto = "Error al redondear, número demasiado alto"
                val textoGrande = SpannableStringBuilder(texto)
                textoGrande.setSpan(RelativeSizeSpan(1.80f), 0, texto.length, 0)
                Toast.makeText(fContexto, textoGrande, Toast.LENGTH_LONG).show()
                return false
            }
        }
        return true
    }

    fun calcularDtoImpBase() {
        val dIvaDiv = (100 + fPorcIva) / 100
        fDtoImp = Redondear(fDtoImpII / dIvaDiv, 2)
    }

    fun calcularDtoImpII() {
        fDtoImpII = fDtoImp + fDtoImp * fPorcIva / 100
        fDtoImpII = Redondear(fDtoImpII, 2)
    }

    private fun tomaPrecioHco() {
        // Si estamos vendiendo con formatos tomaremos del histórico el precio del formato seleccionado
        val cPrecio: Cursor = if (fFormatoLin > 0) {
            dbAlba.rawQuery(
                "SELECT precio, dto FROM historico WHERE cliente = "
                        + fCliente + " AND articulo = " + fArticulo + " AND formato = " + fFormatoLin,
                null
            )
        } else {
            dbAlba.rawQuery(
                "SELECT precio, dto FROM historico WHERE cliente = "
                        + fCliente + " AND articulo = " + fArticulo, null
            )
        }
        if (cPrecio.moveToFirst()) {
            val sPrecio = cPrecio.getString(0).replace(',', '.')
            val sDto = cPrecio.getString(1).replace(',', '.')
            fPrecio = Redondear(sPrecio.toDouble(), fDecPrBase)
            fDtoLin = Redondear(sDto.toDouble(), 2)
        }
        cPrecio.close()
    }

    @SuppressLint("Range")
    private fun tomaPrecioOferta() {
        val ofertaEnt: OfertasEnt = if (fFormatoLin > 0) {
            ofertasDao?.getOftaVtaFto(
                fArticulo,
                fEmpresa.toInt(),
                fTarifaLin.toShort(),
                fFormatoLin.toShort(),
                fechaEnJulian(fFecha)
            ) ?: OfertasEnt()
        } else {
            ofertasDao?.getOftaVtaArt(fArticulo, fEmpresa.toInt(), fTarifaLin.toShort(), fechaEnJulian(fFecha)) ?: OfertasEnt()
        }
        if (ofertaEnt != null && ofertaEnt.articuloId > 0) {
            val sPrecio = ofertaEnt.precio.replace(',', '.')
            val sDto = ofertaEnt.dto.replace(',', '.')
            val queTipoOfta = ofertaEnt.tipoOferta.toInt()
            if (queTipoOfta != 6) {
                fPrecio = Redondear(sPrecio.toDouble(), fDecPrBase)
                fArtEnOferta = true
                if (fDtoLin == 0.0) fDtoLin = Redondear(sDto.toDouble(), 2)

                // Comprobamos si hay alguna oferta de escalado de precios
            } else {
                var quePrecOfta: Double
                val cOftPorCant = dbAlba.rawQuery(
                    "SELECT * FROM oftCantRangos WHERE articulo = $fArticulo",
                    null
                )
                var hayEscalado = false
                var desdeCantidad: Double
                var hastaCantidad: Double
                cOftPorCant.moveToFirst()
                while (!cOftPorCant.isAfterLast) {

                    // Vemos si la cantidad está entre alguno de los escalados.
                    desdeCantidad =
                        cOftPorCant.getDouble(cOftPorCant.getColumnIndex("desdeCantidad"))
                    hastaCantidad =
                        cOftPorCant.getDouble(cOftPorCant.getColumnIndex("hastaCantidad"))
                    // ¿Por qué hago lo que viene a continuación? Para que funcione la oferta por escalado porque,
                    // tal y como están definidos éstos, podemos tener un primer escalado que sea desde 0 hasta X y
                    // si fCantidad es 0 no entraría en éste, ya que la condición es fCantidad > desdeCantidad.
                    // Resumiendo, lo hago únicamente para el caso en que exista un escalado que sea desde 0 hasta X.
                    if (desdeCantidad == 0.0 && hastaCantidad > 0.0) desdeCantidad = -0.0001
                    if (fCantidad > desdeCantidad && fCantidad <= hastaCantidad) {
                        hayEscalado = true
                        fArtEnOferta = true
                        // Si el precio de la oferta es 0 significa que aplicaremos el precio de tarifa
                        quePrecOfta =
                            cOftPorCant.getString(cOftPorCant.getColumnIndex("precioBase"))
                                .replace(',', '.').toDouble()
                        fPrecio = if (quePrecOfta > 0) quePrecOfta else 0.0
                    }
                    // Vemos si la cantidad está en el escalado infinito (si es que lo tenemos definido)
                    if (!hayEscalado) {
                        if (desdeCantidad == 0.0 && hastaCantidad == 0.0) {
                            quePrecOfta =
                                cOftPorCant.getString(cOftPorCant.getColumnIndex("precioBase"))
                                    .replace(',', '.').toDouble()
                            if (quePrecOfta > 0) {
                                fPrecio = quePrecOfta
                                fArtEnOferta = true
                            }
                        }
                    }
                    cOftPorCant.moveToNext()
                }
                cOftPorCant.close()
            }
        }
    }

    private fun tomaPrecioTarifa() {
        // Vemos si tenemos formato para la línea, en cuyo caso tomamos el precio de la tabla "trfformatos".
        var cPrTrfa: Cursor
        cPrTrfa = if (fFormatoLin > 0) {
            dbAlba.rawQuery(
                "SELECT precio, dto FROM trfformatos WHERE articulo = "
                        + fArticulo + " AND tarifa = " + fTarifaLin + " AND formato = " + fFormatoLin,
                null
            )
        } else {
            dbAlba.rawQuery(
                "SELECT precio, dto FROM tarifas WHERE articulo = "
                        + fArticulo + " AND tarifa = " + fTarifaLin, null
            )
        }
        if (cPrTrfa.moveToFirst()) {
            val sPrecio = cPrTrfa.getString(0).replace(',', '.')
            val sDto = cPrTrfa.getString(1).replace(',', '.')
            fPrecio = Redondear(sPrecio.toDouble(), fDecPrBase)
            if (fDtoLin == 0.0) fDtoLin = Redondear(sDto.toDouble(), 2)
        }
        cPrTrfa.close()
        if (fTarifaDto > 0 && fDtoLin == 0.0) {
            // Idem para los descuentos.
            cPrTrfa = if (fFormatoLin > 0) {
                dbAlba.rawQuery(
                    "SELECT dto FROM trfformatos WHERE articulo = "
                            + fArticulo + " AND tarifa = " + fTarifaDto + " AND formato = " + fFormatoLin,
                    null
                )
            } else {
                dbAlba.rawQuery(
                    "SELECT dto FROM tarifas WHERE articulo = "
                            + fArticulo + " AND tarifa = " + fTarifaDto, null
                )
            }
            if (cPrTrfa.moveToFirst()) {
                val sDto = cPrTrfa.getString(0).replace(',', '.')
                fDtoLin = Redondear(sDto.toDouble(), 2)
            }
            cPrTrfa.close()
        }
    }

    private fun fechaEnJulian(queFecha: String): String {
        val queAnyo = queFecha.substring(6, 10)
        val queMes = queFecha.substring(3, 5)
        val queDia = queFecha.substring(0, 2)
        return "$queAnyo-$queMes-$queDia"
    }

    @SuppressLint("Range")
    private fun tomaPrecioRating(queGrupo: String, queDpto: String, queProv: String) {
        val ratingGrDao: RatingGruposDao? = getInstance(fContexto)?.ratingGruposDao()

        var existe = false
        var formatoEncontrado = false
        var cPrecio = dbAlba.rawQuery(
            "SELECT precio FROM ratingart",
            null
        ) // Hago esto para que no me dé el error al compilar por no estar bien inicializado.

        // Rating por artículo. Si estamos vendiendo con formato, buscaremos primero el rating para ese formato.
        if (fFormatoLin > 0) {
            cPrecio = dbAlba.rawQuery(
                "SELECT precio, dto, flag FROM ratingart"
                        + " WHERE articulo = " + fArticulo + " AND alm = " + fAlmacen
                        + " AND Cliente = " + fCliente + " AND formato = " + fFormatoLin + " AND julianday(inicio) <= julianday('"
                        + fechaEnJulian(fFecha) + "') AND julianday(fin) >= julianday('" + fechaEnJulian(
                    fFecha
                ) + "')", null
            )

            // Si no hemos encontrado el precio para el formato lo buscamos para el artículo y cliente.
            if (cPrecio.moveToFirst()) {
                formatoEncontrado = true
            }
        }
        if (!formatoEncontrado) {
            cPrecio = dbAlba.rawQuery(
                "SELECT precio, dto, flag FROM ratingart"
                        + " WHERE articulo = " + fArticulo + " AND alm = " + fAlmacen
                        + " AND Cliente = " + fCliente + " AND formato = 0 AND julianday(inicio) <= julianday('"
                        + fechaEnJulian(fFecha) + "') AND julianday(fin) >= julianday('" + fechaEnJulian(
                    fFecha
                ) + "')", null
            )
        }
        if (cPrecio.moveToFirst()) {
            val sPrecio = cPrecio.getString(0).replace(',', '.')
            val sDto = cPrecio.getString(1).replace(',', '.')
            // Vemos si el descuento es por importe o por porcentaje
            val queFlag = cPrecio.getInt(cPrecio.getColumnIndex("flag"))
            if (queFlag and FLAGRATING_DESCUENTOIMPORTE > 0) fDtoRatingImp =
                java.lang.Double.valueOf(sDto) else fDtoLin = Redondear(sDto.toDouble(), 2)
            fPrecio = Redondear(sPrecio.toDouble(), fDecPrBase)
            existe = true
        }
        cPrecio.close()

        // Rating por grupo.
        if (!existe && queGrupo != "" && queDpto != "") {
            var sDto = ratingGrDao?.getDescuento(queGrupo.toShort(), queDpto.toShort(), fAlmacen, fCliente,
                                    fechaEnJulian(fFecha)) ?: ""

            if (sDto != "") {
                sDto = sDto.replace(',', '.')
                fDtoLin = Redondear(sDto.toDouble(), 2)
                existe = true
            }
        }

        // Rating por ramo.
        if (!existe && fClientes.getRamo() != "") {
            var queRamo = fClientes.getRamo()
            if (queRamo == "") queRamo = "0"
            val iRamo = queRamo.toInt()
            if (iRamo > 0) {
                cPrecio = dbAlba.rawQuery(
                    "SELECT precio, dto, flag FROM ratingart"
                            + " WHERE articulo = " + fArticulo + " AND alm = " + fAlmacen + " AND ramo = " + queRamo
                            + " AND julianday(inicio) <= julianday('" + fechaEnJulian(fFecha) + "') AND julianday(fin) >= julianday('" + fechaEnJulian(
                        fFecha
                    ) + "')", null
                )
                if (cPrecio.moveToFirst()) {
                    val sPrecio = cPrecio.getString(0).replace(',', '.')
                    val sDto = cPrecio.getString(1).replace(',', '.')
                    // Vemos si el descuento es por importe o por porcentaje
                    val queFlag = cPrecio.getInt(cPrecio.getColumnIndex("flag"))
                    if (queFlag and FLAGRATING_DESCUENTOIMPORTE > 0) fDtoRatingImp =
                        java.lang.Double.valueOf(sDto) else fDtoLin = Redondear(sDto.toDouble(), 2)
                    fPrecio = Redondear(sPrecio.toDouble(), fDecPrBase)
                    existe = true
                }
                cPrecio.close()
            }
        }

        // Rating por grupo/tarifa
        if (!existe && queGrupo != "" && queDpto != "") {
            var queRamo = fClientes.getRamo()
            if (queRamo == "") queRamo = "0"
            val iRamo = queRamo.toInt()
            if (iRamo > 0) {
                var sDto = ratingGrDao?.getDtoRamoTarifa(queGrupo.toShort(), queDpto.toShort(), fAlmacen,
                                    queRamo.toShort(), fTarifaLin.toShort(), fechaEnJulian(fFecha)) ?: ""


                if (sDto != "") {
                    sDto = cPrecio.getString(0).replace(',', '.')
                    fDtoLin = Redondear(sDto.toDouble(), 2)
                    existe = true
                }
            }
        }

        // Rating por clientes/proveedor
        if (!existe && queProv != "") {
            var sDto = ratingProvDao?.getDtoClteProv(queProv.toInt(), fAlmacen, fCliente, fechaEnJulian(fFecha)) ?: ""

            if (sDto != "") {
                sDto =  sDto.replace(',', '.')
                fDtoLin = Redondear(sDto.toDouble(), 2)
                existe = true
            }
        }

        // Rating por ramo/tarifa/proveedor
        if (!existe && queProv != "") {
            var queRamo = fClientes.getRamo()
            if (queRamo == "") queRamo = "0"
            val iRamo = queRamo.toInt()
            if (iRamo > 0) {
                var sDto = ratingProvDao?.getDtoRamoTrfProv(queProv.toInt(), fAlmacen, queRamo.toShort(),
                                    fTarifaLin.toShort(), fechaEnJulian(fFecha)) ?: ""

                if (sDto != "") {
                    sDto = sDto.replace(',', '.')
                    fDtoLin = Redondear(sDto.toDouble(), 2)
                }
            }
        }
    }

    fun hayStockLote(): Boolean {
        val stockLote = fLotes.dimeStockLote(fArticulo, fLote)
        return stockLote - fCantidad >= 0
    }

    val numLinea: Int
        @SuppressLint("Range")
        get() = cLineas.getInt(cLineas.getColumnIndex("_id"))

    @SuppressLint("Range")
    fun existeLineaArticulo(queArticulo: Int): Int {
        var iArticulo: Int
        var queLinea = 0
        cLineas.moveToFirst()
        while (!cLineas.isAfterLast) {
            iArticulo = cLineas.getInt(cLineas.getColumnIndex("articulo"))
            if (iArticulo == queArticulo) {
                queLinea = cLineas.getInt(cLineas.getColumnIndex("_id"))
                break
            }
            cLineas.moveToNext()
        }
        return queLinea
    }

    @SuppressLint("Range")
    fun artNumVecesEnDoc(queArticulo: Int): Int {
        var iArticulo: Int
        var numVeces = 0
        cLineas.moveToFirst()
        while (!cLineas.isAfterLast) {
            iArticulo = cLineas.getInt(cLineas.getColumnIndex("articulo"))
            if (iArticulo == queArticulo) {
                numVeces++
            }
            cLineas.moveToNext()
        }
        return numVeces
    }

    @SuppressLint("Range")
    fun datosArtEnCatLineas(queArticulo: Int): Array<String> {
        val catLineasDao: CatalogoLineasDao? = getInstance(fContexto)?.catalogoLineasDao()
        val lDatosCat = catLineasDao?.getDatosArt(queArticulo) ?: emptyList<CatalogoLineasEnt>().toMutableList()

        val sCantCajas = arrayOf("F", "0", "0", "0.0", "0.0", "0.0", "")
        if (lDatosCat.isNotEmpty()) {
            sCantCajas[0] = "T"
            sCantCajas[1] = String.format(Locale.getDefault(), "%.0f", lDatosCat[0].cajas.toDouble())
            sCantCajas[2] = String.format(Locale.getDefault(), "%.0f", lDatosCat[0].cantidad.toDouble())
            sCantCajas[3] = lDatosCat[0].precio
            sCantCajas[4] = lDatosCat[0].dto
            sCantCajas[5] = lDatosCat[0].importe
            sCantCajas[6] = lDatosCat[0].lote
        }
        return sCantCajas
    }


    @SuppressLint("Range")
    fun existeArtYLote(queArticulo: Int, queLote: String): Boolean {
        var iArticulo: Int
        var sLote: String
        cLineas.moveToFirst()
        while (!cLineas.isAfterLast) {
            iArticulo = cLineas.getInt(cLineas.getColumnIndex("articulo"))
            if (cLineas.getString(cLineas.getColumnIndex("lote")) != null)
                sLote = cLineas.getString(cLineas.getColumnIndex("lote"))
            else
                sLote = ""
            if (iArticulo == queArticulo && sLote == queLote) {
                return true
            }
            cLineas.moveToNext()
        }
        return false
    }

    fun poderAplTrfCajas() {
        // Buscamos si existe la tarifa que tenemos configurada como tarifa de cajas. Si es así podremos aplicar tarifa de cajas
        // en el documento, en caso contrario no.
        val queTrfCajas = fConfiguracion.tarifaCajas()
        dbAlba.rawQuery("SELECT codigo FROM cnftarifas WHERE codigo = $queTrfCajas", null)
            .use { buscaTrfCajas -> fPuedoAplTrfCajas = buscaTrfCajas.moveToFirst() }
    }

    @SuppressLint("Range")
    fun getTipoIncidencia(queIncidencia: Int): String {
        val tiposIncDao: TiposIncDao? = MyDatabase.getInstance(fContexto)?.tiposIncDao()
        val tipoInc = tiposIncDao?.getIncidencia(queIncidencia) ?: TiposIncEnt()

        return if (tipoInc.tipoIncId > 0)
            ponerCeros(tipoInc.tipoIncId.toString(), ancho_cod_incidencia) + " " + tipoInc.descripcion
        else ""
    }

    @SuppressLint("Range")
    fun getTextoIncidencia(queId: Int): String {
        dbAlba.rawQuery("SELECT textoincidencia FROM cabeceras WHERE _id = $queId", null)
            .use { buscaTextoInc ->
                if (buscaTextoInc.moveToFirst()) {
                    if (buscaTextoInc.getString(buscaTextoInc.getColumnIndex("textoincidencia")) != null)
                        return buscaTextoInc.getString(buscaTextoInc.getColumnIndex("textoincidencia"))
                    else
                        return ""
                } else
                    return ""
            }
    }

    @SuppressLint("Range")
    fun dimeCantCajasArticulo(queArticulo: Int): Array<String> {
        var iArticulo: Int
        val sCantCajas = arrayOf("0.0", "0.0")
        dbAlba.rawQuery("SELECT * FROM tmphco", null).use { cTmpHco ->
            cTmpHco.moveToFirst()
            while (!cTmpHco.isAfterLast) {
                iArticulo = cTmpHco.getInt(cTmpHco.getColumnIndex("articulo"))
                if (iArticulo == queArticulo) {
                    sCantCajas[0] = cTmpHco.getString(cTmpHco.getColumnIndex("cantidad"))
                    sCantCajas[1] = cTmpHco.getString(cTmpHco.getColumnIndex("cajas"))
                    break
                }
                cTmpHco.moveToNext()
            }
        }
        return sCantCajas
    }

    fun hayArtHabituales(): Boolean {
        dbAlba.rawQuery("SELECT articulo FROM arthabituales", null)
            .use { cursor -> return cursor.moveToFirst() }
    }

}