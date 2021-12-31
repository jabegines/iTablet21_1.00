package es.albainformatica.albamobileandroid.comunicaciones

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.media.Rating
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.util.Xml
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.R.string
import es.albainformatica.albamobileandroid.dao.*
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.*
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.maestros.ClientesClase
import es.albainformatica.albamobileandroid.maestros.LotesClase
import es.albainformatica.albamobileandroid.ventas.NotasClientes
import org.apache.commons.net.util.Base64
import org.xmlpull.v1.XmlPullParser
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.collections.ArrayList

class MiscComunicaciones(context: Context, desdeServicio: Boolean) {
    private var fTerminar: Boolean = false
    private val fContext: Context = context
    private val fDesdeServicio: Boolean = desdeServicio
    private var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(fContext)
    private var rutaLocal: String
    var rutaLocalEnvio: String
    private var fCodTerminal: String = prefs.getString("terminal", "") ?: ""
    private var fUsarMultisistema: Boolean = false

    private lateinit var dbAlba: SQLiteDatabase
    private var fImportando: Boolean = false
    var aCabeceras: ArrayList<Int> = ArrayList()
    private val msjRec = fContext.resources?.getString(string.msj_ImportandoFich)

    lateinit var puente: Handler
    lateinit var cadenaResumen: String
    private var fTamCltes: Long = 0
    private var fTamDirecc: Long = 0
    private var fTamContactos: Long = 0
    private var fTamNotasCltes: Long = 0
    private var fTamCabec: Long = 0
    private var fTamLineas: Long = 0
    private var fTamCobros: Long = 0
    private var fTamPdte: Long = 0
    private var fTamCargas: Long = 0
    private var fTamCargasLineas: Long = 0


    init {
        rutaLocal = prefs.getString("rutacomunicacion", "") ?: ""
        fUsarMultisistema = prefs.getBoolean("usar_multisistema", false)

        rutaLocalEnvio = if (rutaLocal == "") {
            if (fUsarMultisistema) "/storage/sdcard0/alba/envio/" + fCodTerminal + "/" + BaseDatos.queBaseDatos
            else "/storage/sdcard0/alba/envio/$fCodTerminal"
        } else {
            if (fUsarMultisistema) rutaLocal + "/envio/" + fCodTerminal + "/" + BaseDatos.queBaseDatos
            else "$rutaLocal/envio/$fCodTerminal"
        }
    }


    /*
    Estados de los documentos:
        N -> nuevo
        P -> guardado
        R -> reenviar       (no lo usamos con el servicio)
        X -> enviado

    Estados de los clientes:
        N -> nuevo
        M -> modificado
        XN -> exportado
        XM -> exportado

    Estados de la tabla pendiente:
        E -> de exportación (viene de la gestión)
        P -> pendiente
        L -> liquidado
        Para la tabla pendiente estamos usando el campo 'Enviar' (T o F) para saber si borramos dicho registro o no
    */
    fun xmlABaseDatos() {
        val fConfiguracion = Configuracion(fContext)
        val ejercActual: Short = try {
            fConfiguracion.ejercicio()
        } catch (e: Exception) {
            0
        }

        rutaLocal = if (rutaLocal == "") Environment.getExternalStorageDirectory().path + "/alba/recepcion/$fCodTerminal"
        else "$rutaLocal/recepcion/$fCodTerminal"

        val rutarecepcion = File(rutaLocal)
        var nombreFich: String

        // Leemos los ficheros que hemos recibido.
        val xmlFiles = rutarecepcion.listFiles()
        if (xmlFiles != null && xmlFiles.isNotEmpty()) {
            fImportando = true
            // Si no estamos usando el servicio y la fecha del último envío del terminal es mayor que la de la última preparación del PC, no recogeremos,
            // ya que eso implica que el ordenador preparó los datos antes de que el terminal enviara.
            // Así intentamos evitar tener problemas con los contadores, etc.
            if (fDesdeServicio || comprobarFechas(rutaLocal)) {

                // Volvemos a crear todas las tablas si no estamos usando el servicio
                if (!fDesdeServicio) {
                    CrearBD(fContext)
                }

                bd = BaseDatos(fContext)
                dbAlba = bd.writableDatabase
                dbAlba.beginTransaction()
                try {
                    val numArchivos = xmlFiles.size
                    var i = 1

                    // Borraremos aquí los documentos enviados porque puede darse el caso de que hayamos enviado documentos pero
                    // no recibimos desde la central, en cuyo caso no se llamará a importarCabeceras(). Idem con el pendiente.
                    if (fDesdeServicio) {
                        try {
                            dbAlba.execSQL("DELETE FROM lineas WHERE _id IN" +
                                    " (SELECT a._id FROM lineas A" +
                                    " LEFT JOIN cabeceras B ON B._id = A.cabeceraId" +
                                    " WHERE B.estado <> 'N' AND B.estado <> 'P')")

                            dbAlba.delete("cabeceras", "estado<>'N' and estado<>'P'", null)
                            // Dejamos sin borrar aquellos vencimientos que tengamos que enviar (porque los hayamos creado en la tablet)
                            // y aquellos que hemos recibido de la gestión pero hemos cobrado en la tablet y aún no los hemos enviado.
                            val pendienteDao: PendienteDao? = MyDatabase.getInstance(fContext)?.pendienteDao()

                            pendienteDao?.borrarEnviados()

                            // Borramos histRepre porque si no lo recibimos tendremos el histórico anterior
                            val histRepreDao: HistRepreDao? = MyDatabase.getInstance(fContext)?.histRepreDao()
                            histRepreDao?.vaciar()

                            // Idem con proveedores
                            val proveedoresDao: ProveedoresDao? = MyDatabase.getInstance(fContext)?.proveedoresDao()
                            proveedoresDao?.vaciar()

                            // Idem con ofertas
                            val ofertasDao: OfertasDao? = MyDatabase.getInstance(fContext)?.ofertasDao()
                            ofertasDao?.vaciar()

                        } catch (e: Exception) {
                            mostrarExcepcion(e)
                        }
                    }

                    // Bucle que recorre la lista de ficheros.
                    for (file in xmlFiles) {
                        nombreFich = file.name
                        if ((nombreFich != "Clientes.xml") && (nombreFich != "ConClientes.xml") &&
                                (nombreFich != "DirClientes.xml") && (nombreFich != "DtosClientes.xml") &&
                                (nombreFich != "NotasClientes.xml") && (nombreFich != "Proveedores.xml")) {
                            mensajeAActivity(file.name, numArchivos, i)
                            i++
                        }

                        when {
                            nombreFich.equals("Articulos.xml", true) -> importarArticulos()
                            nombreFich.equals("ArticulosHabituales.xml", true) -> importarArtHabituales()
                            nombreFich.equals("Busquedas.xml", true) -> importarBusquedas()
                            // Estos tres archivos no los vaciamos totalmente
                            nombreFich.equals("Facturas.xml", true) -> importarCabeceras("Facturas.xml", TIPODOC_FACTURA.toByte())
                            nombreFich.equals("Albaranes.xml", true) -> importarCabeceras("Albaranes.xml", TIPODOC_ALBARAN.toByte())
                            nombreFich.equals("Pedidos.xml", true) -> importarCabeceras("Pedidos.xml", TIPODOC_PEDIDO.toByte())
                            nombreFich.equals("Presupuestos.xml", true) -> importarCabeceras("Presupuestos.xml", TIPODOC_PRESUPUESTO.toByte())
                            nombreFich.equals("Pendiente.xml", true) -> importarPendiente()
                            nombreFich.equals("FrasDiferidas.xml", true) -> importarFrasDiferidas()

                            nombreFich.equals("CnfTarifas.xml", true) -> importarCnfTarifas()
                            nombreFich.equals("Configuracion.xml", true) -> importarConfiguracion()
                            nombreFich.equals("Divisas.xml", true) -> importarDivisas()
                            nombreFich.equals("FormasPago.xml", true) -> importarFPago()

                            nombreFich.equals("Historico.xml", true) -> importarHco()
                            nombreFich.equals("HistMes.xml", true) -> importarHcoMes()
                            nombreFich.equals("HistRep.xml", true) -> importarHcoRepre()
                            nombreFich.equals("HcoCompSemMes.xml", true) -> importarHcoCompSemMes()
                            nombreFich.equals("HcoPorArticClte.xml", true) -> importarHcoArticClte()
                            nombreFich.equals("EstadDevoluc.xml", true) -> importarEstadDevoluc()

                            nombreFich.equals("Series.xml", true) -> importarSeries(ejercActual)
                            nombreFich.equals("Ejercicios.xml", true) -> importarEjercicios()
                            nombreFich.equals("Ivas.xml", true) -> importarIvas()
                            nombreFich.equals("Ofertas.xml", true) -> importarOfertas()
                            nombreFich.equals("OfertasVol.xml", true) -> importarOfVolumen()
                            nombreFich.equals("OfVolRangos.xml", true) -> importarOfVolRangos()
                            nombreFich.equals("CantOfertas.xml", true) -> importarOfCantRangos()
                            nombreFich.equals("RatingArt.xml", true) -> importarRatingArt()
                            nombreFich.equals("RatingGru.xml", true) -> importarRatingGrupos()
                            nombreFich.equals("RatingPro.xml", true) -> importarRatingProv()
                            nombreFich.equals("Rutas.xml", true) -> importarRutas()
                            nombreFich.equals("Rutero.xml", true) -> importarRutero()
                            nombreFich.equals("Saldos.xml", true) -> importarSaldos()
                            nombreFich.equals("Stock.xml", true) -> importarStock()
                            nombreFich.equals("Tarifas.xml", true) -> importarTarifas()
                            nombreFich.equals("Lotes.xml", true) -> importarLotes()
                            nombreFich.equals("Grupos.xml", true) -> importarGrupos()
                            nombreFich.equals("Departamentos.xml", true) -> importarDepartamentos()
                            nombreFich.equals("Clasificadores.xml", true) -> importarClasificadores()
                            nombreFich.equals("ArticClasif.xml", true) -> importarArticClasif()
                            nombreFich.equals("DatAdicArticulos.xml", true) -> importarDatAdicArtic()
                            nombreFich.equals("Formatos.xml", true) -> importarFormatos()
                            nombreFich.equals("TarifasFormatos.xml", true) -> importarTrfFormatos()
                            nombreFich.equals("TiposInc.xml", true) -> importarTiposIncidencia()
                            nombreFich.equals("Almacenes.xml", true) -> importarAlmacenes()
                            nombreFich.equals("Empresas.xml", true) -> importarEmpresas()
                            nombreFich.equals("Costos.xml", true) -> importarCostos()
                            nombreFich.equals("DocsCabPies.xml", true) -> importarDocsCabPies()
                        }
                        if ((nombreFich != "Clientes.xml") && (nombreFich != "ConClientes.xml") &&
                                (nombreFich != "DirClientes.xml") && (nombreFich != "DtosClientes.xml") &&
                                (nombreFich != "NotasClientes.xml") && (nombreFich != "Proveedores.xml")) {
                            // Borramos el fichero XML de la carpeta de recepción.
                            file.delete()
                        }
                    }

                    // Importamos ahora los clientes y los proveedores
                    clientesABaseDatos(xmlFiles, i)

                } finally {
                    dbAlba.setTransactionSuccessful()
                    dbAlba.endTransaction()
                }

                // Recalculamos stocks
                // Estas llamadas a fConfiguracion.loquesea no pueden estar dentro del bloque beginTransaction - endTransaction, porque
                // al abrir el cursor de fConfiguracion la apk se queda colgada. Entiendo que es porque no podemos tener dos instancias
                // de dbAlba abiertas a la misma vez.
                if (fDesdeServicio) {
                    if (fConfiguracion.controlarStock())
                        recalcularStocks()
                    if (fConfiguracion.usarTrazabilidad())
                        recalcularLotes()

                    // Borramos los cobros enviados anteriores a x días
                    borrarCobrosEnviados(fConfiguracion)
                }

                // Hemos decidido borrar la carpeta de PDF'S en este momento.
                borrarPDFS()

                fImportando = false

            } else {
                val msgFNoValida = Message()
                msgFNoValida.obj = '\n' + fContext.getString(string.msj_FPrepNoValida)
                puente.sendMessage(msgFNoValida)
                fImportando = false
            }
        } else {
            val msgSinFich = Message()
            msgSinFich.obj = "No se encontraron ficheros en la carpeta de recepción"
            puente.sendMessage(msgSinFich)
            fImportando = false
        }
    }


    private fun clientesABaseDatos(xmlFiles: Array<File>, i: Int) {

        val numArchivos = xmlFiles.size
        var x = i

        for (file in xmlFiles) {
            if (file.exists()) {
                val nombreFich = file.name
                mensajeAActivity(nombreFich, numArchivos, x)
                x++

                when {
                    nombreFich.equals("Clientes.xml", true) -> importarClientes()
                    nombreFich.equals("ConClientes.xml", true) -> importarContactos()
                    nombreFich.equals("DirClientes.xml", true) -> importarDirecciones()
                    nombreFich.equals("DtosClientes.xml", true) -> importarDtosCltes()
                    nombreFich.equals("Proveedores.xml", true) -> importarProveedores()
                    nombreFich.equals("NotasClientes.xml", true) -> importarNotasCltes()
                }
                // Borramos el fichero XML de la carpeta de recepción.
                file.delete()
            }
        }
    }

    private fun mensajeAActivity(nombreFich: String, numArchivos: Int, i: Int) {
        val queMensaje = Message()
        queMensaje.obj = msjRec + nombreFich
        queMensaje.arg1 = numArchivos
        queMensaje.arg2 = ((i * 100) / numArchivos)
        puente.sendMessage(queMensaje)
    }


    private fun mostrarExcepcion(e: Exception) {
        val msgExcept = Message()
        msgExcept.obj = e.message
        puente.sendMessage(msgExcept)
    }


