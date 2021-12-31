package es.albainformatica.albamobileandroid.biocatalogo

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.fArtSeleccCat
import kotlinx.android.synthetic.main.bio_fragment_page_6.view.*
import kotlin.math.ceil


class ViewPagerAdapter(queRutaImagenes: String, queImagPorPagina: Int, queEmpresa: Int): RecyclerView.Adapter<ViewPagerAdapter.ViewPagerViewHolder>() {
    private var list: List<Int> = listOf()
    private var fRutaImagenes = queRutaImagenes
    private var fImagPorPagina = queImagPorPagina
    private var fEmpresaActual = queEmpresa


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (fImagPorPagina) {
            1 -> ViewPagerViewHolder(layoutInflater.inflate(R.layout.bio_fragment_page, parent, false), fRutaImagenes, fImagPorPagina, fEmpresaActual, list)
            4 -> ViewPagerViewHolder(layoutInflater.inflate(R.layout.bio_fragment_page_4, parent, false), fRutaImagenes, fImagPorPagina, fEmpresaActual, list)
            else -> ViewPagerViewHolder(layoutInflater.inflate(R.layout.bio_fragment_page_6, parent, false), fRutaImagenes, fImagPorPagina, fEmpresaActual, list)
        }
    }


    override fun onBindViewHolder(holder: ViewPagerViewHolder, position: Int) {
        holder.bind(position * fImagPorPagina)
    }

    fun setItem(list: List<Int>) {
        this.list = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        val dNumero = (list.size.toDouble() / fImagPorPagina)
        return ceil(dNumero).toInt()
    }


    class ViewPagerViewHolder(itemView: View, fRutaImagenes: String, queImagPorPagina: Int, queEmpresa: Int, list: List<Int>): RecyclerView.ViewHolder(itemView) {

        private val queRutaImag = fRutaImagenes
        private val fImagPorPagina = queImagPorPagina
        private val queLista = list
        //private val fMisc = Miscelan.getInstancia()
        private val fDocumento = Comunicador.fDocumento
        private val fArticulos = Comunicador.fArticulos
        private val fConfiguracion = Comunicador.fConfiguracion
        private val fIvaIncluido = fConfiguracion.ivaIncluido(queEmpresa)
        private val fFtoPrecioBase = fConfiguracion.formatoDecPrecioBase()
        private val fFtoPrecioII = fConfiguracion.formatoDecPrecioIva()
        private val fFtoImpteBase = fConfiguracion.formatoDecImptesBase()
        private val fFtoImpteII = fConfiguracion.formatoDecImptesIva()


        fun bind(fPosInicial: Int) {
            var queArtSelecc = fArtSeleccCat
            val fArticulo1 = if (fPosInicial < queLista.size) queLista[fPosInicial] else 0
            var fArticulo2 = 0
            var fArticulo3 = 0
            var fArticulo4 = 0
            var fArticulo5 = 0
            var fArticulo6 = 0

            if (fImagPorPagina > 1) {
                fArticulo2 = if (fPosInicial + 1 < queLista.size) queLista[fPosInicial + 1] else 0
                fArticulo3 = if (fPosInicial + 2 < queLista.size) queLista[fPosInicial + 2] else 0
                fArticulo4 = if (fPosInicial + 3 < queLista.size) queLista[fPosInicial + 3] else 0
                if (fImagPorPagina > 4) {
                    fArticulo5 = if (fPosInicial + 4 < queLista.size) queLista[fPosInicial + 4] else 0
                    fArticulo6 = if (fPosInicial + 5 < queLista.size) queLista[fPosInicial + 5] else 0
                }
            }
            // Si hemos cambiado de página queArtSelecc no será ninguno de los nuevos artículos que presentaremos
            if (queArtSelecc != fArticulo1 && queArtSelecc != fArticulo2 && queArtSelecc != fArticulo3
                && queArtSelecc != fArticulo4 && queArtSelecc != fArticulo5 && queArtSelecc != fArticulo6)
                queArtSelecc = 0

            if (queArtSelecc == 0 || fArticulo1 == queArtSelecc) presentarArticulo(fArticulo1, queArtSelecc, 1)

            if (fImagPorPagina > 1) {
                if (queArtSelecc == 0 || fArticulo2 == queArtSelecc) presentarArticulo(fArticulo2, queArtSelecc, 2)
                if (queArtSelecc == 0 || fArticulo3 == queArtSelecc) presentarArticulo(fArticulo3, queArtSelecc, 3)
                if (queArtSelecc == 0 || fArticulo4 == queArtSelecc) presentarArticulo(fArticulo4, queArtSelecc, 4)

                if (fImagPorPagina > 4) {
                    if (queArtSelecc == 0 || fArticulo5 == queArtSelecc) presentarArticulo(fArticulo5, queArtSelecc, 5)
                    if (queArtSelecc == 0 || fArticulo6 == queArtSelecc) presentarArticulo(fArticulo6, queArtSelecc, 6)
                }
            }
        }


        private fun presentarArticulo(queArticulo: Int, queArtSelecc: Int, queOrden: Int) {
            if (queArticulo > 0) {
                val queFichero = "$queRutaImag/ART_$queArticulo.jpg"
                val bitmap = BitmapFactory.decodeFile(queFichero, null)


                itemView.image1?.alpha = 1f
                itemView.image2?.alpha = 1f
                itemView.image3?.alpha = 1f
                itemView.image4?.alpha = 1f
                itemView.image5?.alpha = 1f
                itemView.image6?.alpha = 1f

                itemView.tvCodigo1?.typeface = Typeface.DEFAULT
                itemView.tvDescr1?.typeface = Typeface.DEFAULT
                itemView.tvCodigo2?.typeface = Typeface.DEFAULT
                itemView.tvDescr2?.typeface = Typeface.DEFAULT
                itemView.tvCodigo3?.typeface = Typeface.DEFAULT
                itemView.tvDescr3?.typeface = Typeface.DEFAULT
                itemView.tvCodigo4?.typeface = Typeface.DEFAULT
                itemView.tvDescr4?.typeface = Typeface.DEFAULT
                itemView.tvCodigo5?.typeface = Typeface.DEFAULT
                itemView.tvDescr5?.typeface = Typeface.DEFAULT
                itemView.tvCodigo6?.typeface = Typeface.DEFAULT
                itemView.tvDescr6?.typeface = Typeface.DEFAULT
                itemView.tvCodigo1?.textSize = 14f
                itemView.tvDescr1?.textSize = 14f
                itemView.tvCodigo2?.textSize = 14f
                itemView.tvDescr2?.textSize = 14f
                itemView.tvCodigo3?.textSize = 14f
                itemView.tvDescr3?.textSize = 14f
                itemView.tvCodigo4?.textSize = 14f
                itemView.tvDescr4?.textSize = 14f
                itemView.tvCodigo5?.textSize = 14f
                itemView.tvDescr5?.textSize = 14f
                itemView.tvCodigo6?.textSize = 14f
                itemView.tvDescr6?.textSize = 14f

                when (queOrden) {
                    1 -> {
                        itemView.image1.setImageBitmap(bitmap)
                        if (queArticulo == queArtSelecc) {
                            itemView.tvCodigo1.typeface = Typeface.DEFAULT_BOLD
                            itemView.tvDescr1.typeface = Typeface.DEFAULT_BOLD
                            itemView.tvCodigo1.textSize = 20f
                            itemView.tvDescr1.textSize = 20f
                            itemView.image1.alpha = 0.5f
                        }
                    }
                    2 -> {
                        itemView.image2.setImageBitmap(bitmap)
                        if (queArticulo == queArtSelecc) {
                            itemView.tvCodigo2.typeface = Typeface.DEFAULT_BOLD
                            itemView.tvDescr2.typeface = Typeface.DEFAULT_BOLD
                            itemView.tvCodigo2.textSize = 20f
                            itemView.tvDescr2.textSize = 20f
                            itemView.image2.alpha = 0.5f
                        }
                    }
                    3 -> {
                        itemView.image3.setImageBitmap(bitmap)
                        if (queArticulo == queArtSelecc) {
                            itemView.tvCodigo3.typeface = Typeface.DEFAULT_BOLD
                            itemView.tvDescr3.typeface = Typeface.DEFAULT_BOLD
                            itemView.tvCodigo3.textSize = 20f
                            itemView.tvDescr3.textSize = 20f
                            itemView.image3.alpha = 0.5f
                        }
                    }
                    4 -> {
                        itemView.image4.setImageBitmap(bitmap)
                        if (queArticulo == queArtSelecc) {
                            itemView.tvCodigo4.typeface = Typeface.DEFAULT_BOLD
                            itemView.tvDescr4.typeface = Typeface.DEFAULT_BOLD
                            itemView.tvCodigo4.textSize = 20f
                            itemView.tvDescr4.textSize = 20f
                            itemView.image4.alpha = 0.5f
                        }
                    }
                    5 -> {
                        itemView.image5.setImageBitmap(bitmap)
                        if (queArticulo == queArtSelecc) {
                            itemView.tvCodigo5.typeface = Typeface.DEFAULT_BOLD
                            itemView.tvDescr5.typeface = Typeface.DEFAULT_BOLD
                            itemView.tvCodigo5.textSize = 20f
                            itemView.tvDescr5.textSize = 20f
                            itemView.image5.alpha = 0.5f
                        }
                    }
                    6 -> {
                        itemView.image6.setImageBitmap(bitmap)
                        if (queArticulo == queArtSelecc) {
                            itemView.tvCodigo6.typeface = Typeface.DEFAULT_BOLD
                            itemView.tvDescr6.typeface = Typeface.DEFAULT_BOLD
                            itemView.tvCodigo6.textSize = 20f
                            itemView.tvDescr6.textSize = 20f
                            itemView.image6.alpha = 0.5f
                        }
                    }
                }

                val existeArt = fArticulos.existeArticulo(queArticulo)
                val aDatosArt = fDocumento.datosArtEnCatLineas(queArticulo)

                if (aDatosArt[0] == "T") {
                    verPreciosVend(aDatosArt, queOrden)

                } else {
                    // Si el artículo aún no está vendido tendremos que calcular su precio
                    fDocumento.inicializarLinea()
                    fDocumento.fArticulo = queArticulo
                    fDocumento.fCantidad = java.lang.Double.parseDouble(aDatosArt[2])
                    fDocumento.calculaPrecioYDto(fArticulos.getGrupo(), fArticulos.getDpto(), fArticulos.fCodProv, fArticulos.getPorcIva())

                    verPreciosNoVend(queOrden)
                }

                if (existeArt) verDatosGenArticulo(queArticulo, queOrden)
                else ocultarImagAux(queOrden)

            } else {
                anularArticulo(queOrden)
            }
        }

        private fun anularArticulo(queOrden: Int) {
            ocultarImagAux(queOrden)
            when (queOrden) {
                1-> {
                    itemView.image1.setImageResource(R.drawable.cancelar)
                    itemView.tvbioTitPr1.visibility = View.GONE
                    itemView.tvbioTitDto1.visibility = View.GONE
                    itemView.tvCodigo1.text = ""
                    itemView.tvDescr1.text = ""
                    itemView.tvCodAlt1.text = ""
                    itemView.tvPrVta1.text = ""
                    itemView.tvDtoVta1.text = ""
                    itemView.tvImpteVta1.text = ""
                }
                2-> {
                    itemView.image2.setImageResource(R.drawable.cancelar)
                    itemView.tvbioTitPr2.visibility = View.GONE
                    itemView.tvbioTitDto2.visibility = View.GONE
                    itemView.tvCodigo2.text = ""
                    itemView.tvDescr2.text = ""
                    itemView.tvCodAlt2.text = ""
                    itemView.tvPrVta2.text = ""
                    itemView.tvDtoVta2.text = ""
                    itemView.tvImpteVta2.text = ""
                }
                3-> {
                    itemView.image3.setImageResource(R.drawable.cancelar)
                    itemView.tvbioTitPr3.visibility = View.GONE
                    itemView.tvbioTitDto3.visibility = View.GONE
                    itemView.tvCodigo3.text = ""
                    itemView.tvDescr3.text = ""
                    itemView.tvCodAlt3.text = ""
                    itemView.tvPrVta3.text = ""
                    itemView.tvDtoVta3.text = ""
                    itemView.tvImpteVta3.text = ""
                }
                4-> {
                    itemView.image4.setImageResource(R.drawable.cancelar)
                    itemView.tvbioTitPr4.visibility = View.GONE
                    itemView.tvbioTitDto4.visibility = View.GONE
                    itemView.tvCodigo4.text = ""
                    itemView.tvDescr4.text = ""
                    itemView.tvCodAlt4.text = ""
                    itemView.tvPrVta4.text = ""
                    itemView.tvDtoVta4.text = ""
                    itemView.tvImpteVta4.text = ""
                }
                5-> {
                    itemView.image5.setImageResource(R.drawable.cancelar)
                    itemView.tvbioTitPr5.visibility = View.GONE
                    itemView.tvbioTitDto5.visibility = View.GONE
                    itemView.tvCodigo5.text = ""
                    itemView.tvDescr5.text = ""
                    itemView.tvCodAlt5.text = ""
                    itemView.tvPrVta5.text = ""
                    itemView.tvDtoVta5.text = ""
                    itemView.tvImpteVta5.text = ""
                }
                6-> {
                    itemView.image6.setImageResource(R.drawable.cancelar)
                    itemView.tvbioTitPr6.visibility = View.GONE
                    itemView.tvbioTitDto6.visibility = View.GONE
                    itemView.tvCodigo6.text = ""
                    itemView.tvDescr6.text = ""
                    itemView.tvCodAlt6.text = ""
                    itemView.tvPrVta6.text = ""
                    itemView.tvDtoVta6.text = ""
                    itemView.tvImpteVta6.text = ""
                }
            }
        }

        private fun verPreciosVend(aDatosArt: Array<String>, queOrden: Int) {
            when (queOrden) {
                1 -> {
                    if (aDatosArt[2] != "0") itemView.tvCantidad1.text = String.format("%.0f", aDatosArt[2].toDouble())
                    else itemView.tvCantidad1.text = ""
                    if (aDatosArt[1] != "0") itemView.tvCajas1.text = String.format("%.0f", aDatosArt[1].toDouble())
                    else itemView.tvCajas1.text = ""

                    if (fIvaIncluido) {
                        itemView.tvPrVta1.text = String.format(fFtoPrecioII, aDatosArt[3].toDouble())
                        itemView.tvDtoVta1.text = String.format("%.2f", aDatosArt[4].toDouble())
                        itemView.tvImpteVta1.text = String.format(fFtoImpteII, aDatosArt[5].toDouble())
                    } else {
                        itemView.tvPrVta1.text = String.format(fFtoPrecioBase, aDatosArt[3].toDouble())
                        itemView.tvDtoVta1.text = String.format("%.2f", aDatosArt[4].toDouble())
                        itemView.tvImpteVta1.text = String.format(fFtoImpteBase, aDatosArt[5].toDouble())
                    }
                }
                2 -> {
                    if (aDatosArt[2] != "0") itemView.tvCantidad2.text = String.format("%.0f", aDatosArt[2].toDouble())
                    else itemView.tvCantidad2.text = ""
                    if (aDatosArt[1] != "0") itemView.tvCajas2.text = String.format("%.0f", aDatosArt[1].toDouble())
                    else itemView.tvCajas2.text = ""

                    if (fIvaIncluido) {
                        itemView.tvPrVta2.text = String.format(fFtoPrecioII, aDatosArt[3].toDouble())
                        itemView.tvDtoVta2.text = String.format("%.2f", aDatosArt[4].toDouble())
                        itemView.tvImpteVta2.text = String.format(fFtoImpteII, aDatosArt[5].toDouble())
                    } else {
                        itemView.tvPrVta2.text = String.format(fFtoPrecioBase, aDatosArt[3].toDouble())
                        itemView.tvDtoVta2.text = String.format("%.2f", aDatosArt[4].toDouble())
                        itemView.tvImpteVta2.text = String.format(fFtoImpteBase, aDatosArt[5].toDouble())
                    }
                }
                3 -> {
                    if (aDatosArt[2] != "0") itemView.tvCantidad3.text = String.format("%.0f", aDatosArt[2].toDouble())
                    else itemView.tvCantidad3.text = ""
                    if (aDatosArt[1] != "0") itemView.tvCajas3.text = String.format("%.0f", aDatosArt[1].toDouble())
                    else itemView.tvCajas3.text = ""

                    if (fIvaIncluido) {
                        itemView.tvPrVta3.text = String.format(fFtoPrecioII, aDatosArt[3].toDouble())
                        itemView.tvDtoVta3.text = String.format("%.2f", aDatosArt[4].toDouble())
                        itemView.tvImpteVta3.text = String.format(fFtoImpteII, aDatosArt[5].toDouble())
                    } else {
                        itemView.tvPrVta3.text = String.format(fFtoPrecioBase, aDatosArt[3].toDouble())
                        itemView.tvDtoVta3.text = String.format("%.2f", aDatosArt[4].toDouble())
                        itemView.tvImpteVta3.text = String.format(fFtoImpteBase, aDatosArt[5].toDouble())
                    }
                }
                4 -> {
                    if (aDatosArt[2] != "0") itemView.tvCantidad4.text = String.format("%.0f", aDatosArt[2].toDouble())
                    else itemView.tvCantidad4.text = ""
                    if (aDatosArt[1] != "0") itemView.tvCajas4.text = String.format("%.0f", aDatosArt[1].toDouble())
                    else itemView.tvCajas4.text = ""

                    if (fIvaIncluido) {
                        itemView.tvPrVta4.text = String.format(fFtoPrecioII, aDatosArt[3].toDouble())
                        itemView.tvDtoVta4.text = String.format("%.2f", aDatosArt[4].toDouble())
                        itemView.tvImpteVta4.text = String.format(fFtoImpteII, aDatosArt[5].toDouble())
                    } else {
                        itemView.tvPrVta4.text = String.format(fFtoPrecioBase, aDatosArt[3].toDouble())
                        itemView.tvDtoVta4.text = String.format("%.2f", aDatosArt[4].toDouble())
                        itemView.tvImpteVta4.text = String.format(fFtoImpteBase, aDatosArt[5].toDouble())
                    }
                }
                5 -> {
                    if (aDatosArt[2] != "0") itemView.tvCantidad5.text = String.format("%.0f", aDatosArt[2].toDouble())
                    else itemView.tvCantidad5.text = ""
                    if (aDatosArt[1] != "0") itemView.tvCajas5.text = String.format("%.0f", aDatosArt[1].toDouble())
                    else itemView.tvCajas5.text = ""

                    if (fIvaIncluido) {
                        itemView.tvPrVta5.text = String.format(fFtoPrecioII, aDatosArt[3].toDouble())
                        itemView.tvDtoVta5.text = String.format("%.2f", aDatosArt[4].toDouble())
                        itemView.tvImpteVta5.text = String.format(fFtoImpteII, aDatosArt[5].toDouble())
                    } else {
                        itemView.tvPrVta5.text = String.format(fFtoPrecioBase, aDatosArt[3].toDouble())
                        itemView.tvDtoVta5.text = String.format("%.2f", aDatosArt[4].toDouble())
                        itemView.tvImpteVta5.text = String.format(fFtoImpteBase, aDatosArt[5].toDouble())
                    }
                }
                6 -> {
                    if (aDatosArt[2] != "0") itemView.tvCantidad6.text = String.format("%.0f", aDatosArt[2].toDouble())
                    else itemView.tvCantidad6.text = ""
                    if (aDatosArt[1] != "0") itemView.tvCajas6.text = String.format("%.0f", aDatosArt[1].toDouble())
                    else itemView.tvCajas6.text = ""

                    if (fIvaIncluido) {
                        itemView.tvPrVta6.text = String.format(fFtoPrecioII, aDatosArt[3].toDouble())
                        itemView.tvDtoVta6.text = String.format("%.2f", aDatosArt[4].toDouble())
                        itemView.tvImpteVta6.text = String.format(fFtoImpteII, aDatosArt[5].toDouble())
                    } else {
                        itemView.tvPrVta6.text = String.format(fFtoPrecioBase, aDatosArt[3].toDouble())
                        itemView.tvDtoVta6.text = String.format("%.2f", aDatosArt[4].toDouble())
                        itemView.tvImpteVta6.text = String.format(fFtoImpteBase, aDatosArt[5].toDouble())
                    }
                }
            }
        }

        private fun verPreciosNoVend(queOrden: Int) {
            when (queOrden) {
                1 -> {
                    itemView.tvCantidad1.text = ""
                    itemView.tvCajas1.text = ""

                    if (fIvaIncluido) itemView.tvPrVta1.text = String.format(fFtoPrecioII, fDocumento.fPrecioII)
                    else itemView.tvPrVta1.text = String.format(fFtoPrecioBase, fDocumento.fPrecio)
                    itemView.tvDtoVta1.text = String.format("%.2f", fDocumento.fDtoLin)
                }
                2 -> {
                    itemView.tvCantidad2.text = ""
                    itemView.tvCajas2.text = ""

                    if (fIvaIncluido) itemView.tvPrVta2.text = String.format(fFtoPrecioII, fDocumento.fPrecioII)
                    else itemView.tvPrVta2.text = String.format(fFtoPrecioBase, fDocumento.fPrecio)
                    itemView.tvDtoVta2.text = String.format("%.2f", fDocumento.fDtoLin)
                }
                3 -> {
                    itemView.tvCantidad3.text = ""
                    itemView.tvCajas3.text = ""

                    if (fIvaIncluido) itemView.tvPrVta3.text = String.format(fFtoPrecioII, fDocumento.fPrecioII)
                    else itemView.tvPrVta3.text = String.format(fFtoPrecioBase, fDocumento.fPrecio)
                    itemView.tvDtoVta3.text = String.format("%.2f", fDocumento.fDtoLin)
                }
                4 -> {
                    itemView.tvCantidad4.text = ""
                    itemView.tvCajas4.text = ""

                    if (fIvaIncluido) itemView.tvPrVta4.text = String.format(fFtoPrecioII, fDocumento.fPrecioII)
                    else itemView.tvPrVta4.text = String.format(fFtoPrecioBase, fDocumento.fPrecio)
                    itemView.tvDtoVta4.text = String.format("%.2f", fDocumento.fDtoLin)
                }
                5 -> {
                    itemView.tvCantidad5.text = ""
                    itemView.tvCajas5.text = ""

                    if (fIvaIncluido) itemView.tvPrVta5.text = String.format(fFtoPrecioII, fDocumento.fPrecioII)
                    else itemView.tvPrVta5.text = String.format(fFtoPrecioBase, fDocumento.fPrecio)
                    itemView.tvDtoVta5.text = String.format("%.2f", fDocumento.fDtoLin)
                }
                6 -> {
                    itemView.tvCantidad6.text = ""
                    itemView.tvCajas6.text = ""

                    if (fIvaIncluido) itemView.tvPrVta6.text = String.format(fFtoPrecioII, fDocumento.fPrecioII)
                    else itemView.tvPrVta6.text = String.format(fFtoPrecioBase, fDocumento.fPrecio)
                    itemView.tvDtoVta6.text = String.format("%.2f", fDocumento.fDtoLin)
                }
            }
        }

        private fun verDatosGenArticulo(queArticulo: Int, queOrden: Int) {
            when (queOrden) {
                1 -> {
                    itemView.tvCodigo1.text = fArticulos.getCodigo()
                    itemView.tvDescr1.text = fArticulos.getDescripcion()
                    itemView.tvCodAlt1.text = fArticulos.getCodAlternativo()

                    if (fArticulos.artEnHistorico(fDocumento.fCliente, queArticulo)) itemView.imgArtEnHco1.visibility = View.VISIBLE
                    else itemView.imgArtEnHco1.visibility = View.GONE

                    if (fArticulos.artEnFtosLineas(queArticulo)) itemView.imgArtVendido1.visibility = View.VISIBLE
                    else itemView.imgArtVendido1.visibility = View.GONE
                }
                2 -> {
                    itemView.tvCodigo2.text = fArticulos.getCodigo()
                    itemView.tvDescr2.text = fArticulos.getDescripcion()
                    itemView.tvCodAlt2.text = fArticulos.getCodAlternativo()

                    if (fArticulos.artEnHistorico(fDocumento.fCliente, queArticulo)) itemView.imgArtEnHco2.visibility = View.VISIBLE
                    else itemView.imgArtEnHco2.visibility = View.GONE

                    if (fArticulos.artEnFtosLineas(queArticulo)) itemView.imgArtVendido2.visibility = View.VISIBLE
                    else itemView.imgArtVendido2.visibility = View.GONE
                }
                3 -> {
                    itemView.tvCodigo3.text = fArticulos.getCodigo()
                    itemView.tvDescr3.text = fArticulos.getDescripcion()
                    itemView.tvCodAlt3.text = fArticulos.getCodAlternativo()

                    if (fArticulos.artEnHistorico(fDocumento.fCliente, queArticulo)) itemView.imgArtEnHco3.visibility = View.VISIBLE
                    else itemView.imgArtEnHco3.visibility = View.GONE

                    if (fArticulos.artEnFtosLineas(queArticulo)) itemView.imgArtVendido3.visibility = View.VISIBLE
                    else itemView.imgArtVendido3.visibility = View.GONE
                }
                4 -> {
                    itemView.tvCodigo4.text = fArticulos.getCodigo()
                    itemView.tvDescr4.text = fArticulos.getDescripcion()
                    itemView.tvCodAlt4.text = fArticulos.getCodAlternativo()

                    if (fArticulos.artEnHistorico(fDocumento.fCliente, queArticulo)) itemView.imgArtEnHco4.visibility = View.VISIBLE
                    else itemView.imgArtEnHco4.visibility = View.GONE

                    if (fArticulos.artEnFtosLineas(queArticulo)) itemView.imgArtVendido4.visibility = View.VISIBLE
                    else itemView.imgArtVendido4.visibility = View.GONE
                }
                5 -> {
                    itemView.tvCodigo5.text = fArticulos.getCodigo()
                    itemView.tvDescr5.text = fArticulos.getDescripcion()
                    itemView.tvCodAlt5.text = fArticulos.getCodAlternativo()

                    if (fArticulos.artEnHistorico(fDocumento.fCliente, queArticulo)) itemView.imgArtEnHco5.visibility = View.VISIBLE
                    else itemView.imgArtEnHco5.visibility = View.GONE

                    if (fArticulos.artEnFtosLineas(queArticulo)) itemView.imgArtVendido5.visibility = View.VISIBLE
                    else itemView.imgArtVendido5.visibility = View.GONE
                }
                6 -> {
                    itemView.tvCodigo6.text = fArticulos.getCodigo()
                    itemView.tvDescr6.text = fArticulos.getDescripcion()
                    itemView.tvCodAlt6.text = fArticulos.getCodAlternativo()

                    if (fArticulos.artEnHistorico(fDocumento.fCliente, queArticulo)) itemView.imgArtEnHco6.visibility = View.VISIBLE
                    else itemView.imgArtEnHco6.visibility = View.GONE

                    if (fArticulos.artEnFtosLineas(queArticulo)) itemView.imgArtVendido6.visibility = View.VISIBLE
                    else itemView.imgArtVendido6.visibility = View.GONE
                }
            }
        }

        private fun ocultarImagAux(queOrden: Int) {
            when (queOrden) {
                1 -> {
                    itemView.imgArtEnHco1.visibility = View.GONE
                    itemView.imgArtVendido1.visibility = View.GONE
                }
                2 -> {
                    itemView.imgArtEnHco2.visibility = View.GONE
                    itemView.imgArtVendido2.visibility = View.GONE
                }
                3 -> {
                    itemView.imgArtEnHco3.visibility = View.GONE
                    itemView.imgArtVendido3.visibility = View.GONE
                }
                4 -> {
                    itemView.imgArtEnHco4.visibility = View.GONE
                    itemView.imgArtVendido4.visibility = View.GONE
                }
                5 -> {
                    itemView.imgArtEnHco5.visibility = View.GONE
                    itemView.imgArtVendido5.visibility = View.GONE
                }
                6 -> {
                    itemView.imgArtEnHco6.visibility = View.GONE
                    itemView.imgArtVendido6.visibility = View.GONE
                }
            }
        }
    }


}