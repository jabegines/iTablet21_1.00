package es.albainformatica.albamobileandroid.comunicaciones

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.entity.NumExportEnt
import kotlinx.android.synthetic.main.ly_numexportac.view.*


class NumExportRvAdapter(private var lExportaciones: MutableList<NumExportEnt>, val context: Context,
            var listener: OnItemClickListener): RecyclerView.Adapter<NumExportRvAdapter.ViewHolder>() {

    private var selectedPos: Int = RecyclerView.NO_POSITION

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = lExportaciones[fPosicion]
        holder.bind(item)

        if (selectedPos == fPosicion) {
            holder.itemView.tvNumExpNumero.setTextColor(Color.BLACK)
            holder.itemView.tvNumExpNumero.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.tvNumExpFecha.setTextColor(Color.BLACK)
            holder.itemView.tvNumExpFecha.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.tvNumExpHora.setTextColor(Color.BLACK)
            holder.itemView.tvNumExpHora.typeface = Typeface.DEFAULT_BOLD
        } else {
            holder.itemView.tvNumExpNumero.setTextColor(Color.GRAY)
            holder.itemView.tvNumExpNumero.typeface = Typeface.DEFAULT
            holder.itemView.tvNumExpFecha.setTextColor(Color.GRAY)
            holder.itemView.tvNumExpFecha.typeface = Typeface.DEFAULT
            holder.itemView.tvNumExpHora.setTextColor(Color.GRAY)
            holder.itemView.tvNumExpHora.typeface = Typeface.DEFAULT
        }

        holder.itemView.setOnClickListener {
            selectedPos = fPosicion
            notifyDataSetChanged()
            listener.onClick(it, lExportaciones[fPosicion])
        }

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_numexportac, parent, false))
    }


    override fun getItemCount(): Int {
        return lExportaciones.size
    }

    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: NumExportEnt)
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val numero = itemView.findViewById(R.id.tvNumExpNumero) as TextView
        private val fecha = itemView.findViewById(R.id.tvNumExpFecha) as TextView
        private val hora = itemView.findViewById(R.id.tvNumExpHora) as TextView

        fun bind(exportacion: NumExportEnt) {
            numero.text = exportacion.numExport.toString()
            fecha.text = exportacion.fecha
            hora.text = exportacion.hora
        }
    }

}