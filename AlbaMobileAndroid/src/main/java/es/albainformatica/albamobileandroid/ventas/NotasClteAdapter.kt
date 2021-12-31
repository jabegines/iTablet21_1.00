package es.albainformatica.albamobileandroid.ventas

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.entity.NotasCltesEnt
import kotlinx.android.synthetic.main.ly_notas_cltes.view.*


class NotasClteAdapter(var notas: MutableList<NotasCltesEnt>, val context: Context,
                       private var listener: OnItemClickListener): RecyclerView.Adapter<NotasClteAdapter.ViewHolder>() {

    private var selectedPos: Int = RecyclerView.NO_POSITION


    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val item = notas[position]
        holder.bind(item)

        if (selectedPos == position) {
            holder.itemView.lyntcl_fecha.setTextColor(Color.BLACK)
            holder.itemView.lyntcl_fecha.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.lyntcl_nota.setTextColor(Color.BLACK)
            holder.itemView.lyntcl_nota.typeface = Typeface.DEFAULT_BOLD
        } else {
            holder.itemView.lyntcl_fecha.setTextColor(Color.GRAY)
            holder.itemView.lyntcl_fecha.typeface = Typeface.DEFAULT
            holder.itemView.lyntcl_nota.setTextColor(Color.GRAY)
            holder.itemView.lyntcl_nota.typeface = Typeface.DEFAULT
        }

        holder.itemView.setOnClickListener {
            selectedPos = position
            notifyItemChanged(position)
            listener.onClick(it, notas[position])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_notas_cltes, parent, false))
    }


    override fun getItemCount(): Int {
        return notas.size
    }


    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: NotasCltesEnt)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvFecha = itemView.findViewById(R.id.lyntcl_fecha) as TextView
        private val tvNota = itemView.findViewById(R.id.lyntcl_nota) as TextView

        fun bind(nota: NotasCltesEnt) {
            tvFecha.text = nota.fecha
            tvNota.text = nota.nota
        }

    }


}