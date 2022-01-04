package es.albainformatica.albamobileandroid.oldcatalogo

import es.albainformatica.albamobileandroid.database.MyDatabase.Companion.getInstance
import android.widget.BaseAdapter
import android.app.Activity
import android.content.Context
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.ventas.Documento
import android.view.LayoutInflater
import android.content.SharedPreferences
import android.net.Uri
import android.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.EditText
import android.widget.ImageView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.entity.OfertasEnt
import java.io.File
import java.util.*

// Un BaseAdapter puede usarse para un Adapter en un listview o gridview.
// Hay que implementar algunos métodos heredados de la clase Adapter,
// porque BaseAdapter es una subclase de Adapter.
// Estos métodos son: getCount(), getItem(), getItemId(), getView().
class GrvImageArticulosAdapter: BaseAdapter {
    var activity: Activity
    private var fArticulosGrv: ArticulosClase
    private lateinit var fDocumento: Documento
    var fConfiguracion = Comunicador.fConfiguracion
    val carpetaImagenes: String
    private var itemsArticulo: ArrayList<ItemArticulo>

    private var fFtoDecCant: String = ""
    private var fFtoPrecio: String = ""
    private var fVendiendo: Boolean = false
    private var fIvaIncluido: Boolean = false
    private var fEnHistorico: Boolean = false
    private var fDecPrBase: Int = 2
    private var fTarifa: Short = 0
    private var inflater: LayoutInflater
    private var fEmpresaActual = 0
    private lateinit var prefs: SharedPreferences

    // El constructor necesita el contexto de la actividad donde se utiliza el adapter
    constructor(activity: Activity, fGrupo: Int, fDepartam: Int, queBuscar: String, dondeBuscar: Short,
        queOrdenacion: Short, fSoloOftas: Boolean, queTarifa: Short, vendiendo: Boolean) {
        this.activity = activity
        carpetaImagenes = dimeRutaImagenes(activity)
        fVendiendo = vendiendo
        fEnHistorico = false
        fTarifa = queTarifa
        inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        fEmpresaActual = prefs.getInt("ultima_empresa", 0) ?: 0
        fArticulosGrv = Comunicador.fArticulosGrv
        if (fVendiendo)
            fDocumento = Comunicador.fDocumento

        //Configuracion fConfiguracion = Comunicador.fConfiguracion;
        fFtoDecCant = fConfiguracion.formatoDecCantidad()
        fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresaActual)
        fFtoPrecio =
            if (fIvaIncluido) fConfiguracion.formatoDecPrecioIva() else fConfiguracion.formatoDecPrecioBase()
        fDecPrBase = fConfiguracion.decimalesPrecioBase()

        // Vemos la tarifa para cajas.
        val queTrfCajas = fConfiguracion.tarifaCajas()
        val queCliente: Int = if (fVendiendo) fDocumento.fCliente else 0

