package es.albainformatica.albamobileandroid.cobros

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.Comunicador.Companion.fConfiguracion
import es.albainformatica.albamobileandroid.FLAGPENDIENTE_EN_CARTERA
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.entity.PendienteEnt
import es.albainformatica.albamobileandroid.tipoDocAsString
import org.w3c.dom.Text


class PdtesRvAdapter(private var lPendiente: List<PendienteEnt>, val context: Context,
    var listener: OnItemClickListener): RecyclerView.Adapter<PdtesRvAdapter.ViewHolder>() {

    private var selectedPos: Int = RecyclerView.NO_POSITION
    private var fFtoDecImpIva = fConfiguracion.formatoDecImptesIva()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = lPendiente[position]
        holder.bind(item, fFtoDecImpIva)

        holder.itemView.setOnClickListener {
            // Tenemos en cuenta si el registro sobre el que pulsamos estaba anteriormente seleccionado
            if (selectedPos == fPosicion) {
                selectedPos = RecyclerView.NO_POSITION
            }
            else //Seleccionamos el registro
            {
                selectedPos = fPosicion
            }

            notifyDataSetChanged()
            listener.onClick(it, lPendiente[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_ver_pendientes, parent, false))
    }


    override fun getItemCount(): Int {
        return lPendiente.size
    }

    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: PendienteEnt)
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tipoDoc = itemView.findViewById(R.id.lyvpTipoDoc) as TextView
        private val serie = itemView.findViewById(R.id.lyvpSerie) as TextView
        private val numero = itemView.findViewById(R.id.lyvpNumero) as TextView
        private val importe = itemView.findViewById(R.id.lyvpImporte) as TextView
        private val tvFlag = itemView.findViewById(R.id.lyvpFlag) as TextView

        fun bind(pendiente: PendienteEnt, fFtoDecImpIva: String) {
            tipoDoc.text = tipoDocAsString(pendiente.tipoDoc)
            if (pendiente.tipoDoc > 0) serie.text = pendiente.serie
            else serie.text = ""
            if (pendiente.tipoDoc != 33.toShort()) numero.text = pendiente.numero.toString()
            else numero.text = ""
            importe.text = String.format(fFtoDecImpIva ,pendiente.importe.replace(',', '.').toDouble())
            if (pendiente.flag and FLAGPENDIENTE_EN_CARTERA > 0) tvFlag.text = "*"
            else tvFlag.text = ""
        }
    }

}