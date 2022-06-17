package es.albainformatica.albamobileandroid.reparto

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import kotlinx.android.synthetic.main.ly_docs_reparto.view.*


class RepartoRvAdapter(var datosReparto: List<DatosReparto>, val context: Context,
                      var listener: OnItemClickListener): RecyclerView.Adapter<RepartoRvAdapter.ViewHolder>() {

    var selectedPos: Int = RecyclerView.NO_POSITION



    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = datosReparto[fPosicion]
        holder.bind(item)

        if (selectedPos == fPosicion) {
            holder.itemView.docrpt_codigo.setTextColor(Color.BLACK)
            holder.itemView.docrpt_codigo.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.docrpt_nombre.setTextColor(Color.BLACK)
            holder.itemView.docrpt_nombre.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.docrpt_tipodoc.setTextColor(Color.BLACK)
            holder.itemView.docrpt_tipodoc.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.docrpt_serienum.setTextColor(Color.BLACK)
            holder.itemView.docrpt_serienum.typeface = Typeface.DEFAULT_BOLD
        }
        else
        {
            holder.itemView.docrpt_codigo.setTextColor(Color.GRAY)
            holder.itemView.docrpt_codigo.typeface = Typeface.DEFAULT
            holder.itemView.docrpt_nombre.setTextColor(Color.GRAY)
            holder.itemView.docrpt_nombre.typeface = Typeface.DEFAULT
            holder.itemView.docrpt_tipodoc.setTextColor(Color.GRAY)
            holder.itemView.docrpt_tipodoc.typeface = Typeface.DEFAULT
            holder.itemView.docrpt_serienum.setTextColor(Color.GRAY)
            holder.itemView.docrpt_serienum.typeface = Typeface.DEFAULT
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
            listener.onClick(it, datosReparto[fPosicion])
        }
    }

    /*
    fun localizarClte(queClteId: Int) {
        for (datRut in datosReparto) {
            if (datRut.clienteId == queClteId)
                selectedPos = datosReparto.indexOf(datRut)
        }
    }
    */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_docs_reparto, parent, false))
    }


    override fun getItemCount(): Int {
        return datosReparto.size
    }


    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosReparto)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvCodigo = itemView.findViewById(R.id.docrpt_codigo) as TextView
        private val tvNombre = itemView.findViewById(R.id.docrpt_nombre) as TextView
        private val tvTipoDoc = itemView.findViewById(R.id.docrpt_tipodoc) as TextView
        private val tvSerieNum = itemView.findViewById(R.id.docrpt_serienum) as TextView
        private val imgTienePend = itemView.findViewById(R.id.imvTienePend) as ImageView
        private val imgDocFdo = itemView.findViewById(R.id.docrpt_DocFdo) as ImageView
        private val imgTieneIncid = itemView.findViewById(R.id.docrpt_DocIncid) as ImageView

        fun bind(reparto: DatosReparto) {
            tvCodigo.text = ponerCeros(reparto.codigo.toString(), ancho_codclte)
            if (Comunicador.fConfiguracion.aconsNomComercial())
                tvNombre.text = reparto.nombreComercial
            else
                tvNombre.text = reparto.nombre

            if (reparto.estado != "") {
                if (reparto.estado == "N") tvTipoDoc.text = itemView.context.getString(R.string.nuevo_doc)
                else tvTipoDoc.text = tipoDocResumAsString(reparto.tipoDoc)
            } else tvTipoDoc.text = ""

            tvSerieNum.text = reparto.serieNumero

            if (reparto.tienePend > 0) imgTienePend.visibility = View.VISIBLE
            else imgTienePend.visibility = View.GONE

            if (reparto.firmado == "T") imgDocFdo.visibility = View.VISIBLE
            else imgDocFdo.visibility = View.GONE

            if (reparto.tipoIncidencia > 0) imgTieneIncid.visibility = View.VISIBLE
            else imgTieneIncid.visibility = View.GONE
        }
    }


}