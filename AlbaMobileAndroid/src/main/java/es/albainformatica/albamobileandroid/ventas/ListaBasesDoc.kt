package es.albainformatica.albamobileandroid.ventas

import android.content.Context
import es.albainformatica.albamobileandroid.Redondear
import es.albainformatica.albamobileandroid.dao.IvasDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.IvasEnt
import java.util.ArrayList

/**
 * Created by jabegines on 14/10/13.
 */
class ListaBasesDoc(contexto: Context) {

    private val ivasDao: IvasDao? = MyDatabase.getInstance(contexto)?.ivasDao()
    var fLista: ArrayList<TBaseDocumento> = ArrayList()
    var fAplicarIva: Boolean = true
    var fAplicarRecargo: Boolean = false
    var fIvaIncluido: Boolean = false
    var fDecImpBase = 0
    var fDecImpII = 0


    fun close() {
        fLista.clear()
    }

    private fun anyadeBase(Base: TBaseDocumento) {
        fLista.add(Base)
    }

    fun calcularBase(codigoIva: Short, importe: Double) {
        var continuar = true

        // Si no tenemos el código de iva en la lista, lo añadimos.
        if (!existeBase(codigoIva)) {
            val ivaEnt = ivasDao?.getCodigoIva(codigoIva) ?: IvasEnt()

                if (ivaEnt.codigo > 0) {
                    val oBase = TBaseDocumento()
                    if (fAplicarIva) {
                        oBase.fCodigoIva = codigoIva
                        oBase.fPorcIva = ivaEnt.porcIva.replace(',', '.').toDouble()
                        if (fAplicarRecargo) oBase.fPorcRe = ivaEnt.porcRe.replace(',', '.').toDouble()
                        else oBase.fPorcRe = 0.0
                    } else {
                        oBase.fCodigoIva = -1
                        oBase.fPorcIva = 0.0
                        oBase.fPorcRe = 0.0
                    }
                    anyadeBase(oBase)
                    // Le aplicamos la configuración a la base que acabamos de añadir.
                    aplicarConfiguracion(oBase)
                } else continuar = false

        }

        if (continuar) {
            val oBase = getBasePorCodigo(codigoIva)
            oBase.calcularBase(importe)
        }
    }

    private fun aplicarConfiguracion(Base: TBaseDocumento) {
        // Nos aseguramos de que si no aplicamos IVA tampoco apliquemos el recargo.
        if (!fAplicarIva) fAplicarRecargo = false
        Base.fAplicarIva = fAplicarIva
        Base.fAplicarRecargo = fAplicarRecargo
        Base.fIvaIncluido = fIvaIncluido
        Base.fDecImpBase = fDecImpBase
        Base.fDecImpII = fDecImpII
    }


    fun calcularDtosPie(Dto1: Double, Dto2: Double, Dto3: Double, Dto4: Double) {
        for (x in fLista) {
            x.fDtoPie1 = Dto1
            x.fDtoPie2 = Dto2
            x.fDtoPie3 = Dto3
            x.fDtoPie4 = Dto4
            x.calcularBaseConDtos()
        }
    }

    private fun existeBase(codigoIva: Short): Boolean {
        for (aFLista in fLista) {
            if (aFLista.fCodigoIva == codigoIva) return true
        }
        return false
    }

    private fun getBasePorCodigo(CodigoIva: Short): TBaseDocumento {
        var x = 0
        for (i in fLista.indices) {
            if (fLista[i].fCodigoIva == CodigoIva) x = i
        }
        // Si no hemos encontrado el código, devolveremos el primer elemento de la lista.
        return fLista[x]
    }

    val totalBruto: Double
        get() {
            var fTotal = 0.0
            for (x in fLista) {
                fTotal += x.fImpteBruto
            }
            return fTotal
        }
    val totalBases: Double
        get() {
            var fTotal = 0.0
            for (x in fLista) {
                fTotal += x.fBaseImponible
            }
            return fTotal
        }
    val totalIva: Double
        get() {
            var fTotal = 0.0
            for (x in fLista) {
                fTotal += x.fImporteIva
            }
            return fTotal
        }
    val totalRe: Double
        get() {
            var fTotal = 0.0
            for (x in fLista) {
                fTotal += x.fImporteRe
            }
            return fTotal
        }
    val totalConImptos: Double
        get() {
            var fTotal = 0.0
            for (x in fLista) {
                fTotal += x.fTotalConImptos
            }
            return fTotal
        }

