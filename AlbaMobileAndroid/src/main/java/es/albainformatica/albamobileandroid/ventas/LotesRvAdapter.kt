package es.albainformatica.albamobileandroid.ventas

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.entity.LotesEnt
import kotlinx.android.synthetic.main.item_cargas_list.view.*


class LotesRvAdapter(var lotes: List<LotesEnt>, private val fFtoCantidad: String, val context: Context,
                     var listener: OnItemClickListener): RecyclerView.Adapter<LotesRvAdapter.ViewHolder>() {

    private var selectedPos: Int = RecyclerView.NO_POSITION
    //var cargaId: Int = 0


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = lotes[fPosicion]
        holder.bind(item, fFtoCantidad)

        if (selectedPos == fPosicion) {
            holder.itemView.tvCargaId.setTextColor(Color.BLACK)
            holder.itemView.tvCargaId.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.tvEmpresaCarga.setTextColor(Color.BLACK)
            holder.itemView.tvEmpresaCarga.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.tvFechaCarga.setTextColor(Color.BLACK)
            holder.itemView.tvFechaCarga.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.tvHoraCarga.setTextColor(Color.BLACK)
            holder.itemView.tvHoraCarga.typeface = Typeface.DEFAULT_BOLD
        }
        else
        {
            holder.itemView.tvCargaId.setTextColor(Color.GRAY)
            holder.itemView.tvCargaId.typeface = Typeface.DEFAULT
            holder.itemView.tvEmpresaCarga.setTextColor(Color.GRAY)
            holder.itemView.tvEmpresaCarga.typeface = Typeface.DEFAULT
            holder.itemView.tvFechaCarga.setTextColor(Color.GRAY)
            holder.itemView.tvFechaCarga.typeface = Typeface.DEFAULT
            holder.itemView.tvHoraCarga.setTextColor(Color.GRAY)
            holder.itemView.tvHoraCarga.typeface = Typeface.DEFAULT
        }

        holder.itemView.setOnClickListener {
            // Tenemos en cuenta si el registro sobre el que pulsamos estaba anteriormente seleccionado
            selectedPos = if (selectedPos == fPosicion) { //Deseleccionamos el registro
                //cargaId = 0
                RecyclerView.NO_POSITION
            } else //Seleccionamos el registro
            {
                //cargaId = cargas[fPosicion].cargaId
                fPosicion
            }

            notifyDataSetChanged()
            listener.onClick(it, lotes[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.layout_lotes, parent, false))
    }

    override fun getItemCount(): Int {
        return lotes.size
    }


    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: LotesEnt)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvLote = itemView.findViewById(R.id.lylot_lote) as TextView
        private val tvStock = itemView.findViewById(R.id.lylot_stock) as TextView


        fun bind(lote: LotesEnt, fFtoCantidad: String) {
            tvLote.text = lote.lote
            val dStock = lote.stock.replace(',', '.').toDouble()
            tvStock.text = String.format(fFtoCantidad, dStock)
        }
    }


}