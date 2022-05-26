package es.albainformatica.albamobileandroid.cobros

import android.app.Activity
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.content.Intent
import android.view.KeyEvent
import android.view.View
import android.widget.*
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.ventas.NumberTextWatcher
import es.albainformatica.albamobileandroid.entity.CobrosEnt
import org.jetbrains.anko.alert
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by jabegines on 14/10/13.
 */
class Cobrar: Activity() {
    private lateinit var fFPago: FormasPagoClase
    private lateinit var fDivisas: DivisasClase
    private lateinit var fCobros: CobrosClase
    private lateinit var fPendiente: PendienteClase
    private lateinit var fConfiguracion: Configuracion

    private var fCliente: Int = 0
    private lateinit var edtImpte: EditText
    private lateinit var edtAnotacion: EditText
    private lateinit var tvTotal: TextView
    private lateinit var tvCobrado: TextView
    private lateinit var tvPdte: TextView

    private var queFPago: String = ""
    private var queDivisa: String =  ""
    private var fEsPendiente: Boolean = false
    private var fTipoDoc: Byte = 0
    private var fTotal: Double = 0.0
    private var fCobrado: Double = 0.0
    private var fImptePdte: Double = 0.0
    private var fFtoDecImpIva: String = ""
    private var fPagoDoc: String = ""
    private var fEsContado: Boolean = false
    private var fDesdeVentas: Boolean = false
    private var fEmpresaActual = 0

    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.cobrar)

        fCobros = Comunicador.fCobros
        fFPago = FormasPagoClase(this)
        fDivisas = DivisasClase(this)
        fConfiguracion = Comunicador.fConfiguracion
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        fEmpresaActual = prefs.getInt("ultima_empresa", 0)
        val i = intent
        fEsPendiente = i.getBooleanExtra("espendiente", false)
        fPagoDoc = i.getStringExtra("fpagoDoc") ?: ""
        fDesdeVentas = i.getBooleanExtra("desdeventas", false)
        fCliente = i.getIntExtra("cliente", 0)
        fTipoDoc = i.getByteExtra("tipoDoc", 1.toByte())
        if (fEsPendiente) {
            val idPendiente = i.getIntExtra("idPendiente", 0)
            if (fDesdeVentas) {
                fPendiente = Comunicador.fPendiente
            } else {
                fPendiente = PendienteClase(this)
                fPendiente.abrirPendienteId(idPendiente)
            }
            fTotal = fPendiente.importe.toDouble()
            fCobrado = fPendiente.cobrado.toDouble()
            // Si no redondeamos tendremos problemas de precisión.
            fImptePdte = redondear(fTotal - fCobrado, 2)
        }
        inicializarControles()
    }


    private fun inicializarControles() {
        edtImpte = findViewById(R.id.edtCobrar_Impte)
        edtAnotacion = findViewById(R.id.edtCobrar_Anot)
        val lyPendiente = findViewById<View>(es.albainformatica.albamobileandroid.R.id.lyPendiente)
        tvTotal = findViewById(R.id.tvCobrar_Total)
        tvCobrado = findViewById(R.id.tvCobrar_Cobrado)
        tvPdte = findViewById(R.id.tvCobrar_Pdte)
        edtImpte.addTextChangedListener(NumberTextWatcher(edtImpte, 6, fConfiguracion.decimalesImpII()))
        fFtoDecImpIva = fConfiguracion.formatoDecImptesIva()
        fEsContado = false
        if (fEsPendiente) {
            // Hacemos un cobro desde ventas.
            if (fDesdeVentas) {
                if (fFPago.fPagoEsContado(fPagoDoc) || fTipoDoc == TIPODOC_ALBARAN.toByte()) {
                    fEsContado = true
                    edtImpte.isEnabled = false
                }
            }
            // A partir de aquí, el cobro puede ser desde ventas o desde la ficha de cobros.
            edtImpte.setText(String.format(fFtoDecImpIva, fImptePdte))
            prepararLyPdte()
        } else {
            lyPendiente.visibility = View.GONE
        }
        prepararSpinners()

        // Si la forma de pago es contado no presentaremos el botón de cancelar porque no permitiremos la cancelación,
        // ya que dejaríamos el documento con la forma de pago CON y sin cobro.
        if (fEsContado) {
            val btnCancelar = findViewById<Button>(R.id.btnCobrar_Cancelar)
            btnCancelar.visibility = View.GONE

            // Si no tenemos configurado pedir cobros al finalizar y el documento es de contado lo que hacemos es llamar
            // a hacerCobro() para que se genere el cobro del vencimiento y se cerrará la actividad
            if (!fConfiguracion.pedirCobrosVtos()) {
                hacerCobro(null)
            }
        } else {
            // Si venimos desde una venta y no tenemos configurado pedir cobros al finalizar y el documento no es de contado lo que hacemos es
            // abandonar la actividad, como si hubiésemos pulsado en Cancelar
            if (fDesdeVentas) {
                if (!fConfiguracion.pedirCobrosVtos()) {
                    val returnIntent = Intent()
                    setResult(RESULT_CANCELED, returnIntent)
                    finish()
                }
            }
        }
        val tvTitulo =
            findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.btn_cobrar)
    }

    private fun prepararLyPdte() {
        tvTotal.text = String.format(fFtoDecImpIva, fTotal)
        tvCobrado.text = String.format(fFtoDecImpIva, fCobrado)
        tvPdte.text = String.format(fFtoDecImpIva, fImptePdte)
    }

    private fun prepararSpinners() {
        prepararSpFPago()
        prepararSpDivisas()
    }

    private fun prepararSpFPago() {
        val spnFPago = findViewById<Spinner>(R.id.spnCobrar_FPago)
        val lFPago: List<DatosFPago> = fFPago.abrirSoloContado()

        // Inicializamos queFPago
        queFPago = lFPago[0].codigo
        val adaptador = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, lFPago)
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnFPago.adapter = adaptador
        spnFPago.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {

                val datFPago = parent.getItemAtPosition(position) as DatosFPago
                queFPago = datFPago.codigo
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun prepararSpDivisas() {
        val spnDivisas = findViewById<Spinner>(R.id.spnCobrar_Divisas)
        val lDivisas: List<DatosDivisa> = fDivisas.abrirParaSpinner()

        // Inicializamos queDivisa
        queDivisa = lDivisas[0].codigo
        val spinnerArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, lDivisas)
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnDivisas.adapter = spinnerArrayAdapter
        spnDivisas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val datDivisa = parent.getItemAtPosition(position) as DatosDivisa
                queDivisa = datDivisa.codigo
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    fun salir(view: View?) {
        view?.getTag(0)              // Para que no dé warning el compilador

        // Si la forma de pago es contado no permitiremos cancelar, ya que
        // dejaríamos el documento con la forma de pago CON y sin cobro.
        if (!fEsContado) {
            val returnIntent = Intent()
            setResult(RESULT_CANCELED, returnIntent)
            finish()
        }
    }

    fun hacerCobro(view: View?) {
        view?.getTag(0)              // Para que no dé warning el compilador

        if (puedoSalvar()) {
            val queEmpresa: Short
            val cobroEnt = CobrosEnt()
            if (fEsPendiente) {
                cobroEnt.clienteId = fPendiente.clienteId
                cobroEnt.tipoDoc = fPendiente.tipoDoc
                cobroEnt.almacen = fPendiente.almacen
                cobroEnt.serie = fPendiente.serie
                cobroEnt.numero = fPendiente.numero
                cobroEnt.ejercicio = fConfiguracion.ejercicio()
                cobroEnt.empresa = fPendiente.empresa
                queEmpresa = fPendiente.empresa

                // Obtenemos la fecha actual.
                val tim = System.currentTimeMillis()
                val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                cobroEnt.fechaCobro = df.format(tim)
                cobroEnt.cobro = edtImpte.text.toString()
                cobroEnt.fPago = queFPago
                cobroEnt.divisa = queDivisa
                cobroEnt.anotacion = edtAnotacion.text.toString()
                if (fDesdeVentas) {
                    if (fEsContado) cobroEnt.codigo = "CN" else cobroEnt.codigo = "CO"
                } else {
                    // El estado 'E' lo tienen los vencimientos que nos vienen desde la gestión. A éstos les pondremos
                    // el código "RE", mientras que a los vencimientos creados en la tablet les pondremos el código "CO".
                    if (fPendiente.estado == "E") cobroEnt.codigo = "RE" else cobroEnt.codigo =
                        "CO"
                }
                cobroEnt.estado = "N"
                cobroEnt.vAlmacen = fPendiente.cAlmacen
                cobroEnt.vPuesto = fPendiente.cPuesto
                cobroEnt.vApunte = fPendiente.cApunte
                cobroEnt.vEjercicio = fPendiente.ejercicio.toString() // vEjer. Tomamos fPendiente.getEjercicio() y no fPendiente.getCEjer() porque el vencimiento es nuevo
                // y "cejer" está en blanco, en cambio "ejer" sí tiene valor. La gestión lo que necesita para
                // localizar el vencimiento y darlo como cobrado es el ejercicio del mismo.
            } else {
                cobroEnt.clienteId = fCliente
                cobroEnt.tipoDoc = 0.toShort()
                cobroEnt.almacen = fConfiguracion.almacen()
                cobroEnt.serie = ""
                cobroEnt.numero = 0
                cobroEnt.ejercicio = fConfiguracion.ejercicio()
                cobroEnt.empresa = fEmpresaActual.toShort()
                queEmpresa = fEmpresaActual.toShort()
                // Obtenemos la fecha actual.
                val tim = System.currentTimeMillis()
                val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                cobroEnt.fechaCobro = df.format(tim)
                cobroEnt.cobro = edtImpte.text.toString()
                cobroEnt.fPago = queFPago
                cobroEnt.divisa = queDivisa
                cobroEnt.anotacion = edtAnotacion.text.toString()
                cobroEnt.codigo = "EN"
                cobroEnt.estado = "N"
                cobroEnt.vAlmacen = ""
                cobroEnt.vPuesto = ""
                cobroEnt.vAlmacen = ""
                cobroEnt.vEjercicio = ""
            }

            // Nos aseguramos de que hemos encontrado un ejercicio válido
            if (cobroEnt.ejercicio > -1) {

                fCobros.nuevoCobro(cobroEnt)
                if (fEsPendiente) fPendiente.actualizarCobrado(
                    edtImpte.text.toString().replace(",", "."), fPendiente.pendienteId
                )
                actualizarSaldo(
                    this,
                    cobroEnt.clienteId,
                    queEmpresa,
                    -cobroEnt.cobro.replace(',', '.').toDouble()
                )
                val returnIntent = Intent()
                setResult(RESULT_OK, returnIntent)
                finish()

            } else {
                alert("No se ha encontrado un ejercicio válido", "Ejercicio incorrecto") {
                    positiveButton("Ok") {
                        val returnIntent = Intent()
                        setResult(RESULT_CANCELED, returnIntent)
                        finish()
                    }
                }.show()
            }
        }
    }

    private fun puedoSalvar(): Boolean {
        return if (edtImpte.text.toString() == "") {
            MsjAlerta(this).alerta(resources.getString(R.string.msj_SinImpte))
            false
        } else if (edtImpte.text.toString().replace(",", ".").toDouble() == 0.0) {
            // Si el total es cero permitiremos también que edtImpte sea cero.
            if (fTotal == 0.0) true else {
                MsjAlerta(this).alerta(resources.getString(R.string.msj_SinImpte))
                false
            }
        } else {
            if (fEsPendiente) {
                if (edtImpte.text.toString().replace(",", ".").toDouble() > fImptePdte) {
                    MsjAlerta(this).alerta(resources.getString(R.string.msj_ImpteMayorPdte))
                    false
                } else true
            } else true
        }
    }

    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Capturamos la tecla atrás de la tablet para que sea la función salir()
        // la que controle la posible salida de la actividad.
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            salir(null)
            return true
        }
        // Para las demás cosas, se reenvía el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }
}