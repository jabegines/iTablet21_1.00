package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.ListaClientes
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.ancho_codclte
import es.albainformatica.albamobileandroid.ponerCeros
import kotlinx.android.synthetic.main.item_clientes_list.view.*


class ClientesRvAdapter(var clientes: List<ListaClientes>, val context: Context, private var listener: OnItemClickListener): RecyclerView.Adapter<ClientesRvAdapter.ViewHolder>() {

    private var selectedPos: Int = RecyclerView.NO_POSITION


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = clientes[position]
        holder.bind(item)

        if (selectedPos == position) {
            holder.itemView.tvClt_CodClte.setTextColor(Color.BLACK)
            holder.itemView.tvClt_CodClte.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.tvClt_NombreClte.setTextColor(Color.BLACK)
            holder.itemView.tvClt_NombreClte.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.tvClt_NombreClte2.setTextColor(Color.BLACK)
            holder.itemView.tvClt_NombreClte2.typeface = Typeface.DEFAULT_BOLD
        } else {
            holder.itemView.tvClt_CodClte.setTextColor(Color.GRAY)
            holder.itemView.tvClt_CodClte.typeface = Typeface.DEFAULT
            holder.itemView.tvClt_NombreClte.setTextColor(Color.GRAY)
            holder.itemView.tvClt_NombreClte.typeface = Typeface.DEFAULT
            holder.itemView.tvClt_NombreClte2.setTextColor(Color.GRAY)
            holder.itemView.tvClt_NombreClte2.typeface = Typeface.DEFAULT
        }

        holder.itemView.setOnClickListener {
            selectedPos = position
            notifyItemChanged(position)
            listener.onClick(it, clientes[position])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.item_clientes_list, parent, false))
    }


    override fun getItemCount(): Int {
        return clientes.size
    }


    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: ListaClientes)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val codigo = itemView.findViewById(R.id.tvClt_CodClte) as TextView
        private val nombre = itemView.findViewById(R.id.tvClt_NombreClte) as TextView
        private val nombreCom = itemView.findViewById(R.id.tvClt_NombreClte2) as TextView

        fun bind(cliente: ListaClientes) {
            codigo.text = ponerCeros(cliente.codigo, ancho_codclte)
            nombre.text = cliente.nombre
            nombreCom.text = cliente.nombreComercial
        }

    }


}