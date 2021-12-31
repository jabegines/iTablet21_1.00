package es.albainformatica.albamobileandroid.ventas

import android.content.Context
import android.database.Cursor
import es.albainformatica.albamobileandroid.Redondear
import android.database.sqlite.SQLiteDatabase

/**
 * Created by jabegines on 30/05/2014.
 */
class DtosCascada(fContexto: Context): BaseDatos(fContexto) {
    private var dbAlba: SQLiteDatabase = writableDatabase
    lateinit var cursor: Cursor
    var fIvaIncluido: Boolean = false
    var fAplicarIva: Boolean = true
    var fExentoIva: Boolean = false
    private var fMovLinea = 0
    var fPorcIva: Double = 0.0
    var fDecPrBase = 0


    override fun close() {
        dbAlba.close()
    }


    fun abrir(queLinea: Int) {
        cursor = dbAlba.rawQuery("SELECT * FROM desctoslineas WHERE linea = $queLinea", null)
        cursor.moveToFirst()
    }

    fun calcularDtoEquiv(precio: Double, fDecPrBase: Int): Float {
        // Tenemos que usar variables de tipo Float para que funcione igual que el programa de gestión,
        // si las usamos de tipo Double no da el mismo resultado.
        val sImporte = precio.toString()
        var fImporte = sImporte.toFloat()
        var redondear = false
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            if (porcDto != 0.0) {
                val dPorc = 1 - porcDto / 100
                val sPorc = dPorc.toString()
                val fPorc = sPorc.toFloat()
                fImporte *= fPorc
            } else if (impDto != 0.0) {
                redondear = true
                val dImpDto = impDto
                val sImpDto = dImpDto.toString()
                val fImpDto = sImpDto.toFloat()
                fImporte -= fImpDto
            } else if (cant1 != 0.0 && cant2 != 0.0) {
                val dCant1 = cant1
                val sCant1 = dCant1.toString()
                val fCant1 = sCant1.toFloat()
                val dCant2 = cant2
                val sCant2 = dCant2.toString()
                val fCant2 = sCant2.toFloat()
                fImporte = fImporte * fCant2 / fCant1
            }
            cursor.moveToNext()
        }
        // Tenemos que redondear a 4 decimales para emular el comportamiento en el programa de gestión
        // de la clase TDivisa, que convierte los valores a currency y, por lo tanto, trabaja con 4 decimales.
        // Esto lo hacemos así para obtener los mismos resultados en la tablet y en la gestión.
        if (redondear) {
            fImporte = Redondear(fImporte, 4)
            fImporte = Redondear(fImporte, fDecPrBase)
        }
        val fDescuento: Float = if (precio == 0.0) 0.0f else {
            val sPrecio = precio.toString()
            val fPrecio = sPrecio.toFloat()
            (1 - fImporte / fPrecio) * 100
        }
        return fDescuento
    }

    fun borrar(queId: Int) {
        dbAlba.delete("desctoslineas", "_id=$queId", null)
    }

    val porcDto: Double
        get() {
            var sDescuento = cursor.getString(cursor.getColumnIndex("descuento"))
            return if (sDescuento != null) {
                sDescuento = sDescuento.replace(',', '.')
                java.lang.Double.valueOf(sDescuento)
            } else 0.0
        }

    // Si estamos vendiendo iva incluído, le quitamos el iva al importe del descuento.
    // El cálculo lo hago con fDecPrBase+1 porque creo que la gestión también lo hace así.
    val impDto: Double
        get() {
            var sDescuento = cursor.getString(cursor.getColumnIndex("importe"))
            return if (sDescuento != null) {
                sDescuento = sDescuento.replace(',', '.')
                var dImporte = java.lang.Double.valueOf(sDescuento)

                // Si estamos vendiendo iva incluído, le quitamos el iva al importe del descuento.
                // El cálculo lo hago con fDecPrBase+1 porque creo que la gestión también lo hace así.
                if ((fIvaIncluido || fMovLinea == 3) && fAplicarIva) {
                    if (fMovLinea != 2 && !fExentoIva) {
                        val dIvaDiv = (100 + fPorcIva) / 100
                        dImporte = Redondear(dImporte / dIvaDiv, fDecPrBase + 1)
                    }
                }
                dImporte
            } else 0.0
        }
    val cant1: Double
        get() {
            var sCantidad = cursor.getString(cursor.getColumnIndex("cantidad1"))
            return if (sCantidad != null) {
                sCantidad = sCantidad.replace(',', '.')
                java.lang.Double.valueOf(sCantidad)
            } else 0.0
        }
    val cant2: Double
        get() {
            var sCantidad = cursor.getString(cursor.getColumnIndex("cantidad2"))
            return if (sCantidad != null) {
                sCantidad = sCantidad.replace(',', '.')
                java.lang.Double.valueOf(sCantidad)
            } else 0.0
        }
    val desdeRating: Boolean
        get() = if (!cursor.isBeforeFirst) {
            val sResultado = cursor.getString(cursor.getColumnIndex("desderating"))
            if (sResultado != null) sResultado == "T" else false
        } else false

}