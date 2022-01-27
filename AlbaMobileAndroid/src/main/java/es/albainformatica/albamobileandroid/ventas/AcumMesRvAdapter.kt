package es.albainformatica.albamobileandroid.ventas

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.Comunicador.Companion.fConfiguracion
import es.albainformatica.albamobileandroid.DatosHistMesDif
import es.albainformatica.albamobileandroid.R


class AcumMesRvAdapter(
    private var lCobrosDivisas: List<DatosHistMesDif>, val context: Context,
    var listener: OnItemClickListener): RecyclerView.Adapter<AcumMesRvAdapter.ViewHolder>() {

    private var fFtoDecCantidad = fConfiguracion.formatoDecCantidad()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lCobrosDivisas[position]
        holder.bind(item, fFtoDecCantidad)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_acum_mes, parent, false))
    }


    override fun getItemCount(): Int {
        return lCobrosDivisas.size
    }

    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosHistMesDif)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvCodigo = itemView.findViewById(R.id.lyhcomesCodigo) as TextView
        private val tvDescr = itemView.findViewById(R.id.lyhcomesDescr) as TextView
        private val tvCantAnt = itemView.findViewById(R.id.lyhcomesCantAnt) as TextView
        private val tvCant = itemView.findViewById(R.id.lyhcomesCant) as TextView
        private val tvDif = itemView.findViewById(R.id.lyhcomesDiferencia) as TextView

        fun bind(hco: DatosHistMesDif, fFtoDecCantidad: String) {
            tvCodigo.text = hco.codigo
            tvDescr.text = hco.descripcion
            tvCantAnt.text = String.format(fFtoDecCantidad, hco.cantidadAnt.toDouble())
            tvCant.text = String.format(fFtoDecCantidad, hco.cantidad.toDouble())
            tvDif.text = String.format(fFtoDecCantidad, hco.diferencia.toDouble())
        }
    }


}