package es.albainformatica.albamobileandroid.historicos

import android.content.Context
import es.albainformatica.albamobileandroid.DatosArtHcArtClte
import es.albainformatica.albamobileandroid.DatosDocsHcArtClte
import es.albainformatica.albamobileandroid.DatosHistorico
import es.albainformatica.albamobileandroid.dao.HcoPorArticClteDao
import es.albainformatica.albamobileandroid.dao.HistoricoDao
import es.albainformatica.albamobileandroid.dao.TmpHcoDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.TmpHcoEnt

/**
 * Created by jabegines on 14/10/13.
 */
class Historico(contexto: Context)  {
    private var historicoDao: HistoricoDao? = MyDatabase.getInstance(contexto)?.historicoDao()
    private var tmpHcoDao: TmpHcoDao? = MyDatabase.getInstance(contexto)?.tmpHcoDao()
    private var hcoPorArticClteDao: HcoPorArticClteDao? = MyDatabase.getInstance(contexto)?.hcoPorArticClteDao()

    var fArticulo = 0
    var fCodigo: String = ""
    var fDescr: String = ""
    var fCajas: Double = 0.0
    var fCantidad: Double = 0.0
    var fPiezas: Double = 0.0
    private var fLineaPorPiezas = false
    var fPrecio: Double = 0.0
    var fPrecioII: Double = 0.0
    var fDtoLin: Double = 0.0
    var fDtoImp: Double = 0.0
    var fDtoImpII: Double = 0.0
    var fCodigoIva: Short = 0
    var fTasa1: Double = 0.0
    var fTasa2: Double = 0.0
    var fFormatoLin: Short = 0
    var fHayArtHabituales: Boolean = false
    var fTextoLinea: String = ""
    var fLote: String = ""
    var fFlag = 0
    var fFlag3 = 0
    var fFlag5 = 0
    var fAlmacPedido: String = "0"
    var fIncidencia = 0

    lateinit var lDatosHistorico: List<DatosHistorico>
    lateinit var lDatArtHcoArtClte: List<DatosArtHcArtClte>
    lateinit var lDatDocHcoArtClte: List<DatosDocsHcArtClte>



    fun abrirArtsHcoPorArtClte(queCliente: Int, queOrdenacion: Short) {
        lDatArtHcoArtClte = hcoPorArticClteDao?.abrirArtsHcoPorArtClte(queCliente, queOrdenacion) ?: emptyList<DatosArtHcArtClte>().toMutableList()
    }

    fun abrirDocsHcoPorArtClte(queArticulo: Int, queCliente: Int) {
        lDatDocHcoArtClte = hcoPorArticClteDao?.abrirDocsHcoPorArtClte(queArticulo, queCliente) ?: emptyList<DatosDocsHcArtClte>().toMutableList()
    }

    fun abrirConBusqueda(queCliente: Int, queEmpresa: Short, queBuscar: String) {
        lDatosHistorico = historicoDao?.abrirConBusqueda(queCliente, queEmpresa, "%$queBuscar%") ?: emptyList<DatosHistorico>().toMutableList()
    }

    fun abrir(queCliente: Int, queEmpresa: Short) {
        lDatosHistorico = historicoDao?.abrir(queCliente, queEmpresa) ?: emptyList<DatosHistorico>().toMutableList()
    }


    fun inicializarLinea() {
        fArticulo = 0
        fCodigo = ""
        fDescr = ""
        fCajas = 0.0
        fCantidad = 0.0
        fPiezas = 0.0
        fPrecio = 0.0
        fPrecioII = 0.0
        fDtoLin = 0.0
        fDtoImp = 0.0
        fDtoImpII = 0.0
        fTasa1 = 0.0
        fTasa2 = 0.0
        fFormatoLin = 0
        fLote = ""
        fFlag = 0
        fFlag3 = 0
        fFlag5 = 0
        fAlmacPedido = ""
        fIncidencia = 0
        fLineaPorPiezas = false
    }


