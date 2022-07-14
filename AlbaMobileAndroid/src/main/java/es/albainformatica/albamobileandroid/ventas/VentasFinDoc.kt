package es.albainformatica.albamobileandroid.ventas


import es.albainformatica.albamobileandroid.cobros.FormasPagoClase
import es.albainformatica.albamobileandroid.cobros.PendienteClase
import android.os.Bundle
import android.content.Intent
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import es.albainformatica.albamobileandroid.maestros.GetDireccClte
import android.content.DialogInterface
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.cobros.CobrosActivity
import es.albainformatica.albamobileandroid.dao.CabecerasDao
import es.albainformatica.albamobileandroid.dao.DtosCltesDao
import es.albainformatica.albamobileandroid.dao.TiposIncDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.DtosCltesEnt
import es.albainformatica.albamobileandroid.entity.TiposIncEnt
import es.albainformatica.albamobileandroid.reparto.FirmarDoc
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParseException
import java.util.*

/**
 * Created by jabegines on 14/10/13.
 */
class VentasFinDoc: AppCompatActivity() {
    private val dtosCltesDao: DtosCltesDao? = MyDatabase.getInstance(this)?.dtosCltesDao()
    private val cabecerasDao: CabecerasDao? = MyDatabase.getInstance(this)?.cabecerasDao()
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fFPago: FormasPagoClase
    private lateinit var fPendiente: PendienteClase
    private lateinit var fDocumento: Documento

    private lateinit var fRecBases: RecyclerView
    private lateinit var fAdpBases: BasesRvAdapter

    private var fTerminar: Boolean = false
    private var queFPago: String = ""
    private var fIdDoc = 0

    //private lateinit var dpFEntrega: DatePicker
    private lateinit var tvFEntrega: TextView
    private lateinit var edtObs1: EditText
    private lateinit var edtObs2: EditText
    private lateinit var edtDtoPie1: EditText
    private lateinit var edtDtoPie2: EditText
    private lateinit var edtDtoPie3: EditText
    private lateinit var edtDtoPie4: EditText
    private lateinit var chsIncidencias: Array<CharSequence>


