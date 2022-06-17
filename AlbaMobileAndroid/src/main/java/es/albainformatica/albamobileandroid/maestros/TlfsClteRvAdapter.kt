package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.entity.ContactosCltesEnt
import kotlinx.android.synthetic.main.layout_telf_cltes.view.*




class TlfsClteRvAdapter(private var lTelefonos: MutableList<ContactosCltesEnt>, val context: Context,
                         var listener: OnItemClickListener): RecyclerView.Adapter<TlfsClteRvAdapter.ViewHolder>() {

    private var selectedPos: Int = RecyclerView.NO_POSITION

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = lTelefonos[fPosicion]
        holder.bind(item)

        if (selectedPos == fPosicion) {
            holder.itemView.ly_contacto.setTextColor(Color.BLACK)
            holder.itemView.ly_contacto.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.ly_telf1.setTextColor(Color.BLACK)
            holder.itemView.ly_telf1.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.ly_telf2.setTextColor(Color.BLACK)
            holder.itemView.ly_telf2.typeface = Typeface.DEFAULT_BOLD
        } else {
            holder.itemView.ly_contacto.setTextColor(Color.GRAY)
            holder.itemView.ly_contacto.typeface = Typeface.DEFAULT
            holder.itemView.ly_telf1.setTextColor(Color.GRAY)
            holder.itemView.ly_telf1.typeface = Typeface.DEFAULT
            holder.itemView.ly_telf2.setTextColor(Color.GRAY)
            holder.itemView.ly_telf2.typeface = Typeface.DEFAULT
        }

        holder.itemView.setOnClickListener {
            selectedPos = fPosicion
            notifyDataSetChanged()
            listener.onClick(it, lTelefonos[fPosicion])
        }

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.layout_telf_cltes, parent, false))
    }


    override fun getItemCount(): Int {
        return lTelefonos.size
    }

    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: ContactosCltesEnt)
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContacto = itemView.findViewById(R.id.ly_contacto) as TextView
        private val tvTelefono1 = itemView.findViewById(R.id.ly_telf1) as TextView
        private val tvTelefono2 = itemView.findViewById(R.id.ly_telf2) as TextView

        fun bind(telefono: ContactosCltesEnt) {
            tvContacto.text = telefono.nombre
            tvTelefono1.text = telefono.telefono1
            tvTelefono2.text = telefono.telefono2
        }
    }

}