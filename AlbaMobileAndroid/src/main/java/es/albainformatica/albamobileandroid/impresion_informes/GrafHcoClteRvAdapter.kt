package es.albainformatica.albamobileandroid.impresion_informes

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.Comunicador.Companion.fConfiguracion
import es.albainformatica.albamobileandroid.DatosHistMesClte
import es.albainformatica.albamobileandroid.R


class GrafHcoClteRvAdapter(private var lHco: List<DatosHistMesClte>, val context: Context,
                           var listener: OnItemClickListener): RecyclerView.Adapter<GrafHcoClteRvAdapter.ViewHolder>() {

    private var selectedPos: Int = RecyclerView.NO_POSITION
    private var fFtoDecCant = fConfiguracion.formatoDecCantidad()
    private var fFtoDecImp = fConfiguracion.formatoDecImptesBase()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = lHco[fPosicion]
        holder.bind(item, fFtoDecCant, fFtoDecImp)

        holder.itemView.setOnClickListener {
            // Tenemos en cuenta si el registro sobre el que pulsamos estaba anteriormente seleccionado
            selectedPos = if (selectedPos == fPosicion) {
                RecyclerView.NO_POSITION
            } else
            {
                fPosicion
            }

            notifyDataSetChanged()
            listener.onClick(it, lHco[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_graf_hco_clte, parent, false))
    }


    override fun getItemCount(): Int {
        return lHco.size
    }

    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosHistMesClte)
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCodigo = itemView.findViewById(R.id.lygrafhcoCodigo) as TextView
        private val tvDescr = itemView.findViewById(R.id.lygrafhcoDescr) as TextView
        private val tvCantAnt = itemView.findViewById(R.id.lygrafhcoCantAnt) as TextView
        private val tvCant = itemView.findViewById(R.id.lygrafhcoCant) as TextView
        private val tvImpAnt = itemView.findViewById(R.id.lygrafhcoImpteAnt) as TextView
        private val tvImpte = itemView.findViewById(R.id.lygrafhcoImpte) as TextView

        fun bind(hco: DatosHistMesClte, fFtoDecCant: String, fFtoDecImp: String) {
            tvCodigo.text = hco.codigo
            tvDescr.text = hco.descripcion
            tvCantAnt.text = String.format(fFtoDecCant, hco.sumCantAnt.toDouble())
            tvCant.text = String.format(fFtoDecCant, hco.sumCant.toDouble())
            tvImpAnt.text = String.format(fFtoDecImp, hco.sumImpteAnt.toDouble())
            tvImpte.text = String.format(fFtoDecImp, hco.sumImpte.toDouble())
        }
    }

}