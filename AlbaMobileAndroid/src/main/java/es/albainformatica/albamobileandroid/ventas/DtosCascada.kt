package es.albainformatica.albamobileandroid.ventas

import android.content.Context
import es.albainformatica.albamobileandroid.redondear
import es.albainformatica.albamobileandroid.dao.DtosLineasDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.DtosLineasEnt

/**
 * Created by jabegines on 30/05/2014.
 */
class DtosCascada(fContexto: Context) {
    private val dtosLineasDao: DtosLineasDao? = MyDatabase.getInstance(fContexto)?.dtosLineasDao()

    var fIvaIncluido: Boolean = false
    var fAplicarIva: Boolean = true
    var fPorcIva: Double = 0.0
    var fDecPrBase = 0

    lateinit var lDescuentos: List<DtosLineasEnt>



    fun abrir(queLinea: Int) {
        lDescuentos = dtosLineasDao?.getAllDtosLinea(queLinea) ?: emptyList<DtosLineasEnt>().toMutableList()
    }


    fun calcularDtoEquiv(precio: Double, fDecPrBase: Int): Float {
        // Tenemos que usar variables de tipo Float para que funcione igual que el programa de gestión,
        // si las usamos de tipo Double no da el mismo resultado.
        val sImporte = precio.toString()
        var fImporte = sImporte.toFloat()
        var redondear = false

        for (dto in lDescuentos) {
            val fPorcDto = dto.descuento.replace(',', '.').toDouble()
            val impDto = getImpDto(dto)
            val cant1 = getCant1(dto)
            val cant2 = getCant2(dto)

            if (fPorcDto != 0.0) {
                val dPorc = 1 - fPorcDto / 100
                val sPorc = dPorc.toString()
                val fPorc = sPorc.toFloat()
                fImporte *= fPorc

            } else if (impDto != 0.0) {
                redondear = true
                val sImpDto = impDto.toString()
                val fImpDto = sImpDto.toFloat()
                fImporte -= fImpDto

            } else if (cant1 != 0.0 && cant2 != 0.0) {
                val sCant1 = cant1.toString()
                val fCant1 = sCant1.toFloat()
                val sCant2 = cant2.toString()
                val fCant2 = sCant2.toFloat()
                fImporte = fImporte * fCant2 / fCant1
            }
        }
        // Tenemos que redondear a 4 decimales para emular el comportamiento en el programa de gestión
        // de la clase TDivisa, que convierte los valores a currency y, por lo tanto, trabaja con 4 decimales.
        // Esto lo hacemos así para obtener los mismos resultados en la tablet y en la gestión.
        if (redondear) {
            fImporte = redondear(fImporte, 4)
            fImporte = redondear(fImporte, fDecPrBase)
        }
        val fDescuento: Float = if (precio == 0.0) 0.0f else {
            val sPrecio = precio.toString()
            val fPrecio = sPrecio.toFloat()
            (1 - fImporte / fPrecio) * 100
        }
        return fDescuento
    }


    fun borrar(queId: Int) {
        dtosLineasDao?.borrarDto(queId)
    }


    // Si estamos vendiendo iva incluído, le quitamos el iva al importe del descuento.
    // El cálculo lo hago con fDecPrBase+1 porque creo que la gestión también lo hace así.
    private fun getImpDto(dto: DtosLineasEnt): Double {

        var sImporte = dto.importe
        return if (sImporte != "") {
            sImporte = sImporte.replace(',', '.')
            var dImporte = sImporte.toDouble()

            // Si estamos vendiendo iva incluído, le quitamos el iva al importe del descuento.
            // El cálculo lo hago con fDecPrBase+1 porque creo que la gestión también lo hace así.
            if (fIvaIncluido && fAplicarIva) {
                val dIvaDiv = (100 + fPorcIva) / 100
                dImporte = redondear(dImporte / dIvaDiv, fDecPrBase + 1)
            }
            dImporte

        } else 0.0
    }


    private fun getCant1(dto: DtosLineasEnt): Double {

        var sCantidad = dto.cantidad1
        return if (sCantidad != "") {
            sCantidad = sCantidad.replace(',', '.')
            sCantidad.toDouble()

        } else 0.0
    }


    private fun getCant2(dto: DtosLineasEnt): Double {

        var sCantidad = dto.cantidad2
        return if (sCantidad != "") {
            sCantidad = sCantidad.replace(',', '.')
            sCantidad.toDouble()

        } else 0.0
    }


    fun desdeRating(): Boolean {
        return if (lDescuentos.count() > 0) {
            lDescuentos[0].desdeRating == "T"
        } else false
    }

}