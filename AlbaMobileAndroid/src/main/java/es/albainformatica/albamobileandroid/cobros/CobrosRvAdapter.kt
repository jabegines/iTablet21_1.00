package es.albainformatica.albamobileandroid.cobros

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.entity.CobrosEnt
import es.albainformatica.albamobileandroid.tipoDocResumAsString


class CobrosRvAdapter(
    private var lCobros: MutableList<CobrosEnt>, val context: Context,
    var listener: OnItemClickListener): RecyclerView.Adapter<CobrosRvAdapter.ViewHolder>() {


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lCobros[position]
        holder.bind(item)
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