package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import es.albainformatica.albamobileandroid.dao.CnfTarifasDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.CnfTarifasEnt
import java.util.ArrayList

/**
 * Created by jabegines on 11/10/13.
 */
class Tarifas(contexto: Context) {
    private var cnfTarifasDao: CnfTarifasDao? = MyDatabase.getInstance(contexto)?.cnfTarifasDao()

    private lateinit var lTarifas: List<CnfTarifasEnt>


    fun abrir(): Boolean {
        lTarifas = cnfTarifasDao?.getAllCnfTarifas() ?: emptyList<CnfTarifasEnt>().toMutableList()

        return (lTarifas.count() > 0)
    }


    fun llenarArray(sArrayList: ArrayList<String>) {
        sArrayList.add("Sin tarifa")

        for (cnfTrf in lTarifas) {
            sArrayList.add(cnfTrf.codigo.toString() + " " + cnfTrf.descrTarifa)
        }
    }
}