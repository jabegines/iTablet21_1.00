package es.albainformatica.albamobileandroid.impresion_informes

import android.R
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import es.albainformatica.albamobileandroid.database.MyDatabase.Companion.getInstance
import android.content.SharedPreferences
import com.lowagie.text.pdf.PdfContentByte
import es.albainformatica.albamobileandroid.ventas.ListaBasesDoc
import es.albainformatica.albamobileandroid.maestros.ClientesClase
import es.albainformatica.albamobileandroid.cobros.PendienteClase
import es.albainformatica.albamobileandroid.cobros.FormasPagoClase
import androidx.preference.PreferenceManager
import com.lowagie.text.pdf.PdfWriter
import android.widget.Toast
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.ColumnText
import android.widget.ArrayAdapter
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import com.lowagie.text.pdf.PdfPageEventHelper
import android.os.StrictMode.VmPolicy
import android.os.StrictMode
import android.widget.ListView
import com.lowagie.text.*
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.CabDiferidasDao
import es.albainformatica.albamobileandroid.dao.LineasDifDao
import es.albainformatica.albamobileandroid.entity.CabDiferidasEnt
import harmony.java.awt.Color
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*

class DocDiferidaPDF(private val fContexto: Context, idDocumento: Int) {
    private val cabDifDao: CabDiferidasDao? = getInstance(fContexto)?.cabDiferidasDao()
    private val linDifDao: LineasDifDao? = getInstance(fContexto)?.lineasDifDao()
    private val fConfiguracion: Configuracion
    private lateinit var fBases: ListaBasesDoc
    private lateinit var prefs: SharedPreferences
    private var fClientes: ClientesClase
    private val fPendiente: PendienteClase
    private val fFormasPago: FormasPagoClase

    private val cabDifEnt = cabDifDao?.getDatosDocumento(idDocumento) ?: CabDiferidasEnt()
    private val lLineas = linDifDao?.getLineasDoc(idDocumento) ?: emptyList<DatosLinDocDif>().toMutableList()

    private lateinit var canvas: PdfContentByte
    private lateinit var documPDF: Document

    var nombrePDF: String = "" // Me servirá para el envio por email.

    private var fExentoIva: Boolean = false
    private lateinit var slEmails: MutableList<String>
    var y: Short = 0
    private var maxLineas: Short = 30
    private var fFtoCant: String = ""
    private var fFtoPrBase: String = ""
    private var fFtoImpBase: String = ""
    private var fFtoImpII: String = ""
    private var fSerie: String = ""
    private var fNumero: String = ""
    private var fEmpresaActual: Short = 0

    private val fntHelv8: Font = Font(Font.HELVETICA, 8f, Font.NORMAL, Color.BLACK)
    private val fntHelvNegra8: Font = Font(Font.HELVETICA, 8f, Font.BOLD, Color.BLACK)
    private val grisClaro = Color(230, 230, 230)


    init {
        // Esto es para evitar el error FileUriExposedException
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        fConfiguracion = Comunicador.fConfiguracion
        fClientes = ClientesClase(fContexto)
        fFormasPago = FormasPagoClase(fContexto)
        fPendiente = PendienteClase(fContexto)

        inicializarControles(fContexto)
    }



    // Agrega las lineas en blanco especificadas a un parrafo especificado
    private fun agregarLineasEnBlanco(parrafo: Paragraph, nLineas: Int) {
        for (i in 0 until nLineas) parrafo.add(Paragraph(" "))
    }


    @SuppressLint("Range")
    private fun inicializarControles(contexto: Context) {
        fFtoCant = fConfiguracion.formatoDecCantidad()
        fFtoPrBase = fConfiguracion.formatoDecPrecioBase()
        fFtoImpBase = fConfiguracion.formatoDecImptesBase()
        fFtoImpII = fConfiguracion.formatoDecImptesIva()
        fBases = ListaBasesDoc(fContexto)

        // Leemos las preferencias de la aplicación;
        prefs = PreferenceManager.getDefaultSharedPreferences(contexto)

        if (cabDifEnt.cabDiferidaId > 0) {
            fSerie = cabDifEnt.serie
            fNumero = cabDifEnt.numero.toString()
            fClientes.abrirUnCliente(cabDifEnt.clienteId)
            fExentoIva = fClientes.fAplIva
            fEmpresaActual = cabDifEnt.empresa
        }
    }

