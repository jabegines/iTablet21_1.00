package es.albainformatica.albamobileandroid.biocatalogo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.GruposParaCat
import es.albainformatica.albamobileandroid.R


class GruposRvAdapter(var grupos: List<GruposParaCat>, val context: Context,
                      private var listener: OnItemClickListener): RecyclerView.Adapter<GruposRvAdapter.ViewHolder>() {


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = grupos[position]
        holder.bind(item)

        holder.itemView.setOnClickListener {
            //selectedPos = position
            notifyItemChanged(position)
            listener.onClick(it, grupos[position])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.layout_biogrupos, parent, false))
    }


    override fun getItemCount(): Int {
        return grupos.size
    }


    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: GruposParaCat)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvDescr = itemView.findViewById(R.id.tvBioDescr) as TextView
        private val tvNumDep = itemView.findViewById(R.id.tvBioCaptArt) as TextView

        fun bind(grupo: GruposParaCat) {
            tvDescr.text = grupo.descripcion
            tvNumDep.text = grupo.numDepartamentos.toString()
        }

    }


}