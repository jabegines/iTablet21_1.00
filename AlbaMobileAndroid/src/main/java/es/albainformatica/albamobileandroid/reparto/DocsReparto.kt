package es.albainformatica.albamobileandroid.reparto


import android.app.AlertDialog
import android.app.DialogFragment
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.cobros.CobrosActivity
import es.albainformatica.albamobileandroid.dao.ContactosCltesDao
import es.albainformatica.albamobileandroid.dao.TiposIncDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.ContactosCltesEnt
import es.albainformatica.albamobileandroid.entity.TiposIncEnt
import es.albainformatica.albamobileandroid.impresion_informes.*
import es.albainformatica.albamobileandroid.maestros.Rutas
import es.albainformatica.albamobileandroid.ventas.*
import kotlinx.android.synthetic.main.docs_reparto.*
import kotlinx.android.synthetic.main.ventas_rutero.*
import java.util.ArrayList


class DocsReparto: AppCompatActivity() {
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fDocumento: Documento
    private lateinit var fReparto: Reparto
    private lateinit var fRutas: Rutas
    private lateinit var prefs: SharedPreferences

    private lateinit var fRecReparto: RecyclerView
    private lateinit var fAdpReparto: RepartoRvAdapter


    private var fIdDocumento = 0
    private var fEstado: String = ""
    private var fRutaActiva: Short = 0
    private var fClteDoc = 0
    private var fTipoDoc: Short = 0
    private lateinit var spRuta: Spinner
    private lateinit var chsIncidencias: Array<CharSequence>
    private var fTipoIncidencia = 0
    private var fPosAnterior = 0


