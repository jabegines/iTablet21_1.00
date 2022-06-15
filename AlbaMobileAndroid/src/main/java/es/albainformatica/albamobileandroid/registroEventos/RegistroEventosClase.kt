package es.albainformatica.albamobileandroid.registroEventos

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.Configuracion
import es.albainformatica.albamobileandroid.dao.RegistroDeEventosDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.RegistroDeEventosEnt
import java.text.SimpleDateFormat
import java.util.*


class RegistroEventosClase(val contexto: Context) {
    private val regEventosDao: RegistroDeEventosDao? = MyDatabase.getInstance(contexto)?.regEventosDao()
    private var prefs: SharedPreferences? = PreferenceManager.getDefaultSharedPreferences(contexto)


    fun registrarEvento(codEvento: String, descrEvento: String) {
        val fConfiguracion: Configuracion = Comunicador.fConfiguracion
        val regEventosEnt = RegistroDeEventosEnt()

        val tim = System.currentTimeMillis()
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dfHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        regEventosEnt.fecha = df.format(tim)
        regEventosEnt.hora = dfHora.format(tim)

        var ultimoOrden = regEventosDao?.getUltimoOrdenDiario(regEventosEnt.fecha) ?: 0
        ultimoOrden++
        regEventosEnt.ordenDiarioPuesto = ultimoOrden
        if (fConfiguracion.vendedor() != "") regEventosEnt.usuario = fConfiguracion.vendedor().toShort()
        else regEventosEnt.usuario = -1
        regEventosEnt.almacen = fConfiguracion.almacen()
        if (fConfiguracion.codTerminal() != "") regEventosEnt.puesto = fConfiguracion.codTerminal().toShort()
        else regEventosEnt.puesto = -1
        regEventosEnt.codigoEvento = codEvento
        regEventosEnt.descrEvento = descrEvento
        regEventosEnt.ip = ""
        regEventosEnt.ejercicio = fConfiguracion.ejercicio()
        val queEmpresa = prefs?.getInt("ultima_empresa", 0) ?: -1
        regEventosEnt.empresa = queEmpresa.toShort()
        regEventosEnt.estado = "N"

        regEventosDao?.insertar(regEventosEnt)
    }


}