    fun cargarDesdeDoc(fIdDoc: Int) {
        var fCodigoIva: Short
        var fImporte: Double
        var fImporteII: Double
        var dPorcIva: Double
        // TODO
        /*
        val cLineas = dbAlba.rawQuery(
            "SELECT A.codigoiva, A.importe, A.importeii, B.iva porciva"
                    + " FROM lineas A "
                    + " LEFT OUTER JOIN ivas B ON B.codigo = A.codigoiva"
                    + " WHERE A.cabeceraId = " + fIdDoc, null
        )
        cLineas.moveToFirst()
        while (!cLineas.isAfterLast) {
            fCodigoIva = cLineas.getShort(cLineas.getColumnIndex("codigoiva"))
            fImporte =
                cLineas.getString(cLineas.getColumnIndex("importe")).replace(',', '.').toDouble()
            // Cuando el importe iva incluído es null lo recalculamos.
            if (cLineas.getString(cLineas.getColumnIndex("importeii")) == null) {
                fImporteII = cLineas.getString(cLineas.getColumnIndex("importe")).replace(',', '.')
                    .toDouble()
                if (cLineas.getString(cLineas.getColumnIndex("porciva")) != null) {
                    dPorcIva =
                        cLineas.getString(cLineas.getColumnIndex("porciva")).replace(',', '.')
                            .toDouble()
                    fImporteII += fImporteII * dPorcIva / 100
                } else fImporteII = 0.0
            } else fImporteII =
                cLineas.getString(cLineas.getColumnIndex("importeii")).replace(',', '.').toDouble()

            //if (fAplicarIva) {
            if (fIvaIncluido) calcularBase(fCodigoIva, fImporteII) else calcularBase(
                fCodigoIva,
                fImporte
            )
            cLineas.moveToNext()
        }
        cLineas.close()
        */
    }


    class TBaseDocumento {
        var fImpteBruto = 0.0
        var fDtoPie1: Double = 0.0
        var fDtoPie2: Double = 0.0
        var fDtoPie3: Double = 0.0
        var fDtoPie4: Double = 0.0
        var fImpDtosPie = 0.0
        var fBaseImponible = 0.0
        var fImporteIva = 0.0
        var fImporteRe = 0.0
        var fPorcIva = 0.0
        var fPorcRe = 0.0
        var fCodigoIva: Short = 0
        private var fTotal = 0.0
        var fTotalConImptos = 0.0
        var fAplicarIva: Boolean = true
        var fAplicarRecargo: Boolean = false
        var fIvaIncluido: Boolean = false
        var fDecImpBase = 0
        var fDecImpII = 0


        fun calcularBase(Importe: Double) {
            // Según vendamos iva incluído o no, el importe bruto será igual a la base
            // antes de descuentos o al total antes de descuentos.
            if (fIvaIncluido) {
                fTotal += Importe
                fImpteBruto += Importe
                fBaseImponible = Redondear(fTotal / ((100 + fPorcIva) / 100), fDecImpII + 2)
            } else {
                fBaseImponible += Importe
                fImpteBruto += Importe
            }
            fImporteIva = if (fAplicarIva) Redondear(fBaseImponible * fPorcIva / 100, fDecImpBase) else 0.0
            fImporteRe = if (fAplicarRecargo) Redondear(fBaseImponible * fPorcRe / 100, fDecImpBase) else 0.0

            // La diferencia entre Total y TotalConImptos es el recargo de
            // equivalencia, que Total no incluye. Me viene bien cuando trabajo iva
            // incluído, ya que a Total no puedo añadirle el recargo y, para calcular
            // la base, parto de Total.
            fTotalConImptos = Redondear(fBaseImponible + fImporteIva + fImporteRe, fDecImpII)
        }

        fun calcularBaseConDtos() {
            if (fIvaIncluido) {
                fTotal = fImpteBruto - fImpteBruto * fDtoPie1 / 100
                fTotal -= fTotal * fDtoPie2 / 100
                fTotal -= fTotal * fDtoPie3 / 100
                fTotal -= fTotal * fDtoPie4 / 100
                fTotal = Redondear(fTotal, fDecImpII)
                fImpDtosPie = Redondear(fImpteBruto - fTotal, fDecImpII)
            } else {
                fBaseImponible = fImpteBruto - fImpteBruto * fDtoPie1 / 100
                fBaseImponible -= fBaseImponible * fDtoPie2 / 100
                fBaseImponible -= fBaseImponible * fDtoPie3 / 100
                fBaseImponible -= fBaseImponible * fDtoPie4 / 100
                fBaseImponible = Redondear(fBaseImponible, fDecImpBase)
                fImpDtosPie = Redondear(fImpteBruto - fBaseImponible, fDecImpBase)
            }
            calcularBase(0.0)
        }
    }

}