    fun crearPDF() {
        // Creamos el documento.
        documPDF = Document(PageSize.A4, 40F, 40F, 40F, 40F)
        try {
            // Creamos el fichero y como nombre llevará la serie/número del documento.
            val f = crearFichero()

            // Creamos el flujo de datos de salida para el fichero donde guardaremos el pdf.
            val ficheroPdf = FileOutputStream(f.absolutePath)
            // Asociamos el flujo que acabamos de crear al documento.
            val writer = PdfWriter.getInstance(documPDF, ficheroPdf)
            val event = TableHeader()
            writer.pageEvent = event

            // Abrimos el documento.
            documPDF.open()
            event.setConfig(cabDifEnt, fConfiguracion, fClientes, fContexto)
            // Obtenemos el contenido del stream.
            canvas = writer.directContent
            event.pdfCabecera(writer)
            pdfLineas()
            pdfPie()
        } catch (e: Exception) {
            Toast.makeText(null, e.message, Toast.LENGTH_LONG).show()
        } finally {
            // Cerramos el documento.
            documPDF.close()
        }
    }

    private fun crearFichero(): File {
        var rutaPdfs = prefs.getString("rutacomunicacion", "")
        rutaPdfs =
            if (rutaPdfs != null && rutaPdfs == "") "/sdcard/alba/pdfs/" else "$rutaPdfs/pdfs/"
        val carpetaPdfs = File(rutaPdfs)
        // Nos aseguramos de que la carpeta existe y, si no, la creamos.
        if (!carpetaPdfs.exists()) carpetaPdfs.mkdirs()
        val fichero = File(carpetaPdfs, "FAC$fSerie-$fNumero.pdf")
        // Damos valor a nombrePDF porque nos servirá para el envío por email.
        nombrePDF = rutaPdfs + "FAC" + fSerie + "-" + fNumero + ".pdf"
        return fichero
    }

