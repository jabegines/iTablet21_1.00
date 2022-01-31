package es.albainformatica.albamobileandroid.cobros

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.DatosInfCobros
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.tipoDocResumAsString


class InfCobrosRvAdapter(
    private var lCobrosDivisas: MutableList<DatosInfCobros>, val context: Context,
    var listener: OnItemClickListener): RecyclerView.Adapter<InfCobrosRvAdapter.ViewHolder>() {


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = lCobrosDivisas[position]
            holder.bind(item)
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            setOnItemClickListener(listener)
            val layoutInflater = LayoutInflater.from(parent.context)
            return ViewHolder(layoutInflater.inflate(R.layout.ly_inf_cobros, parent, false))
        }


        override fun getItemCount(): Int {
            return lCobrosDivisas.size
        }

        private fun setOnItemClickListener(listener: OnItemClickListener) {
            this.listener = listener
        }

        interface OnItemClickListener {
            fun onClick(view: View, data: DatosInfCobros)
        }


        class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            private val nombreClte = itemView.findViewById(R.id.lyicCliente) as TextView
            private val fecha = itemView.findViewById(R.id.lyicFecha) as TextView
            private val tipoDoc = itemView.findViewById(R.id.lyicTipoDoc) as TextView
            private val serie = itemView.findViewById(R.id.lyicSerie) as TextView
            private val numero = itemView.findViewById(R.id.lyicNumero) as TextView
            private val impCobro = itemView.findViewById(R.id.lyicTotal) as TextView

            fun bind(cobro: DatosInfCobros) {
                nombreClte.text = cobro.nombre
                fecha.text = cobro.fechaCobro
                tipoDoc.text = tipoDocResumAsString(cobro.tipoDoc)
                serie.text = cobro.serie
                numero.text = cobro.numero
                impCobro.text = cobro.cobro

            }
        }


}