        // Tenemos en cuenta si hemos indicado alguna cadena de búsqueda.
        if (fSoloOftas) {
            fArticulosGrv.abrirSoloOftas(queTarifa, queCliente, queOrdenacion)
        } else {
            if (queBuscar == "") {
                fArticulosGrv.abrirParaGridView(fGrupo, fDepartam, queTarifa, queTrfCajas, queCliente, queOrdenacion)
            } else {
                if (dondeBuscar.toInt() == 0) fArticulosGrv.abrirBusqEnGrupoParaGridView(
                    queBuscar,
                    fGrupo,
                    fDepartam,
                    queTarifa,
                    queTrfCajas,
                    queCliente,
                    queOrdenacion
                ) else fArticulosGrv.abrirBusqParaGridView(
                    queBuscar,
                    queTarifa,
                    queTrfCajas,
                    queCliente,
                    queOrdenacion
                )
            }
        }
        // Obtenemos los items para el gridView.
        itemsArticulo = obtenerItems(fSoloOftas)
    }

    // Tendremos tres constructores para la clase, dependiendo de si usamos grupos, clasificadores/catalogos o histórico.
    constructor(
        activity: Activity, fClasificador: Int, queBuscar: String, dondeBuscar: Short,
        queOrdenacion: Short, fSoloOftas: Boolean, queTarifa: Short, vendiendo: Boolean
    ) {
        this.activity = activity
        carpetaImagenes = dimeRutaImagenes(activity)
        fVendiendo = vendiendo
        fEnHistorico = false
        inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        fArticulosGrv = Comunicador.fArticulosGrv
        if (fVendiendo)
            fDocumento = Comunicador.fDocumento
        //Configuracion fConfiguracion = Comunicador.fConfiguracion;
        fFtoDecCant = fConfiguracion.formatoDecCantidad()
        fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresaActual)
        fFtoPrecio =
            if (fIvaIncluido) fConfiguracion.formatoDecPrecioIva() else fConfiguracion.formatoDecPrecioBase()
        fDecPrBase = fConfiguracion.decimalesPrecioBase()

        // Vemos la tarifa para cajas.
        val queTrfCajas = fConfiguracion.tarifaCajas()
        val queCliente: Int = if (fVendiendo) fDocumento.fCliente else 0

        // Tenemos en cuenta si hemos indicado alguna cadena de búsqueda.
        if (fSoloOftas) {
            fArticulosGrv.abrirSoloOftas(queTarifa, queCliente, queOrdenacion)
        } else {
            if (queBuscar == "") {
                fArticulosGrv.abrirClasifParaGrView(
                    fClasificador,
                    queTarifa,
                    queTrfCajas,
                    queCliente,
                    queOrdenacion
                )
            } else {
                if (dondeBuscar.toInt() == 0) fArticulosGrv.abrirBusqEnClasifParaGridView(
                    queBuscar,
                    fClasificador,
                    queTarifa,
                    queTrfCajas,
                    queCliente,
                    queOrdenacion
                ) else fArticulosGrv.abrirBusqParaGridView(
                    queBuscar,
                    queTarifa,
                    queTrfCajas,
                    queCliente,
                    queOrdenacion
                )
            }
        }
        // Obtenemos los items para el gridView.
        itemsArticulo = obtenerItems(fSoloOftas)
    }

    // Constructor para cargar el histórico.
    constructor(
        activity: Activity, queBuscar: String, dondeBuscar: Short, queOrdenacion: Short,
        fSoloOftas: Boolean, queTarifa: Short, vendiendo: Boolean
    ) {
        this.activity = activity
        carpetaImagenes = dimeRutaImagenes(activity)
        fVendiendo = vendiendo
        fEnHistorico = false
        inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        fArticulosGrv = Comunicador.fArticulosGrv
        fDocumento = Comunicador.fDocumento
        //Configuracion fConfiguracion = Comunicador.fConfiguracion;
        fFtoDecCant = fConfiguracion.formatoDecCantidad()
        fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresaActual)
        fFtoPrecio =
            if (fIvaIncluido) fConfiguracion.formatoDecPrecioIva() else fConfiguracion.formatoDecPrecioBase()
        fDecPrBase = fConfiguracion.decimalesPrecioBase()

        // Vemos la tarifa para cajas.
        val queTrfCajas = fConfiguracion.tarifaCajas()
        val queCliente: Int = if (fVendiendo) fDocumento.fCliente else 0

        // Tenemos en cuenta si hemos indicado alguna cadena de búsqueda.
        if (fSoloOftas) fArticulosGrv.abrirSoloOftas(queTarifa, queCliente, queOrdenacion) else {
            fEnHistorico = true
            if (queBuscar == "") {
                //fEnHistorico = true;
                fArticulosGrv.abrirHistorico(fDocumento.fCliente, queOrdenacion, queTarifa)
            } else {
                if (dondeBuscar.toInt() == 0) fArticulosGrv.abrirBusqEnHcoParaGridView(
                    queBuscar,
                    queCliente,
                    queOrdenacion,
                    queTarifa
                ) else fArticulosGrv.abrirBusqParaGridView(
                    queBuscar,
                    queTarifa,
                    queTrfCajas,
                    queCliente,
                    queOrdenacion
                )
            }
        }
        // Obtenemos los items para el gridView.
        itemsArticulo = obtenerItems(fSoloOftas)
    }

    private fun obtenerItems(fSoloOftas: Boolean): ArrayList<ItemArticulo> {
        val ofertasDao = getInstance(activity)?.ofertasDao()
        var hayOferta: Boolean
        val items = ArrayList<ItemArticulo>()
        var queCantidad = "0.0"
        var queCajas = "0.0"
        val usarOfertas = fConfiguracion.usarOfertas()
        val ofertas: List<Int> = ofertasDao?.getAllOftas(fEmpresaActual, fTarifa.toShort()) ?: emptyList()
        if (fArticulosGrv.cursor.moveToFirst()) {
            fArticulosGrv.cursor.moveToPosition(-1)
            while (fArticulosGrv.cursor.moveToNext()) {
                var quePrOfta = "0.0"
                var queDtoOfta = "0.0"
                var queArticulo = 0
                if (usarOfertas) {
                    // Vemos si el artículo está en el array de ofertas, en cuyo caso buscaremos el precio y dto. de la oferta
                    val queIndice = ofertas.indexOf(fArticulosGrv.getArticulo())
                    if (queIndice > -1) {
                        queArticulo = ofertas[queIndice]
                        val (_, _, _, precio, dto) = ofertasDao?.getOftaArt(queArticulo, fEmpresaActual, fTarifa.toShort()) ?: OfertasEnt()
                        quePrOfta = precio
                        queDtoOfta = dto
                    }
                }
                hayOferta = queArticulo > 0
                // Buscamos en las líneas del documento si el artículo tiene alguna cantidad vendida. Idem con las cajas.
                if (fVendiendo) {
                    // fDocumento.dimeCantCajasArticulo devolverá un array de string con dos elementos: uno para la cantidad y otro para las cajas.
                    val sCantCajas = fDocumento.dimeCantCajasArticulo(fArticulosGrv.getArticulo())
                    queCantidad = sCantCajas[0]
                    queCajas = sCantCajas[1]
                }

                // Si vamos a cargar el histórico, los campos que necesitamos son otros.
                if (fEnHistorico) {
                    items.add(
                        ItemArticulo(
                            fArticulosGrv.getArticulo(),
                            fArticulosGrv.getCodigo(),
                            fArticulosGrv.getDescripcion(),
                            fArticulosGrv.getUCajaAsString(),
                            fArticulosGrv.getCantHco(),
                            fArticulosGrv.getCajasHco(),
                            fArticulosGrv.getPrecioHco(),
                            fArticulosGrv.getDtoHco(),
                            fArticulosGrv.getPrecio(),
                            fArticulosGrv.getDto(),
                            quePrOfta,
                            queDtoOfta,
                            fArticulosGrv.getPorcIva(),
                            hayOferta,
                            queCantidad,
                            queCajas,
                            fArticulosGrv.getFechaHco()
                        )
                    )
                } else {
                    if (!fSoloOftas || queArticulo > 0) {
                        items.add(
                            ItemArticulo(
                                fArticulosGrv.getArticulo(),
                                fArticulosGrv.getCodigo(),
                                fArticulosGrv.getDescripcion(),
                                fArticulosGrv.getUCajaAsString(),
                                fArticulosGrv.getPrecio(),
                                fArticulosGrv.getDto(),
                                fArticulosGrv.getPrCajas(),
                                fArticulosGrv.getDtoCajas(),
                                quePrOfta,
                                queDtoOfta,
                                fArticulosGrv.getPorcIva(),
                                hayOferta,
                                queCantidad,
                                queCajas,
                                fArticulosGrv.getExistencias(),
                                fArticulosGrv.getTieneHco(),
                                ""
                            )
                        )
                    }
                }
            }
        }
        return items
    }

    // Devuelve el número de elementos que se introducen en el adapter
    override fun getCount(): Int {
        return itemsArticulo.size
    }

    override fun getItem(position: Int): Any {
        return itemsArticulo[position]
    }

    override fun getItemId(position: Int): Long {
        return itemsArticulo[position].articulo.toLong()
    }

    // Crear un nuevo ImageView para cada item referenciado por el Adapter
    override fun getView(position: Int, contentView: View?, parent: ViewGroup?): View? {
        // Este método crea una nueva View para cada elemento añadido al Adapter.
        // Se le pasa el View en el que se ha pulsado: contentView.
        // Si contentView es null, se instancia. Esta View contiene todos los elementos del layout 'ly_cat_articulos',
        // que se irán instanciando uno a uno.
        var vi = contentView
        if (contentView == null) {
            //LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            vi = inflater.inflate(R.layout.ly_cat_articulos, null)
        }
        val item = itemsArticulo[position]

        // Obtenemos la imagen del artículo.
        val imageView = vi?.findViewById<ImageView>(R.id.imvArticulo)
        val path = carpetaImagenes + item.getImagen()
        val file = File(path)
        if (file.exists()) imageView?.setImageURI(Uri.parse(path)) else imageView?.setImageDrawable(null)

        // Descripción
        val tvDescr = vi?.findViewById<TextView>(R.id.tvArticulo)
        tvDescr?.text = item.descr
        // Codigo
        val tvCodigo = vi?.findViewById<TextView>(R.id.tvCodigoArt)
        tvCodigo?.text = item.codigo

        // Si estamos vendiendo desde el histórico presentaremos una información distinta a si lo hacemos desde catálogos
        // o desde grupos y departamentos, ya que presentaremos datos del histórico.
        if (fEnHistorico) {
            cargarEtiquetasHco(item, vi)
        } else {
            cargarEtiquetas(item, vi)
        }

        // Le asignamos a cada imagen la posición a través de la propiedad tag.
        // De esta forma luego podremos saber en qué artículo hemos pulsado la imagen de sumar o restar.
        // Si no estamos vendiendo las ocultamos.
        prepararImagenes(vi, position)

        // Asignamos también el tag en la imagen del histórico, para cuando pulsemos sobre ella
        // para ver el histórico del artículo. Idem con las cajas y las unidades. La mostraremos sólo si
        // el artículo tiene histórico para el cliente de la venta.
        val ivHco = vi?.findViewById<ImageView>(R.id.imvHco)
        if (fVendiendo) {
            ivHco?.tag = position
            if (item.tieneHco) ivHco?.visibility = View.VISIBLE else ivHco?.visibility = View.GONE
        } else ivHco?.visibility = View.GONE


        // Cantidad
        val edtUnidades = vi?.findViewById<EditText>(R.id.edtUnidades)
        edtUnidades?.setText(String.format(fFtoDecCant, item.cantidad))

        // Cajas
        val edtCajas = vi?.findViewById<EditText>(R.id.edtCajas)
        edtCajas?.setText(String.format(fFtoDecCant, item.cajas))
        return vi
    }

    private fun prepararImagenes(vi: View?, position: Int) {
        val ivSumarCant = vi?.findViewById<ImageView>(R.id.imvCantidadMas)
        if (fVendiendo) ivSumarCant?.tag = position else ivSumarCant?.visibility = View.GONE
        val ivRestarCant = vi?.findViewById<ImageView>(R.id.imvCantidadMenos)
        if (fVendiendo) ivRestarCant?.tag = position else ivRestarCant?.visibility = View.GONE
        val ivSumarCajas = vi?.findViewById<ImageView>(R.id.imvCajasMas)
        if (fVendiendo) ivSumarCajas?.tag = position else ivSumarCajas?.visibility = View.GONE
        val ivRestarCajas = vi?.findViewById<ImageView>(R.id.imvCajasMenos)
        if (fVendiendo) ivRestarCajas?.tag = position else ivRestarCajas?.visibility = View.GONE
        val ivArticulo = vi?.findViewById<ImageView>(R.id.imvArticulo)
        ivArticulo?.tag = position
        val tvCajas = vi?.findViewById<TextView>(R.id.edtCajas)
        tvCajas?.tag = position
        val tvUnidades = vi?.findViewById<TextView>(R.id.edtUnidades)
        tvUnidades?.tag = position
    }

    private fun cargarEtiquetasHco(item: ItemArticulo, vi: View?) {
        val tvExistencias = vi?.findViewById<TextView>(R.id.tvExistencias)
        val tvUdsCaja = vi?.findViewById<TextView>(R.id.tvUnidadesCaja)
        val tvPrCajas = vi?.findViewById<TextView>(R.id.tvPrCajas)
        val tvDtCajas = vi?.findViewById<TextView>(R.id.tvDtCajas)
        val tvPrecio = vi?.findViewById<TextView>(R.id.tvPrecio)
        val tvDto = vi?.findViewById<TextView>(R.id.tvDescuento)
        val edtCajas = vi?.findViewById<EditText>(R.id.edtCajas)
        val imvCjMas = vi?.findViewById<ImageView>(R.id.imvCajasMas)
        val imvCjMenos = vi?.findViewById<ImageView>(R.id.imvCajasMenos)

        // Cantidad en hco.
        if (item.cantHco != 0.0) {
            tvExistencias?.text = item.cantHco.toString()
            tvExistencias?.alpha = 1f
        } else {
            tvExistencias?.text = "--"
            tvExistencias?.alpha = 0.2f
        }
        // Cajas en hco.
        if (item.cajasHco != 0.0) {
            tvUdsCaja?.text = item.cajasHco.toString()
            tvUdsCaja?.alpha = 1f
        } else {
            tvUdsCaja?.text = "--"
            tvUdsCaja?.alpha = 0.2f
        }

        // Fecha en hco. Aprovechamos el textview que tenemos para la tarifa de cajas. Le quitamos el icono para que quepa la fecha.
        tvPrCajas?.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        item.fechaHco
        tvPrCajas?.text = item.fechaHco
        tvPrCajas?.alpha = 1f

        // Limpiamos el textview del descuento de cajas.
        tvDtCajas?.visibility = View.INVISIBLE

        // Precio en hco.
        item.prHco
        val sPrecio = item.prHco.replace(',', '.')
        var dPrecio = Redondear(sPrecio.toDouble(), fDecPrBase)
        // Si vendemos iva incluído tendremos que calcular el precio con iva.
        if (fIvaIncluido) {
            val dImpIva = dPrecio * item.porcIva / 100
            dPrecio += dImpIva
        }
        tvPrecio?.text = String.format(Locale.getDefault(), fFtoPrecio, dPrecio) + " €"

        // % Dto. en hco.
        item.dtoHco
        val sDto = item.dtoHco.replace(',', '.')
        val dDto = sDto.toDouble()
        if (dDto != 0.0) {
            tvDto?.text = String.format(Locale.getDefault(), "%.2f", dDto)
            tvDto?.alpha = 1f
        } else {
            tvDto?.text = "--"
            tvDto?.alpha = 0.2f
        }

        // Unidades por caja
        item.undCaja
        val sUdsCaja = item.undCaja.replace(',', '.')
        val dUdsCaja = sUdsCaja.toDouble()
        if (dUdsCaja != 0.0) {
            if (fVendiendo) {
                // Activamos los controles para vender por cajas.
                imvCjMas?.isEnabled = true
                imvCjMas?.alpha = 1f
                imvCjMenos?.isEnabled = true
                imvCjMenos?.alpha = 1f
                edtCajas?.isEnabled = true
            }
        } else {
            if (fVendiendo) {
                // Si el artículo no tiene unidades por caja desactivamos también los controles para vender por cajas.
                imvCjMas?.isEnabled = false
                imvCjMas?.alpha = 0.2f
                imvCjMenos?.isEnabled = false
                imvCjMenos?.alpha = 0.2f
                edtCajas?.isEnabled = false
            }
        }
    }

    private fun cargarEtiquetas(item: ItemArticulo, vi: View?) {
        val tvExistencias = vi?.findViewById<TextView>(R.id.tvExistencias)
        val tvUdsCaja = vi?.findViewById<TextView>(R.id.tvUnidadesCaja)
        val tvPrCajas = vi?.findViewById<TextView>(R.id.tvPrCajas)
        val tvDtCajas = vi?.findViewById<TextView>(R.id.tvDtCajas)
        val tvPrecio = vi?.findViewById<TextView>(R.id.tvPrecio)
        val tvDto = vi?.findViewById<TextView>(R.id.tvDescuento)
        val edtCajas = vi?.findViewById<EditText>(R.id.edtCajas)
        val edtUnidades = vi?.findViewById<EditText>(R.id.edtUnidades)
        val imvCjMas = vi?.findViewById<ImageView>(R.id.imvCajasMas)
        val imvCjMenos = vi?.findViewById<ImageView>(R.id.imvCajasMenos)
        if (!fVendiendo) {
            edtCajas?.isEnabled = false
            edtUnidades?.isEnabled = false
        }

        // Existencias
        if (item.stock != 0.0) tvExistencias?.text = item.stock.toString() else {
            tvExistencias?.text = "--"
            tvExistencias?.alpha = 0.2f
        }

        // Precio/precio en oferta, precio anterior y descuento/descuento en oferta.
        item.prClte
        run {
            val sPrecio = item.prClte?.replace(',', '.') ?: "0.0"
            var dPrecio = Redondear(sPrecio.toDouble(), fDecPrBase)
            // Si vendemos iva incluído tendremos que calcular el precio con iva.
            if (fIvaIncluido) {
                val dImpIva = dPrecio * item.porcIva / 100
                dPrecio += dImpIva
            }
            tvPrecio?.text = String.format(Locale.getDefault(), fFtoPrecio, dPrecio) + " €"
        }

        // % Dto.
        item.dto
        run {
            val sDto = item.dto?.replace(',', '.') ?: "0.0"
            val dDto = sDto.toDouble()
            if (dDto != 0.0) {
                tvDto?.text = String.format(Locale.getDefault(), "%.2f", dDto)
                tvDto?.alpha = 1f
            } else {
                tvDto?.text = "--"
                tvDto?.alpha = 0.2f
            }
        }

        // Unidades por caja
        item.undCaja
        val sUdsCaja = item.undCaja.replace(',', '.')
        val dUdsCaja = sUdsCaja.toDouble()
        if (dUdsCaja != 0.0) {
            tvUdsCaja?.text = item.undCaja + " Ud"
            tvUdsCaja?.alpha = 1f
            if (fVendiendo) {
                // Activamos los controles para vender por cajas.
                imvCjMas?.isEnabled = true
                imvCjMas?.alpha = 1f
                imvCjMenos?.isEnabled = true
                imvCjMenos?.alpha = 1f
                edtCajas?.isEnabled = true
            }
        } else {
            tvUdsCaja?.text = "--"
            tvUdsCaja?.alpha = 0.2f
            if (fVendiendo) {
                // Si el artículo no tiene unidades por caja desactivamos también los controles para vender por cajas.
                imvCjMas?.isEnabled = false
                imvCjMas?.alpha = 0.2f
                imvCjMenos?.isEnabled = false
                imvCjMenos?.alpha = 0.2f
                edtCajas?.isEnabled = false
            }
        }

        // Tarifa cajas
        item.prCajas
        val sPrCajas = item.prCajas?.replace(',', '.') ?: "0.0"
        var dPrecio = Redondear(sPrCajas.toDouble(), fDecPrBase)
        if (fIvaIncluido) {
            val dImpIva = dPrecio * item.porcIva / 100
            dPrecio += dImpIva
        }
        if (dPrecio != 0.0) {
            tvPrCajas?.text = String.format(Locale.getDefault(), fFtoPrecio, dPrecio) + " €"
            tvPrCajas?.alpha = 1f
        } else {
            tvPrCajas?.text = "--"
            tvPrCajas?.alpha = 0.2f
        }

        // % Dto cajas.
        item.dtoCajas
        run {
            val sDto = item.dtoCajas?.replace(',', '.') ?: "0.0"
            val dDto = sDto.toDouble()
            if (dDto != 0.0) {
                tvDtCajas?.text = String.format(Locale.getDefault(), "%.2f", dDto)
                tvDtCajas?.alpha = 1f
            } else {
                tvDtCajas?.text = "--"
                tvDtCajas?.alpha = 0.2f
            }
        }

        // Si el artículo está en oferta, presentaremos el precio de la oferta en la casilla del precio y el precio
        // sin oferta en la casilla tvPrAnt. El descuento, sea de oferta o no, lo presentaremos siempre en la casilla tvDto.
        if (item.tieneOferta()) {
            val sPrecio = item.prOfta.replace(',', '.')
            var dPrecioOfta = Redondear(sPrecio.toDouble(), fDecPrBase)
            if (dPrecioOfta != 0.0) {
                if (fIvaIncluido) {
                    val dImpIva = dPrecioOfta * item.porcIva / 100
                    dPrecioOfta += dImpIva
                }
                tvPrecio?.text = String.format(fFtoPrecio, dPrecioOfta)
            } else {
                item.dtoOfta
                val sDto = item.dtoOfta.replace(',', '.')
                val dDto = sDto.toDouble()
                if (dDto != 0.0) {
                    tvDto?.text = String.format(Locale.getDefault(), "%.2f", dDto)
                    tvDto?.alpha = 1f
                } else {
                    tvDto?.text = "--"
                    tvDto?.alpha = 0.2f
                }
            }
        }
    }
}