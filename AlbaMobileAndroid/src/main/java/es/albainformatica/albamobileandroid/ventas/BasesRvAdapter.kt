package es.albainformatica.albamobileandroid.ventas

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.Comunicador.Companion.fConfiguracion
import es.albainformatica.albamobileandroid.R


class BasesRvAdapter(var bases: ArrayList<ListaBasesDoc.TBaseDocumento>, val context: Context,
                     private var listener: OnItemClickListener): RecyclerView.Adapter<BasesRvAdapter.ViewHolder>() {

    //var selectedPos: Int = RecyclerView.NO_POSITION
    private val fDecPrBase = fConfiguracion.decimalesImpBase()


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = bases[position]
        holder.bind(item, fDecPrBase)

        holder.itemView.setOnClickListener {
            //selectedPos = position
            notifyItemChanged(position)
            listener.onClick(it, bases[position])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.layout_basesdoc, parent, false))
    }


    override fun getItemCount(): Int {
        return bases.size
    }


    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: ListaBasesDoc.TBaseDocumento)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvBase = itemView.findViewById(R.id.baseimp) as TextView
        private val tvPorcIva = itemView.findViewById(R.id.porciva) as TextView
        private val tvImpIva = itemView.findViewById(R.id.impiva) as TextView
        private val tvPorcRe = itemView.findViewById(R.id.porcre) as TextView
        private val tvImpRe = itemView.findViewById(R.id.impre) as TextView
        private val tvImpTotal = itemView.findViewById(R.id.imptotal) as TextView

        fun bind(base: ListaBasesDoc.TBaseDocumento, fDecPrBase: Int) {
            tvBase.text = String.format("%." + fDecPrBase + "f", base.fBaseImponible)
            tvPorcIva.text = String.format("%.2f", base.fPorcIva)
            tvImpIva.text = String.format("%." + fDecPrBase + "f", base.fImporteIva)
            tvPorcRe.text = String.format("%.2f", base.fPorcRe)
            tvImpRe.text = String.format("%." + fDecPrBase + "f", base.fImporteRe)
            tvImpTotal.text = String.format("%." + fDecPrBase + "f", base.fTotalConImptos)
        }

    }


}