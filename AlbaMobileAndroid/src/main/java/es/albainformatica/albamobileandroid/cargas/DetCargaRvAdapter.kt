package es.albainformatica.albamobileandroid.cargas

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.DatosDetCarga
import es.albainformatica.albamobileandroid.R
import kotlinx.android.synthetic.main.ly_ver_carga.view.*


class DetCargaRvAdapter(private var lDetalle: List<DatosDetCarga>, val context: Context, var listener: OnItemClickListener):
    RecyclerView.Adapter<DetCargaRvAdapter.ViewHolder>() {

    private var selectedPos: Int = RecyclerView.NO_POSITION
    //var cargaId: Int = 0


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = lDetalle[fPosicion]
        holder.bind(item)


        if (selectedPos == fPosicion) {
            holder.itemView.tvLNCCodigo.setTextColor(Color.BLACK)
            holder.itemView.tvLNCCodigo.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.tvLNCDescr.setTextColor(Color.BLACK)
            holder.itemView.tvLNCDescr.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.tvLNCLote.setTextColor(Color.BLACK)
            holder.itemView.tvLNCLote.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.tvLNCCajas.setTextColor(Color.BLACK)
            holder.itemView.tvLNCCajas.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.tvLNCCantidad.setTextColor(Color.BLACK)
            holder.itemView.tvLNCCantidad.typeface = Typeface.DEFAULT_BOLD
        }
        else
        {
            holder.itemView.tvLNCCodigo.setTextColor(Color.GRAY)
            holder.itemView.tvLNCCodigo.typeface = Typeface.DEFAULT
            holder.itemView.tvLNCDescr.setTextColor(Color.GRAY)
            holder.itemView.tvLNCDescr.typeface = Typeface.DEFAULT
            holder.itemView.tvLNCLote.setTextColor(Color.GRAY)
            holder.itemView.tvLNCLote.typeface = Typeface.DEFAULT
            holder.itemView.tvLNCCajas.setTextColor(Color.GRAY)
            holder.itemView.tvLNCCajas.typeface = Typeface.DEFAULT
            holder.itemView.tvLNCCantidad.setTextColor(Color.GRAY)
            holder.itemView.tvLNCCantidad.typeface = Typeface.DEFAULT
        }

        holder.itemView.setOnClickListener {
            // Tenemos en cuenta si el registro sobre el que pulsamos estaba anteriormente seleccionado
            if (selectedPos == fPosicion) { //Deseleccionamos el registro
                //cargaId = 0
                selectedPos = RecyclerView.NO_POSITION
            }
            else //Seleccionamos el registro
            {
                //cargaId = cargas[fPosicion].cargaId
                selectedPos = fPosicion
            }

            notifyDataSetChanged()
            listener.onClick(it, lDetalle[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_ver_carga, parent, false))
    }

    override fun getItemCount(): Int {
        return lDetalle.size
    }


    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosDetCarga)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvCodigo = itemView.findViewById(R.id.tvLNCCodigo) as TextView
        private val tvDescr = itemView.findViewById(R.id.tvLNCDescr) as TextView
        private val tvLote = itemView.findViewById(R.id.tvLNCLote) as TextView
        private val tvCajas = itemView.findViewById(R.id.tvLNCCajas) as TextView
        private val tvCantidad = itemView.findViewById(R.id.tvLNCCantidad) as TextView

        fun bind(detalle: DatosDetCarga) {
            tvCodigo.text = detalle.codigo
            tvDescr.text = detalle.descripcion
            tvLote.text = detalle.lote
            tvCajas.text = detalle.cajas
            tvCantidad.text = detalle.cantidad
        }
    }


}