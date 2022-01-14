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
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.DatosLinVtas
import es.albainformatica.albamobileandroid.DatosVerDocs
import es.albainformatica.albamobileandroid.R
import kotlinx.android.synthetic.main.ly_ver_cobros.view.*
import kotlinx.coroutines.selects.select


class VerDocsRvAdapter(var documentos: List<DatosVerDocs>, val context: Context,
                       private var listener: OnItemClickListener): RecyclerView.Adapter<VerDocsRvAdapter.ViewHolder>() {

    private var selectedPos: Int = RecyclerView.NO_POSITION
    private val fDecPrBase = Comunicador.fConfiguracion.decimalesImpBase()


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = documentos[fPosicion]
        holder.bind(item, fDecPrBase)

        if (selectedPos == fPosicion) {
            holder.itemView.lyvdFecha.setTextColor(Color.BLACK)
            holder.itemView.lyvdFecha.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.lyvdTipoDoc.setTextColor(Color.BLACK)
            holder.itemView.lyvdTipoDoc.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.lyvdSerie.setTextColor(Color.BLACK)
            holder.itemView.lyvdSerie.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.lyvdNumero.setTextColor(Color.BLACK)
            holder.itemView.lyvdNumero.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.lyvdTotal.setTextColor(Color.BLACK)
            holder.itemView.lyvdTotal.typeface = Typeface.DEFAULT_BOLD
        } else {
            holder.itemView.lyvdFecha.setTextColor(Color.GRAY)
            holder.itemView.lyvdFecha.typeface = Typeface.DEFAULT
            holder.itemView.lyvdTipoDoc.setTextColor(Color.GRAY)
            holder.itemView.lyvdTipoDoc.typeface = Typeface.DEFAULT
            holder.itemView.lyvdSerie.setTextColor(Color.GRAY)
            holder.itemView.lyvdSerie.typeface = Typeface.DEFAULT
            holder.itemView.lyvdNumero.setTextColor(Color.GRAY)
            holder.itemView.lyvdNumero.typeface = Typeface.DEFAULT
            holder.itemView.lyvdTotal.setTextColor(Color.GRAY)
            holder.itemView.lyvdTotal.typeface = Typeface.DEFAULT
        }

        holder.itemView.setOnClickListener {
            // Tenemos en cuenta si el registro sobre el que pulsamos estaba anteriormente seleccionado
            // Deseleccionamos el registro
            if (selectedPos == fPosicion) {
                selectedPos = RecyclerView.NO_POSITION
            }
            // Seleccionamos el registro
            else {
                selectedPos = fPosicion
            }

            notifyDataSetChanged()
            listener.onClick(it, documentos[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_ver_documentos, parent, false))
    }


    override fun getItemCount(): Int {
        return documentos.size
    }


    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosVerDocs)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvFecha = itemView.findViewById(R.id.lyvdFecha) as TextView
        private val tvTipoDoc = itemView.findViewById(R.id.lyvdTipoDoc) as TextView
        private val tvSerie = itemView.findViewById(R.id.lyvdSerie) as TextView
        private val tvNumero = itemView.findViewById(R.id.lyvdNumero) as TextView
        private val tvTotal = itemView.findViewById(R.id.lyvdTotal) as TextView
        private val imgFirmado = itemView.findViewById(R.id.lyvdDocFdo) as ImageView
        private val imgIncid = itemView.findViewById(R.id.lyvdDocIncid) as ImageView

        fun bind(documento: DatosVerDocs, fDecPrBase: Int) {
            tvFecha.text = documento.fecha
            tvTipoDoc.text = documento.tipoDoc.toString()
            tvSerie.text = documento.serie
            tvNumero.text = documento.numero.toString()
            tvTotal.text = String.format("%." + fDecPrBase + "f", documento.total.toDouble())

            if (documento.firmado == "T") imgFirmado.visibility = View.VISIBLE
            else imgFirmado.visibility = View.GONE

            if (documento.tipoIncidencia > 0) imgIncid.visibility = View.VISIBLE
            else imgIncid.visibility = View.GONE
        }

    }


}