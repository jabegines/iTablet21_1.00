package es.albainformatica.albamobileandroid.impresion_informes

import android.content.Context
import android.content.SharedPreferences
import com.lowagie.text.pdf.PdfContentByte
import com.lowagie.text.pdf.PdfWriter
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.lowagie.text.pdf.PdfPageEventHelper
import com.lowagie.text.pdf.PdfTemplate
import com.lowagie.text.pdf.ColumnText
import android.preference.PreferenceManager
import com.lowagie.text.*
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.CabecerasDao
import es.albainformatica.albamobileandroid.dao.CobrosDao
import es.albainformatica.albamobileandroid.dao.LineasDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import harmony.java.awt.Color
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.ArrayList

/**
 * Created by jabegines on 15/02/2018.
 */
class ResumenPedidos(private val fContexto: Context) {
    private var prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(fContexto)
    private var fConfiguracion: Configuracion = Comunicador.fConfiguracion

    private lateinit var lCabeceras: List<DatosResPedidos>
    private lateinit var lCobros: List<DatosCobrResPedidos>
    private lateinit var lLineas: List<DatosLinResPedidos>

    private lateinit var canvas: PdfContentByte
    private lateinit var documPDF: Document
    private var nombrePDF: String = "" // Me servirá para el envio por email.

    var y: Short = 0
    private var fFtoCant: String = fConfiguracion.formatoDecCantidad()
    private var fFtoPrBase: String = fConfiguracion.formatoDecPrecioBase()
    private var fFtoImpBase: String = fConfiguracion.formatoDecImptesBase()

    private val fntHelv10: Font = Font(Font.HELVETICA, 10f, Font.NORMAL, Color.BLACK)
    private val fntHelv10Bold: Font = Font(Font.HELVETICA, 10f, Font.BOLD, Color.BLACK)
    private val fntHelv12Bold: Font = Font(Font.HELVETICA, 12f, Font.BOLD, Color.BLACK)



