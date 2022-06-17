package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.entity.DireccCltesEnt



class DirCltesRvAdapter(var direcciones: List<DireccCltesEnt>, val context: Context, var listener: OnItemClickListener):
    RecyclerView.Adapter<DirCltesRvAdapter.ViewHolder>() {

    var selectedPos: Int = RecyclerView.NO_POSITION


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = direcciones[fPosicion]
        holder.bind(item)


        //if (selectedPos == fPosicion) {
            //holder.itemView.tvCargaId.setTextColor(Color.BLACK)
            //holder.itemView.tvCargaId.typeface = Typeface.DEFAULT_BOLD
            //holder.itemView.tvEmpresaCarga.setTextColor(Color.BLACK)
            //holder.itemView.tvEmpresaCarga.typeface = Typeface.DEFAULT_BOLD
        //}
        //else
        //{
            //holder.itemView.tvCargaId.setTextColor(Color.GRAY)
            //holder.itemView.tvCargaId.typeface = Typeface.DEFAULT
            //holder.itemView.tvEmpresaCarga.setTextColor(Color.GRAY)
            //holder.itemView.tvEmpresaCarga.typeface = Typeface.DEFAULT
        //}

        holder.itemView.setOnClickListener {
            // Tenemos en cuenta si el registro sobre el que pulsamos estaba anteriormente seleccionado
            selectedPos = if (selectedPos == fPosicion) { //Deseleccionamos el registro
                RecyclerView.NO_POSITION
            } else //Seleccionamos el registro
            {
                fPosicion
            }

            notifyDataSetChanged()
            listener.onClick(it, direcciones[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.layout_direcc_cltes, parent, false))
    }


    override fun getItemCount(): Int {
        return direcciones.size
    }


    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DireccCltesEnt)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvDireccion = itemView.findViewById(R.id.ly_direcc) as TextView
        private val tvPoblacion = itemView.findViewById(R.id.ly_poblac) as TextView

        fun bind(direccion: DireccCltesEnt) {
            tvDireccion.text = direccion.direccion
            tvPoblacion.text = direccion.localidad
        }
    }


}