package es.albainformatica.albamobileandroid.biocatalogo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.DepartParaCat
import es.albainformatica.albamobileandroid.R


class DepartRvAdapter(var grupos: List<DepartParaCat>, val context: Context,
                      private var listener: OnItemClickListener): RecyclerView.Adapter<DepartRvAdapter.ViewHolder>() {


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = grupos[position]
        holder.bind(item)

        holder.itemView.setOnClickListener {
            notifyItemChanged(position)
            listener.onClick(it, grupos[position])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.layout_biocatalogos, parent, false))
    }


    override fun getItemCount(): Int {
        return grupos.size
    }


    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DepartParaCat)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvDescr = itemView.findViewById(R.id.tvBioDescr) as TextView
        private val tvNumDep = itemView.findViewById(R.id.tvBioCaptArt) as TextView

        fun bind(departamento: DepartParaCat) {
            tvDescr.text = departamento.descripcion
            tvNumDep.text = departamento.numArticulos.toString()
        }

    }


}