package es.albainformatica.albamobileandroid.cobros

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.entity.CobrosEnt
import es.albainformatica.albamobileandroid.tipoDocResumAsString
import kotlinx.android.synthetic.main.ly_ver_cobros.view.*
import kotlinx.android.synthetic.main.ly_ver_pendientes.view.*


class CobrosRvAdapter(
    private var lCobros: MutableList<CobrosEnt>, val context: Context,
    var listener: OnItemClickListener): RecyclerView.Adapter<CobrosRvAdapter.ViewHolder>() {

    private var selectedPos: Int = RecyclerView.NO_POSITION


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = lCobros[fPosicion]
        holder.bind(item)

        if (selectedPos == fPosicion) {
            holder.itemView.lyvdFecha.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.lyvdTipoDoc.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.lyvdSerie.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.lyvdNumero.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.lyvdTotal.typeface = Typeface.DEFAULT_BOLD
        } else {
            holder.itemView.lyvdFecha.typeface = Typeface.DEFAULT
            holder.itemView.lyvdTipoDoc.typeface = Typeface.DEFAULT
            holder.itemView.lyvdSerie.typeface = Typeface.DEFAULT
            holder.itemView.lyvdNumero.typeface = Typeface.DEFAULT
            holder.itemView.lyvdTotal.typeface = Typeface.DEFAULT
        }

        holder.itemView.setOnClickListener {
            // Tenemos en cuenta si el registro sobre el que pulsamos estaba anteriormente seleccionado
            if (selectedPos == fPosicion) {
                selectedPos = RecyclerView.NO_POSITION
            }
            else //Seleccionamos el registro
            {
                selectedPos = fPosicion
            }

            notifyDataSetChanged()
            listener.onClick(it, lCobros[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_ver_cobros, parent, false))
    }


    override fun getItemCount(): Int {
        return lCobros.size
    }

    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: CobrosEnt)
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fecha = itemView.findViewById(R.id.lyvdFecha) as TextView
        private val tipoDoc = itemView.findViewById(R.id.lyvdTipoDoc) as TextView
        private val serie = itemView.findViewById(R.id.lyvdSerie) as TextView
        private val numero = itemView.findViewById(R.id.lyvdNumero) as TextView
        private val total = itemView.findViewById(R.id.lyvdTotal) as TextView

        fun bind(cobro: CobrosEnt) {
            fecha.text = cobro.fechaCobro
            tipoDoc.text = tipoDocResumAsString(cobro.tipoDoc)
            serie.text = cobro.serie
            numero.text = cobro.numero.toString()
            total.text = cobro.cobro
        }
    }

}