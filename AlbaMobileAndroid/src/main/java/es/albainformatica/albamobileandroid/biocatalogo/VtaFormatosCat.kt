package es.albainformatica.albamobileandroid.biocatalogo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.FtosLineasDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.DtosLineasEnt
import es.albainformatica.albamobileandroid.entity.FtosLineasEnt
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.maestros.Formatos
import es.albainformatica.albamobileandroid.ventas.Documento
import es.albainformatica.albamobileandroid.ventas.DtosCascada
import es.albainformatica.albamobileandroid.ventas.TextoLinea
import kotlinx.android.synthetic.main.bio_vtaformatoscat.*


class VtaFormatosCat: AppCompatActivity() {
    private val ftosLineasDao: FtosLineasDao? = MyDatabase.getInstance(this)?.ftosLineasDao()
    private lateinit var fArticulos: ArticulosClase
    private lateinit var fFormatos: Formatos
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fDocumento: Documento
    private lateinit var fDtosCascada: DtosCascada

    private var fArticulo = 0
    private var fFormato: Short = 0
    private var fLineaDoc: Int = 0
    private var fTextoLinea: String = ""
    private var fFlag5: Int = 0

    private lateinit var fRecyclerView: RecyclerView
    private lateinit var fAdapter: RecAdapVtasFtos
    private var fRutaImagenes = ""

    private var fPedirCajas: Boolean = true
    private var fFtoDecCant: String = ""
    private var fFtoDecPrBase: String = ""
    private var fFtoDecPrII: String = ""
    private var fDecPrBase: Int = 2
    private var fEstado: Byte = est_Vl_Nueva
    private var fIvaIncluido: Boolean = true
    private var fAplicarIva: Boolean = true

    private lateinit var fContexto: Context
    private var fEmpresaActual: Short = 0


