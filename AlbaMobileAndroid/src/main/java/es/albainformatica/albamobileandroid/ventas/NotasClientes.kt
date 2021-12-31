package es.albainformatica.albamobileandroid.ventas

import es.albainformatica.albamobileandroid.BaseDatos
import android.content.Context
import es.albainformatica.albamobileandroid.dao.NotasCltesDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.NotasCltesEnt
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by jabegines on 27/12/2017.
 */
class NotasClientes(contexto: Context): BaseDatos(contexto) {
    private val notasCltesDao: NotasCltesDao? = MyDatabase.getInstance(contexto)?.notasCltesDao()



    fun abrirUnCliente(queCliente: Int): MutableList<NotasCltesEnt> {
         return notasCltesDao?.abrirUnCliente(queCliente) ?: emptyList<NotasCltesEnt>().toMutableList()
    }

    fun abrirParaEnviar(queNumExportacion: Int): MutableList<NotasCltesEnt> {
        val lNotas: MutableList<NotasCltesEnt> =
            if (queNumExportacion > 0) {
                notasCltesDao?.abrirParaEnviar() ?: emptyList<NotasCltesEnt>().toMutableList()
            } else {
                notasCltesDao?.abrirNumExp(queNumExportacion) ?: emptyList<NotasCltesEnt>().toMutableList()
            }

        return lNotas
    }



    fun anyadirNota(queCliente: Int, queNota: String) {
        val notaEnt = NotasCltesEnt()
        notaEnt.clienteId = queCliente
        notaEnt.nota = queNota
        notaEnt.estado = "N"
        // Obtenemos la fecha actual
        val tim = System.currentTimeMillis()
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        notaEnt.fecha = df.format(tim)

        notasCltesDao?.insertar(notaEnt)

        refrescarNotas(queCliente)
    }


    fun editarNota(queNotaId: Int, queCliente: Int, queNota: String, queEstado: String, queFecha: String) {
        val notaEnt = NotasCltesEnt()
        notaEnt.notaId = queNotaId
        notaEnt.clienteId = queCliente
        notaEnt.nota = queNota
        notaEnt.estado = queEstado
        notaEnt.fecha = queFecha

        notasCltesDao?.actualizar(notaEnt)

        refrescarNotas(queCliente)
    }

    private fun refrescarNotas(queCliente: Int) {
        abrirUnCliente(queCliente)
    }

    fun marcarComoExportadas(iSigExportacion: Int) {
        notasCltesDao?.marcarNuevasComoExport(iSigExportacion)
        notasCltesDao?.marcarModifComoExport(iSigExportacion)
    }

    fun marcarNumExport(fNumPaquete: Int) {
        notasCltesDao?.marcarNumExport(fNumPaquete)
    }

}