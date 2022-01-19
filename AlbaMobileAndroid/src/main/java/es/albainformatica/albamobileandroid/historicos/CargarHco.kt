package es.albainformatica.albamobileandroid.historicos

import android.app.Activity
import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.content.Intent
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.content.DialogInterface
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.HcoCompSemMesDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.ventas.ListaPreciosEspeciales
import es.albainformatica.albamobileandroid.ventas.AcumuladosMes
import kotlinx.android.synthetic.main.ventas_cargarhco.*

/**
 * Created by jabegines on 14/10/13.
 */
class CargarHco: Activity() {
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fHistorico: Historico

    private lateinit var fRecyclerView: RecyclerView
    private lateinit var fAdapter: HcoRvAdapter

    private var fLinea = 0

    private var fIvaIncluido: Boolean = false
    private var fAplicarIva: Boolean = true
    private var fCliente = 0
    private var fDesdeCltes: Boolean = true
    private var fFtoDecPrIva: String = ""
    private var fFtoDecPrBase: String = ""
    private var queBuscar: String = ""
    private lateinit var prefs: SharedPreferences
    private var fAlertarArtNoVend = false
    private var fDiasAlerta = 0
    private var fEmpresaActual = 0

    // Request de las actividades a las que llamamos.
    private  val fRequestEditarHco = 1


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.ventas_cargarhco)

        val i = intent
        fCliente = i.getIntExtra("cliente", 0)
        fAplicarIva = i.getBooleanExtra("aplicariva", true)
        fDesdeCltes = i.getBooleanExtra("desdecltes", false)
        fEmpresaActual = i.getIntExtra("empresa", 0)
        fConfiguracion = Comunicador.fConfiguracion
        fHistorico = if (fDesdeCltes) Historico(this) else Comunicador.fHistorico
        inicializarControles()
    }


    private fun inicializarControles() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        fRecyclerView = rvHcoArticulos
        fRecyclerView.layoutManager = LinearLayoutManager(this)

        queBuscar = ""
        fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresaActual)
        fFtoDecPrIva = fConfiguracion.formatoDecPrecioIva()
        fFtoDecPrBase = fConfiguracion.formatoDecPrecioBase()
        fDiasAlerta = fConfiguracion.diasAlertaArtNoVend()
        fAlertarArtNoVend = fDiasAlerta > 0

        val lyHcoBotones = findViewById<LinearLayout>(R.id.llyHco_Botones)
        if (fDesdeCltes) lyHcoBotones.visibility = View.GONE
        val btnAcumulados = findViewById<Button>(R.id.btnHco_Acumulados)
        if (btnAcumulados != null) {
            if (prefs.getBoolean("usar_acummes", false) || usarHcoCompSemMeses())
                btnAcumulados.visibility = View.VISIBLE else btnAcumulados.visibility = View.GONE
        }

        prepararRecyclerView("")

        ocultarTeclado(this)
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.btn_hco)
    }

    fun buscarEnHco(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Buscar artículos")
        val inflater = LayoutInflater.from(this)
        val dialogLayout = inflater.inflate(R.layout.bio_alert_dialog_with_edittext, null)
        builder.setView(dialogLayout)
        val editText = dialogLayout.findViewById<EditText>(R.id.editText)
        editText.requestFocus()
        builder.setPositiveButton("OK") { _: DialogInterface?, _: Int ->
            prepararRecyclerView(editText.text.toString())
        }
        builder.setNegativeButton("Cancelar") { dialog: DialogInterface, _: Int -> dialog.cancel() }
        val alert = builder.create()
        alert.show()
    }

    fun verPrecEspeciales(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val i = Intent(this, ListaPreciosEspeciales::class.java)
        i.putExtra("cliente", fCliente)
        startActivity(i)
    }


    private fun prepararRecyclerView(queBuscar: String) {
        fAdapter = HcoRvAdapter(getHco(queBuscar), fIvaIncluido, fAplicarIva, this, object: HcoRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosHistorico) {
                // Tomamos el campo historicoId de la fila en la que hemos pulsado.
                fLinea = data.historicoId

                val i = Intent(this@CargarHco, EditarHcoActivity::class.java)
                i.putExtra("linea", fLinea)
                i.putExtra("empresa", fEmpresaActual)
                i.putExtra("posicion", fAdapter.selectedPos)
                startActivityForResult(i, fRequestEditarHco)
            }
        })

        fRecyclerView.adapter = fAdapter
    }


    private fun getHco(queBuscar: String): List<DatosHistorico> {
        return if (queBuscar != "") {
            fHistorico.abrirConBusqueda(fCliente, queBuscar)
            fHistorico.lDatosHistorico
        } else {
            fHistorico.abrir(fCliente)
            fHistorico.lDatosHistorico
        }
    }




    fun editarHco(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fLinea > 0) {
            val i = Intent(this, EditarHcoActivity::class.java)
            i.putExtra("linea", fLinea)
            i.putExtra("empresa", fEmpresaActual)
            startActivityForResult(i, fRequestEditarHco)
        } else MsjAlerta(this).alerta(getString(R.string.msj_NoRegSelecc))
    }

    fun verAcumulados(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (prefs.getBoolean("usar_acummes", false)) {
            val i = Intent(this, AcumuladosMes::class.java)
            i.putExtra("cliente", fCliente)
            startActivity(i)
        } else {
            val i = Intent(this, AcumComSemMes::class.java)
            i.putExtra("cliente", fCliente)
            startActivity(i)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // Actividad editar historico.
        if (requestCode == fRequestEditarHco) {
            if (resultCode == RESULT_OK)
                prepararRecyclerView(queBuscar)

        }
    }

    fun cancelarHco(view: View?) {
        view?.getTag(0)          // Para que no dé warning el compilador

        if (!fDesdeCltes) {
            val aldDialog = NuevoAlertBuilder(
                this, getString(R.string.tit_cancelar_hco),
                getString(R.string.msj_Abandonar), true
            )
            aldDialog.setPositiveButton(getString(R.string.dlg_si)) { _: DialogInterface?, _: Int ->
                fHistorico.borrar()
                val returnIntent = Intent()
                setResult(RESULT_CANCELED, returnIntent)
                finish()
            }
            val alert = aldDialog.create()
            alert.show()
            ColorDividerAlert(this, alert)
        }
    }


    fun salvarHco(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(RESULT_OK, returnIntent)
        finish()
    }


    fun limpiarHco(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        queBuscar = ""
        prepararRecyclerView(queBuscar)
    }

    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            cancelarHco(null)
        }
        // Para las demas cosas, se reenvia el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }


    private fun usarHcoCompSemMeses(): Boolean {
        val hcoCompSemMesDao: HcoCompSemMesDao? = MyDatabase.getInstance(this)?.hcoCompSemMesDao()

        val queId = hcoCompSemMesDao?.existe() ?: 0
        return (queId > 0)
    }

}