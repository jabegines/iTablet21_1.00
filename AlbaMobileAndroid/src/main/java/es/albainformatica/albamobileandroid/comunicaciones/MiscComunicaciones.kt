package es.albainformatica.albamobileandroid.comunicaciones

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.util.Xml
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.R.string
import es.albainformatica.albamobileandroid.dao.*
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.database.MyDatabase.Companion.queBDRoom
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
    private var cabecerasDao: CabecerasDao? = MyDatabase.getInstance(context)?.cabecerasDao()
    private val lineasDao: LineasDao? = MyDatabase.getInstance(context)?.lineasDao()
    private val cargasLineasDao: CargasLineasDao? = MyDatabase.getInstance(context)?.cargasLineasDao()
    private var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val fContext: Context = context
    private var fTerminar: Boolean = false
    private val fDesdeServicio: Boolean = desdeServicio
    private var rutaLocal: String
    var rutaLocalEnvio: String
    private var fCodTerminal: String = prefs.getString("terminal", "") ?: ""
    private var fUsarMultisistema: Boolean = false

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
            if (fUsarMultisistema) "/storage/sdcard0/alba/envio/$fCodTerminal/$queBDRoom"
            else "/storage/sdcard0/alba/envio/$fCodTerminal"
        } else {
            if (fUsarMultisistema) "$rutaLocal/envio/$fCodTerminal/$queBDRoom"
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

                // Vaciamos todas las tablas si no estamos usando el servicio
                if (!fDesdeServicio) {
                    val cursor = MyDatabase.getInstance(fContext)?.openHelper?.readableDatabase?.query("SELECT name FROM sqlite_master WHERE type = 'table'")
                    val sqlDatabase = MyDatabase.getInstance(fContext)?.openHelper?.writableDatabase
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            while (!cursor.isAfterLast) {
                                val tableName = cursor.getString(0) ?: ""
                                if (tableName != "android_metadata"
                                    && tableName != "sqlite_sequence"
                                    && tableName != "room_master_table") {
                                    sqlDatabase?.delete(tableName, null, null)
                                }
                                cursor.moveToNext()
                            }
                        }
                    }
                }

                val numArchivos = xmlFiles.size
                var i = 1

                // Borraremos aquí los documentos enviados porque puede darse el caso de que hayamos enviado documentos pero
                // no recibimos desde la central, en cuyo caso no se llamará a importarCabeceras(). Idem con el pendiente.
                if (fDesdeServicio) {
                    try {
                        lineasDao?.borrarEnviadas()
                        cabecerasDao?.borrarEnviadas()

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

                        // Idem con costos
                        val costosDao: CostosArticulosDao? = MyDatabase.getInstance(fContext)?.costosArticulosDao()
                        costosDao?.vaciar()

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
                        nombreFich.equals("Facturas.xml", true) -> importarCabeceras("Facturas.xml", TIPODOC_FACTURA)
                        nombreFich.equals("Albaranes.xml", true) -> importarCabeceras("Albaranes.xml", TIPODOC_ALBARAN)
                        nombreFich.equals("Pedidos.xml", true) -> importarCabeceras("Pedidos.xml", TIPODOC_PEDIDO)
                        nombreFich.equals("Presupuestos.xml", true) -> importarCabeceras("Presupuestos.xml", TIPODOC_PRESUPUESTO)
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
                                sCampo.equals("Iva", true) -> articEnt.tipoIva = parser.getAttributeValue("", sCampo).toShort()
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
        val clientesDao: ClientesDao? = MyDatabase.getInstance(fContext)?.clientesDao()
        val f = File(rutaLocal, "Clientes.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Si recibimos desde el servicio dejaremos sin borrar aquellos clientes nuevos, los modificados
                // y los que estén en algún documento sin enviar. Luego los incorporaremos a los que recibimos desde la central.
                if (fDesdeServicio) {
                    clientesDao?.borrarViejos()
                    clientes2TemporalCltes(clientesDao)
                }
                clientesDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val clienteEnt = ClientesEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Cliente", ignoreCase = true) -> clienteEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Codigo", ignoreCase = true) -> clienteEnt.codigo = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Nomfi", ignoreCase = true) -> clienteEnt.nombre = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Nomco", ignoreCase = true) -> clienteEnt.nombreComercial = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Cif", ignoreCase = true) -> clienteEnt.cif = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Direcc", ignoreCase = true) -> clienteEnt.direccion = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Locali", ignoreCase = true) -> clienteEnt.localidad = parser.getAttributeValue("", sCampo)
                                sCampo.equals("CPostal", ignoreCase = true) -> clienteEnt.cPostal = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Provin", ignoreCase = true) -> clienteEnt.provincia = parser.getAttributeValue("", sCampo)
                                sCampo.equals("AplIva", ignoreCase = true) -> clienteEnt.aplIva = parser.getAttributeValue("", sCampo)
                                sCampo.equals("AplRec", ignoreCase = true) -> clienteEnt.aplRec = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Iva", ignoreCase = true) -> clienteEnt.tipoIva = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Tarifa", ignoreCase = true) -> clienteEnt.tarifaId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("TarDto", ignoreCase = true) -> clienteEnt.tarifaDtoId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("TarifaPiezas", true) -> clienteEnt.tarifaPiezas = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("FPago", ignoreCase = true) -> clienteEnt.fPago = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Riesgo", ignoreCase = true) -> clienteEnt.riesgo = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Ruta", ignoreCase = true) -> clienteEnt.rutaId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Flag", ignoreCase = true) -> clienteEnt.flag = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Flag2", ignoreCase = true) -> clienteEnt.flag2 = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Ramo", ignoreCase = true) -> clienteEnt.ramo = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Pendiente", ignoreCase = true) -> clienteEnt.pendiente = parser.getAttributeValue("", sCampo)
                                sCampo.equals("MaxDias", ignoreCase = true) -> clienteEnt.maxDias = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("MaxPendientes", ignoreCase = true) -> clienteEnt.maxFrasPdtes = parser.getAttributeValue("", sCampo).toInt()
                            }
                        }
                        // Llenamos el campo "tieneincid" a falso.
                        clienteEnt.tieneIncid = "F"

                        if (clienteEnt.clienteId > 0) {
                            clientesDao?.insertar(clienteEnt)
                        }
                    }
                    event = parser.next()
                }
                fin.close()
                // Realizamos el proceso contrario: copiamos desde la tabla temporal a la de clientes
                if (fDesdeServicio)
                    temporalCltes2Cltes(clientesDao)

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


    private fun temporalCltes2Cltes(clientesDao: ClientesDao?) {
        val tempCltesDao: TempCltesDao? = MyDatabase.getInstance(fContext)?.tempCltesDao()
        val lTemp = tempCltesDao?.getAllCltes() ?: emptyList<ClientesEnt>().toMutableList()

        for (clienteEnt in lTemp) {

            val queClienteId = clientesDao?.existeClteId(clienteEnt.clienteId) ?: 0
            val existe = queClienteId > 0

            if (clienteEnt.estado == "N") {
                // Si ya hemos recibido un cliente con el mismo valor en el campo "Cliente", no lo añadimos.
                // Damos prioridad a lo que venga desde el central.
                if (!existe)
                    clientesDao?.insertar(clienteEnt)

            } else {
                if (existe)
                    clientesDao?.actualizar(clienteEnt)

                // Puede ser que el cliente que hemos modificado no lo estemos reciendo ahora desde la central, por eso añadimos.
                else
                    clientesDao?.insertar(clienteEnt)
            }
        }
        // Recalculamos los saldos, porque hemos podido dejar registros en la tabla "Pendiente" sin enviar.
        // Por ahora entendemos que no hace falta recalcular el pendiente.
        recalcularSaldos()
    }

    private fun recalcularStocks() {
        val fArticulos = ArticulosClase(fContext)

        // Sumamos al stock las líneas de los documentos no enviados
        val lLineas = lineasDao?.getNoEnviadas() ?: emptyList<DatosLinRecStock>().toMutableList()

        for (linea in lLineas) {
            val queArticulo = linea.articuloId
            val queEmpresa = linea.empresa
            val sCajas = linea.cajas
            val dCajas: Double = if (sCajas == "") 0.0
            else sCajas.toDouble()

            val sCantidad = linea.cantidad
            val dCantidad: Double = if (sCantidad == "") 0.0
            else sCantidad.toDouble()

            fArticulos.actualizarStock(queArticulo, queEmpresa, dCantidad, dCajas, false)
        }

        // Sumamos ahora al stock las cargas no enviadas. También aprovechamos y borramos las cargas que estén enviadas.
        val cargasDao: CargasDao? = MyDatabase.getInstance(fContext)?.cargasDao()
        cargasLineasDao?.borrarEnviadas()
        cargasDao?.borrarEnviadas()

        val lCargas = cargasLineasDao?.getNoEnviadas() ?: emptyList<DatosLinRecStock>().toMutableList()

        for (linea in lCargas) {
            val queArticulo = linea.articuloId
            val queEmpresa = linea.empresa
            val sCajas = linea.cajas
            val dCajas: Double = if (sCajas == "") 0.0
            else sCajas.toDouble() * -1

            val sCantidad = linea.cantidad
            val dCantidad: Double = if (sCantidad == "") 0.0
            else sCantidad.toDouble() * -1

            fArticulos.actualizarStock(queArticulo, queEmpresa, dCantidad, dCajas, false)
        }
    }

    private fun recalcularLotes() {
        val fLotes = LotesClase(fContext)

        // Recalculamos los lotes de las líneas de venta no enviadas
        val lLineas = lineasDao?.getNoEnviadas() ?: emptyList<DatosLinRecStock>().toMutableList()

        for (linea in lLineas) {
            val queArticulo = linea.articuloId
            val sCantidad = linea.cantidad
            val dCantidad: Double = if (sCantidad == "") 0.0
            else sCantidad.toDouble()

            val queLote = linea.lote
            val queEmpresa = linea.empresa

            fLotes.actStockLote(queArticulo, dCantidad, queLote, queEmpresa)
        }

        // Recalculamos los lotes de las cargas no enviadas
        val lCargas = cargasLineasDao?.getNoEnviadas() ?: emptyList<DatosLinRecStock>().toMutableList()

        for (linea in lCargas) {
            val queArticulo = linea.articuloId
            val sCantidad = linea.cantidad
            val dCantidad: Double = if (sCantidad == "") 0.0
            else sCantidad.toDouble() * -1

            val queLote = linea.lote
            val queEmpresa = linea.empresa

            fLotes.actStockLote(queArticulo, dCantidad, queLote, queEmpresa)
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



    private fun clientes2TemporalCltes(clientesDao: ClientesDao?) {

        val tempCltesDao: TempCltesDao? = MyDatabase.getInstance(fContext)?.tempCltesDao()
        tempCltesDao?.vaciar()

        val lCltes = clientesDao?.getAllCltes() ?: emptyList<ClientesEnt>().toMutableList()
        for (cliente in lCltes) {
            tempCltesDao?.insertar(cliente)
        }
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
    private fun importarCabeceras(nombreFichXMl: String, queTipoDoc: Short) {
        val f = File(rutaLocal, nombreFichXMl)
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                parser.setInput(fin, "UTF-8")
                var event = parser.next()
                var cabeceraId: Long = 0

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        if (parser.name == "registro") {
                            val cabeceraEnt = CabecerasEnt()
                            // Hay tablas donde el camo Facturado no viene, por eso lo inicializamos a "F".
                            cabeceraEnt.facturado = "F"
                            cabeceraEnt.tipoDoc = queTipoDoc

                            for (i in 0 until parser.attributeCount) {
                                sCampo = parser.getAttributeName(i)

                                when {
                                    sCampo.equals("Facturado", ignoreCase = true) -> cabeceraEnt.facturado = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Almacen", ignoreCase = true) -> cabeceraEnt.almacen = parser.getAttributeValue("", sCampo).toShort()
                                    sCampo.equals("Serie", ignoreCase = true) -> cabeceraEnt.serie = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Numero", ignoreCase = true) -> cabeceraEnt.numero = parser.getAttributeValue("", sCampo).toInt()
                                    sCampo.equals("Ejercicio", ignoreCase = true) -> cabeceraEnt.ejercicio = parser.getAttributeValue("", sCampo).toShort()
                                    sCampo.equals("Empresa", ignoreCase = true) -> cabeceraEnt.empresa = parser.getAttributeValue("", sCampo).toShort()
                                    sCampo.equals("Fecha", ignoreCase = true) -> {
                                        val sFecha = parser.getAttributeValue("", sCampo)
                                        val sFecha2 = (sFecha.substring(8, 10) + "/" + sFecha.substring(5, 7) + "/" + sFecha.substring(0, 4))
                                        cabeceraEnt.fecha = sFecha2
                                    }
                                    sCampo.equals("Cliente", ignoreCase = true) -> cabeceraEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                    sCampo.equals("AplicarIva", ignoreCase = true) -> cabeceraEnt.aplicarIva = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("AplicarRecargo", ignoreCase = true) -> cabeceraEnt.aplicarRe = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Bruto", ignoreCase = true) -> cabeceraEnt.bruto = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Base", ignoreCase = true) -> cabeceraEnt.base = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Dto", ignoreCase = true) -> cabeceraEnt.dto = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Iva", ignoreCase = true) -> cabeceraEnt.iva = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Recargo", ignoreCase = true) -> cabeceraEnt.recargo = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Total", ignoreCase = true) -> cabeceraEnt.total = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Estado", ignoreCase = true) -> {
                                        cabeceraEnt.estado = parser.getAttributeValue("", sCampo)
                                        cabeceraEnt.estadoInicial = parser.getAttributeValue("", sCampo)
                                    }
                                    sCampo.equals("Flag", ignoreCase = true) -> cabeceraEnt.flag = parser.getAttributeValue("", sCampo).toInt()
                                    sCampo.equals("Observ1", ignoreCase = true) -> cabeceraEnt.observ1 = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Observ2", ignoreCase = true) -> cabeceraEnt.observ2 = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Hoja", ignoreCase = true) -> cabeceraEnt.hojaReparto = parser.getAttributeValue("", sCampo).toInt()
                                    sCampo.equals("Orden", ignoreCase = true) -> cabeceraEnt.ordenReparto = parser.getAttributeValue("", sCampo).toInt()
                                }
                            }
                            // Llenamos el campo "firmado" a falso.
                            cabeceraEnt.firmado = "F"

                            cabeceraId = cabecerasDao?.insertar(cabeceraEnt) ?: 0

                        } else if (parser.name == "linea") {
                            val lineaEnt = LineasEnt()
                            for (i in 0 until parser.attributeCount) {
                                sCampo = parser.getAttributeName(i)

                                when {
                                    //sCampo.equals("Linea", ignoreCase = true) -> lineaEnt.lineaId = parser.getAttributeValue("", sCampo).toInt()
                                    sCampo.equals("Articulo", ignoreCase = true) -> lineaEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                    sCampo.equals("Codigo", ignoreCase = true) -> lineaEnt.codArticulo = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Descripcion", ignoreCase = true) -> lineaEnt.descripcion = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Tarifa", ignoreCase = true) -> lineaEnt.tarifaId = parser.getAttributeValue("", sCampo).toShort()
                                    sCampo.equals("Precio", ignoreCase = true) -> lineaEnt.precio = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Importe", ignoreCase = true) -> lineaEnt.importe = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Cajas", ignoreCase = true) -> lineaEnt.cajas = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Cantidad", ignoreCase = true) -> lineaEnt.cantidad = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Piezas", ignoreCase = true) -> lineaEnt.piezas = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("Dto", ignoreCase = true) -> lineaEnt.dto = parser.getAttributeValue("", sCampo)
                                    sCampo.equals("TipoIva", ignoreCase = true) -> lineaEnt.codigoIva = parser.getAttributeValue("", sCampo).toShort()
                                    sCampo.equals("Flag", ignoreCase = true) -> lineaEnt.flag = parser.getAttributeValue("", sCampo).toInt()
                                    sCampo.equals("Flag3", ignoreCase = true) -> lineaEnt.flag3 = parser.getAttributeValue("", sCampo).toInt()
                                    sCampo.equals("Formato", ignoreCase = true) -> lineaEnt.formatoId = parser.getAttributeValue("", sCampo).toShort()
                                }
                            }
                            lineaEnt.cabeceraId = cabeceraId.toInt()

                            lineasDao?.insertar(lineaEnt)
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
        val dtosCltesDao: DtosCltesDao? = MyDatabase.getInstance(fContext)?.dtosCltesDao()
        val f = File(rutaLocal, "DtosClientes.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                dtosCltesDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val dtoClteEnt = DtosCltesEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Cliente", true) -> dtoClteEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("IdDescuento", true) -> dtoClteEnt.idDescuento = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Dto", true) -> dtoClteEnt.dto = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (dtoClteEnt.clienteId > 0) {
                            dtosCltesDao?.insertar(dtoClteEnt)
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


    private fun importarCnfTarifas() {
        val cnfTarifasDao: CnfTarifasDao? = MyDatabase.getInstance(fContext)?.cnfTarifasDao()
        val f = File(rutaLocal, "CnfTarifas.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                cnfTarifasDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val cnfTarifaEnt = CnfTarifasEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Codigo", true) -> cnfTarifaEnt.codigo = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Tarifa", true) -> cnfTarifaEnt.descrTarifa = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Precios", true) -> cnfTarifaEnt.precios = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Flag", true) -> cnfTarifaEnt.flag = parser.getAttributeValue("", sCampo).toInt()
                            }
                        }
                        if (cnfTarifaEnt.codigo > 0) {
                            cnfTarifasDao?.insertar(cnfTarifaEnt)
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
        val direccCltesDao: DireccCltesDao? = MyDatabase.getInstance(fContext)?.direccCltesDao()
        val f = File(rutaLocal, "DirClientes.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                direccCltesDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val dirClteEnt = DireccCltesEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Cliente", true) -> dirClteEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Alm", true) -> dirClteEnt.almacen = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Orden", true) -> dirClteEnt.orden = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Sucursal", true) -> dirClteEnt.sucursal = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Direcc", true) -> dirClteEnt.direccion = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Poblac", true) -> dirClteEnt.localidad = parser.getAttributeValue("", sCampo)
                                sCampo.equals("CPostal", true) -> dirClteEnt.cPostal = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Provin", true) -> dirClteEnt.provincia = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Pais", true) -> dirClteEnt.pais = parser.getAttributeValue("", sCampo)
                                sCampo.equals("DirDoc", true) -> dirClteEnt.direccionDoc = parser.getAttributeValue("", sCampo)
                                sCampo.equals("DirMer", true) -> dirClteEnt.direccionMerc = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (dirClteEnt.clienteId > 0) {
                            direccCltesDao?.insertar(dirClteEnt)
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



    private fun importarHco() {
        val historicoDao: HistoricoDao? = MyDatabase.getInstance(fContext)?.historicoDao()
        val f = File(rutaLocal, "Historico.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                historicoDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val historicoEnt = HistoricoEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Cliente", ignoreCase = true) -> historicoEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Articulo", ignoreCase = true) -> historicoEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Cajas", ignoreCase = true) -> historicoEnt.cajas = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Cantidad", ignoreCase = true) -> historicoEnt.cantidad = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Piezas", ignoreCase = true) -> historicoEnt.piezas = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Precio", ignoreCase = true) -> historicoEnt.precio = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Dto", ignoreCase = true) -> historicoEnt.dto = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Formato", ignoreCase = true) -> historicoEnt.formatoId = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Fecha", ignoreCase = true) -> {
                                    val sFecha = parser.getAttributeValue("", sCampo)
                                    val sFecha2 = sFecha.substring(8, 10) + "/" + sFecha.substring(5, 7) + "/" + sFecha.substring(0, 4)
                                    historicoEnt.fecha = sFecha2
                                }
                            }
                        }
                        if (historicoEnt.clienteId > 0) {
                            historicoDao?.insertar(historicoEnt)
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


    private fun importarHcoMes() {
        val histMesDao: HistMesDao? = MyDatabase.getInstance(fContext)?.histMesDao()
        val f = File(rutaLocal, "HistMes.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                histMesDao?.vaciar()

                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val histMesEnt = HistMesEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Cliente", ignoreCase = true) -> histMesEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Articulo", ignoreCase = true) -> histMesEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Mes", ignoreCase = true) -> histMesEnt.mes = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Cantidad", ignoreCase = true) -> histMesEnt.cantidad = parser.getAttributeValue("", sCampo)
                                sCampo.equals("CantidadAnt", ignoreCase = true) -> histMesEnt.cantidadAnt = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Importe", ignoreCase = true) -> histMesEnt.importe = parser.getAttributeValue("", sCampo)
                                sCampo.equals("ImporteAnt", ignoreCase = true) -> histMesEnt.importeAnt = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Cajas", ignoreCase = true) -> histMesEnt.cajas = parser.getAttributeValue("", sCampo)
                                sCampo.equals("CajasAnt", ignoreCase = true) -> histMesEnt.cajasAnt = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Piezas", ignoreCase = true) -> histMesEnt.piezas = parser.getAttributeValue("", sCampo)
                                sCampo.equals("PiezasAnt", ignoreCase = true) -> histMesEnt.piezasAnt = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (histMesEnt.clienteId > 0) {
                            histMesDao?.insertar(histMesEnt)
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
        val articClasifDao: ArticClasifDao? = MyDatabase.getInstance(fContext)?.articClasifDao()
        val f = File(rutaLocal, "ArticClasif.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                articClasifDao?.vaciar()
                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val articClasifEnt = ArticClasifEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Articulo", ignoreCase = true) -> articClasifEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Clasificador", ignoreCase = true) -> articClasifEnt.clasificadorId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Orden", ignoreCase = true) -> articClasifEnt.orden = parser.getAttributeValue("", sCampo).toInt()
                            }
                        }
                        if (articClasifEnt.articuloId > 0) {
                            articClasifDao?.insertar(articClasifEnt)
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
        val hcoComSemMesDao: HcoCompSemMesDao? = MyDatabase.getInstance(fContext)?.hcoCompSemMesDao()
        val f = File(rutaLocal, "HcoCompSemMes.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                hcoComSemMesDao?.vaciar()
                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val hcoCompEnt = HcoCompSemMesEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Fecha", ignoreCase = true) -> hcoCompEnt.fecha = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Cliente", ignoreCase = true) -> hcoCompEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Articulo", ignoreCase = true) -> hcoCompEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Cantidad", ignoreCase = true) -> hcoCompEnt.cantidad = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (hcoCompEnt.articuloId > 0) {
                            hcoComSemMesDao?.insertar(hcoCompEnt)
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



    private fun importarEstadDevoluc() {
        val estadDevolDao: EstadDevolucDao? = MyDatabase.getInstance(fContext)?.estadDevolucDao()
        val f = File(rutaLocal, "EstadDevoluc.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                estadDevolDao?.vaciar()
                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val estadDevEnt = EstadDevolucEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Cliente", ignoreCase = true) -> estadDevEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Articulo", ignoreCase = true) -> estadDevEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("PorcDevol", ignoreCase = true) -> estadDevEnt.porcDevol = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (estadDevEnt.clienteId > 0) {
                            estadDevolDao?.insertar(estadDevEnt)
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

    private fun importarHcoArticClte() {
        val hcoPorArticClteDao: HcoPorArticClteDao? = MyDatabase.getInstance(fContext)?.hcoPorArticClteDao()
        val f = File(rutaLocal, "HcoPorArticClte.xml")
        val fin = FileInputStream(f)
        var sCampo: String

        try {
            val parser = Xml.newPullParser()
            try {
                // Borro la tabla.
                hcoPorArticClteDao?.vaciar(

                )
                parser.setInput(fin, "UTF-8")
                var event = parser.next()

                while (event != XmlPullParser.END_DOCUMENT && !fTerminar) {
                    if (event == XmlPullParser.START_TAG) {
                        val hcoPorArtCltEnt = HcoPorArticClteEnt()
                        for (i in 0 until parser.attributeCount) {
                            sCampo = parser.getAttributeName(i)

                            when {
                                sCampo.equals("Articulo", ignoreCase = true) -> hcoPorArtCltEnt.articuloId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Cliente", ignoreCase = true) -> hcoPorArtCltEnt.clienteId = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("TipoDoc", ignoreCase = true) -> hcoPorArtCltEnt.tipoDoc = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Serie", ignoreCase = true) -> hcoPorArtCltEnt.serie = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Numero", ignoreCase = true) -> hcoPorArtCltEnt.numero = parser.getAttributeValue("", sCampo).toInt()
                                sCampo.equals("Ejercicio", ignoreCase = true) -> hcoPorArtCltEnt.ejercicio = parser.getAttributeValue("", sCampo).toShort()
                                sCampo.equals("Fecha", ignoreCase = true) -> hcoPorArtCltEnt.fecha = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Ventas", ignoreCase = true) -> hcoPorArtCltEnt.ventas = parser.getAttributeValue("", sCampo)
                                sCampo.equals("Devoluciones", ignoreCase = true) -> hcoPorArtCltEnt.devoluciones = parser.getAttributeValue("", sCampo)
                            }
                        }
                        if (hcoPorArtCltEnt.articuloId > 0) {
                            hcoPorArticClteDao?.insertar(hcoPorArtCltEnt)
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
                val lClientes = fClientes.abrirParaEnviar(queNumExportacion)
                if (lClientes.isNotEmpty()) {
                    hayClientes = true
                    enviarClientes(lClientes)
                }

                val direccCltesDao: DireccCltesDao? = MyDatabase.getInstance(fContext)?.direccCltesDao()
                val lDirecciones: List<DireccCltesEnt> = if (queNumExportacion > 0)
                    direccCltesDao?.getDirParaEnvExp(queNumExportacion) ?: emptyList<DireccCltesEnt>().toMutableList()
                else
                    direccCltesDao?.getDirParaEnv() ?: emptyList<DireccCltesEnt>().toMutableList()
                if (lDirecciones.isNotEmpty()) {
                    hayDirecc = true
                    enviarDirecciones(lDirecciones)
                }

                val cTelfClteDao: ContactosCltesDao? = MyDatabase.getInstance(fContext)?.contactosCltesDao()
                val lTelefonos: List<ContactosCltesEnt> = if (queNumExportacion > 0)
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

        cabecerasDao?.actualizarNumPaquete(fNumPaquete)

        val pendienteDao: PendienteDao? = MyDatabase.getInstance(fContext)?.pendienteDao()
        pendienteDao?.actualizarNumExport(fNumPaquete)

        val cobrosDao: CobrosDao? = MyDatabase.getInstance(fContext)?.cobrosDao()
        cobrosDao?.actualizarNumPaquete(fNumPaquete)

        // Insertamos en numexport el número de paquete junto con la fecha y hora actuales
        val tim = System.currentTimeMillis()
        val df = SimpleDateFormat("dd/mm/yyyy")
        val fFecha = df.format(tim)
        val dfHora = SimpleDateFormat("hh:mm")
        val fHora = dfHora.format(tim)

        val numExportDao: NumExportDao? = MyDatabase.getInstance(fContext)?.numExportDao()
        val numExportEnt = NumExportEnt()
        numExportEnt.numExport = fNumPaquete
        numExportEnt.fecha = fFecha
        numExportEnt.hora = fHora
        numExportDao?.insertar(numExportEnt)
    }


    fun revertirEstado() {
        // Volvemos a establecer el estado 'N' para que los registros sean enviados la próxima vez
        val cargasDao: CargasDao? = MyDatabase.getInstance(fContext)?.cargasDao()
        val pendienteDao: PendienteDao? = MyDatabase.getInstance(fContext)?.pendienteDao()
        val cobrosDao: CobrosDao? = MyDatabase.getInstance(fContext)?.cobrosDao()

        cabecerasDao?.revertirEstado()
        cargasDao?.revertirEstado()
        pendienteDao?.revertirEstado()
        cobrosDao?.revertirEstado()
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


    private fun enviarClientes(lClientes: List<ClientesEnt>) {
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
            for (clienteEnt in lClientes) {
                // Construimos el XML
                serializer.startTag("", "record")
                serializer.attribute(null, "Cliente", clienteEnt.clienteId.toString())
                serializer.attribute(null, "Codigo", clienteEnt.codigo.toString())
                serializer.attribute(null, "Nomfi", clienteEnt.nombre)
                serializer.attribute(null, "Nomco", clienteEnt.nombreComercial)
                serializer.attribute(null, "Cif", clienteEnt.cif)
                serializer.attribute(null, "Direcc", clienteEnt.direccion)
                serializer.attribute(null, "Locali", clienteEnt.localidad)
                serializer.attribute(null, "CPostal", clienteEnt.cPostal)
                serializer.attribute(null, "Provin", clienteEnt.provincia)
                serializer.attribute(null, "AplIva", clienteEnt.aplIva)
                serializer.attribute(null, "ApRec", clienteEnt.aplRec)
                serializer.attribute(null, "Iva", "NULL")
                if (clienteEnt.tarifaId <= 0)
                    serializer.attribute(null, "Tarifa", "NULL")
                else
                    serializer.attribute(null, "Tarifa", clienteEnt.tarifaId.toString())
                if (clienteEnt.tarifaDtoId <= 0)
                    serializer.attribute(null, "TarDto", "NULL")
                else
                    serializer.attribute(null, "TarDto", clienteEnt.tarifaDtoId.toString())

                serializer.attribute(null, "FPago", clienteEnt.fPago)
                serializer.attribute(null, "Ruta", clienteEnt.rutaId.toString())
                serializer.attribute(null, "Riesgo", clienteEnt.riesgo.replace('.', ','))
                serializer.attribute(null, "Flag", clienteEnt.flag.toString())

                when (clienteEnt.estado) {
                    "XN" -> serializer.attribute(null, "Estado", "N")
                    "XM" -> serializer.attribute(null, "Estado", "M")
                    else -> serializer.attribute(null, "Estado", clienteEnt.estado)
                }

                serializer.endTag("", "record")
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

    private fun enviarDirecciones(lDirecciones: List<DireccCltesEnt>) {
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
            for (direccion in lDirecciones) {
                serializer.startTag("", "record")
                serializer.attribute(null, "Cliente", direccion.clienteId.toString())
                serializer.attribute(null, "Orden", direccion.orden.toString())
                serializer.attribute(null, "Direcc", direccion.direccion)
                serializer.attribute(null, "Poblac", direccion.localidad)
                serializer.attribute(null, "CPostal", direccion.cPostal)
                serializer.attribute(null, "Provin", direccion.provincia)
                serializer.attribute(null, "Pais", direccion.pais)
                serializer.attribute(null, "DirDoc", "F")
                serializer.attribute(null, "DirMer", "F")
                serializer.attribute(null, "Estado", "N")
                serializer.endTag("", "record")
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


    private fun enviarContactos(lTelefonos: List<ContactosCltesEnt>) {
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
        val cargasDao: CargasDao? = MyDatabase.getInstance(fContext)?.cargasDao()

        val lCargas: MutableList<CargasEnt> = if (queNumExportacion == 0) {
            cargasDao?.abrirParaEnviar() ?: emptyList<CargasEnt>().toMutableList()
        } else {
            cargasDao?.abrirExportacion(queNumExportacion) ?: emptyList<CargasEnt>().toMutableList()
        }

        if (lCargas.isNotEmpty()) {
            val msg = Message()
            msg.obj = "Preparando cargas"
            puente.sendMessage(msg)

            cargasAXML(lCargas)


            val lLineas: MutableList<CargasLineasEnt> = if (queNumExportacion == 0) {
                cargasLineasDao?.abrirParaEnviar() ?: emptyList<CargasLineasEnt>().toMutableList()
            } else {
                cargasLineasDao?.abrirExportacion(queNumExportacion) ?: emptyList<CargasLineasEnt>().toMutableList()
            }

            cargasLineasAXML(lLineas)

            // Marcamos las cargas como exportadas.
            if (queNumExportacion == 0) {
                cargasDao?.marcarComoExportadas(iSigExportacion)
            }

            return true

        } else {
            return false
        }
    }

    private fun enviarCabeceras(queNumExportacion: Int, iSigExportacion: Int): Boolean {
        val fConfiguracion = Comunicador.fConfiguracion

        val dtosLineasDao: DtosLineasDao? = MyDatabase.getInstance(fContext)?.dtosLineasDao()

        val lCabeceras: MutableList<CabecerasEnt> = if (queNumExportacion == 0) {
            // Si tenemos rutero_reparto mandaremos también las cabeceras de los documentos
            // que estén firmados o tengan alguna incidencia (sólo las cabeceras).
            if (fConfiguracion.hayReparto()) {
                cabecerasDao?.abrirParaEnvReparto() ?: emptyList<CabecerasEnt>().toMutableList()
            } else {
                cabecerasDao?.abrirParaEnviar() ?: emptyList<CabecerasEnt>().toMutableList()
            }
        } else {
            cabecerasDao?.abrirParaEnvExp(queNumExportacion) ?: emptyList<CabecerasEnt>().toMutableList()
        }

        if (lCabeceras.isNotEmpty()) {
            val msg = Message()
            msg.obj = "Preparando documentos"
            puente.sendMessage(msg)

            cabecerasAXML(lCabeceras)

            // No enviaremos las líneas de documentos importados que han sido firmados o marcados con alguna incidencia
            // (desde el módulo de repartos).
            val lLineas: MutableList<LineasEnt> = if (queNumExportacion == 0) {
                lineasDao?.abrirParaEnviar() ?: emptyList<LineasEnt>().toMutableList()
            } else {
                lineasDao?.abrirParaEnvExp(queNumExportacion) ?: emptyList<LineasEnt>().toMutableList()
            }

            lineasAXML(lLineas)

            // Exportamos los descuentos en cascada.
            val lDtos: MutableList<DtosLineasEnt> = if (queNumExportacion == 0) {
                dtosLineasDao?.abrirParaEnviar() ?: emptyList<DtosLineasEnt>().toMutableList()
            } else {
                dtosLineasDao?.abrirParaEnvExp(queNumExportacion) ?: emptyList<DtosLineasEnt>().toMutableList()
            }

            lineasDtoAXML(lDtos)

            // Si hemos ido guardando las imágenes con las firmas digitales, las enviamos.
            if (fDesdeServicio) {
                if (fConfiguracion.activarFirmaDigital() || fConfiguracion.hayReparto()) {
                    enviarFirmas()
                }
            }

            // Marcamos las cabeceras como exportadas.
            if (queNumExportacion == 0) {
                if (fConfiguracion.hayReparto())
                    cabecerasDao?.marcarComoExpReparto(iSigExportacion)
                else
                    cabecerasDao?.marcarComoExportadas(iSigExportacion)
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
            if (fUsarMultisistema) "/storage/sdcard0/alba/firmas/$queBDRoom"
            else "/storage/sdcard0/alba/firmas/"
        } else {
            if (fUsarMultisistema) "$directorioLocal/firmas/$queBDRoom"
            else "$directorioLocal/firmas/"
        }
        return result
    }


    private fun cargasAXML(lCargas: MutableList<CargasEnt>) {
        try {
            val outputFile = File(rutaLocalEnvio, "Cargas.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            for (cargaEnt in lCargas) {
                serializer.startTag("", "record")
                serializer.attribute(null, "CargaId", cargaEnt.cargaId.toString())
                serializer.attribute(null, "Empresa", cargaEnt.empresa.toString())
                serializer.attribute(null, "Fecha", cargaEnt.fecha)
                serializer.attribute(null, "Hora", cargaEnt.hora)
                serializer.attribute(null, "EsFinDeDia", cargaEnt.esFinDeDia)
                serializer.endTag("", "record")
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

    private fun cargasLineasAXML(lLineas: MutableList<CargasLineasEnt>) {
        try {
            val outputFile = File(rutaLocalEnvio, "CargasLineas.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            for (lineaEnt in lLineas) {
                serializer.startTag("", "record")
                serializer.attribute(null, "CargaId", lineaEnt.cargaId.toString())
                serializer.attribute(null, "Articulo", lineaEnt.articuloId.toString())
                serializer.attribute(null, "Lote", lineaEnt.lote)
                serializer.attribute(null, "Cajas", lineaEnt.cajas.replace('.', ','))
                serializer.attribute(null, "Cantidad", lineaEnt.cantidad.replace('.', ','))
                serializer.endTag("", "record")
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


    private fun cabecerasAXML(lCabeceras: MutableList<CabecerasEnt>) {
        try {
            val outputFile = File(rutaLocalEnvio, "Cabeceras.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            for (cabeceraEnt in lCabeceras) {
                // Añadimos el id del documento al array aCabeceras
                aCabeceras.add(cabeceraEnt.cabeceraId)

                serializer.startTag("", "record")
                serializer.attribute(null, "IdDoc", cabeceraEnt.cabeceraId.toString())
                serializer.attribute(null, "TipoDoc", cabeceraEnt.tipoDoc.toString())
                serializer.attribute(null, "TipoPedido", cabeceraEnt.tipoPedido.toString())
                serializer.attribute(null, "Alm", cabeceraEnt.almacen.toString())
                serializer.attribute(null, "Serie", cabeceraEnt.serie)
                serializer.attribute(null, "Numero", cabeceraEnt.numero.toString())
                serializer.attribute(null, "Ejer", cabeceraEnt.ejercicio.toString())
                serializer.attribute(null, "Empresa", cabeceraEnt.empresa.toString())
                serializer.attribute(null, "Fecha", cabeceraEnt.fecha)
                serializer.attribute(null, "Hora", cabeceraEnt.hora)
                serializer.attribute(null, "FechaEntrega", cabeceraEnt.fechaEntrega)
                serializer.attribute(null, "Cliente", cabeceraEnt.clienteId.toString())
                serializer.attribute(null, "Ruta", cabeceraEnt.ruta.toString())
                serializer.attribute(null, "AplIva", cabeceraEnt.aplicarIva)
                serializer.attribute(null, "AplRec", cabeceraEnt.aplicarRe)
                serializer.attribute(null, "Bruto", cabeceraEnt.bruto.replace('.', ','))
                serializer.attribute(null, "Dto", cabeceraEnt.dto.replace('.', ','))
                serializer.attribute(null, "Dto2", cabeceraEnt.dto2.replace('.', ','))
                serializer.attribute(null, "Dto3", cabeceraEnt.dto3.replace('.', ','))
                serializer.attribute(null, "Dto4", cabeceraEnt.dto4.replace('.', ','))
                serializer.attribute(null, "Base", cabeceraEnt.base.replace('.', ','))
                serializer.attribute(null, "Iva", cabeceraEnt.iva.replace('.', ','))
                serializer.attribute(null, "Recargo", cabeceraEnt.recargo.replace('.', ','))
                serializer.attribute(null, "Total", cabeceraEnt.total.replace('.', ','))
                serializer.attribute(null, "Flag", cabeceraEnt.flag.toString())
                serializer.attribute(null, "Obs1", cabeceraEnt.observ1)
                serializer.attribute(null, "Obs2", cabeceraEnt.observ2)
                serializer.attribute(null, "FPago", cabeceraEnt.fPago)
                serializer.attribute(null, "TipoIncidencia", cabeceraEnt.tipoIncidencia.toString())
                serializer.attribute(null, "TextoIncidencia", cabeceraEnt.textoIncidencia)
                serializer.attribute(null, "Entregado", cabeceraEnt.firmado)
                serializer.attribute(null, "FechaFirma", cabeceraEnt.fechaFirma)
                serializer.attribute(null, "HoraFirma", cabeceraEnt.horaFirma)

                val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fFecha = df.format(System.currentTimeMillis())
                serializer.attribute(null, "FechaEnvio", fFecha)
                val dfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
                val fHora = dfHora.format(System.currentTimeMillis())
                serializer.attribute(null, "HoraEnvio", fHora)

                if (cabeceraEnt.estadoInicial != "")
                    if (cabeceraEnt.estadoInicial == "0")
                        serializer.attribute(null, "Estado", "E")
                    else
                        serializer.attribute(null, "Estado", "N")
                else
                    serializer.attribute(null, "Estado", "N")

                // Si damos de alta una dirección en la tablet, la gestión la asigna siempre al almacén 0, por eso indicamos
                // aquí siempre el almacén 0.
                serializer.attribute(null, "AlmDireccionClte", "000")
                serializer.attribute(null, "OrdenDireccionClte", cabeceraEnt.ordenDireccion)
                serializer.endTag("", "record")
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


    private fun lineasAXML(lLineas: MutableList<LineasEnt>) {

        try {
            val outputFile = File(rutaLocalEnvio, "Lineas.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            for (lineaEnt in lLineas) {
                serializer.startTag("", "record")
                // Enviamos el id de la linea para poder enlazar con los descuentos en cascada.
                serializer.attribute(null, "Id", lineaEnt.lineaId.toString())
                serializer.attribute(null, "CabeceraId", lineaEnt.cabeceraId.toString())
                serializer.attribute(null, "Articulo", lineaEnt.articuloId.toString())
                serializer.attribute(null, "Codigo", lineaEnt.codArticulo)
                serializer.attribute(null, "Descr", lineaEnt.descripcion)
                serializer.attribute(null, "Tarifa", lineaEnt.tarifaId.toString())
                serializer.attribute(null, "Precio", lineaEnt.precio.replace('.', ','))
                serializer.attribute(null, "TipoIva", lineaEnt.codigoIva.toString())
                serializer.attribute(null, "Cajas", lineaEnt.cajas.replace('.', ','))
                serializer.attribute(null, "Cantidad", lineaEnt.cantidad.replace('.', ','))
                serializer.attribute(null, "Piezas", lineaEnt.piezas.replace('.', ','))
                serializer.attribute(null, "Dto", lineaEnt.dto.replace('.', ','))
                serializer.attribute(null, "DtoI", lineaEnt.dtoImpte.replace('.', ','))
                serializer.attribute(null, "Lote", lineaEnt.lote)
                serializer.attribute(null, "Flag", lineaEnt.flag.toString())
                serializer.attribute(null, "Flag3", lineaEnt.flag3.toString())
                serializer.attribute(null, "Flag5", lineaEnt.flag5.toString())
                serializer.attribute(null, "Tasa1", lineaEnt.tasa1.replace('.', ','))
                serializer.attribute(null, "Tasa2", lineaEnt.tasa2.replace('.', ','))
                serializer.attribute(null, "Formato", lineaEnt.formatoId.toString())
                serializer.attribute(null, "Inc", lineaEnt.tipoIncId.toString())
                serializer.attribute(null, "TextoLinea", lineaEnt.textoLinea)
                serializer.attribute(null, "AlmacenPedido", lineaEnt.almacenPedido)
                serializer.attribute(null, "IdOferta", lineaEnt.ofertaId.toString())
                serializer.attribute(null, "DtoOftVol", lineaEnt.dtoOftVol)

                serializer.endTag("", "record")
            }
            serializer.endTag("", "consulta")

            serializer.endDocument()
            serializer.flush()
            fout.close()
            fTamLineas = outputFile.length()

        } catch (e: FileNotFoundException) {
            mostrarExcepcion(e)
        } catch (e: Exception) {
            mostrarExcepcion(e)
        }
    }


    private fun lineasDtoAXML(lDtos: MutableList<DtosLineasEnt>) {

        try {
            val outputFile = File(rutaLocalEnvio, "DesctosLineas.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag("", "consulta")
            for (dtoEnt in lDtos) {
                serializer.startTag("", "record")
                serializer.attribute(null, "Linea", dtoEnt.lineaId.toString())
                serializer.attribute(null, "Orden", dtoEnt.orden.toString())
                serializer.attribute(null, "Descuento", dtoEnt.descuento.replace('.', ','))
                serializer.attribute(null, "Importe", dtoEnt.importe.replace('.', ','))
                serializer.attribute(null, "Cantidad1", dtoEnt.cantidad1.replace('.', ','))
                serializer.attribute(null, "Cantidad2", dtoEnt.cantidad2.replace('.', ','))
                serializer.endTag("", "record")
            }
            serializer.endTag("", "consulta")

            serializer.endDocument()
            serializer.flush()
            fout.close()

        } catch (e: FileNotFoundException) {
            mostrarExcepcion(e)
        } catch (e: Exception) {
            mostrarExcepcion(e)
        }
    }


    private fun enviarCobros(queNumExportacion: Int, iSigExportacion: Int): Boolean {
        val cobrosDao: CobrosDao? = MyDatabase.getInstance(fContext)?.cobrosDao()
        //val numCobros: Int = if (queNumExportacion == 0) cobrosDao?.hayCobrosParaEnviar() ?: 0
        //    else cobrosDao?.hayCobrosEnExport(queNumExportacion) ?: 0

        val lCobros: List<CobrosEnt> = if (queNumExportacion == 0) {
            cobrosDao?.abrirParaExportar() ?: emptyList<CobrosEnt>().toMutableList()
        } else {
            cobrosDao?.abrirExportacion(queNumExportacion) ?: emptyList<CobrosEnt>().toMutableList()
        }

        if (lCobros.isNotEmpty()) {
            val msg = Message()
            msg.obj = "Preparando cobros"
            puente.sendMessage(msg)

            cobrosAXML(lCobros)

            // Marcamos los cobros como exportados.
            if (queNumExportacion == 0) {
                cobrosDao?.marcarComoExportados(iSigExportacion)
            }

            return true

        } else {
            return false
        }
    }

    private fun cobrosAXML(lCobros: List<CobrosEnt>) {
        try {
            val outputFile = File(rutaLocalEnvio, "Cobros.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            serializer.startTag("", "consulta")

            for (cobro in lCobros) {
                serializer.startTag("", "record")
                serializer.attribute(null, "Apunte", cobro.cobroId.toString())
                serializer.attribute(null, "Cliente", cobro.clienteId.toString())
                if (cobro.tipoDoc == 33.toShort())
                    serializer.attribute(null, "TipoDoc", 0.toString())
                else
                    serializer.attribute(null, "TipoDoc", cobro.tipoDoc.toString())
                serializer.attribute(null, "Alm", cobro.almacen.toString())
                serializer.attribute(null, "Serie", cobro.serie)
                serializer.attribute(null, "Numero", cobro.numero.toString())
                serializer.attribute(null, "Ejer", cobro.ejercicio.toString())
                serializer.attribute(null, "Empresa", cobro.empresa.toString())
                serializer.attribute(null, "FechaCbro", cobro.fechaCobro)
                serializer.attribute(null, "Cobro", cobro.cobro.replace('.', ','))
                serializer.attribute(null, "FPago", cobro.fPago)
                serializer.attribute(null, "Divisa", cobro.divisa)
                serializer.attribute(null, "Anotacion", cobro.anotacion)
                serializer.attribute(null, "Codigo", cobro.codigo)
                serializer.attribute(null, "Estado", "N")
                serializer.attribute(null, "VAlmacen", cobro.vAlmacen)
                serializer.attribute(null, "VPuesto", cobro.vPuesto)
                serializer.attribute(null, "VApunte", cobro.vApunte)
                serializer.attribute(null, "VEjer", cobro.vEjercicio)
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

        val lPendiente = if (queNumExportacion == 0) {
            pendienteDao?.abrirParaEnviar() ?: emptyList<PendienteEnt>().toMutableList()
        } else {
            pendienteDao?.abrirPorNumExport(queNumExportacion) ?: emptyList<PendienteEnt>().toMutableList()
        }

        if (lPendiente.isNotEmpty()) {
            val msg = Message()
            msg.obj = "Preparando pendiente"
            puente.sendMessage(msg)

            pendienteAXML(lPendiente)

            // Marcamos los pendiente como no enviar.
            if (queNumExportacion == 0) {
                pendienteDao?.marcarNoEnviar(iSigExportacion)
            }

            return true

        } else {
            return false
        }
    }


    private fun pendienteAXML(lPendiente: List<PendienteEnt>) {
        try {
            val outputFile = File(rutaLocalEnvio, "Pendiente.xml")
            val fout = FileOutputStream(outputFile, false)

            val serializer = Xml.newSerializer()
            serializer.setOutput(fout, "UTF-8")
            serializer.startDocument(null, true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            serializer.startTag("", "consulta")

            for (pendiente in lPendiente) {
                serializer.startTag("", "record")
                serializer.attribute(null, "Cliente", pendiente.clienteId.toString())
                serializer.attribute(null, "Ejer", pendiente.ejercicio.toString())
                serializer.attribute(null, "Empresa", pendiente.empresa.toString())
                serializer.attribute(null, "Alm", pendiente.almacen.toString())
                serializer.attribute(null, "TipoDoc", pendiente.tipoDoc.toString())
                serializer.attribute(null, "FPago", pendiente.fPago)
                serializer.attribute(null, "FechaDoc", pendiente.fechaDoc)
                serializer.attribute(null, "Serie", pendiente.serie)
                serializer.attribute(null, "Numero", pendiente.numero.toString())
                serializer.attribute(null, "Importe", pendiente.importe.replace('.', ','))
                serializer.attribute(null, "Cobrado", pendiente.cobrado.replace('.', ','))
                serializer.attribute(null, "FechaVto", pendiente.fechaVto)
                serializer.attribute(null, "Estado", pendiente.estado)
                serializer.attribute(null, "Enviar", pendiente.enviar)
                serializer.attribute(null, "CAlmacen", pendiente.cAlmacen)
                serializer.attribute(null, "CPuesto", pendiente.cPuesto)
                serializer.attribute(null, "CApunte", pendiente.cApunte)
                serializer.attribute(null, "CEjer", pendiente.cEjercicio)
                serializer.attribute(null, "Flag", pendiente.flag.toString())
                serializer.attribute(null, "Anotacion", pendiente.anotacion)

                serializer.endTag("", "record")
            }
            serializer.endTag("", "consulta")

            serializer.endDocument()
            serializer.flush()
            fout.close()
            fTamPdte = outputFile.length()

        } catch (e: FileNotFoundException) {
            mostrarExcepcion(e)
        } catch (e: Exception) {
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