package es.albainformatica.albamobileandroid.historicos

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.DatosHcoCompSemMes
import es.albainformatica.albamobileandroid.R


class AcumComSemMesRvAdapter(var lineas: List<DatosHcoCompSemMes>, val context: Context,
                             private var listener: OnItemClickListener): RecyclerView.Adapter<AcumComSemMesRvAdapter.ViewHolder>() {

    private val fFtoDecCantidad = Comunicador.fConfiguracion.formatoDecCantidad()


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lineas[position]
        holder.bind(item, fFtoDecCantidad)

        holder.itemView.setOnClickListener {
            //selectedPos = position
            notifyItemChanged(position)
            listener.onClick(it, lineas[position])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_hco_com_sem_mes, parent, false))
    }


    override fun getItemCount(): Int {
        return lineas.size
    }


    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosHcoCompSemMes)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvCodigo = itemView.findViewById(R.id.lyhcoSemMesCodigo) as TextView
        private val tvDescr = itemView.findViewById(R.id.lyhcoSemMesDescr) as TextView
        private val tvCant1 = itemView.findViewById(R.id.lyhcoSemMesCant1) as TextView
        private val tvCant2 = itemView.findViewById(R.id.lyhcoSemMesCant2) as TextView

        fun bind(datHco: DatosHcoCompSemMes, fFtoDecCantidad: String) {
            tvCodigo.text = datHco.codigo
            tvDescr.text = datHco.descripcion
            var queSuma1 = "0.0"
            if (datHco.suma1 != "") queSuma1 = datHco.suma1.replace(',', '.')
            val dSuma1 = queSuma1.toDouble()
            tvCant1.text = String.format(fFtoDecCantidad, dSuma1)

            var queSuma2 = "0.0"
            if (datHco.suma2 != "") queSuma2 = datHco.suma2.replace(',', '.')
            val dSuma2 = queSuma2.toDouble()
            tvCant2.text = String.format(fFtoDecCantidad, dSuma2)
        }

    }


}