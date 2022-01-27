package es.albainformatica.albamobileandroid.oldcatalogo

import es.albainformatica.albamobileandroid.dimeRutaImagenes
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.BaseAdapter
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.Configuracion
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.R
import java.io.File
import java.util.*

/**
 * Created by jabegines on 11/10/13.
 */
class ItemArticuloAdapter(protected var activity: Activity, protected var items: ArrayList<ItemArticulo>,
    queEmpresa: Short) : BaseAdapter() {
    val carpetaImagenes: String = dimeRutaImagenes(activity)
    private val fArticulos: ArticulosClase = ArticulosClase(activity)
    private var fFtoPrecio: String = ""
    private val fFtoCantidad: String
    private val fIvaIncluido: Boolean
    private val fConfiguracion: Configuracion = Comunicador.fConfiguracion

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return items[position].articulo.toLong()
    }

    override fun getView(position: Int, contentView: View?, parent: ViewGroup?): View? {
        // El parámetro 'View contentView' que recibimos contiene el layout completo de
        // cada fila del listView, por eso podemos hacer luego vi.findViewById para
        // buscar vistas individuales dentro de dicho layout.
        var vi = contentView

        // Si el parámetro 'View contentView' es nulo, lo inflo con el layout que he
        // diseñado para el listView: ly_artic_imagen.
        if (contentView == null) {
            val inflater =
                activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            vi = inflater.inflate(R.layout.ly_artic_imagen, null)
        }
        val item = items[position]

        // Obtenemos la imagen del artículo.
        val image = vi?.findViewById<ImageView>(R.id.imgvArtIm)
        val path = carpetaImagenes + item.getImagen()
        val file = File(path)
        if (file.exists()) image?.setImageURI(Uri.parse(path)) else image?.setImageDrawable(null)

        // Imagen de la oferta. Buscamos si el artículo tiene oferta para presentar la imagen o no.
        val imgOfta = vi?.findViewById<ImageView>(R.id.imgvArtImgOft)
        if (fArticulos.tieneOferta(item.articulo)) {
            imgOfta?.visibility = View.VISIBLE
        } else {
            imgOfta?.visibility = View.GONE
        }
        val tvDescr = vi?.findViewById<TextView>(R.id.tvArticulo)
        tvDescr?.text = item.descr
        val tvCodigo = vi?.findViewById<TextView>(R.id.tvCodigoArt)
        tvCodigo?.text = item.codigo
        val tvDescrFto = vi?.findViewById<TextView>(R.id.tvDescrFto)
        tvDescrFto?.text = item.descrFto
        cargarEtiquetas(item, vi)
        return vi
    }

    private fun cargarEtiquetas(item: ItemArticulo, vi: View?) {
        val tvExistencias = vi?.findViewById<TextView>(R.id.tvExistencias)
        val tvUdsCaja = vi?.findViewById<TextView>(R.id.tvUnidadesCaja)
        val tvPrCajas = vi?.findViewById<TextView>(R.id.tvPrCajas)
        val tvDtCajas = vi?.findViewById<TextView>(R.id.tvDtCajas)
        val tvPrecio = vi?.findViewById<TextView>(R.id.tvPrecio)
        val tvDto = vi?.findViewById<TextView>(R.id.tvDescuento)

        // Existencias
        if (item.stock != 0.0) {
            tvExistencias?.text = String.format(fFtoCantidad, item.stock)
            tvExistencias?.alpha = 1f
        } else {
            tvExistencias?.text = "--"
            tvExistencias?.alpha = 0.2f
        }

        // Precio/precio en oferta, precio anterior y descuento/descuento en oferta.
        if (item.prClte != null) {
            val sPrecio = item.prClte?.replace(',', '.') ?: "0.0"
            var dPrecio = sPrecio.toDouble()
            // Si vendemos iva incluído tendremos que calcular el precio con iva.
            if (fIvaIncluido) {
                val dImpIva = dPrecio * item.porcIva / 100
                dPrecio += dImpIva
            }
            tvPrecio?.text = String.format(fFtoPrecio, dPrecio) + " €"
        } else tvPrecio?.text = ""

        // % Dto.
        if (item.dto != null) {
            val sDto = item.dto?.replace(',', '.') ?: "0.0"
            val dDto = sDto.toDouble()
            if (dDto != 0.0) {
                tvDto?.text = String.format(Locale.getDefault(), "%.2f", dDto)
                tvDto?.alpha = 1f
            } else {
                tvDto?.text = "--"
                tvDto?.alpha = 0.2f
            }
        } else tvDto?.text = ""

        // Unidades por caja
        val dUndsCaja = item.undCaja.toDouble()
        if (fConfiguracion.fTamanyoPantLargo) tvUdsCaja?.text =
            String.format("%.0f", dUndsCaja) + " Ud" else tvUdsCaja?.text =
            String.format("%.0f", dUndsCaja)

        // Tarifa cajas
        if (item.prCajas != null) {
            val sPrCajas = item.prCajas?.replace(',', '.') ?: "0.0"
            var dPrecio = sPrCajas.toDouble()
            if (fIvaIncluido) {
                val dImpIva = dPrecio * item.porcIva / 100
                dPrecio += dImpIva
            }
            tvPrCajas?.text = String.format(fFtoPrecio, dPrecio) + " €"
        } else tvPrCajas?.text = ""

        // % Dto cajas.
        if (item.dtoCajas != null) {
            val sDto = item.dtoCajas?.replace(',', '.') ?: "0.0"
            val dDto = sDto.toDouble()
            if (dDto != 0.0) {
                tvDtCajas?.text = String.format(Locale.getDefault(), "%.2f", dDto)
                tvDtCajas?.alpha = 1f
            } else {
                tvDtCajas?.text = "--"
                tvDtCajas?.alpha = 0.2f
            }
        } else tvDtCajas?.text = ""

        // Si el artículo está en oferta, presentaremos el precio de la oferta en la casilla del precio y el precio
        // sin oferta en la casilla tvPrAnt. El descuento, sea de oferta o no, lo presentaremos siempre en la casilla tvDto.
        if (item.tieneOferta()) {
            val sPrecio = item.prOfta.replace(',', '.')
            var dPrecioOfta = sPrecio.toDouble()
            if (dPrecioOfta != 0.0) {
                if (fIvaIncluido) {
                    val dImpIva = dPrecioOfta * item.porcIva / 100
                    dPrecioOfta += dImpIva
                }
                tvPrecio?.text = String.format(fFtoPrecio, dPrecioOfta)
            } else if (item.dtoOfta != null) {
                val sDto = item.dtoOfta.replace(',', '.')
                val dDto = sDto.toDouble()
                if (dDto != 0.0) {
                    tvDto?.text = String.format(Locale.getDefault(), "%.2f", dDto)
                    tvDto?.alpha = 1f
                } else {
                    tvDto?.text = "--"
                    tvDto?.alpha = 0.2f
                }
            }
        }
    }

    init {
        // Necesitamos fArticulos para ir viendo si cada artículo está en oferta.
        fIvaIncluido = fConfiguracion.ivaIncluido(queEmpresa)
        fFtoPrecio =
            if (fIvaIncluido) fConfiguracion.formatoDecPrecioIva() else fConfiguracion.formatoDecPrecioBase()
        fFtoCantidad = fConfiguracion.formatoDecCantidad()
    }
}