package es.albainformatica.albamobileandroid.ventas

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.Comunicador.Companion.fConfiguracion
import es.albainformatica.albamobileandroid.DatosRutero
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.ancho_codclte
import es.albainformatica.albamobileandroid.ponerCeros
import kotlinx.android.synthetic.main.ly_cltes_rutero.view.*


class RuteroRvAdapter(var datosRutero: List<DatosRutero>, private val fUsarRutero: Boolean, val context: Context,
                      var listener: OnItemClickListener): RecyclerView.Adapter<RuteroRvAdapter.ViewHolder>() {

    var selectedPos: Int = RecyclerView.NO_POSITION


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = datosRutero[fPosicion]
        holder.bind(item, fUsarRutero)


        if (selectedPos == fPosicion) {
            holder.itemView.cltrt_orden.setTextColor(Color.BLACK)
            holder.itemView.cltrt_orden.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.cltrt_codigo.setTextColor(Color.BLACK)
            holder.itemView.cltrt_codigo.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.cltrt_nombre.setTextColor(Color.BLACK)
            holder.itemView.cltrt_nombre.typeface = Typeface.DEFAULT_BOLD
        }
        else
        {
            holder.itemView.cltrt_orden.setTextColor(Color.GRAY)
            holder.itemView.cltrt_orden.typeface = Typeface.DEFAULT
            holder.itemView.cltrt_codigo.setTextColor(Color.GRAY)
            holder.itemView.cltrt_codigo.typeface = Typeface.DEFAULT
            holder.itemView.cltrt_nombre.setTextColor(Color.GRAY)
            holder.itemView.cltrt_nombre.typeface = Typeface.DEFAULT
        }

        holder.itemView.setOnClickListener {
            // Tenemos en cuenta si el registro sobre el que pulsamos estaba anteriormente seleccionado
            selectedPos = if (selectedPos == fPosicion) { //Deseleccionamos el registro
                RecyclerView.NO_POSITION
            } else //Seleccionamos el registro
            {
                fPosicion
            }

            notifyDataSetChanged()
            listener.onClick(it, datosRutero[fPosicion])
        }
    }


    fun localizarClte(queClteId: Int) {

        for (datRut in datosRutero) {
            if (datRut.clienteId == queClteId)
                selectedPos = datosRutero.indexOf(datRut)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_cltes_rutero, parent, false))
    }

    override fun getItemCount(): Int {
        return datosRutero.size
    }


    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosRutero)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvOrden = itemView.findViewById(R.id.cltrt_orden) as TextView
        private val tvCodigo = itemView.findViewById(R.id.cltrt_codigo) as TextView
        private val tvNombre = itemView.findViewById(R.id.cltrt_nombre) as TextView
        private val imgTieneIncid = itemView.findViewById(R.id.imvTieneInc) as ImageView

        fun bind(rutero: DatosRutero, fUsarRutero: Boolean) {
            if (fUsarRutero) tvOrden.visibility = View.VISIBLE
            else tvOrden.visibility = View.GONE

            tvOrden.text = rutero.orden.toString()
            tvCodigo.text = ponerCeros(rutero.codigo.toString(), ancho_codclte)
            if (fConfiguracion.aconsNomComercial())
                tvNombre.text = rutero.nombreComercial
            else
                tvNombre.text = rutero.nombre

            if (rutero.tieneIncid == "T")
                imgTieneIncid.visibility = View.VISIBLE
            else
                imgTieneIncid.visibility = View.GONE
        }
    }


}