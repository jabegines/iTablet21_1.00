package es.albainformatica.albamobileandroid.historicos

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.DatosDocsHcArtClte
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.tipoDocResumAsString
import kotlinx.android.synthetic.main.ly_hco_doc.view.*


class DocHcoDocRvAdapter(var docs: List<DatosDocsHcArtClte>, val context: Context, var listener: OnItemClickListener):
    RecyclerView.Adapter<DocHcoDocRvAdapter.ViewHolder>() {

    private var selectedPos: Int = RecyclerView.NO_POSITION
    var articuloId: Int = 0
    private var idHco: Int = 0


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = docs[fPosicion]
        holder.bind(item)

        if (selectedPos == fPosicion) {
            holder.itemView.tvHcoArtClTipoDoc.setTextColor(Color.BLACK)
            holder.itemView.tvHcoArtClSerieNum.setTextColor(Color.BLACK)
            holder.itemView.tvHcoArtClFecha.setTextColor(Color.BLACK)
        }
        else
        {
            holder.itemView.tvHcoArtClTipoDoc.setTextColor(Color.GRAY)
            holder.itemView.tvHcoArtClSerieNum.setTextColor(Color.GRAY)
            holder.itemView.tvHcoArtClFecha.setTextColor(Color.GRAY)
        }

        holder.itemView.setOnClickListener {
            // Tenemos en cuenta si el registro sobre el que pulsamos estaba anteriormente seleccionado
            // Deseleccionamos el registro
            if (selectedPos == fPosicion) {
                articuloId = 0
                idHco = 0
                selectedPos = RecyclerView.NO_POSITION
            }
            // Seleccionamos el registro
            else
            {
                //articuloId = docs[fPosicion].articuloId
                //idHco = docs[fPosicion].hcoPorcArticClteId
                //selectedPos = fPosicion
            }

            notifyDataSetChanged()
            listener.onClick(it, docs[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_hco_doc, parent, false))
    }

    override fun getItemCount(): Int {
        return docs.size
    }


    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosDocsHcArtClte)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvTipoDoc = itemView.findViewById(R.id.tvHcoArtClTipoDoc) as TextView
        private val tvSerieNum = itemView.findViewById(R.id.tvHcoArtClSerieNum) as TextView
        private val tvFecha = itemView.findViewById(R.id.tvHcoArtClFecha) as TextView
        private val tvVentas = itemView.findViewById(R.id.tvHcoArtClVentas) as TextView
        private val tvDevoluciones = itemView.findViewById(R.id.tvHcoArtClDevoluciones) as TextView


        @SuppressLint("SetTextI18n")
        fun bind(documento: DatosDocsHcArtClte) {
            tvTipoDoc.text = tipoDocResumAsString(documento.tipoDoc)
            tvSerieNum.text = documento.serie + " " + documento.numero
            tvFecha.text = documento.fecha
            tvVentas.text = documento.ventas
            tvDevoluciones.text = documento.devoluciones
        }
    }



}