package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.Configuracion
import es.albainformatica.albamobileandroid.ListaArticulos
import es.albainformatica.albamobileandroid.R


class ArticulosRvAdapter(var articulos: MutableList<ListaArticulos>, val context: Context, private var listener: OnItemClickListener): RecyclerView.Adapter<ArticulosRvAdapter.ViewHolder>() {

    private val fConfiguracion: Configuracion = Comunicador.fConfiguracion
    var articuloId: Int = 0
    private val fFtoPrecio: String = fConfiguracion.formatoDecPrecioBase()
    private val fFtoCant: String = fConfiguracion.formatoDecCantidad()


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = articulos[position]
        holder.bind(item, context, fFtoPrecio, fFtoCant)

        holder.itemView.setOnClickListener {
            listener.onClick(it, articulos[position])
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.item_articulos_list, parent, false))
    }

    override fun getItemCount(): Int {
        return articulos.size
    }

    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: ListaArticulos)
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val fConfiguracion: Configuracion = Comunicador.fConfiguracion
        private val fRutaImagenes = fConfiguracion.rutaLocalComunicacion() + "/imagenes"
        private val imagen = itemView.findViewById(R.id.imgvArtIm) as ImageView
        private val imgOfta = itemView.findViewById(R.id.imgvArtImgOft) as ImageView

        private val codigo = itemView.findViewById(R.id.tvCodigoArt) as TextView
        private val descripcion = itemView.findViewById(R.id.tvDescrArt) as TextView
        private val descrFto = itemView.findViewById(R.id.tvDescrFto) as TextView
        private val stock = itemView.findViewById(R.id.tvExistencias) as TextView
        private val unidCaja = itemView.findViewById(R.id.tvUnidadesCaja) as TextView
        private val precio = itemView.findViewById(R.id.tvPrecio) as TextView
        private val prCajas = itemView.findViewById(R.id.tvPrCajas) as TextView
        private val dto = itemView.findViewById(R.id.tvDescuento) as TextView

        fun bind(articulo: ListaArticulos, context: Context, fFtoPrecio: String, fFtoCant: String) {
            val queFichero = "$fRutaImagenes/ART_" + articulo.articuloId + ".jpg"
            val bitmap = BitmapFactory.decodeFile(queFichero)
            imagen.setImageBitmap(bitmap)
            if (articulo.idOferta == 0) imgOfta.visibility = View.GONE
            else imgOfta.visibility = View.VISIBLE

            val quePrecio = String.format(fFtoPrecio, articulo.precio?.toDouble()) + " €"
            val quePrCajas = String.format(fFtoPrecio, articulo.prCaja?.toDouble()) + " €"
            val dDto = articulo.dto?.toDouble()
            val queDto: String = String.format("%.2f", articulo.dto?.toDouble()) + context.resources.getString(R.string.porcentaje)
            val dUCaja = articulo.ucaja?.toDouble()
            val queUCaja = String.format("%.0f", dUCaja) + context.resources.getString(R.string.unidades)
            val queStock = articulo.stock ?: "0"
            val dStock = queStock.toDouble()

            codigo.text = articulo.codigo
            descripcion.text = articulo.descripcion
            descrFto.text = articulo.descrfto
            stock.text = String.format(fFtoCant, dStock)
            unidCaja.text = queUCaja
            precio.text = quePrecio
            prCajas.text = quePrCajas
            dto.text = queDto
            if (dDto == 0.0) dto.alpha = 0.1f
            if (dUCaja == 0.0) unidCaja.alpha = 0.1f
            if (dStock == 0.0) stock.alpha = 0.1f
        }
    }
}