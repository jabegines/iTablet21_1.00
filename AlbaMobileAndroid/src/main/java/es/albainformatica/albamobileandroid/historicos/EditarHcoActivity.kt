package es.albainformatica.albamobileandroid.historicos

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.KeyEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.maestros.AlmacenesClase
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.ventas.*
import java.util.*


class EditarHcoActivity: AppCompatActivity() {
    private lateinit var fDocumento: Documento
    private var fHistorico = Comunicador.fHistorico
    private lateinit var fHistMes: HistoricoMes
    private var fConfiguracion = Comunicador.fConfiguracion
    private lateinit var fArticulos: ArticulosClase
    private var fAlmacenes = AlmacenesClase(this)

    private var fPedirCajas: Boolean = false
    private var fPedirDto: Boolean = false
    private var fPedirDtoEur: Boolean = false
    private var fIvaIncluido: Boolean = false
    private var fUsarTasa1: Boolean = false
    private var fUsarTasa2: Boolean = false
    private var fLinea = 0
    private var fPosicion = 0
    private var fFtoDecCantidad: String = ""
    private var fFtoDecPrBase: String = ""
    private var fFtoDecPrII: String = ""
    private var fDesdeHcoArtClte: Boolean = false
    private var fEditAlmEnabled: Boolean = false
    private var fTarifaAnt: Short = 0
    private var fVendTrfPiezas = false


    // EditText's de la actividad.
    private lateinit var tvArticulo: TextView
    private lateinit var edtAlmacen: EditText
    private lateinit var edtCajas: EditText
    private lateinit var edtCantidad: EditText
    private lateinit var edtPiezas: EditText
    private lateinit var edtPrecio: EditText
    private lateinit var edtDto: EditText
    private lateinit var edtDtoI: EditText
    private lateinit var tvPiezas: TextView
    private lateinit var tvDtoI: TextView
    private lateinit var tvPrNeto: TextView
    private lateinit var edtLote: EditText
    private lateinit var tvLote: TextView
    private lateinit var imgLote: ImageView
    private lateinit var imgTrfPiezas: ImageView

    private var fFlag5 = 0
    private var fEmpresaActual: Short = 0

