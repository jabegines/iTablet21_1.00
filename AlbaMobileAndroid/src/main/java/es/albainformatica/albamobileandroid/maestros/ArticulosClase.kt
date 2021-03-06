package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.Comunicador.Companion.fConfiguracion
import es.albainformatica.albamobileandroid.dao.*
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.BusquedasEnt
import es.albainformatica.albamobileandroid.entity.FormatosEnt
import es.albainformatica.albamobileandroid.entity.HistoricoEnt
import es.albainformatica.albamobileandroid.entity.StockEnt


class ArticulosClase(val contexto: Context) {
    private val articulosDao: ArticulosDao? = MyDatabase.getInstance(contexto)?.articulosDao()
    private val ofertasDao: OfertasDao? = MyDatabase.getInstance(contexto)?.ofertasDao()
    private val articDatAdicDao: ArticDatAdicDao? = MyDatabase.getInstance(contexto)?.articDatAdicDao()
    private var formatosDao: FormatosDao? = MyDatabase.getInstance(contexto)?.formatosDao()
    private var historicoDao: HistoricoDao? = MyDatabase.getInstance(contexto)?.historicoDao()
    private var ftosLineasDao: FtosLineasDao? = MyDatabase.getInstance(contexto)?.ftosLineasDao()
    private var stockDao: StockDao? = MyDatabase.getInstance(contexto)?.stockDao()

    lateinit var lArticulos: List<Int>
    lateinit var lArtGridView: List<DatosGridView>
    lateinit var lDatAdic: List<String>

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




    // Por ahora, la diferencia entre abrirUnArticulo y existeArticulo es la empresa, que la tomamos
    // en cuenta en abrirUnArticulo para mostrar el stock del art??culo en dicha empresa
    fun abrirUnArticulo(queArticulo: Int, queEmpresa: Short): Boolean {

        val datosArticulo: DatosArticulo = if (fConfiguracion.sumarStockEmpresas()) {
            articulosDao?.abrirUnArtSum(queArticulo) ?: DatosArticulo()
        } else {
            articulosDao?.abrirUnArticulo(queArticulo, queEmpresa) ?: DatosArticulo()
        }

        return if (datosArticulo.articuloId > 0) {
            fArticulo = queArticulo
            fEmpresa = queEmpresa
            fCodigo = datosArticulo.codigo
            fCodBarras = datosArticulo.clave ?: ""
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
            entradas = if (datosArticulo.ent != null && datosArticulo.ent != "") datosArticulo.ent?.replace(',', '.')?.toDouble() ?: 0.0 else 0.0
            salidas = if (datosArticulo.sal != null && datosArticulo.sal != "") datosArticulo.sal?.replace(',', '.')?.toDouble() ?: 0.0 else 0.0

            return true
        }
        else false
    }


    fun existeArticulo(queArticulo: Int): Boolean {

        val datosArticulo = articulosDao?.existeArticulo(queArticulo) ?: DatosArticulo()

        return if (datosArticulo.articuloId > 0) {
            fArticulo = queArticulo
            fCodigo = datosArticulo.codigo
            fCodBarras = datosArticulo.clave ?: ""
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
            entradas = if (datosArticulo.ent != null && datosArticulo.ent != "")
                datosArticulo.ent?.replace(',', '.')?.toDouble() ?: 0.0 else 0.0
            salidas = if (datosArticulo.sal != null && datosArticulo.sal != "")
                datosArticulo.sal?.replace(',', '.')?.toDouble() ?: 0.0 else 0.0

            true
        }
        else false
    }



    // Cat??logos Bionat
    // =============================================================================
    fun abrirBioCatalogo(queCatalogo: Int, fOrdenacion: Int): Boolean {

        lArticulos = articulosDao?.abrirBioCatalogo(queCatalogo, fOrdenacion) ?: emptyList<Int>().toMutableList()
        return (lArticulos.isNotEmpty())
    }

