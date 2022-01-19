package es.albainformatica.albamobileandroid.historicos

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.DatosHcArtClte
import es.albainformatica.albamobileandroid.R
import kotlinx.android.synthetic.main.item_hcodoc_list.view.*


class ArtHcoDocRvAdapter (var docs: List<DatosHcArtClte>, val context: Context, var listener: OnItemClickListener):
        RecyclerView.Adapter<ArtHcoDocRvAdapter.ViewHolder>() {

    private var selectedPos: Int = RecyclerView.NO_POSITION
    var articuloId: Int = 0
    var idHco: Int = 0


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = docs[fPosicion]
        holder.bind(item)

        if (selectedPos == fPosicion) {
            holder.itemView.tvHcoArtClCodArt.setTextColor(Color.BLACK)
            holder.itemView.tvHcoArtClDescr.setTextColor(Color.BLACK)
            holder.itemView.tvHcoArtClPorcDevol.setTextColor(Color.BLACK)
        }
        else
        {
            holder.itemView.tvHcoArtClCodArt.setTextColor(Color.GRAY)
            holder.itemView.tvHcoArtClDescr.setTextColor(Color.GRAY)
            holder.itemView.tvHcoArtClPorcDevol.setTextColor(Color.GRAY)
        }
        holder.itemView.tvHcoArtClCantPed.setTextColor(Color.MAGENTA)


        holder.itemView.setOnClickListener {
            // Tenemos en cuenta si el registro sobre el que pulsamos estaba anteriormente seleccionado
            //Deseleccionamos el registro
            if (selectedPos == fPosicion) {
                articuloId = 0
                idHco = 0
                selectedPos = RecyclerView.NO_POSITION
            }
            //Seleccionamos el registro
            else
            {
                articuloId = docs[fPosicion].articuloId
                idHco = docs[fPosicion].hcoPorcArticClteId
                selectedPos = fPosicion
            }

            notifyDataSetChanged()
            listener.onClick(it, docs[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.item_hcodoc_list, parent, false))
    }

    override fun getItemCount(): Int {
        return docs.size
    }


    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosHcArtClte)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvCodArt = itemView.findViewById(R.id.tvHcoArtClCodArt) as TextView
        private val tvDescr = itemView.findViewById(R.id.tvHcoArtClDescr) as TextView
        private val tvPorcDev = itemView.findViewById(R.id.tvHcoArtClPorcDevol) as TextView
        private val tvCantPedida = itemView.findViewById(R.id.tvHcoArtClCantPed) as TextView

        @SuppressLint("SetTextI18n")
        fun bind(documento: DatosHcArtClte) {
            tvCodArt.text = documento.codigo
            tvDescr.text = documento.descripcion
            tvPorcDev.text = documento.porcDevol + "%"
            tvCantPedida.text = documento.cantPedida
        }
    }



}