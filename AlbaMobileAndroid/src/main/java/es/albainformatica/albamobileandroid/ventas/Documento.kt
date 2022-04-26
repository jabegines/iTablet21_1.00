package es.albainformatica.albamobileandroid.ventas

import es.albainformatica.albamobileandroid.database.MyDatabase.Companion.getInstance
import es.albainformatica.albamobileandroid.maestros.ClientesClase
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.cobros.FormasPagoClase
import es.albainformatica.albamobileandroid.maestros.LotesClase
import android.content.ContentValues
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.widget.Toast
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.*
import es.albainformatica.albamobileandroid.entity.*
import java.lang.NumberFormatException
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by jabegines on 14/10/13.
 */
class Documento(private val fContexto: Context) {
    private val cabeceraDao: CabecerasDao? = getInstance(fContexto)?.cabecerasDao()
    private val lineasDao: LineasDao? = getInstance(fContexto)?.lineasDao()
    private val facturasDao: FacturasDao? = getInstance(fContexto)?.facturasDao()
    private val lineasFrasDao: LineasFrasDao? = getInstance(fContexto)?.lineasFrasDao()
    private val ofertasDao: OfertasDao? = getInstance(fContexto)?.ofertasDao()
    private val ofVolRangosDao: OftVolRangosDao? = getInstance(fContexto)?.oftVolRangosDao()
    private val oftCantRangosDao: OftCantRangosDao? = getInstance(fContexto)?.oftCantRangosDao()
    private val ratingProvDao: RatingProvDao? = getInstance(fContexto)?.ratingProvDao()
    private val ratingArtDao: RatingArtDao? = getInstance(fContexto)?.ratingArtDao()
    private val dtosLineasDao: DtosLineasDao? = getInstance(fContexto)?.dtosLineasDao()
    private val dtosLinFrasDao: DtosLinFrasDao? = getInstance(fContexto)?.dtosLinFrasDao()
    private val tmpHcoDao: TmpHcoDao? = getInstance(fContexto)?.tmpHcoDao()
    private val tarifasDao: TarifasDao? = getInstance(fContexto)?.tarifasDao()
    private val trfFormatosDao: TrfFormatosDao? = getInstance(fContexto)?.trfFormatosDao()
    private val seriesDao: SeriesDao? = getInstance(fContexto)?.seriesDao()
    private val pendienteDao: PendienteDao? = getInstance(fContexto)?.pendienteDao()
    private val empresasDao: EmpresasDao? = getInstance(fContexto)?.empresasDao()
    private val artHabitDao: ArtHabitualesDao? = getInstance(fContexto)?.artHabitualesDao()
    private val formatosDao: FormatosDao? = getInstance(fContexto)?.formatosDao()
    private val articulosDao: ArticulosDao? = getInstance(fContexto)?.articulosDao()
    private val historicoDao: HistoricoDao? = getInstance(fContexto)?.historicoDao()

    lateinit var lLineas: List<DatosLinVtas>
    lateinit var cabActualEnt: CabecerasEnt
    lateinit var factActualEnt: FacturasEnt

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
    var fTipoPedido: Short = 0
    var fAplOftEnPed: Boolean = false
    var fCliente = 0
    var fFecha: String = ""
    var fFEntrega: String = ""
    var fHora: String = ""
    var fTarifaDoc: Short = 0
    private var fTarifaDto: Short = 0
    var fTarifaLin: Short = 0
    var fPuedoAplTrfCajas = false
    var fFormatoLin: Short = 0
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


    fun close() {
        fBases.close()
    }

    fun abrirLineas() {
        lLineas = if (fTipoDoc == TIPODOC_FACTURA) lineasFrasDao?.abrirLineas(fIdDoc) ?: emptyList<DatosLinVtas>().toMutableList()
        else lineasDao?.abrirLineas(fIdDoc) ?: emptyList<DatosLinVtas>().toMutableList()
    }

    fun borrarOftVolumen(recBases: Boolean) {
        if (fTipoDoc == TIPODOC_FACTURA) lineasFrasDao?.borrarOftVolumen(fIdDoc)
        else lineasDao?.borrarOftVolumen(fIdDoc)

        if (recBases) recalcularBases()
    }

    fun comprobarLineasHuerfanas() {
        // Buscaremos aquellas líneas que estén sin cabecera
        val lLinSinCab = lineasDao?.getLineasHuerfanas() ?: emptyList<DatosLinVtas>().toMutableList()

        if (lLinSinCab.isNotEmpty()) {
            for (linea in lLinSinCab) {
                val fLinea = linea.lineaId
                //val queArticulo = linea.articuloId
                //val queCajas = linea.cajas.toDouble()
                //val queCantidad = linea.cantidad.toDouble()
                //val queLote = linea.lote

                lineasDao?.borrarLinea(fLinea)

                // Borramos las posibles líneas de descuentos en cascada
                dtosLineasDao?.borrarLinea(fLinea)

                // Actualizamos el stock del artículo
                //fControlarStock = fConfiguracion.controlarStock()
                //fUsarTrazabilidad = fConfiguracion.usarTrazabilidad()
                //fTipoDoc = linea.tipoDoc
                //if (fControlarStock && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN))
                //    fArticulos.actualizarStock(queArticulo, fEmpresa, -queCantidad, -queCajas, false)

                // Actualizamos el stock del lote.
                //if (fUsarTrazabilidad && queLote != "" && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN))
                //    fLotes.actStockLote(queArticulo, -queCantidad, queLote, fEmpresa)
            }
        }

        val lLinFrSinCab = lineasFrasDao?.getLineasHuerfanas() ?: emptyList<DatosLinVtas>().toMutableList()
        if (lLinFrSinCab.isNotEmpty()) {
            for (linea in lLinSinCab) {
                val fLinea = linea.lineaId
                lineasFrasDao?.borrarLinea(fLinea)

                // Borramos las posibles líneas de descuentos en cascada
                dtosLinFrasDao?.borrarLinea(fLinea)
            }
        }

        // Borro el histórico, por si he estado indicando cantidades en modo catálogo.
        tmpHcoDao?.vaciar()
    }


    fun anularDocumento() {
        for (linea in lLineas) {
            borrarLinea(linea, false)
        }

        // Borro el histórico, por si he estado indicando cantidades en modo catálogo.
        tmpHcoDao?.vaciar()
    }


    fun borrarLinea(linea: DatosLinVtas, refrescar: Boolean) {
        fArticulo = linea.articuloId
        fOldCajas = linea.cajas.replace(",", ".").toDouble()
        fOldCantidad = linea.cantidad.replace(",", ".").toDouble()
        fOldLote = linea.lote
        val fOldImpte = linea.importe.replace(",", ".").toDouble()
        val fOldImpteII: Double = if (linea.importeII != "")
            linea.importeII.replace(",", ".").toDouble()
            else 0.0

        val queCodIva = linea.codigoIva

        // Recalculamos las bases.
        if (fBases.fIvaIncluido) fBases.calcularBase(queCodIva, -fOldImpteII)
        else fBases.calcularBase(queCodIva, -fOldImpte)

        if (fTipoDoc == TIPODOC_FACTURA) {
            lineasFrasDao?.borrarLinea(linea.lineaId)
            dtosLinFrasDao?.borrarLinea(linea.lineaId)
        }
        else {
            lineasDao?.borrarLinea(linea.lineaId)

            // Borramos las posibles líneas de descuentos en cascada
            dtosLineasDao?.borrarLinea(linea.lineaId)
        }


        // Actualizamos el stock del artículo
        if (fControlarStock && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN))
            fArticulos.actualizarStock(fArticulo, fEmpresa, -fOldCantidad, -fOldCajas, false)