    private val fRequestModifDocReparto = 1
    private val fRequestFirmarDoc = 2
    private val fRequestIncidencia = 3
    private val fRequestPendienteClte = 4


    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.docs_reparto)

        fConfiguracion = Comunicador.fConfiguracion
        fDocumento = Documento(this)
        fReparto = Reparto(this)
        fRutas = Rutas(this)
        inicializarControles()
    }

    override fun onDestroy() {
        // Guardamos el último cliente al que le hemos repartido,
        // para continuar por el siguiente cuando volvamos a entrar en el rutero_reparto.
        guardarPreferencias()
        fDocumento.close()

        // Guardamos la ruta activa para volver a presentarla la siguiente vez que entremos en NewDocsReparto
        fConfiguracion.activarRuta(fRutaActiva)
        super.onDestroy()
    }


    private fun guardarPreferencias() {
        prefs.edit().putInt("reparto_ult_doc", fIdDocumento).apply()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_reparto, menu)
        if (!fConfiguracion.imprimir()) {
            val mni = menu.findItem(R.id.mni_rep_imprimir)
            mni.isVisible = false
        }
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mni_rep_ver -> {
                verDoc(null)
                true
            }
            R.id.mni_rep_imprimir -> {
                imprimirDoc(null)
                true
            }
            R.id.mni_rep_exp_pdf -> {
                crearPDF(null)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun inicializarControles() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val tvNombreRep = findViewById<TextView>(R.id.tvNombreReparto)
        val queNombre = "Reparto: " + fConfiguracion.idReparto() + " - " + fConfiguracion.nombreReparto()
        tvNombreRep.text = queNombre
        spRuta = findViewById(R.id.spnVt_Ruta)

        val btnFirmar = findViewById<Button>(R.id.btnDRFirmar)
        if (!prefs.getBoolean("reparto_pedir_firma", false)) btnFirmar.visibility = View.GONE

        // Permitiremos ventas si tenemos configurado autoventa o preventa
        val btnRutero = findViewById<Button>(R.id.btnDRRutero)
        if (!fConfiguracion.hayAutoventa() && !fConfiguracion.hayPreventa()) btnRutero.visibility = View.GONE
        ocultarTeclado(this)

        fIdDocumento = 0
        fClteDoc = 0
        fEstado = ""

        inicContrRutero()
        prepararSpinners()

        //  Si tenemos algún documento tomamos los datos del primero
        //if (!fReparto.lDocsReparto.isEmpty()) nuevoClick()

        // Preparamos el array de incidencias por si las usamos para el documento.
        prepararIncidencias()
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.reparto)

        hacerClickEnRecycler()
    }


    private fun hacerClickEnRecycler() {
        // Mediante este código seleccionamos el primer registro del recyclerView y hacemos como si pulsáramos
        // click en él. Hay que hacerlo con un Handler().postDelayed() porque si no, da errores.
        if (fAdpReparto.datosReparto.count() > 0) {
            Handler().postDelayed({
                fRecReparto.findViewHolderForAdapterPosition(0)?.itemView?.performClick()
            }, 100)
        }
    }



    private fun prepararSpinners() {
        // Spinner de rutas
        val aRutas: Array<String> = fRutas.abrir()
        val adaptador = ArrayAdapter(this, android.R.layout.simple_spinner_item, aRutas)
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spRuta.adapter = adaptador

        // Seleccionamos la ruta activa.
        spRuta.setSelection(getIndexRuta(spRuta, fRutaActiva))

        spRuta.onItemSelectedListener = object: OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                //val cursor = parent.getItemAtPosition(position) as Cursor

                // Mostramos en el spinner el código de la ruta, no el nombre.
                val fOldRuta = fRutaActiva
                fRutaActiva = (spRuta.getItemAtPosition(position).toString().substring(0, 2).trim { it <= ' ' }).toShort()
                val queView = view as TextView
                queView.textSize = 12f
                queView.text = fRutaActiva.toString()
                queView.gravity = Gravity.CENTER

                if (fRutaActiva != fOldRuta) {
                    if (fRutaActiva > 0) {
                        if (fReparto.abrir(fRutaActiva)) {
                            mostrarRutaAct()
                            prepararRecyclerView()
                            fIdDocumento = fReparto.lDocsReparto[0].cabeceraId
                        } else {
                            fRutaActiva = fOldRuta

                            // Volvemos a dejar el spinner con la ruta que teníamos.
                            spRuta.setSelection(getIndexRuta(spRuta, fRutaActiva))
                            MsjAlerta(this@DocsReparto).alerta("La ruta no tiene clientes")
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun getIndexRuta(s1: Spinner, nombre: Short): Int {
        var index = 0
        for (i in 0 until s1.count) {
            val queCodigo = s1.getItemAtPosition(i).toString().substring(0, 2).trim { it <= ' ' }
            if (queCodigo == nombre.toString()) {
                index = i
            }
        }
        return index
    }


    private fun inicContrRutero() {
        fRutaActiva = fConfiguracion.rutaActiva()

        if (fRutaActiva > 0) {
            mostrarRutaAct()
        } else {
            // Si no tenemos ruta de reparto buscamos la primera que haya en los documentos
            fRutaActiva = fReparto.buscarRutaActiva()
        }

        fRecReparto = rvDRDocumentos
        fRecReparto.layoutManager = LinearLayoutManager(this)
        prepararRecyclerView()

        if (fRutaActiva > 0) {
            // Vemos el último documento que repartimos.
            fIdDocumento = prefs.getInt("reparto_ult_doc", 0)

        } else {
            fIdDocumento = 0
            fClteDoc = 0
        }
    }


    private fun mostrarRutaAct() {
        val tvRutaAct = findViewById<TextView>(R.id.tvDocsRRutaAct)
        tvRutaAct.text = fRutas.dimeNombre(fRutaActiva)
    }



    private fun prepararRecyclerView() {
        fAdpReparto = RepartoRvAdapter(getDocsReparto(), this, object: RepartoRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosReparto) {
                fIdDocumento = data.cabeceraId
                fEstado = data.estado
                fTipoDoc = data.tipoDoc
                fClteDoc = data.clienteId
            }
        })

        fRecReparto.adapter = fAdpReparto
    }


    private fun getDocsReparto(): List<DatosReparto> {
        fReparto.abrir(fRutaActiva)
        return fReparto.lDocsReparto
    }



    private fun siguienteDocReparto() {
        fAdpReparto.selectedPos = fPosAnterior + 2
        fIdDocumento = fAdpReparto.datosReparto[fAdpReparto.selectedPos].cabeceraId
        fAdpReparto.notifyDataSetChanged()
    }


    fun verDoc(view: View?) {
        view?.getTag(0)          // Para que no dé warning el compilador

        if (fIdDocumento > 0) {
            val i = Intent(this, VentasLineas::class.java)
            i.putExtra("nuevo", false)
            i.putExtra("solover", true)
            i.putExtra("iddoc", fIdDocumento)
            i.putExtra("tipodoc", fTipoDoc)
            startActivity(i)
        } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
    }


    fun modificarDoc(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fIdDocumento > 0) {
            // Si el documento viene de gestión llamaremos a ModifDocReparto. En cambio si lo hemos hecho nuevo
            // desde ModifDocReparto no permitiremos la modificación.
            if (fEstado.equals("0", ignoreCase = true)) {
                // Por ahora sólo modificaremos si el documento es albarán.
                if (fTipoDoc == TIPODOC_ALBARAN || fTipoDoc == TIPODOC_FACTURA) {
                    val i = Intent(this, ModifDocReparto::class.java)
                    i.putExtra("iddoc", fIdDocumento)
                    i.putExtra("tipoDocOrg", fTipoDoc)
                    startActivityForResult(i, fRequestModifDocReparto)
                }
                //else new MsjAlerta(this).alerta("En reparto sólo podrá modificar albaranes");
            } else MsjAlerta(this).alerta("No puede modificar un albarán creado por modificar un reparto")
        } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
    }


    fun crearPDF(view: View?) {
        view?.getTag(0)          // Para que no dé warning el compilador

        if (fIdDocumento > 0) {
            Comunicador.fDocumento = fDocumento
            fDocumento.cargarDocumento(fIdDocumento, false)
            val dialogo = ProgressDialog.show(this, "Exportar a PDF", "Creando PDF ...", true, true)
            val hiloExport: Thread = object : Thread() {
                override fun run() {
                    // Necesitamos hacer Looper.prepare() y Looper.loop() para mostrar el
                    // mensaje de alerta dentro del hilo.
                    Looper.prepare()
                    // Creo el objeto de tipo DocPDF y le digo que genere el fichero.
                    val documPDF = DocPDF(this@DocsReparto)
                    documPDF.crearPDF()

                    // Cerramos el diálogo y mostramos mensaje.
                    dialogo.dismiss()

                    // Comprobamos si el Whatsapp está instalado
                    if (whatsappInstalado(this@DocsReparto)) {
                        val aldDialog = nuevoAlertBuilder(
                            this@DocsReparto,
                            "Escoja",
                            "Enviar documento PDF",
                            true
                        )

                        aldDialog.setPositiveButton("Por email") { _, _ ->
                            // Enviamos el documento por email.
                            documPDF.enviarPorEmail()
                            MsjAlerta(this@DocsReparto).alerta("Se terminó de exportar")
                        }
                        aldDialog.setNegativeButton("Por whatsapp") { _, _ ->
                            val telfDao: ContactosCltesDao? = MyDatabase.getInstance(this@DocsReparto)?.contactosCltesDao()
                            val lTelefonos = telfDao?.getTlfsCliente(fDocumento.fCliente) ?: emptyList<ContactosCltesEnt>().toMutableList()
                            var numeroTelefono = lTelefonos[0].telefono1
                            if (numeroTelefono == "") numeroTelefono = lTelefonos[0].telefono2
                            // Si no añadimos el prefijo no funciona
                            if (!numeroTelefono.startsWith("34")) numeroTelefono = "34$numeroTelefono"

                            enviarPorWhatsapPdf(this@DocsReparto, documPDF.nombrePDF, numeroTelefono)
                            MsjAlerta(this@DocsReparto).alerta("Se terminó de exportar")
                        }
                        aldDialog.setCancelable(true)
                        val alert = aldDialog.create()
                        alert.show()
                    }
                    else {
                        documPDF.enviarPorEmail()
                        MsjAlerta(this@DocsReparto).alerta("Se terminó de exportar")
                    }

                    Looper.loop()
                }
            }
            hiloExport.start()
        } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
    }


    fun imprimirDoc(view: View?) {
        view?.getTag(0)          // Para que no dé warning el compilador

        if (fIdDocumento > 0) {
            Comunicador.fDocumento = fDocumento
            fDocumento.cargarDocumento(fIdDocumento, false)

            // Lo primero que debemos hacer es rescatar el layout creado para el prompt.
            val li = LayoutInflater.from(this)
            val prompt = li.inflate(R.layout.imprimir_doc, null)
            // Luego, creamos un constructor de Alert Dialog que nos ayudará a utilizar nuestro layout.
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setView(prompt)
            // Por ultimo, creamos el cuadro de dialogo y las acciones requeridas al aceptar o cancelar el prompt.
            // Mostramos el mensaje del cuadro de diálogo
            alertDialogBuilder.setCancelable(false).setNegativeButton(
                "Cancelar"
            ) { dialog: DialogInterface, _: Int ->
                // Cancelamos el cuadro de dialogo
                dialog.cancel()
            }

            // Creamos un AlertDialog y lo mostramos
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()

            // Establecemos los eventos para los distintos botones del layout del diálogo.
            val btnImpr = alertDialog.findViewById<Button>(R.id.btnImprDoc)
            btnImpr.setOnClickListener {
                // Vemos si tenemos que pedir el formato con el que queremos imprimir o no.
                if (fConfiguracion.pedirFormato()) {
                    val newFragment: DialogFragment = VentasLineas.DlgSeleccFormato.newInstance(R.string.app_name)
                    newFragment.show(fragmentManager, "dialog")
                } else {
                    if (fConfiguracion.impresora() == IMPRESORA_DATAMAX_APEX_2) {
                        val imprDoc = ImprDocDatamaxApex2(this@DocsReparto)
                        imprDoc.imprimir()
                    } else if (fConfiguracion.impresora() == IMPRESORA_BIXOLON_SPP_R410 || fConfiguracion.impresora() == IMPRESORA_GENERICA_110 || fConfiguracion.impresora() == IMPRESORA_GENERICA_80
                    ) {
                        val imprDoc = ImprGenerica(this@DocsReparto)
                        imprDoc.imprimir()
                    } else if (fConfiguracion.impresora() == IMPRESORA_ZEBRA_80) {
                        val imprDoc = ImprZebra(this@DocsReparto)
                        imprDoc.imprimir()
                    } else {
                        val imprDoc = ImprimirDocumento(this@DocsReparto)
                        imprDoc.imprimir(false)
                    }
                }
                alertDialog.dismiss()
            }
            val btnExpPDF = alertDialog.findViewById<Button>(R.id.btnExpPDFDoc)
            btnExpPDF.visibility = View.GONE
        } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
    }


    fun pendienteClte(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fClteDoc > 0) {
            val i = Intent(this, CobrosActivity::class.java)
            i.putExtra("cliente", fClteDoc)
            startActivityForResult(i, fRequestPendienteClte)
        } else MsjAlerta(this).alerta(getString(R.string.msj_SinClte))
    }


    fun firmarDoc(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fIdDocumento > 0) {
            if (fEstado == "X") {
                MsjAlerta(this).alerta(resources.getString(R.string.msj_FirmarRegExp))
            } else {
                val i = Intent(this, FirmarDoc::class.java)
                i.putExtra("id_doc", fIdDocumento)
                i.putExtra("tipo_doc", fDocumento.fTipoDoc)
                startActivityForResult(i, fRequestFirmarDoc)
            }
        } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
    }

    fun incidenciaDoc(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fIdDocumento > 0) {
            if (fEstado == "X") {
                MsjAlerta(this).alerta(resources.getString(R.string.msj_IncidRegExp))
            } else {
                fDocumento.cargarDocumento(fIdDocumento, false)
                val altbld = AlertDialog.Builder(this)
                altbld.setTitle("Escoger incidencia")
                val queTipoInc = fDocumento.cabActualEnt.tipoIncidencia
                val queItem: Int = localizarIncid(queTipoInc)
                altbld.setSingleChoiceItems(chsIncidencias, queItem) { dialog: DialogInterface, item: Int ->
                    val sIncidencia: String = chsIncidencias[item].toString()
                    fTipoIncidencia = sIncidencia.substring(0, 2).toByte().toInt()
                    dialog.dismiss()
                    val i = Intent(this@DocsReparto, VentasIncidencia::class.java)
                    i.putExtra("cliente", fClteDoc)
                    i.putExtra("incidencia", sIncidencia)
                    i.putExtra("texto", fDocumento.getTextoIncidencia(fIdDocumento))
                    startActivityForResult(i, fRequestIncidencia)
                }
                val alert = altbld.create()
                alert.show()
            }
        } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
    }


    private fun localizarIncid(queTipoInc: Int): Int {
        for (i in chsIncidencias.indices) {
            if (chsIncidencias[i].toString().substring(0, 2).toByte().toInt() == queTipoInc)
                return i
        }
        return -1
    }


    fun irARutero(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val i = Intent(this, VentasActivity::class.java)
        i.putExtra("cliente", fClteDoc)
        startActivity(i)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fRequestFirmarDoc) {
            if (resultCode == RESULT_OK) {
                fReparto.marcarComoEntregado(fIdDocumento)
                prepararRecyclerView()
            }
        } else if (requestCode == fRequestModifDocReparto) {
            if (resultCode == RESULT_OK) {
                fPosAnterior = fAdpReparto.selectedPos
                prepararRecyclerView()
                siguienteDocReparto()
            }

        } else if (requestCode == fRequestIncidencia) {
            if (resultCode == RESULT_OK) {
                val textoIncidencia = data?.getStringExtra("textoincid") ?: ""
                fReparto.setTextoIncidencia(fIdDocumento, textoIncidencia, fTipoIncidencia)
                prepararRecyclerView()
            }
        } else if (requestCode == fRequestPendienteClte) {
            prepararRecyclerView()
        }
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



}