package es.albainformatica.albamobileandroid

import es.albainformatica.albamobileandroid.cobros.CobrosClase
import es.albainformatica.albamobileandroid.cobros.PendienteClase
import es.albainformatica.albamobileandroid.historicos.Historico
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.registroEventos.RegistroEventosClase
import es.albainformatica.albamobileandroid.ventas.Documento


class Comunicador {

    companion object {

        lateinit var fDocumento: Documento
        lateinit var fCobros: CobrosClase
        lateinit var fPendiente: PendienteClase
        lateinit var fHistorico: Historico
        lateinit var fConfiguracion: Configuracion
        lateinit var fArticulos: ArticulosClase
        lateinit var fArticulosGrv: ArticulosClase

        lateinit var fRegEventos: RegistroEventosClase

/*
        // --------------------------------------------------------
        // Documento
        fun setDocumento(newDocumento: Documento) {
            fDocumento = newDocumento
        }

        fun getDocumento(): Documento {
            return fDocumento
        }

        // --------------------------------------------------------
        // Cobros
        fun setCobros(newCobros: Cobros) {
            fCobros = newCobros
        }

        fun getCobros(): Cobros {
            return fCobros
        }

        // --------------------------------------------------------
        // Pendiente
        fun setPendiente(newPendiente: PendienteClase) {
            fPendiente = newPendiente
        }

        fun getPendiente(): PendienteClase {
            return fPendiente
        }

        // --------------------------------------------------------
        // Histórico
        fun setHistorico(newHistorico: Historico) {
            fHistorico = newHistorico
        }

        fun getHistorico(): Historico {
            return fHistorico
        }

        // --------------------------------------------------------
        // Configuración
        fun setConfiguracion(newConfiguracion: Configuracion) {
            fConfiguracion = newConfiguracion
        }

        fun getConfiguracion(): Configuracion {
            return fConfiguracion
        }

        // --------------------------------------------------------
        // Articulos
        fun setArticulos(newArticulos: ArticulosClase) {
            fArticulos = newArticulos
        }

        fun getArticulos(): ArticulosClase {
            return fArticulos
        }

        // --------------------------------------------------------
        // ArticulosGrv (para el gridview de articulos en el catálogo)
        fun setArticulosGrv(newArticulosGrv: ArticulosClase) {
            fArticulosGrv = newArticulosGrv
        }

        fun getArticulosGrv(): ArticulosClase {
            return fArticulosGrv
        }
*/

    }

}