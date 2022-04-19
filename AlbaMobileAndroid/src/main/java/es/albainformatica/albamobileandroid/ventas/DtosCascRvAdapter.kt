package es.albainformatica.albamobileandroid.ventas

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.DescuentosLinea
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.entity.DtosLineasEnt


class DtosCascRvAdapter(var desctos: List<DescuentosLinea>, val context: Context,
                        private var listener: OnItemClickListener): RecyclerView.Adapter<DtosCascRvAdapter.ViewHolder>() {

    private val fDecPrBase = Comunicador.fConfiguracion.decimalesImpBase()


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = desctos[position]
        holder.bind(item, fDecPrBase)

        holder.itemView.setOnClickListener {
            notifyItemChanged(position)
            listener.onClick(it, desctos[position])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_dtos_cascada, parent, false))
    }


    override fun getItemCount(): Int {
        return desctos.size
    }


    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DescuentosLinea)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
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

        fun bind(dto: DescuentosLinea, fDecPrBase: Int) {

            //tvDescr.text = linea.descripcion
            //tvImporte.text = String.format("%." + fDecPrBase + "f", linea.importe.toDouble())
            //tvDescrFto.text = linea.descrFto
            //tvCodArt.text = linea.codArticulo
            //tvCajas.text = linea.cajas
            //tvTasa1.text = String.format("%." + fDecPrBase + "f", linea.tasa1.toDouble())
            //tvPrecio.text = String.format("%." + fDecPrBase + "f", linea.precio.toDouble())
            //tvDto.text = String.format("%.2f", linea.dto.toDouble())
            //tvTasa2.text = String.format("%." + fDecPrBase + "f", linea.tasa2.toDouble())
        }

    }


}