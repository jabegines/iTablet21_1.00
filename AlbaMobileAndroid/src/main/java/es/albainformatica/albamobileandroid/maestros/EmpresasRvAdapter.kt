package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.DatosEmpresas
import es.albainformatica.albamobileandroid.R
import kotlinx.android.synthetic.main.item_formatos_list.view.*


class EmpresasRvAdapter(var empresas: MutableList<DatosEmpresas>, val context: Context,
                      var listener: OnItemClickListener): RecyclerView.Adapter<EmpresasRvAdapter.ViewHolder>() {

    private var selectedPos: Int = RecyclerView.NO_POSITION

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = empresas[position]
        holder.bind(item )

        if (selectedPos == position) {
            holder.itemView.tvDescrFto.setBackgroundColor(Color.parseColor("#009DD2"))
            holder.itemView.tvDescrFto.setTextColor(Color.WHITE)
        } else {
            holder.itemView.tvDescrFto.setBackgroundColor(Color.parseColor("#FFFFFF"))
            holder.itemView.tvDescrFto.setTextColor(Color.BLACK)
        }

        holder.itemView.setOnClickListener {
            selectedPos = position
            notifyDataSetChanged()
            listener.onClick(it, empresas[position])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.item_formatos_list, parent, false))
    }


    override fun getItemCount(): Int {
        return empresas.size
    }

    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosEmpresas)
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val descripcion = itemView.findViewById(R.id.tvDescrFto) as TextView

        fun bind(empresa: DatosEmpresas) {
            descripcion.text = empresa.nombreFiscal
        }
    }

}