    private val fRequestTextoLinea = 1
    private val fRequestBuscarLote = 2
    private val fRequestPedirDosis = 3


    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.editar_hco)
        val intent = intent

        fLinea = intent.getIntExtra("linea", 0)
        fPosicion = intent.getIntExtra("posicion", 0)
        fDesdeHcoArtClte = intent.getBooleanExtra("desdeHcoArtClte", false)
        fEmpresaActual = intent.getShortExtra("empresa", 0)
        fVendTrfPiezas = false

        fHistMes = HistoricoMes(this)
        fArticulos = ArticulosClase(this)

        // Nos posicionamos en el articulo de la linea del historico (nos servira para calcular las unidades por caja, etc.).
        if (fDesdeHcoArtClte) {
            fDocumento = Comunicador.fDocumento
            fArticulos.existeArticulo(intent.getIntExtra("articuloId", 0))
        } else {
            fArticulos.existeArticulo(fHistorico.lDatosHistorico[fPosicion].articuloId)
        }

        // Vemos si tenemos que pedir las dosis
        val fPedirDosis = fConfiguracion.hayElaboracionLacteos() && fConfiguracion.usarPiezas() && fArticulos.venderPorDosis() && fArticulos.usarPiezas()
        inicializarControles()

        if (fPedirDosis) {
            val i = Intent(this, PedirDosis::class.java)
            i.putExtra("articulo", fArticulos.fArticulo)
            startActivityForResult(i, fRequestPedirDosis)
        } else {
            fHistorico.inicializarLinea()
            setArticulo()
            // Vemos qu?? control es el primero en obtener el foco
            darFocoAEdit()
        }
    }


    private fun inicializarControles() {
        fFtoDecCantidad = fConfiguracion.formatoDecCantidad()
        fPedirCajas = fConfiguracion.pedirCajas()
        fPedirDto = fConfiguracion.pedirDtos()
        fPedirDtoEur = fConfiguracion.dtosCascada()
        fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresaActual)
        fUsarTasa1 = fConfiguracion.usarTasa1()
        fUsarTasa2 = fConfiguracion.usarTasa2()
        fFtoDecPrBase = fConfiguracion.formatoDecPrecioBase()
        fFtoDecPrII = fConfiguracion.formatoDecPrecioIva()
        tvArticulo = findViewById(R.id.tvHco_Articulo)
        edtAlmacen = findViewById(R.id.edtHco_Almacen)
        edtCajas = findViewById(R.id.edtHco_Cajas)
        edtCantidad = findViewById(R.id.edtHco_Cant)
        edtPiezas = findViewById(R.id.edtHco_Piezas)
        edtPrecio = findViewById(R.id.edtHco_Precio)
        edtDto = findViewById(R.id.edtHco_Dto)
        edtDtoI = findViewById(R.id.edtHco_DtoEur)
        // Por ahora desactivamos la edici??n del dto. por importe, igual que hacemos cuando vendemos normal, ya que este
        // descuento se transformar?? en un descuento en cascada. Al vender normal no permitimos modificar los dtos. en cascada
        // que vienen de este tipo de descuento.
        edtDtoI.isEnabled = false
        tvPiezas = findViewById(R.id.edtHco_tvPiezas)
        tvDtoI = findViewById(R.id.tvHco_CapDtEur)
        tvPrNeto = findViewById(R.id.tvHco_PrNeto)
        tvPrNeto.text = ""
        val tvTxtArtHabit = findViewById<TextView>(R.id.edtHco_txtarthabit)
        edtLote = findViewById(R.id.edtHco_Lote)
        tvLote = findViewById(R.id.tvHco_Lote)
        imgLote = findViewById(R.id.imgHco_Lote)
        imgTrfPiezas = findViewById(R.id.imgTarifaPiezas)
        imgTrfPiezas.visibility = View.GONE

        // fEditAlmEnabled nos servir?? para saber si tenemos el control edtAlmacen enabled o no
        fEditAlmEnabled = fConfiguracion.pedirAlmPorLinPresup() && fDocumento.fTipoDoc == TIPODOC_PRESUPUESTO
        if (!fEditAlmEnabled) {
            val lyAlmacen = findViewById<LinearLayout>(R.id.lyHco_Almacen)
            lyAlmacen.visibility = View.GONE
        }
        prepararEdits()
        // Vemos qu?? control es el primero en obtener el foco
        //darFocoAEdit();
        tvTxtArtHabit.text = ""
        if (!fDesdeHcoArtClte) {
            // Mostramos el posible texto de art??culo habitual.
            val txtArtHabit = fHistorico.lDatosHistorico[fPosicion].texto
            if (txtArtHabit != "") tvTxtArtHabit.text = txtArtHabit
        }

        // Mostramos los acumulados.
        verAcumulados()

        // Si no vamos a usar el lote vemos si el art??culo ya est?? en el documento para mostrar el correspondiente mensaje
        if (fDesdeHcoArtClte && edtLote.visibility == View.GONE) {
            if (fDocumento.existeLineaArticulo(fArticulos.fArticulo) > 0) {
                avisoArtEnDoc()
            }
        }
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.editar_hco)
    }


    private fun darFocoAEdit() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val fVerLote = edtLote.visibility == View.VISIBLE

        // Si el articulo tiene unidades por caja, pedimos la cajas. En caso
        // contrario, no. Siempre y cuando tengamos configurado pedir cajas.
        if (fPedirCajas) {
            if (fArticulos.fUCaja != 0.0) {
                if (!fVerLote) {
                    edtCajas.requestFocus()
                    imm.showSoftInput(edtCajas, 0)
                }
                if (edtPiezas.isEnabled) edtCajas.nextFocusDownId =
                    edtPiezas.id else edtCajas.nextFocusDownId =
                    edtCantidad.id
            } else {
                edtCajas.isEnabled = false
                if (!fVerLote) {
                    if (edtPiezas.isEnabled) {
                        edtPiezas.requestFocus()
                        imm.showSoftInput(edtPiezas, 0)
                    } else {
                        edtCantidad.requestFocus()
                        imm.showSoftInput(edtCantidad, 0)
                    }
                }
            }
        } else {
            if (!fVerLote) {
                if (edtPiezas.isEnabled) {
                    edtPiezas.requestFocus()
                    imm.showSoftInput(edtPiezas, 0)
                } else {
                    edtCantidad.requestFocus()
                    imm.showSoftInput(edtCantidad, 0)
                }
            }
        }
        if (fEditAlmEnabled) {
            edtAlmacen.requestFocus()
            if (edtCajas.isEnabled) {
                edtAlmacen.nextFocusDownId = edtCajas.id
            } else {
                if (edtPiezas.isEnabled) {
                    edtAlmacen.nextFocusDownId = edtPiezas.id
                } else {
                    edtAlmacen.nextFocusDownId = edtCantidad.id
                }
            }
        }
    }


    private fun verAcumulados() {
        val tvAcumCantAnt = findViewById<TextView>(R.id.edtHco_AcumCantAnt)
        val tvAcumCant = findViewById<TextView>(R.id.edtHco_AcumCant)
        val tvAcumDif = findViewById<TextView>(R.id.edtHco_AcumDiferencia)

        fHistMes.abrirClteArt(fDocumento.fCliente, fArticulos.fArticulo)

        val fecha = Calendar.getInstance()
        val anyo = fecha[Calendar.YEAR]
        val nombreMes: String = if (fHistMes.lDatosHistMes.isNotEmpty())
            dimeNombreMesResum(fHistMes.lDatosHistMes[0].mes - 1)
        else
            dimeNombreMesResum(fecha[Calendar.MONTH])

        // Etiquetamos los nombres de los meses
        val tvCantAnt = findViewById<TextView>(R.id.edtHco_TitCantAnt)
        val tvCantAct = findViewById<TextView>(R.id.edtHco_TitCant)
        if (tvCantAnt != null) {
            val queCantAnt = nombreMes + " " + (anyo - 1)
            tvCantAnt.text = queCantAnt
        }
        if (tvCantAct != null) {
            val queCantAct = "$nombreMes $anyo"
            tvCantAct.text = queCantAct
        }
        var sCantidad: String
        var dCantidad = 0.0
        var dCantidadAnt = 0.0
        if (fHistMes.lDatosHistMes.isNotEmpty()) {
            sCantidad = fHistMes.lDatosHistMes[0].cantidadAnt.replace(',', '.')
            dCantidadAnt = sCantidad.toDouble()
            tvAcumCantAnt.text = String.format(fFtoDecCantidad, dCantidadAnt)

            sCantidad = fHistMes.lDatosHistMes[0].cantidad.replace(',', '.')
            dCantidad = sCantidad.toDouble()
            tvAcumCant.text = String.format(fFtoDecCantidad, dCantidad)

        } else {
            if (tvCantAnt != null) tvAcumCantAnt.text = String.format(fFtoDecCantidad, dCantidadAnt)
            if (tvAcumCant != null) tvAcumCant.text = String.format(fFtoDecCantidad, dCantidad)
        }
        if (tvAcumDif != null) tvAcumDif.text = String.format(fFtoDecCantidad, dCantidad - dCantidadAnt)
    }


    private fun prepararEdits() {
        prepararCajas()
        if (fEditAlmEnabled) prepararAlmacen()
        prepararDtos()
        prepararLote()
        prepararPiezas()

        // Establecemos los eventos que controlan los formatos de la cantidad y el precio.
        edtCantidad.keyListener = DigitsKeyListener.getInstance(true, true)
        edtPrecio.keyListener = DigitsKeyListener.getInstance(true, true)
        // Permiso para modificar precios.
        if (!fConfiguracion.modificarPrecios()) {
            edtPrecio.isEnabled = false
            edtPrecio.isFocusable = false
            // Si no pedimos precio y si dtos., hacemos el nextfocusdown a edtCantidad.
            if (fConfiguracion.pedirDtos()) edtCantidad.nextFocusDownId = edtDto.id
        }
    }

    private fun prepararAlmacen() {
        edtAlmacen.setOnKeyListener { v: View, keyCode: Int, event: KeyEvent ->
            if (event.action == KeyEvent.ACTION_DOWN
                && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
            ) {
                val edtCodAlmacen = v as EditText
                if (fAlmacenes.existe(edtCodAlmacen.text.toString())) {
                    edtCodAlmacen.setText(
                        ponerCeros(
                            edtCodAlmacen.text.toString(),
                            ancho_cod_almacen
                        )
                    )

                    // Buscamos el siguiente control al que tenemos que darle el foco.
                    val queId = edtCodAlmacen.nextFocusDownId
                    val queView = findViewById<View>(queId)
                    queView?.requestFocus()
                    return@setOnKeyListener true
                } else {
                    MsjAlerta(this@EditarHcoActivity).alerta(resources.getString(R.string.msj_CodNoExiste))
                    edtCodAlmacen.setText("")
                    edtCodAlmacen.requestFocus()
                    return@setOnKeyListener false
                }
            }
            false
        }
    }


    private fun prepararPiezas() {
        if (!fConfiguracion.usarPiezas()) {
            tvPiezas.visibility = View.GONE
            edtPiezas.visibility = View.GONE
            edtPiezas.isEnabled = false
        } else {
            if (!fArticulos.usarPiezas()) {
                edtPiezas.isEnabled = false
            }
        }
    }


    private fun prepararLote() {
        if (fDocumento.fTipoDoc == TIPODOC_FACTURA || fDocumento.fTipoDoc == TIPODOC_ALBARAN) {
            if (!fConfiguracion.usarTrazabilidad() || !fArticulos.controlaTrazabilidad()) {
                tvLote.visibility = View.GONE
                edtLote.visibility = View.GONE
                imgLote.visibility = View.GONE
            }
        } else {
            tvLote.visibility = View.GONE
            edtLote.visibility = View.GONE
            imgLote.visibility = View.GONE
        }
        if (fDesdeHcoArtClte) {
            // Si el lote est?? visible buscaremos si el art??culo y el lote ya est??n en el documento para presentar el mensaje.
            if (edtLote.visibility == View.VISIBLE) {
                edtLote.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                        if (!hasFocus && edtLote.text.toString() != "") {
                            if (fDocumento.existeArtYLote(fArticulos.fArticulo, edtLote.text.toString())) {
                                avisoArtLoteEnDoc()
                            }
                        }
                    }
            }
        }
    }


    private fun prepararCajas() {
        // Pediremos cajas siempre que este configurado.
        if (fPedirCajas) edtCajas.setOnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
            if (event.action == KeyEvent.ACTION_DOWN
                && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
            ) {
                calcularCantidad()
                return@setOnKeyListener true
            }
            false
        } else edtCajas.isEnabled = false

        // Establecemos el evento que controla el formato del edit.
        edtCajas.addTextChangedListener(
            NumberTextWatcher(
                edtCajas,
                fConfiguracion.enterosCajas(),
                fConfiguracion.decimalesCantidad()
            )
        )
    }

    private fun calcularCantidad() {
        var sCajas = edtCajas.text.toString().replace(',', '.')
        if (sCajas == "") sCajas = "0.0"
        val numCajas = sCajas.toDouble()
        fHistorico.fCantidad = fArticulos.fUCaja * numCajas
        edtCantidad.setText(fHistorico.fCantidad.toString())
    }

    private fun prepararDtos() {
        if (!fPedirDto) {
            edtDto.isEnabled = false
            edtDto.isFocusable = false
        }

        // Por ahora desactivo el descuento por importe, ya que no es un descuento en cascada. Habr??a que hacerlo
        // bien, o sea, pidiendo los descuentos en cascada desde esta actividad.
        if (!fPedirDto || !fPedirDtoEur) {
            edtDtoI.isFocusable = false
            edtDtoI.visibility = View.GONE
            tvDtoI.visibility = View.GONE
        }
        edtDto.addTextChangedListener(NumberTextWatcher(edtDto, 2, 2))
        edtDtoI.addTextChangedListener(NumberTextWatcher(edtDtoI, 4, 2))
    }


    @SuppressLint("Range")
    private fun setArticulo() {
        fDocumento.fArticulo = fArticulos.fArticulo
        fDocumento.fCodArt = fArticulos.fCodigo
        fDocumento.fDescr = fArticulos.fDescripcion
        fDocumento.fCodigoIva = fArticulos.fCodIva
        // Vemos las posibles tasas de la l??nea. Antes inicializamos para no tener problemas con valores nulos. Idem con el formato.
        fDocumento.fTasa1 = 0.0
        fDocumento.fTasa2 = 0.0
        fDocumento.fPrecioRating = false
        fDocumento.fArtEnOferta = false
        fDocumento.fArtSinCargo = false
        fHistorico.fFormatoLin = 0
        if (fDocumento.fAplicarIva) {
            if (fUsarTasa1) fDocumento.fTasa1 = fArticulos.fTasa1
            if (fUsarTasa2) fDocumento.fTasa2 = fArticulos.fTasa2
        }
        val queDescr = fDocumento.fCodArt + " - " + fDocumento.fDescr
        tvArticulo.text = queDescr
        if (fDesdeHcoArtClte) {
            fDocumento.fFormatoLin = 0
        } else {
            // Si la l??nea del hist??rico tiene formato, mantenemos el mismo.
            if (fHistorico.lDatosHistorico[fPosicion].formatoId > 0)
                fHistorico.fFormatoLin = fHistorico.lDatosHistorico[fPosicion].formatoId
            fDocumento.fFormatoLin = fHistorico.fFormatoLin

            // Si la l??nea tiene formato, adjuntaremos la descripci??n del mismo a la del art??culo.
            if (fHistorico.lDatosHistorico[fPosicion].descrFto != null) {
                val fDescrFto = fHistorico.lDatosHistorico[fPosicion].descrFto ?: ""
                val queDescrFto = tvArticulo.text.toString() + " " + fDescrFto
                tvArticulo.text = queDescrFto
            }
        }

        // Si trabajamos con art??culos habituales grabamos el texto del art??culo en el de la l??nea.
        // Si el art??culo tiene texto resaltamos en rojo el t??tulo del bot??n para que el vendedor se percate.
        if (fDocumento.fHayArtHabituales) {
            fDocumento.fTextoLinea = fDocumento.textoArtHabitual()
        }

        // Calculamos precio y dto. una vez que ya hemos escogido la tarifa.
        tvPrNeto.text = ""
        fDocumento.calculaPrecioYDto(fArticulos.fGrupo, fArticulos.fDepartamento, fArticulos.fCodProv, fArticulos.fPorcIva)
        // Mostramos precio y dto.
        if (fIvaIncluido) {
            edtPrecio.setText(String.format(fConfiguracion.formatoDecPrecioIva(), fDocumento.fPrecioII))
        } else
            edtPrecio.setText(String.format(fConfiguracion.formatoDecPrecioBase(), fDocumento.fPrecio))

        // Si tenemos un descuento por importe (desde el rating), lo a??adiremos al control edtDtoI
        if (fDocumento.fDtoRatingImp != 0.0) {
            if (fIvaIncluido) {
                edtDtoI.setText(String.format(fFtoDecPrII, fDocumento.fDtoRatingImp))
                // Mostramos el precio neto.
                var fDtoRatingImpII = fDocumento.fDtoRatingImp + fDocumento.fDtoRatingImp * fDocumento.fPorcIva / 100
                fDtoRatingImpII = redondear(fDtoRatingImpII, 2)
                tvPrNeto.text = String.format(fFtoDecPrII, fDocumento.fPrecioII - fDtoRatingImpII)
            } else {
                edtDtoI.setText(String.format(fFtoDecPrBase, fDocumento.fDtoRatingImp))
                tvPrNeto.text = String.format(fFtoDecPrBase, fDocumento.fPrecio - fDocumento.fDtoRatingImp)
            }
        }
        edtDto.setText(String.format(Locale.getDefault(), "%.2f", fDocumento.fDtoLin))
    }

    fun cancelarHco(view: View?) {
        view?.getTag(0)          // Para que no d?? warning el compilador

        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }

    private fun avisoArtLoteEnDoc() {
        val aldDialog = nuevoAlertBuilder(
            this, "Art??culo y lote vendido",
            "El art??culo con este lote ya est?? en el documento, ??continuar?", true
        )
        aldDialog.setPositiveButton(getString(R.string.dlg_si)) { _: DialogInterface?, _: Int -> }
        aldDialog.setNegativeButton(getString(R.string.dlg_no)) { _: DialogInterface?, _: Int -> cancelarHco(null) }
        val alert = aldDialog.create()
        alert.show()
    }

    private fun avisoArtEnDoc() {
        val aldDialog = nuevoAlertBuilder(
            this, "Art??culo vendido",
            "El art??culo ya est?? en el documento, ??continuar?", true
        )
        aldDialog.setPositiveButton(getString(R.string.dlg_si)) { _: DialogInterface?, _: Int -> }
        aldDialog.setNegativeButton(getString(R.string.dlg_no)) { _: DialogInterface?, _: Int -> cancelarHco(null) }
        val alert = aldDialog.create()
        alert.show()
    }


    fun salvarHco(view: View) {
        view.getTag(0)          // Para que no d?? warning el compilador

        val queView = currentFocus
        // Si pulsamos Aceptar estando en edtCajas, calculamos la cantidad antes de salir.
        if (queView != null && queView.id == edtCajas.id) calcularCantidad()
        var sCantidad = edtCantidad.text.toString().replace(',', '.')
        if (sCantidad == "") sCantidad = "0.0"
        val dCantidad = sCantidad.toDouble()
        val continuar = dCantidad >= 0 || !fConfiguracion.noVenderNeg()

        if (continuar) {
            if (fEditAlmEnabled) {
                fHistorico.fAlmacPedido = edtAlmacen.text.toString()
            }
            if (edtCajas.isEnabled) {
                val sCajas = edtCajas.text.toString().replace(',', '.')
                if (sCajas != "") fHistorico.fCajas = sCajas.toDouble()
            }
            fHistorico.fCantidad = dCantidad
            if (edtPiezas.isEnabled) {
                val sPiezas = edtPiezas.text.toString().replace(',', '.')
                if (sPiezas != "") fHistorico.fPiezas = sPiezas.toDouble()
            }
            val sPrecio = edtPrecio.text.toString().replace(',', '.')
            if (sPrecio != "") {
                if (fIvaIncluido) {
                    // Si tenemos el flag de precio de rating (fPrecioRating) y hemos modificado el precio, quitaremos dicho flag.
                    if (fDocumento.fPrecioRating) {
                        if (fDocumento.fPrecioII != sPrecio.toDouble()) fDocumento.fPrecioRating = false
                    }
                    fDocumento.fPrecioII = sPrecio.toDouble()
                    fDocumento.calculaPrBase()
                } else {
                    // Si tenemos el flag de precio de rating (fPrecioRating) y hemos modificado el precio, quitaremos dicho flag.
                    if (fDocumento.fPrecioRating) {
                        if (fDocumento.fPrecio != sPrecio.toDouble()) fDocumento.fPrecioRating = false
                    }
                    fDocumento.fPrecio = sPrecio.toDouble()
                    fDocumento.calculaPrecioII()
                }
                fHistorico.fPrecio = fDocumento.fPrecio
                fHistorico.fPrecioII = fDocumento.fPrecioII
                fHistorico.fTasa1 = fDocumento.fTasa1
                fHistorico.fTasa2 = fDocumento.fTasa2
            }
            fHistorico.fArticulo = fDocumento.fArticulo
            fHistorico.fCodigo = fDocumento.fCodArt
            fHistorico.fDescr = fDocumento.fDescr
            fHistorico.fHayArtHabituales = fDocumento.fHayArtHabituales
            if (fDocumento.fTextoLinea != "")
                fHistorico.fTextoLinea = fDocumento.fTextoLinea
            else
                fHistorico.fTextoLinea = ""
            fHistorico.fLote = edtLote.text.toString()

            val sDto = edtDto.text.toString().replace(',', '.')
            if (sDto != "") fHistorico.fDtoLin = sDto.toDouble()
            val sDtoEur = edtDtoI.text.toString().replace(',', '.')
            if (sDtoEur != "") {
                if (fIvaIncluido) {
                    fDocumento.fDtoImpII = sDtoEur.toDouble()
                    fDocumento.calcularDtoImpBase()
                } else {
                    fDocumento.fDtoImp = sDtoEur.toDouble()
                    fDocumento.calcularDtoImpII()
                }
                fHistorico.fDtoImp = fDocumento.fDtoImp
                fHistorico.fDtoImpII = fDocumento.fDtoImpII
            }
            fHistorico.fCodigoIva = fArticulos.fCodIva
            var queFlag = 0
            if (fDocumento.fArtEnOferta) queFlag = FLAGLINEAVENTA_ARTICULO_EN_OFERTA
            if (fDocumento.fArtSinCargo) queFlag = queFlag or FLAGLINEAVENTA_SIN_CARGO
            if (fDocumento.fPrecioRating) queFlag = queFlag or FLAGLINEAVENTA_PRECIO_RATING
            var queFlag3 = 0
            if (fVendTrfPiezas) {
                queFlag3 = queFlag3 or FLAG3LINEAVENTA_PRECIO_POR_PIEZAS
                queFlag3 = queFlag3 or FLAG3LINEAVENTA_ARTICULO_POR_PIEZAS
                queFlag = queFlag or FLAGLINEAVENTA_CAMBIAR_TARIFA_PRECIO
                queFlag = queFlag or FLAGLINEAVENTA_CAMBIAR_DESCRIPCION
            }
            fHistorico.fFlag = queFlag
            fHistorico.fFlag3 = queFlag3
            fHistorico.fFlag5 = fFlag5
            fHistorico.aceptarCambios(fLinea)

            val returnIntent = Intent()
            setResult(RESULT_OK, returnIntent)
            finish()
        } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoVenderNeg))
    }


    fun textoLinea(view: View) {
        view.getTag(0)          // Para que no d?? warning el compilador

        if (fDocumento.fArticulo > 0) {
            val i = Intent(this, TextoLinea::class.java)
            i.putExtra("textolinea", fDocumento.fTextoLinea)
            i.putExtra("salvar", false)
            startActivityForResult(i, fRequestTextoLinea)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fRequestTextoLinea) {
            if (resultCode == RESULT_OK) {
                fDocumento.fTextoLinea = data?.getStringExtra("textoLinea") ?: ""
                fFlag5 = data?.getIntExtra("flag5", 0) ?: 0
            }
        } else if (requestCode == fRequestBuscarLote) {
            if (resultCode == RESULT_OK) {
                val sQueLote = data?.getStringExtra("lote")
                edtLote.setText(sQueLote)
            }
        } else if (requestCode == fRequestPedirDosis) {
            if (resultCode == RESULT_OK) {
                val queFormatoId = data?.getIntExtra("formatoId", 0)
                val queNumPiezas = data?.getStringExtra("piezas")
                fHistorico.inicializarLinea()
                fHistorico.fFormatoLin = queFormatoId?.toShort() ?: 0
                if (queNumPiezas != "") fHistorico.fPiezas = queNumPiezas?.toDouble() ?: 0.0
                edtPiezas.setText(String.format(Locale.getDefault(), fFtoDecCantidad, fHistorico.fPiezas))
                setArticulo()
                // Vemos qu?? control es el primero en obtener el foco
                darFocoAEdit()
            }
        }
    }

    fun buscarLote(view: View) {
        view.getTag(0)          // Para que no d?? warning el compilador

        val i = Intent(this, BuscarLotes::class.java)
        i.putExtra("articulo", fDocumento.fArticulo)
        i.putExtra("formatocant", fConfiguracion.formatoDecCantidad())
        startActivityForResult(i, fRequestBuscarLote)
    }

    fun pedirDosis(view: View) {
        view.getTag(0)          // Para que no d?? warning el compilador

        val i = Intent(this, PedirDosis::class.java)
        i.putExtra("articulo", fArticulos.fArticulo)
        startActivityForResult(i, fRequestPedirDosis)
    }


    fun setTarifaPiezas(view: View) {
        view.getTag(0)          // Para que no d?? warning el compilador

        if (fVendTrfPiezas) {
            fDocumento.fTarifaLin = fTarifaAnt
            setArticulo()
            imgTrfPiezas.visibility = View.GONE
            fVendTrfPiezas = false
        } else {
            fTarifaAnt = fDocumento.fTarifaLin
            if (fDocumento.fClientes.fTrfPiezas > 0)
                fDocumento.fTarifaLin = fDocumento.fClientes.fTrfPiezas
            setArticulo()
            imgTrfPiezas.visibility = View.VISIBLE
            fVendTrfPiezas = true
        }
    }


    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val returnIntent = Intent()
            setResult(RESULT_OK, returnIntent)
            finish()

            // Si el listener devuelve true, significa que el evento est?? procesado, y nadie debe hacer nada m??s.
            return true
        }
        // Para las dem??s cosas, se reenv??a el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }


}