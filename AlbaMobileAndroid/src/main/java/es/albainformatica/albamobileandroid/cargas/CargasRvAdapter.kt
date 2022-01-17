package es.albainformatica.albamobileandroid.cargas

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.entity.CargasEnt
import es.albainformatica.albamobileandroid.ponerCeros
import kotlinx.android.synthetic.main.item_cargas_list.view.*


class CargasRvAdapter(var cargas: List<CargasEnt>, val context: Context, var listener: OnItemClickListener):
        RecyclerView.Adapter<CargasRvAdapter.ViewHolder>() {

    private var selectedPos: Int = RecyclerView.NO_POSITION
    var cargaId: Int = 0


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = cargas[fPosicion]
        holder.bind(item)


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
            if (selectedPos == fPosicion) { //Deseleccionamos el registro
                cargaId = 0
                selectedPos = RecyclerView.NO_POSITION
            }
            else //Seleccionamos el registro
            {
                cargaId = cargas[fPosicion].cargaId
                selectedPos = fPosicion
            }

            notifyDataSetChanged()
            listener.onClick(it, cargas[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.item_cargas_list, parent, false))
    }

    override fun getItemCount(): Int {
        return cargas.size
    }


    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: CargasEnt)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvwCargaId = itemView.findViewById(R.id.tvCargaId) as TextView
        private val tvEmpresaCarga = itemView.findViewById(R.id.tvEmpresaCarga) as TextView
        private val tvwFechaCarga = itemView.findViewById(R.id.tvFechaCarga) as TextView
        private val tvwHoraCarga = itemView.findViewById(R.id.tvHoraCarga) as TextView

        fun bind(carga: CargasEnt) {
            val queTexto = "PC-" + carga.cargaId.toString()
            if (carga.esFinDeDia.equals("T", true)) tvwCargaId.text = queTexto
            else tvwCargaId.text = carga.cargaId.toString()
            tvEmpresaCarga.text = ponerCeros(carga.empresa.toString(), 3)
            tvwFechaCarga.text = carga.fecha
            tvwHoraCarga.text = carga.hora
        }
    }


}