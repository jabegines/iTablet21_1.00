package es.albainformatica.albamobileandroid.reparto

import android.content.Context
import android.graphics.BitmapFactory
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
import es.albainformatica.albamobileandroid.R
import kotlinx.android.synthetic.main.ly_docs_reparto.view.*
import kotlinx.android.synthetic.main.ly_docs_reparto.view.docrpt_codigo
import kotlinx.android.synthetic.main.ly_lineas_ventas.view.*


class LinRepVtasRvAdapter(var lineas: List<DatosLinVtas>, val context: Context,
                          private var listener: OnItemClickListener): RecyclerView.Adapter<LinRepVtasRvAdapter.ViewHolder>() {

    var selectedPos: Int = RecyclerView.NO_POSITION
    private val fDecPrBase = Comunicador.fConfiguracion.decimalesImpBase()


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = lineas[fPosicion]
        holder.bind(item, fDecPrBase)

        if (selectedPos == fPosicion) {
            holder.itemView.ly_vl_descr.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.ly_vl_impte.typeface = Typeface.DEFAULT_BOLD
        }
        else
        {
            holder.itemView.ly_vl_descr.typeface = Typeface.DEFAULT
            holder.itemView.ly_vl_impte.typeface = Typeface.DEFAULT
        }

        holder.itemView.setOnClickListener {
            selectedPos = if (selectedPos == fPosicion) {
                RecyclerView.NO_POSITION
            } else {
                fPosicion
            }

            notifyDataSetChanged()
            listener.onClick(it, lineas[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_lineas_ventas, parent, false))
    }


    override fun getItemCount(): Int {
        return lineas.size
    }


    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosLinVtas)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val fRutaImagenes = Comunicador.fConfiguracion.rutaLocalComunicacion() + "/imagenes"

        private val imgArt = itemView.findViewById(R.id.imvArtLinea) as ImageView
        private val tvDescr = itemView.findViewById(R.id.ly_vl_descr) as TextView
        private val tvImporte = itemView.findViewById(R.id.ly_vl_impte) as TextView
        private val tvDescrFto = itemView.findViewById(R.id.ly_vl_descrfto) as TextView
        private val tvCodArt = itemView.findViewById(R.id.ly_vl_codart) as TextView
        private val tvTarifa = itemView.findViewById(R.id.ly_vl_tarifa) as TextView
        private val tvPiezas = itemView.findViewById(R.id.ly_vl_piezas) as TextView
        private val tvCajas = itemView.findViewById(R.id.ly_vl_cajas) as TextView
        private val tvCantidad = itemView.findViewById(R.id.ly_vl_cant) as TextView
        private val tvTasa1 = itemView.findViewById(R.id.ly_vl_tasa1) as TextView
        private val tvPrecio = itemView.findViewById(R.id.ly_vl_precio) as TextView
        private val tvDto = itemView.findViewById(R.id.ly_vl_dto) as TextView
        private val tvTasa2 = itemView.findViewById(R.id.ly_vl_tasa2) as TextView

        fun bind(linea: DatosLinVtas, fDecPrBase: Int) {
            val queFichero = "$fRutaImagenes/ART_" + linea.articuloId + ".jpg"
            val bitmap = BitmapFactory.decodeFile(queFichero)
            imgArt.setImageBitmap(bitmap)

            tvDescr.text = linea.descripcion
            tvImporte.text = String.format("%." + fDecPrBase + "f", linea.importe.toDouble())
            tvDescrFto.text = linea.descrFto
            tvCodArt.text = linea.codArticulo
            //tvTarifa.text = linea.
            //tvPiezas.text = linea.
            tvCajas.text = linea.cajas
            tvCantidad.text = linea.cantidad
            if (linea.tasa1 != "") tvTasa1.text = String.format("%." + fDecPrBase + "f", linea.tasa1.toDouble())
            else tvTasa1.text = ""
            tvPrecio.text = String.format("%." + fDecPrBase + "f", linea.precio.toDouble())
            tvDto.text = String.format("%.2f", linea.dto.toDouble())
            if (linea.tasa2 != "") tvTasa2.text = String.format("%." + fDecPrBase + "f", linea.tasa2.toDouble())
            else tvTasa2.text = ""
        }

    }


}