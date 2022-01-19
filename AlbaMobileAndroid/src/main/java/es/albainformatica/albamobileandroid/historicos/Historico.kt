package es.albainformatica.albamobileandroid.historicos

import android.content.Context
import es.albainformatica.albamobileandroid.DatosHcArtClte
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

    private var fCliente = 0
    private var fCadBusqueda = ""
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
    lateinit var lDatHcoArtClte: List<DatosHcArtClte>



    fun abrirHcoPorArtClte(queCliente: Int, queOrdenacion: Short) {
        lDatHcoArtClte = hcoPorArticClteDao?.abrirHcoPorArtClte(queCliente, queOrdenacion) ?: emptyList<DatosHcArtClte>().toMutableList()
    }

    fun abrirConBusqueda(queCliente: Int, queBuscar: String) {
        lDatosHistorico = historicoDao?.abrirConBusqueda(queCliente, "%$queBuscar%") ?: emptyList<DatosHistorico>().toMutableList()
    }

    fun abrir(queCliente: Int) {
        lDatosHistorico = historicoDao?.abrir(queCliente) ?: emptyList<DatosHistorico>().toMutableList()
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
        if (lTmpHco.count() > 0) {
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
        // TODO
        /*
        dbAlba.rawQuery("SELECT * FROM tmphco", null).use { cTmpHco ->
            cTmpHco.moveToFirst()
            while (!cTmpHco.isAfterLast) {
                if (cTmpHco.getInt(cTmpHco.getColumnIndex("linea") ?: 0) == fLinea) fInsertando = false
                cTmpHco.moveToNext()
            }
            val values = ContentValues()
            values.put("cajas", fCajas)
            values.put("cantidad", fCantidad)
            values.put("piezas", fPiezas)
            values.put("precio", fPrecio)
            values.put("precioii", fPrecioII)
            values.put("dto", fDtoLin)
            values.put("dtoi", fDtoImp)
            values.put("dtoiii", fDtoImpII)
            values.put("codigoiva", fCodigoIva)
            values.put("tasa1", fTasa1)
            values.put("tasa2", fTasa2)
            values.put("incidencia", fIncidencia)
            if (fInsertando) {
                values.put("linea", fLinea)
                values.put("articulo", fArticulo)
                values.put("codigo", fCodigo)
                values.put("descr", fDescr)
                values.put("formato", fFormatoLin)
                dbAlba.insert("tmphco", null, values)
            } else dbAlba.update("tmphco", values, "linea=$fLinea", null)
        }
         */
    }


    fun borrar() {
        // TODO
        //dbAlba.delete("tmphco", "1=1", null)
    }



    // Buscamos si el artículo está en el temporal y según el resultado, insertamos o editamos.
    // Esta función es casi idéntica a la de arriba (aceptarCambios), la diferencia está en que
    // localizamos la línea mediante el artículo.
    fun aceptarCambiosArt(queArticulo: Int) {
        var fInsertando = true
        if (queArticulo > 0) {
            // Compruebo si el artículo ya existe en la tabla temporal.
            // TODO
            /*
            dbAlba.rawQuery("SELECT * FROM tmphco", null).use { cTmpHco ->
                cTmpHco.moveToFirst()
                while (!cTmpHco.isAfterLast) {
                    if (cTmpHco.getInt(cTmpHco.getColumnIndex("articulo") ?: 19) == queArticulo) {
                        fInsertando = false
                    }
                    cTmpHco.moveToNext()
                }
            }
             */
        }
        // TODO
        /*
        val values = ContentValues()
        values.put("cajas", fCajas)
        values.put("cantidad", fCantidad)
        values.put("piezas", fPiezas)
        values.put("precio", fPrecio)
        values.put("precioii", fPrecioII)
        values.put("dto", fDtoLin)
        values.put("dtoi", fDtoImp)
        values.put("dtoiii", fDtoImpII)
        values.put("codigoiva", fCodigoIva)
        values.put("tasa1", fTasa1)
        values.put("tasa2", fTasa2)
        values.put("flag", fFlag)
        values.put("flag5", fFlag5)
        if (fInsertando) {
            values.put("articulo", fArticulo)
            values.put("codigo", fCodigo)
            values.put("descr", fDescr)
            values.put("formato", fFormatoLin)
            dbAlba.insert("tmphco", null, values)
        } else dbAlba.update("tmphco", values, "articulo=$queArticulo", null)
         */
    }

}