    private val fRequestCambiarDirecc = 1

    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.ventas_fin_doc)

        fDocumento = Comunicador.fDocumento
        fConfiguracion = Comunicador.fConfiguracion
        fFPago = FormasPagoClase(this)
        fPendiente = PendienteClase(this)
        val i = intent
        fIdDoc = i.getIntExtra("iddoc", 0)
        fTerminar = i.getBooleanExtra("terminar", false)
        inicializarControles()
    }


    @SuppressLint("SetTextI18n")
    private fun inicializarControles() {
        val tvTipoDoc =
            findViewById<TextView>(R.id.tvVL_TipoDoc)
        val tvSerieNum =
            findViewById<TextView>(R.id.tvVL_SerieNum)
        val tvNombre = findViewById<TextView>(R.id.tvVL_Clte)
        val tvNomComerc =
            findViewById<TextView>(R.id.tvVL_NComClte)
        // Si tenemos rutero_reparto no mostraremos aquí el botón de incidencia, porque ya lo tenemos en la actividad de documentos.
        val btnIncidencia = findViewById<Button>(R.id.btnIncidenciaDoc)
        if (fConfiguracion.hayReparto()) btnIncidencia.visibility = View.GONE
        //dpFEntrega = findViewById(R.id.datePicker)
        tvFEntrega = findViewById(R.id.tvFEntr)
        edtObs1 = findViewById(R.id.edtVFP_Ob1)
        edtObs2 = findViewById(R.id.edtVFP_Ob2)
        edtDtoPie1 = findViewById(R.id.edtVFP_Dto1)
        edtDtoPie2 = findViewById(R.id.edtVFP_Dto2)
        edtDtoPie3 = findViewById(R.id.edtVFP_Dto3)
        edtDtoPie4 = findViewById(R.id.edtVFP_Dto4)
        tvTipoDoc.text = tipoDocAsString(fDocumento.fTipoDoc)
        tvSerieNum.text = fDocumento.serie + '/' + fDocumento.numero
        tvNombre.text = fDocumento.fClientes.fCodigo + " - " + fDocumento.nombreCliente()
        tvNomComerc.text = fDocumento.nombreComClte()
        val llFEntrega =
            findViewById<LinearLayout>(R.id.lyVFD_FEntrega)
        if (fDocumento.fTipoDoc == TIPODOC_PEDIDO) {
            //dpFEntrega.calendarView().firstDayOfWeek = Calendar.MONDAY
            // Le ponemos al control datepicker la fecha de entrega que tenga el documento.
            val day = ponerCeros(fDocumento.fFEntrega.substring(0, 2), 2)
            val month = ponerCeros(fDocumento.fFEntrega.substring(3, 5), 2)
            val year = fDocumento.fFEntrega.substring(6, 10)
            //dpFEntrega.updateDate(year, month, day)
            tvFEntrega.text = "$day/$month/$year"
            tvFEntrega.setOnClickListener { showDatePickerDialog(tvFEntrega) }

        } else {
            llFEntrega.isEnabled = false
            //dpFEntrega.isEnabled = false
            tvFEntrega.isEnabled = false
            // Si no estamos haciendo un pedido ocultamos el botón de más direcciones
            val btnMasDir =
                findViewById<Button>(R.id.btnMasDirecciones)
            btnMasDir.visibility = View.GONE
        }

        if (fConfiguracion.dtosPie()) {
            // Vemos si tenemos configurado aplicar descuentos sólo en factura
            if ((fDocumento.fTipoDoc == TIPODOC_FACTURA) || (!fConfiguracion.dtosCltesSoloFact()))  {
                edtDtoPie1.isEnabled = fConfiguracion.dtoPie1()
                edtDtoPie2.isEnabled = fConfiguracion.dtoPie2()
                edtDtoPie3.isEnabled = fConfiguracion.dtoPie3()
                edtDtoPie4.isEnabled = fConfiguracion.dtoPie4()
            }
            else {
                val lyDtos = findViewById<LinearLayout>(R.id.llVFP_LyDtos)
                lyDtos.visibility = View.GONE
            }
        } else {
            val lyDtos = findViewById<LinearLayout>(R.id.llVFP_LyDtos)
            lyDtos.visibility = View.GONE
        }

        if (fConfiguracion.fTamanyoPantLargo) {
            fRecBases = findViewById(R.id.rvVl_BasesDoc)
            prepararBases()
        }

        // Si estoy modificando un documento, cargo las observaciones y los dtos.
        if (fIdDoc > 0) {
            cargarDatos()
            aceptarDtos(edtDtoPie1)
            // Si estamos haciendo un documento nuevo, cargaremos los descuentos del cliente.
        } else {
            cargarDtosClte()
        }
        prepararSpFPago()
        // Preparamos el array de incidencias por si las usamos para el documento.
        prepararIncidencias()
        edtObs1.requestFocus()
        val tvTitulo =
            findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.fin_doc)

        // En el manifest, para esta actividad, tenemos
        // 'windowSoftInputMode="stateHidden|adjustNothing"', de forma que al entrar
        // en la misma no mostramos el teclado, pero si pulsamos en cualquier
        // control para mostrarlo, éste no hará ningún ajuste, o sea, cuando
        // mostremos el teclado éste ocultará los controles que tenga debajo.
    }

    private fun prepararSpFPago() {
        val sFPago: String
        val llFpago = findViewById<LinearLayout>(R.id.llVFD_Fpago)

        // Para los pedidos también pediremos la forma de pago.
        if (fDocumento.fTipoDoc == TIPODOC_FACTURA || fDocumento.fTipoDoc == TIPODOC_PEDIDO) {
            val spnFPago = findViewById<Spinner>(R.id.spnVFD_FPago)
            val lFPago: List<DatosFPago> = fFPago.abrir()

            val adaptador = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, lFPago)
            adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spnFPago.adapter = adaptador

            // Localizamos en el spinner la forma de pago que tuviera el documento,
            // para seguir aconsejándola, o bien si es un documento nuevo, aconsejamos
            // la forma de pago que tenga el cliente.
            sFPago = if (queFPago != "") queFPago
            else fDocumento.fClientes.fPago

            for (datFPago in lFPago) {
                if (sFPago == datFPago.codigo) {
                    spnFPago.setSelection(lFPago.indexOf(datFPago))
                    break
                }
            }

            // Tenemos en cuenta si el usuario no tiene permiso para modificar la forma de pago
            if (fConfiguracion.noCambiarFPago()) {
                spnFPago.isEnabled = false
                queFPago = sFPago

            } else {
                spnFPago.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {

                        val datFPago = parent.getItemAtPosition(position) as DatosFPago
                        queFPago = datFPago.codigo
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }

        } else llFpago.visibility = View.GONE
    }

    private fun cargarDtosClte() {

        val lDtosClte = dtosCltesDao?.getDtosClte(fDocumento.fCliente) ?: emptyList<DtosCltesEnt>().toMutableList()

        for (dtoClte in lDtosClte) {
            if (dtoClte.idDescuento == 1) edtDtoPie1.setText(dtoClte.dto)
            if (dtoClte.idDescuento == 2) edtDtoPie2.setText(dtoClte.dto)
            if (dtoClte.idDescuento == 3) edtDtoPie3.setText(dtoClte.dto)
            if (dtoClte.idDescuento == 4) edtDtoPie4.setText(dtoClte.dto)
        }
    }

    private fun cargarDatos() {

        val datosCabFinDoc = cabecerasDao?.getDatosFinDoc(fIdDoc) ?: DatosCabFinDoc()

        edtObs1.setText(datosCabFinDoc.observ1)
        edtObs2.setText(datosCabFinDoc.observ2)
        edtDtoPie1.setText(datosCabFinDoc.dto)
        edtDtoPie2.setText(datosCabFinDoc.dto2)
        edtDtoPie3.setText(datosCabFinDoc.dto3)
        edtDtoPie4.setText(datosCabFinDoc.dto4)
        // Esto nos sirve sólo para pedidos.
        if (fDocumento.fTipoDoc == TIPODOC_PEDIDO)
            queFPago = datosCabFinDoc.fPago

        if (fDocumento.fTipoDoc == TIPODOC_FACTURA) {
            queFPago = fPendiente.getFPagoDoc(fDocumento.fAlmacen, fDocumento.serie, fDocumento.numero, fDocumento.fEjercicio)
        }
    }

    private fun prepararBases() {
        fAdpBases = BasesRvAdapter(fDocumento.fBases.fLista, this, object: BasesRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: ListaBasesDoc.TBaseDocumento) {}
        })

        fRecBases.layoutManager = LinearLayoutManager(this)
        fRecBases.adapter = fAdpBases

        val tvTotal = findViewById<TextView>(R.id.tvVL_Total)
        val fFtoDecImpIva = fConfiguracion.formatoDecImptesIva()
        tvTotal.text = String.format(fFtoDecImpIva, fDocumento.fBases.totalConImptos)
    }

    fun aceptarDtos(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val dto1: Double = if (edtDtoPie1.text.toString() == "") 0.0 else edtDtoPie1.text.toString().replace(',', '.').toDouble()
        val dto2: Double = if (edtDtoPie2.text.toString() == "") 0.0 else edtDtoPie2.text.toString().replace(',', '.').toDouble()
        val dto3: Double = if (edtDtoPie3.text.toString() == "") 0.0 else edtDtoPie3.text.toString().replace(',', '.').toDouble()
        val dto4: Double = if (edtDtoPie4.text.toString() == "") 0.0 else edtDtoPie4.text.toString().replace(',', '.').toDouble()
        fDocumento.fDtoPie1 = dto1
        fDocumento.fDtoPie2 = dto2
        fDocumento.fDtoPie3 = dto3
        fDocumento.fDtoPie4 = dto4
        fDocumento.calcularDtosPie()
        if (fConfiguracion.fTamanyoPantLargo) prepararBases()
    }

    fun aceptarPie(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        // Si estamos haciendo un pedido pediremos la dirección de envío de mercancía
        if (fDocumento.fTipoDoc == TIPODOC_PEDIDO) {
            if (fDocumento.fClientes.variasDirecciones(fDocumento.fCliente)) {
                val i = Intent(this, GetDireccClte::class.java)
                i.putExtra("cliente", fDocumento.fCliente)
                startActivityForResult(i, fRequestCambiarDirecc)
            } else terminaAceptarPie()
        } else terminaAceptarPie()
    }

    fun masDirecciones(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val i = Intent(this, GetDireccClte::class.java)
        i.putExtra("cliente", fDocumento.fCliente)
        startActivityForResult(i, fRequestCambiarDirecc)
    }

    private fun terminaAceptarPie() {
        val returnIntent = Intent()
        returnIntent.putExtra("obs1", edtObs1.text.toString())
        returnIntent.putExtra("obs2", edtObs2.text.toString())
        // Si estamos haciendo un pedido devolvemos también la fecha de entrega.
        if (fDocumento.fTipoDoc == TIPODOC_PEDIDO) {
            val day = tvFEntrega.text.substring(0,2)
            val month = tvFEntrega.text.substring(3,5)
            val year = tvFEntrega.text.subSequence(6,10)
            val numCeros: Byte = 2
            var sFEntrega = ponerCeros(day, numCeros)
            sFEntrega += '/'.toString() + ponerCeros(month, numCeros)
            sFEntrega += "/$year"
            returnIntent.putExtra("fentrega", sFEntrega)
        }
        var dto: Double
        var sDto: String = edtDtoPie1.text.toString().replace(',', '.')
        dto = if (sDto != "") sDto.toDouble() else 0.0
        returnIntent.putExtra("dto1", dto)
        sDto = edtDtoPie2.text.toString().replace(',', '.')
        dto = if (sDto != "") sDto.toDouble() else 0.0
        returnIntent.putExtra("dto2", dto)
        sDto = edtDtoPie3.text.toString().replace(',', '.')
        dto = if (sDto != "") sDto.toDouble() else 0.0
        returnIntent.putExtra("dto3", dto)
        sDto = edtDtoPie4.text.toString().replace(',', '.')
        dto = if (sDto != "") sDto.toDouble() else 0.0
        returnIntent.putExtra("dto4", dto)
        // Devolveremos la forma de pago para facturas y para pedidos.
        if (fDocumento.fTipoDoc == TIPODOC_FACTURA || fDocumento.fTipoDoc == TIPODOC_PEDIDO)
            returnIntent.putExtra("fpago", queFPago)
        returnIntent.putExtra("terminar", fTerminar)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    fun anularPie(view: View?) {
        view?.getTag(0)              // Para que no dé warning el compilador

        val returnIntent = Intent()
        if (fDocumento.fTipoDoc == TIPODOC_FACTURA || fDocumento.fTipoDoc == TIPODOC_PEDIDO) returnIntent.putExtra(
            "fpago",
            queFPago
        )
        returnIntent.putExtra("terminar", fTerminar)
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }

    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            anularPie(null)
        }
        // Para las demás cosas, se reenvía el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }

    private fun prepararIncidencias() {
        val tiposIncDao: TiposIncDao? = MyDatabase.getInstance(this)?.tiposIncDao()
        val lIncidencias = tiposIncDao?.getAllIncidencias() ?: emptyList<TiposIncEnt>().toMutableList()

        val listItems: MutableList<String> = ArrayList()
        for (incidencia in lIncidencias) {
            listItems.add(ponerCeros(incidencia.tipoIncId.toString(), ancho_cod_incidencia) + "  " + incidencia.descripcion)
        }

        chsIncidencias = listItems.toTypedArray()
    }

    fun dialogoCambiarTipoDoc(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        // Tenemos esto para que no nos dé error. Desde esta actividad no haremos nada,
        // funciona en VentasLineas.
    }

    class DialogoIncidenciaDoc : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

            // Usamos la clase Builder para construir el diálogo
            val builder = AlertDialog.Builder(activity)
            // Obtenemos un layout inflater
            val inflater = activity.layoutInflater
            // Obtenemos una referencia de la vista que inflaremos, para luego poder acceder a cualquier
            // control que esté dentro de ella (p.ej. EditText textoIncidencia). Esta vista es la que pasamos al AlertDialog.
            val dialogView = inflater.inflate(R.layout.incidencia_doc, null)
            val edtTexto = dialogView.findViewById<EditText>(R.id.edtIncidencia)
            edtTexto.setText((activity as VentasFinDoc).fDocumento.fTextoIncidencia)

            // Llamamos a Inflate y establecemos un layout propio para el dialogo
            builder.setView(dialogView) // Añadimos action buttons
                .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                    (activity as VentasFinDoc).fDocumento.fTextoIncidencia = edtTexto.text.toString()
                    ocultarTeclado(activity)
                }
                .setNegativeButton(android.R.string.no) { _: DialogInterface?, _: Int -> }

            // Create the AlertDialog object and return it
            return builder.create()
        }

        companion object {
            fun newInstance(title: Int): DialogoIncidenciaDoc {
                val frag = DialogoIncidenciaDoc()
                val args = Bundle()
                args.putInt("title", title)
                frag.arguments = args
                return frag
            }
        }
    }

    fun incidenciaDoc(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val altBld = AlertDialog.Builder(this)
        altBld.setTitle("Escoger incidencia")
        altBld.setSingleChoiceItems(chsIncidencias, -1) { dialog: DialogInterface, item: Int ->
            val sIncidencia = chsIncidencias[item].toString()
            fDocumento.fIncidenciaDoc = sIncidencia.substring(0, 2).toByte().toInt()
            fDocumento.fTextoIncidencia = sIncidencia.substring(3)
            dialog.dismiss()
            val newFragment: DialogFragment = DialogoIncidenciaDoc.newInstance(R.string.app_name)
            newFragment.show(fragmentManager, "dialog")
        }
        val alert = altBld.create()
        alert.show()
    }

    fun firmarDoc(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val i = Intent(this, FirmarDoc::class.java)
        i.putExtra("id_doc", fDocumento.fIdDoc)
        i.putExtra("tipo_doc", fDocumento.fTipoDoc)
        startActivity(i)
    }

    fun verDesctos(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val pattern = "00.00"
        val formatter = DecimalFormat(pattern, DecimalFormatSymbols())
        try {
            MsjAlerta(this).informacion(
                """
                    Dto. pie 1:   ${formatter.format(formatter.parse(fDocumento.fDtoPie1.toString()))}
                    Dto. pie 2:   ${formatter.format(formatter.parse(fDocumento.fDtoPie2.toString()))}
                    Dto. pie 3:   ${formatter.format(formatter.parse(fDocumento.fDtoPie3.toString()))}
                    Dto. pie 4:   ${formatter.format(formatter.parse(fDocumento.fDtoPie4.toString()))}
                    """.trimIndent()
            )
        } catch (e: ParseException) {
            //
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Cambiar dirección del cliente
        if (requestCode == fRequestCambiarDirecc) {
            if (resultCode == RESULT_OK) {
                val queIdDireccion = data?.getIntExtra("idDireccion", 0) ?: 0
                if (queIdDireccion > 0) {
                    fDocumento.fAlmDireccion = data?.getShortExtra("almDireccion", 0).toString()
                    fDocumento.fOrdenDireccion = data?.getShortExtra("ordenDireccion", 0).toString()
                }
            }
            terminaAceptarPie()
        }
    }






    private fun showDatePickerDialog(editText: TextView) {
        val newFragment = CobrosActivity.DatePickerFragment.newInstance { _, year, month, day ->
            val selectedDate = ponerCeros(day.toString(), 2) + "/" + ponerCeros((month + 1).toString(), 2) + "/" + year
            editText.text = selectedDate
        }
        newFragment.show(supportFragmentManager, "datePicker")
    }

}