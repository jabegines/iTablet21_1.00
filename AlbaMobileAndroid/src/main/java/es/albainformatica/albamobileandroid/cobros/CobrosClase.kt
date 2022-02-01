package es.albainformatica.albamobileandroid.cobros

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.Comunicador.Companion.fConfiguracion
import es.albainformatica.albamobileandroid.dao.CobrosDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.CobrosEnt
import java.text.SimpleDateFormat
import java.util.*


class CobrosClase(queContexto: Context) {
    private val fContexto = queContexto
    private val cobrosDao: CobrosDao? = MyDatabase.getInstance(queContexto)?.cobrosDao()
    var fCliente = 0



    fun abrir(queCliente: Int): MutableList<CobrosEnt> {
        fCliente = queCliente

        return cobrosDao?.abrir(queCliente) ?: emptyList<CobrosEnt>().toMutableList()
    }


    fun impCobradoDoc(): Double {
        val fDocumento = Comunicador.fDocumento

        val sCobrado = cobrosDao?.dimeCobradoDoc(fDocumento.fTipoDoc, fDocumento.fAlmacen, fDocumento.serie,
                                        fDocumento.numero, fDocumento.fEjercicio) ?: "0.0"
        return sCobrado.toDouble()
    }


    fun borrarCobroDoc() {
        val fDocumento = Comunicador.fDocumento

        cobrosDao?.borrarCobroDoc(fDocumento.fTipoDoc, fDocumento.fAlmacen, fDocumento.serie,
            fDocumento.numero, fDocumento.fEjercicio)
    }



    fun nuevoCobro(cobroEnt: CobrosEnt) {
        // Comprobamos que no haya otro cobro igual, en cuyo caso preguntamos si lo insertamos o no
        val cobroExist = cobrosDao?.existeCobro(cobroEnt.clienteId, cobroEnt.tipoDoc,
            cobroEnt.ejercicio, cobroEnt.empresa, cobroEnt.fechaCobro, cobroEnt.cobro, cobroEnt.vAlmacen,
            cobroEnt.vPuesto, cobroEnt.vApunte, cobroEnt.vEjercicio) ?: CobrosEnt()

        if (cobroExist.cobroId > 0) {
            val aldDialog = nuevoAlertBuilder(fContexto as Activity, "Cobro existente",
                fContexto.resources.getString(R.string.msj_CobroExiste), true)

            aldDialog.setPositiveButton("Sí") { _: DialogInterface?, _: Int ->
                // Estableceremos la matrícula en el momento de crear el cobro, así nos aseguramos
                // de que cada cobro tiene una matrícula única
                val tim = System.currentTimeMillis()
                val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS", Locale.getDefault())
                val queFechaMatr = df.format(tim).replace("/", "").replace(" ", "").replace(":", "").replace(".", "")
                cobroEnt.matricula = fConfiguracion.almacen().toString() + fConfiguracion.codTerminal() + queFechaMatr

                 cobrosDao?.insertar(cobroEnt)
            }
            val alert = aldDialog.create()
            alert.show()

        } else {
            // Estableceremos la matrícula en el momento de crear el cobro, así nos aseguramos
            // de que cada cobro tiene una matrícula única
            val tim = System.currentTimeMillis()
            val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS", Locale.getDefault())
            val queFechaMatr = df.format(tim).replace("/", "").replace(" ", "").replace(":", "").replace(".", "")
            cobroEnt.matricula = fConfiguracion.almacen().toString() + fConfiguracion.codTerminal() + queFechaMatr

            cobrosDao?.insertar(cobroEnt)
        }
    }

    fun dimeTotalCobros(queCliente: Int): Double {
        var fTotal = 0.0

        val lCobros = cobrosDao?.dimeCobrosClte(queCliente) ?: emptyList<String>().toMutableList()

        for (queCobro in lCobros) {
            fTotal += queCobro.replace(',', '.').toDouble()
        }

        return fTotal
    }


    fun dimeCobrosDoc(queTipoDoc: String, queAlmacen: String, queSerie: String, queNumero: String,
        queEjercicio: String, queEmpresa: String): Double {

        val sCobrado = cobrosDao?.dimeCobrosDoc(queTipoDoc, queAlmacen, queSerie, queNumero, queEjercicio, queEmpresa) ?: "0.0"
        return sCobrado.replace(',', '.').toDouble()
    }


    fun abrirEntreFechas(fDesdeFecha: String, fHastaFecha: String): MutableList<DatosInfCobros> {

        return cobrosDao?.abrirEntreFechas(fechaEnJulian(fDesdeFecha), fechaEnJulian(fHastaFecha))
            ?: emptyList<DatosInfCobros>().toMutableList()
    }


    fun abrirResDivisas(fDesdeFecha: String, fHastaFecha: String): MutableList<DatosResCobros> {

        return cobrosDao?.abrirResDivisas(fechaEnJulian(fDesdeFecha), fechaEnJulian(fHastaFecha))
            ?: emptyList<DatosResCobros>().toMutableList()

    }



}