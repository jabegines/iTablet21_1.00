package es.albainformatica.albamobileandroid.biocatalogo

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.ventas.BuscarLotes
import es.albainformatica.albamobileandroid.ventas.Documento
import es.albainformatica.albamobileandroid.ventas.NumberTextWatcher
import kotlinx.android.synthetic.main.bio_vtadetallescat.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.yesButton
import java.util.*

class VtaDetallesCat: AppCompatActivity() {
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fArticulos: ArticulosClase
    private lateinit var fDocumento: Documento

    private var fFtoDecCantidad: String = ""
    private var fFtoDecPrBase: String = ""
    private var fFtoDecPrII: String = ""
    private var fPedirCajas: Boolean = false
    private var fPedirDto: Boolean = false
    private var fIvaIncluido: Boolean = false
    private var fUsarTasa1: Boolean = false
    private var fUsarTasa2: Boolean = false

    private var fEmpresaActual: Short = 0

    private lateinit var chsLineas: Array<CharSequence>
    private var queLinea: Int = 0


    private val fRequestBuscarLote = 1


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.bio_vtadetallescat)

        val intent = intent
        val fArticulo = intent.getIntExtra("articulo", 0)
        val fArtNumVecesEnDoc = intent.getIntExtra("numVecesEnDoc", 0)
        fEmpresaActual = intent.getShortExtra("empresa", 0)

        fDocumento = Comunicador.fDocumento
        fConfiguracion = Comunicador.fConfiguracion
        fArticulos = ArticulosClase(this)
        fArticulos.existeArticulo(fArticulo)

        fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresaActual)
        fUsarTasa1 = fConfiguracion.usarTasa1()
        fUsarTasa2 = fConfiguracion.usarTasa2()

        if (fArtNumVecesEnDoc > 1) {
            escogerLineaArt(fArticulo)
        } else {
            queLinea = fDocumento.existeLineaArticulo(fArticulos.fArticulo)
            setArticulo()
            inicializarControles()
        }


    }


    private fun escogerLineaArt(fArticulo: Int) {
        val listItems: MutableList<String> = ArrayList()

        if (fDocumento.lLineas.isNotEmpty()) {
            for (linea in fDocumento.lLineas) {
                if (linea.articuloId == fArticulo) {

                    if (fIvaIncluido && fDocumento.fClientes.fAplIva) {
                        listItems.add(linea.lineaId.toString() +
                                " - Cantidad: " + linea.cantidad +
                                " - Precio: " + linea.precioII +
                                " - Importe: " + linea.importeII)

                    } else {
                        listItems.add(linea.lineaId.toString() +
                                " - Cantidad: " + linea.cantidad +
                                " - Precio: " + linea.precio +
                                " - Importe: " + linea.importe)
                    }
                }
            }
        }

        chsLineas = listItems.toTypedArray()

        val altbld = AlertDialog.Builder(this)
        altbld.setTitle("Escoger línea")
        altbld.setSingleChoiceItems(chsLineas, 0) { _: DialogInterface, item: Int ->
            queLinea = chsLineas[item].toString().substringBefore(" -").toInt()
        }

        altbld
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                if (queLinea == 0)
                    queLinea = chsLineas[0].toString().substringBefore(" -").toInt()

                dialog.cancel()
                setArticulo()
                inicializarControles()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
                finish()
            }

        val alert = altbld.create()
        alert.setCancelable(false)
        alert.setCanceledOnTouchOutside(false)
        alert.show()
    }

    private fun inicializarControles() {
        fFtoDecCantidad = fConfiguracion.formatoDecCantidad()
        fFtoDecPrBase = fConfiguracion.formatoDecPrecioBase()
        fFtoDecPrII = fConfiguracion.formatoDecPrecioIva()

        fPedirCajas = fConfiguracion.pedirCajas()
        fPedirDto = fConfiguracion.pedirDtos()
        //fPedirDtoEur = fConfiguracion.dtosCascada()

        prepararEdits()
        // Vemos qué control es el primero en obtener el foco
        darFocoAEdit()
    }


    private fun prepararEdits() {
        prepararCajas()
        prepararDtos()
        prepararLote()
        prepararPiezas()

        // Establecemos los eventos que controlan los formatos de la cantidad y el precio.
        edtDet_Cant.keyListener = DigitsKeyListener.getInstance(true, true)
        edtDet_Precio.keyListener = DigitsKeyListener.getInstance(true, true)
        // Permiso para modificar precios.
        if (!fConfiguracion.modificarPrecios()) {
            edtDet_Precio.isEnabled = false
            edtDet_Precio.isFocusable = false
            // Si no pedimos precio y si dtos., hacemos el nextfocusdown a edtCantidad.
            if (fConfiguracion.pedirDtos()) edtDet_Cant.nextFocusDownId = edtDet_Dto.id
        }
    }


    private fun prepararCajas() {
        // Pediremos cajas siempre que este configurado.
        if (fPedirCajas) edtDet_Cajas.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN
                    && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
                calcularCantidad()
                return@OnKeyListener true
            }
            false
        }) else edtDet_Cajas.isEnabled = false

        // Establecemos el evento que controla el formato del edit.
        edtDet_Cajas.addTextChangedListener(NumberTextWatcher(edtDet_Cajas, fConfiguracion.enterosCajas(), fConfiguracion.decimalesCantidad()))
    }

    private fun calcularCantidad() {
        var sCajas: String = edtDet_Cajas.text.toString().replace(',', '.')
        if (sCajas == "") sCajas = "0.0"
        val numCajas = sCajas.toDouble()
        val fCantidad = fArticulos.fUCaja * numCajas

        edtDet_Cant.setText(fCantidad.toString())
    }

    private fun prepararDtos() {
        if (!fPedirDto) {
            edtDet_Dto.isEnabled = false
            edtDet_Dto.isFocusable = false
        }

        edtDet_Dto.addTextChangedListener(NumberTextWatcher(edtDet_Dto, 2, 2))
    }


    private fun prepararLote() {
        if (fDocumento.fTipoDoc == TIPODOC_FACTURA || fDocumento.fTipoDoc == TIPODOC_ALBARAN) {
            if (!fConfiguracion.usarTrazabilidad()) {
                tvDet_Lote.visibility = View.GONE
                edtDet_Lote.visibility = View.GONE
                imgDet_Lote.visibility = View.GONE
            }
        } else {
            tvDet_Lote.visibility = View.GONE
            edtDet_Lote.visibility = View.GONE
            imgDet_Lote.visibility = View.GONE
        }
    }


    private fun prepararPiezas() {
        if (!fConfiguracion.usarPiezas()) {
            tvDet_Piezas.visibility = View.GONE
            edtDet_Piezas.visibility = View.GONE
            edtDet_Piezas.isEnabled = false
        } else {
            if (!fArticulos.usarPiezas()) {
                edtDet_Piezas.isEnabled = false
            }
        }
    }


    private fun darFocoAEdit() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val fVerLote = edtDet_Lote.visibility == View.VISIBLE

        // Si el articulo tiene unidades por caja, pedimos la cajas. En caso
        // contrario, no. Siempre y cuando tengamos configurado pedir cajas.
        if (fPedirCajas) {
            if (fArticulos.fUCaja != 0.0) {
                if (!fVerLote) {
                    edtDet_Cajas.requestFocus()
                    imm.showSoftInput(edtDet_Cajas, 0)
                }
                if (edtDet_Piezas.isEnabled) edtDet_Cajas.nextFocusDownId = edtDet_Piezas.id else edtDet_Cajas.nextFocusDownId = edtDet_Cant.id
            } else {
                edtDet_Cajas.isEnabled = false
                if (!fVerLote) {
                    if (edtDet_Piezas.isEnabled) {
                        edtDet_Piezas.requestFocus()
                        imm.showSoftInput(edtDet_Piezas, 0)
                    } else {
                        edtDet_Cant.requestFocus()
                        imm.showSoftInput(edtDet_Cant, 0)
                    }
                }
            }
        } else {
            if (!fVerLote) {
                if (edtDet_Piezas.isEnabled) {
                    edtDet_Piezas.requestFocus()
                    imm.showSoftInput(edtDet_Piezas, 0)
                } else {
                    edtDet_Cant.requestFocus()
                    imm.showSoftInput(edtDet_Cant, 0)
                }
            }
        }
    }


    private fun setArticulo() {
        // Tenemos que comprobar si el artículo ya está incluido en el documento, para mantener el precio
        // que tenga la línea (puede que le hayamos modificado el precio después de salir del catálogo).
        // Si es así, tomaremos los datos que necesitamos de la línea del documento.

        if (queLinea > 0) {
            fDocumento.cargarLinea(queLinea)

        } else {
            fDocumento.fArticulo = fArticulos.fArticulo
            fDocumento.fCodArt = fArticulos.fCodigo
            fDocumento.fDescr = fArticulos.fDescripcion
            fDocumento.fCodigoIva = fArticulos.fCodIva
            fDocumento.fTasa1 = 0.0
            fDocumento.fTasa2 = 0.0
            // Vemos las posibles tasas de la línea. Antes inicializamos para no tener problemas con valores nulos. Idem con el formato.
            fDocumento.fPrecioRating = false
            fDocumento.fArtEnOferta = false
            fDocumento.fArtSinCargo = false
            fDocumento.fCajas = 0.0
            fDocumento.fPiezas = 0.0
            fDocumento.fCantidad = 0.0

            if (fDocumento.fAplicarIva) {
                if (fUsarTasa1) fDocumento.fTasa1 = fArticulos.fTasa1
                if (fUsarTasa2) fDocumento.fTasa2 = fArticulos.fTasa2
            }

            // Si trabajamos con artículos habituales grabamos el texto del artículo en el de la línea.
            if (fDocumento.fHayArtHabituales) {
                fDocumento.fTextoLinea = fDocumento.textoArtHabitual()
            }

            // Calculamos precio y dto. una vez que ya hemos escogido la tarifa.
            fDocumento.calculaPrecioYDto(fArticulos.fGrupo, fArticulos.fDepartamento, fArticulos.fCodProv, fArticulos.fPorcIva)
        }

        val fDescr = fArticulos.fCodigo + " - " + fArticulos.fDescripcion
        tvDet_Articulo.text = fDescr

        val fFtoDecCant = fConfiguracion.formatoDecCantidad()
        edtDet_Cajas.setText(String.format(fFtoDecCant, fDocumento.fCajas))
        edtDet_Piezas.setText(String.format(fFtoDecCant, fDocumento.fPiezas))
        edtDet_Cant.setText(String.format(fFtoDecCant, fDocumento.fCantidad))

        // Mostramos precio y dto.
        if (fIvaIncluido) edtDet_Precio.setText(String.format(fConfiguracion.formatoDecPrecioIva(), fDocumento.fPrecioII))
        else edtDet_Precio.setText(String.format(fConfiguracion.formatoDecPrecioBase(), fDocumento.fPrecio))

        edtDet_Dto.setText(String.format("%.2f", fDocumento.fDtoLin))
    }


    fun cancelarVtaDet(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(Activity.RESULT_CANCELED, returnIntent)
        finish()
    }

    fun salvarVtaDet(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val queView = currentFocus
        // Si pulsamos Aceptar estando en edtCajas, calculamos la cantidad antes de salir.
        if (queView != null && queView.id == edtDet_Cajas.id) calcularCantidad()

        val sCant = edtDet_Cant.text.toString().replace(',', '.')
        val sCajas = edtDet_Cajas.text.toString().replace(',', '.')
        val sPiezas = edtDet_Piezas.text.toString().replace(',', '.')
        val hayCant = sCant.toDouble() != 0.0
        val hayCajas = sCajas.toDouble() != 0.0
        val hayPiezas = sPiezas.toDouble() != 0.0

        val returnIntent = Intent()

        val dCantidad: Double = sCant.toDouble()
        if (dCantidad < 0 && fConfiguracion.noVenderNeg()) {
            alert(resources.getString(R.string.msj_NoVenderNeg)) {
                title = "Información"
                yesButton { setResult(Activity.RESULT_CANCELED, returnIntent) }
            }.show()

        } else {
            if (hayCant || hayCajas || hayPiezas) {
                returnIntent.putExtra("linea", queLinea)
                returnIntent.putExtra("cantidad", sCant)
                returnIntent.putExtra("cajas", sCajas)
                returnIntent.putExtra("piezas", sPiezas)
                returnIntent.putExtra("lote", edtDet_Lote.text.toString())
                returnIntent.putExtra("precio", edtDet_Precio.text.toString())
                returnIntent.putExtra("dto", edtDet_Dto.text.toString())
                setResult(Activity.RESULT_OK, returnIntent)
            } else setResult(Activity.RESULT_CANCELED, returnIntent)

            finish()
        }
    }


    fun buscarLote(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val i = Intent(this, BuscarLotes::class.java)
        i.putExtra("articulo", fArticulos.fArticulo)
        i.putExtra("formatocant", fConfiguracion.formatoDecCantidad())
        startActivityForResult(i, fRequestBuscarLote)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Actividad buscar articulos.
        if (requestCode == fRequestBuscarLote) {
            if (resultCode == Activity.RESULT_OK) {
                val sQueLote = data?.getStringExtra("lote")
                edtDet_Lote.setText(sQueLote)
                // Si el artículo no tiene lotes o no hemos escogido ninguno, limpiamos edtDet_Lote
            } else {
                edtDet_Lote.setText("")
            }
            edtDet_Lote.requestFocus()
        }
    }


}