    @SuppressLint("Range")
    private fun pdfLineas() {
        var sCodigo: String?
        var sDescr: String?
        var sCajas: String
        var sCant: String
        var sPrecio: String
        var sDto: String
        var sImpte: String
        var fHayDtoLinea = false
        var x: Byte = 1
        val numLineas = lLineas.count()
        val anchosFilas = floatArrayOf(0.5f, 2f, 0.3f, 0.5f, 0.5f, 0.3f, 0.5f)
        val tabla = PdfPTable(anchosFilas)
        var cell: PdfPCell
        // Porcentaje que ocupa a lo ancho de la pagina del PDF
        tabla.widthPercentage = 100f
        // Para que repita el encabezado de la tabla si hay más de una página.
        tabla.headerRows = 1
        val parrafo = Paragraph()
        // Añadimos líneas en blanco para situar la tabla.
        agregarLineasEnBlanco(parrafo, 9)
        try {
            for (linea in lLineas) {
                sDto = linea.dto.replace(',', '.')
                val dDto = sDto.toDouble()
                if (dDto != 0.0) fHayDtoLinea = true
            }
            cabeceraLineas(tabla, fHayDtoLinea)

            // Iterar mientras haya una fila siguiente
            for (linea in lLineas) {

                // Agregar 9 celdas
                sCodigo = linea.codigo
                sDescr = linea.descripcion
                sCajas = linea.cajas.replace(',', '.')
                sCant = linea.cantidad.replace(',', '.')
                sPrecio = linea.precio.replace(',', '.')
                sDto = linea.dto.replace(',', '.')
                sImpte = linea.importe.replace(',', '.')
                val dCajas = sCajas.toDouble()
                sCajas = if (dCajas != 0.0) String.format(fFtoCant, dCajas) else ""
                val dCant = sCant.toDouble()
                sCant = String.format(fFtoCant, dCant)
                val dPrecio = sPrecio.toDouble()
                sPrecio = String.format(fFtoPrBase, dPrecio)
                val dDto = sDto.toDouble()
                sDto = if (dDto != 0.0) String.format(Locale.getDefault(), "%.2f", dDto) else ""
                val dImpte = sImpte.toDouble()
                sImpte = String.format(fFtoImpBase, dImpte)
                cell = PdfPCell(Paragraph(sCodigo, fntHelv8))
                cell.border = 0
                tabla.addCell(cell)
                cell = PdfPCell(Paragraph(sDescr, fntHelv8))
                cell.border = 0
                tabla.addCell(cell)
                cell = PdfPCell(Paragraph(sCajas, fntHelv8))
                cell.horizontalAlignment = Element.ALIGN_RIGHT
                cell.border = 0
                tabla.addCell(cell)
                cell = PdfPCell(Paragraph(sCant, fntHelv8))
                cell.horizontalAlignment = Element.ALIGN_RIGHT
                cell.border = 0
                tabla.addCell(cell)
                cell = PdfPCell(Paragraph(sPrecio, fntHelv8))
                cell.horizontalAlignment = Element.ALIGN_RIGHT
                cell.border = 0
                tabla.addCell(cell)
                cell = PdfPCell(Paragraph(sDto, fntHelv8))
                cell.horizontalAlignment = Element.ALIGN_RIGHT
                cell.border = 0
                tabla.addCell(cell)
                cell = PdfPCell(Paragraph(sImpte, fntHelv8))
                cell.horizontalAlignment = Element.ALIGN_RIGHT
                cell.border = 0
                tabla.addCell(cell)
                x++
                if (x > maxLineas) {
                    // Para no saturar la memoria del dispositivo, vamos añadiendo la
                    // tabla al párrafo (y éste al documento).
                    parrafo.add(tabla)
                    documPDF.add(parrafo)
                    parrafo.clear()
                    tabla.deleteBodyRows()
                    tabla.isSkipFirstHeader = true
                    if (x < numLineas + 1) {
                        documPDF.newPage()
                        // Volvemos a añadir líneas en blanco para situar la tabla.
                        agregarLineasEnBlanco(parrafo, 9)
                        cabeceraLineas(tabla, fHayDtoLinea)
                    }
                    x = 1
                }
            }
            // Agregar la tabla con los datos al parrafo.
            parrafo.add(tabla)
            documPDF.add(parrafo)
        } catch (e: Exception) {
            Toast.makeText(null, e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun cabeceraLineas(tabla: PdfPTable, fHayDtoLinea: Boolean) {
        val rotulosColumnas = arrayOfNulls<String>(7)

        rotulosColumnas[0] = "Código"
        rotulosColumnas[1] = "Descripción"
        rotulosColumnas[2] = "Cajas"
        rotulosColumnas[3] = "Cantidad"
        rotulosColumnas[4] = "Precio"
        if (fHayDtoLinea) rotulosColumnas[5] = "Dto" else rotulosColumnas[5] = " "
        rotulosColumnas[6] = "Importe"

        // Mostrar los rotulos de las columnas
        for (rotulosColumna in rotulosColumnas) {
            val cell = PdfPCell(Paragraph(rotulosColumna, fntHelvNegra8))
            // Esto me sirve para el alineamiento vertical.
            cell.minimumHeight = 15f
            cell.horizontalAlignment = Element.ALIGN_CENTER
            cell.backgroundColor = grisClaro
            tabla.addCell(cell)
        }
    }

    private fun pdfPie() {
        val anchosFilas = floatArrayOf(0.5f, 2f, 0.3f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
        val tabla = PdfPTable(anchosFilas)
        tabla.widthPercentage = 100f
        pdfBases()
        pdfFPago()
        pdfLineasPie()
    }

    private fun pdfBases() {
        y = 150
        var sBruto: String
        var sImpDto: String
        var sBase: String
        var sPorcIva: String
        var sImpIva: String
        var sPorcRe: String
        var sImpRe: String
        val lBruto = 9
        val lImpDto = 8
        val lBase = 9
        val lImpIva = 9
        val lImpRe = 8
        val lTotal = 9
        recalcularBases()
        for (x in fBases.fLista) {
            if (x.fBaseImponible != 0.0) {
                sBruto = String.format(fFtoImpBase, x.fImpteBruto)
                val dImpDto = x.fImpteBruto - x.fBaseImponible
                sImpDto = String.format(fFtoImpBase, dImpDto, false)
                sBase = String.format(fFtoImpBase, x.fBaseImponible)
                sPorcIva = String.format(Locale.getDefault(), "%.2f", x.fPorcIva)
                sImpIva = String.format(fFtoImpBase, x.fImporteIva)
                sPorcRe = String.format(fFtoImpBase, x.fPorcRe)
                sImpRe = String.format(fFtoImpBase, x.fImporteRe)
                var c = Chunk(ajustarCadena(sBruto, lBruto, false))
                mostrarChunk(c, 80f, y.toFloat(), Element.ALIGN_RIGHT)
                c = Chunk(ajustarCadena(sImpDto, lImpDto, false))
                mostrarChunk(c, 150f, y.toFloat(), Element.ALIGN_RIGHT)
                c = Chunk(ajustarCadena(sBase, lBase, false))
                mostrarChunk(c, 220f, y.toFloat(), Element.ALIGN_RIGHT)
                c = Chunk(ajustarCadena(sPorcIva, 5, false))
                mostrarChunk(c, 280f, y.toFloat(), Element.ALIGN_RIGHT)
                c = Chunk(ajustarCadena(sImpIva, lImpIva, false))
                mostrarChunk(c, 350f, y.toFloat(), Element.ALIGN_RIGHT)
                if (x.fImporteRe != 0.0) {
                    c = Chunk(ajustarCadena(sPorcRe, 5, false))
                    mostrarChunk(c, 410f, y.toFloat(), Element.ALIGN_RIGHT)
                    c = Chunk(ajustarCadena(sImpRe, lImpRe, false))
                    mostrarChunk(c, 480f, y.toFloat(), Element.ALIGN_RIGHT)
                }
                y = (y - 10).toShort()
            }
        }
        val sTotal: String = String.format(fFtoImpII, fBases.totalConImptos)
        val c = Chunk(ajustarCadena(sTotal, lTotal, false))
        mostrarChunk(c, 550f, (y + 10).toFloat(), Element.ALIGN_RIGHT)
    }

    @SuppressLint("Range")
    private fun pdfFPago() {
        y = 110
        val sImpte: String
        val sCobrado: String
        val sPdte: String
        var lineaSimple = ""
        val lImptes = 9
        if (fPendiente.abrirFraDiferida(cabDifEnt.serie, cabDifEnt.numero, cabDifEnt.ejercicio)) {
            var c = Chunk("Forma de pago: " + fFormasPago.getDescrFPago(fPendiente.fPago))
            mostrarChunk(c, 40f, y.toFloat(), Element.ALIGN_LEFT)
            for (x in 0..34) {
                lineaSimple = "$lineaSimple-"
            }
            y = (y - 20).toShort()
            c = Chunk("IMPORTE")
            mostrarChunk(c, 40f, y.toFloat(), Element.ALIGN_LEFT)
            c = Chunk("ENTREGADO")
            mostrarChunk(c, 100f, y.toFloat(), Element.ALIGN_LEFT)
            c = Chunk("PENDIENTE")
            mostrarChunk(c, 160f, y.toFloat(), Element.ALIGN_LEFT)
            val dImporte = fPendiente.importe.toDouble()
            sImpte = String.format(fFtoImpII, dImporte)
            val dCobrado = fPendiente.cobrado.toDouble()
            sCobrado = String.format(fFtoImpII, dCobrado)
            val dPdte = dImporte - dCobrado
            sPdte = String.format(fFtoImpII, dPdte)
            y = (y - 10).toShort()
            c = Chunk(ajustarCadena(sImpte, lImptes, false))
            mostrarChunk(c, 75f, y.toFloat(), Element.ALIGN_RIGHT)
            c = Chunk(ajustarCadena(sCobrado, lImptes, false))
            mostrarChunk(c, 145f, y.toFloat(), Element.ALIGN_RIGHT)
            c = Chunk(ajustarCadena(sPdte, lImptes, false))
            mostrarChunk(c, 200f, y.toFloat(), Element.ALIGN_RIGHT)
        }
    }

    @SuppressLint("Range")
    private fun pdfLineasPie() {
        y = 60
        var c: Chunk
        if (cabDifEnt.obs1 != "") {
            c = Chunk("Observaciones: " + cabDifEnt.obs1)
            mostrarChunk(c, 40f, y.toFloat(), Element.ALIGN_LEFT)
        }
        y = (y - 10).toShort()
        if (cabDifEnt.obs2 != "") {
            c = Chunk(cabDifEnt.obs2)
            mostrarChunk(c, 40f, y.toFloat(), Element.ALIGN_LEFT)
        }
        y = (y - 10).toShort()
        c = Chunk(fConfiguracion.lineaPieDoc(cabDifEnt.empresa.toShort(), "1".toShort()))
        mostrarChunk(c, 40f, y.toFloat(), Element.ALIGN_LEFT)
        y = (y - 10).toShort()
        c = Chunk(fConfiguracion.lineaPieDoc(cabDifEnt.empresa.toShort(), "2".toShort()))
        mostrarChunk(c, 40f, y.toFloat(), Element.ALIGN_LEFT)
    }

    @SuppressLint("Range")
    fun recalcularBases() {
        fBases.fLista.clear()
        //val fDecImpII = fConfiguracion.decimalesImpII()
        configurarBases()

        for (linea in lLineas) {
            val fImporte = linea.importe.replace(',', '.').toDouble()
            //var fImpteII: Double
            //if (fExentoIva) fImpteII = fImporte else {

                //val fPorcIva = linea.porcIva.replace(',', '.').toDouble()

                //fImpteII = fImporte + fImporte * fPorcIva / 100
                //fImpteII = redondear(fImpteII, fDecImpII)
            //}
            val fCodigoIva = linea.codigoIva
            //if (fBases.fIvaIncluido) fBases.calcularBase(fCodigoIva, fImpteII)
            //else fBases.calcularBase(fCodigoIva, fImporte)

            // La gestión siempre calcula las facturas diferidas desde la base imponible, nosotros hacemos igual
            fBases.calcularBase(fCodigoIva, fImporte)

            if (linea.porcDtoAlb.toDouble() != 0.0) {
                fBases.calcularDtosPie(linea.porcDtoAlb.toDouble(), 0.0, 0.0, 0.0)
            }
        }
    }

    private fun configurarBases() {
        fBases.fAplicarIva = fClientes.fAplIva
        fBases.fAplicarRecargo = fBases.fAplicarIva && fClientes.fAplRec
        //fBases.fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresaActual)
        // La gestión siempre calcula las facturas diferidas desde la base imponible, nosotros hacemos igual
        fBases.fIvaIncluido = false
        fBases.fDecImpBase = fConfiguracion.decimalesImpBase()
        fBases.fDecImpII = fConfiguracion.decimalesImpII()
    }

    private fun mostrarChunk(c: Chunk, x: Float, y: Float, alineacion: Int) {
        c.font = fntHelv8
        val phrase = Phrase(c)
        ColumnText.showTextAligned(canvas, alineacion, phrase, x, y, 0f)
    }

    fun enviarPorEmail() {
        val numEmails = dimeNumEmailsClte()
        if (numEmails > 0) {
            if (numEmails > 1) {
                seleccionarEmails()
            } else {
                emailALista()
                enviar()
            }
        }
    }

    private fun emailALista() {
        slEmails = fClientes.getEmailsClte(cabDifEnt.clienteId)
    }

    private fun dimeEmailsClte(): ArrayList<String> {
        // El tamaño de un array no puede ser modificado. Para ello, trabajo con un
        // ArrayList que luego convierto en array.
        val emailsClte: List<String> = fClientes.getEmailsClte(cabDifEnt.clienteId)

        // Creo el array y convierto el ArrayList en array.
        //val simpleArray = arrayOfNulls<String>(emailsClte.size)
        val simpleArray = arrayListOf<String>()
        simpleArray.addAll(emailsClte)
        //emailsClte.toArray<String>(simpleArray)

        return simpleArray
    }

    private fun dimeNumEmailsClte(): Short {
        val emailsClte: List<String> = fClientes.getEmailsClte(cabDifEnt.clienteId)
        return emailsClte.size.toString().toShort()
    }

    private fun seleccionarEmails() {
        // Me creo sobre la marcha un listView y como adaptador le paso un array con los emails.
        val setListAdapter = ArrayAdapter(fContexto, R.layout.simple_list_item_multiple_choice, dimeEmailsClte())
        val listView = ListView(fContexto)
        listView.itemsCanFocus = false
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        listView.adapter = setListAdapter
        val dialog: Dialog
        val builder = AlertDialog.Builder(fContexto)
        builder.setTitle("Seleccione un email")
        builder.setView(listView)
        builder.setPositiveButton("OK") { _: DialogInterface?, _: Int ->
            val cntChoice = listView.count
            slEmails = ArrayList()
            val sparseBooleanArray = listView.checkedItemPositions
            for (i in 0 until cntChoice) {
                if (sparseBooleanArray[i]) slEmails.add(listView.getItemAtPosition(i).toString())
            }
            if (slEmails.size > 0) enviar() else MsjAlerta(fContexto).alerta("No ha seleccionado ningún email")
        }
        dialog = builder.create()
        dialog.show()
    }

    @SuppressLint("Range")
    private fun enviar() {
        //val to = arrayOfNulls<String>(slEmails.size)
        val to = arrayListOf<String>()
        to.addAll(slEmails)
        //slEmails.toArray<String>(to)
        val cc = arrayOf("")
        val asunto = "Envío de documento."
        val mensaje =
            ("Estimado cliente, el fichero pdf adjuntado en el mensaje corresponde al documento "
                    + cabDifEnt.serie + "/" + cabDifEnt.numero)
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.data = Uri.parse("mailto:")
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to)
        emailIntent.putExtra(Intent.EXTRA_CC, cc)
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, asunto)
        emailIntent.putExtra(Intent.EXTRA_TEXT, mensaje)
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://$nombrePDF"))
        emailIntent.type = "text/html"
        fContexto.startActivity(Intent.createChooser(emailIntent, "Email "))
    }

    private fun ajustarCadena(cCadena: String, maxLong: Int, fPorLaDerecha: Boolean): String {
        var result = cCadena
        // Si la cadena supera el máximo de caracteres, la recortamos. En cambio, si
        // no llega a esta cifra, le añadimos espacios al final.
        if (result.length > maxLong) result =
            result.substring(0, maxLong) else if (result.length < maxLong) {
            result = if (fPorLaDerecha) result + stringOfChar(
                " ",
                maxLong - result.length
            ) else stringOfChar(" ", maxLong - result.length) + result
        }
        return result
    }

    internal class TableHeader: PdfPageEventHelper() {
        /**
         * The template with the total number of pages.
         */
        private lateinit var fConfiguracion: Configuracion
        private lateinit var fClientes: ClientesClase
        private lateinit var fContexto: Context
        private lateinit var fCabDifEnt: CabDiferidasEnt

        private val fntCabecera = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22f, Font.BOLD, Color.BLACK)
        private val fntHelv8: Font = Font(Font.HELVETICA, 8f, Font.NORMAL, Color.BLACK)

        fun setConfig(queCabEnt: CabDiferidasEnt, queConfiguracion: Configuracion, queClientes: ClientesClase, queContexto: Context) {
            fConfiguracion = queConfiguracion
            fClientes = queClientes
            fCabDifEnt = queCabEnt
            fContexto = queContexto
        }

        override fun onEndPage(writer: PdfWriter, document: Document) {
            pdfCabecera(writer)
            pdfDatosClteYDoc(writer)
            pdfCabecBases(writer)
        }

        @SuppressLint("Range")
        fun pdfCabecera(writer: PdfWriter) {
            val myBD = getInstance(fContexto)
            val docsCabPiesDao = myBD?.docsCabPiesDao()
            var y: Short = 790
            val numceros: Byte = 2
            val canvas = writer.directContent

            // Añadimos la cabecera con una fuente personalizada.
            var c = Chunk(docsCabPiesDao?.cabeceraDoc(fCabDifEnt.empresa.toShort()))
            mostrarChunk(canvas, c, 40f, y.toFloat(), fntCabecera)
            y = (y - 20).toShort()
            for (i in 0..4) {
                c = Chunk(fConfiguracion.lineaCabDoc(fCabDifEnt.empresa.toShort(), ponerCeros(i.toString(), numceros).toShort()))
                mostrarChunk(canvas, c, 40f, y.toFloat(), fntHelv8)
                y = (y - 10).toShort()
            }
        }

        @SuppressLint("Range")
        private fun pdfDatosClteYDoc(writer: PdfWriter) {
            var y: Short = 710
            val canvas = writer.directContent
            var c = Chunk(ponerCeros(fClientes.fCodigo.toString(), ancho_codclte) + " " + fClientes.fNombre)
            mostrarChunk(canvas, c, 40f, y.toFloat(), fntHelv8)
            c = Chunk("Vendedor: " + fConfiguracion.vendedor())
            mostrarChunk(canvas, c, 400f, y.toFloat(), fntHelv8)
            y = (y - 10).toShort()
            c = Chunk(fClientes.fDireccion)
            mostrarChunk(canvas, c, 40f, y.toFloat(), fntHelv8)
            c = Chunk("Fecha: " + fCabDifEnt.fecha)
            mostrarChunk(canvas, c, 400f, y.toFloat(), fntHelv8)
            y = (y - 10).toShort()
            c = Chunk(fClientes.fCodPostal + " " + fClientes.fPoblacion)
            mostrarChunk(canvas, c, 40f, y.toFloat(), fntHelv8)
            c = Chunk("Doc: " + tipoDocAsString("1".toByte().toShort()))
            mostrarChunk(canvas, c, 400f, y.toFloat(), fntHelv8)
            y = (y - 10).toShort()
            c = Chunk(fClientes.fProvincia)
            mostrarChunk(canvas, c, 40f, y.toFloat(), fntHelv8)
            c = Chunk("Num.: " + fCabDifEnt.serie + "/" + fCabDifEnt.numero)
            mostrarChunk(canvas, c, 400f, y.toFloat(), fntHelv8)
            y = (y - 10).toShort()
            c = Chunk("C.I.F.: " + fClientes.fCif)
            mostrarChunk(canvas, c, 40f, y.toFloat(), fntHelv8)
        }

        private fun pdfCabecBases(writer: PdfWriter) {
            var y: Short = 170
            val canvas = writer.directContent
            var c = Chunk("Venta")
            mostrarChunk(canvas, c, 55f, y.toFloat(), fntHelv8)
            c = Chunk("Dto.")
            mostrarChunk(canvas, c, 130f, y.toFloat(), fntHelv8)
            c = Chunk("Neto")
            mostrarChunk(canvas, c, 200f, y.toFloat(), fntHelv8)
            c = Chunk("% IVA")
            mostrarChunk(canvas, c, 255f, y.toFloat(), fntHelv8)
            c = Chunk("IVA")
            mostrarChunk(canvas, c, 330f, y.toFloat(), fntHelv8)
            c = Chunk("% Rec.")
            mostrarChunk(canvas, c, 380f, y.toFloat(), fntHelv8)
            c = Chunk("Rec.")
            mostrarChunk(canvas, c, 460f, y.toFloat(), fntHelv8)
            c = Chunk("TOTAL")
            mostrarChunk(canvas, c, 520f, y.toFloat(), fntHelv8)
            y = (y - 5).toShort() // Dibujamos la línea.
            canvas.setLineWidth(0.5f)
            canvas.moveTo(40f, y.toFloat())
            canvas.lineTo(550f, y.toFloat())
            canvas.stroke()
        }

        private fun mostrarChunk(
            canvas: PdfContentByte,
            c: Chunk,
            x: Float,
            y: Float,
            fuente: Font
        ) {
            c.font = fuente
            val phrase = Phrase(c)
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, phrase, x, y, 0f)
        }
    }


}