    fun crearResumen() {
        if (obtenerCabeceras()) {

            // Creamos el documento. Va apaisado, por eso hacemos el rotate().
            documPDF = Document(PageSize.A4.rotate(), 40F, 40F, 40F, 40F)
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
                // Obtenemos el contenido del stream.
                canvas = writer.directContent

                // Imprimimos los pedidos
                y = 560
                tituloInforme(writer)
                for (cabecera in lCabeceras) {
                    event.pdfCabecera(cabecera, writer, y)
                    obtenerLineas(cabecera)
                    y = (y - 30).toShort()
                    pdfLineas(writer)
                    y= (y - 40).toShort()
                    if (y < 30) {
                        documPDF.newPage()
                        y = 560
                        tituloInforme(writer)
                    }
                }

                // Imprimimos los cobros
                if (obtenerCobros()) {
                    y = (y - 30).toShort()
                    pdfCobros(writer)
                }
            } catch (e: Exception) {
                Toast.makeText(null, e.message, Toast.LENGTH_LONG).show()
            } finally {
                // Cerramos el documento.
                documPDF.close()
            }
        }
    }

    fun enviarPorEmail() {
        //var to = arrayOfNulls<String>(1)
        val para: MutableList<String> = ArrayList()
        para.add(fConfiguracion.emailResumPedidos())
        //para.toArray<String>(to)
        val to = arrayListOf<String>()
        to.addAll(para)


        val cc = arrayOf("")
        val asunto = "Informe de pedidos tablet " + fConfiguracion.vendedor() + " " + fConfiguracion.nombreVendedor()
        val mensaje = "Informe de pedidos de la tablet " + fConfiguracion.codTerminal() + " " +
                fConfiguracion.nombreTerminal() + " del vendedor " + fConfiguracion.vendedor() + " " +
                fConfiguracion.nombreVendedor()

        try {
            val emailIntent = Intent(Intent.ACTION_SEND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val file = File(nombrePDF)
                val fileUri = FileProvider.getUriForFile(fContexto, BuildConfig.APPLICATION_ID + ".provider", file)
                emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
            } else {
                emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://$nombrePDF"))
            }
            emailIntent.data = Uri.parse("mailto:")
            emailIntent.putExtra(Intent.EXTRA_EMAIL, to)
            emailIntent.putExtra(Intent.EXTRA_CC, cc)
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, asunto)
            emailIntent.putExtra(Intent.EXTRA_TEXT, mensaje)
            emailIntent.type = "text/html"
            fContexto.startActivity(Intent.createChooser(emailIntent, "Email "))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun crearFichero(): File {
        var rutaPdfs = prefs.getString("rutacomunicacion", "")
        rutaPdfs = if (rutaPdfs === "") "/storage/sdcard0/alba/pdfs/" else "$rutaPdfs/pdfs/"
        val carpetaPdfs = File(rutaPdfs)
        // Nos aseguramos de que la carpeta existe y, si no, la creamos.
        if (!carpetaPdfs.exists()) carpetaPdfs.mkdirs()
        val fichero = File(carpetaPdfs, "ResumenPedidos.pdf")
        // Damos valor a nombrePDF porque nos servirá para el envío por email.
        nombrePDF = rutaPdfs + "ResumenPedidos.pdf"
        return fichero
    }

    private fun pdfCobros(writer: PdfWriter) {
        val canvas = writer.directContent
        var c = Chunk("COBROS:")
        mostrarChunk(canvas, c, 10f, y.toFloat(), fntHelv12Bold, Element.ALIGN_LEFT)
        y = (y - 20).toShort()

        for (cobro in lCobros) {
            c = Chunk("Cliente: ")
            mostrarChunk(canvas, c, 10f, y.toFloat(), fntHelv10Bold, Element.ALIGN_LEFT)
            val lNombre = cobro.nombre.length
            c = if (lNombre >= 40) {
                Chunk(ponerCeros(cobro.codigo.toString(), ancho_codclte) + " " + cobro.nombre.substring(0, 40))
            } else {
                Chunk(ponerCeros(cobro.codigo.toString(), ancho_codclte) + " " + cobro.nombre.substring(0, lNombre))
            }
            mostrarChunk(canvas, c, 50f, y.toFloat(), fntHelv10, Element.ALIGN_LEFT)
            c = Chunk("Fecha: ")
            mostrarChunk(canvas, c, 300f, y.toFloat(), fntHelv10Bold, Element.ALIGN_LEFT)
            c = Chunk(cobro.fechaCobro)
            mostrarChunk(canvas, c, 340f, y.toFloat(), fntHelv10, Element.ALIGN_LEFT)
            c = Chunk("Forma pago: ")
            mostrarChunk(canvas, c, 420f, y.toFloat(), fntHelv10Bold, Element.ALIGN_LEFT)
            c = Chunk(cobro.descrFPago)
            mostrarChunk(canvas, c, 485f, y.toFloat(), fntHelv10, Element.ALIGN_LEFT)
            c = Chunk("Divisa: ")
            mostrarChunk(canvas, c, 540f, y.toFloat(), fntHelv10Bold, Element.ALIGN_LEFT)
            c = Chunk(cobro.descrDivisa)
            mostrarChunk(canvas, c, 580f, y.toFloat(), fntHelv10, Element.ALIGN_LEFT)
            c = Chunk("Importe: ")
            mostrarChunk(canvas, c, 630f, y.toFloat(), fntHelv10Bold, Element.ALIGN_LEFT)
            c = Chunk(cobro.cobro)
            mostrarChunk(canvas, c, 680f, y.toFloat(), fntHelv10, Element.ALIGN_LEFT)
            y = (y - 20).toShort()
            c = Chunk("Documento: ")
            mostrarChunk(canvas, c, 10f, y.toFloat(), fntHelv10Bold, Element.ALIGN_LEFT)
            c = Chunk(tipoDocAsString(cobro.tipoDoc)
            )
            mostrarChunk(canvas, c, 75f, y.toFloat(), fntHelv10, Element.ALIGN_LEFT)
            c = Chunk("Serie/Nº: ")
            mostrarChunk(canvas, c, 140f, y.toFloat(), fntHelv10Bold, Element.ALIGN_LEFT)
            c = Chunk(cobro.serie + "/" + cobro.numero.toString())
            mostrarChunk(canvas, c, 190f, y.toFloat(), fntHelv10, Element.ALIGN_LEFT)
            c = Chunk("F. doc.: ")
            mostrarChunk(canvas, c, 300f, y.toFloat(), fntHelv10Bold, Element.ALIGN_LEFT)
            c = Chunk(cobro.fechaDoc)
            mostrarChunk(canvas, c, 340f, y.toFloat(), fntHelv10, Element.ALIGN_LEFT)
            c = Chunk("Anotación: ")
            mostrarChunk(canvas, c, 420f, y.toFloat(), fntHelv10Bold, Element.ALIGN_LEFT)
            c = Chunk(cobro.anotacion)
            mostrarChunk(canvas, c, 480f, y.toFloat(), fntHelv10, Element.ALIGN_LEFT)
            y = (y - 30).toShort()
        }
    }

    private fun pdfLineas(writer: PdfWriter) {
        val canvas = writer.directContent
        var sCodigo: String?
        var sDescr: String?
        var sFormato: String
        var sCajas: String
        var sCant: String
        var sPiezas: String
        var sPrecio: String
        var sDto: String
        var sImpte: String

        cabeceraLineas(canvas)
        y = (y - 12).toShort()

        for (linea in lLineas) {
            sCodigo = linea.codArticulo
            sDescr = linea.descripcion
            sFormato = if (linea.formatoId > 0) linea.formatoId.toString() + " " + linea.descrFto else ""
            sCajas = linea.cajas.replace(',', '.')
            sCant = linea.cantidad.replace(',', '.')
            sPiezas = linea.piezas.replace(',', '.')
            sPrecio = linea.precio.replace(',', '.')
            sDto = linea.dto.replace(',', '.')
            sImpte = linea.importe.replace(',', '.')
            val dCajas = sCajas.toDouble()
            sCajas = if (dCajas != 0.0) String.format(fFtoCant, dCajas) else ""
            val dCant = sCant.toDouble()
            sCant = String.format(fFtoCant, dCant)
            val dPiezas = sPiezas.toDouble()
            sPiezas = String.format(fFtoCant, dPiezas)
            val dPrecio = sPrecio.toDouble()
            sPrecio = String.format(fFtoPrBase, dPrecio)
            val dDto = sDto.toDouble()
            sDto = if (dDto != 0.0) String.format("%.2f", dDto) else ""
            val dImpte = sImpte.toDouble()
            sImpte = String.format(fFtoImpBase, dImpte)
            var c = Chunk(sCodigo)
            mostrarChunk(canvas, c, 25f, y.toFloat(), fntHelv10, Element.ALIGN_LEFT)
            c = Chunk(sDescr)
            mostrarChunk(canvas, c, 150f, y.toFloat(), fntHelv10, Element.ALIGN_LEFT)
            c = Chunk(sFormato)
            mostrarChunk(canvas, c, 420f, y.toFloat(), fntHelv10, Element.ALIGN_RIGHT)
            c = Chunk(sCajas)
            mostrarChunk(canvas, c, 490f, y.toFloat(), fntHelv10, Element.ALIGN_RIGHT)
            c = Chunk(sCant)
            mostrarChunk(canvas, c, 540f, y.toFloat(), fntHelv10, Element.ALIGN_RIGHT)
            c = Chunk(sPiezas)
            mostrarChunk(canvas, c, 600f, y.toFloat(), fntHelv10, Element.ALIGN_RIGHT)
            c = Chunk(sPrecio)
            mostrarChunk(canvas, c, 650f, y.toFloat(), fntHelv10, Element.ALIGN_RIGHT)
            c = Chunk(sDto)
            mostrarChunk(canvas, c, 700f, y.toFloat(), fntHelv10, Element.ALIGN_RIGHT)
            c = Chunk(sImpte)
            mostrarChunk(canvas, c, 750f, y.toFloat(), fntHelv10, Element.ALIGN_RIGHT)
            y = (y - 10).toShort()
            if (y < 30) {
                documPDF.newPage()
                y = 560
                tituloInforme(writer)
            }
        }
    }

    private fun cabeceraLineas(canvas: PdfContentByte) {
        var c = Chunk("Codigo")
        mostrarChunk(canvas, c, 25f, y.toFloat(), fntHelv10Bold, Element.ALIGN_LEFT)
        c = Chunk("Descripcion")
        mostrarChunk(canvas, c, 150f, y.toFloat(), fntHelv10Bold, Element.ALIGN_LEFT)
        c = Chunk("Formato")
        mostrarChunk(canvas, c, 420f, y.toFloat(), fntHelv10Bold, Element.ALIGN_RIGHT)
        c = Chunk("Cajas")
        mostrarChunk(canvas, c, 490f, y.toFloat(), fntHelv10Bold, Element.ALIGN_RIGHT)
        c = Chunk("Cantidad")
        mostrarChunk(canvas, c, 540f, y.toFloat(), fntHelv10Bold, Element.ALIGN_RIGHT)
        c = Chunk("Piezas")
        mostrarChunk(canvas, c, 600f, y.toFloat(), fntHelv10Bold, Element.ALIGN_RIGHT)
        c = Chunk("Precio")
        mostrarChunk(canvas, c, 650f, y.toFloat(), fntHelv10Bold, Element.ALIGN_RIGHT)
        c = Chunk("%Dto")
        mostrarChunk(canvas, c, 700f, y.toFloat(), fntHelv10Bold, Element.ALIGN_RIGHT)
        c = Chunk("Importe")
        mostrarChunk(canvas, c, 750f, y.toFloat(), fntHelv10Bold, Element.ALIGN_RIGHT)
    }


    internal class TableHeader : PdfPageEventHelper() {
        /** The template with the total number of pages.  */
        var total: PdfTemplate? = null
        override fun onOpenDocument(writer: PdfWriter, document: Document) {
            total = writer.directContent.createTemplate(30f, 16f)
        }

        override fun onEndPage(writer: PdfWriter, document: Document) {
            //
        }

        override fun onCloseDocument(writer: PdfWriter, document: Document) {
            ColumnText.showTextAligned(
                total,
                Element.ALIGN_LEFT,
                Phrase((writer.pageNumber - 1).toString()),
                2f,
                2f,
                0f
            )
        }

        fun pdfCabecera(cabecera: DatosResPedidos, writer: PdfWriter, y: Short) {
            val fntHelv10 = Font(Font.HELVETICA, 10f, Font.NORMAL, Color.BLACK)
            val fntHelv10Bold = Font(Font.HELVETICA, 10f, Font.BOLD, Color.BLACK)

            var i = y
            val canvas = writer.directContent
            var c = Chunk("Pedido nº: ")

            mostrarChunk(canvas, c, 10f, i.toFloat(), fntHelv10Bold)
            c = Chunk(cabecera.serie + "/" + cabecera.numero.toString())
            mostrarChunk(canvas, c, 65f, i.toFloat(), fntHelv10)
            c = Chunk("Fecha: ")
            mostrarChunk(canvas, c, 100f, i.toFloat(), fntHelv10Bold)
            c = Chunk(cabecera.fecha)
            mostrarChunk(canvas, c, 135f, i.toFloat(), fntHelv10)
            c = Chunk("Cliente: ")
            mostrarChunk(canvas, c, 200f, i.toFloat(), fntHelv10Bold)
            val lNombre = cabecera.nombre.length

            c = if (lNombre >= 40) {
                Chunk(ponerCeros(cabecera.codigo.toString(), ancho_codclte) + " " + cabecera.nombre.substring(0, 40))
            } else {
                Chunk(ponerCeros(cabecera.codigo.toString(), ancho_codclte) + " " + cabecera.nombre.substring(0, lNombre))
            }
            mostrarChunk(canvas, c, 240f, i.toFloat(), fntHelv10)
            c = Chunk("Fecha entrega: ")
            mostrarChunk(canvas, c, 480f, i.toFloat(), fntHelv10Bold)
            c = Chunk(cabecera.fechaEntrega)
            mostrarChunk(canvas, c, 555f, i.toFloat(), fntHelv10)
            i = (i - 15).toShort()
            c = Chunk("Observaciones: ")
            mostrarChunk(canvas, c, 10f, i.toFloat(), fntHelv10Bold)
            c = Chunk(cabecera.observ1 + cabecera.observ2)
            mostrarChunk(canvas, c, 90f, i.toFloat(), fntHelv10)
        }


        private fun mostrarChunk(canvas: PdfContentByte, c: Chunk, x: Float, y: Float, fuente: Font) {
            c.font = fuente
            val phrase = Phrase(c)
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, phrase, x, y, 0f)
        }
    }

    private fun obtenerCabeceras(): Boolean {
        val cabecerasDao: CabecerasDao? = MyDatabase.getInstance(fContexto)?.cabecerasDao()
        val lCabeceras = cabecerasDao?.getResumenPedidos() ?: emptyList<DatosResPedidos>().toMutableList()

        return (lCabeceras.count() > 0)
    }


    private fun obtenerCobros(): Boolean {
        val cobrosDao: CobrosDao? = MyDatabase.getInstance(fContexto)?.cobrosDao()
        val lCobros = cobrosDao?.getResumenPedidos() ?: emptyList<DatosCobrResPedidos>().toMutableList()

        return (lCobros.count() > 0)
    }

    private fun obtenerLineas(cabecera: DatosResPedidos): Boolean {
        val lineasDao: LineasDao? = MyDatabase.getInstance(fContexto)?.lineasDao()
        lLineas = lineasDao?.getResumenPedidos(cabecera.cabeceraId) ?: emptyList<DatosLinResPedidos>().toMutableList()

        return (lLineas.count() > 0)
    }


    private fun mostrarChunk(canvas: PdfContentByte, c: Chunk, x: Float, y: Float, fuente: Font, alineacion: Int) {
        c.font = fuente
        val phrase = Phrase(c)
        //ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, phrase, x, y, 0);
        ColumnText.showTextAligned(canvas, alineacion, phrase, x, y, 0f)
    }

    private fun tituloInforme(writer: PdfWriter) {
        val canvas = writer.directContent
        var c = Chunk("INFORME DE PEDIDOS Y COBROS.")
        mostrarChunk(canvas, c, 10f, y.toFloat(), fntHelv12Bold, Element.ALIGN_LEFT)
        y = (y -30).toShort()
        c = Chunk("PEDIDOS:")
        mostrarChunk(canvas, c, 10f, y.toFloat(), fntHelv12Bold, Element.ALIGN_LEFT)
        y = (y - 20).toShort()
    }




}