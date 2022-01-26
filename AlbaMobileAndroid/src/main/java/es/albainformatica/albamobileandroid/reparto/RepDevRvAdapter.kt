package es.albainformatica.albamobileandroid.reparto

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.DatosHistorico
import es.albainformatica.albamobileandroid.R


class RepDevRvAdapter(var lineas: List<DatosHistorico>, val context: Context,
                      private var listener: OnItemClickListener): RecyclerView.Adapter<RepDevRvAdapter.ViewHolder>() {

    var selectedPos: Int = RecyclerView.NO_POSITION
    //private val fDecPrBase = Comunicador.fConfiguracion.decimalesImpBase()


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = lineas[fPosicion]
        holder.bind(item)

        /*
        if (selectedPos == fPosicion) {
            holder.itemView.ly_vl_descr.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.ly_vl_impte.typeface = Typeface.DEFAULT_BOLD
        }
        else {
            holder.itemView.ly_vl_descr.typeface = Typeface.DEFAULT
            holder.itemView.ly_vl_impte.typeface = Typeface.DEFAULT
        }
        */

        holder.itemView.setOnClickListener {
            selectedPos = if (selectedPos == fPosicion) {
                RecyclerView.NO_POSITION
            } else {
                fPosicion
            }

            notifyDataSetChanged()
            listener.onClick(it, lineas[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_dev_reparto, parent, false))
    }


    override fun getItemCount(): Int {
        return lineas.size
    }


    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosHistorico)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvCodigo = itemView.findViewById(R.id.lyDevRepCodigo) as TextView
        private val tvDescr = itemView.findViewById(R.id.lyDevRepDescr) as TextView
        private val tvCantidad = itemView.findViewById(R.id.lyDevRepCant) as TextView
        private val tvCajas = itemView.findViewById(R.id.lyDevRepCajas) as TextView
        private val tvFecha = itemView.findViewById(R.id.lyhcoFecha) as TextView
        private val tvCantPedida = itemView.findViewById(R.id.lyDevRepCantPedida) as TextView

        fun bind(linea: DatosHistorico) {
            tvCodigo.text = linea.codigo
            tvDescr.text = linea.descripcion
            tvCantidad.text = linea.cantidad
            tvCajas.text = linea.cajas
            tvFecha.text = linea.fecha
            tvCantPedida.text = linea.cantPedida
        }
    }


}