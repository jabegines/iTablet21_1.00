package es.albainformatica.albamobileandroid.ventas

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.Comunicador.Companion.fConfiguracion
import java.util.*


class PrecEspRvAdapter(private var lPrecios: List<ListaPreciosEsp>, private val fIvaIncl: Boolean, val context: Context,
                       var listener: OnItemClickListener): RecyclerView.Adapter<PrecEspRvAdapter.ViewHolder>() {

    private val fFtoDecPrII = fConfiguracion.formatoDecPrecioIva()
    private val fDecPrII = fConfiguracion.decimalesPrecioIva()
    private val fFtoDecPrBase = fConfiguracion.formatoDecPrecioBase()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lPrecios[position]
        holder.bind(item, fIvaIncl, fFtoDecPrBase, fFtoDecPrII, fDecPrII)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_prec_especiales, parent, false))
    }


    override fun getItemCount(): Int {
        return lPrecios.size
    }

    private fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onClick(view: View, data: ListaPreciosEsp)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvDescr = itemView.findViewById(R.id.pr_esp_descrArt) as TextView
        private val tvDescrFto = itemView.findViewById(R.id.pr_esp_descrFto) as TextView
        private val tvPrecio = itemView.findViewById(R.id.pr_esp_tvPrecio) as TextView
        private val tvDto = itemView.findViewById(R.id.pr_esp_tvDto) as TextView
        private val tvPrNeto = itemView.findViewById(R.id.pr_esp_tvPrNeto) as TextView
        private val tvPrTarifa = itemView.findViewById(R.id.pr_esp_tvPrTrf) as TextView
        private val tvDtoTrf = itemView.findViewById(R.id.pr_esp_tvDtTrf) as TextView

        fun bind(precEsp: ListaPreciosEsp, fIvaIncl: Boolean, fFtoDecPrBase: String, fFtoDecPrII: String, fDecPrII: Int) {
            tvDescr.text = precEsp.descripcion
            tvDescrFto.text = precEsp.descrFto

            var sPrecio = precEsp.precio.replace(',', '.')
            var sDto = precEsp.dto.replace(',', '.')
            val sPorcIva = precEsp.porcIva.replace(',', '.')

            if (fIvaIncl) {
                var dPrecioII = sPrecio.toDouble() + ((sPrecio.toDouble() * sPorcIva.toDouble()) / 100)
                dPrecioII = Redondear(dPrecioII, fDecPrII)
                tvPrecio.text = String.format(fFtoDecPrII, dPrecioII)
            } else {
                tvPrecio.text = String.format(fFtoDecPrBase, sPrecio.toDouble())
            }

            tvDto.text = sDto

            // Puede ser que el precio del rating sea 0 y sólo tengamos un descuento. En este caso el descuento
            // del rating se aplicaría sobre el precio de la tarifa.
            // Si en el rating no viene precio lo tomamos de la tarifa.
            if (sPrecio.toDouble() == 0.0) {
                sPrecio = if (precEsp.prTarifa != "") precEsp.prTarifa.replace(',', '.')
                else precEsp.prTrfFto.replace(',', '.')
            }

            if (precEsp.flag and FLAGRATING_DESCUENTOIMPORTE > 0) {
                val dPrecio = sPrecio.toDouble() - sDto.toDouble()

                if (fIvaIncl) {
                    var dPrecioII = dPrecio + ((dPrecio * sPorcIva.toDouble()) / 100)
                    dPrecioII = Redondear(dPrecioII, fDecPrII)

                    tvPrNeto.text = String.format(fFtoDecPrII, dPrecioII)
                } else {
                    tvPrNeto.text = String.format(fFtoDecPrBase, dPrecio)
                }
            } else {
                var dPrecio = sPrecio.toDouble()
                dPrecio -= (dPrecio * sDto.toDouble()) / 100

                if (fIvaIncl) {
                    var dPrecioII = dPrecio + ((dPrecio * sPorcIva.toDouble()) / 100)
                    dPrecioII = Redondear(dPrecioII, fDecPrII)

                    tvPrNeto.text = String.format(fFtoDecPrII, dPrecioII)
                } else {
                    tvPrNeto.text = String.format(fFtoDecPrBase, dPrecio)
                }
            }

            // Si el artículo no tiene formato el precio será el de la tarifa.
            sPrecio = if (precEsp.prTarifa != "") precEsp.prTarifa.replace(',', '.')
            else precEsp.prTrfFto.replace(',', '.')
            if (sPrecio != "") {
                val dPrecio = sPrecio.toDouble()
                if (fIvaIncl) {
                    var dPrecioII = dPrecio + ((dPrecio * sPorcIva.toDouble()) / 100)
                    dPrecioII = Redondear(dPrecioII, fDecPrII)

                    tvPrTarifa.text = String.format(fFtoDecPrII, dPrecioII)
                } else {
                    tvPrTarifa.text = String.format(fFtoDecPrBase, dPrecio)
                }
            }

            sDto = if (precEsp.dtoTarifa != "") precEsp.dtoTarifa.replace(',', '.')
            else precEsp.dtoTrfFto.replace(',', '.')

            if (sDto != "") {
                tvDtoTrf.text = String.format(Locale.getDefault(), "%.2f", sDto.toDouble())
            }

        }
    }


}