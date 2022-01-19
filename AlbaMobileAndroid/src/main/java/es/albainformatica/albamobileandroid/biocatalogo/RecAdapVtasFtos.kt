package es.albainformatica.albamobileandroid.biocatalogo

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.DatosVtaFtos
import es.albainformatica.albamobileandroid.R
import kotlinx.android.synthetic.main.item_vtaftos.view.*




class RecAdapVtasFtos (var formatos: MutableList<DatosVtaFtos>, val context: Context, var listener: OnItemClickListener):
                        RecyclerView.Adapter<RecAdapVtasFtos.ViewHolder>() {

    var selectedPos: Int = RecyclerView.NO_POSITION
    var formatoId: Short = 0


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = formatos[fPosicion]
        holder.bind(item)

        if (selectedPos == fPosicion) {
            holder.itemView.tvDescrFto.setBackgroundColor(Color.parseColor("#009DD2"))
            holder.itemView.tvDescrFto.setTextColor(Color.WHITE)
        }
        else
        {
            holder.itemView.tvDescrFto.setBackgroundColor(Color.parseColor("#979797"))
            holder.itemView.tvDescrFto.setTextColor(Color.BLACK)
        }

        holder.itemView.setOnClickListener {
            // Tenemos en cuenta si el registro sobre el que pulsamos estaba anteriormente seleccionado
            if (selectedPos == fPosicion) { //Deseleccionamos el registro
                formatoId = 0
                selectedPos = RecyclerView.NO_POSITION
            }
            else //Seleccionamos el registro
            {
                formatoId = formatos[fPosicion].formatoId
                selectedPos = fPosicion
            }

            notifyDataSetChanged()
            listener.onClick(it, formatos[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.item_vtaftos, parent, false))
    }


    override fun getItemCount(): Int {
        return formatos.size
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosVtaFtos)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvDescr = itemView.findViewById(R.id.tvDescrFto) as TextView
        private val imvFto = itemView.findViewById(R.id.imgFtoVdo) as ImageView
        private val imvHco = itemView.findViewById(R.id.imgFtoHco) as ImageView

        fun bind(formato: DatosVtaFtos) {
            tvDescr.text = formato.descripcion
            // Presentamos u ocultamos la imagen que nos indica que el formato está vendido
            if (formato.ftoLineaId > 0 && formato.borrar != "T")
                imvFto.visibility = View.VISIBLE
            else
                imvFto.visibility = View.INVISIBLE

            if (formato.historicoId > 0)
                imvHco.visibility = View.VISIBLE
            else
                imvHco.visibility = View.INVISIBLE
        }

    }


}