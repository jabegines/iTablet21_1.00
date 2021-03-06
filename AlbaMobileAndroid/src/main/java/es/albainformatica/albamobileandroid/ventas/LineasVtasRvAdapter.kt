package es.albainformatica.albamobileandroid.ventas

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.Comunicador.Companion.fConfiguracion
import kotlinx.android.synthetic.main.ly_lineas_ventas.view.*
import java.util.*


class LineasVtasRvAdapter(var lineas: List<DatosLinVtas>, val fIvaIncluido: Boolean, val fAplicarIva: Boolean,
                          val activity: Activity, private var listener: OnItemClickListener):
    RecyclerView.Adapter<LineasVtasRvAdapter.ViewHolder>() {

    private val fDecPrBase = fConfiguracion.decimalesPrecioBase()
    private val fDecPrIva = fConfiguracion.decimalesPrecioIva()
    private val fDecImpBase = fConfiguracion.decimalesImpBase()
    private val fDecImpIva = fConfiguracion.decimalesImpII()
    private val fUsarPiezas = fConfiguracion.usarPiezas()
    private val fUsarTasa1 = fConfiguracion.usarTasa1()
    private val fUsarTasa2 = fConfiguracion.usarTasa2()
    private val fNombreTasa1 = fConfiguracion.nombreTasa1()
    private val fNombreTasa2 = fConfiguracion.nombreTasa2()

    private var selectedPos: Int = RecyclerView.NO_POSITION


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fPosicion = holder.adapterPosition
        val item = lineas[fPosicion]
        holder.bind(item)

        if (selectedPos == fPosicion) {
            holder.itemView.ly_vl_descr.typeface = Typeface.DEFAULT_BOLD
            holder.itemView.ly_vl_impte.typeface = Typeface.DEFAULT_BOLD
        } else {
            holder.itemView.ly_vl_descr.typeface = Typeface.DEFAULT
            holder.itemView.ly_vl_impte.typeface = Typeface.DEFAULT
        }

        holder.itemView.setOnClickListener {
            //selectedPos = if (selectedPos == fPosicion) {
            //    RecyclerView.NO_POSITION
            //} else {
            //    fPosicion
            //}
            selectedPos = fPosicion

            notifyDataSetChanged()
            listener.onClick(it, lineas[fPosicion])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        setOnItemClickListener(listener)
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.ly_lineas_ventas, parent, false),
                    fIvaIncluido, fAplicarIva, fUsarPiezas, fUsarTasa1, fUsarTasa2, fNombreTasa1,
                    fNombreTasa2, fDecPrBase, fDecPrIva, fDecImpBase, fDecImpIva, activity)
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


    class ViewHolder(itemView: View, ivaIncluido: Boolean, aplicarIva: Boolean, usarPiezas: Boolean,
                     usarTasa1: Boolean, usarTasa2: Boolean, nombreTasa1: String, nombreTasa2: String, decPrBase: Int,
                     decPrIva: Int, decImpBase: Int, decImpIva: Int, activity: Activity): RecyclerView.ViewHolder(itemView) {
        private val fIvaIncluido = ivaIncluido
        private val fAplicarIva = aplicarIva
        private val fUsarTasa1 = usarTasa1
        private val fUsarTasa2 = usarTasa2
        private val fNombreTasa1 = nombreTasa1
        private val fNombreTasa2 = nombreTasa2
        private val fUsarPiezas = usarPiezas
        private val fDecPrBase = decPrBase
        private val fDecPrIva = decPrIva
        private val fDecImpBase = decImpBase
        private val fDecImpIva = decImpIva
        private val fRutaImagenes = dimeRutaImagenes(activity)

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
        private val tvTextoLinea = itemView.findViewById(R.id.ly_vl_textolinea) as TextView

        fun bind(linea: DatosLinVtas) {
            val queFichero = "$fRutaImagenes/ART_" + linea.articuloId + ".jpg"
            val bitmap = BitmapFactory.decodeFile(queFichero)
            imgArt.setImageBitmap(bitmap)

            tvDescr.text = linea.descripcion

            val sPorcIva = linea.porcIva?.replace(',', '.') ?: "0.0"
            val dPorcIva = sPorcIva.toDouble()

            // Si la l??nea es sin cargo lo indicamos.
            val queFlag = linea.flag
            val lineaSinCargo = (queFlag and FLAGLINEAVENTA_SIN_CARGO > 0)
            if (lineaSinCargo) {
                tvImporte.setText(R.string.sincargo)
            } else {
                val sImpte = if (fIvaIncluido && linea.importeII != "") linea.importeII.replace(',', '.')
                    else linea.importe.replace(',', '.')

                var dImpte = sImpte.toDouble()

                if (fIvaIncluido && fAplicarIva) {
                    // Tambi??n calculamos el importe iva inclu??do cuando es null.
                    if (linea.importeII == "") {
                        dImpte += dImpte * dPorcIva / 100
                    }
                    tvImporte.text = String.format("%." + fDecImpIva + "f", dImpte)
                } else {
                    tvImporte.text = String.format("%." + fDecImpBase + "f", dImpte)
                }
            }

            tvDescrFto.text = linea.descrFto
            tvCodArt.text = linea.codArticulo
            tvCajas.text = linea.cajas.replace(',', '.')
            tvCantidad.text = linea.cantidad.replace(',', '.')

            if (fUsarTasa1) {
                if (linea.tasa1 != "") {
                    val sTasa = linea.tasa1.replace(',', '.')
                    var dTasa = sTasa.toDouble()

                    if (fIvaIncluido && fAplicarIva) {
                        dTasa += dTasa * dPorcIva / 100
                    }
                    val queTasa =
                        fNombreTasa1 + " " + String.format(Locale.getDefault(), "%.3f", dTasa)
                    tvTasa1.text = queTasa
                } else tvTasa1.text = ""
            } else tvTasa1.text = ""

            if (fUsarTasa2) {
                if (linea.tasa2 != "") {
                    val sTasa = linea.tasa2.replace(',', '.')
                    var dTasa = sTasa.toDouble()

                    if (fIvaIncluido && fAplicarIva) {
                        dTasa += dTasa * dPorcIva / 100
                    }
                    val queTasa =
                        fNombreTasa2 + " " + String.format(Locale.getDefault(), "%.3f", dTasa)
                    tvTasa2.text = queTasa
                } else tvTasa2.text = ""
            } else tvTasa2.text = ""

            if (fIvaIncluido && fAplicarIva) {
                // El precio iva inclu??do vendr?? a null desde la gesti??n, por eso lo calculamos.
                val sPrecio = if (linea.precioII != "") linea.precioII.replace(',', '.')
                    else linea.precio.replace(',', '.')

                var dPrecio = sPrecio.toDouble()

                if (linea.precioII == "") {
                    dPrecio += dPrecio * dPorcIva / 100
                }
                tvPrecio.text = String.format("%." + fDecPrIva + "f", dPrecio)
            } else {
                tvPrecio.text = String.format("%." + fDecPrBase + "f", linea.precio.replace(',', '.').toDouble())
            }

            tvDto.text = String.format("%.2f", linea.dto.replace(',', '.').toDouble())

            if (linea.textoLinea != "") {
                if (linea.flag5 == 1) tvTextoLinea.setText(R.string.texto_l_modif)
                else tvTextoLinea.setText(R.string.texto_linea)
            }
            else tvTextoLinea.text = ""

            if (fUsarPiezas) {
                if (linea.piezas != "") {
                    tvPiezas.text = linea.piezas.replace(',', '.')

                    val queFlag3 = linea.flag3
                    val fLineaPorPiezas = (queFlag3 and FLAG3LINEAVENTA_PRECIO_POR_PIEZAS > 0)
                    if (fLineaPorPiezas) tvPiezas.setBackgroundColor(Color.parseColor("#979797"))
                    else tvPiezas.setBackgroundColor(Color.parseColor("#efefef"))
                }
                else tvPiezas.text = ""

            } else {
                // Quitamos el drawableLeft del TextView, adem??s de dejar el texto vac??o y el background en blanco.
                tvPiezas.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                tvPiezas.text = ""
                tvPiezas.setBackgroundColor(Color.WHITE)
            }

            tvTarifa.text = linea.tarifaId.toString()
        }
    }


}