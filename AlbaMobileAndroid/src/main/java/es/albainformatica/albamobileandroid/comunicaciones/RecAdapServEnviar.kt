package es.albainformatica.albamobileandroid.comunicaciones

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.ListaPaquetes
import es.albainformatica.albamobileandroid.R
import kotlinx.android.synthetic.main.item_paquetes_list.view.*


class RecAdapServEnviar(var paquetes: MutableList<ListaPaquetes>, val context: Context, var listener: OnItemClickListener):
        RecyclerView.Adapter<RecAdapServEnviar.ViewHolder>() {

    private var selectedPos: Int = RecyclerView.NO_POSITION
    private var paqueteId: Int = 0


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = paquetes[fPosicion]
        holder.bind(item)

        if (selectedPos == fPosicion) {
            holder.itemView.tvIdPaquete.setTextColor(Color.BLACK)
            holder.itemView.tvIdPaquete.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.tvFechaEnvio.setTextColor(Color.BLACK)
            holder.itemView.tvFechaEnvio.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.tvFechaRecogida.setTextColor(Color.BLACK)
            holder.itemView.tvFechaRecogida.typeface = Typeface.DEFAULT_BOLD
        }
        else
        {
            if (item.fechaHoraRecogida == "") {
                holder.itemView.tvIdPaquete.setTextColor(Color.RED)
                holder.itemView.tvIdPaquete.typeface = Typeface.DEFAULT
                holder.itemView.tvFechaEnvio.setTextColor(Color.RED)
                holder.itemView.tvFechaEnvio.typeface = Typeface.DEFAULT
                holder.itemView.tvFechaRecogida.setTextColor(Color.RED)
                holder.itemView.tvFechaRecogida.typeface = Typeface.DEFAULT
            } else {
                holder.itemView.tvIdPaquete.setTextColor(Color.GRAY)
                holder.itemView.tvIdPaquete.typeface = Typeface.DEFAULT
                holder.itemView.tvFechaEnvio.setTextColor(Color.GRAY)
                holder.itemView.tvFechaEnvio.typeface = Typeface.DEFAULT
                holder.itemView.tvFechaRecogida.setTextColor(Color.GREEN)
                holder.itemView.tvFechaRecogida.typeface = Typeface.DEFAULT
            }
        }

        holder.itemView.setOnClickListener {
            // Tenemos en cuenta si el registro sobre el que pulsamos estaba anteriormente seleccionado
            if (selectedPos == fPosicion) { //Deseleccionamos el registro
                paqueteId = 0
                selectedPos = RecyclerView.NO_POSITION
            }
            else //Seleccionamos el registro
            {
                paqueteId = paquetes[fPosicion].numPaquete
                selectedPos = fPosicion
            }

            notifyDataSetChanged()
            listener.onClick(it, paquetes[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.item_paquetes_list, parent, false))
    }


    override fun getItemCount(): Int {
        return paquetes.size
    }


    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }


    interface OnItemClickListener {
        fun onClick(view: View, data: ListaPaquetes)
    }



    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val fIdPaquete = itemView.findViewById(R.id.tvIdPaquete) as TextView
        private val fFechaEnvio = itemView.findViewById(R.id.tvFechaEnvio) as TextView
        private val fFechaRecogida = itemView.findViewById(R.id.tvFechaRecogida) as TextView

        fun bind(paquete: ListaPaquetes) {
            fIdPaquete.text = paquete.numPaquete.toString()
            fFechaEnvio.text = paquete.fechaHoraEnvio
            fFechaRecogida.text = paquete.fechaHoraRecogida
        }
    }

}