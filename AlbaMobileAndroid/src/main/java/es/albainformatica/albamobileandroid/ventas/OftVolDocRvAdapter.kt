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
import es.albainformatica.albamobileandroid.DatosLinVtas
import es.albainformatica.albamobileandroid.DatosOftVol
import es.albainformatica.albamobileandroid.R


class OftVolDocRvAdapter(var lineas: List<DatosOftVol>, val context: Context,
                         private var listener: OnItemClickListener): RecyclerView.Adapter<OftVolDocRvAdapter.ViewHolder>() {

    private val fDecPrBase = Comunicador.fConfiguracion.decimalesImpBase()


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
        return ViewHolder(layoutInflater.inflate(R.layout.ly_lineas_oftvol, parent, false))
    }


    override fun getItemCount(): Int {
        return lineas.size
    }


    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosOftVol)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvDescr = itemView.findViewById(R.id.tvLinOftVolDescr) as TextView
        private val tvImporte = itemView.findViewById(R.id.tvLinOftVolImpte) as TextView

        fun bind(oferta: DatosOftVol, fDecPrBase: Int) {
            tvDescr.text = oferta.descripcion
            tvImporte.text = String.format("%." + fDecPrBase + "f", oferta.importe.toDouble())
        }

    }


}