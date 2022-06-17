package es.albainformatica.albamobileandroid.maestros

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.Comunicador.Companion.fConfiguracion
import es.albainformatica.albamobileandroid.DatosTrfArt
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.ancho_tarifa
import es.albainformatica.albamobileandroid.ponerCeros



class TrfArtRvAdapter(var tarifas: List<DatosTrfArt>, private val usarFormatos: Boolean, val fPorcIva: Double,
              val context: Context, var listener: OnItemClickListener): RecyclerView.Adapter<TrfArtRvAdapter.ViewHolder>() {

    private val fFtoPrBase = fConfiguracion.formatoDecPrecioBase()
    private val fFtoPrIva = fConfiguracion.formatoDecPrecioIva()


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = tarifas[fPosicion]
        holder.bind(item, usarFormatos, fPorcIva, fFtoPrBase, fFtoPrIva)

        holder.itemView.setOnClickListener {
            notifyDataSetChanged()
            listener.onClick(it, tarifas[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.layout_tarifas_artic, parent, false))
    }

    override fun getItemCount(): Int {
        return tarifas.size
    }


    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosTrfArt)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvTarifa = itemView.findViewById(R.id.ly_tarifa) as TextView
        private val tvDescrTrf = itemView.findViewById(R.id.ly_nombretrf) as TextView
        private val tvDescrFto = itemView.findViewById(R.id.ly_nombrefto) as TextView
        private val tvPrBase = itemView.findViewById(R.id.ly_prbase) as TextView
        private val tvPrIva = itemView.findViewById(R.id.ly_priva) as TextView
        private val tvDto = itemView.findViewById(R.id.ly_dto) as TextView

        fun bind(tarifa: DatosTrfArt, usarFormatos: Boolean, fPorcIva: Double, fFtoPrBase: String, fFtoPrIva: String) {
            tvTarifa.text = ponerCeros(tarifa.tarifaId.toString(), ancho_tarifa)
            tvDescrTrf.text = tarifa.descrTarifa
            if (usarFormatos) {
                tvDescrFto.visibility = View.VISIBLE
                tvDescrFto.text = tarifa.descrFto
            } else {
                tvDescrFto.visibility = View.GONE
            }
            val sPrecio = tarifa.precio.replace(',', '.')
            tvPrBase.text = String.format(fFtoPrBase, sPrecio.toDouble())

            var dPrecio = sPrecio.toDouble()
            val dImpIva = dPrecio *  fPorcIva / 100
            dPrecio += dImpIva
            tvPrIva.text = String.format(fFtoPrIva, dPrecio)

            tvDto.text = String.format("%.2f", tarifa.dto.toDouble())
        }
    }


}