    fun aceptarCambios(fLinea: Int) {
        var fInsertando = true
        var fTmpHcoId = 0

        val lTmpHco = tmpHcoDao?.getAllLineas() ?: emptyList<TmpHcoEnt>().toMutableList()
        if (lTmpHco.isNotEmpty()) {
            for (tmpHco in lTmpHco) {
                if (tmpHco.linea == fLinea) {
                    fInsertando = false
                    fTmpHcoId = tmpHco.tmpHcoId
                }
            }
        }

        val tmpHcoEnt = TmpHcoEnt()
        tmpHcoEnt.cajas = fCajas.toString()
        tmpHcoEnt.cantidad = fCantidad.toString()
        tmpHcoEnt.piezas = fPiezas.toString()
        tmpHcoEnt.precio = fPrecio.toString()
        tmpHcoEnt.precioII = fPrecioII.toString()
        tmpHcoEnt.dto = fDtoLin.toString()
        tmpHcoEnt.dtoImpte = fDtoImp.toString()
        tmpHcoEnt.dtoImpteII = fDtoImpII.toString()
        tmpHcoEnt.codigoIva = fCodigoIva
        tmpHcoEnt.tasa1 = fTasa1.toString()
        tmpHcoEnt.tasa2 = fTasa2.toString()
        tmpHcoEnt.lote = fLote
        if (fAlmacPedido != "") tmpHcoEnt.almacenPedido = fAlmacPedido.toShort()
        else tmpHcoEnt.almacenPedido = 0
        tmpHcoEnt.textoLinea = fTextoLinea
        tmpHcoEnt.flag = fFlag
        tmpHcoEnt.flag3 = fFlag3
        tmpHcoEnt.flag5 = fFlag5

        if (fInsertando) {
            tmpHcoEnt.linea = fLinea
            tmpHcoEnt.articuloId = fArticulo
            tmpHcoEnt.codigo = fCodigo
            tmpHcoEnt.descripcion = fDescr
            tmpHcoEnt.formatoId = fFormatoLin

            tmpHcoDao?.insertar(tmpHcoEnt)

        } else {
            tmpHcoDao?.actualizar(fTmpHcoId, tmpHcoEnt.cajas, tmpHcoEnt.cantidad, tmpHcoEnt.piezas,
                    tmpHcoEnt.precio, tmpHcoEnt.precioII, tmpHcoEnt.dto, tmpHcoEnt.dtoImpte, tmpHcoEnt.dtoImpteII,
                    tmpHcoEnt.codigoIva, tmpHcoEnt.tasa1, tmpHcoEnt.tasa2, tmpHcoEnt.lote, tmpHcoEnt.almacenPedido,
                    tmpHcoEnt.textoLinea, tmpHcoEnt.flag, tmpHcoEnt.flag3, tmpHcoEnt.flag5)
        }
    }