    @SuppressLint("SimpleDateFormat")
    private fun comprobarFechas(rutaLocal: String): Boolean {
        val fUltEnvio = prefs.getString("fecha_ult_envio", "") ?: ""
        var sCampo: String
        // Si no hemos enviado nunca, devolvemos true.
        if (fUltEnvio == "") return true
        else {
            try {
                val f = File(rutaLocal, "FPreparacion.xml")
                val fin = FileInputStream(f)

                val parser = Xml.newPullParser()
                try {
                    parser.setInput(fin, "UTF-8")
                    var event = parser.next()

                    while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                        if (event == XmlPullParser.START_TAG) {

                            for (i: Int in 0 until parser.attributeCount) {
                                sCampo = parser.getAttributeName(i)

                                if (sCampo.equals("FECHA_PREPARACION", true)) {
                                    val fUltPreparacion = parser.getAttributeValue("", sCampo) ?: ""

                                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                                    val dFUltPreparacion = sdf.parse(fUltPreparacion) ?: Date()
                                    val dFUltEnvio = sdf.parse(fUltEnvio)

                                    return (dFUltPreparacion.after(dFUltEnvio))
                                }
                            }
                        }
                        event = parser.next()
                    }
                    fin.close()
                    return false

                } catch (e: Exception) {
                    mostrarExcepcion(e)
                    return false
                }
            } catch (e: Exception) {
                mostrarExcepcion(e)
                return false
            }
        }
    }


    private fun borrarPDFS() {
        var rutaPdfs = prefs.getString("rutacomunicacion", "") ?: ""
        if (rutaPdfs == "")
            rutaPdfs = "/storage/sdcard0/alba/pdfs/"
        else
            rutaPdfs += "/pdfs/"

        val carpetaPdfs = File(rutaPdfs)

        if (carpetaPdfs.exists()) {
            val pdfFiles = carpetaPdfs.listFiles()
            for (file in pdfFiles) {
                file.delete()
            }
        }
    }


    private fun importarArticulos() {
        val articulosDao: ArticulosDao? = MyDatabase.getInstance(fContext)?.articulosDao()
        val f = File(rutaLocal, "Articulos.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                articulosDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val articEnt = ArticulosEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Articulo", true) -> articEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Codigo", true) -> articEnt.codigo = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Descr", true) -> articEnt.descripcion = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Iva", true) -> articEnt.tipoiva = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Prov", true) -> articEnt.proveedorId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("UCaja", true) -> articEnt.uCaja = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Flag1", true) -> articEnt.flag1 = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Faag2", true) -> articEnt.flag2 = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Medida", true) -> articEnt.medida = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Grupo", true) -> articEnt.grupoId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Dpto", true) -> articEnt.departamentoId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Peso", true) -> articEnt.peso = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Costo", true) -> articEnt.costo = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Tasa1", true) -> articEnt.tasa1 = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Tasa2", true) -> articEnt.tasa2 = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Enlace", true) -> articEnt.enlace = parser.getAttributeValue("", sCampo).toInt()
                            }
                        }
                        if (articEnt.articuloId > 0)
                            articulosDao?.insertar(articEnt)
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }



    private fun importarClientes() {
        val f = File(rutaLocal, "Clientes.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val values = ContentValues()
            val parser = Xml.newPullParser()
            try {
                // Si recibimos desde el servicio dejaremos sin borrar aquellos clientes nuevos, los modificados
                // y los que estén en algún documento sin enviar. Luego los incorporaremos a los que recibimos desde la central.
                if (fDesdeServicio) {
                    dbAlba.execSQL("DELETE FROM clientes WHERE (estado IS NULL OR (estado<>'N' AND estado<>'M'))" +
                            " AND cliente NOT IN (SELECT cliente FROM cabeceras WHERE estado = 'N' OR estado = 'P')")
                    clientes2TemporalCltes()
                }
                dbAlba.delete("clientes", "1=1", null)

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("CLIENTE", ignoreCase = true) -> values.put("cliente", parser.getAttributeValue("", sCampo))
                                sCampo.equals("CODIGO", ignoreCase = true) -> values.put("codigo", parser.getAttributeValue("", sCampo))
                                sCampo.equals("NOMFI", ignoreCase = true) -> values.put("nomfi", parser.getAttributeValue("", sCampo))
                                sCampo.equals("NOMCO", ignoreCase = true) -> values.put("nomco", parser.getAttributeValue("", sCampo))
                                sCampo.equals("CIF", ignoreCase = true) -> values.put("cif", parser.getAttributeValue("", sCampo))
                                sCampo.equals("DIRECC", ignoreCase = true) -> values.put("direcc", parser.getAttributeValue("", sCampo))
                                sCampo.equals("LOCALI", ignoreCase = true) -> values.put("locali", parser.getAttributeValue("", sCampo))
                                sCampo.equals("CPOSTAL", ignoreCase = true) -> values.put("cpostal", parser.getAttributeValue("", sCampo))
                                sCampo.equals("PROVIN", ignoreCase = true) -> values.put("provin", parser.getAttributeValue("", sCampo))
                                sCampo.equals("APLIVA", ignoreCase = true) -> values.put("apliva", parser.getAttributeValue("", sCampo))
                                sCampo.equals("APLREC", ignoreCase = true) -> values.put("aplrec", parser.getAttributeValue("", sCampo))
                                sCampo.equals("IVA", ignoreCase = true) -> values.put("tipoiva", parser.getAttributeValue("", sCampo))
                                sCampo.equals("TARIFA", ignoreCase = true) -> values.put("tarifa", parser.getAttributeValue("", sCampo))
                                sCampo.equals("TARDTO", ignoreCase = true) -> values.put("tardto", parser.getAttributeValue("", sCampo))
                                sCampo.equals("TarifaPiezas", true) -> values.put("tarifaPiezas", parser.getAttributeValue("", sCampo))
                                sCampo.equals("FPAGO", ignoreCase = true) -> values.put("fpago", parser.getAttributeValue("", sCampo))
                                sCampo.equals("RIESGO", ignoreCase = true) -> values.put("riesgo", parser.getAttributeValue("", sCampo))
                                sCampo.equals("RUTA", ignoreCase = true) -> values.put("ruta", parser.getAttributeValue("", sCampo))
                                sCampo.equals("FLAG", ignoreCase = true) -> values.put("flag", parser.getAttributeValue("", sCampo))
                                sCampo.equals("FLAG2", ignoreCase = true) -> values.put("flag2", parser.getAttributeValue("", sCampo))
                                sCampo.equals("RAMO", ignoreCase = true) -> values.put("ramo", parser.getAttributeValue("", sCampo))
                                sCampo.equals("PENDIENTE", ignoreCase = true) -> values.put("pendiente", parser.getAttributeValue("", sCampo))
                                sCampo.equals("MAXDIAS", ignoreCase = true) -> values.put("maxdias", parser.getAttributeValue("", sCampo))
                                sCampo.equals("MAXPENDIENTES", ignoreCase = true) -> values.put("maxfraspdtes", parser.getAttributeValue("", sCampo))
                            }
                        }
                        // Llenamos el campo "tieneincid" a falso.
                        values.put("tieneincid", "F")
                        if (values.getAsString("cliente") != null) {
                            dbAlba.insertWithOnConflict("clientes", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                        }
                        values.clear()
                    }
                    event = parser.next()
                }
                fin.close()
                // Realizamos el proceso contrario: copiamos desde la tabla temporal a la de clientes
                if (fDesdeServicio)
                    temporalCltes2Cltes()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun temporalCltes2Cltes() {
        val tempCltesDao: TempCltesDao? = MyDatabase.getInstance(fContext)?.tempCltesDao()
        val lTemp = tempCltesDao?.getAllCltes() ?: emptyList<TempCltesEnt>().toMutableList()

        val values = ContentValues()
        for (tempClte in lTemp) {
            values.put("cliente", tempClte.clienteId)
            values.put("codigo", tempClte.codigo)
            values.put("nomfi", tempClte.nombre)
            values.put("nomco", tempClte.nombreComercial)
            values.put("cif", tempClte.cif)
            values.put("direcc", tempClte.direccion)
            values.put("locali", tempClte.localidad)
            values.put("cpostal", tempClte.cPostal)
            values.put("provin", tempClte.provincia)
            values.put("apliva", tempClte.aplIva)
            values.put("aplrec", tempClte.aplRec)
            values.put("tipoiva", tempClte.tipoIva)
            values.put("tarifa", tempClte.tarifaId)
            values.put("tardto", tempClte.tarifaDtoId)
            values.put("fpago", tempClte.fPago)
            values.put("ruta", tempClte.rutaId)
            values.put("riesgo", tempClte.riesgo)
            values.put("pendiente", tempClte.pendiente)
            values.put("flag", tempClte.flag)
            values.put("estado", tempClte.estado)
            values.put("flag2", tempClte.flag2)
            values.put("ramo", tempClte.ramo)
            values.put("numexport", tempClte.numExport)
            values.put("tieneincid", tempClte.tieneIncid)
            values.put("maxdias", tempClte.maxDias)
            values.put("maxfraspdtes", tempClte.maxFrasPdtes)

            val cCliente = dbAlba.rawQuery("SELECT cliente FROM clientes WHERE cliente = " + tempClte.clienteId, null)
            val existe = cCliente.moveToFirst()
            cCliente.close()

            if (tempClte.estado == "N") {
                // Si ya hemos recibido un cliente con el mismo valor en el campo "Cliente", no lo añadimos.
                // Damos prioridad a lo que venga desde el central.
                if (!existe)
                    dbAlba.insert("clientes", null, values)

            } else {
                if (existe)
                    dbAlba.update("clientes", values, "codigo=" + tempClte.codigo, null)
                // Puede ser que el cliente que hemos modificado no lo estemos reciendo ahora desde la central, por eso añadimos.
                else
                    dbAlba.insert("clientes", null, values)
            }
        }
        // Recalculamos los saldos, porque hemos podido dejar registros en la tabla "Pendiente" sin enviar.
        // Por ahora entendemos que no hace falta recalcular el pendiente.
        recalcularSaldos()
    }

    private fun recalcularStocks() {
        val fArticulos = ArticulosClase(fContext)

        // Sumamos al stock las líneas de los documentos no enviados
        val cCabeceras = dbAlba.rawQuery("SELECT A.articulo, A.cajas, A.cantidad, B.empresa FROM lineas A" +
                " LEFT JOIN cabeceras B ON B._id = A.cabeceraId" +
                " WHERE (B.estado = 'N' OR B.estado = 'P') AND B.tipodoc <> " + TIPODOC_PEDIDO, null)

        cCabeceras.use {
            it.moveToFirst()
            while (!it.isAfterLast) {
                val queArticulo = it.getInt(it.getColumnIndex("articulo"))
                val queEmpresa = it.getShort(it.getColumnIndex("empresa"))
                val sCajas = it.getString(it.getColumnIndex("cajas")) ?: "0.0"
                val dCajas: Double = if (sCajas == "") 0.0
                else sCajas.toDouble()

                val sCantidad = it.getString(it.getColumnIndex("cantidad")) ?: "0.0"
                val dCantidad: Double = if (sCantidad == "") 0.0
                else sCantidad.toDouble()

                fArticulos.actualizarStock(queArticulo, queEmpresa, dCantidad, dCajas, false)
                it.moveToNext()
            }
        }

        // Sumamos ahora al stock las cargas no enviadas. También aprovechamos y borramos las cargas que estén enviadas.
        dbAlba.execSQL("DELETE FROM cargasLineas WHERE _id IN" +
                " (SELECT a._id FROM cargasLineas A" +
                " LEFT JOIN cargas B ON B.cargaId = A.cargaId" +
                " WHERE B.estado <> 'N')")
        dbAlba.delete("cargas", "estado<>'N'", null)

        val cCargas = dbAlba.rawQuery("SELECT A.articulo, A.cajas, A.cantidad, B.empresa FROM cargasLineas A" +
                " LEFT JOIN cargas B ON B.cargaId = A.cargaId" +
                " WHERE B.estado = 'N'", null)

        cCargas.use {
            it.moveToFirst()
            while (!it.isAfterLast) {
                val queArticulo = it.getInt(it.getColumnIndex("articulo"))
                val queEmpresa = it.getShort(it.getColumnIndex("empresa"))
                val sCajas = it.getString(it.getColumnIndex("cajas")) ?: "0.0"
                val dCajas: Double = if (sCajas == "") 0.0
                else sCajas.toDouble() * -1

                val sCantidad = it.getString(it.getColumnIndex("cantidad")) ?: "0.0"
                val dCantidad: Double = if (sCantidad == "") 0.0
                else sCantidad.toDouble() * -1

                fArticulos.actualizarStock(queArticulo, queEmpresa, dCantidad, dCajas, false)
                it.moveToNext()
            }
        }

        fArticulos.close()
    }

    private fun recalcularLotes() {
        val fLotes = LotesClase(fContext)

        // Recalculamos los lotes de las líneas de venta no enviadas
        val cCabeceras = dbAlba.rawQuery("SELECT A.articulo, A.cantidad, A.lote, B.empresa FROM lineas A" +
                " LEFT JOIN cabeceras B ON B._id = A.cabeceraId" +
                " WHERE (B.estado = 'N' OR B.estado = 'P') AND B.tipodoc <> " + TIPODOC_PEDIDO, null)

        cCabeceras.use {
            it.moveToFirst()
            while (!it.isAfterLast) {
                val queArticulo = it.getInt(it.getColumnIndex("articulo"))
                val sCantidad = it.getString(it.getColumnIndex("cantidad"))
                val dCantidad: Double = if (sCantidad == "") 0.0
                else sCantidad.toDouble()

                val queLote = it.getString(it.getColumnIndex("lote"))
                val queEmpresa = it.getShort(it.getColumnIndex("empresa"))

                fLotes.actStockLote(queArticulo, dCantidad, queLote, queEmpresa)
                it.moveToNext()
            }
        }

        // Recalculamos los lotes de las cargas no enviadas
        val cCargas = dbAlba.rawQuery("SELECT A.articulo, A.cantidad, A.lote, B.empresa FROM cargasLineas A" +
                " LEFT JOIN cargas B ON B.cargaId = A.cargaId" +
                " WHERE B.estado = 'N'", null)

        cCargas.use {
            it.moveToFirst()
            while (!it.isAfterLast) {
                val queArticulo = it.getInt(it.getColumnIndex("articulo"))
                val sCantidad = it.getString(it.getColumnIndex("cantidad"))
                val dCantidad: Double = if (sCantidad == "") 0.0
                else sCantidad.toDouble() * -1

                val queLote = it.getString(it.getColumnIndex("lote"))
                val queEmpresa = it.getShort(it.getColumnIndex("empresa"))

                fLotes.actStockLote(queArticulo, dCantidad, queLote, queEmpresa)
                it.moveToNext()
            }
        }
    }


    private fun recalcularSaldos() {
        val pendienteDao: PendienteDao? = MyDatabase.getInstance(fContext)?.pendienteDao()

        val listaPend = pendienteDao?.getPendienteEnviar() ?: emptyList<PendienteEnt>().toMutableList()

        // Si el cliente es nuevo no tendrá ningún registro en la tabla "Saldos", por eso también los recalculamos
        for (pendiente in listaPend) {
            actualizarSaldo(fContext, pendiente.clienteId, pendiente.empresa, pendiente.importe.toDouble())
        }
    }


    private fun borrarCobrosEnviados(fConfiguracion: Configuracion) {
        val cobrosDao: CobrosDao? = MyDatabase.getInstance(fContext)?.cobrosDao()

        val diasMantCobros = fConfiguracion.diasMantCobros() * -1
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, diasMantCobros)
        val fHastaFecha = ponerCeros(calendar.get(Calendar.DAY_OF_MONTH).toString(), 2) + '/' +
                ponerCeros((calendar.get(Calendar.MONTH) + 1).toString(), 2) + '/' +
                calendar.get(Calendar.YEAR).toString()

        cobrosDao?.borrarCobrosEnviados(fechaEnJulian(fHastaFecha))
    }



    private fun clientes2TemporalCltes() {
        val tempCltesDao: TempCltesDao? = MyDatabase.getInstance(fContext)?.tempCltesDao()
        tempCltesDao?.vaciar()

        val cClientes = dbAlba.rawQuery("SELECT * FROM clientes", null)
        cClientes.moveToFirst()
        while (!cClientes.isAfterLast) {
            val tempClteEnt = TempCltesEnt()
            tempClteEnt.clienteId = cClientes.getInt(cClientes.getColumnIndex("cliente"))
            tempClteEnt.codigo = cClientes.getInt(cClientes.getColumnIndex("codigo"))
            tempClteEnt.nombre = cClientes.getString(cClientes.getColumnIndex("nomfi"))
            tempClteEnt.nombreComercial = cClientes.getString(cClientes.getColumnIndex("nomco"))
            tempClteEnt.cif = cClientes.getString(cClientes.getColumnIndex("cif"))
            tempClteEnt.direccion = cClientes.getString(cClientes.getColumnIndex("direcc"))
            tempClteEnt.localidad = cClientes.getString(cClientes.getColumnIndex("locali"))
            tempClteEnt.cPostal = cClientes.getString(cClientes.getColumnIndex("cpostal"))
            tempClteEnt.provincia = cClientes.getString(cClientes.getColumnIndex("provin"))
            tempClteEnt.aplIva = cClientes.getString(cClientes.getColumnIndex("apliva"))
            tempClteEnt.aplRec = cClientes.getString(cClientes.getColumnIndex("aplrec"))
            tempClteEnt.tipoIva = cClientes.getShort(cClientes.getColumnIndex("tipoiva"))
            tempClteEnt.tarifaId = cClientes.getShort(cClientes.getColumnIndex("tarifa"))
            tempClteEnt.tarifaDtoId = cClientes.getShort(cClientes.getColumnIndex("tardto"))
            tempClteEnt.fPago = cClientes.getString(cClientes.getColumnIndex("fpago"))
            tempClteEnt.rutaId = cClientes.getShort(cClientes.getColumnIndex("ruta"))
            tempClteEnt.riesgo = cClientes.getString(cClientes.getColumnIndex("riesgo"))
            tempClteEnt.pendiente = cClientes.getString(cClientes.getColumnIndex("pendiente"))
            tempClteEnt.flag = cClientes.getInt(cClientes.getColumnIndex("flag"))
            tempClteEnt.flag2 = cClientes.getInt(cClientes.getColumnIndex("flag2"))
            tempClteEnt.estado = cClientes.getString(cClientes.getColumnIndex("estado"))
            tempClteEnt.ramo = cClientes.getShort(cClientes.getColumnIndex("ramo"))
            tempClteEnt.numExport = cClientes.getInt(cClientes.getColumnIndex("numexport"))
            tempClteEnt.tieneIncid = cClientes.getString(cClientes.getColumnIndex("tieneincid"))
            tempClteEnt.maxDias = cClientes.getInt(cClientes.getColumnIndex("maxdias"))
            tempClteEnt.maxFrasPdtes = cClientes.getInt(cClientes.getColumnIndex("maxfraspdtes"))

            tempCltesDao?.insertar(tempClteEnt)
            cClientes.moveToNext()
        }
        cClientes.close()
    }

    private fun importarArtHabituales() {
        val artHabitualesDao: ArtHabitualesDao? = MyDatabase.getInstance(fContext)?.artHabitualesDao()
        val f = File(rutaLocal, "ArticulosHabituales.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                artHabitualesDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val artHabitualEnt = ArtHabitualesEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Articulo", ignoreCase = true) -> artHabitualEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Cliente", ignoreCase = true) -> artHabitualEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Formato", ignoreCase = true) -> artHabitualEnt.formatoId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Flag", ignoreCase = true) -> artHabitualEnt.flag = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Texto", ignoreCase = true) -> artHabitualEnt.texto = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (artHabitualEnt.articuloId > 0) {
                            artHabitualesDao?.insertar(artHabitualEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarBusquedas() {
        val busquedasDao: BusquedasDao? = MyDatabase.getInstance(fContext)?.busquedasDao()
        val f = File(rutaLocal, "Busquedas.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                busquedasDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val busquedaEnt = BusquedasEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Clave", ignoreCase = true) -> busquedaEnt.clave = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Articulo", ignoreCase = true) -> busquedaEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Tipo", ignoreCase = true) -> busquedaEnt.tipo = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("TCaja", ignoreCase = true) -> busquedaEnt.tcaja = parser.getAttributeValue("", sCampo)
                                sCampo.equals("UCaja", ignoreCase = true) -> busquedaEnt.ucaja = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (busquedaEnt.clave != "") {
                            busquedasDao?.insertar(busquedaEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarFrasDiferidas() {
        val cabDiferidasDao: CabDiferidasDao? = MyDatabase.getInstance(fContext)?.cabDiferidasDao()
        val linDiferidasDao: LineasDifDao? = MyDatabase.getInstance(fContext)?.lineasDifDao()
        val f = File(rutaLocal, "FrasDiferidas.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                cabDiferidasDao?.vaciar()
                linDiferidasDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()
                val cabeceraId: Long = 0

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        if (parser.name == "registro") {
                            val cabDifEnt = CabDiferidasEnt()
                            for (i in 0 until parser.attributeCount) {
                                sCampo = parser.getAttributeName(i)

                                when {
                                    sCampo.equals("Serie", ignoreCase = true) -> cabDifEnt.serie = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Numero", ignoreCase = true) -> cabDifEnt.numero = parser.getAttributeValue("", sCampo).toInt()
                                    sCampo.equals("Ejercicio", ignoreCase = true) -> cabDifEnt.ejercicio = parser.getAttributeValue("", sCampo).toShort()
                                    sCampo.equals("Empresa", ignoreCase = true) -> cabDifEnt.empresa = parser.getAttributeValue("", sCampo).toInt()
                                    sCampo.equals("Fecha", ignoreCase = true) -> {
                                        val sFecha = parser.getAttributeValue("", sCampo)
                                        val sFecha2 = (sFecha.substring(8, 10) + "/" + sFecha.substring(5, 7) + "/" + sFecha.substring(0, 4))
                                        cabDifEnt.fecha = sFecha2
                                    }
                                    sCampo.equals("Cliente", ignoreCase = true) -> cabDifEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                    sCampo.equals("AplIva", ignoreCase = true) -> cabDifEnt.aplIva = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("AplRec", ignoreCase = true) -> cabDifEnt.aplRec = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Bruto", ignoreCase = true) -> cabDifEnt.bruto = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Base", ignoreCase = true) -> cabDifEnt.base = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Dto", ignoreCase = true) -> cabDifEnt.dto = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Iva", ignoreCase = true) -> cabDifEnt.iva = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Recargo", ignoreCase = true) -> cabDifEnt.recargo = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Total", ignoreCase = true) -> cabDifEnt.total = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Flag", ignoreCase = true) -> cabDifEnt.flag = parser.getAttributeValue("", sCampo).toInt()
                                    sCampo.equals("Obs1", ignoreCase = true) -> cabDifEnt.obs1 = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Obs2", ignoreCase = true) -> cabDifEnt.obs2 = parser.getAttributeValue("", sCampo)
                                }
                            }
                            if (cabDifEnt.clienteId > 0) {
                                cabDiferidasDao?.insertar(cabDifEnt)
                            }
                        } else if (parser.name == "linea") {
                            val linDifEnt = LineasDifEnt()
                            for (i in 0 until parser.attributeCount) {
                                sCampo = parser.getAttributeName(i)

                                when {
                                    sCampo.equals("Serie", ignoreCase = true) -> linDifEnt.serie = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Numero", ignoreCase = true) -> linDifEnt.numero = parser.getAttributeValue("", sCampo).toInt()
                                    sCampo.equals("Fecha", ignoreCase = true) -> {
                                        val sFecha = parser.getAttributeValue("", sCampo)
                                        val sFecha2 = (sFecha.substring(8, 10) + "/" + sFecha.substring(5, 7) + "/" + sFecha.substring(0, 4))
                                        linDifEnt.fecha = sFecha2
                                    }
                                    sCampo.equals("Linea", ignoreCase = true) -> linDifEnt.linea = parser.getAttributeValue("", sCampo).toShort()
                                    sCampo.equals("Articulo", ignoreCase = true) -> linDifEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                    sCampo.equals("Codigo", ignoreCase = true) -> linDifEnt.codigo = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Descripcion", ignoreCase = true) -> linDifEnt.descripcion = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Precio", ignoreCase = true) -> linDifEnt.precio = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Cajas", ignoreCase = true) -> linDifEnt.cajas = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Cantidad", ignoreCase = true) -> linDifEnt.cantidad = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Importe", ignoreCase = true) -> linDifEnt.importe = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Dto", ignoreCase = true) -> linDifEnt.dto = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Piezas", ignoreCase = true) -> linDifEnt.piezas = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("TipoIva", ignoreCase = true) -> linDifEnt.codigoIva = parser.getAttributeValue("", sCampo).toShort()
                                    sCampo.equals("Flag", ignoreCase = true) -> linDifEnt.flag = parser.getAttributeValue("", sCampo).toInt()
                                    sCampo.equals("Flag3", ignoreCase = true) -> linDifEnt.flag3 = parser.getAttributeValue("", sCampo).toInt()
                                    sCampo.equals("Formato", ignoreCase = true) -> linDifEnt.formatoId = parser.getAttributeValue("", sCampo).toShort()
                                }
                            }
                            linDifEnt.cabeceraId = cabeceraId.toInt()
                            if (linDifEnt.cabeceraId > 0) {
                                linDiferidasDao?.insertar(linDifEnt)
                            }
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }

    // Estados de las cabeceras:
    // N -> nuevo
    // P -> guardado
    // R -> reenviar (no se usa con servicio)
    // X -> enviado
    private fun importarCabeceras(nombreFichXMl: String, queTipoDoc: Byte) {
        val f = File(rutaLocal, nombreFichXMl)
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val values = ContentValues()
            val parser = Xml.newPullParser()
            try {
                parser.setInput(fin, "UTF-8")
                var event = parser.next()
                var cabeceraId: Long = 0

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        if (parser.name == "registro") {
                            // Hay tablas donde el camo Facturado no viene, por eso lo inicializamos a "F".
                            values.put("facturado", "F")
                            values.put("tipodoc", queTipoDoc)

                            for (i in 0 until parser.attributeCount) {
                                sCampo = parser.getAttributeName(i)

                                when {
                                    sCampo.equals("Facturado", ignoreCase = true) -> values.put("facturado", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Almacen", ignoreCase = true) -> values.put("alm", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Serie", ignoreCase = true) -> values.put("serie", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Numero", ignoreCase = true) -> values.put("numero", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Ejercicio", ignoreCase = true) -> values.put("ejer", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Empresa", ignoreCase = true) -> values.put("empresa", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Fecha", ignoreCase = true) -> {
                                        val sFecha = parser.getAttributeValue("", sCampo)
                                        val sFecha2 = (sFecha.substring(8, 10) + "/"
                                                + sFecha.substring(5, 7) + "/" + sFecha.substring(0, 4))
                                        values.put("fecha", sFecha2)
                                    }
                                    sCampo.equals("Cliente", ignoreCase = true) -> values.put("cliente", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("AplicarIva", ignoreCase = true) -> values.put("apliva", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("AplicarRecargo", ignoreCase = true) -> values.put("aplrec", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Bruto", ignoreCase = true) -> values.put("bruto", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Base", ignoreCase = true) -> values.put("base", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Dto", ignoreCase = true) -> values.put("dto", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Iva", ignoreCase = true) -> values.put("iva", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Recargo", ignoreCase = true) -> values.put("recargo", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Total", ignoreCase = true) -> values.put("total", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Estado", ignoreCase = true) -> {
                                        values.put("estado", parser.getAttributeValue("", sCampo))
                                        values.put("estadoinicial", parser.getAttributeValue("", sCampo))
                                    }
                                    sCampo.equals("Flag", ignoreCase = true) -> values.put("flag", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Observ1", ignoreCase = true) -> values.put("obs1", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Observ2", ignoreCase = true) -> values.put("obs2", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Hoja", ignoreCase = true) -> values.put("hoja", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Orden", ignoreCase = true) -> values.put("orden", parser.getAttributeValue("", sCampo))
                                }
                            }
                            // Llenamos el campo "firmado" a falso.
                            values.put("firmado", "F")

                            cabeceraId = dbAlba.insertWithOnConflict("cabeceras", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                            values.clear()

                        } else if (parser.name == "linea") {
                            for (i in 0 until parser.attributeCount) {
                                sCampo = parser.getAttributeName(i)

                                when {
                                    sCampo.equals("Linea", ignoreCase = true) -> values.put("linea", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Articulo", ignoreCase = true) -> values.put("articulo", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Codigo", ignoreCase = true) -> values.put("codigo", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Descripcion", ignoreCase = true) -> values.put("descr", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Tarifa", ignoreCase = true) -> values.put("tarifa", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Precio", ignoreCase = true) -> values.put("precio", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Importe", ignoreCase = true) -> values.put("importe", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Cajas", ignoreCase = true) -> values.put("cajas", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Cantidad", ignoreCase = true) -> values.put("cantidad", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Piezas", ignoreCase = true) -> values.put("piezas", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Dto", ignoreCase = true) -> values.put("dto", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("TipoIva", ignoreCase = true) -> values.put("codigoiva", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Flag", ignoreCase = true) -> values.put("flag", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Flag3", ignoreCase = true) -> values.put("flag3", parser.getAttributeValue("", sCampo))
                                    sCampo.equals("Formato", ignoreCase = true) -> values.put("formato", parser.getAttributeValue("", sCampo))
                                }
                            }
                            values.put("cabeceraId", cabeceraId)

                            dbAlba.insertWithOnConflict("lineas", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                            values.clear()
                        }
                    }
                    event = parser.next()
                }
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }



    private fun importarDtosCltes() {
        val f = File(rutaLocal, "DtosClientes.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val values = ContentValues()
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                dbAlba.delete("dtoscltes", "1=1", null)

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("CLIENTE", true) -> values.put("cliente", parser.getAttributeValue("", sCampo))
                                sCampo.equals("IDDESCUENTO", true) -> values.put("iddescuento", parser.getAttributeValue("", sCampo))
                                sCampo.equals("DTO", true) -> values.put("dto", parser.getAttributeValue("", sCampo))
                            }
                        }
                        if (values.getAsString("cliente") != null) {
                            dbAlba.insertWithOnConflict("dtoscltes", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                        }
                        values.clear()
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarCnfTarifas() {
        val f = File(rutaLocal, "CnfTarifas.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val values = ContentValues()
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                dbAlba.delete("cnftarifas", "1=1", null)

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("CODIGO", true) -> values.put("codigo", parser.getAttributeValue("", sCampo))
                                sCampo.equals("TARIFA", true) -> values.put("tarifa", parser.getAttributeValue("", sCampo))
                                sCampo.equals("PRECIOS", true) -> values.put("precios", parser.getAttributeValue("", sCampo))
                                sCampo.equals("FLAG", true) -> values.put("flag", parser.getAttributeValue("", sCampo))
                            }
                        }
                        if (values.getAsString("codigo") != null) {
                            dbAlba.insertWithOnConflict("cnftarifas", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                        }
                        values.clear()
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarContactos() {
        val contactosDao: ContactosCltesDao? = MyDatabase.getInstance(fContext)?.contactosCltesDao()
        val f = File(rutaLocal, "ConClientes.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                contactosDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val contactoEnt = ContactosCltesEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Cliente", true) -> contactoEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Alm", true) -> contactoEnt.almacen = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Orden", true) -> contactoEnt.orden = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Sucursal", true) -> contactoEnt.sucursal = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Contacto", true) -> contactoEnt.nombre = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Tel1", true) -> contactoEnt.telefono1 = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Tel2", true) -> contactoEnt.telefono2 = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Obs1", true) -> contactoEnt.obs1 = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Email", true) -> contactoEnt.eMail = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Flag", true) -> contactoEnt.flag = parser.getAttributeValue("", sCampo).toInt()
                            }
                        }
                        if (contactoEnt.clienteId > 0) {
                            contactosDao?.insertar(contactoEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarConfiguracion() {
        val configuracionDao: ConfiguracionDao? = MyDatabase.getInstance(fContext)?.configuracionDao()
        val f = File(rutaLocal, "Configuracion.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                configuracionDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val configuracionEnt = ConfiguracionEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Grupo", true) -> configuracionEnt.grupo = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Descr", true) -> configuracionEnt.descripcion = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Valor", true) -> configuracionEnt.valor = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (configuracionEnt.grupo > 0) {
                            configuracionDao?.insertar(configuracionEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }

    private fun importarTiposIncidencia() {
        val tiposIncDao: TiposIncDao? = MyDatabase.getInstance(fContext)?.tiposIncDao()
        val f = File(rutaLocal, "TiposInc.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                tiposIncDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val tipoIncEnt = TiposIncEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Codigo", ignoreCase = true) -> tipoIncEnt.tipoIncId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Descr", ignoreCase = true) -> tipoIncEnt.descripcion = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (tipoIncEnt.tipoIncId > 0) {
                            tiposIncDao?.insertar(tipoIncEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarRatingProv() {
        val ratingProvDao: RatingProvDao? = MyDatabase.getInstance(fContext)?.ratingProvDao()
        val f = File(rutaLocal, "RatingPro.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                ratingProvDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val ratingEnt = RatingProvEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Prov", ignoreCase = true) -> ratingEnt.proveedorId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Alm", ignoreCase = true) -> ratingEnt.almacen = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Cliente", ignoreCase = true) -> ratingEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Ramo", ignoreCase = true) -> ratingEnt.ramoId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Tarifa", ignoreCase = true) -> ratingEnt.tarifa = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Inicio", ignoreCase = true) -> ratingEnt.inicio = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Fin", ignoreCase = true) -> ratingEnt.fin = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Dto", ignoreCase = true) -> ratingEnt.dto = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (ratingEnt.proveedorId > 0) {
                            ratingProvDao?.insertar(ratingEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarNotasCltes() {
        val notasCltesDao: NotasCltesDao? = MyDatabase.getInstance(fContext)?.notasCltesDao()
        val f = File(rutaLocal, "NotasClientes.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                if (fDesdeServicio) {
                    notasCltesDao?.borrarViejas()
                } else {
                    notasCltesDao?.vaciar()
                }

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val notaEnt = NotasCltesEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Cliente", true) -> notaEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Cadena", true) -> notaEnt.nota = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Fecha", true) -> notaEnt.fecha = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (notaEnt.clienteId > 0) {
                            notasCltesDao?.vaciar()
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarDirecciones() {
        val f = File(rutaLocal, "DirClientes.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val values = ContentValues()
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                dbAlba.delete("dirclientes", "1=1", null)

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("CLIENTE", true) -> values.put("cliente", parser.getAttributeValue("", sCampo))
                                sCampo.equals("ALM", true) -> values.put("alm", parser.getAttributeValue("", sCampo))
                                sCampo.equals("ORDEN", true) -> values.put("orden", parser.getAttributeValue("", sCampo))
                                sCampo.equals("SUCURSAL", true) -> values.put("sucursal", parser.getAttributeValue("", sCampo))
                                sCampo.equals("DIRECC", true) -> values.put("direcc", parser.getAttributeValue("", sCampo))
                                sCampo.equals("POBLAC", true) -> values.put("poblac", parser.getAttributeValue("", sCampo))
                                sCampo.equals("CPOSTAL", true) -> values.put("cpostal", parser.getAttributeValue("", sCampo))
                                sCampo.equals("PROVIN", true) -> values.put("provin", parser.getAttributeValue("", sCampo))
                                sCampo.equals("PAIS", true) -> values.put("pais", parser.getAttributeValue("", sCampo))
                                sCampo.equals("DIRDOC", true) -> values.put("dirdoc", parser.getAttributeValue("", sCampo))
                                sCampo.equals("DIRMER", true) -> values.put("dirmer", parser.getAttributeValue("", sCampo))
                            }
                        }
                        if (values.getAsString("cliente") != null) {
                            dbAlba.insertWithOnConflict("dirclientes", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                        }
                        values.clear()
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }



    private fun importarHco() {
        val f = File(rutaLocal, "Historico.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val values = ContentValues()
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                dbAlba.delete("historico", "1=1", null)

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("CLIENTE", ignoreCase = true) -> values.put("cliente", parser.getAttributeValue("", sCampo))
                                sCampo.equals("ARTICULO", ignoreCase = true) -> values.put("articulo", parser.getAttributeValue("", sCampo))
                                sCampo.equals("CAJAS", ignoreCase = true) -> values.put("cajas", parser.getAttributeValue("", sCampo))
                                sCampo.equals("CANTIDAD", ignoreCase = true) -> values.put("cantidad", parser.getAttributeValue("", sCampo))
                                sCampo.equals("PIEZAS", ignoreCase = true) -> values.put("piezas", parser.getAttributeValue("", sCampo))
                                sCampo.equals("PRECIO", ignoreCase = true) -> values.put("precio", parser.getAttributeValue("", sCampo))
                                sCampo.equals("DTO", ignoreCase = true) -> values.put("dto", parser.getAttributeValue("", sCampo))
                                sCampo.equals("FORMATO", ignoreCase = true) -> values.put("formato", parser.getAttributeValue("", sCampo))
                                sCampo.equals("FECHA", ignoreCase = true) -> {
                                    val sFecha = parser.getAttributeValue("", sCampo)
                                    val sFecha2 = sFecha.substring(8, 10) + "/" + sFecha.substring(5, 7) + "/" + sFecha.substring(0, 4)
                                    values.put("fecha", sFecha2)
                                }
                            }
                        }
                        if (values.getAsString("cliente") != null) {
                            dbAlba.insertWithOnConflict("historico", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                        }
                        values.clear()
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarHcoMes() {
        val f = File(rutaLocal, "HistMes.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val values = ContentValues()
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                dbAlba.delete("histmes", "1=1", null)

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("CLIENTE", ignoreCase = true) -> values.put("cliente", parser.getAttributeValue("", sCampo))
                                sCampo.equals("ARTICULO", ignoreCase = true) -> values.put("articulo", parser.getAttributeValue("", sCampo))
                                sCampo.equals("MES", ignoreCase = true) -> values.put("mes", parser.getAttributeValue("", sCampo))
                                sCampo.equals("CANTIDAD", ignoreCase = true) -> values.put("cantidad", parser.getAttributeValue("", sCampo))
                                sCampo.equals("CANTIDADANT", ignoreCase = true) -> values.put("cantidadant", parser.getAttributeValue("", sCampo))
                                sCampo.equals("IMPORTE", ignoreCase = true) -> values.put("importe", parser.getAttributeValue("", sCampo))
                                sCampo.equals("IMPORTEANT", ignoreCase = true) -> values.put("importeant", parser.getAttributeValue("", sCampo))
                                sCampo.equals("CAJAS", ignoreCase = true) -> values.put("cajas", parser.getAttributeValue("", sCampo))
                                sCampo.equals("CAJASANT", ignoreCase = true) -> values.put("cajasant", parser.getAttributeValue("", sCampo))
                                sCampo.equals("PIEZAS", ignoreCase = true) -> values.put("piezas", parser.getAttributeValue("", sCampo))
                                sCampo.equals("PIEZASANT", ignoreCase = true) -> values.put("piezasant", parser.getAttributeValue("", sCampo))
                            }
                        }
                        if (values.getAsString("cliente") != null) {
                            dbAlba.insertWithOnConflict("histmes", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                        }
                        values.clear()
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }



    private fun importarIvas() {
        val ivasDao: IvasDao? = MyDatabase.getInstance(fContext)?.ivasDao()
        val f = File(rutaLocal, "Ivas.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                ivasDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val ivaEnt = IvasEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Codigo", ignoreCase = true) -> ivaEnt.codigo = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Tipo", ignoreCase = true) -> ivaEnt.tipo = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Iva", ignoreCase = true) -> ivaEnt.porcIva = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Recargo", ignoreCase = true) -> ivaEnt.porcRe = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (ivaEnt.codigo > 0) {
                            ivasDao?.insertar(ivaEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }



    private fun importarProveedores() {
        val proveedoresDao: ProveedoresDao? = MyDatabase.getInstance(fContext)?.proveedoresDao()
        val f = File(rutaLocal, "Proveedores.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                proveedoresDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val provEnt = ProveedoresEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Codigo", ignoreCase = true) -> provEnt.proveedorId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Nombre", ignoreCase = true) -> provEnt.nombre = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Cif", ignoreCase = true) -> provEnt.cif = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (provEnt.proveedorId > 0) {
                            proveedoresDao?.insertar(provEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarRatingArt() {
        val ratingArtDao: RatingArtDao? = MyDatabase.getInstance(fContext)?.ratingArtDao()
        val f = File(rutaLocal, "RatingArt.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                ratingArtDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val ratingEnt = RatingArtEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Articulo", ignoreCase = true) -> ratingEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Alm", ignoreCase = true) -> ratingEnt.almacen = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Cliente", ignoreCase = true) -> ratingEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Ramo", ignoreCase = true) -> ratingEnt.ramoId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Tarifa", ignoreCase = true) -> ratingEnt.tarifaId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Inicio", ignoreCase = true) -> ratingEnt.inicio = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Fin", ignoreCase = true) -> ratingEnt.fin = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Formato", ignoreCase = true) -> ratingEnt.formatoId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Precio", ignoreCase = true) -> ratingEnt.precio = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Dto", ignoreCase = true) -> ratingEnt.dto = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Flag", ignoreCase = true) -> ratingEnt.flag = parser.getAttributeValue("", sCampo).toInt()
                            }
                        }
                        if (ratingEnt.articuloId > 0) {
                            ratingArtDao?.insertar(ratingEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarRatingGrupos() {
        val ratingGruposDao: RatingGruposDao? = MyDatabase.getInstance(fContext)?.ratingGruposDao()
        val f = File(rutaLocal, "RatingGru.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                ratingGruposDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val ratingEnt = RatingGruposEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Grupo", ignoreCase = true) -> ratingEnt.grupo = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Dpto", ignoreCase = true) -> ratingEnt.departamento = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Alm", ignoreCase = true) -> ratingEnt.almacen = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Cliente", ignoreCase = true) -> ratingEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Ramo", ignoreCase = true) -> ratingEnt.ramo = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Tarifa", ignoreCase = true) -> ratingEnt.tarifaId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Inicio", ignoreCase = true) -> ratingEnt.inicio = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Fin", ignoreCase = true) -> ratingEnt.fin = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Dto", ignoreCase = true) -> ratingEnt.dto = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (ratingEnt.grupo > 0) {
                            ratingGruposDao?.insertar(ratingEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }





    private fun importarRutero() {
        val ruterosDao: RuterosDao? = MyDatabase.getInstance(fContext)?.ruterosDao()
        val f = File(rutaLocal, "Rutero.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                ruterosDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val ruteroEnt = RuterosEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Ruta", true) -> ruteroEnt.rutaId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Orden", true) -> ruteroEnt.orden = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Cliente", true) -> ruteroEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                            }
                        }
                        if (ruteroEnt.rutaId > 0) {
                            ruterosDao?.insertar(ruteroEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarOfertas() {
        val ofertasDao: OfertasDao? = MyDatabase.getInstance(fContext)?.ofertasDao()
        val f = File(rutaLocal, "Ofertas.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                ofertasDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val ofertaEnt = OfertasEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Articulo", ignoreCase = true) -> ofertaEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Empresa", ignoreCase = true) -> ofertaEnt.empresa = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Tarifa", ignoreCase = true) -> ofertaEnt.tarifa = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Precio", ignoreCase = true) -> ofertaEnt.precio = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Dto", ignoreCase = true) -> ofertaEnt.dto = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Formato", ignoreCase = true) -> ofertaEnt.formato = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Final", ignoreCase = true) -> ofertaEnt.fFinal = parser.getAttributeValue("", sCampo)
                                sCampo.equals("TipoOferta", ignoreCase = true) -> ofertaEnt.tipoOferta = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Id", ignoreCase = true) -> ofertaEnt.idOferta = parser.getAttributeValue("", sCampo).toInt()
                            }
                        }
                        if (ofertaEnt.articuloId > 0) {
                            ofertasDao?.insertar(ofertaEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }

    private fun importarHcoRepre() {
        val histRepreDao: HistRepreDao? = MyDatabase.getInstance(fContext)?.histRepreDao()
        val f = File(rutaLocal, "HistRep.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                histRepreDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val histRepreEnt = HistRepreEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Representante", ignoreCase = true) -> histRepreEnt.representanteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Mes", ignoreCase = true) -> histRepreEnt.mes = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Anyo", ignoreCase = true) -> histRepreEnt.anyo = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Importe", ignoreCase = true) -> histRepreEnt.importe = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (histRepreEnt.representanteId > 0) {
                            histRepreDao?.insertar(histRepreEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarRutas() {
        val rutasDao: RutasDao? = MyDatabase.getInstance(fContext)?.rutasDao()
        val f = File(rutaLocal, "Rutas.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                rutasDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val rutaEnt = RutasEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Codigo", ignoreCase = true) -> rutaEnt.rutaId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Ruta", ignoreCase = true) -> rutaEnt.descripcion =  parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (rutaEnt.rutaId > 0) {
                            rutasDao?.insertar(rutaEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarOfVolumen() {
        val oftVolumenDao: OftVolumenDao? = MyDatabase.getInstance(fContext)?.oftVolumenDao()
        val f = File(rutaLocal, "OfertasVol.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                oftVolumenDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val oftaVolEnt = OftVolumenEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Id", ignoreCase = true) -> oftaVolEnt.oftVolumenId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Almacen", ignoreCase = true) -> oftaVolEnt.almacen = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Articulo", ignoreCase = true) -> oftaVolEnt.articuloDesct = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Tarifa", ignoreCase = true) -> oftaVolEnt.tarifa = parser.getAttributeValue("", sCampo).toShort()
                            }
                        }
                        if (oftaVolEnt.oftVolumenId > 0) {
                            oftVolumenDao?.insertar(oftaVolEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarOfVolRangos() {
        val oftVolRangosDao: OftVolRangosDao? = MyDatabase.getInstance(fContext)?.oftVolRangosDao()
        val f = File(rutaLocal, "OfVolRangos.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                oftVolRangosDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val oftVolRangEnt = OftVolRangosEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Id", ignoreCase = true) -> oftVolRangEnt.idOferta = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("DesdeImpte", ignoreCase = true) -> oftVolRangEnt.desdeImpte = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("HastaImpte", ignoreCase = true) -> oftVolRangEnt.hastaImpte = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Descuento", ignoreCase = true) -> oftVolRangEnt.descuento = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (oftVolRangEnt.idOferta > 0) {
                            oftVolRangosDao?.insertar(oftVolRangEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }

    private fun importarOfCantRangos() {
        val oftCantRangosDao: OftCantRangosDao? = MyDatabase.getInstance(fContext)?.oftCantRangosDao()
        val f = File(rutaLocal, "CantOfertas.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        var sDesdeCantidad = "0.0"
        var sHastaCantidad = ""
        var queArticulo: String
        var sArticAnterior = ""

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                oftCantRangosDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val oftCantRangEnt = OftCantRangosEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Id", ignoreCase = true) -> oftCantRangEnt.idOferta = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Articulo", ignoreCase = true) -> {
                                    queArticulo = parser.getAttributeValue("", sCampo)
                                    oftCantRangEnt.articuloId = queArticulo.toInt()

                                    // Vamos calculando el valor de "desdeCantidad"
                                    if (queArticulo !== sArticAnterior) {
                                        sArticAnterior = queArticulo
                                        sDesdeCantidad = "0.0"
                                    } else
                                        sDesdeCantidad = sHastaCantidad
                                }
                                sCampo.equals("Cantidad", ignoreCase = true) -> {
                                    sHastaCantidad = parser.getAttributeValue("", sCampo)
                                    oftCantRangEnt.hastaCantidad = sHastaCantidad
                                    oftCantRangEnt.desdeCantidad = sDesdeCantidad
                                }
                                sCampo.equals("PrecioBase", ignoreCase = true) -> oftCantRangEnt.precioBase = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (oftCantRangEnt.idOferta > 0) {
                            oftCantRangosDao?.insertar(oftCantRangEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarEjercicios() {
        val ejerciciosDao: EjerciciosDao? = MyDatabase.getInstance(fContext)?.ejerciciosDao()
        val f = File(rutaLocal, "Ejercicios.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                ejerciciosDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val ejercicioEnt = EjerciciosEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Ejercicio", ignoreCase = true) -> ejercicioEnt.ejercicio = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Descripcion", ignoreCase = true) -> ejercicioEnt.descripcion = parser.getAttributeValue("", sCampo)
                                sCampo.equals("FechaInicio", ignoreCase = true) -> ejercicioEnt.fechaInicio = parser.getAttributeValue("", sCampo)
                                sCampo.equals("FechaFin", ignoreCase = true) -> ejercicioEnt.fechaFin = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (ejercicioEnt.ejercicio >= 0) {
                            ejerciciosDao?.insertar(ejercicioEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarSaldos() {
        val saldosDao: SaldosDao? = MyDatabase.getInstance(fContext)?.saldosDao()
        val f = File(rutaLocal, "Saldos.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                saldosDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val saldoEnt = SaldosEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Cliente", true) -> saldoEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Empresa", true) -> saldoEnt.empresa = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Saldo", true) -> saldoEnt.saldo = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Pendiente", true) -> saldoEnt.pendiente = parser.getAttributeValue("", sCampo)
                                sCampo.equals("FacturasPtes", true) -> saldoEnt.facturasPtes = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("AlbaranesPtes", true) -> saldoEnt.albaranesPtes = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("PedidosPtes", true) -> saldoEnt.pedidosPtes = parser.getAttributeValue("", sCampo).toInt()
                            }
                        }
                        if (saldoEnt.clienteId > 0) {
                            saldosDao?.insertar(saldoEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarSeries(ejercActual: Short) {
        val seriesDao: SeriesDao? = MyDatabase.getInstance(fContext)?.seriesDao()
        val f = File(rutaLocal, "Series.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Aprovechamos y borramos las series que se han quedado antiguas
                seriesDao?.borrarAntiguas(ejercActual)

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val serieEnt = SeriesEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Serie", ignoreCase = true) -> serieEnt.serie = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Ejercicio", ignoreCase = true) -> serieEnt.ejercicio = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Empresa", ignoreCase = true) -> serieEnt.empresa = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Factura", ignoreCase = true) -> serieEnt.factura = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Albaran", ignoreCase = true) -> serieEnt.albaran = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Pedido", ignoreCase = true) -> serieEnt.pedido = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Presupuesto", ignoreCase = true) -> serieEnt.presupuesto = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Flag", ignoreCase = true) -> serieEnt.flag = parser.getAttributeValue("", sCampo).toInt()
                            }
                        }
                        if (serieEnt.serie != "") {
                            var queEjercicio: Short = -1
                            if (serieEnt.ejercicio > -1) queEjercicio = serieEnt.ejercicio

                            if (serieNoExiste(seriesDao, serieEnt.serie, queEjercicio)) {
                                seriesDao?.insertar(serieEnt)
                            }
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }

    private fun importarPendiente() {
        val pendienteDao: PendienteDao? = MyDatabase.getInstance(fContext)?.pendienteDao()
        val f = File(rutaLocal, "Pendiente.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Si estamos recogiendo desde el servicio no borraremos lo que no hayamos enviado
                if (!fDesdeServicio)
                    pendienteDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val pendienteEnt = PendienteEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Cliente", ignoreCase = true) -> pendienteEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Ejercicio", ignoreCase = true) -> pendienteEnt.ejercicio = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Empresa", ignoreCase = true) -> pendienteEnt.empresa = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Almacen", ignoreCase = true) -> pendienteEnt.almacen = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("TipoDocumento", ignoreCase = true) -> pendienteEnt.tipoDoc = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("FormaDePago", ignoreCase = true) -> pendienteEnt.fPago = parser.getAttributeValue("", sCampo)
                                sCampo.equals("FechaDoc", ignoreCase = true) -> pendienteEnt.fechaDoc = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Serie", ignoreCase = true) -> pendienteEnt.serie = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Numero", ignoreCase = true) -> pendienteEnt.numero = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Importe", ignoreCase = true) -> pendienteEnt.importe = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Cobrado", ignoreCase = true) -> pendienteEnt.cobrado = parser.getAttributeValue("", sCampo)
                                sCampo.equals("FechaVen", ignoreCase = true) -> pendienteEnt.fechaVto = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Estado", ignoreCase = true) -> pendienteEnt.estado = parser.getAttributeValue("", sCampo)
                                sCampo.equals("CAlmacen", ignoreCase = true) -> pendienteEnt.cAlmacen = parser.getAttributeValue("", sCampo)
                                sCampo.equals("CPuesto", ignoreCase = true) -> pendienteEnt.cPuesto = parser.getAttributeValue("", sCampo)
                                sCampo.equals("CApunte", ignoreCase = true) -> pendienteEnt.cApunte = parser.getAttributeValue("", sCampo)
                                sCampo.equals("CEjer", ignoreCase = true) -> pendienteEnt.cEjercicio = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Flag", ignoreCase = true) ->  pendienteEnt.flag = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Hoja", ignoreCase = true) -> pendienteEnt.hoja = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Orden", ignoreCase = true) -> pendienteEnt.orden = parser.getAttributeValue("", sCampo).toInt()
                            }

                            pendienteEnt.enviar = "F"
                        }
                        if (pendienteEnt.clienteId > 0) {
                            // Comprobamos que el vencimiento no exista para no duplicarlo. Puede ser que lo tengamos
                            // marcado como liquidado (estado = 'L') y por eso no lo hemos borrado.
                            val queId = pendienteDao?.existeVencimiento(pendienteEnt.tipoDoc,pendienteEnt.cAlmacen,
                                pendienteEnt.cPuesto, pendienteEnt.cApunte, pendienteEnt.cEjercicio) ?: 0
                            if (queId <= 0) {
                                pendienteDao?.insertar(pendienteEnt)
                            }
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarFPago() {
        val formasPagoDao: FormasPagoDao? = MyDatabase.getInstance(fContext)?.formasPagoDao()
        val f = File(rutaLocal, "FormasPago.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                formasPagoDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val fPagoEnt = FormasPagoEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Codigo", true) -> fPagoEnt.codigo = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Clave", true) -> fPagoEnt.clave = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Orden", true) -> fPagoEnt.orden = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("FormaDePago", true) -> fPagoEnt.descripcion = parser.getAttributeValue("", sCampo)
                                sCampo.equals("GeneraCobro", true) -> fPagoEnt.generaCobro = parser.getAttributeValue("", sCampo)
                                sCampo.equals("GenVtos", true) -> fPagoEnt.generaVtos = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Vencimientos", true) -> fPagoEnt.numVtos = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("PrimerVto", true) -> fPagoEnt.primerVto = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("EntrePagos", true) -> fPagoEnt.entrePagos = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("PDivisas", true) -> fPagoEnt.pideDivisas = parser.getAttributeValue("", sCampo)
                                sCampo.equals("PAnotacion", true) -> fPagoEnt.pideAnotacion = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Anotacion", true) -> fPagoEnt.anotacion = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (fPagoEnt.codigo != "") {
                            formasPagoDao?.insertar(fPagoEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }

    private fun importarDivisas() {
        val divisasDao: DivisasDao? = MyDatabase.getInstance(fContext)?.divisasDao()
        val f = File(rutaLocal, "Divisas.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                divisasDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val divisaEnt = DivisasEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Codigo", true) -> divisaEnt.codigo = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Clave", true) -> divisaEnt.clave = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Orden", true) -> divisaEnt.orden = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Divisa", true) -> divisaEnt.descripcion = parser.getAttributeValue("", sCampo)
                                sCampo.equals("PideAnotacion", true) -> divisaEnt.pideAnotacion = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Anotacion", true) -> divisaEnt.anotacion = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (divisaEnt.codigo != "") {
                            divisasDao?.insertar(divisaEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarEmpresas() {
        val empresasDao: EmpresasDao? = MyDatabase.getInstance(fContext)?.empresasDao()
        val f = File(rutaLocal, "Empresas.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                empresasDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val empresaEnt = EmpresasEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Codigo", ignoreCase = true) -> empresaEnt.codigo = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("NombreFiscal", ignoreCase = true) -> empresaEnt.nombreFiscal = parser.getAttributeValue("", sCampo)
                                sCampo.equals("NombreComercial", ignoreCase = true) -> empresaEnt.nombrecomercial = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Serie", ignoreCase = true) -> empresaEnt.serie = parser.getAttributeValue("", sCampo)
                                sCampo.equals("VenderIvaIncl", ignoreCase = true) -> empresaEnt.venderIvaIncl = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (empresaEnt.codigo >= 0) {
                            empresasDao?.insertar(empresaEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarDocsCabPies() {
        val docsCabPiesDao: DocsCabPiesDao? = MyDatabase.getInstance(fContext)?.docsCabPiesDao()
        val f = File(rutaLocal, "DocsCabPies.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                docsCabPiesDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val docCabPieEnt = DocsCabPiesEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Empresa", ignoreCase = true) -> docCabPieEnt.empresa = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Valor", ignoreCase = true) -> docCabPieEnt.valor = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Cadena", ignoreCase = true) -> docCabPieEnt.cadena = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Entero", ignoreCase = true) -> docCabPieEnt.entero = parser.getAttributeValue("", sCampo).toInt()
                            }
                        }
                        if (docCabPieEnt.valor != "") {
                            docsCabPiesDao?.insertar(docCabPieEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarCostos() {
        val costosDao: CostosArticulosDao? = MyDatabase.getInstance(fContext)?.costosArticulosDao()
        val f = File(rutaLocal, "Costos.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                costosDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val costoEnt = CostosArticulosEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Articulo", ignoreCase = true) -> costoEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Empresa", ignoreCase = true) -> costoEnt.empresa = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Costo", ignoreCase = true) -> costoEnt.costo = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (costoEnt.articuloId >= 0) {
                            costosDao?.insertar(costoEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarAlmacenes() {
        val almacenesDao: AlmacenDao? = MyDatabase.getInstance(fContext)?.almacenesDao()
        val f = File(rutaLocal, "Almacenes.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                almacenesDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val almacenEnt = AlmacenesEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Codigo", ignoreCase = true) -> almacenEnt.codigo = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Almacen", ignoreCase = true) -> almacenEnt.descripcion = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (almacenEnt.codigo >= 0) {
                            almacenesDao?.insertar(almacenEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }

    private fun importarDatAdicArtic() {
        val articDatAdicDao: ArticDatAdicDao? = MyDatabase.getInstance(fContext)?.articDatAdicDao()
        val f = File(rutaLocal, "DatAdicArticulos.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                articDatAdicDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val datAdicEnt = ArticDatAdicEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Articulo", ignoreCase = true) -> datAdicEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Valor", ignoreCase = true) -> datAdicEnt.valor = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Cadena", ignoreCase = true) -> datAdicEnt.cadena = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (datAdicEnt.articuloId > 0) {
                            articDatAdicDao?.insertar(datAdicEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarLotes() {
        val lotesDao: LotesDao? = MyDatabase.getInstance(fContext)?.lotesDao()
        val f = File(rutaLocal, "Lotes.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
               lotesDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val loteEnt = LotesEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Articulo", ignoreCase = true) -> loteEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Lote", ignoreCase = true) -> loteEnt.lote = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Flag", ignoreCase = true) -> loteEnt.flag = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Stock", ignoreCase = true) -> loteEnt.stock = parser.getAttributeValue("", sCampo)
                                sCampo.equals("PStock", ignoreCase = true) -> loteEnt.stockPiezas = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (loteEnt.articuloId > 0) {
                            lotesDao?.insertar(loteEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }



    private fun serieNoExiste(seriesDao: SeriesDao?, queSerie: String, queEjercicio: Short): Boolean {

        val fSerie = if (queEjercicio > -1) {
            seriesDao?.existeSerieYEjerc(queSerie, queEjercicio) ?: ""
        } else {
            seriesDao?.existeSerie(queSerie) ?: ""
        }

        return fSerie == ""
    }


    private fun importarStock() {
        val stockDao: StockDao? = MyDatabase.getInstance(fContext)?.stockDao()
        val f = File(rutaLocal, "Stock.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                stockDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val stockEnt = StockEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Articulo", ignoreCase = true) -> stockEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Empresa", ignoreCase = true) -> stockEnt.empresa = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Ent", ignoreCase = true) -> stockEnt.ent = parser.getAttributeValue("", sCampo).replace(',', '.')
                                sCampo.equals("EntC", ignoreCase = true) -> stockEnt.entc = parser.getAttributeValue("", sCampo).replace(',', '.')
                                sCampo.equals("EntP", ignoreCase = true) -> stockEnt.entp = parser.getAttributeValue("", sCampo).replace(',', '.')
                                sCampo.equals("Sal", ignoreCase = true) -> stockEnt.sal = parser.getAttributeValue("", sCampo).replace(',', '.')
                                sCampo.equals("SalC", ignoreCase = true) -> stockEnt.salc = parser.getAttributeValue("", sCampo).replace(',', '.')
                                sCampo.equals("SalP", ignoreCase = true) -> stockEnt.salp = parser.getAttributeValue("", sCampo).replace(',', '.')
                            }
                        }
                        if (stockEnt.articuloId > 0) {
                            stockDao?.insertar(stockEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarTarifas() {
        val tarifasDao: TarifasDao? = MyDatabase.getInstance(fContext)?.tarifasDao()
        val f = File(rutaLocal, "Tarifas.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                tarifasDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val tarifaEnt = TarifasEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Articulo", ignoreCase = true) -> tarifaEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Tarifa", ignoreCase = true) -> tarifaEnt.tarifaId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Precio", ignoreCase = true) -> tarifaEnt.precio = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Dto", ignoreCase = true) -> tarifaEnt.dto = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (tarifaEnt.articuloId > 0) {
                            tarifasDao?.insertar(tarifaEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }



    private fun importarGrupos() {
        val gruposDao: GruposDao? = MyDatabase.getInstance(fContext)?.gruposDao()
        val f = File(rutaLocal, "Grupos.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                gruposDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val grupoEnt = GruposEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Codigo", ignoreCase = true) -> grupoEnt.codigo = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Grupo", ignoreCase = true) -> grupoEnt.descripcion = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (grupoEnt.codigo > 0) {
                            gruposDao?.insertar(grupoEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarDepartamentos() {
        val departamentosDao: DepartamentosDao? = MyDatabase.getInstance(fContext)?.departamentosDao()
        val f = File(rutaLocal, "Departamentos.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                departamentosDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val departamentoEnt = DepartamentosEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Grupo", ignoreCase = true) -> departamentoEnt.grupoId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Codigo", ignoreCase = true) -> departamentoEnt.departamentoId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Departamento", ignoreCase = true) -> departamentoEnt.descripcion = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (departamentoEnt.grupoId > 0) {
                            departamentosDao?.insertar(departamentoEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarClasificadores() {
        val clasificadoresDao: ClasificadoresDao? = MyDatabase.getInstance(fContext)?.clasificadoresDao()
        val f = File(rutaLocal, "Clasificadores.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                clasificadoresDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val clasificadorEnt = ClasificadoresEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Codigo", ignoreCase = true) -> clasificadorEnt.clasificadorId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Descripcion", ignoreCase = true) -> clasificadorEnt.descripcion = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Padre", ignoreCase = true) -> clasificadorEnt.padre = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Nivel", ignoreCase = true) -> clasificadorEnt.nivel = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Orden", ignoreCase = true) -> clasificadorEnt.orden = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Flag", ignoreCase = true) -> clasificadorEnt.flag = parser.getAttributeValue("", sCampo).toInt()
                            }
                        }
                        if (clasificadorEnt.clasificadorId > 0) {
                            clasificadoresDao?.insertar(clasificadorEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarArticClasif() {
        val f = File(rutaLocal, "ArticClasif.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val values = ContentValues()
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                dbAlba.delete("articclasif", "1=1", null)
                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("ARTICULO", ignoreCase = true) -> values.put("articulo", parser.getAttributeValue("", sCampo))
                                sCampo.equals("CLASIFICADOR", ignoreCase = true) -> values.put("clasificador", parser.getAttributeValue("", sCampo))
                                sCampo.equals("ORDEN", ignoreCase = true) -> values.put("orden", parser.getAttributeValue("", sCampo))
                            }
                        }
                        if (values.getAsString("articulo") != null) {
                            dbAlba.insertWithOnConflict("articclasif", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                        }
                        values.clear()
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarFormatos() {
        val formatosDao: FormatosDao? = MyDatabase.getInstance(fContext)?.formatosDao()
        val f = File(rutaLocal, "Formatos.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                formatosDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val formatoEnt = FormatosEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Codigo", ignoreCase = true) -> formatoEnt.formatoId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Descripcion", ignoreCase = true) -> formatoEnt.descripcion = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Flag", ignoreCase = true) -> formatoEnt.flag = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Dosis1", ignoreCase = true) -> formatoEnt.dosis1 = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (formatoEnt.formatoId > 0) {
                            formatosDao?.insert(formatoEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }


    private fun importarTrfFormatos() {
        val trfFormatosDao: TrfFormatosDao? = MyDatabase.getInstance(fContext)?.trfFormatosDao()
        val f = File(rutaLocal, "TarifasFormatos.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                trfFormatosDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val trfFormatoEnt = TrfFormatosEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Articulo", ignoreCase = true) -> trfFormatoEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Tarifa", ignoreCase = true) -> trfFormatoEnt.tarifaId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Formato", ignoreCase = true) -> trfFormatoEnt.formatoId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Precio", ignoreCase = true) -> trfFormatoEnt.precio = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Dto", ignoreCase = true) -> trfFormatoEnt.dto = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (trfFormatoEnt.articuloId > 0) {
                            trfFormatosDao?.insertar(trfFormatoEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }

    private fun importarHcoCompSemMes() {
        val f = File(rutaLocal, "HcoCompSemMes.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val values = ContentValues()
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                dbAlba.delete("hcoCompSemMes", "1=1", null)
                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Fecha", ignoreCase = true) -> values.put("fecha", parser.getAttributeValue("", sCampo))
                                sCampo.equals("Cliente", ignoreCase = true) -> values.put("cliente", parser.getAttributeValue("", sCampo))
                                sCampo.equals("Articulo", ignoreCase = true) -> values.put("articulo", parser.getAttributeValue("", sCampo))
                                sCampo.equals("Cantidad", ignoreCase = true) -> values.put("cantidad", parser.getAttributeValue("", sCampo))
                            }
                        }
                        if (values.getAsString("cliente") != null) {
                            dbAlba.insertWithOnConflict("hcoCompSemMes", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                        }
                        values.clear()
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }



    private fun importarEstadDevoluc() {
        val f = File(rutaLocal, "EstadDevoluc.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val values = ContentValues()
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                dbAlba.delete("estadDevoluc", "1=1", null)
                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("CLIENTE", ignoreCase = true) -> values.put("cliente", parser.getAttributeValue("", sCampo))
                                sCampo.equals("ARTICULO", ignoreCase = true) -> values.put("articulo", parser.getAttributeValue("", sCampo))
                                sCampo.equals("PORCDEVOL", ignoreCase = true) -> values.put("porcDevol", parser.getAttributeValue("", sCampo))
                            }
                        }
                        if (values.getAsString("cliente") != null) {
                            dbAlba.insertWithOnConflict("estadDevoluc", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                        }
                        values.clear()
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }

    private fun importarHcoArticClte() {
        val f = File(rutaLocal, "HcoPorArticClte.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val values = ContentValues()
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                dbAlba.delete("hcoPorArticClte", "1=1", null)
                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("ARTICULO", ignoreCase = true) -> values.put("articulo", parser.getAttributeValue("", sCampo))
                                sCampo.equals("CLIENTE", ignoreCase = true) -> values.put("cliente", parser.getAttributeValue("", sCampo))
                                sCampo.equals("TIPODOC", ignoreCase = true) -> values.put("tipodoc", parser.getAttributeValue("", sCampo))
                                sCampo.equals("SERIE", ignoreCase = true) -> values.put("serie", parser.getAttributeValue("", sCampo))
                                sCampo.equals("NUMERO", ignoreCase = true) -> values.put("numero", parser.getAttributeValue("", sCampo))
                                sCampo.equals("EJERCICIO", ignoreCase = true) -> values.put("ejercicio", parser.getAttributeValue("", sCampo))
                                sCampo.equals("FECHA", ignoreCase = true) -> values.put("fecha", parser.getAttributeValue("", sCampo))
                                sCampo.equals("VENTAS", ignoreCase = true) -> values.put("ventas", parser.getAttributeValue("", sCampo))
                                sCampo.equals("DEVOLUCIONES", ignoreCase = true) -> values.put("devoluciones", parser.getAttributeValue("", sCampo))
                            }
                        }
                        if (values.getAsString("articulo") != null) {
                            dbAlba.insertWithOnConflict("hcoPorArticClte", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                        }
                        values.clear()
                    }
                    event = parser.next()
                }
                fin.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }

        } catch (e: Exception) {
            mostrarExcepcion(e)
        } finally {
            try {
                fin.close()
            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        }
    }



    @SuppressLint("SimpleDateFormat")
    fun baseDatosAXML(queNumExportacion: Int): Boolean {
        //val fConfiguracion = Comunicador.fConfiguracion
        var resultado = false
        val fClientes = ClientesClase(fContext)
        val fNotas = NotasClientes(fContext)
        val fNumExportaciones = NumExportaciones(fContext)

        try {
            var hayClientes = false; var hayDirecc = false; var hayContactos = false; var hayNotas = false
            var hayDocumentos = false; var hayCobros = false; var hayPendiente = false; var hayCargas = false
            // Si estamos enviando desde el servicio primero asignamos el número de exportación a -1 y luego
            // actualizaremos los registros que tengan este número con el número de paquete que nos devuelva el servicio.
            val iSigExportacion: Int = if (fDesdeServicio) -1
            else prefs.getInt("num_sig_exportacion", 1)

            // Nos aseguramos de que la carpeta existe y, si no, la creamos.
            val rutaenvio = File(rutaLocalEnvio)
            if (!rutaenvio.exists())
                rutaenvio.mkdirs()

            // Igualmente, nos aseguramos de que la carpeta esté vacía, para que no haya restos de un
            // envío anterior fallido.
            val rutaLocEnv = File(rutaLocalEnvio)
            val xmlFiles = rutaLocEnv.listFiles()
            for (File in xmlFiles) {
                File.delete()
            }
            val quedanFich = rutaLocEnv.listFiles()
            val continuar = (quedanFich.isEmpty())

            if (continuar) {
                fClientes.abrirParaEnviar(queNumExportacion)
                if (fClientes.cursor.count > 0) {
                    hayClientes = true
                    enviarClientes(fClientes)
                }

                fClientes.abrirDirParaEnviar(queNumExportacion)
                if (fClientes.cDirecciones.count > 0) {
                    hayDirecc = true
                    enviarDirecciones(fClientes)
                }

                val cTelfClteDao: ContactosCltesDao? = MyDatabase.getInstance(fContext)?.contactosCltesDao()
                val lTelefonos: MutableList<ContactosCltesEnt> = if (queNumExportacion > 0)
                    cTelfClteDao?.getTlfsParaEnvExp(queNumExportacion) ?: emptyList<ContactosCltesEnt>().toMutableList()
                else
                    cTelfClteDao?.getTlfsParaEnviar() ?: emptyList<ContactosCltesEnt>().toMutableList()
                if (lTelefonos.isNotEmpty()) {
                    hayContactos = true
                    enviarContactos(lTelefonos)
                }

                if (queNumExportacion == 0) fClientes.marcarComoExportados(iSigExportacion)
                fClientes.close()

                val lNotas = fNotas.abrirParaEnviar(queNumExportacion)
                if (lNotas.isNotEmpty()) {
                    hayNotas = true
                    enviarNotas(lNotas)
                }

                if (queNumExportacion == 0) fNotas.marcarComoExportadas(iSigExportacion)
                fNotas.close()

                if (enviarCargas(queNumExportacion, iSigExportacion)) hayCargas = true
                if (enviarCabeceras(queNumExportacion, iSigExportacion)) hayDocumentos = true
                if (enviarCobros(queNumExportacion, iSigExportacion)) hayCobros = true
                if (enviarPendiente(queNumExportacion, iSigExportacion)) hayPendiente = true

                if (hayClientes || hayDirecc || hayContactos || hayNotas || hayDocumentos || hayCobros || hayPendiente || hayCargas) {
                    // Enviamos información técnica de la tablet
                    enviarInfTecnica()

                    crearLog(
                        hayClientes,
                        hayDirecc,
                        hayContactos,
                        hayNotas,
                        hayDocumentos,
                        hayCobros,
                        hayPendiente,
                        hayCargas
                    )
                    crearCadenaResumen(hayClientes, hayDirecc, hayContactos, hayNotas, hayDocumentos, hayCobros, hayPendiente, hayCargas)

                    // Una vez que hemos preparado los XML para enviar, haremos una copia.
                    // De esta forma siempre tendremos una copia de lo último que hayamos enviado.
                    copiarEnvio()

                    if (queNumExportacion == 0) {
                        if (!fDesdeServicio) {
                            prefs.edit().putInt("num_sig_exportacion", iSigExportacion + 1).apply()
                            fNumExportaciones.guardarExportacion(iSigExportacion)
                        }
                    }
                    // Guardamos también en las preferencias la fecha y hora del último envío.
                    val tim = System.currentTimeMillis()
                    val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                    val fFecha = df.format(tim)
                    prefs.edit().putString("fecha_ult_envio", fFecha).apply()

                    if (fDesdeServicio) {
                        resultado = comprimirEnvio()
                    } else {
                        val msg = Message()
                        msg.obj = fContext.getString(string.msj_FinPrep)
                        puente.sendMessage(msg)

                        resultado = true
                    }
                } else {
                    val msg = Message()
                    msg.obj = fContext.getString(string.msj_NoDatosExport)
                    puente.sendMessage(msg)
                }
            } else {
                val msg = Message()
                msg.obj = fContext.getString(string.msj_NoDatosExport)
                puente.sendMessage(msg)
            }
        } finally {
            fClientes.close()
            fNotas.close()
        }

        return resultado
    }


    @SuppressLint("SimpleDateFormat")
    fun actualizarNumPaquete(fNumPaquete: Int) {
        val fClientes = ClientesClase(fContext)
        fClientes.marcarNumExport(fNumPaquete)
        fClientes.close()

        val fNotas = NotasClientes(fContext)
        fNotas.marcarNumExport(fNumPaquete)

        // Hacemos esto para poder trabajar con las cabeceras
        val dbAlba = BaseDatos(fContext).writableDatabase
        dbAlba.beginTransaction()

        try {
            val values = ContentValues()
            values.put("numexport", fNumPaquete)
            dbAlba.update("cabeceras", values, "numexport=-1", null)

            val pendienteDao: PendienteDao? = MyDatabase.getInstance(fContext)?.pendienteDao()
            pendienteDao?.actualizarNumExport(fNumPaquete)

            val cobrosDao: CobrosDao? = MyDatabase.getInstance(fContext)?.cobrosDao()
            cobrosDao?.actualizarNumPaquete(fNumPaquete)

            // Insertamos en numexport el número de paquete junto con la fecha y hora actuales
            val tim = System.currentTimeMillis()
            val df = SimpleDateFormat("dd/MM/yyyy")
            val fFecha = df.format(tim)
            val dfHora = SimpleDateFormat("HH:mm")
            val fHora = dfHora.format(tim)
            values.put("fecha", fFecha)
            values.put("hora", fHora)
            dbAlba.insert("numexport", null, values)

        } finally {
            dbAlba.setTransactionSuccessful()
            dbAlba.endTransaction()
        }
    }

    fun revertirEstado() {
        val dbAlba = BaseDatos(fContext).writableDatabase
        dbAlba.beginTransaction()

        // Volvemos a establecer el estado 'N' para que los registros sean enviados la próxima vez
        try {
            val values = ContentValues()
            values.put("estado", "N")
            dbAlba.update("cabeceras", values, "numexport=-1", null)
            dbAlba.update("cargas", values, "numexport=-1", null)

            val pendienteDao: PendienteDao? = MyDatabase.getInstance(fContext)?.pendienteDao()
            pendienteDao?.revertirEstado()

            val cobrosDao: CobrosDao? = MyDatabase.getInstance(fContext)?.cobrosDao()
            cobrosDao?.revertirEstado()


        } finally {
            dbAlba.setTransactionSuccessful()
            dbAlba.endTransaction()
        }
    }


    private fun comprimirEnvio(): Boolean {
        try {
            val files = File(rutaLocalEnvio).listFiles()

            var origin: BufferedInputStream?
            val dest = FileOutputStream("$rutaLocalEnvio/envio.zip")
            val out = ZipOutputStream(BufferedOutputStream(dest))
            val data = ByteArray(DEFAULT_BUFFER_SIZE)

            for (i in 0 until files.count()) {
                val fi = FileInputStream(files[i])
                origin = BufferedInputStream(fi, DEFAULT_BUFFER_SIZE)

                val entry = ZipEntry(files[i].name)
                out.putNextEntry(entry)
                var count: Int
                var continuar = true

                while (continuar) {
                    count = origin.read(data, 0, DEFAULT_BUFFER_SIZE)
                    if (count != -1) out.write(data, 0, count)
                    else continuar = false
                }

                origin.close()
            }

            out.close()

        } catch (e: Exception) {
            return false
        }

        return true
    }



    private fun enviarInfTecnica() {
        try {
            val fConfiguracion = Comunicador.fConfiguracion
            val tim = System.currentTimeMillis()
            val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val fFecha = df.format(tim)

            val outputFile = File(rutaLocalEnvio, "Tecnica.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            serializer.startTag("", "record")
                serializer.attribute(null, "NUM_TABLET", fConfiguracion.codTerminal())
                serializer.attribute(null, "FECHA", fFecha)
                serializer.attribute(null, "VERSION", VERSION_PROGRAMA)
                serializer.attribute(null, "COMPILACION", COMPILACION_PROGRAMA.substring(1, COMPILACION_PROGRAMA.length))
                serializer.attribute(null, "MODO_VENTA", prefs.getString("modo_venta", "1"))
            serializer.endTag("", "record")
            serializer.endTag("", "consulta")
            serializer.endDocument()
            serializer.flush()
            fout.close()

        } catch (e: Exception) {
            mostrarExcepcion(e)
        }
    }


    private fun enviarClientes(fClientes: ClientesClase) {

        val msg = Message()
        msg.obj = "Preparando clientes"
        puente.sendMessage(msg)

        try {
            val outputFile = File(rutaLocalEnvio, "Clientes.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            fClientes.cursor.moveToFirst()
            while (!fClientes.cursor.isAfterLast) {
                // Construimos el XML
                serializer.startTag("", "record")
                serializer.attribute(null, "CLIENTE", fClientes.getCliente().toString())
                serializer.attribute(null, "CODIGO", fClientes.getCodigo())
                serializer.attribute(null, "NOMFI", fClientes.getNFiscal())
                serializer.attribute(null, "NOMCO", fClientes.getNComercial())
                serializer.attribute(null, "CIF", fClientes.getCIF())
                serializer.attribute(null, "DIRECC", fClientes.getDireccion())
                serializer.attribute(null, "LOCALI", fClientes.getPoblacion())
                serializer.attribute(null, "CPOSTAL", fClientes.getCodPostal())
                serializer.attribute(null, "PROVIN", fClientes.getProvincia())
                serializer.attribute(null, "APLIVA", logicoACadena(fClientes.getAplicarIva()))
                serializer.attribute(null, "APLREC", logicoACadena(fClientes.getAplicarRe()))
                serializer.attribute(null, "IVA", "NULL")
                if (fClientes.getTarifa() == "")
                    serializer.attribute(null, "TARIFA", "NULL")
                else
                    serializer.attribute(null, "TARIFA", fClientes.getTarifa())
                if (fClientes.getTarifaDto() == "")
                    serializer.attribute(null, "TARDTO", "NULL")
                else
                    serializer.attribute(null, "TARDTO", fClientes.getTarifaDto())

                serializer.attribute(null, "FPAGO", fClientes.getFPago())
                serializer.attribute(null, "RUTA", fClientes.getRuta())
                serializer.attribute(null, "RIESGO", fClientes.getRiesgo().toString().replace('.', ','))
                serializer.attribute(null, "FLAG", fClientes.getFlag().toString())

                when (fClientes.getEstado()) {
                    "XN" -> serializer.attribute(null, "ESTADO", "N")
                    "XM" -> serializer.attribute(null, "ESTADO", "M")
                    else -> serializer.attribute(null, "ESTADO", fClientes.getEstado())
                }

                serializer.endTag("", "record")
                fClientes.cursor.moveToNext()
            }
            serializer.endTag("", "consulta")

            serializer.endDocument()
            serializer.flush()
            fout.close()
            // Guardamos el tamaño del fichero CClientes.xml.
            fTamCltes = outputFile.length()

        } catch (e: FileNotFoundException) {
            mostrarExcepcion(e)
        } catch (e: Exception) {
            mostrarExcepcion(e)
        }
    }

    private fun enviarDirecciones(fClientes: ClientesClase) {

        val msg = Message()
        msg.obj = "Preparando direcciones"
        puente.sendMessage(msg)

        try {
            val outputFile = File(rutaLocalEnvio, "DirClientes.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            fClientes.cDirecciones.moveToFirst()
            while (!fClientes.cDirecciones.isAfterLast) {
                serializer.startTag("", "record")
                serializer.attribute(null, "CLIENTE", fClientes.getDir_Cliente().toString())
                serializer.attribute(null, "ORDEN", fClientes.getDir_Orden())
                serializer.attribute(null, "DIRECC", fClientes.getDir_Direccion())
                serializer.attribute(null, "POBLAC", fClientes.getDir_Poblac())
                serializer.attribute(null, "CPOSTAL", fClientes.getDir_CP())
                serializer.attribute(null, "PROVIN", fClientes.getDir_Provincia())
                serializer.attribute(null, "PAIS", fClientes.getDir_Pais())
                serializer.attribute(null, "DIRDOC", "F")
                serializer.attribute(null, "DIRMER", "F")
                serializer.attribute(null, "ESTADO", "N")
                serializer.endTag("", "record")
                fClientes.cDirecciones.moveToNext()
            }
            serializer.endTag("", "consulta")

            serializer.endDocument()
            serializer.flush()
            fout.close()
            fTamDirecc = outputFile.length()

        } catch (e: FileNotFoundException) {
            mostrarExcepcion(e)
        } catch (e: Exception) {
            mostrarExcepcion(e)
        }
    }


    private fun enviarContactos(lTelefonos: MutableList<ContactosCltesEnt>) {
        val msg = Message()
        msg.obj = "Preparando contactos"
        puente.sendMessage(msg)

        try {
            val outputFile = File(rutaLocalEnvio, "ConClientes.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            for (telefono in lTelefonos) {
                serializer.startTag("", "record")
                serializer.attribute(null, "CLIENTE", telefono.clienteId.toString())
                serializer.attribute(null, "ORDEN", telefono.orden.toString())
                serializer.attribute(null, "CONTACTO", telefono.nombre)
                serializer.attribute(null, "TEL1", telefono.telefono1)
                serializer.attribute(null, "TEL2", telefono.telefono2)
                serializer.attribute(null, "OBS1", telefono.obs1)
                serializer.attribute(null, "EMAIL", telefono.eMail)
                serializer.attribute(null, "Estado", "N")
                serializer.endTag("", "record")
            }
            serializer.endTag("", "consulta")

            serializer.endDocument()
            serializer.flush()
            fout.close()
            fTamContactos = outputFile.length()

        } catch (e: FileNotFoundException) {
            mostrarExcepcion(e)
        } catch (e: Exception) {
            mostrarExcepcion(e)
        }
    }


    private fun enviarNotas(lNotas: MutableList<NotasCltesEnt>) {

        val msg = Message()
        msg.obj = "Preparando notas de clientes"
        puente.sendMessage(msg)

        try {
            val outputFile = File(rutaLocalEnvio, "NotasCltes.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            serializer.startTag("", "consulta")

            for (notaClte in lNotas) {
                // Construimos el XML
                serializer.startTag("", "record")
                serializer.attribute(null, "CLIENTE", notaClte.clienteId.toString())
                serializer.attribute(null, "NOTA", notaClte.nota)
                serializer.attribute(null, "FECHA", notaClte.fecha)
                serializer.attribute(null, "ESTADO", notaClte.estado)
                serializer.endTag("", "record")
            }
            serializer.endTag("", "consulta")

            serializer.endDocument()
            serializer.flush()
            fout.close()
            // Guardamos el tamanyo del fichero Notas.xml.
            fTamNotasCltes = outputFile.length()

        } catch (e: FileNotFoundException) {
            mostrarExcepcion(e)
        } catch (e: Exception) {
            mostrarExcepcion(e)
        }
    }



    private fun enviarCargas(queNumExportacion: Int, iSigExportacion: Int): Boolean {
        // Para obtener el cursor con los documentos creo sobre la marcha un objeto de tipo BaseDatos.
        val dbAlba = BaseDatos(fContext).writableDatabase
        var sCondicion: String = if (queNumExportacion == 0) {
            " WHERE estado = 'N' OR estado = 'R'"
        } else
            " WHERE numexport = $queNumExportacion"

        val cCargas = dbAlba.rawQuery("SELECT * FROM cargas $sCondicion", null)

        if (cCargas.moveToFirst()) {
            val msg = Message()
            msg.obj = "Preparando cargas"
            puente.sendMessage(msg)

            cargasAXML(cCargas)
            cCargas.close()

            sCondicion = if (queNumExportacion == 0) " WHERE B.estado = 'N' OR B.estado = 'R'"
            else " WHERE B.numexport = $queNumExportacion"

            val cLineas = dbAlba.rawQuery("SELECT A.* FROM cargasLineas A"
                    + " LEFT JOIN cargas B ON B.cargaId = A.cargaId" + sCondicion, null)

            cargasLineasAXML(cLineas)
            cLineas.close()

            // Marcamos las cargas como exportadas.
            if (queNumExportacion == 0) {
                val values = ContentValues()
                values.put("estado", "X")
                values.put("numexport", iSigExportacion)
                dbAlba.update("cargas", values, "estado='N' OR estado='R'", null)
            }

            return true

        } else {
            return false
        }
    }

    private fun enviarCabeceras(queNumExportacion: Int, iSigExportacion: Int): Boolean {
        // Para obtener el cursor con los documentos creo sobre la marcha un objeto de tipo BaseDatos.
        val dbAlba = BaseDatos(fContext).writableDatabase
        var sCondicion: String
        val fConfiguracion = Comunicador.fConfiguracion

        sCondicion = if (queNumExportacion == 0) {
            // Si tenemos rutero_reparto mandaremos también las cabeceras de los documentos que estén firmados o tengan alguna incidencia (sólo las cabeceras).
            if (fConfiguracion.hayReparto())
                " WHERE estado = 'N' OR estado = 'R' OR " + "((firmado = 'T' OR tipoincidencia IS NOT NULL) AND estado <> 'X')"
            else
                " WHERE estado = 'N' OR estado = 'R'"
        } else
            " WHERE numexport = $queNumExportacion"

        val cCabeceras = dbAlba.rawQuery("SELECT * FROM cabeceras $sCondicion", null)

        if (cCabeceras.moveToFirst()) {
            val msg = Message()
            msg.obj = "Preparando documentos"
            puente.sendMessage(msg)

            cabecerasAXML(cCabeceras)
            cCabeceras.close()

            // No enviaremos las líneas de documentos importados que han sido firmados o marcados con alguna incidencia
            // (desde el módulo de repartos).
            sCondicion = if (queNumExportacion == 0) " WHERE B.estado = 'N' OR B.estado = 'R'"
            else " WHERE B.numexport = $queNumExportacion AND B.estadoinicial IS NULL"

            val cLineas = dbAlba.rawQuery("SELECT A.* FROM lineas A" +
                    " LEFT OUTER JOIN cabeceras B ON B._id = A.cabeceraId" +
                    sCondicion, null)

            lineasAXML(cLineas)
            cLineas.close()

            // Exportamos los descuentos en cascada.
            val cLineasDt = dbAlba.rawQuery("SELECT A.* FROM desctoslineas A" +
                    " LEFT JOIN lineas C ON C._id = A.linea" +
                    " LEFT JOIN cabeceras B ON B._id = C.cabeceraId" +
                    sCondicion, null)

            lineasDtoAXML(cLineasDt)
            cLineasDt.close()

            // Si hemos ido guardando las imágenes con las firmas digitales, las enviamos.
            if (fDesdeServicio) {
                if (fConfiguracion.activarFirmaDigital() || fConfiguracion.hayReparto()) {
                    enviarFirmas()
                }
            }

            // Marcamos las cabeceras como exportadas.
            if (queNumExportacion == 0) {
                val values = ContentValues()
                values.put("estado", "X")
                values.put("numexport", iSigExportacion)
                if (fConfiguracion.hayReparto())
                    dbAlba.update("cabeceras", values,
                            "estado='N' OR estado='R' OR ((firmado = 'T' OR tipoincidencia IS NOT NULL) AND estado <> 'X')", null)
                else
                    dbAlba.update("cabeceras", values, "estado='N' OR estado='R'", null)
            }

            return true

        } else {
            return false
        }
    }


    @Throws(Exception::class)
    private fun enviarFirmas() {
        val dirlocFirmas = dimeRutaLocalFirmas()

        val rutaLocalFirmas = File(dirlocFirmas)
        val firmasFiles = rutaLocalFirmas.listFiles { _, name -> nombreFirmaValido(name) }

        if (firmasFiles != null && firmasFiles.isNotEmpty()) {
            for (fichFirma in firmasFiles) {
                File(dirlocFirmas + "/" + fichFirma.name).copyTo(File(rutaLocalEnvio + "/" + fichFirma.name), true)
            }

            // Borramos los ficheros de firmas de la carpeta local
            for (fichFirma in firmasFiles) {
                fichFirma.delete()
            }
        }
    }


    private fun nombreFirmaValido(name: String): Boolean {
        // Buscamos si en el array de aCabeceras está el documento al que pertenece la firma, para enviarla o no.
        var resultado = false
        var queFichero: String
        for (idDoc in aCabeceras) {
            queFichero = "$idDoc.jpg"
            if (name.equals(queFichero, ignoreCase = true)) {
                resultado = true
                break
            }
        }
        return resultado
    }


    private fun dimeRutaLocalFirmas(): String {
        val result: String
        // Vemos la carpeta de envio que tenemos en preferencias.
        val directorioLocal = prefs.getString("rutacomunicacion", "") ?: ""
        result = if (directorioLocal == "") {
            if (fUsarMultisistema) "/storage/sdcard0/alba/firmas/" + BaseDatos.queBaseDatos
            else "/storage/sdcard0/alba/firmas/"
        } else {
            if (fUsarMultisistema) "$directorioLocal/firmas/${BaseDatos.queBaseDatos}"
            else "$directorioLocal/firmas/"
        }
        return result
    }


    private fun cargasAXML(cCargas: Cursor) {
        try {
            val outputFile = File(rutaLocalEnvio, "Cargas.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            cCargas.moveToFirst()
            while (!cCargas.isAfterLast) {
                serializer.startTag("", "record")
                serializer.attribute(null, "CARGAID", cCargas.getString(cCargas.getColumnIndex("cargaId")))
                serializer.attribute(null, "EMPRESA", cCargas.getInt(cCargas.getColumnIndex("empresa")).toString())
                serializer.attribute(null, "FECHA", cCargas.getString(cCargas.getColumnIndex("fecha")))
                serializer.attribute(null, "HORA", cCargas.getString(cCargas.getColumnIndex("hora")))
                serializer.attribute(null, "ESFINDEDIA", cCargas.getString(cCargas.getColumnIndex("esFinDeDia")))
                serializer.endTag("", "record")
                cCargas.moveToNext()
            }
            serializer.endTag("", "consulta")

            serializer.endDocument()
            serializer.flush()
            fout.close()
            fTamCargas = outputFile.length()

        } catch (e: FileNotFoundException) {
            mostrarExcepcion(e)
        } catch (e: Exception) {
            mostrarExcepcion(e)
        }
    }

    private fun cargasLineasAXML(cLineas: Cursor) {
        try {
            val outputFile = File(rutaLocalEnvio, "CargasLineas.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            cLineas.moveToFirst()
            while (!cLineas.isAfterLast) {
                serializer.startTag("", "record")
                serializer.attribute(null, "CARGAID", cLineas.getString(cLineas.getColumnIndex("cargaId")))
                serializer.attribute(null, "ARTICULO", cLineas.getString(cLineas.getColumnIndex("articulo")))
                serializer.attribute(null, "LOTE", cLineas.getString(cLineas.getColumnIndex("lote")))
                serializer.attribute(null, "CAJAS", cLineas.getString(cLineas.getColumnIndex("cajas")).replace('.', ','))
                serializer.attribute(null, "CANTIDAD", cLineas.getString(cLineas.getColumnIndex("cantidad")).replace('.', ','))
                serializer.endTag("", "record")
                cLineas.moveToNext()
            }
            serializer.endTag("", "consulta")

            serializer.endDocument()
            serializer.flush()
            fout.close()
            fTamCargasLineas = outputFile.length()

        } catch (e: FileNotFoundException) {
            mostrarExcepcion(e)
        } catch (e: Exception) {
            mostrarExcepcion(e)
        }
    }


    private fun cabecerasAXML(cCabeceras: Cursor) {
        try {
            val outputFile = File(rutaLocalEnvio, "Cabeceras.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            cCabeceras.moveToFirst()
            while (!cCabeceras.isAfterLast) {
                // Añadimos el id del documento al array aCabeceras
                aCabeceras.add(cCabeceras.getInt(cCabeceras.getColumnIndex("_id")))

                serializer.startTag("", "record")
                serializer.attribute(null, "IdDoc", cCabeceras.getString(cCabeceras.getColumnIndex("_id")))
                serializer.attribute(null, "TIPODOC", cCabeceras.getString(cCabeceras.getColumnIndex("tipodoc")))
                if (cCabeceras.getString(cCabeceras.getColumnIndex("tipoPedido")) != null)
                    serializer.attribute(null, "TIPOPEDIDO", cCabeceras.getString(cCabeceras.getColumnIndex("tipoPedido")))
                else
                    serializer.attribute(null, "TIPOPEDIDO", "")
                serializer.attribute(null, "ALM", cCabeceras.getString(cCabeceras.getColumnIndex("alm")))
                serializer.attribute(null, "SERIE", cCabeceras.getString(cCabeceras.getColumnIndex("serie")))
                serializer.attribute(null, "NUMERO", cCabeceras.getString(cCabeceras.getColumnIndex("numero")))
                serializer.attribute(null, "EJER", cCabeceras.getString(cCabeceras.getColumnIndex("ejer")))
                serializer.attribute(null, "EMPRESA", cCabeceras.getString(cCabeceras.getColumnIndex("empresa")))
                serializer.attribute(null, "FECHA", cCabeceras.getString(cCabeceras.getColumnIndex("fecha")))
                if (cCabeceras.getString(cCabeceras.getColumnIndex("hora")) != null)
                    serializer.attribute(null, "HORA", cCabeceras.getString(cCabeceras.getColumnIndex("hora")))
                else
                    serializer.attribute(null, "HORA", "")
                if (cCabeceras.getString(cCabeceras.getColumnIndex("fechaentrega")) != null)
                    serializer.attribute(null, "FECHAENTREGA", cCabeceras.getString(cCabeceras.getColumnIndex("fechaentrega")))
                else
                    serializer.attribute(null, "FECHAENTREGA", "")
                serializer.attribute(null, "CLIENTE", cCabeceras.getString(cCabeceras.getColumnIndex("cliente")))
                if (cCabeceras.getString(cCabeceras.getColumnIndex("ruta")) != null)
                    serializer.attribute(null, "RUTA", cCabeceras.getString(cCabeceras.getColumnIndex("ruta")))
                else
                    serializer.attribute(null, "RUTA", "")
                serializer.attribute(null, "APLIVA", cCabeceras.getString(cCabeceras.getColumnIndex("apliva")))
                serializer.attribute(null, "APLREC", cCabeceras.getString(cCabeceras.getColumnIndex("aplrec")))
                serializer.attribute(null, "BRUTO", cCabeceras.getString(cCabeceras.getColumnIndex("bruto")).replace('.', ','))
                serializer.attribute(null, "DTO", cCabeceras.getString(cCabeceras.getColumnIndex("dto")).replace('.', ','))
                if (cCabeceras.getString(cCabeceras.getColumnIndex("dto2")) != null)
                    serializer.attribute(null, "DTO2", cCabeceras.getString(cCabeceras.getColumnIndex("dto2")).replace('.', ','))
                else
                    serializer.attribute(null, "DTO2", "")
                if (cCabeceras.getString(cCabeceras.getColumnIndex("dto3")) != null)
                    serializer.attribute(null, "DTO3", cCabeceras.getString(cCabeceras.getColumnIndex("dto3")).replace('.', ','))
                else
                    serializer.attribute(null, "DTO3", "")
                if (cCabeceras.getString(cCabeceras.getColumnIndex("dto4")) != null)
                    serializer.attribute(null, "DTO4", cCabeceras.getString(cCabeceras.getColumnIndex("dto4")).replace('.', ','))
                else
                    serializer.attribute(null, "DTO4", "")
                serializer.attribute(null, "BASE", cCabeceras.getString(cCabeceras.getColumnIndex("base")).replace('.', ','))
                serializer.attribute(null, "IVA", cCabeceras.getString(cCabeceras.getColumnIndex("iva")).replace('.', ','))
                serializer.attribute(null, "RECARGO", cCabeceras.getString(cCabeceras.getColumnIndex("recargo")).replace('.', ','))
                serializer.attribute(null, "TOTAL", cCabeceras.getString(cCabeceras.getColumnIndex("total")).replace('.', ','))
                serializer.attribute(null, "FLAG", cCabeceras.getString(cCabeceras.getColumnIndex("flag")))
                serializer.attribute(null, "OBS1", cCabeceras.getString(cCabeceras.getColumnIndex("obs1")))
                serializer.attribute(null, "OBS2", cCabeceras.getString(cCabeceras.getColumnIndex("obs2")))
                if (cCabeceras.getString(cCabeceras.getColumnIndex("fpago")) != null)
                    serializer.attribute(null, "FPAGO", cCabeceras.getString(cCabeceras.getColumnIndex("fpago")))
                else
                    serializer.attribute(null, "FPAGO", "")
                if (cCabeceras.getString(cCabeceras.getColumnIndex("tipoincidencia")) != null)
                    serializer.attribute(null, "TIPOINCIDENCIA", cCabeceras.getString(cCabeceras.getColumnIndex("tipoincidencia")))
                else
                    serializer.attribute(null, "TIPOINCIDENCIA", "")
                if (cCabeceras.getString(cCabeceras.getColumnIndex("textoincidencia")) != null)
                    serializer.attribute(null, "TEXTOINCIDENCIA", cCabeceras.getString(cCabeceras.getColumnIndex("textoincidencia")))
                else
                    serializer.attribute(null, "TEXTOINCIDENCIA", "")
                if (cCabeceras.getString(cCabeceras.getColumnIndex("firmado")) != null)
                    serializer.attribute(null, "ENTREGADO", cCabeceras.getString(cCabeceras.getColumnIndex("firmado")))
                else
                    serializer.attribute(null, "ENTREGADO", "")
                if (cCabeceras.getString(cCabeceras.getColumnIndex("fechafirma")) != null)
                    serializer.attribute(null, "FECHAFIRMA", cCabeceras.getString(cCabeceras.getColumnIndex("fechafirma")))
                else
                    serializer.attribute(null, "FECHAFIRMA", "")
                if (cCabeceras.getString(cCabeceras.getColumnIndex("horafirma")) != null)
                    serializer.attribute(null, "HORAFIRMA", cCabeceras.getString(cCabeceras.getColumnIndex("horafirma")))
                else
                    serializer.attribute(null, "HORAFIRMA", "")

                val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fFecha = df.format(System.currentTimeMillis())
                serializer.attribute(null, "FECHAENVIO", fFecha)
                val dfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
                val fHora = dfHora.format(System.currentTimeMillis())
                serializer.attribute(null, "HORAENVIO", fHora)

                if (cCabeceras.getString(cCabeceras.getColumnIndex("estadoinicial")) != null)
                    if (cCabeceras.getString(cCabeceras.getColumnIndex("estadoinicial")) == "0")
                        serializer.attribute(null, "ESTADO", "E")
                    else
                        serializer.attribute(null, "ESTADO", "N")
                else
                    serializer.attribute(null, "ESTADO", "N")

                if (cCabeceras.getString(cCabeceras.getColumnIndex("almDireccion")) != null)
                    //serializer.attribute(null, "ALMDIRECCIONCLTE", cCabeceras.getString(cCabeceras.getColumnIndex("almDireccion")))
                    // Si damos de alta una dirección en la tablet, la gestión la asigna siempre al almacén 0, por eso indicamos
                    // aquí siempre el almacén 0.
                    serializer.attribute(null, "ALMDIRECCIONCLTE", "000")
                else
                    serializer.attribute(null, "ALMDIRECCIONCLTE", "")

                if (cCabeceras.getString(cCabeceras.getColumnIndex("ordenDireccion")) != null)
                    serializer.attribute(null, "ORDENDIRECCIONCLTE", cCabeceras.getString(cCabeceras.getColumnIndex("ordenDireccion")))
                else
                    serializer.attribute(null, "ORDENDIRECCIONCLTE", "")

                serializer.endTag("", "record")
                cCabeceras.moveToNext()
            }
            serializer.endTag("", "consulta")

            serializer.endDocument()
            serializer.flush()
            fout.close()
            fTamCabec = outputFile.length()

        } catch (e: FileNotFoundException) {
            mostrarExcepcion(e)
        } catch (e: Exception) {
            mostrarExcepcion(e)
        }
    }


    private fun lineasAXML(cLineas: Cursor) {

        try {
            val outputFile = File(rutaLocalEnvio, "Lineas.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            cLineas.moveToFirst()
            while (!cLineas.isAfterLast) {
                serializer.startTag("", "record")
                // Enviamos el id de la linea para poder enlazar con los descuentos en cascada.
                serializer.attribute(null, "Id", cLineas.getString(cLineas.getColumnIndex("_id")))
                serializer.attribute(null, "CabeceraId", cLineas.getInt(cLineas.getColumnIndex("cabeceraId")).toString())
                //serializer.attribute(null, "TipoDoc", cLineas.getString(cLineas.getColumnIndex("tipodoc")))
                //serializer.attribute(null, "ALM", cLineas.getString(cLineas.getColumnIndex("alm")))
                //serializer.attribute(null, "SERIE", cLineas.getString(cLineas.getColumnIndex("serie")))
                //serializer.attribute(null, "EJER", cLineas.getString(cLineas.getColumnIndex("ejer")))
                //serializer.attribute(null, "NUMERO", cLineas.getString(cLineas.getColumnIndex("numero")))
                serializer.attribute(null, "ARTICULO", cLineas.getString(cLineas.getColumnIndex("articulo")))
                serializer.attribute(null, "CODIGO", cLineas.getString(cLineas.getColumnIndex("codigo")))
                serializer.attribute(null, "DESCR", cLineas.getString(cLineas.getColumnIndex("descr")))
                if (cLineas.getString(cLineas.getColumnIndex("tarifa")) != null)
                    serializer.attribute(null, "TARIFA", cLineas.getString(cLineas.getColumnIndex("tarifa")))
                else
                    serializer.attribute(null, "TARIFA", "0")
                serializer.attribute(null, "PRECIO", cLineas.getString(cLineas.getColumnIndex("precio")).replace('.', ','))
                serializer.attribute(null, "TIPOIVA", cLineas.getString(cLineas.getColumnIndex("codigoiva")))
                serializer.attribute(null, "CAJAS", cLineas.getString(cLineas.getColumnIndex("cajas")).replace('.', ','))
                serializer.attribute(null, "CANTIDAD", cLineas.getString(cLineas.getColumnIndex("cantidad")).replace('.', ','))

                if (cLineas.getString(cLineas.getColumnIndex("piezas")) == null)
                    serializer.attribute(null, "PIEZAS", "NULL")
                else
                    serializer.attribute(null, "PIEZAS", cLineas.getString(cLineas.getColumnIndex("piezas")).replace('.', ','))
                serializer.attribute(null, "DTO", cLineas.getString(cLineas.getColumnIndex("dto")).replace('.', ','))
                if (cLineas.getString(cLineas.getColumnIndex("dtoi")) != null)
                    serializer.attribute(null, "DTOI", cLineas.getString(cLineas.getColumnIndex("dtoi")).replace('.', ','))
                else
                    serializer.attribute(null, "DTOI", "NULL")
                if (cLineas.getString(cLineas.getColumnIndex("lote")) == null) serializer.attribute(null, "LOTE", "NULL")
                else serializer.attribute(null, "LOTE", cLineas.getString(cLineas.getColumnIndex("lote")))

                serializer.attribute(null, "FLAG", cLineas.getString(cLineas.getColumnIndex("flag")))

                if (cLineas.getString(cLineas.getColumnIndex("flag3")) == null)
                    serializer.attribute(null, "FLAG3", "0")
                else
                    serializer.attribute(null, "FLAG3", cLineas.getString(cLineas.getColumnIndex("flag3")))

                if (cLineas.getString(cLineas.getColumnIndex("flag5")) == null)
                    serializer.attribute(null, "FLAG5", "0")
                else
                    serializer.attribute(null, "FLAG5", cLineas.getString(cLineas.getColumnIndex("flag5")))

                if (cLineas.getString(cLineas.getColumnIndex("tasa1")) == null)
                    serializer.attribute(null, "TASA1", "0")
                else
                    serializer.attribute(null, "TASA1", cLineas.getString(cLineas.getColumnIndex("tasa1")).replace('.', ','))
                if (cLineas.getString(cLineas.getColumnIndex("tasa2")) == null)
                    serializer.attribute(null, "TASA2", "0")
                else
                    serializer.attribute(null, "TASA2", cLineas.getString(cLineas.getColumnIndex("tasa2")).replace('.', ','))

                if (cLineas.getString(cLineas.getColumnIndex("formato")) == null)
                    serializer.attribute(null, "FORMATO", "0")
                else
                    serializer.attribute(null, "FORMATO", cLineas.getString(cLineas.getColumnIndex("formato")))

                if (cLineas.getString(cLineas.getColumnIndex("incidencia")) == null)
                    serializer.attribute(null, "INC", "0")
                else
                    serializer.attribute(null, "INC", cLineas.getString(cLineas.getColumnIndex("incidencia")))

                if (cLineas.getString(cLineas.getColumnIndex("textolinea")) == null)
                    serializer.attribute(null, "TEXTOLINEA", "")
                else
                    serializer.attribute(null, "TEXTOLINEA", cLineas.getString(cLineas.getColumnIndex("textolinea")))

                if (cLineas.getString(cLineas.getColumnIndex("almacenPedido")) == null)
                    serializer.attribute(null, "ALMACENPEDIDO", "")
                else
                    serializer.attribute(null, "ALMACENPEDIDO", cLineas.getString(cLineas.getColumnIndex("almacenPedido")))

                if (cLineas.getString(cLineas.getColumnIndex("idOferta")) == null)
                    serializer.attribute(null, "IDOFERTA", "")
                else
                    serializer.attribute(null, "IDOFERTA", cLineas.getString(cLineas.getColumnIndex("idOferta")))
                if (cLineas.getString(cLineas.getColumnIndex("dtoOftVol")) == null)
                    serializer.attribute(null, "DTOOFTVOL", "")
                else
                    serializer.attribute(null, "DTOOFTVOL", cLineas.getString(cLineas.getColumnIndex("dtoOftVol")))

                serializer.endTag("", "record")
                cLineas.moveToNext()
            }
            serializer.endTag("", "consulta")

            serializer.endDocument()
            serializer.flush()
            fout.close()
            fTamLineas = outputFile.length()

        } catch (e: FileNotFoundException) {
            //if (!fDesdeServicio) mostrarExcepcion(e)
            mostrarExcepcion(e)
        } catch (e: Exception) {
            //if (!fDesdeServicio) mostrarExcepcion(e)
            mostrarExcepcion(e)
        }
    }


    private fun lineasDtoAXML(cLineasDt: Cursor) {

        try {
            val outputFile = File(rutaLocalEnvio, "DesctosLineas.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            cLineasDt.moveToFirst()
            while (!cLineasDt.isAfterLast) {
                serializer.startTag("", "record")
                serializer.attribute(null, "LINEA", cLineasDt.getString(cLineasDt.getColumnIndex("linea")))
                serializer.attribute(null, "ORDEN", cLineasDt.getString(cLineasDt.getColumnIndex("orden")))
                serializer.attribute(null, "DESCUENTO", cLineasDt.getString(cLineasDt.getColumnIndex("descuento")).replace('.', ','))
                serializer.attribute(null, "IMPORTE", cLineasDt.getString(cLineasDt.getColumnIndex("importe")).replace('.', ','))
                serializer.attribute(null, "CANTIDAD1", cLineasDt.getString(cLineasDt.getColumnIndex("cantidad1")).replace('.', ','))
                serializer.attribute(null, "CANTIDAD2", cLineasDt.getString(cLineasDt.getColumnIndex("cantidad2")).replace('.', ','))
                serializer.endTag("", "record")
                cLineasDt.moveToNext()
            }
            serializer.endTag("", "consulta")

            serializer.endDocument()
            serializer.flush()
            fout.close()

        } catch (e: FileNotFoundException) {
            //if (!fDesdeServicio) mostrarExcepcion(e)
            mostrarExcepcion(e)
        } catch (e: Exception) {
            //if (!fDesdeServicio) mostrarExcepcion(e)
            mostrarExcepcion(e)
        }
    }


    private fun enviarCobros(queNumExportacion: Int, iSigExportacion: Int): Boolean {
        // Para obtener el cursor con las cabeceras creo sobre la marcha un objeto de tipo BaseDatos.
        val dbAlba = BaseDatos(fContext).writableDatabase
        val cobrosDao: CobrosDao? = MyDatabase.getInstance(fContext)?.cobrosDao()
        val numCobros: Int =
            if (queNumExportacion == 0) cobrosDao?.hayCobrosParaEnviar() ?: 0
            else cobrosDao?.hayCobrosEnExport(queNumExportacion) ?: 0

        if (queNumExportacion > 0) {
            return if (numCobros > 0) {
                val msg = Message()
                msg.obj = "Preparando cobros"
                puente.sendMessage(msg)

                cobrosExpAXML(queNumExportacion)
                true

            } else {
                false
            }
        } else {

            if (numCobros > 0) {

                // Tenemos que recorrer este bucle porque la tabla Cabeceras está en otra base de datos y
                // no podemos incluirla en la misma consulta que Cobros.
                var hayCobros = false
                val lCobros = cobrosDao?.abrirParaExportar() ?: emptyList<CobrosEnt>().toMutableList()

                for (cobro in lCobros) {
                    val cCabeceras = dbAlba.rawQuery("SELECT estado FROM cabeceras" +
                            " WHERE tipodoc = " + cobro.tipoDoc + " AND alm = " + cobro.almacen +
                            " AND serie = '" + cobro.serie + "' AND numero = " + cobro. numero +
                            " AND ejer = " + cobro.ejercicio, null)

                    if (cCabeceras.moveToFirst()) {
                        if (cCabeceras.getString(0) != "P" || cCabeceras.getString(0) == null) {
                            hayCobros = true
                            break
                        }
                    }
                    cCabeceras.close()
                }

                return if (hayCobros || lCobros.isNotEmpty()) {
                    val msg = Message()
                    msg.obj = "Preparando cobros"
                    puente.sendMessage(msg)

                    cobrosAXML()

                    // Marcamos los cobros como exportados.
                    if (queNumExportacion == 0) {
                        cobrosDao?.marcarComoExportados(iSigExportacion)
                    }

                    true
                } else false
            } else {
                return false
            }
        }
    }

    private fun cobrosAXML() {
        val cobrosDao: CobrosDao? = MyDatabase.getInstance(fContext)?.cobrosDao()
        val dbAlba = BaseDatos(fContext).readableDatabase

        try {
            val outputFile = File(rutaLocalEnvio, "Cobros.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            val lCobros = cobrosDao?.abrirParaExportar() ?: emptyList<CobrosEnt>().toMutableList()

            for (cobro in lCobros) {
                val cCabeceras = dbAlba.rawQuery("SELECT estado FROM cabeceras" +
                        " WHERE tipodoc = " + cobro.tipoDoc + " AND alm = " + cobro.almacen +
                        " AND serie = '" + cobro.serie + "' AND numero = " + cobro. numero +
                        " AND ejer = " + cobro.ejercicio, null)

                var continuar = true
                if (cCabeceras.moveToFirst()) {
                    continuar = cCabeceras.getString(0) != "P" || cCabeceras.getString(0) == null
                }
                cCabeceras.close()

                if (continuar) {
                    serializer.startTag("", "record")
                    serializer.attribute(null, "APUNTE", cobro.cobroId.toString())
                    serializer.attribute(null, "CLIENTE", cobro.clienteId.toString())
                    if (cobro.tipoDoc == 33.toShort())
                        serializer.attribute(null, "TIPODOC", 0.toString())
                    else
                        serializer.attribute(null, "TIPODOC", cobro.tipoDoc.toString())
                    serializer.attribute(null, "ALM", cobro.almacen.toString())
                    serializer.attribute(null, "SERIE", cobro.serie)
                    serializer.attribute(null, "NUMERO", cobro.numero.toString())
                    serializer.attribute(null, "EJER", cobro.ejercicio.toString())
                    serializer.attribute(null, "EMPRESA", cobro.empresa.toString())
                    serializer.attribute(null, "FECHACOBRO", cobro.fechaCobro)
                    serializer.attribute(null, "COBRO", cobro.cobro.replace('.', ','))
                    serializer.attribute(null, "FPAGO", cobro.fPago)
                    serializer.attribute(null, "DIVISA", cobro.divisa)
                    serializer.attribute(null, "ANOTACION", cobro.anotacion)
                    serializer.attribute(null, "CODIGO", cobro.codigo)
                    serializer.attribute(null, "ESTADO", "N")
                    serializer.attribute(null, "VALMACEN", cobro.vAlmacen)
                    serializer.attribute(null, "VPUESTO", cobro.vPuesto)
                    serializer.attribute(null, "VAPUNTE", cobro.vApunte)
                    serializer.attribute(null, "VEJER", cobro.vEjercicio)
                    serializer.attribute(null, "Matricula", cobro.matricula)

                    serializer.endTag("", "record")
                }
            }
            serializer.endTag("", "consulta")

            serializer.endDocument()
            serializer.flush()
            fout.close()
            fTamCobros = outputFile.length()

        } catch (e: FileNotFoundException) {
            mostrarExcepcion(e)
        } catch (e: Exception) {
            mostrarExcepcion(e)
        }
    }


    private fun cobrosExpAXML(queNumExportacion: Int) {
        val cobrosDao: CobrosDao? = MyDatabase.getInstance(fContext)?.cobrosDao()

        try {
            val outputFile = File(rutaLocalEnvio, "Cobros.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            val lCobros = cobrosDao?.abrirExportacion(queNumExportacion) ?: emptyList<CobrosEnt>().toMutableList()

            for (cobro in lCobros) {
                serializer.startTag("", "record")
                serializer.attribute(null, "APUNTE", cobro.cobroId.toString())
                serializer.attribute(null, "CLIENTE", cobro.clienteId.toString())
                serializer.attribute(null, "TIPODOC", cobro.tipoDoc.toString())
                serializer.attribute(null, "ALM", cobro.almacen.toString())
                serializer.attribute(null, "SERIE", cobro.serie)
                serializer.attribute(null, "NUMERO", cobro.numero.toString())
                serializer.attribute(null, "EJER", cobro.ejercicio.toString())
                serializer.attribute(null, "EMPRESA", cobro.empresa.toString())
                serializer.attribute(null, "FECHACOBRO", cobro.fechaCobro)
                serializer.attribute(null, "COBRO", cobro.cobro.replace('.', ','))
                serializer.attribute(null, "FPAGO", cobro.fPago)
                serializer.attribute(null, "DIVISA", cobro.divisa)
                serializer.attribute(null, "ANOTACION", cobro.anotacion)
                serializer.attribute(null, "CODIGO", cobro.codigo)
                serializer.attribute(null, "ESTADO", "N")
                serializer.attribute(null, "VALMACEN", cobro.vAlmacen)
                serializer.attribute(null, "VPUESTO", cobro.vPuesto)
                serializer.attribute(null, "VAPUNTE", cobro.vApunte)
                serializer.attribute(null, "VEJER", cobro.vEjercicio)
                serializer.attribute(null, "Matricula", cobro.matricula)

                serializer.endTag("", "record")
            }
            serializer.endTag("", "consulta")

            serializer.endDocument()
            serializer.flush()
            fout.close()
            fTamCobros = outputFile.length()

        } catch (e: FileNotFoundException) {
            mostrarExcepcion(e)
        } catch (e: Exception) {
            mostrarExcepcion(e)
        }
    }


    private fun enviarPendiente(queNumExportacion: Int, iSigExportacion: Int): Boolean {
        val pendienteDao: PendienteDao? = MyDatabase.getInstance(fContext)?.pendienteDao()

        // Grabamos en los vencimientos liquidados el número de exportación, para que se borren
        // la siguiente vez que recibamos.
        if (queNumExportacion == 0) {
            pendienteDao?.numExp2VtosLiquidados(iSigExportacion)
        }

        val cPdte: Cursor? = if (queNumExportacion == 0) {
            pendienteDao?.abrirParaEnviar()
        } else {
            pendienteDao?.abrirPorNumExport(queNumExportacion)
        }

        if (cPdte?.moveToFirst() == true) {
            val msg = Message()
            msg.obj = "Preparando pendiente"
            puente.sendMessage(msg)

            pendienteAXML(cPdte)
            cPdte.close()

            // Marcamos los pendiente como no enviar.
            if (queNumExportacion == 0) {
                pendienteDao?.marcarNoEnviar(iSigExportacion)
            }

            return true

        } else {
            return false
        }
    }


    private fun pendienteAXML(cPdte: Cursor) {

        try {
            val outputFile = File(rutaLocalEnvio, "Pendiente.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            cPdte.moveToFirst()
            while (!cPdte.isAfterLast) {
                serializer.startTag("", "record")
                serializer.attribute(null, "CLIENTE", cPdte.getString(cPdte.getColumnIndex("clienteId")))
                serializer.attribute(null, "EJER", cPdte.getString(cPdte.getColumnIndex("ejercicio")))
                serializer.attribute(null, "EMPRESA", cPdte.getString(cPdte.getColumnIndex("empresa")))
                serializer.attribute(null, "ALM", cPdte.getString(cPdte.getColumnIndex("almacen")))
                serializer.attribute(null, "TIPODOC", cPdte.getString(cPdte.getColumnIndex("tipoDoc")))
                if (cPdte.getString(cPdte.getColumnIndex("fPago")) == null) serializer.attribute(null, "FPAGO", "")
                else serializer.attribute(null, "FPAGO", cPdte.getString(cPdte.getColumnIndex("fPago")))
                serializer.attribute(null, "FECHADOC", cPdte.getString(cPdte.getColumnIndex("fechaDoc")))
                serializer.attribute(null, "SERIE", cPdte.getString(cPdte.getColumnIndex("serie")))
                serializer.attribute(null, "NUMERO", cPdte.getString(cPdte.getColumnIndex("numero")))
                serializer.attribute(null, "IMPORTE", cPdte.getString(cPdte.getColumnIndex("importe")).replace('.', ','))
                serializer.attribute(null, "COBRADO", cPdte.getString(cPdte.getColumnIndex("cobrado")).replace('.', ','))
                serializer.attribute(null, "FECHAVTO", cPdte.getString(cPdte.getColumnIndex("fechaVto")))
                serializer.attribute(null, "ESTADO", cPdte.getString(cPdte.getColumnIndex("estado")))
                serializer.attribute(null, "ENVIAR", cPdte.getString(cPdte.getColumnIndex("enviar")))
                serializer.attribute(null, "CALMACEN", cPdte.getString(cPdte.getColumnIndex("cAlmacen")))
                serializer.attribute(null, "CPUESTO", cPdte.getString(cPdte.getColumnIndex("cPuesto")))
                serializer.attribute(null, "CAPUNTE", cPdte.getString(cPdte.getColumnIndex("cApunte")))
                serializer.attribute(null, "CEJER", cPdte.getString(cPdte.getColumnIndex("cEjercicio")))
                if (cPdte.getString(cPdte.getColumnIndex("flag")) == null) serializer.attribute(null, "FLAG", "0")
                else serializer.attribute(null, "FLAG", cPdte.getString(cPdte.getColumnIndex("flag")))
                if (cPdte.getString(cPdte.getColumnIndex("anotacion")) == null) serializer.attribute(null, "ANOTACION", "")
                else serializer.attribute(null, "ANOTACION", cPdte.getString(cPdte.getColumnIndex("anotacion")))

                serializer.endTag("", "record")
                cPdte.moveToNext()
            }
            serializer.endTag("", "consulta")

            serializer.endDocument()
            serializer.flush()
            fout.close()
            fTamPdte = outputFile.length()

        } catch (e: FileNotFoundException) {
            //if (!fDesdeServicio) mostrarExcepcion(e)
            mostrarExcepcion(e)
        } catch (e: Exception) {
            //if (!fDesdeServicio) mostrarExcepcion(e)
            mostrarExcepcion(e)
        }
    }

    private fun crearCadenaResumen(hayClientes: Boolean, hayDirecc: Boolean, hayContactos: Boolean, hayNotas: Boolean,
                                   hayDocumentos: Boolean, hayCobros: Boolean, hayPendiente: Boolean, hayCargas: Boolean) {
        cadenaResumen = ""

        try {
            if (hayClientes) cadenaResumen += cadenaResFichero("Clientes.xml")
            if (hayDirecc) cadenaResumen += cadenaResFichero("DirClientes.xml")
            if (hayContactos) cadenaResumen += cadenaResFichero("ConClientes.xml")
            if (hayNotas) cadenaResumen += cadenaResFichero("NotasCltes.xml")

            if (hayDocumentos) {
                cadenaResumen += cadenaResFichero("Cabeceras.xml")
                cadenaResumen += cadenaResFichero("Lineas.xml")
                cadenaResumen += cadenaResFichero("DesctosLineas.xml")
            }

            if (hayCobros) cadenaResumen += cadenaResFichero("Cobros.xml")
            if (hayPendiente) cadenaResumen += cadenaResFichero("Pendiente.xml")
            if (hayCargas) cadenaResumen += cadenaResFichero("Cargas.xml")

            cadenaResumen = sha1(cadenaResumen)
            cadenaResumen = Base64.encodeBase64String(cadenaResumen.toByteArray())

        } catch (e: Exception) {
            mostrarExcepcion(e)
        }
    }


    private fun cadenaResFichero(queFichero: String): String {
        var queCadena = ""
        val f = File(rutaLocalEnvio, queFichero)
        val fis = FileInputStream(f)

        var buffer: ByteArray
        var continuar = true
        while (continuar) {
            buffer = fis.readBytes()
            if (buffer.isNotEmpty()) queCadena += String(buffer)
            else continuar = false
        }
        fis.close()

        return queCadena
    }


    private fun crearLog(hayClientes: Boolean, hayDirecc: Boolean, hayContactos: Boolean, hayNotas: Boolean,
                         hayDocumentos: Boolean, hayCobros: Boolean, hayPendiente: Boolean, hayCargas: Boolean) {
        try {
            val outputFile = File(rutaLocalEnvio, "Log.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            try {
                serializer.setOutput(fout, "UTF-8")
                serializer.startDocument(null, true)
                serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

                serializer.startTag("", "Log")

                if (hayClientes) {
                    serializer.startTag("", "record")
                    serializer.attribute(null, "Table", "Clientes.xml")
                    serializer.attribute(null, "Size", fTamCltes.toString())
                    serializer.endTag("", "record")
                }
                if (hayDirecc) {
                    serializer.startTag("", "record")
                    serializer.attribute(null, "Table", "DirClientes.xml")
                    serializer.attribute(null, "Size", fTamDirecc.toString())
                    serializer.endTag("", "record")
                }
                if (hayContactos) {
                    serializer.startTag("", "record")
                    serializer.attribute(null, "Table", "ConClientes.xml")
                    serializer.attribute(null, "Size", fTamContactos.toString())
                    serializer.endTag("", "record")
                }
                if (hayNotas) {
                    serializer.startTag("", "record")
                    serializer.attribute(null, "Table", "NotasCltes.xml")
                    serializer.attribute(null, "Size", fTamNotasCltes.toString())
                    serializer.endTag("", "record")
                }
                if (hayDocumentos) {
                    serializer.startTag("", "record")
                    serializer.attribute(null, "Table", "Cabeceras.xml")
                    serializer.attribute(null, "Size", fTamCabec.toString())
                    serializer.endTag("", "record")

                    serializer.startTag("", "record")
                    serializer.attribute(null, "Table", "Lineas.xml")
                    serializer.attribute(null, "Size", fTamLineas.toString())
                    serializer.endTag("", "record")
                }
                if (hayCobros) {
                    serializer.startTag("", "record")
                    serializer.attribute(null, "Table", "Cobros.xml")
                    serializer.attribute(null, "Size", fTamCobros.toString())
                    serializer.endTag("", "record")
                }
                if (hayPendiente) {
                    serializer.startTag("", "record")
                    serializer.attribute(null, "Table", "Pendiente.xml")
                    serializer.attribute(null, "Size", fTamPdte.toString())
                    serializer.endTag("", "record")
                }
                if (hayCargas) {
                    serializer.startTag("", "record")
                    serializer.attribute(null, "Table", "Cargas.xml")
                    serializer.attribute(null, "Size", fTamCargas.toString())
                    serializer.endTag("", "record")

                    serializer.startTag("", "record")
                    serializer.attribute(null, "Table", "CargasLineas.xml")
                    serializer.attribute(null, "Size", fTamCargasLineas.toString())
                    serializer.endTag("", "record")
                }

                serializer.endTag("", "Log")

                serializer.endDocument()
                serializer.flush()
                fout.close()

            } catch (e: Exception) {
                mostrarExcepcion(e)
            }
        } catch (e: FileNotFoundException) {
            mostrarExcepcion(e)
        }
    }


    private fun copiarEnvio() {
        val rutaLocalEnvio = File(rutaLocalEnvio)

        var rutaCopia = prefs.getString("rutacomunicacion", "")
        rutaCopia = if (rutaCopia == "") "/storage/sdcard0/alba/copia/$fCodTerminal"
        else "$rutaCopia/copia/$fCodTerminal"

        // Nos aseguramos de que la carpeta de copia existe y, si no, la creamos.
        val rutaCopEnvio = File(rutaCopia)
        if (!rutaCopEnvio.exists())
            rutaCopEnvio.mkdirs()

        val xmlFiles = rutaLocalEnvio.listFiles()
        if (xmlFiles != null && xmlFiles.isNotEmpty()) {

            // Bucle que recorre la lista de ficheros.
            for (file in xmlFiles) {

                try {
                    val outFile = File(rutaCopia + "/" + file.name)

                    val fInput = FileInputStream(file)
                    val fOutput = FileOutputStream(outFile)

                    var c: Int
                    var fContinuar = true
                    while (fContinuar) {
                        c = fInput.read()
                        if (c != -1) fOutput.write(c)
                        else fContinuar = false
                    }

                    fInput.close()
                    fOutput.close()
                } catch (e: IOException) {
                    //if (!fDesdeServicio) mostrarExcepcion(e)
                    mostrarExcepcion(e)
                }
            }
        }
    }


}