package es.albainformatica.albamobileandroid.registroEventos

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.Configuracion
import es.albainformatica.albamobileandroid.dao.RegistroDeEventosDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.RegistroDeEventosEnt
import es.albainformatica.albamobileandroid.sha512
import java.text.SimpleDateFormat
import java.util.*


class RegistroEventosClase(val contexto: Context) {
    private val regEventosDao: RegistroDeEventosDao? = MyDatabase.getInstance(contexto)?.regEventosDao()
    private var prefs: SharedPreferences? = PreferenceManager.getDefaultSharedPreferences(contexto)

    private val semilla111 = "ALBAIBS"
    private val semilla211 = "PLAZADEFRANCIA"
    private val semilla311 = "SEVILLA-UTRERA"
    private val semilla411 = "ANDALUCIA-ESPAÑA"
    private val semilla511 = "IGES21ICONTA21"
    private val versionHuella = "1"



    fun registrarEvento(codEvento: String, descrEvento: String) {
        val fConfiguracion: Configuracion = Comunicador.fConfiguracion
        val queEmpresa = prefs?.getInt("ultima_empresa", 0)?.toShort() ?: -1
        val regEventosEnt = RegistroDeEventosEnt()

        val tim = System.currentTimeMillis()
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dfHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        regEventosEnt.fecha = df.format(tim)
        regEventosEnt.hora = dfHora.format(tim)

        var ultimoOrden = regEventosDao?.getUltimoOrdenDiario(regEventosEnt.fecha, queEmpresa) ?: 0
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
        regEventosEnt.empresa = queEmpresa
        regEventosEnt.estado = "N"

        val queRegId = regEventosDao?.insertar(regEventosEnt) ?: 0

        /*
        val fDato = "<row FECHAYHORA=\"" + regEventosEnt.fecha + "T" +
                regEventosEnt.hora + "\" ORDENDIARIOPUESTO=\"" + regEventosEnt.ordenDiarioPuesto +
                "\" USUARIO=\"" + regEventosEnt.usuario + "\" ALMACEN=\"" + regEventosEnt.almacen +
                "\" PUESTO=\"" + regEventosEnt.puesto + "\" IP=\"" + regEventosEnt.ip +
                "\" EJERCICIO=\"" + regEventosEnt.ejercicio +
                "\" EMPRESA=\"" + regEventosEnt.empresa + "\" CODIGOEVENTO=\"" + regEventosEnt.codigoEvento +
                "\" DESCRIPCIONEVENTO=\"" + regEventosEnt.descrEvento + "\" TEXTOEVENTO=\"" +
                regEventosEnt.textoEvento + "\"/>"
        val fTextoBruto = semilla111 + semilla211 + fDato + semilla311 + semilla411

        val regEventAntEnt = regEventosDao?.getEventoAnterior(queRegId.toInt(), queEmpresa) ?: RegistroDeEventosEnt()
        val fDatoRefAnterior = "<row FECHAYHORA=\"" + regEventAntEnt.fecha + "T" + regEventAntEnt.hora +
                "\" ORDENDIARIOPUESTO=\"" + regEventAntEnt.ordenDiarioPuesto + "\" USUARIO=\"" + regEventAntEnt.usuario +
                "\" ALMACEN=\"" + regEventAntEnt.almacen + "\" PUESTO=\"" + regEventAntEnt.puesto +
                "\" IP=\"" + regEventAntEnt.ip + "\" EJERCICIO=\"" + regEventAntEnt.ejercicio +
                "\" EMPRESA=\"" + regEventAntEnt.empresa + "\" CODIGOEVENTO=\"" + regEventAntEnt.codigoEvento + "\"/>"

        val fTextoBrutoCadena = semilla111 + fDatoRefAnterior + semilla211
        val fFirmaRegistro = calcularFirmaRegistro(fTextoBruto)
        val fTextoFirmaEnc = semilla211 + fFirmaRegistro + semilla111 + fTextoBrutoCadena
        val fFirmaEncadenada = calcularFirmaEncadenada(fTextoFirmaEnc)
        val fHashRefAnterior = hashDelDato(fDatoRefAnterior)
        val fHuella = hashDelDato(fDato)

        regEventosDao?.actualizarHuella(queRegId.toInt(), fDatoRefAnterior, fHashRefAnterior, fHuella, fFirmaRegistro,
                        fFirmaEncadenada, versionHuella)
         */
    }
    

    private fun calcularFirmaEncadenada(queDato: String): String {
        val aux = queDato + semilla511
        return hashDelDato(aux)
    }

    private fun calcularFirmaRegistro(queDato: String): String {
        val aux = queDato + semilla511
        return hashDelDato(aux)
    }

    // Tenemos que hacer la conversión a hexadecimal con la codificación ISO, porque en las semillas hay eñes
    // y también puede haberlas en los datos
    private fun hashDelDato(queDato: String): String {
        val cadHex = queDato.toByteArray(charset("ISO_8859_1")).toHexString().uppercase()
        return sha512(cadHex)
    }

    
    private fun ByteArray.toHexString(): String {
        return this.joinToString("") {
            java.lang.String.format("%02x", it)
        }
    }

}