    fun abrirBioDepartamento(queGrupo: Short, queDepartamento: Short, fOrdenacion: Int): Boolean {

        lArticulos = articulosDao?.abrirBioDepartamento(queGrupo, queDepartamento, fOrdenacion) ?: emptyList<Int>().toMutableList()
        return (lArticulos.isNotEmpty())
    }

    fun abrirBioHistorico(queCliente: Int, fOrdenacion: Int): Boolean {

        lArticulos = articulosDao?.abrirBioHistorico(queCliente, fOrdenacion) ?: emptyList<Int>().toMutableList()
        return (lArticulos.isNotEmpty())
    }


    fun bioBuscar(queBuscar: String): Boolean {

        lArticulos = articulosDao?.bioBuscar("%$queBuscar%") ?: emptyList<Int>().toMutableList()
        return (lArticulos.isNotEmpty())
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

    // Buscamos una cadena dentro de un cat??logo concreto.
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
        // Pondremos a cero el campo idHco porque cuando estemos vendiendo en modo hist??rico no queremos que aparezca el icono
        // indicando que el art??culo tiene hist??rico. Si ya estamos vendiendo desde el hist??rico no nos hace falta ver
        // dicho icono, ser??a muy redundante, porque aparecer??a en todos los art??culos.

        lArtGridView = articulosDao?.abrirHistoricoPGV(queTarifa, queCliente, queOrdenacion)
                            ?: emptyList<DatosGridView>().toMutableList()
    }


    // Buscamos una cadena dentro del hist??rico de un cliente concreto.
    fun abrirBusqEnHcoParaGridView(queBuscar: String, queCliente: Int, queOrdenacion: Short, queTarifa: Short) {
        val cadenaLike = "%$queBuscar%"

        lArtGridView = articulosDao?.abrirBusqEnHcoPGV(cadenaLike, queTarifa, queCliente,
                queOrdenacion) ?: emptyList<DatosGridView>().toMutableList()
    }


    fun abrirSoloOftasParaGrView(queTarifa: Short, queCliente: Int, queOrdenacion: Short) {
        // Aunque mostremos los precios en oferta, tomamos tambi??n los precios normales para mostrarlos tachados.

        lArtGridView = if (queCliente > 0)
            articulosDao?.abrirSoloOftasPGVConHco(queTarifa, queCliente, queOrdenacion)
                            ?: emptyList<DatosGridView>().toMutableList()
        else
            articulosDao?.abrirSoloOftasPGV(queTarifa, queOrdenacion) ?: emptyList<DatosGridView>().toMutableList()
    }



    fun datosAdicionales(queArticulo: Int): Boolean {
        lDatAdic = articDatAdicDao?.getDatosArticulo(queArticulo) ?: emptyList<String>().toMutableList()
        return lDatAdic.isNotEmpty()
    }



    fun articuloEnTablet(queArticulo: Int): Boolean {
        val queArticuloId = articulosDao?.getArticulo(queArticulo) ?: 0
        return (queArticuloId > 0)
    }


    fun existeCodigo(queCodigo: String): Boolean {
        val busqEnt = articulosDao?.existeCodigo(queCodigo) ?: BusquedasEnt()

        return if (busqEnt.articuloId > 0) {
            val queArticulo = busqEnt.articuloId
            val tipoCodigo = busqEnt.tipo
            // Si el c??digo es de cajas, asignamos las unidades por caja. Si no, las
            // asignaremos en existeArticulo().
            if (tipoCodigo == tipoBusq_Caja) {
                fCodBCajas = true
                fUCaja = busqEnt.ucaja.toDouble()
                fTarifaCajas = (busqEnt.tcaja == "T")
            } else {
                fCodBCajas = false
                fTarifaCajas = false
            }
            existeArticulo(queArticulo)

        } else {
            false
        }
    }