    fun aceptarDatosDevolucion(fLinea: Int) {
        var fInsertando = true

        val lTmpHco = tmpHcoDao?.getAllLineas() ?: emptyList<TmpHcoEnt>().toMutableList()

        for (tmpHco in lTmpHco) {
            if (tmpHco.linea == fLinea) fInsertando = false
        }

        val tmpHcoEnt = TmpHcoEnt()
        tmpHcoEnt.linea = fLinea
        tmpHcoEnt.articuloId = fArticulo
        tmpHcoEnt.codigo = fCodigo
        tmpHcoEnt.descripcion = fDescr
        tmpHcoEnt.cajas = fCajas.toString()
        tmpHcoEnt.cantidad = fCantidad.toString()
        tmpHcoEnt.piezas = fPiezas.toString()
        tmpHcoEnt.precio = fPrecio.toString()
        tmpHcoEnt.precioII = fPrecioII.toString()
        tmpHcoEnt.dto = fDtoLin.toString()
        tmpHcoEnt.dtoImpte = fDtoImp.toString()
        tmpHcoEnt.dtoImpteII = fDtoImpII.toString()
        tmpHcoEnt.codigoIva = fCodigoIva
        tmpHcoEnt.tasa1 = fTasa1.toString()
        tmpHcoEnt.tasa2 = fTasa2.toString()
        tmpHcoEnt.formatoId = fFormatoLin
        tmpHcoEnt.incidenciaId = fIncidencia

        if (fInsertando) {
            tmpHcoDao?.insertar(tmpHcoEnt)
        } else
            tmpHcoDao?.actualizar(fLinea, tmpHcoEnt.cajas, tmpHcoEnt.cantidad, tmpHcoEnt.piezas,
                tmpHcoEnt.precio, tmpHcoEnt.precioII, tmpHcoEnt.dto, tmpHcoEnt.dtoImpte, tmpHcoEnt.dtoImpteII,
                tmpHcoEnt.codigoIva, tmpHcoEnt.tasa1, tmpHcoEnt.tasa2, tmpHcoEnt.lote, tmpHcoEnt.almacenPedido,
                tmpHcoEnt.textoLinea, tmpHcoEnt.flag, tmpHcoEnt.flag3, tmpHcoEnt.flag5)
    }


    fun borrar() {
        tmpHcoDao?.vaciar()
    }



    // Buscamos si el artículo está en el temporal y según el resultado, insertamos o editamos.
    // Esta función es casi idéntica a la de arriba (aceptarCambios), la diferencia está en que
    // localizamos la línea mediante el artículo.
    fun aceptarCambiosArt(queArticulo: Int) {
        var fInsertando = true
        var queTmpHcoId = 0

        if (queArticulo > 0) {
            // Compruebo si el artículo ya existe en la tabla temporal.
            val lTmpHco = tmpHcoDao?.getAllLineas() ?: emptyList<TmpHcoEnt>().toMutableList()

            for (tmpHco in lTmpHco) {
                if (tmpHco.articuloId == queArticulo) {
                    fInsertando = false
                    queTmpHcoId = tmpHco.tmpHcoId
                }
            }

            val tmpHcoEnt = TmpHcoEnt()
            tmpHcoEnt.articuloId = fArticulo
            tmpHcoEnt.codigo = fCodigo
            tmpHcoEnt.descripcion = fDescr
            tmpHcoEnt.cajas = fCajas.toString()
            tmpHcoEnt.cantidad = fCantidad.toString()
            tmpHcoEnt.piezas = fPiezas.toString()
            tmpHcoEnt.precio = fPrecio.toString()
            tmpHcoEnt.precioII = fPrecioII.toString()
            tmpHcoEnt.dto = fDtoLin.toString()
            tmpHcoEnt.dtoImpte = fDtoImp.toString()
            tmpHcoEnt.dtoImpteII = fDtoImpII.toString()
            tmpHcoEnt.codigoIva = fCodigoIva
            tmpHcoEnt.tasa1 = fTasa1.toString()
            tmpHcoEnt.tasa2 = fTasa2.toString()
            tmpHcoEnt.formatoId = fFormatoLin
            tmpHcoEnt.flag = fFlag
            tmpHcoEnt.flag5 = fFlag

            if (fInsertando) {
                tmpHcoDao?.insertar(tmpHcoEnt)
            } else
                tmpHcoDao?.actualizar(queTmpHcoId, tmpHcoEnt.cajas, tmpHcoEnt.cantidad, tmpHcoEnt.piezas,
                    tmpHcoEnt.precio, tmpHcoEnt.precioII, tmpHcoEnt.dto, tmpHcoEnt.dtoImpte, tmpHcoEnt.dtoImpteII,
                    tmpHcoEnt.codigoIva, tmpHcoEnt.tasa1, tmpHcoEnt.tasa2, tmpHcoEnt.lote, tmpHcoEnt.almacenPedido,
                    tmpHcoEnt.textoLinea, tmpHcoEnt.flag, tmpHcoEnt.flag3, tmpHcoEnt.flag5)
        }
    }

}