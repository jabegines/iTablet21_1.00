package es.albainformatica.albamobileandroid.ventas

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.Comunicador.Companion.fConfiguracion
import es.albainformatica.albamobileandroid.DatosLinVtas
import es.albainformatica.albamobileandroid.R


class LineasVtasRvAdapter(var lineas: List<DatosLinVtas>, val context: Context,
                          private var listener: OnItemClickListener): RecyclerView.Adapter<LineasVtasRvAdapter.ViewHolder>() {

    private val fDecPrBase = fConfiguracion.decimalesImpBase()


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lineas[position]
        holder.bind(item, fDecPrBase)

        holder.itemView.setOnClickListener {
            //selectedPos = position
            notifyItemChanged(position)
            listener.onClick(it, lineas[position])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_lineas_ventas, parent, false))
    }


    override fun getItemCount(): Int {
        return lineas.size
    }


    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosLinVtas)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val fRutaImagenes = fConfiguracion.rutaLocalComunicacion() + "/imagenes"

        private val imgArt = itemView.findViewById(R.id.imvArtLinea) as ImageView
        private val tvDescr = itemView.findViewById(R.id.ly_vl_descr) as TextView
        private val tvImporte = itemView.findViewById(R.id.ly_vl_impte) as TextView
        private val tvDescrFto = itemView.findViewById(R.id.ly_vl_descrfto) as TextView
        private val tvCodArt = itemView.findViewById(R.id.ly_vl_codart) as TextView
        private val tvTarifa = itemView.findViewById(R.id.ly_vl_tarifa) as TextView
        private val tvPiezas = itemView.findViewById(R.id.ly_vl_piezas) as TextView
        private val tvCajas = itemView.findViewById(R.id.ly_vl_cajas) as TextView
        private val tvCantidad = itemView.findViewById(R.id.ly_vl_cant) as TextView
        private val tvTasa1 = itemView.findViewById(R.id.ly_vl_tasa1) as TextView
        private val tvPrecio = itemView.findViewById(R.id.ly_vl_precio) as TextView
        private val tvDto = itemView.findViewById(R.id.ly_vl_dto) as TextView
        private val tvTasa2 = itemView.findViewById(R.id.ly_vl_tasa2) as TextView

        fun bind(linea: DatosLinVtas, fDecPrBase: Int) {
            val queFichero = "$fRutaImagenes/ART_" + linea.articuloId + ".jpg"
            val bitmap = BitmapFactory.decodeFile(queFichero)
            imgArt.setImageBitmap(bitmap)

            tvDescr.text = linea.descripcion
            tvImporte.text = String.format("%." + fDecPrBase + "f", linea.importe.toDouble())
            tvDescrFto.text = linea.descrFto
            tvCodArt.text = linea.codArticulo
            //tvTarifa.text = linea.
            //tvPiezas.text = linea.
            tvCajas.text = linea.cajas
            tvCantidad.text = linea.cantidad
            tvTasa1.text = String.format("%." + fDecPrBase + "f", linea.tasa1.toDouble())
            tvPrecio.text = String.format("%." + fDecPrBase + "f", linea.precio.toDouble())
            tvDto.text = String.format("%.2f", linea.dto.toDouble())
            tvTasa2.text = String.format("%." + fDecPrBase + "f", linea.tasa2.toDouble())
        }

    }


}