    fun getCosto(): Double {
        val costosDao: CostosArticulosDao? = MyDatabase.getInstance(contexto)?.costosArticulosDao()
        return costosDao?.getCostoArticulo(fArticulo, fEmpresa) ?: 0.0
    }


    fun tieneOferta(queArticulo: Int): Boolean {
        val queArticuloId = ofertasDao?.articuloEnOfta(queArticulo, fEmpresa) ?: 0
        return (queArticuloId > 0)
    }


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
    }


    fun cargarHcoArtClte(queArticulo: Int, queCliente: Int, listItems: MutableList<String>) {
        val hcoEnt = historicoDao?.cargarHcoArtClte(queArticulo, queCliente) ?: HistoricoEnt()

        if (hcoEnt.articuloId > 0) {
            listItems.add(hcoEnt.cajas)
            listItems.add(hcoEnt.cantidad)
            listItems.add(hcoEnt.precio)
            listItems.add(hcoEnt.dto)
            listItems.add(hcoEnt.fecha)
        }
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
        val iArticulo = stockDao?.existeArtYEmpresa(queArticulo, queEmpresa) ?: 0
        val bInsertar = iArticulo == 0

        // Vemos si el art??culo est?? en la tabla Stock
        if (deEntradas) {
            val sEntradas: String
            val sEntCajas: String
            val dEntradas: Double
            val dEntCajas: Double

            val datosStock = stockDao?.getEntrArtEmpr(queArticulo, queEmpresa) ?: DatosStock()

            if (datosStock.unidades != "" || datosStock.cajas != "") {

                sEntradas = if (datosStock.unidades != "") {
                    datosStock.unidades.replace(',', '.')
                } else {
                    "0.0"
                }
                sEntCajas = if (datosStock.cajas != "") {
                    datosStock.cajas.replace(',', '.')
                } else {
                    "0.0"
                }

                dEntradas = sEntradas.toDouble() + dCantidad
                dEntCajas = sEntCajas.toDouble() + dCajas
            } else {
                dEntradas = dCantidad
                dEntCajas = dCajas
            }

            val stockEnt = StockEnt()
            stockEnt.articuloId = queArticulo
            stockEnt.empresa = queEmpresa

            if (bInsertar) {
                stockEnt.sal = "0.0"
                stockEnt.salc = "0.0"
                stockEnt.salp = "0.0"
            }
            stockEnt.ent = dEntradas.toString()
            stockEnt.entc = dEntCajas.toString()
            stockEnt.entp = "0.0"

            if (bInsertar) stockDao?.insertar(stockEnt)
            else stockDao?.actualizar(stockEnt)

        } else {
            val sSalidas: String
            val sSalCajas: String
            val dSalidas: Double
            val dSalCajas: Double

            val datosStock = stockDao?.getSalArtEmpr(queArticulo, queEmpresa) ?: DatosStock()

            if (datosStock.unidades != "" || datosStock.cajas != "") {

                sSalidas = if (datosStock.unidades!= "") {
                    datosStock.unidades.replace(',', '.')
                } else {
                    "0.0"
                }
                sSalCajas = if (datosStock.cajas != "") {
                    datosStock.cajas.replace(',', '.')
                } else {
                    "0.0"
                }

                dSalidas = sSalidas.toDouble() + dCantidad
                dSalCajas = sSalCajas.toDouble() + dCajas
            } else {
                dSalidas = dCantidad
                dSalCajas = dCajas
            }

            val stockEnt = StockEnt()
            stockEnt.articuloId = queArticulo
            stockEnt.empresa = queEmpresa

            if (bInsertar) {
                stockEnt.ent = "0.0"
                stockEnt.entc = "0.0"
                stockEnt.entp = "0.0"
            }
            stockEnt.sal = dSalidas.toString()
            stockEnt.salc = dSalCajas.toString()
            stockEnt.salp = "0.0"

            if (bInsertar) stockDao?.insertar(stockEnt)
            else stockDao?.actualizar(stockEnt)
        }
    }


}