        // Actualizamos el stock del lote.
        if (fUsarTrazabilidad && fOldLote != "" && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN))
            fLotes.actStockLote(fArticulo, -fOldCantidad, fOldLote, fEmpresa)
        if (refrescar) refrescarLineas()
    }


    fun borrarDocumento(queIdDoc: Int) {
        // Borramos las líneas.
        anularDocumento()

        // Borramos la cabecera.
        cabeceraDao?.borrarDoc(queIdDoc)
    }

    fun borrarModifDocReparto(queIdDoc: Int) {
        // Borramos las líneas.
        anularDocumento()

        // Borramos la cabecera.
        cabeceraDao?.borrarDoc(queIdDoc)
    }

    fun marcarParaEnviar(queIdDoc: Int) {
        cabeceraDao?.marcarParaEnviar(queIdDoc)
    }

    fun abrirTodos(queCliente: Int, queEmpresa: Int, queFiltro: Int): List<DatosVerDocs> {
        // Vemos si queremos filtrar por estado
        var sCadFiltro = ""
        if (queFiltro > 0) {
            when (queFiltro) {
                1 -> sCadFiltro = " A.estado = 'P'"
                2 -> sCadFiltro = " (A.estado = 'N' OR A.estado = 'R')"
                3 -> sCadFiltro = " A.estado = 'X'"
            }
        } else sCadFiltro = "1=1"

        return if (queCliente > 0) {
            cabeceraDao?.abrirTodosClte(queCliente, queEmpresa, queFiltro) ?: emptyList<DatosVerDocs>().toMutableList()
        } else {
            cabeceraDao?.abrirTodos(queEmpresa, sCadFiltro) ?: emptyList<DatosVerDocs>().toMutableList()
        }
    }


    fun copiarAAlbaran(queIdDoc: Int): Double {
        var queTotal = 0.0
        var haySerieNumero = false

        val cabEntOrigen = cabeceraDao?.cargarDoc(queIdDoc) ?: CabecerasEnt()
        if (cabEntOrigen.cabeceraId > 0) {
            fTipoDoc = TIPODOC_ALBARAN
            // Tomamos la serie configurada para la empresa
            serie = empresasDao?.getSerieEmpresa(cabEntOrigen.empresa.toInt()) ?: ""
            haySerieNumero = setSerieNumero(serie)
            if (haySerieNumero) {
                // Tomamos el total del documento para devolverlo.
                queTotal = cabEntOrigen.total.replace(',', '.').toDouble()

                val queViejoId = cabEntOrigen.cabeceraId
                val queAlmacen = cabEntOrigen.almacen
                val queEjercicio = cabEntOrigen.ejercicio
                val queEmpresa = cabEntOrigen.empresa

                // Obtenemos la fecha y hora actuales, que son las que grabaremos en el nuevo albarán.
                val tim = System.currentTimeMillis()
                val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                fFecha = df.format(tim)
                fFEntrega = fFecha
                val dfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
                fHora = dfHora.format(tim)

                val cabEntAlbaran = CabecerasEnt()
                cabEntAlbaran.tipoDoc = TIPODOC_ALBARAN
                cabEntAlbaran.almacen = queAlmacen
                cabEntAlbaran.serie = serie
                cabEntAlbaran.numero = numero
                cabEntAlbaran.ejercicio = queEjercicio
                cabEntAlbaran.empresa = queEmpresa
                // Mantendremos el nuevo documento en la misma hoja de reparto que el original. También le daremos el mismo orden.
                cabEntAlbaran.hojaReparto = cabEntOrigen.hojaReparto
                cabEntAlbaran.ordenReparto = cabEntOrigen.ordenReparto
                cabEntAlbaran.fecha = fFecha
                cabEntAlbaran.hora = fHora
                cabEntAlbaran.clienteId = cabEntOrigen.clienteId
                cabEntAlbaran.aplicarIva = cabEntOrigen.aplicarIva
                cabEntAlbaran.aplicarRe = cabEntOrigen.aplicarRe
                cabEntAlbaran.bruto = cabEntOrigen.bruto
                cabEntAlbaran.dto = cabEntOrigen.dto.replace(',', '.')
                cabEntAlbaran.dto2 = cabEntOrigen.dto2.replace(',', '.')
                cabEntAlbaran.dto3 = cabEntOrigen.dto3.replace(',', '.')
                cabEntAlbaran.dto4 = cabEntOrigen.dto4.replace(',', '.')
                cabEntAlbaran.base = cabEntOrigen.base.replace(',', '.')
                cabEntAlbaran.iva = cabEntOrigen.iva.replace(',', '.')
                cabEntAlbaran.recargo = cabEntOrigen.recargo.replace(',', '.')
                cabEntAlbaran.total = cabEntOrigen.total.replace(',', '.')
                cabEntAlbaran.dirEnv = cabEntOrigen.dirEnv
                // Cuando modificamos un albarán de reparto, en el campo ruta pondremos lo que tengamos en 'hoja'
                cabEntAlbaran.ruta = cabEntOrigen.hojaReparto.toShort()
                cabEntAlbaran.estado = "N"
                cabEntAlbaran.flag = 0
                cabEntAlbaran.observ1 = cabEntOrigen.observ1
                cabEntAlbaran.observ2 = cabEntOrigen.observ2
                cabEntAlbaran.facturado = "F"

                fIdDoc = cabeceraDao?.insertar(cabEntAlbaran)?.toInt() ?: -1

                copiarLineasAAlb(queViejoId)
            }
        }

        return if (haySerieNumero) {
            // Una vez copiado, cargamos el albarán.
            cargarDocumento(fIdDoc, false)
            queTotal
        } else -0.00001
    }

    private fun copiarLineasAAlb(queViejoId: Int) {
        val lineasAlb = lineasDao?.abrirLineas(queViejoId) ?: emptyList<DatosLinVtas>().toMutableList()

        for (lineaOrg in lineasAlb) {
            val lineaEnt = LineasEnt()
            val viejaLinea = lineaOrg.lineaId

            lineaEnt.cabeceraId = fIdDoc
            lineaEnt.articuloId = lineaOrg.articuloId
            lineaEnt.codArticulo = lineaOrg.codArticulo
            lineaEnt.descripcion = lineaOrg.descripcion
            lineaEnt.tarifaId = lineaOrg.tarifaId
            lineaEnt.precio = lineaOrg.precio.replace(',', '.')
            lineaEnt.precioII = lineaOrg.precioII.replace(',', '.')
            lineaEnt.codigoIva = lineaOrg.codigoIva
            lineaEnt.cajas = lineaOrg.cajas.replace(',', '.')
            lineaEnt.cantidad = lineaOrg.cantidad.replace(',', '.')
            lineaEnt.importe = lineaOrg.importe.replace(',', '.')
            lineaEnt.importeII = lineaOrg.importeII.replace(',', '.')
            lineaEnt.dto = lineaOrg.dto.replace(',', '.')
            lineaEnt.dtoImpte = lineaOrg.dtoImpte.replace(',', '.')
            lineaEnt.dtoImpteII = lineaOrg.dtoImpteII.replace(',', '.')
            lineaEnt.lote = lineaOrg.lote
            lineaEnt.piezas = lineaOrg.piezas.replace(',', '.')
            lineaEnt.flag = lineaOrg.flag
            lineaEnt.flag3 = lineaOrg.flag3
            lineaEnt.flag5 = lineaOrg.flag5
            lineaEnt.tasa1 = lineaOrg.tasa1.replace(',', '.')
            lineaEnt.tasa2 = lineaOrg.tasa2.replace(',', '.')
            lineaEnt.formatoId = lineaOrg.formatoId
            lineaEnt.tipoIncId = lineaOrg.tipoIncId
            lineaEnt.textoLinea = lineaOrg.textoLinea
            // Estos valores nos servirán para saber si hemos modificado una línea del albarán original y para calcular
            // la diferencia, que es el dato que al final quedará en el nuevo albarán.
            lineaEnt.cajasOrg = lineaOrg.cajas.replace(',', '.')
            lineaEnt.cantidadOrg = lineaOrg.cantidad.replace(',', '.')
            lineaEnt.piezasOrg = lineaOrg.piezas.replace(',', '.')
            lineaEnt.modifNueva = "F"

            lineasDao?.insertar(lineaEnt)

            copiarDtosCascAAlbaran(viejaLinea)
        }
    }

    private fun copiarDtosCascAAlbaran(viejaLinea: Int) {

        val lDtos = dtosLineasDao?.getAllDtosLinea(viejaLinea) ?: emptyList<DescuentosLinea>().toMutableList()

        for (dtoOrg in lDtos) {
            val dtoLineaEnt = DtosLineasEnt()
            dtoLineaEnt.lineaId = dtoOrg.lineaId
            dtoLineaEnt.orden = dtoOrg.orden
            dtoLineaEnt.descuento = dtoOrg.descuento
            dtoLineaEnt.importe = dtoOrg.importe
            dtoLineaEnt.cantidad1 = dtoOrg.cantidad1
            dtoLineaEnt.cantidad2 = dtoOrg.cantidad2
            dtoLineaEnt.desdeRating = dtoOrg.desdeRating

            dtosLineasDao?.insertar(dtoLineaEnt)
        }
    }

    // Borraremos las líneas que no han sido modificadas ni insertadas.
    fun borrarLineasNoModif() {
        val fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresa)

        for (linea in lLineas) {
            val fLinea = linea.lineaId

            if (linea.modifNueva.equals("F", ignoreCase = true)) {
                borrarLinea(linea, false)

            } else {
                val fAplicarIva = fClientes.fAplIva
                fPrecio = linea.precio.toDouble()
                fPrecioII = linea.precioII.toDouble()
                fCodigoIva = linea.codigoIva
                fDtoLin = linea.dto.toDouble()
                fDtoImp = linea.dtoImpte.toDouble()
                fDtoImpII = linea.dtoImpteII.toDouble()
                fTasa1 = linea.tasa1.toDouble()
                fTasa2 = linea.tasa2.toDouble()
                fOldCajas = 0.0
                var fOldPiezas = 0.0
                fOldCantidad = 0.0
                if (linea.cajasOrg != "") fOldCajas = linea.cajasOrg.toDouble()
                fCajas = linea.cajas.toDouble()
                if (linea.piezasOrg != "") fOldPiezas =  linea.piezasOrg.toDouble()
                fPiezas = linea.piezas.toDouble()
                if (linea.cantidadOrg != "") fOldCantidad = linea.cantidadOrg.toDouble()
                fCantidad = linea.cantidad.toDouble()

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

                lineasDao?.actDatosReparto(fLinea, fCajas.toString(), fPiezas.toString(), fCantidad.toString(),
                                    fImporte.toString(), fImpteII.toString())
            }
        }
    }

    fun recalcularBases() {
        fBases.fLista.clear()
        configurarBases()

        for (linea in lLineas) {
            fImporte = linea.importe.toDouble()
            fImpteII = linea.importeII.toDouble()
            fCodigoIva = linea.codigoIva
            if (fBases.fIvaIncluido)
                fBases.calcularBase(fCodigoIva, fImpteII)
            else
                fBases.calcularBase(fCodigoIva, fImporte)
        }
    }


    fun cargarDocumento(queIdDoc: Int, borrarOftVol: Boolean) {
        cabActualEnt = cabeceraDao?.cargarDoc(queIdDoc) ?: CabecerasEnt()

        if (cabActualEnt.cabeceraId > 0) {
            // Establezco el cliente del documento y las propiedades necesarias para cargar las líneas y el pie del documento.
            fIdDoc = queIdDoc
            setCliente(cabActualEnt.clienteId)
            fTipoDoc = cabActualEnt.tipoDoc
            fAlmacen = cabActualEnt.almacen
            serie = cabActualEnt.serie
            numero = cabActualEnt.numero
            fEjercicio = cabActualEnt.ejercicio
            fEmpresa = cabActualEnt.empresa
            fFecha = cabActualEnt.fecha
            fHora = cabActualEnt.hora
            fObs1 = cabActualEnt.observ1
            fObs2 = cabActualEnt.observ2
            fIncidenciaDoc = cabActualEnt.tipoIncidencia
            fTextoIncidencia = cabActualEnt.textoIncidencia
            fFEntrega = cabActualEnt.fechaEntrega
            fPago = cabActualEnt.fPago

            // Vemos si tenemos que aplicar las ofertas si el documento es un pedido de Bionat
            if (fTipoDoc == TIPODOC_PEDIDO && fConfiguracion.codigoProducto() == "UY6JK-6KAYw-PO0Py-6OX9B-OJOPY") {
                fAplOftEnPed = (cabActualEnt.hojaReparto == 1)
            }
            fDtoPie1 = if (cabActualEnt.dto == "") 0.0 else cabActualEnt.dto.replace(',', '.').toDouble()
            fDtoPie2 = if (cabActualEnt.dto2 == "") 0.0 else cabActualEnt.dto2.replace(',', '.').toDouble()
            fDtoPie3 = if (cabActualEnt.dto3 == "") 0.0 else cabActualEnt.dto3.replace(',', '.').toDouble()
            fDtoPie4 = if (cabActualEnt.dto4 == "") 0.0 else cabActualEnt.dto4.replace(',', '.').toDouble()

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
            fBases.cargarDesdeDoc(fIdDoc, fTipoDoc)
            fTotalAnterior = fBases.totalConImptos
        }
    }

    fun cargarFactura(queIdDoc: Int, borrarOftVol: Boolean) {
        factActualEnt = facturasDao?.cargarDoc(queIdDoc) ?: FacturasEnt()

        if (factActualEnt.facturaId > 0) {
            // Establezco el cliente del documento y las propiedades necesarias para cargar las líneas y el pie del documento.
            fIdDoc = queIdDoc
            setCliente(factActualEnt.clienteId)
            fTipoDoc = TIPODOC_FACTURA
            fAlmacen = factActualEnt.almacen
            serie = factActualEnt.serie
            numero = factActualEnt.numero
            fEjercicio = factActualEnt.ejercicio
            fEmpresa = factActualEnt.empresa
            fFecha = factActualEnt.fecha
            fHora = factActualEnt.hora
            fObs1 = factActualEnt.observ1
            fObs2 = factActualEnt.observ2
            fIncidenciaDoc = factActualEnt.tipoIncidencia
            fTextoIncidencia = factActualEnt.textoIncidencia
            fPago = factActualEnt.fPago

            fDtoPie1 = if (factActualEnt.dto == "") 0.0 else factActualEnt.dto.replace(',', '.').toDouble()
            fDtoPie2 = if (factActualEnt.dto2 == "") 0.0 else factActualEnt.dto2.replace(',', '.').toDouble()
            fDtoPie3 = if (factActualEnt.dto3 == "") 0.0 else factActualEnt.dto3.replace(',', '.').toDouble()
            fDtoPie4 = if (factActualEnt.dto4 == "") 0.0 else factActualEnt.dto4.replace(',', '.').toDouble()

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
            fBases.cargarDesdeDoc(fIdDoc, fTipoDoc)
            fTotalAnterior = fBases.totalConImptos
        }
    }

    fun esContado(): Boolean {
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
        val fClteExento = !fClientes.fAplIva
        val queFlag = seriesDao?.getFlag(serie, fEjercicio) ?: 0
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
                    fAplicarRe = fClientes.fAplRec
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
        fBases.fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresa)
        fBases.fDecImpBase = fConfiguracion.decimalesImpBase()
        fBases.fDecImpII = fConfiguracion.decimalesImpII()
    }


    private fun serieNumeroValidos(): Boolean {
        return if (fTipoDoc == TIPODOC_FACTURA) {
            val queNumero = facturasDao?.getSerieNum(fAlmacen, serie, numero, fEjercicio) ?: 0
            (queNumero == 0)

        } else {
            val queNumero = cabeceraDao?.getSerieNum(fTipoDoc, fAlmacen, serie, numero, fEjercicio) ?: 0
            (queNumero == 0)
        }
    }

    private fun actualizarNumero() {
        when (fTipoDoc) {
            2.toShort() -> seriesDao?.setNumAlbaran(serie, fEjercicio, numero + 1)
            3.toShort() -> seriesDao?.setNumPedido(serie, fEjercicio, numero + 1)
            6.toShort() -> seriesDao?.setNumPresupuesto(serie, fEjercicio, numero + 1)
            else -> seriesDao?.setNumFactura(serie, fEjercicio, numero + 1)
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


    fun cargarLinea(fLinea: Int): Boolean {
        var fEncontrada = false
        val sPorcIva: String
        var datosLinVta = DatosLinVtas()

        for (linea in lLineas) {
            if (fLinea == linea.lineaId) {
                fEncontrada = true
                datosLinVta = linea
                break
            }
        }

        return if (fEncontrada) {
            sPorcIva = datosLinVta.porcIva?.replace(',', '.') ?: "0.0"
            fArticulo = datosLinVta.articuloId
            fCodArt = datosLinVta.codArticulo
            fDescr = datosLinVta.descripcion
            fTarifaLin = datosLinVta.tarifaId
            fPorcIva = sPorcIva.toDouble()
            fPrecio = datosLinVta.precio.toDouble()
            if (datosLinVta.precioII != "") fPrecioII = datosLinVta.precioII.toDouble() else calculaPrecioII()
            fPrecioTarifa = if (datosLinVta.precioTarifa != "") datosLinVta.precioTarifa.toDouble() else fPrecio
            fCodigoIva = datosLinVta.codigoIva
            fCajas = datosLinVta.cajas.toDouble()
            fPiezas = datosLinVta.piezas.toDouble()
            fOldCajas = fCajas
            fCantidad = datosLinVta.cantidad.toDouble()
            fOldCantidad = fCantidad
            fImporte = datosLinVta.importe.toDouble()
            fDtoLin = datosLinVta.dto.toDouble()
            fDtoLinTarifa = if (datosLinVta.dtoTarifa != "") datosLinVta.dtoTarifa.toDouble() else fDtoLin
            fDtoImp =  if (datosLinVta.dtoImpte != "") datosLinVta.dtoImpte.toDouble() else 0.0
            fDtoImpII = 0.0 // Por ahora lo dejamos así, tiene que tener algún valor porque lo usamos en algunas funciones.
            // Vemos si la línea es sin cargo.
            val queFlag = datosLinVta.flag
            fArtEnOferta = queFlag and FLAGLINEAVENTA_ARTICULO_EN_OFERTA > 0
            fArtSinCargo = queFlag and FLAGLINEAVENTA_SIN_CARGO > 0
            fPrecioRating = queFlag and FLAGLINEAVENTA_PRECIO_RATING > 0
            fHayCambPrecio = queFlag and FLAGLINEAVENTA_CAMBIAR_PRECIO > 0
            val queFlag3 = datosLinVta.flag3
            fLineaPorPiezas = queFlag3 and FLAG3LINEAVENTA_PRECIO_POR_PIEZAS > 0
            fCodIncidencia = datosLinVta.tipoIncId
            fFormatoLin = datosLinVta.formatoId
            fTextoLinea = datosLinVta.textoLinea
            fFlag5 = datosLinVta.flag5
            fAlmacPedido = datosLinVta.almacenPedido

            // Vemos si la línea tiene descuentos en cascada.
            val queLinea = if (fTipoDoc == TIPODOC_FACTURA) dtosLinFrasDao?.getLinea(fLinea) ?: 0
                            else dtosLineasDao?.getLinea(fLinea) ?: 0
            fLineaConDtCasc = (queLinea > 0)

            fLote = datosLinVta.lote
            fOldLote = fLote
            fTasa1 = if (datosLinVta.tasa1 != "") datosLinVta.tasa1.toDouble() else 0.0
            fTasa2 = if (datosLinVta.tasa2 != "") datosLinVta.tasa2.toDouble() else 0.0

            true
        } else false
    }

    fun grabarHistorico() {
        val fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresa)

        val lineasHco = tmpHcoDao?.getAllLineas() ?: emptyList<TmpHcoEnt>().toMutableList()
        for (linHco in lineasHco) {
            // Comprobamos que alguna de las cantidades sea distinta de cero.
            if (linHco.cajas.toDouble() != 0.0 || linHco.cantidad.toDouble() != 0.0 || linHco.piezas.toDouble() != 0.0) {
                inicializarLinea()
                fArticulo = linHco.articuloId
                fCodArt = linHco.codigo
                fDescr = linHco.descripcion
                fTarifaLin = fTarifaDoc
                fPrecio = linHco.precio.toDouble()
                fPrecioII = linHco.precioII.toDouble()
                fCodigoIva = linHco.codigoIva
                fCajas = linHco.cajas.toDouble()
                fCantidad = linHco.cantidad.toDouble()
                fPiezas = linHco.piezas.toDouble()
                fDtoLin = linHco.dto.toDouble()
                fDtoImp = linHco.dtoImpte.toDouble()
                fDtoImpII = linHco.dtoImpteII.toDouble()
                fTasa1 = linHco.tasa1.toDouble()
                fTasa2 = linHco.tasa2.toDouble()
                fFormatoLin = linHco.formatoId
                fTextoLinea = linHco.textoLinea
                fLote = linHco.lote
                // Por ahora entenderemos que el precio y el dto. que vienen del hco. van a ser el precio y dto. de tarifa,
                // por si luego modificamos la línea
                fPrecioTarifa = fPrecio
                fDtoLinTarifa = fDtoLin

                fAlmacPedido = linHco.almacenPedido.toString()
                fCodIncidencia = linHco.incidenciaId
                // Tenemos que calcular fLineaPorPiezas antes de llamar a calcularImpte(), ya que fLineaPorPiezas interviene en esta función
                val queFlag3 = linHco.flag3
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
                val queFlag = linHco.flag
                fArtEnOferta = queFlag and FLAGLINEAVENTA_ARTICULO_EN_OFERTA > 0
                fArtSinCargo = queFlag and FLAGLINEAVENTA_SIN_CARGO > 0
                fPrecioRating = queFlag and FLAGLINEAVENTA_PRECIO_RATING > 0
                fHayCambPrecio = queFlag and FLAGLINEAVENTA_CAMBIAR_PRECIO > 0
                fFlag5 = linHco.flag5

                insertarLinea()
            }
        }
        // Borro el histórico.
        tmpHcoDao?.vaciar()

        refrescarLineas()
    }

    private fun anyadirDtoCascada() {

        val dtoLineaEnt = DtosLineasEnt()
        dtoLineaEnt.lineaId = -1
        dtoLineaEnt.orden = 1
        dtoLineaEnt.descuento = "0.0"
        dtoLineaEnt.importe = fDtoImp.toString()
        dtoLineaEnt.cantidad1 = "0.0"
        dtoLineaEnt.cantidad2 = "0.0"
        dtoLineaEnt.desdeRating = "T"

        insertarDtoCasc(dtoLineaEnt)

        fDtosCascada.abrir(-1)
        // Configuramos el objeto de los dtos. en cascada
        fDtosCascada.fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresa)
        fDtosCascada.fAplicarIva = fClientes.fAplIva
        fDtosCascada.fPorcIva = fPorcIva
        fDtosCascada.fDecPrBase = fConfiguracion.decimalesPrecioBase()
        fDtoLin = fDtosCascada.calcularDtoEquiv(fPrecio, fDecPrBase).toDouble()
        fLineaConDtCasc = true
    }

    fun insertarLinea() {
        //val lineaEnt = LineasEnt()
        val lineaEnt = DatosLinVtas()

        lineaEnt.cabeceraId = fIdDoc
        lineaEnt.articuloId = fArticulo
        lineaEnt.codArticulo = fCodArt
        lineaEnt.descripcion = fDescr
        lineaEnt.tarifaId = fTarifaLin
        lineaEnt.precio = fPrecio.toString()
        lineaEnt.precioII = fPrecioII.toString()
        lineaEnt.codigoIva = fCodigoIva
        lineaEnt.cajas = fCajas.toString()
        lineaEnt.cantidad = fCantidad.toString()
        lineaEnt.piezas = fPiezas.toString()
        lineaEnt.formatoId = fFormatoLin
        lineaEnt.importe = fImporte.toString()
        lineaEnt.importeII = fImpteII.toString()
        lineaEnt.dto = fDtoLin.toString()
        lineaEnt.dtoImpte = fDtoImp.toString()
        lineaEnt.dtoImpteII = fDtoImpII.toString()
        lineaEnt.lote = fLote
        lineaEnt.flag5 = fFlag5
        lineaEnt.tasa1 = fTasa1.toString()
        lineaEnt.tasa2 = fTasa2.toString()
        lineaEnt.tipoIncId = fCodIncidencia
        lineaEnt.textoLinea = fTextoLinea
        lineaEnt.modifNueva = "T"
        lineaEnt.precioTarifa = fPrecioTarifa.toString() // Nos servirán para saber si modificamos el precio de tarifa y, en este caso,
        lineaEnt.dtoTarifa = fDtoLinTarifa.toString()    // no aplicar oferta por volumen.
        lineaEnt.almacenPedido = fAlmacPedido
        if (fLineaEsEnlace) lineaEnt.esEnlace = "T" else lineaEnt.esEnlace = "F"

        var queFlag = 0
        if (fArtSinCargo) queFlag = queFlag or FLAGLINEAVENTA_SIN_CARGO
        if (fPrecioRating) queFlag = queFlag or FLAGLINEAVENTA_PRECIO_RATING
        if (fHayCambPrecio) queFlag = queFlag or FLAGLINEAVENTA_CAMBIAR_PRECIO
        if (fArtEnOferta && !fPrecioRating && !fHayCambPrecio)
            queFlag = queFlag or FLAGLINEAVENTA_ARTICULO_EN_OFERTA
        // Si el artículo está en oferta pero vamos a aplicar el precio por rating lo marcamos como posible oferta,
        // siempre que no hayamos cambiado el precio
        if (fArtEnOferta && fPrecioRating && !fHayCambPrecio)
            queFlag = queFlag or FLAGLINEAVENTA_POSIBLE_OFERTA
        var queFlag3 = 0
        if (fLineaPorPiezas) {
            queFlag3 = queFlag3 or FLAG3LINEAVENTA_PRECIO_POR_PIEZAS
            queFlag3 = queFlag3 or FLAG3LINEAVENTA_ARTICULO_POR_PIEZAS
            queFlag = queFlag or FLAGLINEAVENTA_CAMBIAR_TARIFA_PRECIO
            queFlag = queFlag or FLAGLINEAVENTA_CAMBIAR_DESCRIPCION
        }

        lineaEnt.flag = queFlag
        lineaEnt.flag3 =  queFlag3

        val fIdLinea = if (fTipoDoc == TIPODOC_FACTURA) {
            lineasFrasDao?.insertar(datosLinVt2LinFra(lineaEnt))?.toInt() ?: -1
        }
        else {
            lineasDao?.insertar(datosLinVt2LinVta(lineaEnt))?.toInt() ?: -1
        }

        // Si la línea tiene descuentos en cascada lo que hacemos es reemplazar en la tabla "desctoslineas"
        // el campo linea, que estará a -1, por el id de la línea que acabamos de insertar.
        if (fLineaConDtCasc && fIdLinea > -1) {
            asignarLineaADtos(fIdLinea)
        }

        // Actualizamos el stock del artículo, si el almacen <> 0.
        if (fControlarStock && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN))
            fArticulos.actualizarStock(fArticulo, fEmpresa, fCantidad, fCajas, false)
        // Actualizamos el stock del lote. Aunque el lote esté en blanco lo llevaremos también a la tabla de lotes, porque me viene
        // bien para el momento de realizar el fin de día de las cargas.
        if (fUsarTrazabilidad && fLote != "" && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN))
            fLotes.actStockLote(fArticulo, fCantidad, fLote, fEmpresa)
        if (fBases.fIvaIncluido) fBases.calcularBase(fCodigoIva, fImpteII)
        else fBases.calcularBase(fCodigoIva, fImporte)

        abrirLineas()
    }

    private fun datosLinVt2LinVta(lineaEnt: DatosLinVtas): LineasEnt {
        val queLinea = LineasEnt()
        queLinea.lineaId = lineaEnt.lineaId
        queLinea.cabeceraId = lineaEnt.cabeceraId
        queLinea.articuloId = lineaEnt.articuloId
        queLinea.codArticulo = lineaEnt.codArticulo
        queLinea.descripcion = lineaEnt.descripcion
        queLinea.tarifaId = lineaEnt.tarifaId
        queLinea.precio = lineaEnt.precio
        queLinea.precioII = lineaEnt.precioII
        queLinea.codigoIva = lineaEnt.codigoIva
        queLinea.cajas = lineaEnt.cajas
        queLinea.cajasOrg = lineaEnt.cajasOrg
        queLinea.cantidad = lineaEnt.cantidad
        queLinea.cantidadOrg = lineaEnt.cantidadOrg
        queLinea.piezas = lineaEnt.piezas
        queLinea.piezasOrg = lineaEnt.piezasOrg
        queLinea.formatoId = lineaEnt.formatoId
        queLinea.importe = lineaEnt.importe
        queLinea.importeII = lineaEnt.importeII
        queLinea.dto = lineaEnt.dto
        queLinea.dtoImpte = lineaEnt.dtoImpte
        queLinea.dtoImpteII = lineaEnt.dtoImpteII
        queLinea.lote = lineaEnt.lote
        queLinea.flag = lineaEnt.flag
        queLinea.flag3 = lineaEnt.flag3
        queLinea.flag5 = lineaEnt.flag5
        queLinea.tasa1 = lineaEnt.tasa1
        queLinea.tasa2 = lineaEnt.tasa2
        queLinea.tipoIncId = lineaEnt.tipoIncId
        queLinea.textoLinea = lineaEnt.textoLinea
        queLinea.modifNueva = lineaEnt.modifNueva
        queLinea.precioTarifa = lineaEnt.precioTarifa
        queLinea.dtoTarifa = lineaEnt.dtoTarifa
        queLinea.almacenPedido = lineaEnt.almacenPedido
        queLinea.ofertaId = lineaEnt.ofertaId
        queLinea.dtoOftVol = lineaEnt.dtoOftVol
        queLinea.esEnlace = lineaEnt.esEnlace

        return queLinea
    }

    private fun datosLinVt2LinFra(lineaEnt: DatosLinVtas): LineasFrasEnt {
        val queLinea = LineasFrasEnt()
        queLinea.lineaId = lineaEnt.lineaId
        queLinea.facturaId = lineaEnt.cabeceraId
        queLinea.articuloId = lineaEnt.articuloId
        queLinea.codArticulo = lineaEnt.codArticulo
        queLinea.descripcion = lineaEnt.descripcion
        queLinea.tarifaId = lineaEnt.tarifaId
        queLinea.precio = lineaEnt.precio
        queLinea.precioII = lineaEnt.precioII
        queLinea.codigoIva = lineaEnt.codigoIva
        queLinea.cajas = lineaEnt.cajas
        queLinea.cajasOrg = lineaEnt.cajasOrg
        queLinea.cantidad = lineaEnt.cantidad
        queLinea.cantidadOrg = lineaEnt.cantidadOrg
        queLinea.piezas = lineaEnt.piezas
        queLinea.piezasOrg = lineaEnt.piezasOrg
        queLinea.formatoId = lineaEnt.formatoId
        queLinea.importe = lineaEnt.importe
        queLinea.importeII = lineaEnt.importeII
        queLinea.dto = lineaEnt.dto
        queLinea.dtoImpte = lineaEnt.dtoImpte
        queLinea.dtoImpteII = lineaEnt.dtoImpteII
        queLinea.lote = lineaEnt.lote
        queLinea.flag = lineaEnt.flag
        queLinea.flag3 = lineaEnt.flag3
        queLinea.flag5 = lineaEnt.flag5
        queLinea.tasa1 = lineaEnt.tasa1
        queLinea.tasa2 = lineaEnt.tasa2
        queLinea.tipoIncId = lineaEnt.tipoIncId
        queLinea.textoLinea = lineaEnt.textoLinea
        queLinea.modifNueva = lineaEnt.modifNueva
        queLinea.precioTarifa = lineaEnt.precioTarifa
        queLinea.dtoTarifa = lineaEnt.dtoTarifa
        queLinea.almacenPedido = lineaEnt.almacenPedido
        queLinea.ofertaId = lineaEnt.ofertaId
        queLinea.dtoOftVol = lineaEnt.dtoOftVol
        queLinea.esEnlace = lineaEnt.esEnlace

        return queLinea
    }

    // Vemos si el artículo tiene texto habitual
    fun textoArtHabitual(): String {
        return artHabitDao?.getTexto(fArticulo, fCliente, fFormatoLin) ?: ""
    }


    fun getDescrFormato(queCodigo: Int): String {

        return if (fFormatoLin.toInt() != 0) {
            return formatosDao?.getDescripcion(queCodigo) ?: ""
        } else ""
    }

    private fun asignarLineaADtos(fIdLinea: Int) {
        if (fTipoDoc == TIPODOC_FACTURA) dtosLinFrasDao?.asignarLinea(fIdLinea)
        else dtosLineasDao?.asignarLinea(fIdLinea)
    }


    fun insertarDtoCasc(dtoLineaEnt: DtosLineasEnt) {
        dtosLineasDao?.insertar(dtoLineaEnt)
    }

    fun insertarDtoCascFras(dtoLineaEnt: DtosLinFrasEnt) {
        dtosLinFrasDao?.insertar(dtoLineaEnt)
    }


    fun editarDtoCasc(dtoId: Int, dtoLineaEnt: DtosLineasEnt) {
        dtosLineasDao?.actualizar(dtoId, dtoLineaEnt.descuento, dtoLineaEnt.importe, dtoLineaEnt.cantidad1, dtoLineaEnt.cantidad2)
    }

    fun borrarDtosCasc(fIdLinea: Long) {
        if (fTipoDoc == TIPODOC_FACTURA) dtosLinFrasDao?.borrarLinea(fIdLinea.toInt())
        else dtosLineasDao?.borrarLinea(fIdLinea.toInt())
    }

    fun editarLinea(fLinea: Int) {
        val datosLinVta = if (fTipoDoc == TIPODOC_FACTURA) lineasFrasDao?.getLinea(fLinea) ?: DatosLinVtas()
            else lineasDao?.getLinea(fLinea) ?: DatosLinVtas()

        val fOldImpte = datosLinVta.importe.toDouble()
        var fOldImpteII: Double
        if (datosLinVta.importeII != "")
            fOldImpteII = datosLinVta.importeII.toDouble()
        else {
            if (!fAplicarIva) fOldImpteII = fOldImpte
            else {
                fOldImpteII = fOldImpte + ((fOldImpte * fPorcIva) / 100)
                fOldImpteII = redondear(fOldImpteII, fDecImpII)
            }
        }

        val lineaEnt = DatosLinVtas()
        lineaEnt.lineaId = fLinea
        lineaEnt.cabeceraId = datosLinVta.cabeceraId
        lineaEnt.articuloId = datosLinVta.articuloId
        lineaEnt.codArticulo = datosLinVta.codArticulo
        lineaEnt.descripcion = datosLinVta.descripcion
        lineaEnt.tarifaId = datosLinVta.tarifaId
        lineaEnt.codigoIva = datosLinVta.codigoIva
        lineaEnt.cajasOrg = datosLinVta.cajasOrg
        lineaEnt.cantidadOrg = datosLinVta.cantidadOrg
        lineaEnt.piezasOrg = datosLinVta.piezasOrg
        lineaEnt.formatoId = datosLinVta.formatoId
        lineaEnt.precioTarifa = datosLinVta.precioTarifa
        lineaEnt.dtoTarifa = datosLinVta.dtoTarifa
        lineaEnt.ofertaId = datosLinVta.ofertaId
        lineaEnt.dtoOftVol = datosLinVta.dtoOftVol
        lineaEnt.esEnlace = datosLinVta.esEnlace

        lineaEnt.precio = fPrecio.toString()
        lineaEnt.precioII = fPrecioII.toString()
        lineaEnt.cajas = fCajas.toString()
        lineaEnt.piezas = fPiezas.toString()
        lineaEnt.cantidad = fCantidad.toString()
        lineaEnt.importe = fImporte.toString()
        lineaEnt.importeII = fImpteII.toString()
        lineaEnt.dto = fDtoLin.toString()
        lineaEnt.dtoImpte = fDtoImp.toString()
        lineaEnt.dtoImpteII = fDtoImpII.toString()
        lineaEnt.lote = fLote
        lineaEnt.tasa1 = fTasa1.toString()
        lineaEnt.tasa2 = fTasa2.toString()
        lineaEnt.textoLinea = fTextoLinea
        lineaEnt.flag5 = fFlag5
        lineaEnt.almacenPedido = fAlmacPedido
        lineaEnt.modifNueva = "T"

        var queFlag = 0
        // Si la línea no tiene cargo guardamos el flag y el código de incidencia.
        if (fArtSinCargo) {
            queFlag = queFlag or FLAGLINEAVENTA_SIN_CARGO
            lineaEnt.tipoIncId = fCodIncidencia
        }
        else lineaEnt.tipoIncId = datosLinVta.tipoIncId

        if (fPrecioRating) queFlag = queFlag or FLAGLINEAVENTA_PRECIO_RATING
        if (fHayCambPrecio) queFlag = queFlag or FLAGLINEAVENTA_CAMBIAR_PRECIO
        if (fArtEnOferta && !fPrecioRating && !fHayCambPrecio)
            queFlag = queFlag or FLAGLINEAVENTA_ARTICULO_EN_OFERTA
        // Si el artículo está en oferta pero vamos a aplicar el precio por rating lo marcamos como posible oferta,
        // siempre que no hayamos cambiado el precio
        if (fArtEnOferta && fPrecioRating && !fHayCambPrecio)
            queFlag = queFlag or FLAGLINEAVENTA_POSIBLE_OFERTA

        var queFlag3 = 0
        if (fLineaPorPiezas) {
            queFlag3 = queFlag3 or FLAG3LINEAVENTA_PRECIO_POR_PIEZAS
            queFlag3 = queFlag3 or FLAG3LINEAVENTA_ARTICULO_POR_PIEZAS
            queFlag = queFlag or FLAGLINEAVENTA_CAMBIAR_TARIFA_PRECIO
            queFlag = queFlag or FLAGLINEAVENTA_CAMBIAR_DESCRIPCION
        }
        lineaEnt.flag = queFlag
        lineaEnt.flag3 = queFlag3

        if (fTipoDoc == TIPODOC_FACTURA) lineasFrasDao?.actualizar(datosLinVt2LinFra(lineaEnt))
        else lineasDao?.actualizar(datosLinVt2LinVta(lineaEnt))

        // Actualizamos el stock del artículo
        if (fControlarStock && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN))
            fArticulos.actualizarStock(fArticulo, fEmpresa, fCantidad - fOldCantidad, fCajas - fOldCajas,false)

        // Actualizamos el stock del lote.
        if (fUsarTrazabilidad && fLote != "" && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN))
            fLotes.actStockLote(fArticulo, fCantidad, fLote, fEmpresa)

        if (fUsarTrazabilidad && fOldLote != "" && (fTipoDoc == TIPODOC_FACTURA || fTipoDoc == TIPODOC_ALBARAN))
            fLotes.actStockLote(fArticulo, -fOldCantidad, fOldLote, fEmpresa)

        if (fBases.fIvaIncluido) fBases.calcularBase(fCodigoIva, -fOldImpteII)
        else fBases.calcularBase(fCodigoIva, -fOldImpte)

        if (fBases.fIvaIncluido) fBases.calcularBase(fCodigoIva, fImpteII)
        else fBases.calcularBase(fCodigoIva, fImporte)

        refrescarLineas()
    }


    fun terminarDoc(docNuevo: Boolean, queEstado: String) {

        if (fTipoDoc == TIPODOC_FACTURA) {
            val facturaEnt = tomarDatosFra(docNuevo, queEstado)

            if (docNuevo) {
                    fIdDoc = facturasDao?.insertar(facturaEnt)?.toInt() ?: -1

                    // En las líneas del nuevo documento hemos ido guardando cabeceraId a -1 y ahora lo actualizamos
                    lineasFrasDao?.actualizarCabId(fIdDoc)

                // Actualizamos el contador.
                actualizarNumero()

            } else {
                facturasDao?.actualizar(facturaEnt)
            }
        }
        else
        {
            val cabeceraEnt = tomarDatosCabecera(docNuevo, queEstado)

            if (docNuevo) {
                    fIdDoc = cabeceraDao?.insertar(cabeceraEnt)?.toInt() ?: -1

                    // En las líneas del nuevo documento hemos ido guardando cabeceraId a -1 y ahora lo actualizamos
                    lineasDao?.actualizarCabId(fIdDoc)

                // Actualizamos el contador.
                actualizarNumero()

                // Si estamos haciendo un pedido actualizaremos el pendiente del cliente.
                if (fTipoDoc == TIPODOC_PEDIDO) actualizarPendiente(true)

            } else {
                cabeceraDao?.actualizar(cabeceraEnt)

                // Si estamos haciendo un pedido actualizaremos el pendiente del cliente.
                if (fTipoDoc == TIPODOC_PEDIDO) actualizarPendiente(false)
            }
        }
    }


    private fun tomarDatosFra(docNuevo: Boolean, queEstado: String): FacturasEnt {
        val facturaEnt: FacturasEnt

        if (docNuevo) {
            facturaEnt = FacturasEnt()

            facturaEnt.almacen = fAlmacen
            facturaEnt.serie = serie
            facturaEnt.numero = numero
            facturaEnt.ejercicio = fEjercicio
            facturaEnt.empresa = fEmpresa
            facturaEnt.fecha = fFecha
            facturaEnt.hora = fHora
            facturaEnt.clienteId = fCliente
            facturaEnt.ruta = fClientes.fRuta
        } else {
            facturaEnt = factActualEnt
        }
        facturaEnt.aplicarIva = logicoACadena(fAplicarIva)
        facturaEnt.aplicarRe = logicoACadena(fAplicarRe)
        facturaEnt.dto = fDtoPie1.toString()
        facturaEnt.dto2 = fDtoPie2.toString()
        facturaEnt.dto3 = fDtoPie3.toString()
        facturaEnt.dto4 = fDtoPie4.toString()
        facturaEnt.fPago = fPago
        facturaEnt.bruto = fBases.totalBruto.toString()
        facturaEnt.base = fBases.totalBases.toString()
        facturaEnt.iva = fBases.totalIva.toString()
        facturaEnt.recargo = fBases.totalRe.toString()
        facturaEnt.total = fBases.totalConImptos.toString()
        if (queEstado == "") facturaEnt.estado = "N"
        else facturaEnt.estado = queEstado

        // Por ahora el flag "AplicarIvaCliente" no se usa, ya que no viene en la configuración.
        if (fConfiguracion.ivaIncluido(fEmpresa))
            facturaEnt.flag = FLAGCABECERAVENTA_PRECIOS_IVA_INCLUIDO
        else
            facturaEnt.flag = 0

        facturaEnt.observ1 = fObs1
        facturaEnt.observ2 = fObs2
        facturaEnt.tipoIncidencia = fIncidenciaDoc
        facturaEnt.textoIncidencia = fTextoIncidencia
        // Dirección para el pedido
        facturaEnt.almDireccion = fAlmDireccion
        facturaEnt.ordenDireccion = fOrdenDireccion

        // Si el documento es un pedido de Bionat grabamos si hemos aplicado las ofertas o no. Para ello aprovechamos
        // el campo 'Hoja', ya que Bionat no lo utiliza
        if (docNuevo && fTipoDoc == TIPODOC_PEDIDO && fConfiguracion.codigoProducto() == "UY6JK-6KAYw-PO0Py-6OX9B-OJOPY") {
            if (fAplOftEnPed)
                facturaEnt.hojaReparto = 1
            else
                facturaEnt.hojaReparto = 0
        }

        return facturaEnt
    }


    private fun tomarDatosCabecera(docNuevo: Boolean, queEstado: String): CabecerasEnt {
        val cabeceraEnt: CabecerasEnt

        if (docNuevo) {
            cabeceraEnt = CabecerasEnt()

            cabeceraEnt.tipoDoc = fTipoDoc
            cabeceraEnt.tipoPedido = fTipoPedido
            // Por ahora el campo Facturado irá a falso.
            cabeceraEnt.facturado = "F"
            cabeceraEnt.almacen = fAlmacen
            cabeceraEnt.serie = serie
            cabeceraEnt.numero = numero
            cabeceraEnt.ejercicio = fEjercicio
            cabeceraEnt.empresa = fEmpresa
            cabeceraEnt.fecha = fFecha
            cabeceraEnt.hora = fHora
            cabeceraEnt.clienteId = fCliente
            cabeceraEnt.ruta = fClientes.fRuta
        } else {
            cabeceraEnt = cabActualEnt
        }
        cabeceraEnt.fechaEntrega = fFEntrega
        cabeceraEnt.aplicarIva = logicoACadena(fAplicarIva)
        cabeceraEnt.aplicarRe = logicoACadena(fAplicarRe)
        cabeceraEnt.dto = fDtoPie1.toString()
        cabeceraEnt.dto2 = fDtoPie2.toString()
        cabeceraEnt.dto3 = fDtoPie3.toString()
        cabeceraEnt.dto4 = fDtoPie4.toString()
        cabeceraEnt.fPago = fPago
        cabeceraEnt.bruto = fBases.totalBruto.toString()
        cabeceraEnt.base = fBases.totalBases.toString()
        cabeceraEnt.iva = fBases.totalIva.toString()
        cabeceraEnt.recargo = fBases.totalRe.toString()
        cabeceraEnt.total = fBases.totalConImptos.toString()
        if (queEstado == "") cabeceraEnt.estado = "N"
        else cabeceraEnt.estado = queEstado

        // Por ahora el flag "AplicarIvaCliente" no se usa, ya que no viene en la configuración.
        if (fConfiguracion.ivaIncluido(fEmpresa))
            cabeceraEnt.flag = FLAGCABECERAVENTA_PRECIOS_IVA_INCLUIDO
        else
            cabeceraEnt.flag = 0

        cabeceraEnt.observ1 = fObs1
        cabeceraEnt.observ2 = fObs2
        cabeceraEnt.tipoIncidencia = fIncidenciaDoc
        cabeceraEnt.textoIncidencia = fTextoIncidencia
        // Dirección para el pedido
        cabeceraEnt.almDireccion = fAlmDireccion
        cabeceraEnt.ordenDireccion = fOrdenDireccion

        // Si el documento es un pedido de Bionat grabamos si hemos aplicado las ofertas o no. Para ello aprovechamos
        // el campo 'Hoja', ya que Bionat no lo utiliza
        if (docNuevo && fTipoDoc == TIPODOC_PEDIDO && fConfiguracion.codigoProducto() == "UY6JK-6KAYw-PO0Py-6OX9B-OJOPY") {
            if (fAplOftEnPed)
                cabeceraEnt.hojaReparto = 1
            else
                cabeceraEnt.hojaReparto = 0
        }

        return cabeceraEnt
    }

    private fun actualizarPendiente(docNuevo: Boolean) {
        val fTotalDoc = fBases.totalConImptos
        if (docNuevo)
            fClientes.actualizarPendiente(fCliente, fTotalDoc)
        else
            fClientes.actualizarPendiente(fCliente, fTotalDoc - fTotalAnterior)
    }

    fun calcularDtosPie() {
        fBases.calcularDtosPie(fDtoPie1, fDtoPie2, fDtoPie3, fDtoPie4)
    }

    fun verOftVolumen() {
        val lOftVol = ArrayList<ListOftVol>()
        var queArticulo: Int
        var queTarifaLin: Short
        var oListOftVol: ListOftVol
        var indice: Int
        var linConCambPrecio: Boolean
        var linConRating: Boolean

        for (linea in lLineas) {
            val queFlag = linea.flag
            linConCambPrecio = queFlag and FLAGLINEAVENTA_CAMBIAR_PRECIO > 0
            linConRating = queFlag and FLAGLINEAVENTA_PRECIO_RATING > 0
            // Las líneas con cambio de precio no contabilizan para las ofertas por volumen. Tampoco las que tengan precio por rating.
            if (!linConCambPrecio && !linConRating) {
                queArticulo = linea.articuloId
                queTarifaLin = linea.tarifaId

                // Buscamos si el artículo tiene oferta por volumen e insertamos en la lista.
                val lista: List<ListOftVol> = ofertasDao?.getOftVolArt(queArticulo, fEmpresa, queTarifaLin,
                    fechaEnJulian(fFecha)) ?: emptyList()

                for (oferta in lista) {
                    oListOftVol = ListOftVol()
                    oListOftVol.idOferta = oferta.idOferta
                    oListOftVol.articuloDesct = oferta.articuloDesct
                    oListOftVol.tarifa = oferta.tarifa
                    oListOftVol.importe = linea.importe.toDouble()
                    indice = localizaId(lOftVol, oListOftVol.idOferta)
                    if (indice > -1) lOftVol[indice].importe =
                        lOftVol[indice].importe + oListOftVol.importe else lOftVol.add(oListOftVol)
                }
            }
        }
        // Añadimos las ofertas
        if (lOftVol.isNotEmpty()) anyadirOftVol(lOftVol)
    }


    private fun anyadirOftVol(lista: ArrayList<ListOftVol>) {
        var dDto: Double
        for (oftVol in lista) {
            // Tendremos que averiguar si el importe acumulado para cada oferta está entre algún rango
            val queDescto: String = ofVolRangosDao?.getDescuento(oftVol.idOferta, oftVol.importe) ?: "0.0"
            if (queDescto != "") {
                dDto = queDescto.replace(',', '.').toDouble()

                val articEnt = articulosDao?.existeArticulo(oftVol.articuloDesct) ?: DatosArticulo()
                if (articEnt.articuloId > 0) {
                    insertarLineaOftVol(dDto, oftVol)
                    // Marcamos las líneas del documento que pertenezcan a esta oferta con el flag 4096 (linea con oferta),
                    // siempre que no tengan cambio de precio
                    marcarLinComoOfta(oftVol.idOferta)
                } else MsjAlerta(fContexto).alerta("No se encontró el artículo para los descuentos de la oferta")

            } else  // Si no hemos llegado a completar la oferta marcamos las líneas como posible oferta.
                marcarLinComoPosibleOfta(oftVol.idOferta)
        }
    }

    private fun marcarLinComoPosibleOfta(queIdOfta: Int) {
        // Cargamos una lista con todos los artículos de la oferta
        val lArtOfta: List<Int> = ofertasDao?.getAllArtOftaId(queIdOfta) ?: emptyList()
        for (queArticulo in lArtOfta) {
            // Buscamos el artículo en concreto dentro del documento, excluyendo las lineas con cambio de precio
            val lLineasArt = lineasDao?.getArticNoCambPr(queArticulo, fIdDoc) ?: emptyList<LineasEnt>().toMutableList()

            // Actualizamos las líneas con el flag 16384 (artículo en posible oferta)
            if (lLineasArt.count() > 0) {
                for (lineaEnt in lLineasArt) {
                    lineasDao?.marcarComoPosOfta(lineaEnt.lineaId)
                }
            }
        }
    }

    private fun marcarLinComoOfta(queIdOfta: Int) {
        // Cargamos una lista con todos los artículos de la oferta
        val lArtOfta: List<Int> = ofertasDao?.getAllArtOftaId(queIdOfta) ?: emptyList()
        for (queArticulo in lArtOfta) {
            // Buscamos el artículo en concreto dentro del documento, excluyendo las lineas con cambio de precio
            val lLineasArt = lineasDao?.getArticNoCambPr(queArticulo, fIdDoc) ?: emptyList<LineasEnt>().toMutableList()

            // Actualizamos la línea con el flag 16384 (posible oferta)
            if (lLineasArt.count() > 0) {
                for (lineaEnt in lLineasArt) {
                    lineasDao?.marcarComoPosOfta(lineaEnt.lineaId)
                }
            }
        }
    }


    private fun insertarLineaOftVol(dDto: Double, oftVol: ListOftVol) {
        val fFtoDecImpBase = fConfiguracion.formatoDecImptesBase()
        val lineaEnt = DatosLinVtas()
        lineaEnt.cabeceraId = fIdDoc
        lineaEnt.articuloId = oftVol.articuloDesct
        lineaEnt.tarifaId = oftVol.tarifa.toShort()

        //values.put("linea", siguienteLinea())
        val datArtDesctOftVol = articulosDao?.datosArtDesctOftVol(oftVol.articuloDesct) ?: DatosArtDesctOftVol()
        if (datArtDesctOftVol.codigo != "") {
            lineaEnt.codArticulo = datArtDesctOftVol.codigo
            lineaEnt.descripcion = "[DTO " + String.format(Locale.getDefault(), "%.2f", dDto) + "% sobre " +
                    String.format(fFtoDecImpBase, oftVol.importe) + " €] " +
                    datArtDesctOftVol.descripcion
        }

        fPrecio = oftVol.importe * dDto / 100
        fCodigoIva = datArtDesctOftVol.codigoIva
        fPorcIva = datArtDesctOftVol.porcIva.toDouble()
        fDtoImp = 0.0
        fImporte = fPrecio * -1

        calculaPrecioII()
        calcularDtoImpII()
        calcularImpteII(true)

        lineaEnt.precio = fPrecio.toString()
        lineaEnt.precioII = fPrecioII.toString()
        lineaEnt.codigoIva = fCodigoIva
        lineaEnt.cajas = "0.0"
        lineaEnt.piezas = "0.0"

        fCantidad = -1.0
        lineaEnt.cantidad = fCantidad.toString()
        lineaEnt.importe = fImporte.toString()
        lineaEnt.importeII = fImpteII.toString()
        lineaEnt.dto = "0.0"
        lineaEnt.dtoImpte = fDtoImp.toString()
        lineaEnt.dtoImpteII = "0.0"
        lineaEnt.lote = ""
        lineaEnt.flag = 16      // Cambiar descripción
        lineaEnt.flag3 = 128    // Ajuste por oferta
        lineaEnt.ofertaId = oftVol.idOferta
        lineaEnt.dtoOftVol = dDto.toString()

        if (fTipoDoc == TIPODOC_FACTURA) lineasFrasDao?.insertar(datosLinVt2LinFra(lineaEnt))
        else lineasDao?.insertar(datosLinVt2LinVta(lineaEnt))

        // Recalculamos las bases
        if (fBases.fIvaIncluido) fBases.calcularBase(fCodigoIva, fImpteII)
        else fBases.calcularBase(fCodigoIva, fImporte)
    }


    fun hayOftVolumen(): Boolean {
        val queLinea = lineasDao?.hayOftVolumen(fIdDoc) ?: 0

        return queLinea > 0
    }


    fun cargarListaOftVol(): List<DatosOftVol> {
        return lineasDao?.cargarOftVol(fIdDoc) ?: emptyList<DatosOftVol>().toMutableList()
    }

    private fun localizaId(lista: ArrayList<ListOftVol>, queId: Int): Int {
        var result = -1
        for (oftVol in lista) {
            if (oftVol.idOferta == queId) result = lista.indexOf(oftVol)
        }
        return result
    }

    fun borrarArticuloDeDoc(queArticulo: Int) {
        for (linea in lLineas) {
            if (linea.articuloId == queArticulo) {
                // He detectado que si no refresco no borra. No tenemos problemas al hacer
                // el break porque a esta función se llama una vez por cada formato, de forma que
                // si un artículo tiene varios formatos se llamará una vez por cada uno de ellos y,
                // al final, se borran todas las líneas de dicho artículo.
                borrarLinea(linea, true)
                break
            }
        }
    }


    private fun refrescarLineas() {
        abrirLineas()
    }

    /*
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
    */


    fun setCliente(queCliente: Int) {
        fCliente = queCliente
        // Abrimos el objeto fClientes para tener acceso a los datos del cliente del documento.
        fClientes.abrirUnCliente(queCliente)
        // Aplicaremos la tarifa según configuración: si tenemos configurado usar la
        // del cliente usaremos la de éste, si no, la que tengamos para ventas.
        fTarifaDoc = fConfiguracion.tarifaVentas()
        if (fConfiguracion.usarTarifaClte()) {
            if (fClientes.fTarifa > 0)
                fTarifaDoc = fClientes.fTarifa
        }
        // La tarifa de descuento será la que tenga el cliente en su ficha y, si no, la que esté en configuración.
        fTarifaDto = if (fClientes.fTrfDto > 0) fClientes.fTrfDto
        else fConfiguracion.tarifaDto()
        // Por ahora la tarifa de la línea será la del documento, a no ser que cambiemos luego.
        fTarifaLin = fTarifaDoc
    }

    fun nombreCliente(): String {
        return fClientes.fNombre
    }

    fun nombreComClte(): String {
        return fClientes.fNomComercial
    }

    fun marcarComoImprimido(queId: Int) {
        cabeceraDao?.marcarComoImprimido(queId)
    }

    fun marcarComoEntregado(queId: Int, queCliente: Int, queEmpresa: Int, refrescar: Boolean) {
        // Obtenemos la fecha y hora actuales, que son las que grabaremos como fecha y hora de la firma.
        val tim = System.currentTimeMillis()
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dfHora = SimpleDateFormat("HH:mm", Locale.getDefault())

        cabeceraDao?.marcarComoEntregado(queId, df.format(tim), dfHora.format(tim))

        if (refrescar) {
            // Refresco el cursor cerrándolo y volviéndolo a abrir.
            abrirTodos(queCliente, queEmpresa, 0)
        }
    }

    fun setTextoIncidencia(queId: Int, queTexto: String, queCliente: Int, queEmpresa: Int, queTipoIncid: Int) {

        cabeceraDao?.setTextoIncidencia(queId, queTipoIncid, queTexto)

        // Refresco el cursor cerrándolo y volviéndolo a abrir.
        abrirTodos(queCliente, queEmpresa, 0)
    }


    fun reenviar(dataActual: DatosVerDocs) {     //queId: Int, queCliente: Int, empresaActual: Int) {

        cabeceraDao?.reenviarDoc(dataActual.cabeceraId)

        if (dataActual.tipoDoc == TIPODOC_FACTURA) {
            // Si marcamos para reenviar una factura también tendremos que reenviar el pendiente.
            pendienteDao?.reenviar(dataActual.tipoDoc, dataActual.empresa, dataActual.almacen, dataActual.serie,
                               dataActual.numero, dataActual.ejercicio)
        }

        // Refresco el cursor cerrándolo y volviéndolo a abrir.
        abrirTodos(dataActual.clienteId, dataActual.empresa.toInt(), 0)
    }


    fun calculaPrecioYDto(pGrupo: Short, pDpto: Short, pProv: Int, porcIva: Double) {
        // El orden para obtener el precio y el dto. del artículo será el siguiente:
        // 1º- Del histórico, último precio de venta (si está configurado).
        // 2º- De la oferta o el rating.
        // 3º- De la tarifa.

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

        // Vemos el precio del histórico.
        if (fConfiguracion.pvpHistorico()) {
            tomaPrecioHco()
        }
        // Si tenemos configurado aplicar el precio más ventajoso obviamos la configuración del rating.
        if (fPrecio == 0.0 && fConfiguracion.aplicarPvpMasVent() && fClientes.getAplicarOfertas()) {
            if (fConfiguracion.usarRating()) {
                tomaPrecioRating(pGrupo, pDpto, pProv)
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
            fPrecio1 = if (fDtoRatingImp != 0.0) fPrRating - fDtoRatingImp else fPrRating - fPrRating * fDtoRating / 100
            fPrecio2 = fPrecioOfta - fPrecioOfta * fDtoOfta / 100
            if (fPrecio1 != 0.0) {
                if (fPrecio2 != 0.0) {
                    // Si tenemos valor en ambos precios calculamos el más ventajoso
                    if (fPrecio1 < fPrecio2) {
                        fPrecioRating = true
                        fPrecio = fPrRating
                        fArtEnOferta = false // Nos aseguramos de quitar el flag si no vamos a aplicar la oferta
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
                tomaPrecioRating(pGrupo, pDpto, pProv)
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
                tomaPrecioRating(pGrupo, pDpto, pProv)
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
            fPrecioII = redondear(fPrecioII, fDecPrII)
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
            redondear(fPrecioII / dIvaDiv, fDecPrII + 2)
        }
    }

    fun calcularImpte(desdeIvaIncl: Boolean): Boolean {
        if (desdeIvaIncl) {
            fImporte = if (!fAplicarIva) {
                fImpteII
            } else {
                val dIvaDiv = (100 + fPorcIva) / 100
                try {
                    redondear(fImpteII / dIvaDiv, fDecImpII + 1)
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
                redondear(fImporte, fDecImpBase)
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
                    redondear(fImpteII, fDecImpII)
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
                redondear(fImpteII, fDecImpII)
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
        fDtoImp = redondear(fDtoImpII / dIvaDiv, 2)
    }

    fun calcularDtoImpII() {
        fDtoImpII = fDtoImp + fDtoImp * fPorcIva / 100
        fDtoImpII = redondear(fDtoImpII, 2)
    }

    private fun tomaPrecioHco() {
        // Si estamos vendiendo con formatos tomaremos del histórico el precio del formato seleccionado
        val datosPrecios: DatosPrecios = if (fFormatoLin > 0) {
            historicoDao?.getPrecioFto(fCliente, fArticulo, fFormatoLin) ?: DatosPrecios()
        } else {
            historicoDao?.getPrecio(fCliente, fArticulo) ?: DatosPrecios()
        }

        if (datosPrecios.precio != "") {
            val sPrecio = datosPrecios.precio.replace(',', '.')
            val sDto = datosPrecios.dto.replace(',', '.')
            fPrecio = redondear(sPrecio.toDouble(), fDecPrBase)
            fDtoLin = redondear(sDto.toDouble(), 2)
        }
    }


    private fun tomaPrecioOferta() {
        val ofertaEnt: OfertasEnt = if (fFormatoLin > 0) {
            ofertasDao?.getOftaVtaFto(fArticulo, fEmpresa.toInt(), fTarifaLin, fFormatoLin, fechaEnJulian(fFecha)) ?: OfertasEnt()
        } else {
            ofertasDao?.getOftaVtaArt(fArticulo, fEmpresa.toInt(), fTarifaLin, fechaEnJulian(fFecha)) ?: OfertasEnt()
        }
        if (ofertaEnt.articuloId > 0) {
            val sPrecio = ofertaEnt.precio.replace(',', '.')
            val sDto = ofertaEnt.dto.replace(',', '.')
            val queTipoOfta = ofertaEnt.tipoOferta.toInt()
            if (queTipoOfta != 6) {
                fPrecio = redondear(sPrecio.toDouble(), fDecPrBase)
                fArtEnOferta = true
                if (fDtoLin == 0.0) fDtoLin = redondear(sDto.toDouble(), 2)

            // Comprobamos si hay alguna oferta de escalado de precios
            } else {
                var quePrecOfta: Double

                val lOftasCant = oftCantRangosDao?.getAllOftArt(fArticulo) ?: emptyList<OftCantRangosEnt>().toMutableList()

                var hayEscalado = false
                var desdeCantidad: Double
                var hastaCantidad: Double

                for (oftCant in lOftasCant) {
                    // Vemos si la cantidad está entre alguno de los escalados.
                    desdeCantidad = oftCant.desdeCantidad.toDouble()
                    hastaCantidad = oftCant.hastaCantidad.toDouble()
                    // ¿Por qué hago lo que viene a continuación? Para que funcione la oferta por escalado porque,
                    // tal y como están definidos éstos, podemos tener un primer escalado que sea desde 0 hasta X y
                    // si fCantidad es 0 no entraría en éste, ya que la condición es fCantidad > desdeCantidad.
                    // Resumiendo, lo hago únicamente para el caso en que exista un escalado que sea desde 0 hasta X.
                    if (desdeCantidad == 0.0 && hastaCantidad > 0.0) desdeCantidad = -0.0001
                    if (fCantidad > desdeCantidad && fCantidad <= hastaCantidad) {
                        hayEscalado = true
                        fArtEnOferta = true
                        // Si el precio de la oferta es 0 significa que aplicaremos el precio de tarifa
                        quePrecOfta = oftCant.precioBase.replace(',', '.').toDouble()
                        fPrecio = if (quePrecOfta > 0) quePrecOfta else 0.0
                    }
                    // Vemos si la cantidad está en el escalado infinito (si es que lo tenemos definido)
                    if (!hayEscalado) {
                        if (desdeCantidad == 0.0 && hastaCantidad == 0.0) {
                            quePrecOfta = oftCant.precioBase.replace(',', '.').toDouble()
                            if (quePrecOfta > 0) {
                                fPrecio = quePrecOfta
                                fArtEnOferta = true
                            }
                        }
                    }
                }
            }
        }
    }


    private fun tomaPrecioTarifa() {
        // Vemos si tenemos formato para la línea, en cuyo caso tomamos el precio de la tabla "trfformatos".
        var datosPrecios = if (fFormatoLin > 0) {
            trfFormatosDao?.getPrecioDto(fArticulo, fTarifaLin, fFormatoLin) ?: DatosPrecios()
        } else {
            tarifasDao?.getPrecioDto(fArticulo, fTarifaLin) ?: DatosPrecios()
        }

        if (datosPrecios.precio != "") {
            val sPrecio = datosPrecios.precio.replace(',', '.')
            val sDto = datosPrecios.dto.replace(',', '.')
            fPrecio = redondear(sPrecio.toDouble(), fDecPrBase)
            if (fDtoLin == 0.0) fDtoLin = redondear(sDto.toDouble(), 2)
        }

        if (fTarifaDto > 0 && fDtoLin == 0.0) {
            // Idem para los descuentos.
            datosPrecios = if (fFormatoLin > 0) {
                trfFormatosDao?.getPrecioDto(fArticulo, fTarifaDto, fFormatoLin) ?: DatosPrecios()
            } else {
                tarifasDao?.getPrecioDto(fArticulo, fTarifaDto) ?: DatosPrecios()
            }

            if (datosPrecios.dto != "") {
                val sDto = datosPrecios.dto.replace(',', '.')
                fDtoLin = redondear(sDto.toDouble(), 2)
            }
        }
    }

    private fun fechaEnJulian(queFecha: String): String {
        val queAnyo = queFecha.substring(6, 10)
        val queMes = queFecha.substring(3, 5)
        val queDia = queFecha.substring(0, 2)
        return "$queAnyo-$queMes-$queDia"
    }

    private fun tomaPrecioRating(queGrupo: Short, queDpto: Short, queProv: Int) {
        val ratingGrDao: RatingGruposDao? = getInstance(fContexto)?.ratingGruposDao()
        var quePrec = DatosPrecRat()

        var existe = false
        var formatoEncontrado = false

        // Rating por artículo. Si estamos vendiendo con formato, buscaremos primero el rating para ese formato.
        if (fFormatoLin > 0) {
            quePrec = ratingArtDao?.getPrecioFto(fArticulo, fAlmacen, fCliente,
                        fFormatoLin, fechaEnJulian(fFecha)) ?: DatosPrecRat()

            // Si no hemos encontrado el precio para el formato lo buscamos para el artículo y cliente.
            if (quePrec.precio != "") {
                formatoEncontrado = true
            }
        }
        if (!formatoEncontrado) {
            quePrec = ratingArtDao?.getPrecio(fArticulo, fAlmacen, fCliente, fechaEnJulian(fFecha)) ?: DatosPrecRat()
        }

        if (quePrec.precio != "" || quePrec.dto != "") {
            val sPrecio = quePrec.precio.replace(',', '.')
            val sDto = quePrec.dto.replace(',', '.')
            // Vemos si el descuento es por importe o por porcentaje
            val queFlag = quePrec.flag
            if (queFlag and FLAGRATING_DESCUENTOIMPORTE > 0) fDtoRatingImp = sDto.toDouble()
            else fDtoLin = redondear(sDto.toDouble(), 2)
            fPrecio = redondear(sPrecio.toDouble(), fDecPrBase)
            existe = true
        }

        // Rating por grupo.
        if (!existe && queGrupo > 0 && queDpto > 0) {
            var sDto = ratingGrDao?.getDescuento(queGrupo, queDpto, fAlmacen, fCliente, fechaEnJulian(fFecha)) ?: ""

            if (sDto != "") {
                sDto = sDto.replace(',', '.')
                fDtoLin = redondear(sDto.toDouble(), 2)
                existe = true
            }
        }

        // Rating por ramo.
        if (!existe && fClientes.fRamo > 0) {
            val iRamo = fClientes.fRamo
            if (iRamo > 0) {
                quePrec = ratingArtDao?.getPrecioRamo(fArticulo, fAlmacen, iRamo, fechaEnJulian(fFecha)) ?: DatosPrecRat()
                if (quePrec.precio != "" || quePrec.dto != "") {
                    val sPrecio = quePrec.precio.replace(',', '.')
                    val sDto = quePrec.dto.replace(',', '.')
                    // Vemos si el descuento es por importe o por porcentaje
                    val queFlag = quePrec.flag
                    if (queFlag and FLAGRATING_DESCUENTOIMPORTE > 0)
                        fDtoRatingImp = sDto.toDouble()
                    else fDtoLin = redondear(sDto.toDouble(), 2)
                    fPrecio = redondear(sPrecio.toDouble(), fDecPrBase)
                    existe = true
                }
            }
        }

        // Rating por grupo/tarifa
        if (!existe && queGrupo > 0 && queDpto > 0) {
            val iRamo = fClientes.fRamo
            if (iRamo > 0) {
                var sDto = ratingGrDao?.getDtoRamoTarifa(queGrupo, queDpto, fAlmacen, iRamo, fTarifaLin, fechaEnJulian(fFecha)) ?: ""

                if (sDto != "") {
                    sDto = sDto.replace(',', '.')
                    fDtoLin = redondear(sDto.toDouble(), 2)
                    existe = true
                }
            }
        }

        // Rating por clientes/proveedor
        if (!existe && queProv > 0) {
            var sDto = ratingProvDao?.getDtoClteProv(queProv, fAlmacen, fCliente, fechaEnJulian(fFecha)) ?: ""

            if (sDto != "") {
                sDto =  sDto.replace(',', '.')
                fDtoLin = redondear(sDto.toDouble(), 2)
                existe = true
            }
        }

        // Rating por ramo/tarifa/proveedor
        if (!existe && queProv > 0) {
            val iRamo = fClientes.fRamo
            if (iRamo > 0) {
                var sDto = ratingProvDao?.getDtoRamoTrfProv(queProv, fAlmacen, iRamo, fTarifaLin, fechaEnJulian(fFecha)) ?: ""

                if (sDto != "") {
                    sDto = sDto.replace(',', '.')
                    fDtoLin = redondear(sDto.toDouble(), 2)
                }
            }
        }
    }


    fun hayStockLote(): Boolean {
        val stockLote = fLotes.dimeStockLote(fArticulo, fLote)
        return stockLote - fCantidad >= 0
    }


    fun existeLineaArticulo(queArticulo: Int): Int {
        var iArticulo: Int
        var queLinea = 0

        for (linea in lLineas) {
            iArticulo = linea.articuloId
            if (iArticulo == queArticulo) {
                queLinea = linea.lineaId
                break
            }
        }
        return queLinea
    }

    fun artNumVecesEnDoc(queArticulo: Int): Int {
        var iArticulo: Int
        var numVeces = 0

        for (linea in lLineas) {
            iArticulo = linea.articuloId
            if (iArticulo == queArticulo) {
                numVeces++
            }
        }
        return numVeces
    }

    fun datosArtEnCatLineas(queArticulo: Int): Array<String> {
        val catLineasDao: CatalogoLineasDao? = getInstance(fContexto)?.catalogoLineasDao()
        val lDatosCat = catLineasDao?.getDatosArt(queArticulo) ?: emptyList<CatalogoLineasEnt>().toMutableList()

        val sCantCajas = arrayOf("F", "0", "0", "0.0", "0.0", "0.0", "")
        if (lDatosCat.isNotEmpty()) {
            if (lDatosCat[0].cajas == "") lDatosCat[0].cajas = "0.0"
            if (lDatosCat[0].cantidad == "") lDatosCat[0].cantidad = "0.0"

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


    fun existeArtYLote(queArticulo: Int, queLote: String): Boolean {
        var iArticulo: Int
        var sLote: String

        for (linea in lLineas) {
            iArticulo = linea.articuloId
            sLote = linea.lote

            if (iArticulo == queArticulo && sLote == queLote) {
                return true
            }
        }
        return false
    }

    fun poderAplTrfCajas() {
        // Buscamos si existe la tarifa que tenemos configurada como tarifa de cajas. Si es así podremos aplicar tarifa de cajas
        // en el documento, en caso contrario no.
        val queTrfCajas = fConfiguracion.tarifaCajas()

        val cnfTarifasDao: CnfTarifasDao? = getInstance(fContexto)?.cnfTarifasDao()
        val queCodigo = cnfTarifasDao?.existeCodigo(queTrfCajas) ?: 0

        fPuedoAplTrfCajas = (queCodigo > 0)
    }


    fun getTipoIncidencia(queIncidencia: Int): String {
        val tiposIncDao: TiposIncDao? = getInstance(fContexto)?.tiposIncDao()
        val tipoInc = tiposIncDao?.getIncidencia(queIncidencia) ?: TiposIncEnt()

        return if (tipoInc.tipoIncId > 0)
            ponerCeros(tipoInc.tipoIncId.toString(), ancho_cod_incidencia) + " " + tipoInc.descripcion
        else ""
    }


    fun getTextoIncidencia(queId: Int): String {
        return cabeceraDao?.getTextoIncidencia(queId) ?: ""
    }


    fun dimeCantCajasArticulo(queArticulo: Int): Array<String> {
        var iArticulo: Int
        val sCantCajas = arrayOf("0.0", "0.0")

        val lineasTmpHco = tmpHcoDao?.getAllLineas() ?: emptyList<TmpHcoEnt>().toMutableList()

        for (linTmpHco in lineasTmpHco) {
            iArticulo = linTmpHco.articuloId
            if (iArticulo == queArticulo) {
                sCantCajas[0] = linTmpHco.cantidad
                sCantCajas[1] = linTmpHco.cajas
                break
            }
        }

        return sCantCajas
    }

    fun hayArtHabituales(): Boolean {
        val queArticulo = artHabitDao?.hayArtHabituales() ?: 0
        return (queArticulo > 0)
    }

}