package es.albainformatica.albamobileandroid.historicos

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.Comunicador.Companion.fConfiguracion
import es.albainformatica.albamobileandroid.DatosHistorico
import es.albainformatica.albamobileandroid.R
import kotlinx.android.synthetic.main.ly_hco_cliente.view.*


class HcoRvAdapter(private var lHistorico: List<DatosHistorico>, ivaIncluido: Boolean, aplicarIva: Boolean,
    val context: Context, var listener: OnItemClickListener):
                    RecyclerView.Adapter<HcoRvAdapter.ViewHolder>() {

    var selectedPos: Int = RecyclerView.NO_POSITION

    private var ftoDecCantidad = fConfiguracion.formatoDecCantidad()
    private var ftoDecPrBase = fConfiguracion.formatoDecPrecioBase()
    private var ftoDecPrIva = fConfiguracion.formatoDecPrecioIva()
    private var fIvaIncluido = ivaIncluido
    private var fAplicarIva = aplicarIva

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = lHistorico[fPosicion]
        holder.bind(item, ftoDecCantidad, ftoDecPrBase, ftoDecPrIva, fIvaIncluido, fAplicarIva)


        if (selectedPos == fPosicion) {
            holder.itemView.lyhcoCodigo.setTextColor(Color.BLACK)
            holder.itemView.lyhcoCodigo.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.lyhcoDescr.setTextColor(Color.BLACK)
            holder.itemView.lyhcoDescr.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.lyhcoStock.setTextColor(Color.BLACK)
            holder.itemView.lyhcoStock.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.lyhcoFormato.setTextColor(Color.BLACK)
            holder.itemView.lyhcoFormato.typeface = Typeface.DEFAULT_BOLD

        }
        else
        {
            holder.itemView.lyhcoCodigo.setTextColor(Color.GRAY)
            holder.itemView.lyhcoCodigo.typeface = Typeface.DEFAULT
            holder.itemView.lyhcoDescr.setTextColor(Color.GRAY)
            holder.itemView.lyhcoDescr.typeface = Typeface.DEFAULT
            holder.itemView.lyhcoStock.setTextColor(Color.GRAY)
            holder.itemView.lyhcoStock.typeface = Typeface.DEFAULT
            holder.itemView.lyhcoFormato.setTextColor(Color.GRAY)
            holder.itemView.lyhcoFormato.typeface = Typeface.DEFAULT

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
            listener.onClick(it, lHistorico[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_hco_cliente, parent, false))
    }

    override fun getItemCount(): Int {
        return lHistorico.size
    }


    fun getItemPosition(queHcoId: Int): Int {
        for (i in lHistorico.indices) {
            if (lHistorico[i].historicoId == queHcoId) {
                return i
            }
        }
        return -1
    }


    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: DatosHistorico)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvCodigo = itemView.findViewById(R.id.lyhcoCodigo) as TextView
        private val tvDescr = itemView.findViewById(R.id.lyhcoDescr) as TextView
        private val tvStock = itemView.findViewById(R.id.lyhcoStock) as TextView
        private val tvFormato = itemView.findViewById(R.id.lyhcoFormato) as TextView
        private val tvArtHab = itemView.findViewById(R.id.lyhcoTxtArtHab) as TextView
        private val tvPiezPed = itemView.findViewById(R.id.lyhcoPiezPedida) as TextView
        private val tvCantPed = itemView.findViewById(R.id.lyhcoCantPedida) as TextView
        private val tvCantidad = itemView.findViewById(R.id.lyhcoCant) as TextView
        private val tvPrecio = itemView.findViewById(R.id.lyhcoPrecio) as TextView
        private val tvDto = itemView.findViewById(R.id.lyhcoDto) as TextView
        private val tvPrNeto = itemView.findViewById(R.id.lyhcoPrNeto) as TextView
        private val tvCajas = itemView.findViewById(R.id.lyhcoCajas) as TextView
        private val tvFecha = itemView.findViewById(R.id.lyhcoFecha) as TextView

        fun bind(hco: DatosHistorico, ftoDecCantidad: String, ftoDecPrBase: String, ftoDecPrIva: String,
                 fIvaIncluido: Boolean, fAplicarIva: Boolean) {

            tvCodigo.text = hco.codigo
            tvDescr.text = hco.descripcion
            if (hco.stock != null)
                tvStock.text = String.format(ftoDecCantidad, hco.stock?.toDouble())
            else tvStock.text = ""

            if (hco.formatoId > 0) {
                tvFormato.visibility = View.VISIBLE
                tvFormato.text = hco.descrFto
            } else tvFormato.visibility = View.GONE

            tvArtHab.text = hco.texto
            tvPiezPed.text = if (hco.piezPedida != null) String.format(ftoDecCantidad, hco.piezPedida?.toDouble()) else ""
            tvCantPed.text = if (hco.cantPedida != null) String.format(ftoDecCantidad, hco.cantPedida?.toDouble()) else ""
            tvCantidad.text = String.format(ftoDecCantidad, hco.cantidad.toDouble())

            val sPrecio = hco.precio.replace(',', '.')
            var dPrecio = sPrecio.toDouble()
            val sDto = hco.dto.replace(',', '.')
            val dDto = sDto.toDouble()

            if (fIvaIncluido && fAplicarIva) {
                // No tenemos un campo para el precio iva inclu??do, por eso lo calculamos.
                val sPorcIva = hco.porcIva.replace(',', '.')
                val dPorcIva = sPorcIva.toDouble()
                dPrecio += dPrecio * dPorcIva / 100
                tvPrecio.text = String.format(ftoDecPrIva, dPrecio)

                val dPrNeto = dPrecio - (dPrecio * dDto / 100)
                tvPrNeto.text = String.format(ftoDecPrIva, dPrNeto)

            } else {

                tvPrecio.text = String.format(ftoDecPrBase, dPrecio)

                val dPrNeto = dPrecio - (dPrecio * dDto / 100)
                tvPrNeto.text = String.format(ftoDecPrBase, dPrNeto)
            }

            tvDto.text = String.format("%.2f", hco.dto.toDouble())

            tvCajas.text = String.format(ftoDecCantidad, hco.cajas.toDouble())
            tvFecha.text = hco.fecha
        }
    }


}