package es.albainformatica.albamobileandroid.historicos

import android.content.Context
import es.albainformatica.albamobileandroid.DatosHcoCompSemMes
import es.albainformatica.albamobileandroid.dao.HcoCompSemMesDao
import es.albainformatica.albamobileandroid.database.MyDatabase


class HcoComSemMes(contexto: Context) {
    private var hcoComSemMesDao: HcoCompSemMesDao? = MyDatabase.getInstance(contexto)?.hcoCompSemMesDao()

    lateinit var lHcoCompSemMes: List<DatosHcoCompSemMes>


    fun abrir(queCliente: Int, sHoy: String, sHoyMenos6: String, sHoyMenos7: String, sHoyMenos13: String) {

        lHcoCompSemMes = hcoComSemMesDao?.abrir(queCliente, fechaEnJulian(sHoyMenos6), fechaEnJulian(sHoy),
                                fechaEnJulian(sHoyMenos13), fechaEnJulian(sHoyMenos7))
                            ?: emptyList<DatosHcoCompSemMes>().toMutableList()
    }


    private fun fechaEnJulian(queFecha: String): String {
        val queAnyo = queFecha.substring(6, 10)
        val queMes = queFecha.substring(3, 5)
        val queDia = queFecha.substring(0, 2)
        return "$queAnyo-$queMes-$queDia"
    }

}