    private val fRequestTextoLinea = 1



    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.bio_vtaformatoscat)

        fContexto = this

        fArticulos = Comunicador.fArticulos
        fConfiguracion = Comunicador.fConfiguracion
        fDocumento = Comunicador.fDocumento
        fFormatos = Formatos(this)
        fDtosCascada = DtosCascada(this)

        val i = intent
        fArticulo = i.getIntExtra("articulo", 0)
        fArticulos.existeArticulo(fArticulo)
        fRutaImagenes = i.getStringExtra("rutaimagenes") ?: ""
        fEmpresaActual = intent.getShortExtra("empresa", 0)

        inicializarControles()
    }



    private fun inicializarControles() {

        tvDescrArt.text = fArticulos.fDescripcion

        // Presentamos también aquí la imagen del artículo
        val queFichero = "$fRutaImagenes/ART_$fArticulo.jpg"
        val bitmap = BitmapFactory.decodeFile(queFichero)
        imgArtFtos.setImageBitmap(bitmap)


        fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresaActual)
        fPedirCajas = fConfiguracion.pedirCajas()
        fFtoDecCant = fConfiguracion.formatoDecCantidad()
        fFtoDecPrBase = fConfiguracion.formatoDecPrecioBase()
        fFtoDecPrII = fConfiguracion.formatoDecPrecioIva()
        fDecPrBase = fConfiguracion.decimalesPrecioBase()
        fAplicarIva = fDocumento.fClientes.fAplIva

        setupRecyclerView()

        fFormato = 0
        // Comprobamos si hay formatos o tenemos que trabajar sin ellos
        if (fFormatos.lFtosCat.isNotEmpty())
            activarDesactivarEdits(false)
        else {
            activarDesactivarEdits(true)
            actualizarEdits()
        }

        prepararEdits()
        edtFtCatCajas.requestFocus()
    }


    private fun setupRecyclerView() {
        fRecyclerView = rvVtaFormatosCat
        fRecyclerView.layoutManager = GridLayoutManager(this, 4)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        fAdapter = RecAdapVtasFtos(getFormatos(), this, object : RecAdapVtasFtos.OnItemClickListener {
            override fun onClick(view: View, data: DatosVtaFtos) {

                if (fFormato == 0.toShort()) {
                    fFormato = data.formatoId
                    activarDesactivarEdits(true)
                    actualizarEdits()
                    if (fTextoLinea != "")
                        bioImgCatTxtLinea.setBackgroundColor(Color.LTGRAY)
                    imm.showSoftInput(edtFtCatCajas, 0)
                }
                else {
                    fFormato = 0
                    limpiarEdits()
                    activarDesactivarEdits(false)
                    bioImgCatTxtLinea.setBackgroundColor(Color.WHITE)
                }
            }
        })

        fRecyclerView.adapter = fAdapter
    }


    private fun getFormatos(): MutableList<DatosVtaFtos> {

        val lFormatos: MutableList<DatosVtaFtos> = arrayListOf()
        if (fFormatos.abrirFormatos(fArticulo, fDocumento.fCliente)) {
            for (datoVtaFto in fFormatos.lFtosCat) {
                val dVtasFtos = DatosVtaFtos()
                dVtasFtos.formatoId = datoVtaFto.formatoId
                dVtasFtos.descripcion = datoVtaFto.descripcion
                dVtasFtos.ftoLineaId = datoVtaFto.ftoLineaId
                dVtasFtos.borrar = datoVtaFto.borrar
                dVtasFtos.historicoId = datoVtaFto.historicoId
                lFormatos.add(dVtasFtos)
            }
        }

        return lFormatos
    }


    private fun prepararEdits() {
        prepararCajas()
    }


    private fun prepararCajas() {
        // Pediremos cajas siempre que esté configurado. Establecemos los eventos OnKeyListener y OnFocusChangeListener.
        if (fPedirCajas) {
            edtFtCatCajas.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
                    // Calcularemos la cantidad a partir de las cajas siempre que estemos en una línea nueva
                    // o lo tengamos configurado así.
                    if (fEstado == est_Vl_Nueva || fConfiguracion.aconsUnidCajaModif())
                        calcularCantidadYTrfCj()

                    // Si tenemos configurado no pedir la cantidad al indicar cajas (Configuracion.noModifCantidad()),
                    // buscamos el siguiente control disponible y, si no lo hay, terminamos la línea.
                    if (fConfiguracion.noModifCantidad()) {
                        if (edtFtCatCajas.text.toString() != "" && java.lang.Double.parseDouble(edtFtCatCajas.text.toString().replace(',', '.')) != 0.0) {

                            edtFtCatCantidad.isEnabled = false
                            when {
                                edtFtCatPiezas.isEnabled -> edtFtCatPiezas.requestFocus()
                                edtFtCatPrecio.isFocusable -> edtFtCatPrecio.requestFocus()
                                edtFtCatDto.isEnabled -> edtFtCatDto.requestFocus()
                                //else if (edtLote.getVisibility() == View.VISIBLE)
                                //    edtLote.requestFocus()
                                else -> aceptarFormato(bioImgCatAcep)
                            }
                        }
                    }

                    return@OnKeyListener true
                }
                false
            })

            edtFtCatCajas.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus)
                    if (fConfiguracion.noModifCantidad()) {
                        edtFtCatCantidad.isEnabled = !(edtFtCatCajas.text.toString() != "" &&
                                java.lang.Double.parseDouble(edtFtCatCajas.text.toString().replace(',', '.')) != 0.0)
                    }
            }

        } else edtFtCatCajas.isEnabled = false
    }


    private fun calcularCantidadYTrfCj() {
        var sCajas = edtFtCatCajas.text.toString().replace(',', '.')
        if (sCajas == "") {
            sCajas = "0.0"
            fDocumento.fCajas = 0.0
        }
        val numCajas = java.lang.Double.parseDouble(sCajas)

        fDocumento.fCantidad = fArticulos.fUCaja * numCajas
        edtFtCatCantidad.setText(fDocumento.fCantidad.toString())

        // Si el artículo tiene el flag de Aplicar tarifa de cajas e indicamos alguna caja, aplicaremos dicha tarifa, siempre y
        // cuando el documento lo permita.
        //if (!fHeElegidoTarifa) {
        if (numCajas != 0.0) {
            if (fArticulos.aplicarTrfCajas() && fDocumento.fPuedoAplTrfCajas) {
                fDocumento.fTarifaLin = fConfiguracion.tarifaCajas()
                calcularPrecioYDto()
            }
        }
        //}
    }


    private fun calcularPrecioYDto() {
        edtFtCatPrecio.setText("")
        // Calculamos precio y dto. una vez que ya tenemos la tarifa.
        fDocumento.calculaPrecioYDto(fArticulos.fGrupo, fArticulos.fDepartamento, fArticulos.fCodProv, fArticulos.fPorcIva)

        // Mostramos precio y dto.
        if (fIvaIncluido && fAplicarIva) {
            edtFtCatPrecio.setText(String.format(fFtoDecPrII, fDocumento.fPrecioII))
        } else {
            edtFtCatPrecio.setText(String.format(fFtoDecPrBase, fDocumento.fPrecio))
        }

        // Si tenemos un descuento por importe (desde el rating), lo añadiremos como un descuento en cascada. Por ahora en estado linea nueva.
        if (fEstado == est_Vl_Nueva) {
            if (fDocumento.fDtoRatingImp != 0.0) {
                // Borramos los posibles dtos. en cascada anteriores
                if (fDocumento.fLineaConDtCasc) fDocumento.borrarDtosCasc(-1)
                anyadirDtoCascada()

                // Mostramos el precio neto.
                if (fIvaIncluido && fAplicarIva) {
                    var fDtoRatingImpII: Double = fDocumento.fDtoRatingImp + fDocumento.fDtoRatingImp * fDocumento.fPorcIva / 100
                    fDtoRatingImpII = redondear(fDtoRatingImpII, 2)
                    edtFtCatPrecio.setText(String.format(fFtoDecPrII, fDocumento.fPrecioII - fDtoRatingImpII))
                } else {
                    edtFtCatPrecio.setText(String.format(fFtoDecPrBase, fDocumento.fPrecio - fDocumento.fDtoRatingImp))
                }
            }
        }
        edtFtCatDto.setText(String.format("%.2f", fDocumento.fDtoLin))

        //if (fEstado == Constantes.est_Vl_Nueva) {
            // Guardamos el precio y dto. de la tarifa
        //    fDocumento.fPrecioTarifa = fDocumento.fPrecio
        //    fDocumento.fDtoLinTarifa = fDocumento.fDtoLin
        //}
    }


    private fun anyadirDtoCascada() {
        val dtoLineaEnt = DtosLineasEnt()

        dtoLineaEnt.lineaId = -1
        dtoLineaEnt.orden = 1
        dtoLineaEnt.descuento = "0.0"
        dtoLineaEnt.importe = fDocumento.fDtoRatingImp.toString()
        dtoLineaEnt.cantidad1 = "0.0"
        dtoLineaEnt.cantidad2 = "0.0"
        dtoLineaEnt.desdeRating = "T"

        fDocumento.insertarDtoCasc(dtoLineaEnt)

        fDtosCascada.abrir(-1)
        // Configuramos el objeto de los dtos. en cascada
        fDtosCascada.fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresaActual)
        fDtosCascada.fAplicarIva = fDocumento.fClientes.fAplIva
        fDtosCascada.fPorcIva = fDocumento.fPorcIva
        fDtosCascada.fDecPrBase = fConfiguracion.decimalesPrecioBase()

        fDocumento.fDtoLin = fDtosCascada.calcularDtoEquiv(fDocumento.fPrecio, fDecPrBase).toDouble()
        fDocumento.fLineaConDtCasc = true
    }



    @SuppressLint("Recycle")
    private fun actualizarEdits() {
        // En esta función localizamos el artículo y el formato en la tabla ftosLineas y presentamos sus valores.
        // Vemos también si el registro está relacionado con una línea del documento,
        // para hacer fDocumento.cargarLinea o fDocumento.inicializarLinea y establecer el estado de nueva línea o editar línea.
        limpiarEdits()
        fLineaDoc = 0
        fTextoLinea = ""
        fFlag5 = 0

        val lFtosLineas = ftosLineasDao?.getArtYFto(fArticulo, fFormato) ?: emptyList<FtosLineasEnt>().toMutableList()

        if (lFtosLineas.isNotEmpty()) {
            // Si hemos encontrado el formato entendemos que estamos editando.
            fEstado = est_Vl_Editar
            fLineaDoc = lFtosLineas[0].lineaId
            fTextoLinea = lFtosLineas[0].textoLinea
            fFlag5 = lFtosLineas[0].flag5

            val sCajas = lFtosLineas[0].cajas.replace(',', '.')
            val sPiezas = lFtosLineas[0].piezas.replace(',', '.')
            val sCantidad = lFtosLineas[0].cantidad.replace(',', '.')
            val sPrecio = lFtosLineas[0].precio.replace(',', '.')
            val sDto = lFtosLineas[0].dto.replace(',', '.')
            val queCajas = if (sCajas != "") sCajas.toDouble() else 0.0
            val quePiezas = if (sPiezas != "") sPiezas.toDouble() else 0.0
            val queCantidad = if (sCantidad != "") sCantidad.toDouble() else 0.0
            val quePrecio = if (sPrecio != "") sPrecio.toDouble() else 0.0
            val queDto = if (sDto != "") sDto.toDouble() else 0.0
            edtFtCatCajas.setText(String.format(fFtoDecCant, queCajas))
            edtFtCatPiezas.setText(String.format(fFtoDecCant, quePiezas))
            edtFtCatCantidad.setText(String.format(fFtoDecCant, queCantidad))
            edtFtCatPrecio.setText(String.format(fFtoDecPrBase, quePrecio))
            edtFtCatDto.setText(String.format("%.2f", queDto))

        } else fEstado = est_Vl_Nueva

        // Si el artículo y formato están asociados a una línea del documento la cargamos.
        if (fLineaDoc > 0)
            fDocumento.cargarLinea(fLineaDoc)
        

        if (fEstado == est_Vl_Nueva) {
            fDocumento.inicializarLinea()
            fDocumento.fArticulo = fArticulo
            fDocumento.fFormatoLin = fFormato
            // Ahora que le hemos asignado al documento el formato de la línea, tomamos el texto de artículo habitual
            fTextoLinea = fDocumento.textoArtHabitual()
            // Si la línea es nueva calculamos el precio y dto. para el artículo y formato
            calcularPrecioYDto()
        }

        edtFtCatCajas.requestFocus()
    }


    private fun limpiarEdits() {
        edtFtCatCajas.setText("")
        edtFtCatPiezas.setText("")
        edtFtCatCantidad.setText("")
        edtFtCatPrecio.setText("")
        edtFtCatDto.setText("")
    }



    private fun activarDesactivarEdits(activar: Boolean) {
        edtFtCatCajas.isEnabled = activar
        edtFtCatCantidad.isEnabled = activar
        edtFtCatPrecio.isEnabled = activar
        edtFtCatDto.isEnabled = activar

        // Activaremos las piezas siempre que en configuración así lo tengamos y también lo tenga el artículo en su ficha
        edtFtCatPiezas.isEnabled = fConfiguracion.usarPiezas() && fArticulos.usarPiezas() && activar
    }


    fun aceptarFormato(view: View) {
        // Si no hemos indicado nada no grabaremos. Comprobamos las cajas y la cantidad.
        if ((edtFtCatCajas.text.toString() != "") || (edtFtCatCantidad.text.toString() != "") || (edtFtCatPiezas.text.toString() != "")) {

            val queView = currentFocus
            // Si pulsamos Aceptar estando en edtFtCatCajas, calculamos la cantidad antes de salir. Lo mismo hacemos con la tarifa de cajas.
            if (queView?.id == edtFtCatCajas.id)
                calcularCantidadYTrfCj()

            val dCantidad: Double = edtFtCatCantidad.text.toString().toDouble()
            if (dCantidad < 0 && fConfiguracion.noVenderNeg()) {
                MsjAlerta(this).alerta(resources.getString(R.string.msj_NoVenderNeg))

            } else {
                val ftoLineaEnt = FtosLineasEnt()
                ftoLineaEnt.cajas = edtFtCatCajas.text.toString()
                ftoLineaEnt.piezas = edtFtCatPiezas.text.toString()
                ftoLineaEnt.cantidad = edtFtCatCantidad.text.toString()
                ftoLineaEnt.precio = edtFtCatPrecio.text.toString()
                ftoLineaEnt.dto = edtFtCatDto.text.toString()
                ftoLineaEnt.textoLinea = fTextoLinea
                ftoLineaEnt.flag5 = fFlag5
                ftoLineaEnt.borrar = "F"

                // Por ahora al editar mantenemos el flag que teniamos, no me he querido complicar más.
                if (fEstado == est_Vl_Editar) {
                    ftosLineasDao?.actualizar(ftoLineaEnt.cajas, ftoLineaEnt.piezas, ftoLineaEnt.cantidad,
                            ftoLineaEnt.precio, ftoLineaEnt.dto, ftoLineaEnt.textoLinea, ftoLineaEnt.flag5,
                            ftoLineaEnt.borrar, fArticulo, fFormato)
                } else {
                    ftoLineaEnt.articuloId = fArticulo
                    ftoLineaEnt.formatoId = fFormato

                    var queFlag = 0
                    if (fDocumento.fPrecioRating) queFlag = queFlag or FLAGLINEAVENTA_PRECIO_RATING
                    if (fDocumento.fHayCambPrecio) queFlag = queFlag or FLAGLINEAVENTA_CAMBIAR_PRECIO
                    if (fDocumento.fArtEnOferta) queFlag = FLAGLINEAVENTA_ARTICULO_EN_OFERTA
                    ftoLineaEnt.flag = queFlag

                    ftosLineasDao?.insertar(ftoLineaEnt)
                }

                // Si no tenemos ningún formato para el artículo o sólo uno, salimos al pulsar en el botón
                if (fFormatos.lFtosCat.count() > 1) {
                    fFormato = 0
                    limpiarEdits()
                    activarDesactivarEdits(false)
                    bioImgCatTxtLinea.setBackgroundColor(Color.WHITE)

                    // Desmarcamos el formato del que acabamos de vender
                    fAdapter.formatos[fAdapter.selectedPos].ftoLineaId = 1000
                    fAdapter.formatos[fAdapter.selectedPos].borrar = "F"
                    fAdapter.selectedPos = RecyclerView.NO_POSITION
                    fAdapter.notifyDataSetChanged()

                } else {
                    aceptarTodosFtos(view)
                }
            }
        }
    }


    fun borrarFormato(view: View) {
        view.getTag(0)       // Esto no vale para nada, sólo para que no dé warning el compilador

        if (fFormato > 0) {
            if (fEstado == est_Vl_Editar) {
                ftosLineasDao?.inicializarFormato(fArticulo, fFormato)
            }

            // Desmarcamos el formato que acabamos de borrar
            fFormato = 0
            limpiarEdits()
            activarDesactivarEdits(false)
            bioImgCatTxtLinea.setBackgroundColor(Color.WHITE)
            fAdapter.formatos[fAdapter.selectedPos].borrar = "T"
            fAdapter.selectedPos = RecyclerView.NO_POSITION
            fAdapter.notifyDataSetChanged()
        }
    }



    fun aceptarTodosFtos(view: View) {
        view.getTag(0)       // Esto no vale para nada, sólo para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }


    fun textoLinea(view: View) {
        view.getTag(0)       // Esto no vale para nada, sólo para que no dé warning el compilador

        if (fFormato > 0) {
            val i = Intent(this, TextoLinea::class.java)
            i.putExtra("textolinea", fTextoLinea)
            i.putExtra("salvar", false)
            startActivityForResult(i, fRequestTextoLinea)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fRequestTextoLinea) {
            if (resultCode == Activity.RESULT_OK) {
                fTextoLinea = data?.getStringExtra("textoLinea") ?: ""
                fFlag5 = data?.getIntExtra("flag5", 0) ?: 0
            }
        }
    }

}