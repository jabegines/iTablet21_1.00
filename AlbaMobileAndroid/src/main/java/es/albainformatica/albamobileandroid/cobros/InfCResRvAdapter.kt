package es.albainformatica.albamobileandroid.cobros

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.Comunicador.Companion.fConfiguracion
import es.albainformatica.albamobileandroid.DatosResCobros
import es.albainformatica.albamobileandroid.R


class InfCResRvAdapter(
    private var lCobrosDivisas: MutableList<DatosResCobros>, val context: Context,
    var listener: OnItemClickListener): RecyclerView.Adapter<InfCResRvAdapter.ViewHolder>() {


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lCobrosDivisas[position]
        holder.bind(item )
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_res_inf_cobros, parent, false))
    }


    override fun getItemCount(): Int {
        return lCobrosDivisas.size
    }

    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosResCobros)
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fFtoDecImpIva = fConfiguracion.formatoDecImptesIva()
        private val descripcion = itemView.findViewById(R.id.lyricDivisa) as TextView
        private val importe = itemView.findViewById(R.id.lyricImpte) as TextView

        fun bind(divisa: DatosResCobros) {
            descripcion.text = divisa.descripcion
            importe.text = String.format(fFtoDecImpIva, divisa.cobro)
        }
    }

}